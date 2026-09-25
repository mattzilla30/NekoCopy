package org.nekomanga.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.theme.Size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRefresh(
    enabled: Boolean = true,
    isRefreshing: Boolean,
    onRefresh: (() -> Unit)?,
    trackColor: Color = MaterialTheme.colorScheme.secondary,
    blurBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()

    if (enabled && onRefresh != null) {
        PullToRefreshBox(
            modifier =
                Modifier.conditional(blurBackground) {
                    this.blur(Size.medium).clickable(enabled = false) {}
                },
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = state,
            indicator = {
                KittyPullIndicator(
                    state = state,
                    isRefreshing = isRefreshing,
                    color = trackColor,
                    modifier =
                        Modifier.align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = Size.appBarHeight),
                )
            },
        ) {
            content()
        }
    } else {
        Box(
            modifier =
                Modifier.conditional(blurBackground) {
                    this.blur(Size.medium).clickable(enabled = false) {}
                }
        ) {
            content()
        }
    }
}

/**
 * Pull-to-refresh with the refresh arrows. While pulling, the arrows turn with the pull. While
 * refreshing, they spin. The indicator slides out from under the top app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KittyPullIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    PullToRefreshDefaults.IndicatorBox(
        state = state,
        isRefreshing = isRefreshing,
        modifier = modifier,
        containerColor = PullToRefreshDefaults.loadingIndicatorContainerColor,
        elevation = PullToRefreshDefaults.LoadingIndicatorElevation,
    ) {
        if (isRefreshing) {
            KittyLoadingIndicator(color = color, size = Size.large)
        } else {
            RefreshArrows(
                rotation = state.distanceFraction.coerceIn(0f, 1f) * 270f,
                color = color,
                size = Size.large,
            )
        }
    }
}
