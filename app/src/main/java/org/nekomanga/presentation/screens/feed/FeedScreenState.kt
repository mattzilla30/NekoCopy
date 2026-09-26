package org.nekomanga.presentation.screens.feed

import androidx.compose.runtime.Immutable
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.manga.Artwork

@Immutable
data class FeedScreenState(
    val outlineCovers: Boolean,
    val dynamicCovers: Boolean,
    val outlineCards: Boolean,
    val incognitoMode: Boolean = false,
)

@Immutable
data class HistoryScreenPagingState(
    val offset: Int = 0,
    val hasMoreResults: Boolean = true,
    val pageLoading: Boolean = false,
    val historyGrouping: FeedHistoryGroup,
    val historyFeedMangaList: List<FeedManga> = listOf(),
    val searchHistoryFeedMangaList: List<FeedManga> = listOf(),
    val searchQuery: String = "",
)

enum class FeedHistoryGroup {
    No,
    Series,
    Day,
    Week,
}

@Immutable
data class FeedSettingActions(
    val groupHistoryClick: (FeedHistoryGroup) -> Unit,
    val clearHistoryClick: () -> Unit,
    val outlineCoversClick: () -> Unit,
    val outlineCardsClick: () -> Unit,
)

@Immutable
data class FeedScreenActions(
    val mangaClick: (Long) -> Unit,
    val chapterClick: (Long, Long) -> Unit,
    val deleteHistoryClick: (FeedManga, SimpleChapter) -> Unit,
    val deleteAllHistoryClick: (FeedManga) -> Unit,
    val search: (String?) -> Unit,
)

@Immutable
data class FeedManga(
    val mangaTitle: String,
    val date: Long,
    val mangaId: Long,
    val artwork: Artwork,
    val chapters: List<ChapterItem>,
    val lastReadChapter: String = "",
)
