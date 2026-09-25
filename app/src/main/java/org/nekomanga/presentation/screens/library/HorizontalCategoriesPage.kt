package org.nekomanga.presentation.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import jp.wasabeef.gap.Gap
import kotlinx.coroutines.launch
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.functions.numberOfColumns
import org.nekomanga.presentation.theme.Size

@Composable
fun HorizontalCategoriesPage(
    contentPadding: PaddingValues,
    selectionMode: Boolean,
    libraryScreenState: LibraryScreenState,
    libraryScreenActions: LibraryScreenActions,
    libraryCategoryActions: LibraryCategoryActions,
    categorySortClick: (CategoryItem) -> Unit,
) {

    val pageCount = libraryScreenState.items.size

    val pagerState =
        rememberPagerState(
            initialPage = libraryScreenState.pagerIndex,
            initialPageOffsetFraction = 0f,
        ) {
            pageCount
        }

    LaunchedEffect(pagerState.currentPage) {
        libraryScreenActions.pagerIndexChanged(pagerState.currentPage)
    }

    LaunchedEffect(libraryScreenState.currentGroupBy) {
        if (pagerState.currentPage != 0) {
            pagerState.scrollToPage(0, 0f) // Snap to page 0
        }
    }

    LaunchedEffect(libraryScreenState.pagerIndex) {
        if (
            pagerState.currentPage != libraryScreenState.pagerIndex &&
                !pagerState.isScrollInProgress
        ) {
            pagerState.scrollToPage(libraryScreenState.pagerIndex)
        }
    }

    val isValidState = pagerState.currentPage < pageCount

    val scope = rememberCoroutineScope()
    val columns = numberOfColumns(rawValue = libraryScreenState.rawColumnCount)
    val selectedIds =
        remember(libraryScreenState.selectedItems) {
            libraryScreenState.selectedItems.map { it.displayManga.mangaId }
        }

    val indicatorColor =
        if (libraryScreenState.useVividColorHeaders) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val displayOptions =
        remember(
            libraryScreenState.showUnreadBadges,
            libraryScreenState.showDownloadBadges,
            libraryScreenState.showStartReadingButton,
            libraryScreenState.outlineCovers,
            libraryScreenState.dynamicCovers,
        ) {
            LibraryItemDisplayOptions(
                showUnreadBadges = libraryScreenState.showUnreadBadges,
                showDownloadBadges = libraryScreenState.showDownloadBadges,
                showStartReadingButton = libraryScreenState.showStartReadingButton,
                outlineCovers = libraryScreenState.outlineCovers,
                dynamicCovers = libraryScreenState.dynamicCovers,
            )
        }

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding).padding(top = Size.tiny)) {
        if (isValidState) {
            SecondaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = Size.small,
                divider = {},
            ) {
                libraryScreenState.items.forEachIndexed { index, item ->
                    val isSelected = pagerState.currentPage == index

                    Tab(
                        text = {
                            Text(
                                item.categoryItem.name,
                                color =
                                    if (isSelected) indicatorColor
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        selected = isSelected,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    )
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) {
                page ->
                val item = libraryScreenState.items[page]

                val allSelected by
                    remember(selectionMode, selectedIds) {
                        mutableStateOf(
                            item.libraryItems.isNotEmpty() &&
                                item.libraryItems.all { libraryItem ->
                                    libraryItem.displayManga.mangaId in selectedIds
                                }
                        )
                    }
                when (libraryScreenState.libraryDisplayMode) {
                    is LibraryDisplayMode.ComfortableGrid,
                    is LibraryDisplayMode.CompactGrid -> {
                        val gridState =
                            rememberLazyGridState(
                                initialFirstVisibleItemIndex =
                                    libraryScreenState.scrollPositions[page] ?: 0
                            )

                        DisposableEffect(libraryScreenActions, page) {
                            onDispose {
                                libraryScreenActions.scrollPositionChanged(
                                    page,
                                    gridState.firstVisibleItemIndex,
                                )
                            }
                        }
                        Column {
                            HorizontalCategoryHeader(
                                selectionMode = selectionMode,
                                allSelected = allSelected,
                                indicatorColor = indicatorColor,
                                totalItems = item.libraryItems.size,
                                onSelectAll = {
                                    libraryScreenActions.selectAllLibraryMangaItems(
                                        item.libraryItems
                                    )
                                },
                                categoryItem = item.categoryItem,
                                categorySortClick = categorySortClick,
                                libraryCategoryActions = libraryCategoryActions,
                                isRefreshing = item.isRefreshing,
                            )
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(columns),
                                modifier = Modifier.fillMaxSize().padding(horizontal = Size.small),
                                horizontalArrangement = Arrangement.spacedBy(Size.small),
                                contentPadding =
                                    PaddingValues(
                                        bottom = contentPadding.calculateBottomPadding(),
                                        top = Size.small,
                                    ),
                            ) {
                                itemsIndexed(
                                    items = item.libraryItems,
                                    key = { _, libraryItem ->
                                        "${item.categoryItem.id}-${libraryItem.displayManga.mangaId}"
                                    },
                                ) { index, libraryItem ->
                                    LibraryGridItem(
                                        libraryItem = libraryItem,
                                        displayOptions = displayOptions,
                                        libraryScreenActions = libraryScreenActions,
                                        selectedIds = selectedIds,
                                        isComfortable =
                                            libraryScreenState.libraryDisplayMode
                                                is LibraryDisplayMode.ComfortableGrid,
                                    )
                                }
                            }
                        }
                    }

                    is LibraryDisplayMode.List -> {
                        val listState =
                            rememberLazyListState(
                                initialFirstVisibleItemIndex =
                                    libraryScreenState.scrollPositions[page] ?: 0
                            )
                        DisposableEffect(libraryScreenActions, page) {
                            onDispose {
                                libraryScreenActions.scrollPositionChanged(
                                    page,
                                    listState.firstVisibleItemIndex,
                                )
                            }
                        }
                        Column(modifier = Modifier.fillMaxSize()) {
                            HorizontalCategoryHeader(
                                selectionMode = selectionMode,
                                allSelected = allSelected,
                                indicatorColor = MaterialTheme.colorScheme.tertiary,
                                totalItems = null,
                                onSelectAll = {
                                    libraryScreenActions.selectAllLibraryMangaItems(
                                        item.libraryItems
                                    )
                                },
                                categoryItem = item.categoryItem,
                                categorySortClick = categorySortClick,
                                libraryCategoryActions = libraryCategoryActions,
                                isRefreshing = item.isRefreshing,
                            )
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding =
                                    PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                            ) {
                                itemsIndexed(
                                    items = item.libraryItems,
                                    key = { _, libraryItem ->
                                        "${item.categoryItem.id}-${libraryItem.displayManga.mangaId}"
                                    },
                                ) { index, libraryItem ->
                                    LibraryListItem(
                                        index = index,
                                        totalSize = item.libraryItems.size,
                                        selectedIds = selectedIds,
                                        displayOptions = displayOptions,
                                        libraryItem = libraryItem,
                                        libraryScreenActions = libraryScreenActions,
                                    )
                                    Gap(Size.tiny)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalCategoryHeader(
    selectionMode: Boolean,
    allSelected: Boolean,
    indicatorColor: Color,
    totalItems: Int?,
    onSelectAll: () -> Unit,
    categoryItem: CategoryItem,
    categorySortClick: (CategoryItem) -> Unit,
    libraryCategoryActions: LibraryCategoryActions,
    isRefreshing: Boolean,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth().clickable(enabled = selectionMode, onClick = onSelectAll),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (totalItems == null) {
            Gap(Size.medium)
            AnimatedVisibility(selectionMode) {
                Icon(
                    imageVector =
                        if (allSelected) Icons.Default.CheckCircleOutline
                        else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = indicatorColor,
                )
            }
        } else {
            AnimatedVisibility(selectionMode) {
                Row {
                    Gap(Size.medium)
                    Icon(
                        imageVector =
                            if (allSelected) Icons.Default.CheckCircleOutline
                            else Icons.Outlined.Circle,
                        contentDescription = null,
                        tint = indicatorColor,
                    )
                }
            }
            if (totalItems > 0) {
                Gap(Size.medium)
                Text(
                    text = stringResource(org.nekomanga.R.string.total_items, totalItems),
                    color = indicatorColor,
                    style = MaterialTheme.typography.labelLarge,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        CategorySortButtons(
            textColor =
                if (totalItems != null) indicatorColor else MaterialTheme.colorScheme.primary,
            enabled = true,
            categorySortClick = { categorySortClick(categoryItem) },
            sortString = stringResource(categoryItem.sortOrder.stringRes(categoryItem.isDynamic)),
            isAscending = categoryItem.isAscending,
            categoryIsRefreshing = isRefreshing,
            ascendingClick = { libraryCategoryActions.categoryAscendingClick(categoryItem) },
            categoryRefreshClick = { libraryCategoryActions.categoryRefreshClick(categoryItem) },
        )
    }
}
