package org.nekomanga.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.Size.large as horizontalPadding
import org.nekomanga.presentation.theme.Size.medium as verticalPadding
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.toggle

@Composable
fun HeadingItem(text: UiText) {
    Text(
        text = text.asString(),
        style = MaterialTheme.typography.headlineLarge,
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    )
}

@Composable
fun IconItem(
    labelText: UiText,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    BaseSettingsItem(
        labelText = labelText,
        isSelected = isSelected,
        widget = {
            Icon(
                modifier = Modifier.size(Size.large),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
        onClick = onClick,
    )
}

@Composable
fun BaseSortItem(labelText: UiText, icon: ImageVector?, onClick: () -> Unit) {
    BaseSettingsItem(
        labelText = labelText,
        widget = {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Gap(Size.large)
            }
        },
        onClick = onClick,
    )
}

@Composable
fun CheckboxItem(labelText: UiText, preference: Preference<Boolean>) {
    val checked by preference.collectAsState<Boolean>()
    CheckboxItem(labelText = labelText, checked = checked, onClick = { preference.toggle() })
}

@Composable
fun CheckboxItem(labelText: UiText, checked: Boolean, onClick: () -> Unit) {
    BaseSettingsItem(
        labelText = labelText,
        widget = { Checkbox(checked = checked, onCheckedChange = null) },
        onClick = onClick,
    )
}

@Composable
private fun BaseSettingsItem(
    labelText: UiText,
    widget: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    isSelected: Boolean = false,
) {
    Row(
        modifier =
            Modifier.clickable(onClick = onClick)
                .fillMaxWidth()
                .then(
                    if (isSelected) {
                        Modifier.background(color = MaterialTheme.colorScheme.secondaryContainer)
                    } else Modifier
                )
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Size.large),
    ) {
        widget(this)
        Text(text = labelText.asString(), style = MaterialTheme.typography.bodyLarge)
    }
}
