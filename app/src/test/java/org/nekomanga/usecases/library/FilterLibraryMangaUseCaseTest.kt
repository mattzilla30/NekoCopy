package org.nekomanga.usecases.library

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.presentation.screens.library.LibraryFilters
import org.nekomanga.presentation.screens.library.filter.FilterBookmarked
import org.nekomanga.presentation.screens.library.filter.FilterCompleted
import org.nekomanga.presentation.screens.library.filter.FilterMangaType
import org.nekomanga.presentation.screens.library.filter.FilterTracked
import org.nekomanga.presentation.screens.library.filter.FilterUnavailable
import org.nekomanga.presentation.screens.library.filter.FilterUnread

class FilterLibraryMangaUseCaseTest {

    private val useCase = FilterLibraryMangaUseCase()

    private fun mockMangaItem(
        unreadCount: Int = 0,
        downloadCount: Int = 0,
        bookmarkCount: Int = 0,
        isTracked: Boolean = false,
        unavailableCount: Int = 0,
        hasMissingChapters: Boolean = false,
        status: List<String> = emptyList(),
        seriesType: FilterMangaType = FilterMangaType.Manga,
    ): LibraryMangaItem {
        val displayManga = mockk<DisplayManga>()
        every { displayManga.getTitle() } returns "Manga Title"
        every { displayManga.mangaId } returns 1L

        return LibraryMangaItem(
            displayManga = displayManga,
            unreadCount = unreadCount,
            downloadCount = downloadCount,
            bookmarkCount = bookmarkCount,
            trackCount = if (isTracked) 1 else 0,
            userCover = null,
            dynamicCover = null,
            url = "",
            addedToLibraryDate = 0L,
            latestChapterDate = 0L,
            readCount = 0,
            category = 0,
            unavailableCount = unavailableCount,
            hasMissingChapters = hasMissingChapters,
            allCategories = emptyList(),
            altTitles = emptyList(),
            genre = emptyList(),
            author = emptyList(),
            contentRating = emptyList(),
            status = status,
            seriesType = seriesType,
        )
    }

    @Test
    fun `when all filters inactive, returns true`() {
        val manga = mockMangaItem()
        val filters = LibraryFilters()

        assertTrue(useCase(manga, filters))
    }

    @Test
    fun `when unread filter active and item has unread, returns true`() {
        val manga = mockMangaItem(unreadCount = 1)
        val filters = LibraryFilters(filterUnread = FilterUnread.Unread)

        assertTrue(useCase(manga, filters))
    }

    @Test
    fun `when unread filter active and item has no unread, returns false`() {
        val manga = mockMangaItem(unreadCount = 0)
        val filters = LibraryFilters(filterUnread = FilterUnread.Unread)

        assertFalse(useCase(manga, filters))
    }

    @Test
    fun `when tracked filter active and item is tracked, returns true`() {
        val manga = mockMangaItem(isTracked = true)
        val filters = LibraryFilters(filterTracked = FilterTracked.Tracked)

        assertTrue(useCase(manga, filters))
    }

    @Test
    fun `when tracked filter active and item is not tracked, returns false`() {
        val manga = mockMangaItem(isTracked = false)
        val filters = LibraryFilters(filterTracked = FilterTracked.Tracked)

        assertFalse(useCase(manga, filters))
    }

    @Test
    fun `when multiple filters active, fails fast on first mismatch`() {
        val manga = mockMangaItem(unreadCount = 0, bookmarkCount = 1)
        // Has bookmarks but no unread chapters
        val filters =
            LibraryFilters(
                filterUnread = FilterUnread.Unread,
                filterBookmarked = FilterBookmarked.Bookmarked,
            )

        // Fails because unread matches() returns false
        assertFalse(useCase(manga, filters))
    }

    @Test
    fun `when bookmarked filter active and item is bookmarked, returns true`() {
        val manga = mockMangaItem(bookmarkCount = 1)
        val filters = LibraryFilters(filterBookmarked = FilterBookmarked.Bookmarked)

        assertTrue(useCase(manga, filters))
    }

    @Test
    fun `when bookmarked filter active and item is not bookmarked, returns false`() {
        val manga = mockMangaItem(bookmarkCount = 0)
        val filters = LibraryFilters(filterBookmarked = FilterBookmarked.Bookmarked)

        assertFalse(useCase(manga, filters))
    }

