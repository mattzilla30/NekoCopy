package eu.kanade.tachiyomi.ui.source.latest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.map
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.manga.resyncDisplayManga
import eu.kanade.tachiyomi.util.manga.unique
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.nekomanga.core.preferences.observeAndUpdate
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.network.ResultError
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DisplayViewModel(val displayScreenType: DisplayScreenType) : ViewModel() {

    private val displayRepository: DisplayRepository = Injekt.get()
    private val preferences: PreferencesHelper = Injekt.get()

    private val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get()
    private val securityPreferences: SecurityPreferences = Injekt.get()

    private val mangaRepository: MangaRepository = Injekt.get()

    private val _displayScreenState =
        MutableStateFlow(
            DisplayScreenState(
                isList = preferences.browseAsList().get(),
                title = displayScreenType.title,
                incognitoMode = securityPreferences.incognitoMode().get(),
                outlineCovers = preferences.outlineOnCovers().get(),
                dynamicCovers = mangaDetailsPreferences.dynamicCovers().get(),
            )
        )
    val displayScreenState: StateFlow<DisplayScreenState> = _displayScreenState.asStateFlow()

    private val paginator =
        DefaultPaginator(
            initialKey = _displayScreenState.value.page,
            onLoadUpdated = { _displayScreenState.update { state -> state.copy(isLoading = it) } },
            onRequest = { nextPage ->
                displayRepository.getPage(nextPage, displayScreenType).map { result ->
                    result.hasNextPage to listOf(result)
                }
            },
            getNextKey = { _displayScreenState.value.page + 1 },
            onError = { resultError ->
                _displayScreenState.update {
                    it.copy(
                        isLoading = false,
                        error =
                            when (resultError) {
                                is ResultError.Generic -> resultError.errorString
                                else -> (resultError as ResultError.HttpError).message
                            },
                    )
                }
            },
            onSuccess = { hasNextPage, items, newKey ->
                val isDisplayResult = _displayScreenState.value.isDisplayResult

                if (isDisplayResult) {
                    _displayScreenState.update {
                        it.copy(
                            isLoading = false,
                            page = newKey,
                            endReached = !hasNextPage,
                            alternativeDisplay = items.first().displayResult,
                        )
                    }
                } else {
                    _displayScreenState.update {
                        val allDisplayManga =
                            (_displayScreenState.value.allDisplayManga + items.first().displayManga)
                                .distinctBy { it.mangaId }
                        it.copy(
                            isLoading = false,
                            page = newKey,
                            endReached = !hasNextPage,
                            allDisplayManga = allDisplayManga.toList(),
                        )
                    }
                }
            },
        )

    init {
        if (
            displayScreenType is DisplayScreenType.AuthorByName ||
                displayScreenType is DisplayScreenType.GroupByName
        ) {
            _displayScreenState.update { it.copy(isDisplayResult = true) }
        }
        loadNextItems()

        preferences.browseAsList().changes().observeAndUpdate(viewModelScope) {
            _displayScreenState.update { state -> state.copy(isList = it) }
        }
    }

    fun loadNextItems() {
        viewModelScope.launchIO { paginator.loadNextItems() }
    }

    fun switchDisplayMode() {
        preferences.browseAsList().set(!displayScreenState.value.isList)
    }

    fun updateMangaForChanges() {
        viewModelScope.launchIO {
            val newDisplayManga =
                _displayScreenState.value.allDisplayManga
                    .resyncDisplayManga(mangaRepository)
                    .unique()
                    .toList()
            _displayScreenState.update { it.copy(allDisplayManga = newDisplayManga) }
        }
    }
}
