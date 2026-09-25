package org.nekomanga.presentation.components.bars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.AutoSizeText
import org.nekomanga.presentation.components.ToolTipButton
import org.nekomanga.presentation.components.icons.IncognitoIcon
import org.nekomanga.presentation.theme.Size

/**
 * The app's M3 Expressive top app bar: a centered, emphasized title with an optional subtitle. The
 * incognito badge sits next to the navigation icon.
 */
@Composable
fun TitleTopAppBar(
    color: Color,
    onColor: Color = LocalContentColor.current,
    title: String = "",
    subtitle: String = "",
    navigationIconLabel: String = "",
    navigationIcon: ImageVector? = null,
    incognitoMode: Boolean,
    onNavigationIconClicked: () -> Unit = {},
    onTitleClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit) = {},
    scrolledContainerColor: Color = Color.Transparent,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val titleModifier =
        if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier

    TopAppBar(
        title = {
            if (title.isNotEmpty()) {
                AutoSizeText(
                    text = title,
                    style = MaterialTheme.typography.titleLargeEmphasized.copy(color = onColor),
                    textAlign = TextAlign.Center,
                    modifier = titleModifier,
                )
            }
        },
        subtitle = {
            if (subtitle.isNotEmpty()) {
                AutoSizeText(
                    text = subtitle,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            color = onColor.copy(alpha = SUBTITLE_ALPHA)
                        ),
                    textAlign = TextAlign.Center,
                    modifier = titleModifier,
                )
            }
        },
        titleHorizontalAlignment = Alignment.CenterHorizontally,
        navigationIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (navigationIcon != null) {
                    ToolTipButton(
                        toolTipLabel = navigationIconLabel,
                        icon = navigationIcon,
                        onClick = onNavigationIconClicked,
                        enabledTint = onColor,
                    )
                }
                if (incognitoMode) {
                    Gap(Size.smedium)
                    IncognitoIcon()
                }
            }
        },
        actions = actions,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = color,
                scrolledContainerColor = scrolledContainerColor,
                navigationIconContentColor = onColor,
                titleContentColor = onColor,
                subtitleContentColor = onColor.copy(alpha = SUBTITLE_ALPHA),
                actionIconContentColor = onColor,
            ),
        scrollBehavior = scrollBehavior,
    )
}

private const val SUBTITLE_ALPHA = 0.7f
