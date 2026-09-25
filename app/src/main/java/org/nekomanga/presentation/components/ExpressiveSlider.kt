package org.nekomanga.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier

/**
 * A value-driven slider built on the state-based M3 Expressive [Slider]. Callers keep owning the
 * value, and the slider state follows it whenever the user is not dragging.
 */
@Composable
fun ExpressiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val state = remember(steps, valueRange) { SliderState(value, steps, valueRange) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    SideEffect {
        if (!state.isDragging && state.value != value) {
            state.value = value
        }
    }

    Slider(
        state = state,
        modifier = modifier,
        enabled = enabled,
        onValueChange = { newValue ->
            state.value = newValue
            currentOnValueChange(newValue)
        },
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
    )
}
