package org.nekomanga.presentation.screens.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.manga.resyncDisplayManga
import eu.kanade.tachiyomi.util.manga.resyncHomePageManga
import eu.kanade.tachiyomi.util.manga.unique
import eu.kanade.tachiyomi.util.system.activeNetworkState
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import org.nekomanga.R
import org.nekomanga.core.preferences.observeAndUpdate
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.data.database.repository.BrowseFilterRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.filter.Filter
import org.nekomanga.domain.filter.QueryType
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.network.message
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.presentation.components.UiText
import org.nekomanga.usecases.filter.CalculateDexFilterUseCase
import org.nekomanga.usecases.manga.MangaUseCases
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class BrowseViewModel : ViewModel() {
    private val browseRepository: BrowseRepository = Injekt.get()
    val preferences: PreferencesHelper = Injekt.get()
    private val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get()
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get()
    val securityPreferences: SecurityPreferences = Injekt.get()

    val browseFilterRepository: BrowseFilterRepository = Injekt.get()

    private val mangaRepository: MangaRepository = Injekt.get()

    private val mangaUseCases: MangaUseCases by injectLazy()
    private val calculateDexFilter: CalculateDexFilterUseCase by injectLazy()

    private val _browseScreenState =
        MutableStateFlow(
            BrowseScreenState(
                outlineCovers = preferences.outlineOnCovers().get(),
                dynamicCovers = mangaDetailsPreferences.dynamicCovers().get(),
                filters = createInitialDexFilter(""),
                defaultContentRatings = mangaDexPreferences.visibleContentRatings().get().toSet(),
                screenType = BrowseScreenType.Homepage,
            )
        )
    val browseScreenState: StateFlow<BrowseScreenState> = _browseScreenState.asStateFlow()

    private val _deepLinkManga = MutableStateFlow<Long?>(null)
    val deepLinkMangaFlow = _deepLinkManga.asStateFlow()

    private val _navigateEvent = Channel<NavigationEvent>()
    val navigateEvent = _navigateEvent.receiveAsFlow()

    fun initSearch(titleQuery: String) {
        if (_browseScreenState.value.deepLinkHandled) return
        _browseScreenState.update {
            it.copy(deepLinkHandled = true, filters = createInitialDexFilter(titleQuery))
        }
        getSearchPage()
    }

    private fun createInitialDexFilter(incomingQuery: String): DexFilters {
        val enabledContentRatings = mangaDexPreferences.visibleContentRatings().get()
        val contentRatings =
            MangaContentRating.getOrdered()
                .map { Filter.ContentRating(it, enabledContentRatings.contains(it.key)) }
                .toList()

        return DexFilters(
            query = Filter.Query(incomingQuery, QueryType.Title),
            contentRatings = contentRatings,
            contentRatingVisible = mangaDexPreferences.showContentRatingFilter().get(),
        )
    }

    private val paginator =
        DefaultPaginator(
            initialKey = _browseScreenState.value.page,
            onLoadUpdated = { _browseScreenState.update { state -> state.copy(pageLoading = it) } },
            onRequest = {
                browseRepository.getSearchPage(
                    browseScreenState.value.page,
                    browseScreenState.value.filters,
                )
            },
            getNextKey = { _browseScreenState.value.page + 1 },
            onError = { resultError ->
                _browseScreenState.update {
                    it.copy(
                        initialLoading = false,
                        pageLoading = false,
                        error =
                            UiText.String(
                                when (resultError) {
                                    is ResultError.Generic -> resultError.errorString
                                    else -> (resultError as ResultError.HttpError).message
                                }
                            ),
                    )
                }
            },
            onSuccess = { hasNextPage, items, nextKey ->
                val allDisplayManga =
                    (_browseScreenState.value.displayMangaHolder.allDisplayManga + items)
                        .distinctBy { it.url }

                val filteredDisplayManga = allDisplayManga.toList()
                _browseScreenState.update { state ->
                    state.copy(
                        screenType = BrowseScreenType.Filter,
                        displayMangaHolder =
                            DisplayMangaHolder(
                                BrowseScreenType.Filter,
                                allDisplayManga.toList(),
                                filteredDisplayManga.toList(),
                            ),
                        initialLoading = false,
                        pageLoading = false,
                        page = nextKey,
                        endReached = !hasNextPage,
                    )
                }
                if (filteredDisplayManga.isEmpty()) {
                    loadNextItems()
                }
            },
        )

    init {
        isOnline()

        if (_browseScreenState.value.firstLoad) {
            if (browseScreenState.value.filters.query.text.isNotBlank()) {
                getSearchPage()
            } else {
                getHomepage()
            }
        }

        updateBrowseFilters(_browseScreenState.value.firstLoad)
        _browseScreenState.update { it.copy(firstLoad = false) }

        preferences.useVividColorHeaders().changes().observeAndUpdate(viewModelScope) { enabled ->
            _browseScreenState.update { it.copy(useVividColorHeaders = enabled) }
        }

        securityPreferences.incognitoMode().changes().observeAndUpdate(viewModelScope) {
            _browseScreenState.update { state -> state.copy(incognitoMode = it) }
        }
    }

    fun loadNextItems() {
        viewModelScope.launchIO { paginator.loadNextItems() }
    }

    private fun getHomepage() {
        viewModelScope.launchIO {
            if (!isOnline()) return@launchIO
            browseRepository
                .getHomePage()
                .onErr { resultError ->
                    _browseScreenState.update { state ->
                        state.copy(
                            error = UiText.String(resultError.message()),
                            initialLoading = false,
                        )
                    }
                }
                .onOk {
                    _browseScreenState.update { state ->
                        state.copy(
                            homePageManga = it,
                            initialLoading = false,
                        )
                    }
                }
        }
    }

    fun retry() {
        viewModelScope.launchIO {
            val initialLoading = _browseScreenState.value.page == 1
            _browseScreenState.update { state ->
                state.copy(initialLoading = initialLoading, error = null)
            }
            when (browseScreenState.value.screenType) {
                BrowseScreenType.Homepage -> getHomepage()
                else -> getSearchPage()
            }
        }
    }

    fun getSearchPage() {
        viewModelScope.launchIO {
            if (!isOnline()) return@launchIO

            _browseScreenState.update { state ->
                state.copy(
                    initialLoading = true,
                    error = null,
                    page = 1,
                    pageLoading = false,
                    screenType = BrowseScreenType.Filter,
                    displayMangaHolder =
                        DisplayMangaHolder(
                            resultType = BrowseScreenType.Filter,
                            allDisplayManga = listOf(),
                            filteredDisplayManga = listOf(),
                        ),
                )
            }

            // If no new search is provided, we use the existing filters in state
            val currentMode = browseScreenState.value.filters.queryMode
            val currentQuery = browseScreenState.value.filters.query.text

            when (currentMode) {
                QueryType.Author -> {
                    _navigateEvent.send(
                        NavigationEvent.NavigateToDisplay(
                            DisplayScreenType.AuthorByName(UiText.String(currentQuery))
                        )
                    )
                }

                QueryType.Group -> {
                    _navigateEvent.send(
                        NavigationEvent.NavigateToDisplay(
                            DisplayScreenType.GroupByName(UiText.String(currentQuery))
                        )
                    )
                }

                QueryType.List -> {
                    _navigateEvent.send(
                        NavigationEvent.NavigateToDisplay(
                            DisplayScreenType.List(
                                title = UiText.String(""),
                                listUUID = currentQuery,
                            )
                        )
                    )
                }

                else -> {
                    // Standard Title/Tag search
                    paginator.loadNextItems()
                }
            }
        }
    }

    fun otherClick(uuid: String) {}

    fun onDeepLinkMangaHandled() {
        _deepLinkManga.value = null
    }

    fun randomManga() {
        viewModelScope.launchIO {
            _browseScreenState.update { it.copy(initialLoading = true) }
            browseRepository
                .getRandomManga()
                .onErr { error ->
                    _browseScreenState.update {
                        it.copy(initialLoading = false, error = UiText.String(error.message()))
                    }
                }
                .onOk { displayManga ->
                    _browseScreenState.update { it.copy(initialLoading = false) }
                    _deepLinkManga.value = displayManga.mangaId
                }
        }
    }

    fun saveFilter(name: String) {
        viewModelScope.launchIO {
            val browseFilter =
                BrowseFilterImpl(
                    name = name,
                    dexFilters = Json.encodeToString(browseScreenState.value.filters),
                )
            browseFilterRepository.insertBrowseFilter(browseFilter)
            updateBrowseFilters()
        }
    }

    fun loadFilter(browseFilterImpl: BrowseFilterImpl) {
        viewModelScope.launchIO {
            val dexFilters = Json.decodeFromString<DexFilters>(browseFilterImpl.dexFilters)
            _browseScreenState.update { it.copy(filters = dexFilters) }
        }
    }

    fun markFilterAsDefault(name: String, makeDefault: Boolean) {
        viewModelScope.launchIO {
            val updatedFilters =
                browseScreenState.value.savedFilters.map {
                    if (it.name == name) {
                        it.copy(default = makeDefault)
                    } else {
                        it.copy(default = false)
                    }
                }
            browseFilterRepository.insertBrowseFilters(updatedFilters)
            updateBrowseFilters()
        }
    }

    fun deleteFilter(name: String) {
        viewModelScope.launchIO {
            browseFilterRepository.deleteBrowseFilterByName(name)
            updateBrowseFilters()
        }
    }

    fun resetFilter() {
        viewModelScope.launchIO {
            val resetFilters = createInitialDexFilter("")
            _browseScreenState.update { it.copy(filters = resetFilters) }
        }
    }

    fun filterChanged(newFilter: Filter) {
        viewModelScope.launchIO {
            _browseScreenState.update {
                it.copy(filters = calculateDexFilter(it.filters, newFilter))
            }
        }
    }

    fun changeScreenType(browseScreenType: BrowseScreenType) {
        viewModelScope.launchIO {
            when (browseScreenType) {
                BrowseScreenType.Filter -> getSearchPage()
                else -> Unit
            }
            _browseScreenState.update { it.copy(screenType = browseScreenType, error = null) }
        }
    }

    private fun updateBrowseFilters(initialLoad: Boolean = false) {
        viewModelScope.launchIO {
            val filters = browseFilterRepository.getBrowseFilters().toList()
            _browseScreenState.update { it.copy(savedFilters = filters) }
            if (initialLoad) {
                filters
                    .firstOrNull { it.default }
                    ?.let { filter ->
                        val dexFilters = Json.decodeFromString<DexFilters>(filter.dexFilters)
                        _browseScreenState.update { it.copy(filters = dexFilters) }
                    }
            }
        }
    }

    fun updateMangaForChanges() {
        if (!_browseScreenState.value.firstLoad) {
            viewModelScope.launchIO {
                val newHomePageManga =
                    _browseScreenState.value.homePageManga.resyncHomePageManga(mangaRepository)
                _browseScreenState.update { it.copy(homePageManga = newHomePageManga) }
            }
            viewModelScope.launchIO {
                val allDisplayManga =
                    _browseScreenState.value.displayMangaHolder.allDisplayManga
                        .resyncDisplayManga(mangaRepository)
                        .unique()
                _browseScreenState.update {
                    it.copy(
                        displayMangaHolder =
                            it.displayMangaHolder.copy(
                                allDisplayManga = allDisplayManga.toList(),
                                filteredDisplayManga = allDisplayManga.toList(),
                            )
                    )
                }
            }
        }
    }

    /** Check if can access internet */
    private fun isOnline(): Boolean {
        val isOnline = preferences.context.activeNetworkState().isOnline
        if (!isOnline) {
            _browseScreenState.update {
                it.copy(
                    initialLoading = false,
                    error = UiText.StringResource(R.string.no_network_connection),
                )
            }
        }
        return isOnline
    }
}
