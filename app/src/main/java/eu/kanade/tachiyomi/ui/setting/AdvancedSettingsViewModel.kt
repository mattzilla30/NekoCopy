package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.launchUI
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.nekomanga.R
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.presentation.components.UiText
import uy.kohesive.injekt.injectLazy

// This class just holds some injects.  If a settings screen requires
class AdvancedSettingsViewModel : ViewModel() {
    val preferences: PreferencesHelper by injectLazy()

    val readerPreferences: ReaderPreferences by injectLazy()

    val mangaDetailsPreferences: MangaDetailsPreferences by injectLazy()
    val networkPreference: NetworkPreferences by injectLazy()
    val networkHelper: NetworkHelper by injectLazy()

    val historyRepository: HistoryRepository by injectLazy()

    val chapterRepository: ChapterRepository by injectLazy()

    val mangaRepository: MangaRepository by injectLazy()

    private val _toastEvent = MutableSharedFlow<UiText>()
    val toastEvent = _toastEvent.asSharedFlow()

    fun clearNetworkCookies() {
        viewModelScope.launchUI {
            networkHelper.cookieManager.removeAll()
            _toastEvent.emit(UiText.StringResource(R.string.cookies_cleared))
        }
    }

    fun clearDatabase(keepRead: Boolean) {
        viewModelScope.launchUI {
            mangaDetailsPreferences.coverVibrantColors().delete()
            if (keepRead) {
                mangaRepository.deleteAllNotInLibraryAndNotRead()
            } else {
                mangaRepository.deleteAllNotInLibrary()
            }
            historyRepository.deleteHistoryNoLastRead()

            _toastEvent.emit(UiText.StringResource(R.string.clear_database_completed))
        }
    }
}
