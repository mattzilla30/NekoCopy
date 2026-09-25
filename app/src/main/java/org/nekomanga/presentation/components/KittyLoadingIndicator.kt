package org.nekomanga.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Two bold arrows chasing each other around a circle. */
val RefreshArrowsIcon: ImageVector by
    lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
                name = "RefreshArrows",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
            .path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Upper arc, left to upper right, ending in an arrowhead.
                moveTo(4f, 12f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18.13f, 6.86f)
                moveTo(18.13f, 2.4f)
                verticalLineTo(6.86f)
                horizontalLineTo(13.7f)
                // Lower arc, right to lower left, ending in an arrowhead.
                moveTo(20f, 12f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5.87f, 17.14f)
                moveTo(5.87f, 21.6f)
                verticalLineTo(17.14f)
                horizontalLineTo(10.3f)
            }
            .build()
    }

private const val SPIN_MILLIS = 900

/** The refresh arrows, spinning while work runs. */
@Composable
fun KittyLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 48.dp,
) {
    val rotation by
        rememberInfiniteTransition(label = "loadingSpin")
            .animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec =
                    infiniteRepeatable(
                        tween(SPIN_MILLIS, easing = LinearEasing),
                        RepeatMode.Restart,
                    ),
                label = "loadingRotation",
            )
    RefreshArrows(rotation = rotation, color = color, size = size, modifier = modifier)
}

/** The spinning refresh arrows on a round container, for loading over content. */
@Composable
fun KittyContainedLoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        KittyLoadingIndicator(color = MaterialTheme.colorScheme.onPrimaryContainer, size = 38.dp)
    }
}

/** The refresh arrows at a fixed [rotation] in degrees, for progress driven by a gesture. */
@Composable
fun RefreshArrows(rotation: Float, color: Color, size: Dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector = RefreshArrowsIcon,
        contentDescription = null,
        tint = color,
        modifier = modifier.size(size).rotate(rotation),
    )
}
