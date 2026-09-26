package org.nekomanga.presentation.screens.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.core.preferences.observeAndUpdate
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.details.MangaDetailsPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SimilarViewModel(val mangaUUID: String) : ViewModel() {

    private val repo: SimilarRepo = Injekt.get()
    private val mangaRepository: MangaRepository = Injekt.get()
    private val preferences: PreferencesHelper = Injekt.get()
    private val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get()
    private val securityPreferences: SecurityPreferences = Injekt.get()

    private val _similarScreenState =
        MutableStateFlow(
            SimilarScreenState(
                isList = preferences.browseAsList().get(),
                incognitoMode = securityPreferences.incognitoMode().get(),
                outlineCovers = preferences.outlineOnCovers().get(),
                dynamicCovers = mangaDetailsPreferences.dynamicCovers().get(),
            )
        )

    val similarScreenState: StateFlow<SimilarScreenState> = _similarScreenState.asStateFlow()

    init {
        getSimilarManga()
        preferences.browseAsList().changes().observeAndUpdate(viewModelScope) { isList ->
            _similarScreenState.update { state -> state.copy(isList = isList) }
        }
    }

    fun refresh() {
        getSimilarManga(true)
    }

    private fun getSimilarManga(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (mangaUUID.isNotEmpty()) {
                _similarScreenState.update {
                    it.copy(
                        isRefreshing = true,
                        allDisplayManga = mapOf(),
                    )
                }

                val list = repo.fetchSimilar(mangaUUID, forceRefresh)
                val allDisplayManga =
                    list.associate { group -> group.type to group.manga.toList() }.toMap()
                _similarScreenState.update {
                    it.copy(
                        isRefreshing = false,
                        allDisplayManga = allDisplayManga,
                    )
                }
            }
        }
    }

    fun switchDisplayMode() {
        preferences.browseAsList().set(!similarScreenState.value.isList)
    }
}
