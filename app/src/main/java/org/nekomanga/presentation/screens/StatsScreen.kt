package org.nekomanga.presentation.screens

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.nekomanga.R
import org.nekomanga.presentation.components.ChartColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.scaffold.ChildScreenScaffold
import org.nekomanga.presentation.screens.stats.DetailedStats
import org.nekomanga.presentation.screens.stats.SimpleStats
import org.nekomanga.presentation.screens.stats.StatsConstants
import org.nekomanga.presentation.screens.stats.StatsConstants.ScreenState.Detailed
import org.nekomanga.presentation.screens.stats.StatsConstants.ScreenState.Loading
import org.nekomanga.presentation.screens.stats.StatsTopBar
import org.nekomanga.presentation.screens.stats.StatsViewModel

@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    onBackPressed: () -> Unit,
    windowSizeClass: WindowSizeClass,
) {

    val statsState by statsViewModel.simpleState.collectAsStateWithLifecycle()
    val detailedState by statsViewModel.detailState.collectAsStateWithLifecycle()

    StatsWrapper(
        statsState = statsState,
        detailedState = detailedState,
        onBackPressed = onBackPressed,
        onSwitchClick = statsViewModel::switchState,
        windowSizeClass = windowSizeClass,
    )
}

@Composable
fun StatsWrapper(
    statsState: StatsConstants.SimpleState,
    detailedState: StatsConstants.DetailedState,
    onBackPressed: () -> Unit,
    onSwitchClick: () -> Unit,
    windowSizeClass: WindowSizeClass,
) {
    val colors = remember {
        listOf(
            ChartColors.one,
            ChartColors.two,
            ChartColors.three,
            ChartColors.four,
            ChartColors.five,
            ChartColors.six,
            ChartColors.seven,
            ChartColors.eight,
            ChartColors.nine,
            ChartColors.ten,
            ChartColors.eleven,
            ChartColors.twelve,
        )
    }

    val isSimple =
        rememberSaveable(statsState.screenState) {
            statsState.screenState is StatsConstants.ScreenState.Simple
        }
    val scrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState())

    ChildScreenScaffold(
        scrollBehavior = scrollBehavior,
        topBar = {
            StatsTopBar(
                statsState = statsState,
                onNavigationIconClicked = onBackPressed,
                scrollBehavior = scrollBehavior,
                onSwitchClick = onSwitchClick,
            )
        },
    ) { contentPadding ->
        if (
            statsState.screenState is Loading ||
                (statsState.screenState is Detailed && detailedState.isLoading)
        ) {
            LoadingScreen()
        } else if (statsState.screenState is StatsConstants.ScreenState.NoResults) {
            EmptyScreen(
                message = UiText.StringResource(resourceId = R.string.unable_to_generate_stats),
                contentPadding = contentPadding,
            )
        } else {
            if (isSimple) {
                SimpleStats(
                    statsState = statsState,
                    contentPadding = contentPadding,
                    windowSizeClass = windowSizeClass,
                )
            } else {
                DetailedStats(
                    detailedStats = detailedState,
                    colors = colors,
                    contentPadding = contentPadding,
                    windowSizeClass = windowSizeClass,
                )
            }
        }
    }
}
