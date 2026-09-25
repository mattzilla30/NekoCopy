package org.nekomanga.presentation.screens.feed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.nekomanga.R
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.bars.SearchOutlineTopAppBar
import org.nekomanga.presentation.functions.getTopAppBarColor

@Composable
fun FeedScreenTopBar(
    feedScreenState: FeedScreenState,
    feedScreenActions: FeedScreenActions,
    scrollBehavior: TopAppBarScrollBehavior,
    mainDropDown: AppBar.MainDropdown,
    openSheetClick: () -> Unit,
) {
    val (color, _, _) = getTopAppBarColor(false, false)

    val searchHint =
        when (feedScreenState.feedScreenType) {
            FeedScreenType.History -> stringResource(R.string.search_history)
            FeedScreenType.Updates -> stringResource(R.string.search_updates)
        }

    // The download queue lives on the Updates tab, shown while anything is queued.
    val showDownloadsAction =
        feedScreenState.feedScreenType == FeedScreenType.Updates &&
            (feedScreenState.downloads.isNotEmpty() || feedScreenState.showingDownloads)

    SearchOutlineTopAppBar(
        onSearch = feedScreenActions.search,
        searchPlaceHolder = searchHint,
        color = color,
        incognitoMode = feedScreenState.incognitoMode,
        actions = {
            AppBarActions(
                actions =
                    buildList {
                        if (showDownloadsAction) {
                            add(
                                AppBar.Action(
                                    title = UiText.StringResource(R.string.downloads),
                                    icon =
                                        if (feedScreenState.showingDownloads) {
                                            Icons.Filled.Downloading
                                        } else {
                                            Icons.Outlined.Downloading
                                        },
                                    onClick = feedScreenActions.toggleShowingDownloads,
                                )
                            )
                        }
                        add(
                            AppBar.Action(
                                title = UiText.StringResource(R.string.settings),
                                icon = Icons.Outlined.Tune,
                                onClick = openSheetClick,
                            )
                        )
                        add(mainDropDown)
                    }
            )
        },
        scrollBehavior = scrollBehavior,
    )
}
