package org.nekomanga.presentation.screens.feed.updates

import android.text.format.DateUtils
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.util.Date
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.presentation.components.LoadMoreNearEnd
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.screens.EmptyScreen
import org.nekomanga.presentation.screens.feed.FeedManga
import org.nekomanga.presentation.screens.feed.FeedScreenActions
import org.nekomanga.presentation.theme.Size

/**
 * One card on the updates page: a chapter, or with [chapterCount] set, a series' newest chapter.
 */
private data class UpdateRow(
    val feedManga: FeedManga,
    val chapter: ChapterItem,
    val chapterCount: Int?,
)

@Composable
fun FeedUpdatesPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    feedUpdatesMangaList: List<FeedManga> = listOf(),
    outlineCovers: Boolean,
    dynamicCovers: Boolean,
    useVividColorHeaders: Boolean,
    hasMoreResults: Boolean,
    loadingResults: Boolean,
    groupedBySeries: Boolean,
    updatesFetchSort: Boolean,
    feedScreenActions: FeedScreenActions,
    loadNextPage: () -> Unit,
) {
    if (feedUpdatesMangaList.isEmpty()) {
        if (!loadingResults) {
            EmptyScreen(message = UiText.StringResource(R.string.no_results_found))
        }
        return
    }

    val scrollState = rememberLazyListState()
    scrollState.LoadMoreNearEnd(
        enabled = hasMoreResults && !loadingResults,
        itemCount = feedUpdatesMangaList.size,
        itemsFromEnd = 5,
        loadMore = loadNextPage,
    )

    val sections =
        remember(feedUpdatesMangaList, groupedBySeries) {
            val now = Date().time
            feedUpdatesMangaList
                .groupBy { getDateString(it.date, now) }
                .mapValues { (_, mangaForDate) ->
                    if (groupedBySeries) seriesRows(mangaForDate) else chapterRows(mangaForDate)
                }
        }
    val headerColor =
        if (useVividColorHeaders) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface
    val headerPrefix = if (updatesFetchSort) R.string.fetched_ else R.string.updated_

    LazyColumn(modifier = modifier, state = scrollState, contentPadding = contentPadding) {
        sections.forEach { (dateString, rows) ->
            if (dateString.isNotEmpty()) {
                item(key = dateString) {
                    Text(
                        text = stringResource(id = headerPrefix, dateString),
                        color = headerColor,
                        style = MaterialTheme.typography.titleLarge,
                        modifier =
                            Modifier.padding(
                                start = Size.small,
                                top = Size.small,
                                end = Size.small,
                            ),
                    )
                }
            }

            rows.forEachIndexed { index, row ->
                val feedManga = row.feedManga
                item(key = "$dateString-$index-${feedManga.mangaId}-${row.chapter.chapter.id}") {
                    ExpressiveListCard(
                        modifier = Modifier.padding(horizontal = Size.small),
                        listCardType = ListCardType.forPosition(index, rows.size),
                    ) {
                        UpdatesCard(
                            chapterItem = row.chapter,
                            numberOfChapters = row.chapterCount ?: 1,
                            isGrouped = row.chapterCount != null,
                            mangaTitle = feedManga.mangaTitle,
                            artwork = feedManga.artwork,
                            outlineCovers = outlineCovers,
                            dynamicCovers = dynamicCovers,
                            mangaClick = { feedScreenActions.mangaClick(feedManga.mangaId) },
                            chapterClick = { chapterId ->
                                feedScreenActions.chapterClick(feedManga.mangaId, chapterId)
                            },
                            chapterSwipe = { feedScreenActions.chapterSwipe(row.chapter) },
                            downloadClick = { action ->
                                feedScreenActions.downloadClick(row.chapter, feedManga, action)
                            },
                        )
                    }
                    if (index != rows.lastIndex) {
                        Gap(Size.tiny)
                    }
                }
            }
        }
    }
}

/** One row per chapter. */
private fun chapterRows(mangaForDate: List<FeedManga>): List<UpdateRow> =
    mangaForDate.flatMap { feedManga ->
        feedManga.chapters.map { chapter -> UpdateRow(feedManga, chapter, chapterCount = null) }
    }

/** One row per series, showing its oldest unread chapter, or its newest if all are read. */
private fun seriesRows(mangaForDate: List<FeedManga>): List<UpdateRow> =
    mangaForDate
        .groupBy { it.mangaId }
        .map { (_, entries) ->
            val (read, unread) = entries.flatMap { it.chapters }.partition { it.chapter.read }
            val chapters = unread.reversed() + read
            UpdateRow(entries.first(), chapters.first(), chapterCount = chapters.size)
        }

private fun getDateString(date: Long, currentDate: Long): String {
    return DateUtils.getRelativeTimeSpanString(date, currentDate, DateUtils.DAY_IN_MILLIS)
        .toString()
}
