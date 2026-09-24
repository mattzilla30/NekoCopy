package org.nekomanga.presentation.theme

import androidx.compose.ui.unit.dp

/** Corner radii that follow the M3 Expressive shape scale. */
object Shapes {
    /** Expressive "large" corner, used on covers and cards. */
    val coverRadius = 16.dp
    /** Expressive "extra large" corner, used on sheets and dialogs. */
    val sheetRadius = 28.dp
    /** Expressive "extra small" corner, used on inner segments of grouped lists. */
    val segmentRadius = 4.dp
}

object Size {
    val none = 0.dp
    val extraExtraTiny = 1.dp
    val extraTiny = 2.dp
    val tiny = 4.dp
    val small = 8.dp
    val smedium = 12.dp
    val medium = 16.dp
    val mediumLarge = 20.dp
    val large = 24.dp
    val largePlus = 28.dp
    val extraLarge = 32.dp
    val huge = 48.dp
    val extraHuge = 56.dp
    val extraExtraHuge = 64.dp

    val appBarHeight = 64.dp
    val navBarSize = 68.dp
    val squareCoverMedium = 72.dp
    val squareCoverLarge = 96.dp
}
