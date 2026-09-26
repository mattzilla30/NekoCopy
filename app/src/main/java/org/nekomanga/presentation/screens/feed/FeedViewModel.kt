package org.nekomanga.presentation.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onOk
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.nekomanga.core.preferences.observeAndUpdate
import org.nekomanga.core.preferences.toggle
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Drives the History tab: reading history, paged from the database, with search. */
class FeedViewModel : ViewModel() {
    private val preferences: PreferencesHelper = Injekt.get()
    private val securityPreferences: SecurityPreferences = Injekt.get()
    private val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get()
    private val feedRepository: FeedRepository = Injekt.get()

    private val _feedScreenState =
        MutableStateFlow(
            FeedScreenState(
                outlineCovers = preferences.outlineOnCovers().get(),
                dynamicCovers = mangaDetailsPreferences.dynamicCovers().get(),
                outlineCards = preferences.feedViewOutlineCards().get(),
                incognitoMode = securityPreferences.incognitoMode().get(),
            )
        )
    val feedScreenState: StateFlow<FeedScreenState> = _feedScreenState.asStateFlow()

    private val _historyScreenPagingState =
        MutableStateFlow(
            HistoryScreenPagingState(
                historyGrouping = preferences.historyChapterGrouping().get(),
                historyFeedMangaList = lastHistoryFeedMangaList ?: listOf(),
            )
        )
    val historyScreenPagingState: StateFlow<HistoryScreenPagingState> =
        _historyScreenPagingState.asStateFlow()

    private var searchJob: Job? = null

    private val historyPaginator =
        DefaultPaginator(
            initialKey = _historyScreenPagingState.value.offset,
            onLoadUpdated = {
                _historyScreenPagingState.update { state -> state.copy(pageLoading = it) }
            },
            onRequest = {
                feedRepository.getHistoryPage(
                    offset = _historyScreenPagingState.value.offset,
                    group = _historyScreenPagingState.value.historyGrouping,
                )
            },
            getNextKey = { _historyScreenPagingState.value.offset + HISTORY_ENDLESS_LIMIT },
            onError = {
                _historyScreenPagingState.update { state -> state.copy(pageLoading = false) }
            },
            onSuccess = { hasNextPage, items, newKey ->
                _historyScreenPagingState.update { state ->
                    state.copy(
                        offset = newKey,
                        pageLoading = false,
                        hasMoreResults = hasNextPage,
                        // The first page replaces the list shown from the last visit.
                        historyFeedMangaList =
                            if (state.offset == 0) items else state.historyFeedMangaList + items,
                    )
                }
            },
        )

    init {
        lastHistoryFeedMangaList = null

        securityPreferences.incognitoMode().changes().observeAndUpdate(viewModelScope) {
            _feedScreenState.update { state -> state.copy(incognitoMode = it) }
        }

        preferences.feedViewOutlineCards().changes().observeAndUpdate(viewModelScope) {
            _feedScreenState.update { state -> state.copy(outlineCards = it) }
        }

        preferences.outlineOnCovers().changes().observeAndUpdate(viewModelScope) {
            _feedScreenState.update { state -> state.copy(outlineCovers = it) }
        }

        // Emits the current grouping first, which loads the first page.
        preferences.historyChapterGrouping().changes().observeAndUpdate(viewModelScope) {
            _historyScreenPagingState.update { state ->
                state.copy(historyGrouping = it, offset = 0)
            }
            historyPaginator.reset()
            loadNextPage()
        }
    }

    override fun onCleared() {
        lastHistoryFeedMangaList = _historyScreenPagingState.value.historyFeedMangaList
    }

    fun loadNextPage() {
        viewModelScope.launchIO { historyPaginator.loadNextItems() }
    }

    fun toggleGroupHistoryType(historyGrouping: FeedHistoryGroup) {
        viewModelScope.launchIO { preferences.historyChapterGrouping().set(historyGrouping) }
    }

    fun toggleOutlineCards() {
        viewModelScope.launchIO { preferences.feedViewOutlineCards().toggle() }
    }

