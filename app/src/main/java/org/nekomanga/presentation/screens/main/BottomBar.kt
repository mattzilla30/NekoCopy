package org.nekomanga.presentation.screens.main

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.main.NavigationItem

@Composable
fun BottomBar(
    items: List<NavigationItem>,
    selectedItemIndex: Int,
    onNavigate: (NavKey) -> Unit,
) {
    // ShortNavigationBar already spans the full width. A fillMaxWidth modifier would raise the
    // items' minimum width to the whole bar and push every item after the first off-screen.
    ShortNavigationBar {
        items.forEachIndexed { index, item ->
            val selected = selectedItemIndex == index
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    Icon(
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
