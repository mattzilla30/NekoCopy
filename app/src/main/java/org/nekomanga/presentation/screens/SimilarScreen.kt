package org.nekomanga.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.main.states.RefreshState
import org.nekomanga.R
import org.nekomanga.presentation.components.MangaGridWithHeader
import org.nekomanga.presentation.components.MangaListWithHeader
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.bars.DisplayOptionsTopBar
import org.nekomanga.presentation.components.scaffold.ChildScreenScaffold
import org.nekomanga.presentation.screens.similar.SimilarScreenState
import org.nekomanga.presentation.screens.similar.SimilarViewModel

/**
 * SimilarScreen displays manga recommendations similar to a given manga.
 *
 * This screen-level Composable is responsive to [WindowSizeClass]. On expanded screens
 * (tablets/foldables), the layout limits the maximum width of the recommendations list/grid to
 * 800.dp and centers it, preventing content from stretching uncomfortably wide.
 *
 * @param viewModel The ViewModel orchestrating the state of similar manga recommendations.
 * @param windowSizeClass The screen's window size class constraints used to determine adaptive
 *   styling.
 * @param onBackPressed Callback invoked when the user navigates back.
 * @param onNavigateTo Callback invoked to navigate to another screen.
 */
@Composable
fun SimilarScreen(
    viewModel: SimilarViewModel,
    windowSizeClass: WindowSizeClass,
    onBackPressed: () -> Unit,
    onNavigateTo: (NavKey) -> Unit,
) {

    val screenState by viewModel.similarScreenState.collectAsStateWithLifecycle()

    SimilarWrapper(
        similarScreenState = screenState,
        windowSizeClass = windowSizeClass,
        switchDisplayClick = viewModel::switchDisplayMode,
        onBackPress = onBackPressed,
        mangaClick = { id -> onNavigateTo(Screens.Manga(id)) },
        onRefresh = viewModel::refresh,
    )
}

@Composable
private fun SimilarWrapper(
    similarScreenState: SimilarScreenState,
    windowSizeClass: WindowSizeClass,
    switchDisplayClick: () -> Unit,
    onBackPress: () -> Unit,
    mangaClick: (Long) -> Unit,
    onRefresh: () -> Unit,
) {
    val refreshState =
        remember(similarScreenState.isRefreshing) {
            RefreshState(
                enabled = true,
                isRefreshing = similarScreenState.isRefreshing,
                onRefresh = onRefresh,
            )
        }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    ChildScreenScaffold(
        refreshState = refreshState,
        scrollBehavior = scrollBehavior,
        topBar = {
            DisplayOptionsTopBar(
                title = stringResource(R.string.similar),
                incognitoMode = similarScreenState.incognitoMode,
                isList = similarScreenState.isList,
                onNavigationIconClicked = onBackPress,
                scrollBehavior = scrollBehavior,
                switchDisplayClick = switchDisplayClick,
            )
        },
    ) { contentPadding ->
        val isTablet = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            SimilarContent(
                modifier = if (isTablet) Modifier.widthIn(max = 800.dp) else Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                similarScreenState = similarScreenState,
                onRefresh = onRefresh,
                mangaClick = mangaClick,
            )
        }
    }
}

@Composable
private fun SimilarContent(
    similarScreenState: SimilarScreenState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onRefresh: () -> Unit,
    mangaClick: (Long) -> Unit,
) {
    var collapsedGroups by rememberSaveable { mutableStateOf(emptySet<Int>()) }
    val toggleGroupCollapse: (Int) -> Unit = { groupId ->
        collapsedGroups =
            if (groupId in collapsedGroups) collapsedGroups - groupId else collapsedGroups + groupId
    }
    if (similarScreenState.allDisplayManga.isEmpty()) {
        if (similarScreenState.isRefreshing) {
            Box(modifier = modifier.fillMaxSize())
        } else {
            EmptyScreen(
                message = UiText.StringResource(resourceId = R.string.no_results_found),
                modifier = modifier,
                actions =
                    listOf(
                        Action(text = UiText.StringResource(R.string.retry), onClick = onRefresh)
                    ),
            )
        }
    } else {
        if (similarScreenState.isList) {
            MangaListWithHeader(
                groupedManga = similarScreenState.allDisplayManga,
                shouldOutlineCover = similarScreenState.outlineCovers,
                dynamicCover = similarScreenState.dynamicCovers,
                modifier = modifier,
                contentPadding = contentPadding,
                collapsedGroups = collapsedGroups,
                onToggleGroupCollapse = toggleGroupCollapse,
                onClick = mangaClick,
            )
        } else {
            MangaGridWithHeader(
                groupedManga = similarScreenState.allDisplayManga,
                shouldOutlineCover = similarScreenState.outlineCovers,
                dynamicCover = similarScreenState.dynamicCovers,
                modifier = modifier,
                contentPadding = contentPadding,
                collapsedGroups = collapsedGroups,
                onToggleGroupCollapse = toggleGroupCollapse,
                onClick = mangaClick,
            )
        }
    }
}
