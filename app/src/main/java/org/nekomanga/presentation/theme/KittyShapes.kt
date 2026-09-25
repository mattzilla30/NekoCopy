package org.nekomanga.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Shapes as MaterialShapeScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon

/**
 * Kitty's shape language. Corners run a step rounder than the M3 defaults, and the kitty head joins
 * the Material shape library as the app's signature shape.
 */
object KittyShapes {

    /** The corner scale handed to MaterialExpressiveTheme. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val scale =
        MaterialShapeScale(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(26.dp),
            extraLarge = RoundedCornerShape(34.dp),
            largeIncreased = RoundedCornerShape(30.dp),
            extraLargeIncreased = RoundedCornerShape(40.dp),
            extraExtraLarge = RoundedCornerShape(52.dp),
        )

    private val earTip = CornerRounding(radius = 3f, smoothing = 0.4f)
    private val earNotch = CornerRounding(radius = 3f)
    private val crown = CornerRounding(radius = 8f, smoothing = 0.6f)
    private val jaw = CornerRounding(radius = 9f, smoothing = 0.7f)
    private val cheek = CornerRounding(radius = 14f, smoothing = 0.8f)
    private val chin = CornerRounding(radius = 16f, smoothing = 1f)

    /**
     * A kitty head with two ears, traced from the launcher icon on a 108 unit grid and normalized
     * to the unit square. Vertices run clockwise from the left ear tip.
     */
    val Head: RoundedPolygon = run {
        val corners =
            listOf(
                Offset(35f, 26f) to earTip, // left ear tip
                Offset(49f, 38f) to earNotch, // left ear notch
                Offset(54f, 37f) to crown,
                Offset(59f, 38f) to earNotch, // right ear notch
                Offset(73f, 26f) to earTip, // right ear tip
                Offset(77f, 50f) to jaw, // right jaw
                Offset(79f, 62f) to cheek, // right cheek
                Offset(68f, 76f) to chin, // right chin
                Offset(54f, 78f) to chin,
                Offset(40f, 76f) to chin, // left chin
                Offset(29f, 62f) to cheek, // left cheek
                Offset(31f, 50f) to jaw, // left jaw
            )
        RoundedPolygon(
                vertices =
                    corners.flatMap { (point, _) -> listOf(point.x, point.y) }.toFloatArray(),
                perVertexRounding = corners.map { (_, rounding) -> rounding },
            )
            .normalized()
    }

    /** Shapes the indeterminate loading indicator morphs through. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val indeterminateLoading: List<RoundedPolygon> =
        listOf(Head, MaterialShapes.Cookie9Sided, MaterialShapes.Puffy, MaterialShapes.SoftBurst)

    /** A determinate indicator grows from a round cookie into the kitty head as work finishes. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val determinateLoading: List<RoundedPolygon> = listOf(MaterialShapes.Cookie9Sided, Head)
}
