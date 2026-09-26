package org.nekomanga.presentation.screens.feed

import androidx.compose.material.icons.Icons
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
    incognitoMode: Boolean,
    onSearch: (String?) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    mainDropDown: AppBar.MainDropdown,
    openSheetClick: () -> Unit,
) {
    val (color, _, _) = getTopAppBarColor(false, false)

    SearchOutlineTopAppBar(
        onSearch = onSearch,
        searchPlaceHolder = stringResource(R.string.search_history),
        color = color,
        incognitoMode = incognitoMode,
        actions = {
            AppBarActions(
                actions =
                    listOf(
                        AppBar.Action(
                            title = UiText.StringResource(R.string.settings),
                            icon = Icons.Outlined.Tune,
                            onClick = openSheetClick,
                        ),
                        mainDropDown,
                    )
            )
        },
        scrollBehavior = scrollBehavior,
    )
}
