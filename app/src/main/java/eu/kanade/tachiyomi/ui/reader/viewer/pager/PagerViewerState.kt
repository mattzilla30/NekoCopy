package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderHost
import kotlin.math.min
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.nekomanga.logging.TimberKt

/** Reading direction of a paged reader. */
enum class PagerDirection {
    LeftToRight,
    RightToLeft,
    Vertical,
}

/**
 * Compose state holder for the paged reading modes. [ComposePagerViewer] renders it, and it talks
 * to the reader screen through [ReaderHost].
 */
class PagerViewerState(private val host: ReaderHost, val direction: PagerDirection) : BaseViewer {
    val scope = MainScope()

    /** Target page position to synchronize with Compose Pager. */
    var requestedPagePosition by mutableStateOf<Pair<Int, Boolean>?>(null)

    /** Configuration used by the pager. */
    val config = PagerConfig(scope, direction)

    /** Pure domain controller for page pairing and transitions. */
    val controller = ReaderPagerController()

    /** Computed items list for Compose HorizontalPager. */
    var items by mutableStateOf<List<ReaderUiItem>>(emptyList())
        private set

    /** Currently active page index in the items list. */
    var currentPagePosition: Int = 0

    val isRtl: Boolean
        get() = direction == PagerDirection.RightToLeft

    val isVertical: Boolean
        get() = direction == PagerDirection.Vertical

    val prevTransition: ChapterTransition.Prev?
        get() = controller.prevTransition

    val nextTransition: ChapterTransition.Next?
        get() = controller.nextTransition

    val currentChapter: ReaderChapter?
        get() = controller.currentChapter

    var hasMoved = false

    private var isTransitioning: Boolean = false

    init {
        config.imagePropertyChangedListener = { host.onPagerImagePropertyChanged() }
        config.reloadChapterListener = { host.reloadChapters(it) }
        config.navigationModeChangedListener = {
            val showOnStart = config.navigationOverlayForNewUser
            host.setNavigation(config.navigator, showOnStart)
        }
        config.navigationModeInvertedListener = { host.showNavigationAgain() }
    }

    override fun destroy() {
        super.destroy()
        scope.cancel()
    }

    fun getShiftedPage(): ReaderPage? = controller.pageToShift

    fun updateShifting(page: ReaderPage? = null) {
        TimberKt.d { "update shifting" }
        controller.pageToShift =
            page ?: (items.getOrNull(currentPagePosition) as? ReaderUiItem.Page)?.page
    }

    fun triggerLoadChapter(
        chapter: Chapter,
        navTarget: ChapterNavTarget = ChapterNavTarget.Resume,
    ) {
        if (isTransitioning) return
        isTransitioning = true
        try {
            host.navigateToChapter(chapter, navTarget)
        } finally {
            isTransitioning = false
        }
    }

    /** Tells this viewer to set the given [chapters] as active. */
    override fun setChapters(chapters: ViewerChapters) {
        TimberKt.d { "setChapters" }
        val forceTransition = config.alwaysShowChapterTransition
        items =
            controller.buildItems(
                chapters = chapters,
                forceTransition = forceTransition,
                doublePages = config.doublePages,
                splitPages = config.splitPages,
                shiftDoublePage = config.shiftDoublePage,
                isRtl = isRtl,
            )
        host.onViewerItemsChanged()

        val pages = chapters.currChapter.pages ?: return
        val requestedIndex = min(chapters.currChapter.requestedPage, pages.lastIndex)
        if (requestedIndex in pages.indices) {
            moveToPage(pages[requestedIndex], false)
        }
    }

    val menuVisible: Boolean
        get() = host.menuVisible

    fun hideMenu() = host.hideMenu()

    fun toggleMenu() = host.toggleMenu()

    fun onPageLongTap(page: ReaderPage, extraPage: ReaderPage?) =
        host.onPageLongTap(page, extraPage)

    /** Tells this viewer to move to the given [page]. */
    override fun moveToPage(page: ReaderPage, animated: Boolean) {
        TimberKt.d { "moveToPage ${page.number}" }
        val position = controller.findPageIndex(items, page)
        if (position != -1) {
            currentPagePosition = position
            requestedPagePosition = position to animated
        } else {
            TimberKt.d { "Page $page not found in items" }
        }
    }

    override fun moveToNext() {
        if (isRtl) moveLeft() else moveRight()
    }

    override fun moveToPrevious() {
        if (isRtl) moveRight() else moveLeft()
    }

    /** Moves to the page at the right. */
    fun moveRight() {
        step(forward = true, entersNextChapter = !isRtl)
    }

    /** Moves to the page at the left. */
    fun moveLeft() {
        step(forward = false, entersNextChapter = isRtl)
    }

    /**
     * Moves one item through the list. When the current item is the chapter transition in the
     * direction of travel, loads that chapter instead.
     */
    private fun step(forward: Boolean, entersNextChapter: Boolean) {
        val current =
            (requestedPagePosition?.first ?: currentPagePosition).coerceIn(
                0,
                (items.size - 1).coerceAtLeast(0),
            )
        val transition = (items.getOrNull(current) as? ReaderUiItem.Transition)?.transition
        val targetChapter = transition?.to
        if (targetChapter != null) {
            if (entersNextChapter && transition is ChapterTransition.Next) {
                triggerLoadChapter(targetChapter.chapter, navTarget = ChapterNavTarget.Start)
                return
            }
            if (!entersNextChapter && transition is ChapterTransition.Prev) {
                triggerLoadChapter(targetChapter.chapter, navTarget = ChapterNavTarget.End)
                return
            }
        }
        val target = if (forward) current + 1 else current - 1
        if (target in items.indices) {
            hasMoved = true
            currentPagePosition = target
            requestedPagePosition = target to config.usePageTransitions
        }
    }

    /** Moves to the page at the top (or previous). */
    private fun moveUp() {
        moveToPrevious()
    }

    /** Moves to the page at the bottom (or next). */
    private fun moveDown() {
        moveToNext()
    }

    /**
     * Called from the containing activity when a key [event] is received. It should return true if
     * the event was handled, false otherwise.
     */
    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val isUp = event.action == KeyEvent.ACTION_UP
        val ctrlPressed = event.metaState.and(KeyEvent.META_CTRL_ON) > 0

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!config.volumeKeysEnabled || host.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveDown() else moveUp()
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!config.volumeKeysEnabled || host.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveUp() else moveDown()
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isUp) {
                    if (ctrlPressed) moveToNext() else moveRight()
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isUp) {
                    if (ctrlPressed) moveToPrevious() else moveLeft()
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> if (isUp) moveDown()
            KeyEvent.KEYCODE_DPAD_UP -> if (isUp) moveUp()
            KeyEvent.KEYCODE_PAGE_DOWN -> if (isUp) moveDown()
            KeyEvent.KEYCODE_PAGE_UP -> if (isUp) moveUp()
            KeyEvent.KEYCODE_MENU -> if (isUp) host.toggleMenu()
            else -> return false
        }
        return true
    }

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    override fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_CLASS_POINTER != 0) {
            when (event.action) {
                MotionEvent.ACTION_SCROLL -> {
                    if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                        moveDown()
                    } else {
                        moveUp()
                    }
                    return true
                }
            }
        }
        return false
    }
}
