package org.nekomanga.presentation.components

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.RippleConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object NekoColors {
    const val highAlphaHighContrast = 1f
    const val highAlphaLowContrast = .87f
    const val mediumAlphaHighContrast = .74f
    const val mediumAlphaLowContrast = .6f
    const val halfAlpha = .5f
    const val disabledAlphaHighContrast = .38f
    const val disabledAlphaLowContrast = .38f
    const val veryLowContrast = .1f
}

object Outline {
    val color = Color(0XFF9D9D9D)
    val thickness = .75.dp
}

fun nekoRippleConfiguration(color: Color) = RippleConfiguration(color)

fun dynamicTextSelectionColor(color: Color) =
    TextSelectionColors(
        handleColor = color,
        backgroundColor = color.copy(alpha = NekoColors.disabledAlphaHighContrast),
    )
