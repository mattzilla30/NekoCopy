package org.nekomanga.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.main.states.RefreshState
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.StateFlow
import org.nekomanga.constants.MdConstants
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.scaffold.RootScaffold
import org.nekomanga.presentation.components.sheets.Sheet
import org.nekomanga.presentation.components.sheets.rememberSheetHost
import org.nekomanga.presentation.screens.feed.DownloadScreenActions
import org.nekomanga.presentation.screens.feed.FeedBottomSheet
import org.nekomanga.presentation.screens.feed.FeedScreenActions
import org.nekomanga.presentation.screens.feed.FeedScreenContent
import org.nekomanga.presentation.screens.feed.FeedScreenDialogs
import org.nekomanga.presentation.screens.feed.FeedScreenState
import org.nekomanga.presentation.screens.feed.FeedScreenTopBar
import org.nekomanga.presentation.screens.feed.FeedSettingActions
import org.nekomanga.presentation.screens.feed.FeedViewModel
import org.nekomanga.presentation.screens.feed.HistoryScreenPagingState
import org.nekomanga.presentation.screens.feed.UpdatesScreenPagingState
import org.nekomanga.presentation.theme.Size

/**
 * FeedScreen shows one feed tab: recent updates or reading history, picked by the view model.
 *
 * This screen-level Composable is responsive to [WindowSizeClass]. On expanded screens
 * (tablets/foldables), the layout limits the maximum width of the feed lists and grids to 800.dp
 * and centers it, preventing UI stretching and delivering a premium, polished user experience.
 *
 * @param navigationRail Optional sidebar navigation rail shown on larger screens.
 * @param bottomBar Optional bottom navigation bar shown on compact screens.
 * @param feedViewModel The Viewmodel managing the state and operations for the feeds.
 * @param mainDropdown Main application drop down menu configuration.
 * @param mainDropdownShowing Boolean state representing if the dropdown menu is visible.
 * @param openManga Callback triggered when a manga item is clicked.
 * @param windowSizeClass The screen's window size class constraints used to determine adaptive
 *   layouts.
 */
@Composable
fun FeedScreen(
    navigationRail: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    feedViewModel: FeedViewModel,
    mainDropdown: AppBar.MainDropdown,
    mainDropdownShowing: Boolean,
    openManga: (Long) -> Unit,
    windowSizeClass: WindowSizeClass,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentUpdateMangaForChanges by rememberUpdatedState(feedViewModel::updateMangaForChanges)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentUpdateMangaForChanges()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    FeedWrapper(
        navigationRail = navigationRail,
        bottomBar = bottomBar,
        feedScreenFlow = feedViewModel.feedScreenState,
        updateScreenFlow = feedViewModel.updatesScreenPagingState,
        historyScreenFlow = feedViewModel.historyScreenPagingState,
        windowSizeClass = windowSizeClass,
        mainDropdown = mainDropdown,
        mainDropdownShowing = mainDropdownShowing,
        loadNextPage = feedViewModel::loadNextPage,
        feedSettingActions =
            FeedSettingActions(
                groupHistoryClick = feedViewModel::toggleGroupHistoryType,
                clearHistoryClick = feedViewModel::deleteAllHistoryForAllManga,
                switchUploadsSortOrder = feedViewModel::toggleUploadsSortOrder,
                outlineCoversClick = feedViewModel::toggleOutlineCovers,
                outlineCardsClick = feedViewModel::toggleOutlineCards,
                clearDownloadQueueClick = feedViewModel::clearDownloadQueue,
                toggleDownloadOnUnmetered = feedViewModel::toggleDownloadOnUnmetered,
                toggleGroupUpdateChapters = feedViewModel::togglerGroupUpdateChapters,
                toggleSwipeRefresh = feedViewModel::toggleSwipeRefresh,
            ),
        feedScreenActions =
            FeedScreenActions(
                mangaClick = openManga,
                chapterClick = { mangaId, chapterId ->
                    context.startActivity(ReaderActivity.newIntent(context, mangaId, chapterId))
                },
                chapterSwipe = feedViewModel::toggleChapterRead,
                deleteAllHistoryClick = feedViewModel::deleteAllHistory,
                deleteHistoryClick = feedViewModel::deleteHistory,
                search = feedViewModel::search,
                downloadClick = { chapterItem, feedManga, downloadAction ->
                    if (
                        MdConstants.UnsupportedOfficialGroupList.contains(
                            chapterItem.chapter.scanlator
                        )
                    ) {
                        context.toast("${chapterItem.chapter.scanlator} not supported, try WebView")
                    } else if (chapterItem.chapter.isUnavailable) {
                        context.toast("Chapter is not available")
                    } else {
                        feedViewModel.downloadChapter(chapterItem, feedManga, downloadAction)
                    }
                },
                toggleShowingDownloads = feedViewModel::toggleShowingDownloads,
                updateLibrary = { start ->
                    if (LibraryUpdateJob.isRunning(context) && !start) {
                        LibraryUpdateJob.stop(context)
                    } else if (!LibraryUpdateJob.isRunning(context) && start) {
                        LibraryUpdateJob.startNow(context)
                    }
                },
            ),
        downloadScreenActions =
            DownloadScreenActions(
                downloadSwiped = feedViewModel::removeDownload,
                fabClick = feedViewModel::toggleDownloader,
                moveDownloadClick = feedViewModel::moveDownload,
                moveSeriesClick = feedViewModel::moveDownloadSeries,
                cancelSeriesClick = feedViewModel::cancelDownloadSeries,
                cancelSourceClick = feedViewModel::cancelDownloadSource,
            ),
    )
}

