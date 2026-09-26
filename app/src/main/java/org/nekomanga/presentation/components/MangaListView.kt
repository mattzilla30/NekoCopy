package org.nekomanga.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.theme.Size

@Composable
fun MangaList(
    mangaList: List<DisplayManga>,
    shouldOutlineCover: Boolean = true,
    dynamicCover: Boolean,
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Long) -> Unit = {},
    lastPage: Boolean = true,
    loadNextItems: () -> Unit = {},
) {
    val scrollState = rememberLazyListState()

    scrollState.LoadMoreNearEnd(
        enabled = !lastPage && mangaList.isNotEmpty(),
        itemCount = mangaList.size,
        loadMore = loadNextItems,
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scrollState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Size.tiny),
    ) {
        itemsIndexed(mangaList, key = { _, display -> display.mangaId }) { index, displayManga ->
            val listCardType = ListCardType.forPosition(index, mangaList.size)
            MangaListItem(
                displayManga = displayManga,
                listCardType = listCardType,
                shouldOutlineCover = shouldOutlineCover,
                dynamicCover = dynamicCover,
                onClick = onClick,
            )
        }
    }
}

@Composable
fun MangaListWithHeader(
    groupedManga: Map<Int, List<DisplayManga>>,
    shouldOutlineCover: Boolean,
    dynamicCover: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    collapsedGroups: Set<Int> = emptySet(),
    onToggleGroupCollapse: (Int) -> Unit = {},
    onClick: (Long) -> Unit = {},
) {
    val filteredGroupedManga =
        remember(groupedManga) { groupedManga.filterValues { it.isNotEmpty() }.toMap() }

    LazyColumn(
        modifier = modifier.wrapContentWidth(align = Alignment.CenterHorizontally),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Size.tiny),
    ) {
        filteredGroupedManga.forEach { (stringRes, mangaList) ->
            item(key = "header-$stringRes") {
                HeaderCard {
                    DefaultHeaderText(
                        text = stringResource(id = stringRes),
                        isExpanded = stringRes !in collapsedGroups,
                        onClick = { onToggleGroupCollapse(stringRes) },
                    )
                }
            }
            if (stringRes !in collapsedGroups) {
                itemsIndexed(
                    mangaList,
                    key = { _, displayManga -> "${stringRes}-item-${displayManga.mangaId}" },
                ) { index, displayManga ->
                    val listCardType = ListCardType.forPosition(index, mangaList.size)
                    MangaListItem(
                        modifier = Modifier.animateItem(),
                        displayManga = displayManga,
                        listCardType = listCardType,
                        shouldOutlineCover = shouldOutlineCover,
                        dynamicCover = dynamicCover,
                        onClick = onClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaListItem(
    modifier: Modifier = Modifier,
    displayManga: DisplayManga,
    listCardType: ListCardType,
    shouldOutlineCover: Boolean,
    dynamicCover: Boolean,
    onClick: (Long) -> Unit,
) {
    ExpressiveListCard(
        modifier = modifier.padding(horizontal = Size.small),
        listCardType = listCardType,
    ) {
        MangaRow(
            displayManga = displayManga,
            shouldOutlineCover = shouldOutlineCover,
            dynamicCover = dynamicCover,
            modifier =
                Modifier.fillMaxWidth().wrapContentHeight().clickable {
                    onClick(displayManga.mangaId)
                },
        )
    }
}

@Composable
fun MangaRow(
    displayManga: DisplayManga,
    shouldOutlineCover: Boolean,
    dynamicCover: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(Size.tiny),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover.Square.invoke(
            artwork = displayManga.currentArtwork,
            shouldOutlineCover = shouldOutlineCover,
            dynamicCover = dynamicCover,
            modifier = Modifier.size(Size.huge).padding(Size.tiny),
        )
        Column(modifier = Modifier.weight(1f).padding(Size.tiny)) {
            val titleLineCount = if (displayManga.displayText.isBlank()) 2 else 1
            MangaListTitle(title = displayManga.getTitle(), maxLines = titleLineCount)
            MangaListSubtitle(
                text = displayManga.displayText,
                textRes = displayManga.displayTextRes,
            )
        }
    }
}

@Composable
private fun MangaListTitle(title: String, maxLines: Int, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MangaListSubtitle(text: String, @StringRes textRes: Int?) {
    val displayText =
        when (textRes) {
            null -> text
            else -> stringResource(textRes)
        }
    if (displayText.isNotBlank()) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
