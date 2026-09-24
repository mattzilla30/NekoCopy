package org.nekomanga.presentation.screens.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.main.NavigationItem

@Composable
fun BottomBar(
    items: List<NavigationItem>,
    libraryUpdating: Boolean,
    downloaderRunning: Boolean,
    selectedItemIndex: Int,
    onNavigate: (NavKey) -> Unit,
) {
    ShortNavigationBar(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            val selected = selectedItemIndex == index
            ShortNavigationBarItem(
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
                            if (selected) MaterialTheme.typography.labelMediumEmphasized
                            else MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }
    }
}
