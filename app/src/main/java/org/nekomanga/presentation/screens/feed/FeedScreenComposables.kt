package org.nekomanga.presentation.screens.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.nekomanga.R
import org.nekomanga.presentation.components.KittyContainedLoadingIndicator
import org.nekomanga.presentation.components.dialog.ConfirmationDialog
import org.nekomanga.presentation.screens.feed.history.FeedHistoryPage

@Composable
fun ClearHistoryDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    ConfirmationDialog(
        title = stringResource(R.string.clear_history_confirmation_1),
        body = stringResource(R.string.clear_history_confirmation_2),
        confirmButton = stringResource(id = R.string.clear),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
fun FeedScreenContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    feedScreenState: FeedScreenState,
    historyPagingScreenState: HistoryScreenPagingState,
    feedScreenActions: FeedScreenActions,
    loadNextPage: () -> Unit,
) {
    Box(modifier = modifier) {
        if (historyPagingScreenState.pageLoading && historyPagingScreenState.offset == 0) {
            KittyContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Search results replace the history while a search is showing.
        val searching = historyPagingScreenState.searchHistoryFeedMangaList.isNotEmpty()
        FeedHistoryPage(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            feedHistoryMangaList =
                if (searching) historyPagingScreenState.searchHistoryFeedMangaList
                else historyPagingScreenState.historyFeedMangaList,
            outlineCovers = feedScreenState.outlineCovers,
            dynamicCovers = feedScreenState.dynamicCovers,
            outlineCards = feedScreenState.outlineCards,
            hasMoreResults = !searching && historyPagingScreenState.hasMoreResults,
            loadingResults = historyPagingScreenState.pageLoading,
            feedScreenActions = feedScreenActions,
            loadNextPage = loadNextPage,
            historyGrouping = historyPagingScreenState.historyGrouping,
        )
    }
}
