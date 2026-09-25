package org.nekomanga.presentation.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import org.nekomanga.R

/**
 * Kitty's type pairing. Fredoka, a round and bouncy face, sets display, headline and title text.
 * Nunito, a rounded sans, sets body and label text. Both ship as variable fonts, so every weight
 * comes from one file each.
 */
object Typefaces {
    private val defaultTypography = Typography()

    private val fredokaWeights =
        listOf(
            FontWeight.Light,
            FontWeight.Normal,
            FontWeight.Medium,
            FontWeight.SemiBold,
            FontWeight.Bold,
        )

    private val nunitoWeights =
        listOf(
            FontWeight.ExtraLight,
            FontWeight.Light,
            FontWeight.Normal,
            FontWeight.Medium,
            FontWeight.SemiBold,
            FontWeight.Bold,
            FontWeight.ExtraBold,
            FontWeight.Black,
        )

    @OptIn(ExperimentalTextApi::class)
    private fun variableFamily(fontRes: Int, weights: List<FontWeight>) =
        FontFamily(
            weights.map { weight ->
                Font(
                    fontRes,
                    weight = weight,
                    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
                )
            }
        )

    val fredoka = variableFamily(R.font.fredoka, fredokaWeights)

    val nunito = variableFamily(R.font.nunito, nunitoWeights)

    /**
     * Headings sit one step heavier than the M3 default so Fredoka's rounded forms read clearly.
     */
    private fun TextStyle.heading(): TextStyle =
        copy(
            fontFamily = fredoka,
            fontWeight =
                when (fontWeight ?: FontWeight.Normal) {
                    FontWeight.Normal -> FontWeight.Medium
                    else -> FontWeight.SemiBold
                },
        )

    private fun TextStyle.text(): TextStyle = copy(fontFamily = nunito)

    /** Emphasized styles carry one step more weight than their base style, per M3 Expressive. */
    private fun TextStyle.emphasized(): TextStyle =
        copy(
            fontWeight =
                when (fontWeight ?: FontWeight.Normal) {
                    FontWeight.Normal -> FontWeight.SemiBold
                    FontWeight.Medium -> FontWeight.SemiBold
                    else -> FontWeight.Bold
                }
        )

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val appTypography =
        with(defaultTypography) {
            Typography(
                displayLarge = displayLarge.heading(),
                displayMedium = displayMedium.heading(),
                displaySmall = displaySmall.heading(),
                headlineLarge = headlineLarge.heading(),
                headlineMedium = headlineMedium.heading(),
                headlineSmall = headlineSmall.heading(),
                titleLarge = titleLarge.heading(),
                titleMedium = titleMedium.heading(),
                titleSmall = titleSmall.heading(),
                bodyLarge = bodyLarge.text(),
                bodyMedium = bodyMedium.text(),
                bodySmall = bodySmall.text(),
                labelLarge = labelLarge.text(),
                labelMedium = labelMedium.text(),
                labelSmall = labelSmall.text(),
                displayLargeEmphasized = displayLarge.heading().emphasized(),
                displayMediumEmphasized = displayMedium.heading().emphasized(),
                displaySmallEmphasized = displaySmall.heading().emphasized(),
                headlineLargeEmphasized = headlineLarge.heading().emphasized(),
                headlineMediumEmphasized = headlineMedium.heading().emphasized(),
                headlineSmallEmphasized = headlineSmall.heading().emphasized(),
                titleLargeEmphasized = titleLarge.heading().emphasized(),
                titleMediumEmphasized = titleMedium.heading().emphasized(),
                titleSmallEmphasized = titleSmall.heading().emphasized(),
                bodyLargeEmphasized = bodyLarge.text().emphasized(),
                bodyMediumEmphasized = bodyMedium.text().emphasized(),
                bodySmallEmphasized = bodySmall.text().emphasized(),
                labelLargeEmphasized = labelLarge.text().emphasized(),
                labelMediumEmphasized = labelMedium.text().emphasized(),
                labelSmallEmphasized = labelSmall.text().emphasized(),
            )
        }
}
