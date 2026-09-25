package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.data.coil.ReaderPageSplitFetcher
import eu.kanade.tachiyomi.ui.reader.loader.ReaderPreloadController
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderHost
import kotlin.math.min
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.nekomanga.logging.TimberKt

/**
 * Compose state holder for the continuous vertical (webtoon) reading mode. [ComposeWebtoonViewer]
 * renders it, and it talks to the reader screen through [ReaderHost].
 */
class WebtoonViewerState(
    private val host: ReaderHost,
    private val preloadController: ReaderPreloadController,
    val noWebtoonTag: Boolean = false,
) : BaseViewer {

    val scope = MainScope()

    data class WebtoonPagePosition(
        val targetPage: Int,
        val animated: Boolean,
        val timestamp: Long = System.nanoTime(),
    )

    /** Target page position to synchronize with Compose LazyList. */
    var requestedPagePosition by mutableStateOf<WebtoonPagePosition?>(null)

    /** Delta scroll to synchronize with Compose LazyList. */
    var requestedScrollDelta by mutableStateOf<Int?>(null)

    val hasMargins: Boolean
        get() = noWebtoonTag && !config.disableGaps

    /** Controller used for pure domain item building and split page calculations. */
    val controller = ReaderWebtoonController()

    /** Currently computed reader UI items. */
    var items by mutableStateOf<List<ReaderUiItem>>(emptyList())
        private set

    val prevTransition: ChapterTransition.Prev?
        get() = controller.prevTransition

    val nextTransition: ChapterTransition.Next?
        get() = controller.nextTransition

    val currentChapter: ReaderChapter?
        get() = controller.currentChapter

    /** Distance to scroll when the user taps on one side of the viewer. */
    private val scrollDistance: Int
        get() = host.screenHeight * 3 / 4

    /** Configuration used by this viewer. */
    val config = WebtoonConfig(scope)

    val menuVisible: Boolean
        get() = host.menuVisible

    fun toggleMenu() = host.toggleMenu()

    fun onPageLongTap(page: ReaderPage) = host.onPageLongTap(page)

    fun requestPreloadChapter(chapter: ReaderChapter) = host.requestPreloadChapter(chapter)

    init {
        preloadController.onPageSplit = { originalPage, insertPages ->
            splitPage(originalPage, insertPages)
        }
        config.reloadViewerListener = { host.reloadViewer() }
        config.navigationModeChangedListener = {
            val showOnStart = config.navigationOverlayForNewUser
            host.setNavigation(config.navigator, showOnStart)
        }
        config.navigationModeInvertedListener = { host.showNavigationAgain() }
        config.preloadPageAmountChangedListener = { amount ->
            updatePreload(lastActiveIndex, items, amount)
        }
    }

    private fun updatePreload(activeIndex: Int, items: List<ReaderUiItem>, preloadAmount: Int) {
        preloadController.onPositionChanged(
            currentIndex = activeIndex,
            items = items,
            preloadAmount = preloadAmount,
            isRtl = false,
            isWebtoon = true,
        )
    }

    /** Destroys this viewer. Called when leaving the reader or swapping viewers. */
    override fun destroy() {
        super.destroy()
        preloadController.onPageSplit = null
        preloadController.release()
        scope.cancel()
        pendingPageMove = null
        ReaderPageSplitFetcher.clearCache()
    }

    private var activeChapterId: Long? = null
    private var isInitialLoad = true
    private var pendingPageMove: Pair<ReaderPage, Boolean>? = null
    private var lastActiveIndex = 0

    /** Tells this viewer to set the given [chapters] as active. */
    override fun setChapters(chapters: ViewerChapters) {
        TimberKt.d { "setChapters" }
        val forceTransition = config.alwaysShowChapterTransition
        val screenHeight = host.screenHeight
        val newItems =
            controller.buildItems(
                chapters = chapters,
                forceTransition = forceTransition,
                screenHeight = if (config.splitTallPages) screenHeight else 0,
                existingItems = items,
            )
        val chapterChanged = activeChapterId != chapters.currChapter.chapter.id
        activeChapterId = chapters.currChapter.chapter.id

        items = newItems
        host.onViewerItemsChanged()

        val pages = chapters.currChapter.pages
        val requestedIndex = pages?.let { min(chapters.currChapter.requestedPage, it.lastIndex) }
        val targetPage =
            if (requestedIndex != null && requestedIndex in pages.indices) pages[requestedIndex]
            else pages?.firstOrNull()
        val initialActiveIndex =
            if (!chapterChanged && lastActiveIndex in newItems.indices) {
                lastActiveIndex
            } else {
                targetPage?.let { controller.findPageIndex(newItems, it) }?.takeIf { it != -1 } ?: 0
            }
        lastActiveIndex = initialActiveIndex
        updatePreload(initialActiveIndex, newItems, config.preloadPageAmount)

        val pending = pendingPageMove
        pendingPageMove = null
        if (
            pending != null && pending.first.chapter.chapter.id == chapters.currChapter.chapter.id
        ) {
            moveToPage(pending.first, pending.second)
        } else if (isInitialLoad) {
            isInitialLoad = false
            if (requestedIndex != null && requestedIndex in pages.indices) {
                moveToPage(pages[requestedIndex], false)
            }
        }
    }

    /** Tells this viewer to move to the given [page]. */
    override fun moveToPage(page: ReaderPage, animated: Boolean) {
        TimberKt.d { "moveToPage for page ${page.number} in chapter ${page.chapter.chapter.id}" }
        if (activeChapterId != null && page.chapter.chapter.id != activeChapterId) {
            TimberKt.d {
                "Queuing moveToPage for non-active chapter ${page.chapter.chapter.id} (active is $activeChapterId)"
            }
            pendingPageMove = page to animated
            return
        }
        val position = controller.findPageIndex(items, page)
        if (position != -1) {
            pendingPageMove = null
            requestedPagePosition = WebtoonPagePosition(position, animated)
        } else {
            TimberKt.d { "Page $page not found in items, queuing" }
            pendingPageMove = page to animated
        }
    }

    /** Notifies the viewer that a tall page was split into [insertPages]. */
    fun splitPage(originalPage: ReaderPage, insertPages: List<ReaderPageSplit>) {
        scope.launch {
            val newItems = controller.splitPage(items, originalPage, insertPages)
            items = newItems
            host.onViewerItemsChanged()
            updatePreload(lastActiveIndex, newItems, config.preloadPageAmount)
        }
    }

    /** Scrolls up by [scrollDistance]. */
    override fun moveToPrevious() {
        requestedScrollDelta = -scrollDistance
    }

    /** Scrolls down by [scrollDistance]. */
    override fun moveToNext() {
        requestedScrollDelta = scrollDistance
    }

    /**
     * Called from the containing activity when a key [event] is received. It should return true if
     * the event was handled, false otherwise.
     */
    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val isUp = event.action == KeyEvent.ACTION_UP

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!config.volumeKeysEnabled || host.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveToNext() else moveToPrevious()
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!config.volumeKeysEnabled || host.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveToPrevious() else moveToNext()
                }
            }
            KeyEvent.KEYCODE_MENU -> if (isUp) host.toggleMenu()
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_PAGE_UP -> if (isUp) moveToPrevious()
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN -> if (isUp) moveToNext()
            else -> return false
        }
        return true
    }

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    override fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        return false
    }
}