@Composable
private fun FeedWrapper(
    navigationRail: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    feedScreenFlow: StateFlow<FeedScreenState>,
    updateScreenFlow: StateFlow<UpdatesScreenPagingState>,
    historyScreenFlow: StateFlow<HistoryScreenPagingState>,
    windowSizeClass: WindowSizeClass,
    mainDropdown: AppBar.MainDropdown,
    mainDropdownShowing: Boolean,
    loadNextPage: () -> Unit,
    feedSettingActions: FeedSettingActions,
    feedScreenActions: FeedScreenActions,
    downloadScreenActions: DownloadScreenActions,
) {
    val feedScreenState by feedScreenFlow.collectAsState()
    val updatesPagingScreenState by updateScreenFlow.collectAsState()
    val historyPagingScreenState by historyScreenFlow.collectAsState()

    val sheets = rememberSheetHost<Unit>()

    val feedScreenType = feedScreenState.feedScreenType

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearDownloadsDialog by remember { mutableStateOf(false) }

    var downloadScreenVisible by
        remember(feedScreenState.showingDownloads, feedScreenState.downloads.size) {
            mutableStateOf(
                feedScreenState.showingDownloads && feedScreenState.downloads.isNotEmpty()
            )
        }

    Box(modifier = Modifier.fillMaxSize()) {
        sheets.Sheet {
            FeedBottomSheet(
                feedScreenType = feedScreenState.feedScreenType,
                downloadScreenVisible = downloadScreenVisible,
                downloadOnlyOnUnmetered = feedScreenState.downloadOnlyOnUnmetered,
                historyGrouping = historyPagingScreenState.historyGrouping,
                sortByFetched = updatesPagingScreenState.updatesSortedByFetch,
                outlineCovers = feedScreenState.outlineCovers,
                outlineCards = feedScreenState.outlineCards,
                swipeRefreshEnabled = feedScreenState.swipeRefreshEnabled,
                groupUpdateChapters = feedScreenState.groupUpdateChapters,
                groupHistoryClick = { feedHistoryGroup ->
                    feedSettingActions.groupHistoryClick(feedHistoryGroup)
                },
                clearHistoryClick = { showClearHistoryDialog = true },
                clearDownloadsClick = { showClearDownloadsDialog = true },
                sortClick = { feedSettingActions.switchUploadsSortOrder() },
                outlineCoversClick = { feedSettingActions.outlineCoversClick() },
                outlineCardsClick = { feedSettingActions.outlineCardsClick() },
                toggleDownloadOnUnmetered = { feedSettingActions.toggleDownloadOnUnmetered() },
                toggleGroupUpdateChapters = { feedSettingActions.toggleGroupUpdateChapters() },
                toggleSwipeRefresh = { feedSettingActions.toggleSwipeRefresh() },
            )
        }
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

        val refreshState =
            remember(
                feedScreenState.swipeRefreshEnabled,
                feedScreenState.isRefreshing,
                feedScreenActions.updateLibrary,
            ) {
                RefreshState(
                    enabled = feedScreenState.swipeRefreshEnabled,
                    isRefreshing = feedScreenState.isRefreshing,
                    onRefresh = { feedScreenActions.updateLibrary(true) },
                )
            }

        RootScaffold(
            refreshState = refreshState,
            scrollBehavior = scrollBehavior,
            mainSettingsExpanded = mainDropdownShowing,
            navigationRail = navigationRail,
            bottomBar = bottomBar,
            topBar = {
                FeedScreenTopBar(
                    scrollBehavior = scrollBehavior,
                    mainDropDown = mainDropdown,
                    feedScreenState = feedScreenState,
                    feedScreenActions = feedScreenActions,
                    openSheetClick = { sheets.open(Unit) },
                )
            },
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            // Create new padding that ignores the top bar's height
            val contentPadding =
                PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = innerPadding.calculateBottomPadding(),
                    top = 0.dp,
                )

            val recyclerPadding =
                PaddingValues(top = innerPadding.calculateTopPadding(), bottom = Size.small)

            val isTablet = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
            Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                FeedScreenContent(
                    modifier =
                        if (isTablet) Modifier.widthIn(max = 800.dp) else Modifier.fillMaxSize(),
                    downloadScreenVisible = downloadScreenVisible,
                    contentPadding = recyclerPadding,
                    feedScreenState = feedScreenState,
                    historyPagingScreenState = historyPagingScreenState,
                    updatesPagingScreenState = updatesPagingScreenState,
                    downloadScreenActions = downloadScreenActions,
                    feedScreenActions = feedScreenActions,
                    loadNextPage = loadNextPage,
                )
            }
        }

        FeedScreenDialogs(
            showClearHistoryDialog = showClearHistoryDialog,
            showClearDownloadsDialog = showClearDownloadsDialog,
            onClearHistoryDismiss = { showClearHistoryDialog = false },
            onClearHistoryConfirm = { feedSettingActions.clearHistoryClick() },
            onClearDownloadsDismiss = { showClearDownloadsDialog = false },
            onClearDownloadsConfirm = {
                feedSettingActions.clearDownloadQueueClick()
                sheets.close()
            },
        )
    }
}