    @Test
    fun `when not bookmarked filter active and item is not bookmarked, returns true`() {
        val manga = mockMangaItem(bookmarkCount = 0)
        val filters = LibraryFilters(filterBookmarked = FilterBookmarked.NotBookmarked)

        assertTrue(useCase(manga, filters))
    }

    @Test
    fun `when not bookmarked filter active and item is bookmarked, returns false`() {
        val manga = mockMangaItem(bookmarkCount = 1)
        val filters = LibraryFilters(filterBookmarked = FilterBookmarked.NotBookmarked)

        assertFalse(useCase(manga, filters))
    }

    @Test
    fun `when completed filter active and item is completed, returns true`() {
        val manga = mockMangaItem(status = listOf("Completed"))
        val filters = LibraryFilters(filterCompleted = FilterCompleted.Completed)

        assertTrue(useCase(manga, filters))
    }

    @Test
    fun `when completed filter active and item is ongoing, returns false`() {
        val manga = mockMangaItem(status = listOf("Ongoing"))
        val filters = LibraryFilters(filterCompleted = FilterCompleted.Completed)

        assertFalse(useCase(manga, filters))
    }

    @Test
    fun `when ongoing filter active and item is ongoing, returns true`() {
        val manga = mockMangaItem(status = listOf("Ongoing"))
        val filters = LibraryFilters(filterCompleted = FilterCompleted.Ongoing)

        assertTrue(useCase(manga, filters))
    }

    @Test
    fun `when ongoing filter active and item is completed, returns false`() {
        val manga = mockMangaItem(status = listOf("Completed"))
        val filters = LibraryFilters(filterCompleted = FilterCompleted.Ongoing)

        assertFalse(useCase(manga, filters))
    }

    @Test
    fun `when manga type filter active and item is manga, returns true`() {
        val manga = mockMangaItem(seriesType = FilterMangaType.Manga)
        val filters = LibraryFilters(filterMangaType = FilterMangaType.Manga)

        assertTrue(useCase(manga, filters))
    }

    @Test
    fun `when manga type filter active and item is manhwa, returns false`() {
        val manga = mockMangaItem(seriesType = FilterMangaType.Manhwa)
        val filters = LibraryFilters(filterMangaType = FilterMangaType.Manga)

        assertFalse(useCase(manga, filters))
    }

    @Test
    fun `when manhwa type filter active and item is manhwa, returns true`() {
        val manga = mockMangaItem(seriesType = FilterMangaType.Manhwa)
        val filters = LibraryFilters(filterMangaType = FilterMangaType.Manhwa)

        assertTrue(useCase(manga, filters))
    }

    @Test
    fun `when manhwa type filter active and item is manhua, returns false`() {
        val manga = mockMangaItem(seriesType = FilterMangaType.Manhua)
        val filters = LibraryFilters(filterMangaType = FilterMangaType.Manhwa)

        assertFalse(useCase(manga, filters))
    }

    @Test
    fun `when manhua type filter active and item is manhua, returns true`() {
        val manga = mockMangaItem(seriesType = FilterMangaType.Manhua)
        val filters = LibraryFilters(filterMangaType = FilterMangaType.Manhua)

        assertTrue(useCase(manga, filters))
    }

    @Test
    fun `when unavailable filter active and item has unavailable chapters, returns true`() {
        val manga = mockMangaItem(unavailableCount = 1)
        val filters = LibraryFilters(filterUnavailable = FilterUnavailable.Unavailable)

        assertTrue(useCase(manga, filters))
    }

    @Test
    fun `when unavailable filter active and item has no unavailable chapters, returns false`() {
        val manga = mockMangaItem(unavailableCount = 0)
        val filters = LibraryFilters(filterUnavailable = FilterUnavailable.Unavailable)

        assertFalse(useCase(manga, filters))
    }

    @Test
    fun `when no unavailable filter active and item has no unavailable chapters, returns true`() {
        val manga = mockMangaItem(unavailableCount = 0)
        val filters = LibraryFilters(filterUnavailable = FilterUnavailable.NoUnavailable)

        assertTrue(useCase(manga, filters))
    }

    @Test
    fun `when no unavailable filter active and item has unavailable chapters, returns false`() {
        val manga = mockMangaItem(unavailableCount = 1)
        val filters = LibraryFilters(filterUnavailable = FilterUnavailable.NoUnavailable)

        assertFalse(useCase(manga, filters))
    }
}
