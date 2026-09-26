package org.nekomanga.presentation.screens.settings.widgets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.nekomanga.presentation.components.NekoColors

@Composable
fun SitePreferenceWidget(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    loggedIn: Boolean,
    onClick: () -> Unit,
) {
    TextPreferenceWidget(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        onPreferenceClick = onClick,
        widget = {
            Icon(
                imageVector =
                    if (loggedIn) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                tint =
                    if (loggedIn) MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(
                            NekoColors.disabledAlphaHighContrast
                        ),
                contentDescription = null,
            )
        },
    )
}
