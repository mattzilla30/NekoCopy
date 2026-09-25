package eu.kanade.tachiyomi.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.manga.MangaShortcutManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.core.preferences.observeAndUpdate
import org.nekomanga.core.preferences.toggle
import org.nekomanga.core.security.SecurityPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class MainActivityViewModel : ViewModel() {

    private val mangaShortcutManager: MangaShortcutManager by injectLazy()

    private val _deepLinkScreen = MutableStateFlow<List<NavKey>?>(null)
    val deepLinkScreen: StateFlow<List<NavKey>?> = _deepLinkScreen.asStateFlow()

    val downloadManager: DownloadManager by injectLazy()

    fun setDeepLink(screens: List<NavKey>) {
        _deepLinkScreen.value = screens
    }

    fun consumeDeepLink() {
        _deepLinkScreen.value = null
    }

    val securityPreferences: SecurityPreferences = Injekt.get()
    val preferences: PreferencesHelper = Injekt.get()

    private val _mainScreenState = MutableStateFlow(MainScreenState())
    val mainScreenState: StateFlow<MainScreenState> = _mainScreenState.asStateFlow()

    fun setWhatsNewDialog(shouldShow: Boolean) {
        _mainScreenState.update { it.copy(showWhatsNewDialog = shouldShow) }
    }

    init {
        securityPreferences.incognitoMode().changes().observeAndUpdate(viewModelScope) {
            incognitoMode ->
            _mainScreenState.update { it.copy(incognitoMode = incognitoMode) }
        }

        preferences.sideNavIconAlignment().changes().observeAndUpdate(viewModelScope) {
            sideNavAlignment ->
            _mainScreenState.update { it.copy(sideNavAlignment = sideNavAlignment) }
        }

        preferences.sideNavMode().changes().observeAndUpdate(viewModelScope) { sideNavMode ->
            _mainScreenState.update { it.copy(sideNavMode = sideNavMode) }
        }
    }

    fun toggleIncoginito() {
        viewModelScope.launch { securityPreferences.incognitoMode().toggle() }
    }

    fun onboardingCompleted() {
        viewModelScope.launch { preferences.hasShownOnboarding().set(true) }
    }

    fun saveExtras(lastUsedTab: Int) {
        viewModelScope.launch { preferences.lastUsedStartingTab().set(lastUsedTab) }

        viewModelScope.launch {
            mangaShortcutManager.updateShortcuts()
            MangaCoverMetadata.savePrefs()
        }
    }
}
