package org.nekomanga.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.functions.gridColumns
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun MangaGridWithHeader(
    groupedManga: Map<Int, List<DisplayManga>>,
    shouldOutlineCover: Boolean,
    dynamicCover: Boolean,
    modifier: Modifier = Modifier,
    columns: Int = gridColumns(),
    contentPadding: PaddingValues = PaddingValues(),
    collapsedGroups: Set<Int> = emptySet(),
    onToggleGroupCollapse: (Int) -> Unit = {},
    onClick: (Long) -> Unit = {},
) {
    // Chunk each group into grid rows, dropping empty groups.
    val chunkedGroupedManga =
        remember(groupedManga, columns) {
            groupedManga
                .mapValues { (_, list) -> list.chunked(columns) }
                .filterValues { it.isNotEmpty() }
                .toMap()
        }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Size.tiny),
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        chunkedGroupedManga.forEach { (stringRes, chunks) ->
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
                itemsIndexed(items = chunks, key = { index, _ -> "grid-row-$stringRes-$index" }) {
                    _,
                    rowItems ->
                    VerticalGrid(
                        columns = SimpleGridCells.Fixed(columns),
                        modifier =
                            Modifier.fillMaxWidth().padding(horizontal = Size.small).animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(Size.small),
                    ) {
                        rowItems.forEach { displayManga ->
                            MangaGridItem(
                                displayManga = displayManga,
                                shouldOutlineCover = shouldOutlineCover,
                                dynamicCover = dynamicCover,
                                onClick = onClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MangaGrid(
    mangaList: List<DisplayManga>,
    shouldOutlineCover: Boolean,
    dynamicCover: Boolean,
    columns: Int = gridColumns(),
    contentPadding: PaddingValues = PaddingValues(),
    onClick: (Long) -> Unit = {},
    lastPage: Boolean = true,
    loadNextItems: () -> Unit = {},
) {
    val cells = GridCells.Fixed(columns)

    val scrollState = rememberLazyGridState()

    scrollState.LoadMoreNearEnd(
        enabled = !lastPage && mangaList.isNotEmpty(),
        itemCount = mangaList.size,
        loadMore = loadNextItems,
    )

    LazyVerticalGrid(
        columns = cells,
        state = scrollState,
        modifier = Modifier.fillMaxSize().padding(Size.tiny),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Size.tiny),
        horizontalArrangement = Arrangement.spacedBy(Size.medium),
    ) {
        itemsIndexed(mangaList, key = { _, display -> display.mangaId }) { _, displayManga ->
            MangaGridItem(
                displayManga = displayManga,
                shouldOutlineCover = shouldOutlineCover,
                dynamicCover = dynamicCover,
                onClick = onClick,
            )
        }
    }
}

@Composable
fun MangaGridItem(
    displayManga: DisplayManga,
    shouldOutlineCover: Boolean,
    dynamicCover: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Long) -> Unit = {},
) {
    val subtitleText =
        when (displayManga.displayTextRes) {
            null -> displayManga.displayText
            else -> stringResource(displayManga.displayTextRes)
        }
    val title = displayManga.getTitle()
    val contentDescription =
        remember(title, subtitleText) {
            listOf(title, subtitleText).filter { it.isNotBlank() }.joinToString(", ")
        }

    Column(
        modifier =
            modifier
                .padding(start = Size.extraTiny, top = Size.extraTiny)
                .clip(RoundedCornerShape(Shapes.coverRadius))
                .clickable { onClick(displayManga.mangaId) }
                .padding(Size.extraTiny)
                .semantics { this.contentDescription = contentDescription }
    ) {
        MangaCover.Book.invoke(
            artwork = displayManga.currentArtwork,
            shouldOutlineCover = shouldOutlineCover,
            dynamicCover = dynamicCover,
        )
        MangaGridTitle(title = title, hasSubtitle = subtitleText.isNotBlank())
        MangaGridSubtitle(subtitleText = subtitleText)
    }
}

@Composable
fun MangaGridTitle(title: String, maxLines: Int = 3, hasSubtitle: Boolean = false) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier.padding(
                top = Size.tiny,
                bottom = if (hasSubtitle) Size.none else Size.tiny,
                start = Size.tiny,
                end = Size.tiny,
            ),
    )
}

@Composable
fun MangaGridSubtitle(subtitleText: String) {
    if (subtitleText.isNotBlank()) {
        Text(
            text = subtitleText,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier.padding(
                    top = Size.none,
                    bottom = Size.tiny,
                    start = Size.tiny,
                    end = Size.tiny,
                ),
        )
    }
}
