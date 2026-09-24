package org.nekomanga.presentation.components.listcard

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.extensions.surfaceColorAtElevationCustomColor
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

enum class ListCardType {
    Top,
    Center,
    Bottom,
    Single,
}

@Composable
fun ExpressiveListCard(
    modifier: Modifier = Modifier,
    listCardType: ListCardType,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    content: @Composable () -> Unit,
) {
    val defaultColors = CardDefaults.elevatedCardColors()
    val defaultThemeColorState = defaultThemeColorState()
    val elevatedColor =
        MaterialTheme.colorScheme.surfaceColorAtElevationCustomColor(
            themeColorState.containerColor,
            Size.small,
        )
    val colors =
        remember(themeColorState) {
            if (themeColorState == defaultThemeColorState) {
                defaultColors
            } else {
                defaultColors.copy(containerColor = elevatedColor)
            }
        }

    val large = MaterialTheme.shapes.large
    val shape =
        remember(listCardType, large) {
            val inner = CornerSize(Shapes.segmentRadius)
            when (listCardType) {
                ListCardType.Top -> large.copy(bottomStart = inner, bottomEnd = inner)
                ListCardType.Center -> RoundedCornerShape(inner)
                ListCardType.Single -> large
                ListCardType.Bottom -> large.copy(topStart = inner, topEnd = inner)
            }
        }

    ElevatedCard(modifier = modifier, shape = shape, colors = colors) { content() }
}
