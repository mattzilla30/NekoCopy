package org.nekomanga.presentation.screens.library

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.nekomanga.domain.manga.LibraryMangaItem
import org.nekomanga.presentation.components.MangaGridItem
import org.nekomanga.presentation.components.MangaRow
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.theme.Size

// Library manga as a grid cell or a list row, shared by the paged and the scrolling layouts.
// Tapping opens the manga, or toggles its selection while a selection is active.

@Composable
fun LibraryGridItem(
    libraryItem: LibraryMangaItem,
    displayOptions: LibraryItemDisplayOptions,
    libraryScreenActions: LibraryScreenActions,
    selectedIds: List<Long>,
    isComfortable: Boolean,
) {
    MangaGridItem(
        displayManga = libraryItem.displayManga,
        showUnreadBadge = displayOptions.showUnreadBadges,
        unreadCount = libraryItem.unreadCount,
        showDownloadBadge = displayOptions.showDownloadBadges,
        downloadCount = libraryItem.downloadCount,
        shouldOutlineCover = displayOptions.outlineCovers,
        dynamicCover = displayOptions.dynamicCovers,
        isComfortable = isComfortable,
        isSelected = selectedIds.contains(libraryItem.displayManga.mangaId),
        showStartReadingButton =
            displayOptions.showStartReadingButton && libraryItem.unreadCount > 0,
        onStartReadingClick = {
            libraryScreenActions.mangaStartReadingClick(libraryItem.displayManga.mangaId)
        },
        onClick = { _ ->
            if (selectedIds.isNotEmpty()) {
                libraryScreenActions.mangaLongClick(libraryItem)
            } else {
                libraryScreenActions.mangaClick(libraryItem.displayManga.mangaId)
            }
        },
        onLongClick = { _ -> libraryScreenActions.mangaLongClick(libraryItem) },
    )
}

@Composable
fun LibraryListItem(
    modifier: Modifier = Modifier,
    index: Int,
    totalSize: Int,
    selectedIds: List<Long>,
    displayOptions: LibraryItemDisplayOptions,
    libraryItem: LibraryMangaItem,
    libraryScreenActions: LibraryScreenActions,
) {
    val listCardType = ListCardType.forPosition(index, totalSize)
    ExpressiveListCard(
        modifier = modifier.padding(horizontal = Size.small),
        listCardType = listCardType,
    ) {
        MangaRow(
            modifier =
                Modifier.fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (selectedIds.isNotEmpty()) {
                                libraryScreenActions.mangaLongClick(libraryItem)
                            } else {
                                libraryScreenActions.mangaClick(libraryItem.displayManga.mangaId)
                            }
                        },
                        onLongClick = { libraryScreenActions.mangaLongClick(libraryItem) },
                    ),
            displayManga = libraryItem.displayManga,
            isSelected = selectedIds.contains(libraryItem.displayManga.mangaId),
            showUnreadBadge = displayOptions.showUnreadBadges,
            showStartReadingButton =
                displayOptions.showStartReadingButton && libraryItem.unreadCount > 0,
            onStartReadingClick = {
                libraryScreenActions.mangaStartReadingClick(libraryItem.displayManga.mangaId)
            },
            unreadCount = libraryItem.unreadCount,
            showDownloadBadge = displayOptions.showDownloadBadges,
            downloadCount = libraryItem.downloadCount,
            shouldOutlineCover = displayOptions.outlineCovers,
            dynamicCover = displayOptions.dynamicCovers,
        )
    }
}
