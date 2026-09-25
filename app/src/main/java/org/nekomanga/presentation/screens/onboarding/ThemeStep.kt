package org.nekomanga.presentation.screens.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.theme.ThemeFollowSystemSwitch
import org.nekomanga.presentation.components.theme.ThemeSelector
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.theme.Size
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class ThemeStep : OnboardingStep {
    override val isComplete: Boolean = true

    private val preferences: PreferencesHelper = Injekt.get()

    @Composable
    override fun Content() {

        val nightMode by preferences.nightMode().collectAsState()

        Column(modifier = Modifier.padding(Size.medium)) {
            ThemeSelector(preferences = preferences, darkThemeSelector = false)
            Gap(Size.small)
            ThemeSelector(preferences = preferences, darkThemeSelector = true)
            ThemeFollowSystemSwitch(
                modifier = Modifier.padding(vertical = Size.small),
                nightMode = nightMode,
                nightModePreference = preferences.nightMode(),
            )
        }
    }
}
