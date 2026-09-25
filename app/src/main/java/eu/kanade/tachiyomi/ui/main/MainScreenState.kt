package eu.kanade.tachiyomi.ui.main

import eu.kanade.tachiyomi.ui.main.states.SideNavAlignment
import eu.kanade.tachiyomi.ui.main.states.SideNavMode

data class MainScreenState(
    val incognitoMode: Boolean = false,
    val sideNavAlignment: SideNavAlignment = SideNavAlignment.Center,
    val sideNavMode: SideNavMode = SideNavMode.Default,
    val showWhatsNewDialog: Boolean = false,
)