    fun toggleOutlineCovers() {
        viewModelScope.launchIO { preferences.outlineOnCovers().toggle() }
    }

    fun deleteAllHistoryForAllManga() {
        viewModelScope.launchIO {
            _historyScreenPagingState.update {
                it.copy(
                    offset = 0,
                    historyFeedMangaList = listOf(),
                    searchHistoryFeedMangaList = listOf(),
                    searchQuery = "",
                )
            }
            feedRepository.deleteAllHistory()
            historyPaginator.reset()
            historyPaginator.loadNextItems()
        }
    }

    fun deleteAllHistory(feedManga: FeedManga) {
        viewModelScope.launchIO {
            feedRepository.deleteAllHistoryForManga(feedManga.mangaId)
            _historyScreenPagingState.update {
                it.copy(
                    historyFeedMangaList =
                        it.historyFeedMangaList.filter { fm -> fm.mangaId != feedManga.mangaId }
                )
            }
        }
    }

    fun deleteHistory(feedManga: FeedManga, simpleChapter: SimpleChapter) {
        viewModelScope.launchIO {
            feedRepository.markChapter(simpleChapter.toChapterItem(), ChapterMarkActions.Unread())
            feedRepository.deleteHistoryForChapter(simpleChapter.url)

            val list = _historyScreenPagingState.value.historyFeedMangaList
            val index = list.indexOfFirst { it.mangaId == feedManga.mangaId }
            if (index < 0) return@launchIO

            val updated = list.toMutableList()
            when {
                feedManga.chapters.size == 1 -> updated.removeAt(index)
                _historyScreenPagingState.value.historyGrouping == FeedHistoryGroup.Series ->
                    updated[index] = feedRepository.getUpdatedFeedMangaForHistoryBySeries(feedManga)
                else ->
                    updated[index] =
                        list[index].copy(
                            chapters =
                                list[index].chapters.filter { it.chapter.url != simpleChapter.url }
                        )
            }
            _historyScreenPagingState.update { it.copy(historyFeedMangaList = updated) }
        }
    }

    fun search(searchQuery: String?) {
        searchJob?.cancel()
        searchJob = viewModelScope.launchIO {
            if (searchQuery.isNullOrBlank()) {
                _historyScreenPagingState.update {
                    it.copy(searchHistoryFeedMangaList = listOf(), searchQuery = "")
                }
            } else {
                searchHistory(searchQuery)
            }
        }
    }

    private suspend fun searchHistory(searchQuery: String) {
        feedRepository
            .getHistoryPage(
                searchQuery = searchQuery,
                offset = 0,
                limit = SEARCH_LIMIT,
                group = _historyScreenPagingState.value.historyGrouping,
            )
            .onOk { (_, results) ->
                _historyScreenPagingState.update {
                    it.copy(searchQuery = searchQuery, searchHistoryFeedMangaList = results)
                }
            }
    }

    /** Reloads the pages already shown, for changes made in the reader or on the manga screen. */
    fun updateMangaForChanges() {
        viewModelScope.launchIO {
            delay(500L)
            val state = _historyScreenPagingState.value
            if (state.searchQuery.isNotBlank()) searchHistory(state.searchQuery)
            if (state.historyFeedMangaList.isEmpty()) return@launchIO

            val reloaded =
                (0..state.offset step HISTORY_ENDLESS_LIMIT).flatMap { offset ->
                    feedRepository
                        .getHistoryPage(offset = offset, group = state.historyGrouping)
                        .get()
                        ?.second
                        .orEmpty()
                }
            _historyScreenPagingState.update { it.copy(historyFeedMangaList = reloaded) }
        }
    }

    companion object {
        /** The history shown when the tab closed, shown again at once when it reopens. */
        private var lastHistoryFeedMangaList: List<FeedManga>? = null

        fun onLowMemory() {
            lastHistoryFeedMangaList = null
        }

        const val HISTORY_ENDLESS_LIMIT = 15
        private const val SEARCH_LIMIT = 100
    }
}
