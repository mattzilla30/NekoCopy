package org.nekomanga.presentation.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.main.NavigationItem
import eu.kanade.tachiyomi.ui.main.states.SideNavAlignment
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

@Composable
fun NavigationSideBar(
    items: List<NavigationItem>,
    sideNavAlignment: SideNavAlignment,
    libraryUpdating: Boolean,
    downloaderRunning: Boolean,
    selectedItemIndex: Int,
    onNavigate: (NavKey) -> Unit,
) {
    val arrangement =
        remember(sideNavAlignment) {
            when (sideNavAlignment) {
                SideNavAlignment.Bottom -> Arrangement.spacedBy(Size.tiny, Alignment.Bottom)
                SideNavAlignment.Center ->
                    Arrangement.spacedBy(Size.tiny, Alignment.CenterVertically)
                SideNavAlignment.Top -> Arrangement.spacedBy(Size.tiny, Alignment.Top)
            }
        }

    val railState = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()
    val expanded = railState.targetValue == WideNavigationRailValue.Expanded

    WideNavigationRail(
        modifier = Modifier.fillMaxHeight(),
        state = railState,
        arrangement = arrangement,
        header = {
            IconButton(
                modifier = Modifier.padding(start = Size.large),
                shapes = IconButtonDefaults.shapes(),
                onClick = {
                    scope.launch { if (expanded) railState.collapse() else railState.expand() }
                },
            ) {
                Icon(
                    imageVector =
                        if (expanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Filled.Menu,
                    contentDescription =
                        stringResource(if (expanded) R.string.collapse else R.string.expand),
                )
            }
        },
    ) {
        items.forEachIndexed { index, item ->
            val selected = selectedItemIndex == index
            WideNavigationRailItem(
                railExpanded = expanded,
                selected = selected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    PulsingIcon(
                        isPulsing =
                            ((index == 0 && libraryUpdating) || (index == 1 && downloaderRunning)),
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style =
                            if (selected) MaterialTheme.typography.labelLargeEmphasized
                            else MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}
