package org.nekomanga.presentation.components

import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.nekomanga.presentation.theme.KittyShapes

/** The M3 Expressive loading indicator, morphing through Kitty's signature shapes. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KittyLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = LoadingIndicatorDefaults.indicatorColor,
) {
    LoadingIndicator(
        modifier = modifier,
        color = color,
        polygons = KittyShapes.indeterminateLoading,
    )
}

/** The contained M3 Expressive loading indicator, morphing through Kitty's signature shapes. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KittyContainedLoadingIndicator(modifier: Modifier = Modifier) {
    ContainedLoadingIndicator(modifier = modifier, polygons = KittyShapes.indeterminateLoading)
}

/** A determinate contained indicator that grows into the kitty head as [progress] reaches 1. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KittyContainedLoadingIndicator(progress: () -> Float, modifier: Modifier = Modifier) {
    ContainedLoadingIndicator(
        progress = progress,
        modifier = modifier,
        polygons = KittyShapes.determinateLoading,
    )
}
