package org.nekomanga.presentation.screens.about

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.main.AppSnackbarManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.snackbar.SnackbarState
import org.nekomanga.usecases.preferences.GetFormattedBuildTimeUseCase
import uy.kohesive.injekt.injectLazy

class AboutViewModel : ViewModel() {
    private val preferences: PreferencesHelper by injectLazy()

    private val securityPreferences: SecurityPreferences by injectLazy()
    private val getFormattedBuildTimeUseCase: GetFormattedBuildTimeUseCase by injectLazy()

    val appSnackbarManager: AppSnackbarManager by injectLazy()

    private val _aboutScreenState =
        MutableStateFlow(
            AboutScreenState(
                buildTime = getFormattedBuildTime(),
                incognitoMode = securityPreferences.incognitoMode().get(),
            )
        )
    val aboutScreenState: StateFlow<AboutScreenState> = _aboutScreenState.asStateFlow()

    private fun getFormattedBuildTime(): String {
        return getFormattedBuildTimeUseCase(BuildConfig.BUILD_TIME)
    }

    fun onVersionLongClicked() {
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                appSnackbarManager.showSnackbar(
                    SnackbarState(
                        messageRes = R.string._copied_to_clipboard,
                        fieldRes = R.string.build_information,
                    )
                )
            }
        }
    }
}
