package org.nekomanga.presentation.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.nekomanga.R

object Typefaces {
    private val defaultTypography = Typography()

    private const val LETTER_SPACING = -.15
    val mplusRounded =
        FontFamily(
            Font(R.font.mplus_rounded1c_thin, FontWeight.Thin),
            Font(R.font.mplus_rounded1c_black, FontWeight.Black),
            Font(R.font.mplus_rounded1c_bold, FontWeight.Bold),
            Font(R.font.mplus_rounded1c_extra_bold, FontWeight.ExtraBold),
            Font(R.font.mplus_rounded1c_medium, FontWeight.Medium),
            Font(R.font.mplus_rounded1c_semi_bold, FontWeight.SemiBold),
            Font(R.font.mplus_rounded1c_regular, FontWeight.Normal),
        )

    private fun TextStyle.rounded(): TextStyle =
        copy(fontFamily = mplusRounded, letterSpacing = LETTER_SPACING.sp)

    /** Emphasized styles carry one step more weight than their base style, per M3 Expressive. */
    private fun TextStyle.emphasized(): TextStyle =
        rounded()
            .copy(
                fontWeight =
                    when (fontWeight ?: FontWeight.Normal) {
                        FontWeight.Normal -> FontWeight.Medium
                        FontWeight.Medium -> FontWeight.Bold
                        else -> FontWeight.ExtraBold
                    }
            )

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val appTypography =
        with(defaultTypography) {
            Typography(
                displayLarge = displayLarge.rounded(),
                displayMedium = displayMedium.rounded(),
                displaySmall = displaySmall.rounded(),
                headlineLarge = headlineLarge.rounded(),
                headlineMedium = headlineMedium.rounded(),
                headlineSmall = headlineSmall.rounded(),
                titleLarge = titleLarge.rounded(),
                titleMedium = titleMedium.rounded(),
                titleSmall = titleSmall.rounded(),
                bodyLarge = bodyLarge.rounded(),
                bodyMedium = bodyMedium.rounded(),
                bodySmall = bodySmall.rounded(),
                labelLarge = labelLarge.rounded(),
                labelMedium = labelMedium.rounded(),
                labelSmall = labelSmall.rounded(),
                displayLargeEmphasized = displayLarge.emphasized(),
                displayMediumEmphasized = displayMedium.emphasized(),
                displaySmallEmphasized = displaySmall.emphasized(),
                headlineLargeEmphasized = headlineLarge.emphasized(),
                headlineMediumEmphasized = headlineMedium.emphasized(),
                headlineSmallEmphasized = headlineSmall.emphasized(),
                titleLargeEmphasized = titleLarge.emphasized(),
                titleMediumEmphasized = titleMedium.emphasized(),
                titleSmallEmphasized = titleSmall.emphasized(),
                bodyLargeEmphasized = bodyLarge.emphasized(),
                bodyMediumEmphasized = bodyMedium.emphasized(),
                bodySmallEmphasized = bodySmall.emphasized(),
                labelLargeEmphasized = labelLarge.emphasized(),
                labelMediumEmphasized = labelMedium.emphasized(),
                labelSmallEmphasized = labelSmall.emphasized(),
            )
        }
}
