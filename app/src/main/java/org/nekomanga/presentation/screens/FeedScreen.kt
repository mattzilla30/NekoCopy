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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.scaffold.RootScaffold
import org.nekomanga.presentation.components.sheets.Sheet
import org.nekomanga.presentation.components.sheets.rememberSheetHost
import org.nekomanga.presentation.screens.feed.ClearHistoryDialog
import org.nekomanga.presentation.screens.feed.FeedBottomSheet
import org.nekomanga.presentation.screens.feed.FeedScreenActions
import org.nekomanga.presentation.screens.feed.FeedScreenContent
import org.nekomanga.presentation.screens.feed.FeedScreenTopBar
import org.nekomanga.presentation.screens.feed.FeedSettingActions
import org.nekomanga.presentation.screens.feed.FeedViewModel
import org.nekomanga.presentation.theme.Size

/**
 * The History tab: the chapters read most recently. On expanded screens the list is limited to 800
 * dp and centered.
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

    val feedScreenState by feedViewModel.feedScreenState.collectAsStateWithLifecycle()
    val historyPagingScreenState by
        feedViewModel.historyScreenPagingState.collectAsStateWithLifecycle()

    val feedSettingActions =
        FeedSettingActions(
            groupHistoryClick = feedViewModel::toggleGroupHistoryType,
            clearHistoryClick = feedViewModel::deleteAllHistoryForAllManga,
            outlineCoversClick = feedViewModel::toggleOutlineCovers,
            outlineCardsClick = feedViewModel::toggleOutlineCards,
        )
    val feedScreenActions =
        FeedScreenActions(
            mangaClick = openManga,
            chapterClick = { mangaId, chapterId ->
                context.startActivity(ReaderActivity.newIntent(context, mangaId, chapterId))
            },
            deleteAllHistoryClick = feedViewModel::deleteAllHistory,
            deleteHistoryClick = feedViewModel::deleteHistory,
            search = feedViewModel::search,
        )

    val sheets = rememberSheetHost<Unit>()
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    sheets.Sheet {
        FeedBottomSheet(
            historyGrouping = historyPagingScreenState.historyGrouping,
            outlineCovers = feedScreenState.outlineCovers,
            outlineCards = feedScreenState.outlineCards,
            groupHistoryClick = feedSettingActions.groupHistoryClick,
            clearHistoryClick = { showClearHistoryDialog = true },
            outlineCoversClick = feedSettingActions.outlineCoversClick,
            outlineCardsClick = feedSettingActions.outlineCardsClick,
        )
    }

    if (showClearHistoryDialog) {
        ClearHistoryDialog(
            onDismiss = { showClearHistoryDialog = false },
            onConfirm = feedSettingActions.clearHistoryClick,
        )
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    RootScaffold(
        scrollBehavior = scrollBehavior,
        mainSettingsExpanded = mainDropdownShowing,
        navigationRail = navigationRail,
        bottomBar = bottomBar,
        topBar = {
            FeedScreenTopBar(
                scrollBehavior = scrollBehavior,
                mainDropDown = mainDropdown,
                incognitoMode = feedScreenState.incognitoMode,
                onSearch = feedScreenActions.search,
                openSheetClick = { sheets.open(Unit) },
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        // Leave out the top bar's height: the list scrolls under it.
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
                modifier = if (isTablet) Modifier.widthIn(max = 800.dp) else Modifier.fillMaxSize(),
                contentPadding = recyclerPadding,
                feedScreenState = feedScreenState,
                historyPagingScreenState = historyPagingScreenState,
                feedScreenActions = feedScreenActions,
                loadNextPage = feedViewModel::loadNextPage,
            )
        }
    }
}
