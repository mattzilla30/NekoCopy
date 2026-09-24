package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import org.nekomanga.presentation.theme.Size

/**
 * An M3 Expressive icon button wrapped in a tooltip. A long press shows the tooltip, and a press
 * morphs the button from round to square.
 */
@Composable
fun ToolTipButton(
    toolTipLabel: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    icon: ImageVector? = null,
    painter: Painter? = null,
    isEnabled: Boolean = true,
    enabledTint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {},
) {
    require(icon != null || painter != null)

    val haptic = LocalHapticFeedback.current
    val tooltipState = rememberTooltipState()

    LaunchedEffect(tooltipState.isVisible) {
        if (tooltipState.isVisible) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below, Size.tiny),
        state = tooltipState,
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ) {
                Text(
                    modifier = Modifier.padding(Size.tiny),
                    style = MaterialTheme.typography.bodyLarge,
                    text = toolTipLabel,
                )
            }
        },
    ) {
        IconButton(
            modifier = modifier,
            onClick = onClick,
            enabled = isEnabled,
            shapes = IconButtonDefaults.shapes(),
            colors =
                IconButtonDefaults.iconButtonColors(
                    contentColor = enabledTint,
                    disabledContentColor =
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = NekoColors.disabledAlphaLowContrast
                        ),
                ),
        ) {
            when {
                icon != null -> {
                    Icon(
                        imageVector = icon,
                        modifier = iconModifier,
                        contentDescription = toolTipLabel,
                    )
                }
                painter != null -> {
                    Icon(
                        painter = painter,
                        modifier = iconModifier,
                        contentDescription = toolTipLabel,
                    )
                }
            }
        }
    }
}
