package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

/**
 * The screen that hosts a reader viewer. Viewer state holders reach the reader chrome, navigation,
 * and chapter loading only through this interface, so they never hold an Activity.
 */
interface ReaderHost {
    val menuVisible: Boolean

    /** Height of the reader window in pixels, used to split tall webtoon pages. */
    val screenHeight: Int

    fun toggleMenu()

    fun hideMenu()

    fun onPageLongTap(page: ReaderPage, extraPage: ReaderPage? = null)

    fun requestPreloadChapter(chapter: ReaderChapter)

    fun navigateToChapter(chapter: Chapter, navTarget: ChapterNavTarget)

    /** Called when the viewer rebuilt its item list. */
    fun onViewerItemsChanged()

    /** Called when a pager image setting changed and the pages need rebuilding. */
    fun onPagerImagePropertyChanged()

    fun reloadChapters(doublePages: Boolean, force: Boolean = false)

    fun reloadViewer()

    fun setNavigation(navigation: ViewerNavigation, showOnStart: Boolean)

    fun showNavigationAgain()
}
