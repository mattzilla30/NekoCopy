package org.nekomanga.presentation.components.bars

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.nekomanga.R
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.functions.getTopAppBarColor

/** A titled top bar with a back arrow and a button that opens the display options. */
@Composable
fun DisplayOptionsTopBar(
    title: String,
    incognitoMode: Boolean,
    onNavigationIconClicked: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    onSettingClick: () -> Unit,
) {
    val (color, _, _) = getTopAppBarColor(true, false)
    TitleTopAppBar(
        color = color,
        title = title,
        navigationIcon = Icons.AutoMirrored.Default.ArrowBack,
        onNavigationIconClicked = onNavigationIconClicked,
        navigationIconLabel = stringResource(R.string.back),
        incognitoMode = incognitoMode,
        scrollBehavior = scrollBehavior,
        actions = {
            AppBarActions(
                actions =
                    listOf(
                        AppBar.Action(
                            title = UiText.StringResource(R.string.settings),
                            icon = Icons.Outlined.Tune,
                            onClick = onSettingClick,
                        )
                    )
            )
        },
    )
}
