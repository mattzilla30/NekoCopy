package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onErr
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.ArtworkImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.models.dto.AggregateVolume
import eu.kanade.tachiyomi.source.online.models.dto.asMdMap
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.main.AppSnackbarManager
import eu.kanade.tachiyomi.ui.manga.MangaConstants.NextUnreadChapter
import eu.kanade.tachiyomi.ui.manga.MangaConstants.SortOption
import eu.kanade.tachiyomi.ui.manga.MangaConstants.applyToManga
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import eu.kanade.tachiyomi.util.chapter.ChapterItemSort
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.chapter.MissingChapterHolder
import eu.kanade.tachiyomi.util.chapter.getMissingChapters
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.isOnline
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.openInWebView
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.data.database.repository.ArtworkRepository
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.MangaAggregateRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.manga.Stats
import org.nekomanga.domain.manga.getDescription
import org.nekomanga.domain.manga.toManga
import org.nekomanga.domain.manga.toMangaItem
import org.nekomanga.domain.manga.uuid
import org.nekomanga.domain.network.message
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.domain.snackbar.SnackbarColor
import org.nekomanga.domain.snackbar.SnackbarState
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.UiText
import org.nekomanga.usecases.chapters.ChapterUseCases
import org.nekomanga.usecases.chapters.GetChapterFilterText
import org.nekomanga.usecases.manga.MangaUseCases
import org.nekomanga.usecases.preferences.GetDateFormatUseCase
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaViewModel(val mangaId: Long) : ViewModel() {

    companion object {
        private const val DYNAMIC_COVER_UPDATE_DELAY_MS = 1000L
    }

    private val preferences: PreferencesHelper = Injekt.get()
    private val getDateFormatUseCase: GetDateFormatUseCase = Injekt.get()
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get()
    val securityPreferences: SecurityPreferences = Injekt.get()
    val mangaDetailsPreferences: MangaDetailsPreferences = Injekt.get()
    val coverCache: CoverCache = Injekt.get()
    val mangaRepository: MangaRepository = Injekt.get()
    val mangaAggregateRepository: MangaAggregateRepository = Injekt.get()

    val artworkRepository: ArtworkRepository = Injekt.get()

    val chapterRepository: ChapterRepository = Injekt.get()
    val historyRepository: HistoryRepository = Injekt.get()

    val appSnackbarManager: AppSnackbarManager = Injekt.get()
    val chapterItemFilter: ChapterItemFilter = Injekt.get()
    val sourceManager: SourceManager = Injekt.get()
    private val mangaUpdateCoordinator: MangaUpdateCoordinator = Injekt.get()
    private val storageManager: StorageManager = Injekt.get()
    private val chapterUseCases: ChapterUseCases = Injekt.get()

    private val mangaUseCases: MangaUseCases = Injekt.get()

    private val getFilterText = GetChapterFilterText(preferences.context)

    private var dynamicCoverUpdateJob: Job? = null

    private val _mangaDetailScreenState =
        MutableStateFlow(
            MangaConstants.MangaDetailScreenState(
                general = MangaConstants.MangaScreenGeneralState(),
                manga =
                    MangaConstants.MangaScreenMangaState(
                        currentArtwork = createInitialCurrentArtwork()
                    ),
            )
        )

    val mangaDetailScreenState: StateFlow<MangaConstants.MangaDetailScreenState> =
        _mangaDetailScreenState.asStateFlow()

    private val chapterSort = ChapterItemSort(chapterItemFilter, preferences)

    private data class MangaFilterState(
        val scanlators: Set<String>? = null,
        val languages: Set<String>? = null,
        val sortOption: SortOption? = null,
        val readFilter: Int? = null,
        val bookmarkedFilter: Int? = null,
        val availableFilter: Int? = null,
        val hideChapterTitles: Boolean? = null,
        val forceGlobal: Boolean = false,
    )

    private val _mangaFilterState = MutableStateFlow<MangaFilterState?>(null)

    // Channel to debounce DB writes for filters
    private val _persistFilterChannel = Channel<Unit>(Channel.CONFLATED)

    /**
     * MACRO-LEVEL PERFORMANCE OPTIMIZATION (Overclock):
     *
     * Why: Previously, these database flows (`mangaFlow`, `historyFlow`, `artworkFlow`, etc.) were
     * standard cold flows. When they were combined inside the large `combine` block below, or when
     * collected multiple times due to UI state updates or rotation, they would each trigger
     * redundant database queries and re-execute expensive mapping operations. In complex screens
     * with many DB observers, this caused massive CPU spikes and unnecessary memory allocations as
     * the same data was fetched repeatedly.
     *
     * Architecture: By applying `.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)`
     * to these intermediate flows, we fundamentally shift them from Cold to Hot streams.
     * - `viewModelScope` bounds the flow lifecycle to the ViewModel, preventing memory leaks.
     * - `SharingStarted.WhileSubscribed(5000)` ensures the upstream database subscription stays
     *   alive for 5 seconds after the last subscriber disconnects (e.g., during a configuration
     *   change like device rotation). This avoids tearing down and recreating the expensive DB
     *   query.
     * - `replay = 1` immediately emits the latest cached value to any new parallel collectors
     *   without needing a dummy initial value like `stateIn` would require.
     *
     * Impact: Reduces N database queries per observer down to exactly 1 query per active stream.
     * Massive reduction in UI thread blocking, measurement overhead, and GC thrashing.
     */
    val mangaFlow =
        mangaRepository
            .observeMangaById(mangaId)
            .mapNotNull {
                it ?: return@mapNotNull null
                it.toMangaItem()
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val historyFlow =
        historyRepository
            .observeHistoryByMangaId(mangaId)
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val artworkFlow =
        artworkRepository
            .observeArtworkByMangaId(mangaId)
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    /**
     * Chapters for the manga, without those from blocked groups and uploaders. The flow reads the
     * manga from [mangaFlow] instead of querying it on every emission.
     */
    val allChapterFlow =
        combine(
                chapterRepository.observeChaptersForManga(mangaId),
                mangaFlow,
                mangaDexPreferences.blockedGroups().changes(),
                mangaDexPreferences.blockedUploaders().changes(),
            ) { dbChapters, _, blockedGroups, blockedUploaders ->
                dbChapters.mapNotNull { dbChapter ->
                    dbChapter
                        .toSimpleChapter()
                        ?.takeIf { chapter ->
                            (blockedGroups.isEmpty() && blockedUploaders.isEmpty()) ||
                                chapterUseCases.validateChapterNotBlocked(
                                    chapter.scanlatorList(),
                                    chapter.uploader,
                                    blockedGroups,
                                    blockedUploaders,
                                )
                        }
                        ?.let { chapter -> ChapterItem(chapter = chapter) }
                }
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val staticChapterDataFlow =
        allChapterFlow
            .map { chapters ->
                val persistentChapters = chapters.toList()
                val missingChapterHolder = persistentChapters.getMissingChapters()

                val allChapterScanlators =
                    persistentChapters
                        .flatMap { ChapterUtil.getScanlators(it.chapter.scanlator) }
                        .toMutableSet()
                val allChapterUploaders =
                    persistentChapters
                        .mapNotNull {
                            if (it.chapter.uploader.isEmpty()) return@mapNotNull null
                            if (it.chapter.scanlator != Constants.NO_GROUP) return@mapNotNull null
                            it.chapter.uploader
                        }
                        .toSet()

                val allLanguages =
                    persistentChapters
                        .flatMap { ChapterUtil.getLanguages(it.chapter.language) }
                        .toSet()

                MangaConstants.StaticChapterData(
                    allChapters = persistentChapters,
                    missingChapters = missingChapterHolder,
                    allScanlators = allChapterScanlators.toSet(),
                    allUploaders = allChapterUploaders,
                    allLanguages = allLanguages,
                )
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    init {
        viewModelScope.launchIO {
            if (!mangaRepository.getMangaById(mangaId)!!.initialized) {
                onRefresh()
            }
        }

        // Debouncer for persisting filters to DB
        viewModelScope.launchIO {
            @OptIn(FlowPreview::class)
            _persistFilterChannel.receiveAsFlow().debounce(2000).collect {
                val manga = mangaRepository.getMangaById(mangaId)!!
                val filterState = _mangaFilterState.value ?: return@collect

                // Apply state to DB object using the consolidated logic
                filterState.applyToManga(manga, mangaDetailsPreferences)

                mangaRepository.updateManga(manga)
                mangaRepository.updateChapterFlags(manga)
                mangaRepository.updateScanlatorFilter(manga)
                mangaRepository.updateLanguageFilter(manga)
            }
        }

        initialLoad()

        viewModelScope.launchIO {
            combine(
                    combine(mangaFlow, staticChapterDataFlow, artworkFlow, ::Triple),
                    combine(
                        _mangaFilterState,
                        mangaDetailsPreferences.dynamicCovers().changes(),
                        historyFlow,
                        ::Triple,
                    ),
                ) {
                    (mangaItem, staticChapterData, artworkList),
                    (filterState, dynamicCover, history) ->
                    withContext(Dispatchers.Default) {
                        val effectiveManga =
                            if (filterState != null) {
                                val tempManga = mangaItem.toManga()
                                // Apply state to in-memory object using consolidated logic
                                filterState.applyToManga(tempManga, mangaDetailsPreferences)
                                tempManga.toMangaItem()
                            } else {
                                mangaItem
                            }

                        if (!effectiveManga.initialized) {
                            AllInfo(
                                mangaItem = MangaItem(title = effectiveManga.title),
                                artwork = createCurrentArtwork(effectiveManga),
                            )
                        } else {

                            if (dynamicCover) {
                                dynamicCoverUpdateJob?.cancel()
                                dynamicCoverUpdateJob = viewModelScope.launchIO {
                                    delay(DYNAMIC_COVER_UPDATE_DELAY_MS)
                                    val lastReadChapterId =
                                        history.maxByOrNull { it.last_read }?.chapter_id
                                    updateDynamicCover(
                                        effectiveManga = effectiveManga,
                                        lastReadChapterId = lastReadChapterId,
                                        allChapters = staticChapterData.allChapters,
                                        artworkList = artworkList,
                                    )
                                }
                            }

                            val artwork = createCurrentArtwork(effectiveManga)

                            val alternativeArtwork =
                                createAltArtwork(
                                    manga = effectiveManga,
                                    currentArtwork = artwork,
                                    dbArtwork = artworkList,
                                    useDynamicCover = dynamicCover,
                                )

                            if (
                                (staticChapterData.allScanlators.size +
                                    staticChapterData.allUploaders.size) <= 1 &&
                                    !effectiveManga.filteredScanlators.isEmpty()
                            ) {
                                val manga =
                                    effectiveManga.copy(filteredScanlators = listOf()).toManga()
                                mangaRepository.updateManga(manga)
                            }

                            val activeChapters =
                                chapterSort
                                    .getChaptersSorted(
                                        effectiveManga.toManga(),
                                        staticChapterData.allChapters,
                                    )
                                    .toList()

                            val nextUnread = getNextUnread(effectiveManga, activeChapters)

                            val allChapterInfo =
                                AllChapterInfo(
                                    nextUnread = nextUnread,
                                    activeChapters = activeChapters,
                                    allChapters = staticChapterData.allChapters,
                                    missingChapters = staticChapterData.missingChapters,
                                    allScanlators = staticChapterData.allScanlators,
                                    allUploaders = staticChapterData.allUploaders,
                                    allLanguages = staticChapterData.allLanguages,
                                )

                            val displayFilter = getChapterDisplay(effectiveManga)
                            val sortFilter = getSortFilter(effectiveManga)
                            val scanlatorFilter =
                                getChapterScanlatorFilter(
                                    effectiveManga,
                                    (allChapterInfo.allScanlators + allChapterInfo.allUploaders)
                                        .toSet(),
                                )
                            val languageFilter =
                                getChapterLanguageFilter(
                                    effectiveManga,
                                    allChapterInfo.allLanguages,
                                )
                            val chapterFilterText =
                                getFilterText(
                                    displayFilter,
                                    scanlatorFilter,
                                    languageFilter,
                                )

                            AllInfo(
                                mangaItem = effectiveManga,
                                dynamicCover = dynamicCover,
                                chapterDisplay = displayFilter,
                                chapterScanlatorFilter = scanlatorFilter,
                                chapterLanguageFilter = languageFilter,
                                chapterSortFilter = sortFilter,
                                chapterFilterText = chapterFilterText,
                                allChapterInfo = allChapterInfo,
                                artwork = artwork,
                                altArtwork = alternativeArtwork,
                            )
                        }
                    }
                }
                .distinctUntilChanged()
                .collectLatest { allInfo ->
                    _mangaDetailScreenState.update {
                        it.copy(
                            general =
                                it.general.copy(
                                    vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId),
                                    isRefreshing = allInfo.isRefreshing,
                                ),
                            manga =
                                it.manga.copy(
                                    alternativeTitles = allInfo.mangaItem.altTitles,
                                    artist = allInfo.mangaItem.artist,
                                    author = allInfo.mangaItem.author,
                                    alternativeArtwork = allInfo.altArtwork,
                                    currentArtwork = allInfo.artwork,
                                    currentDescription = allInfo.mangaItem.getDescription(),
                                    currentTitle =
                                        allInfo.mangaItem.userTitle.ifEmpty {
                                            allInfo.mangaItem.title
                                        },
                                    externalLinks = allInfo.mangaItem.externalLinks,
                                    genres = allInfo.mangaItem.genre,
                                    initialized = allInfo.mangaItem.initialized,
                                    isPornographic =
                                        allInfo.mangaItem.contentRating.equals(
                                            MdConstants.ContentRating.pornographic,
                                            ignoreCase = true,
                                        ),
                                    langFlag = allInfo.mangaItem.langFlag,
                                    missingChapters = allInfo.mangaItem.missingChapters,
                                    originalTitle = allInfo.mangaItem.title,
                                    stats =
                                        Stats(
                                            rating = allInfo.mangaItem.rating,
                                            follows = allInfo.mangaItem.users,
                                            threadId = allInfo.mangaItem.threadId,
                                            repliesCount = allInfo.mangaItem.repliesCount,
                                        ),
                                    status = allInfo.mangaItem.status,
                                    lastVolume = allInfo.mangaItem.lastVolumeNumber,
                                    lastChapter = allInfo.mangaItem.lastChapterNumber,
                                    estimatedMissingChapters =
                                        allInfo.allChapterInfo.missingChapters.estimatedChapters,
                                ),
                            chapters =
                                it.chapters.copy(
                                    activeChapters = allInfo.allChapterInfo.activeChapters,
                                    nextUnreadChapter = allInfo.allChapterInfo.nextUnread,
                                    chapterFilter = allInfo.chapterDisplay,
                                    chapterFilterText = allInfo.chapterFilterText,
                                    chapterSortFilter = allInfo.chapterSortFilter,
                                    chapterScanlatorFilter = allInfo.chapterScanlatorFilter,
                                    chapterLanguageFilter = allInfo.chapterLanguageFilter,
                                    allChapters = allInfo.allChapterInfo.allChapters,
                                    allScanlators = allInfo.allChapterInfo.allScanlators,
                                    allUploaders = allInfo.allChapterInfo.allUploaders,
                                    allLanguages = allInfo.allChapterInfo.allLanguages,
                                ),
                        )
                    }

                    if (_mangaDetailScreenState.value.general.firstLoad) {
                        _mangaDetailScreenState.update {
                            it.copy(general = it.general.copy(firstLoad = false))
                        }
                    }

                    if (allInfo.mangaItem.initialized) {
                        viewModelScope.launchIO {
                            mangaUseCases.updateMangaStatusAndMissingCount(
                                allInfo.mangaItem.toManga()
                            )
                        }
                    }
                }
        }
    }

    fun onRefresh() {
        TimberKt.d { "On Refresh called" }
        viewModelScope.launchIO {
            if (!isOnline()) {
                appSnackbarManager.showSnackbar(
                    SnackbarState(
                        messageRes = R.string.no_network_connection,
                        snackBarColor = _mangaDetailScreenState.value.general.snackbarColor,
                    )
                )
                return@launchIO
            }

            _mangaDetailScreenState.update {
                it.copy(general = it.general.copy(isRefreshing = true))
            }

            val mangaItem = mangaRepository.getMangaById(mangaId)!!.toMangaItem()

            mangaUpdateCoordinator
                .update(mangaItem = mangaItem)
                .onCompletion {
                    _mangaDetailScreenState.update {
                        it.copy(general = it.general.copy(isRefreshing = false))
                    }
                }
                .catch { e ->
                    e.message?.let {
                        appSnackbarManager.showSnackbar(
                            SnackbarState(
                                message = it,
                                snackBarColor = _mangaDetailScreenState.value.general.snackbarColor,
                            )
                        )
                    }
                }
                .collect { result ->
                    when (result) {
                        is MangaResult.Error -> {
                            appSnackbarManager.showSnackbar(
                                SnackbarState(
                                    message = result.text,
                                    messageRes = result.id,
                                    snackBarColor =
                                        _mangaDetailScreenState.value.general.snackbarColor,
                                )
                            )
                        }

                        else -> Unit
                    }
                }
        }
    }

    fun markPreviousChapters(chapterItem: ChapterItem, read: Boolean) {
        val chapterList =
            if (mangaDetailScreenState.value.general.isSearching) {
                mangaDetailScreenState.value.general.searchChapters
            } else {
                mangaDetailScreenState.value.chapters.activeChapters
            }

        val result = chapterUseCases.markPreviousChapters(chapterItem, chapterList, read)
        if (result != null) {
            val (chaptersToMark, action) = result
            markChapters(chaptersToMark, action)
        }
    }

    fun markChapters(
        chapterItems: List<ChapterItem>,
        markAction: ChapterMarkActions,
    ) {
        viewModelScope.launchIO {
            val manga = mangaRepository.getMangaById(mangaId) ?: return@launchIO
            val updatedChapterList =
                if (
                    markAction is ChapterMarkActions.PreviousRead ||
                        markAction is ChapterMarkActions.PreviousUnread
                ) {
                    when (manga.sortDescending(mangaDetailsPreferences)) {
                        true -> chapterItems
                        false ->
                            (markAction as? ChapterMarkActions.PreviousRead)?.altChapters
                                ?: (markAction as ChapterMarkActions.PreviousUnread).altChapters
                    }
                } else {
                    chapterItems
                }

            val nameRes =
                when (markAction) {
                    is ChapterMarkActions.Bookmark -> R.string.bookmarked
                    is ChapterMarkActions.UnBookmark -> R.string.removed_bookmark
                    is ChapterMarkActions.Read -> R.string.marked_as_read
                    is ChapterMarkActions.PreviousRead -> R.string.marked_as_read
                    is ChapterMarkActions.PreviousUnread -> R.string.marked_as_unread
                    is ChapterMarkActions.Unread -> R.string.marked_as_unread
                }

            chapterUseCases.markChapters(markAction, updatedChapterList)

            if (markAction.canUndo) {
                appSnackbarManager.showSnackbar(
                    SnackbarState(
                        messageRes = nameRes,
                        actionLabelRes = R.string.undo,
                        action = {
                            viewModelScope.launchIO {
                                val originalDbChapters =
                                    updatedChapterList.map { it.chapter }.map { it.toDbChapter() }
                                chapterRepository.updateChaptersProgress(originalDbChapters)
                            }
                        },
                        snackBarColor = _mangaDetailScreenState.value.general.snackbarColor,
                    )
                )
            }
        }
    }

    private fun isOnline(): Boolean = preferences.context.isOnline()

    fun initialLoad() {
        viewModelScope.launchIO {
            _mangaDetailScreenState.update {
                it.copy(
                    general =
                        it.general.copy(
                            incognitoMode = securityPreferences.incognitoMode().get(),
                            hideButtonText = mangaDetailsPreferences.hideButtonText().get(),
                            backdropSize = mangaDetailsPreferences.backdropSize().get(),
                            forcePortrait = mangaDetailsPreferences.forcePortrait().get(),
                            themeBasedOffCovers = mangaDetailsPreferences.autoThemeByCover().get(),
                            wrapAltTitles = mangaDetailsPreferences.wrapAltTitles().get(),
                            chapterSwipeRightAction =
                                mangaDetailsPreferences.chapterSwipeRightAction().get(),
                            chapterSwipeLeftAction =
                                mangaDetailsPreferences.chapterSwipeLeftAction().get(),
                        ),
                    manga =
                        it.manga.copy(
                            dynamicCovers = mangaDetailsPreferences.dynamicCovers().get()
                        ),
                )
            }
        }
    }

    private fun createInitialCurrentArtwork(): Artwork {
        val manga = runBlocking { mangaRepository.getMangaById(mangaId)!!.toMangaItem() }
        return Artwork(
            cover = manga.userCover,
            dynamicCover = manga.dynamicCover,
            originalCover = manga.coverUrl,
            mangaId = mangaId,
        )
    }

    private fun createCurrentArtwork(manga: MangaItem): Artwork {
        return Artwork(
            cover = manga.userCover,
            dynamicCover = manga.dynamicCover,
            originalCover = manga.coverUrl,
            mangaId = mangaId,
        )
    }

    private fun createAltArtwork(
        manga: MangaItem,
        currentArtwork: Artwork,
        dbArtwork: List<ArtworkImpl>,
        useDynamicCover: Boolean,
    ): List<Artwork> {
        val quality = mangaDexPreferences.coverQuality().get()

        return dbArtwork
            .map { aw ->
                Artwork(
                    mangaId = aw.mangaId,
                    cover = MdUtil.cdnCoverUrl(manga.uuid(), aw.fileName, quality),
                    volume = aw.volume,
                    description = aw.description,
                    active =
                        when {
                            // Priority 1: User custom cover
                            currentArtwork.cover.isNotBlank() ->
                                currentArtwork.cover.contains(aw.fileName)
                            // Priority 2: Dynamic cover (if enabled and available)
                            useDynamicCover && currentArtwork.dynamicCover.isNotBlank() ->
                                currentArtwork.dynamicCover.contains(aw.fileName)
                            // Priority 3: Default cover
                            else -> currentArtwork.originalCover.contains(aw.fileName)
                        },
                )
            }
            .toList()
    }

    /** Get current sort filter */
    private fun getChapterDisplay(mangaItem: MangaItem): MangaConstants.ChapterDisplay {
        val manga = mangaItem.toManga()
        val read =
            when (manga.readFilter(mangaDetailsPreferences)) {
                Manga.CHAPTER_SHOW_UNREAD -> ToggleableState.On
                Manga.CHAPTER_SHOW_READ -> ToggleableState.Indeterminate
                else -> ToggleableState.Off
            }
        val bookmark =
            when (manga.bookmarkedFilter(mangaDetailsPreferences)) {
                Manga.CHAPTER_SHOW_BOOKMARKED -> ToggleableState.On
                Manga.CHAPTER_SHOW_NOT_BOOKMARKED -> ToggleableState.Indeterminate
                else -> ToggleableState.Off
            }

        val hideTitle =
            when (manga.hideChapterTitle(mangaDetailsPreferences)) {
                true -> ToggleableState.On
                else -> ToggleableState.Off
            }

        val available =
            when (manga.availableFilter(mangaDetailsPreferences)) {
                Manga.CHAPTER_SHOW_AVAILABLE -> ToggleableState.On
                Manga.CHAPTER_SHOW_UNAVAILABLE -> ToggleableState.Indeterminate
                else -> ToggleableState.Off
            }

        val all =
            read == ToggleableState.Off &&
                bookmark == ToggleableState.Off &&
                available == ToggleableState.Off

        val matchesDefaults = mangaFilterMatchesDefault(manga)

        return MangaConstants.ChapterDisplay(
            showAll = all,
            unread = read,
            bookmarked = bookmark,
            hideChapterTitles = hideTitle,
            available = available,
            matchesGlobalDefaults = matchesDefaults,
        )
    }

    /** Used to filter sources, or scanlators/uploaders */
    private fun getChapterScanlatorFilter(
        mangaItem: MangaItem,
        allScanlators: Set<String>,
    ): MangaConstants.ScanlatorFilter {
        val scanlatorSet = mangaItem.filteredScanlators.toSet()
        val scanlatorOptions =
            allScanlators
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                .map { scanlator ->
                    MangaConstants.ScanlatorOption(
                        name = scanlator,
                        disabled = scanlator in scanlatorSet,
                    )
                }
                .toList()

        return MangaConstants.ScanlatorFilter(scanlators = scanlatorOptions.toList())
    }

    /** Used to filter sources, or scanlators/uploaders */
    private fun getChapterLanguageFilter(
        mangaItem: MangaItem,
        allLanguages: Set<String>,
    ): MangaConstants.LanguageFilter {
        val languageSet = mangaItem.filteredLanguage.toSet()
        val languageOptions =
            allLanguages
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                .map { language ->
                    MangaConstants.LanguageOption(
                        name = language,
                        disabled = language in languageSet,
                    )
                }
                .toList()

        return MangaConstants.LanguageFilter(languages = languageOptions.toList())
    }

    private fun getSortFilter(mangaItem: MangaItem): MangaConstants.SortFilter {
        val manga = mangaItem.toManga()
        val sortOrder = manga.chapterOrder(mangaDetailsPreferences)
        val status =
            when (manga.sortDescending(mangaDetailsPreferences)) {
                true -> MangaConstants.SortState.Descending
                false -> MangaConstants.SortState.Ascending
            }

        val matchesDefaults = mangaSortMatchesDefault(manga)

        return when (sortOrder) {
            Manga.CHAPTER_SORTING_SOURCE ->
                MangaConstants.SortFilter(
                    sourceOrderSort = status,
                    matchesGlobalDefaults = matchesDefaults,
                )

            Manga.CHAPTER_SORTING_UPLOAD_DATE ->
                MangaConstants.SortFilter(
                    uploadDateSort = status,
                    matchesGlobalDefaults = matchesDefaults,
                )

            else ->
                MangaConstants.SortFilter(
                    smartOrderSort = status,
                    matchesGlobalDefaults = matchesDefaults,
                )
        }
    }

    private fun getNextUnread(
        mangaItem: MangaItem,
        activeChapters: List<ChapterItem>,
    ): NextUnreadChapter {
        val nextChapter =
            chapterSort.getNextUnreadChapter(mangaItem.toManga(), activeChapters)?.chapter
        return nextChapter?.let { chapter ->
            val id =
                if (chapter.lastPageRead > 0) {
                    R.string.continue_reading_
                } else {
                    R.string.start_reading_
                }

            val chapterText =
                when {
                    chapter.volume.isNotEmpty() -> "Vol. ${chapter.volume} ${chapter.chapterText}"
                    else -> chapter.chapterText
                }

            NextUnreadChapter(
                text = UiText.StringResource(id, chapterText),
                simpleChapter = chapter,
            )
        } ?: NextUnreadChapter()
    }

    fun onSearch(searchQuery: String?) {
        viewModelScope.launchIO {
            val searchActive = searchQuery != null

            _mangaDetailScreenState.update {
                it.copy(general = it.general.copy(isSearching = searchActive))
            }

            val filteredChapters =
                when (searchActive) {
                    true -> {
                        mangaDetailScreenState.value.chapters.activeChapters.filter {
                            it.chapter.chapterTitle.contains(searchQuery, true) ||
                                it.chapter.scanlator.contains(searchQuery, true) ||
                                it.chapter.name.contains(searchQuery, true)
                        }
                    }

                    false -> emptyList()
                }

            _mangaDetailScreenState.update {
                it.copy(general = it.general.copy(searchChapters = filteredChapters.toList()))
            }
        }
    }

    fun copiedToClipboard(message: String) {
        viewModelScope.launchIO {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                appSnackbarManager.showSnackbar(
                    SnackbarState(
                        messageRes = R.string._copied_to_clipboard,
                        message = message,
                        snackBarColor = _mangaDetailScreenState.value.general.snackbarColor,
                    )
                )
            }
        }
    }

    fun setAltTitle(title: String?) {
        viewModelScope.launchNonCancellable {
            val previousTitle = mangaDetailScreenState.value.manga.currentTitle

            val dbManga =
                mangaUseCases.modifyManga.setAltTitle(mangaId, title) ?: return@launchNonCancellable

            appSnackbarManager.showSnackbar(
                SnackbarState(
                    messageRes = R.string.updated_title_to_,
                    message = dbManga.user_title,
                    actionLabelRes = R.string.undo,
                    action = { setAltTitle(previousTitle) },
                    snackBarColor = _mangaDetailScreenState.value.general.snackbarColor,
                )
            )
        }
    }

    /** Get change Sort option */
    fun changeSortOption(sortOption: SortOption?) {
        _mangaFilterState.update { current ->
            val newState = current ?: MangaFilterState()
            newState.copy(sortOption = sortOption, forceGlobal = sortOption == null)
        }
        _persistFilterChannel.trySend(Unit)
    }

    fun changeFilterOption(filterOption: MangaConstants.ChapterDisplayOptions?) {
        // 1. Update UI state optimistically
        if (filterOption != null) {
            _mangaDetailScreenState.update { state ->
                state.copy(
                    chapters =
                        state.chapters.copy(
                            chapterFilter =
                                chapterUseCases.calculateChapterFilter(
                                    state.chapters.chapterFilter,
                                    filterOption,
                                )
                        )
                )
            }
        }

        // 2. Update Local State (Accumulate values!)
        _mangaFilterState.update { current ->
            val base = current ?: MangaFilterState()

            if (filterOption == null) {
                base.copy(forceGlobal = true)
            } else {
                // Helper to get tri-state int
                fun getTriState(on: Int, off: Int): Int =
                    when (filterOption.displayState) {
                        ToggleableState.On -> on
                        ToggleableState.Indeterminate -> off
                        else -> Manga.SHOW_ALL
                    }

                when (filterOption.displayType) {
                    MangaConstants.ChapterDisplayType.All ->
                        base.copy(
                            readFilter = Manga.SHOW_ALL,
                            bookmarkedFilter = Manga.SHOW_ALL,
                            availableFilter = Manga.SHOW_ALL,
                        )

                    MangaConstants.ChapterDisplayType.Unread ->
                        base.copy(
                            readFilter =
                                getTriState(Manga.CHAPTER_SHOW_UNREAD, Manga.CHAPTER_SHOW_READ)
                        )

                    MangaConstants.ChapterDisplayType.Bookmarked ->
                        base.copy(
                            bookmarkedFilter =
                                getTriState(
                                    Manga.CHAPTER_SHOW_BOOKMARKED,
                                    Manga.CHAPTER_SHOW_NOT_BOOKMARKED,
                                )
                        )

                    MangaConstants.ChapterDisplayType.Available ->
                        base.copy(
                            availableFilter =
                                getTriState(
                                    Manga.CHAPTER_SHOW_AVAILABLE,
                                    Manga.CHAPTER_SHOW_UNAVAILABLE,
                                )
                        )

                    MangaConstants.ChapterDisplayType.HideTitles ->
                        base.copy(
                            hideChapterTitles = filterOption.displayState == ToggleableState.On
                        )
                }
            }
        }

        // 3. Queue DB write
        _persistFilterChannel.trySend(Unit)
    }

    /** Changes the filtered scanlators, if null then it resets the scanlator filter */
    fun changeScanlatorOption(scanlatorOption: MangaConstants.ScanlatorOption?) {
        viewModelScope.launchIO {
            val manga = mangaRepository.getMangaById(mangaId)!!

            val newFilteredScanlators =
                if (scanlatorOption != null) {
                    val filteredScanlators =
                        ChapterUtil.getScanlators(manga.filtered_scanlators).toMutableSet()
                    // Merge with local overrides
                    val local = _mangaFilterState.value?.scanlators
                    val effective = local?.toMutableSet() ?: filteredScanlators

                    when (scanlatorOption.disabled) {
                        true -> effective.add(scanlatorOption.name)
                        false -> effective.remove(scanlatorOption.name)
                    }
                    effective
                } else {
                    emptySet()
                }

            _mangaFilterState.update { current ->
                (current ?: MangaFilterState()).copy(scanlators = newFilteredScanlators)
            }
            _persistFilterChannel.trySend(Unit)
        }
    }

    /** Changes the filtered scanlators, if null then it resets the scanlator filter */
    fun changeLanguageOption(languageOptions: MangaConstants.LanguageOption?) {
        viewModelScope.launchIO {
            val manga = mangaRepository.getMangaById(mangaId)!!
            val newFilteredLanguages =
                if (languageOptions != null) {
                    val filteredLanguages =
                        ChapterUtil.getLanguages(manga.filtered_language).toMutableSet()
                    val local = _mangaFilterState.value?.languages
                    val effective = local?.toMutableSet() ?: filteredLanguages

                    when (languageOptions.disabled) {
                        true -> effective.add(languageOptions.name)
                        false -> effective.remove(languageOptions.name)
                    }
                    effective
                } else {
                    emptySet()
                }

            _mangaFilterState.update { current ->
                (current ?: MangaFilterState()).copy(languages = newFilteredLanguages)
            }
            _persistFilterChannel.trySend(Unit)
        }
    }

    /** Changes the filtered scanlators, if null then it resets the scanlator filter */
    fun setGlobalOption(option: MangaConstants.SetGlobal) {
        viewModelScope.launchIO {
            val manga = mangaRepository.getMangaById(mangaId)!!
            when (option) {
                MangaConstants.SetGlobal.Sort -> {
                    mangaDetailsPreferences.sortChapterOrder().set(manga.sorting)
                    mangaDetailsPreferences.chaptersDescAsDefault().set(manga.sortDescending)
                    manga.setSortToGlobal()
                }

                MangaConstants.SetGlobal.Filter -> {
                    mangaDetailsPreferences.filterChapterByRead().set(manga.readFilter)
                    mangaDetailsPreferences.filterChapterByBookmarked().set(manga.bookmarkedFilter)
                    mangaDetailsPreferences
                        .hideChapterTitlesByDefault()
                        .set(manga.hideChapterTitles)
                    mangaDetailsPreferences.filterChapterByAvailable().set(manga.availableFilter)
                    manga.setFilterToGlobal()
                }
            }
            mangaRepository.updateChapterFlags(manga)
            mangaRepository.updateScanlatorFilter(manga)
            mangaRepository.updateLanguageFilter(manga)
        }
    }

    /** Update the current artwork with the vibrant color */
    fun updateMangaColor(vibrantColor: Int) {
        viewModelScope.launchIO {
            MangaCoverMetadata.addVibrantColor(mangaId, vibrantColor)
            _mangaDetailScreenState.update {
                it.copy(
                    general =
                        it.general.copy(vibrantColor = MangaCoverMetadata.getVibrantColor(mangaId))
                )
            }
        }
    }

    /** Save the given url cover to file */
    fun saveCover(artwork: Artwork, destDir: UniFile? = null) {
        viewModelScope.launchIO {
            try {
                val directory = destDir ?: storageManager.getCoverDirectory()!!

                val destinationUri = saveCover(directory, artwork)
                preferences.context.let { context ->
                    DiskUtil.scanMedia(context, destinationUri)
                    appSnackbarManager.showSnackbar(
                        SnackbarState(
                            messageRes = R.string.cover_saved,
                            snackBarColor = _mangaDetailScreenState.value.general.snackbarColor,
                        )
                    )
                }
            } catch (e: Exception) {
                TimberKt.e(e) { "error saving cover" }
                appSnackbarManager.showSnackbar(
                    SnackbarState(
                        messageRes = R.string.error_saving_cover,
                        snackBarColor = _mangaDetailScreenState.value.general.snackbarColor,
                    )
                )
            }
        }
    }

    /** Save Cover to directory, if given a url save that specific cover */
    private fun saveCover(directory: UniFile, artwork: Artwork): Uri {
        val dbManga = runBlocking { mangaRepository.getMangaById(mangaId)!! }
        val cover =
            when (artwork.cover.isBlank() || dbManga.thumbnail_url == artwork.cover) {
                true ->
                    coverCache.getCustomCoverFile(dbManga).takeIf { it.exists() }
                        ?: coverCache.getCoverFile(dbManga.thumbnail_url)

                false -> coverCache.getCoverFile(artwork.cover)
            }

        val type =
            cover.inputStream().use { ImageUtil.findImageType(it) }
                ?: throw Exception("Not an image")

        // Build destination file.
        val fileNameNoExtension =
            listOfNotNull(dbManga.title, artwork.volume.ifEmpty { null }, dbManga.uuid())
                .joinToString("-")

        val filename = DiskUtil.buildValidFilename("$fileNameNoExtension.${type.extension}")

        val destFile = directory.createFile(filename)!!

        cover.inputStream().use { input ->
            destFile.openOutputStream().use { output -> input.copyTo(output) }
        }
        return destFile.uri
    }

    /** Set custom cover */
    fun setCover(artwork: Artwork) {
        viewModelScope.launchIO {
            val dbManga = mangaRepository.getMangaById(mangaId)!!
            coverCache.setCustomCoverToCache(dbManga, artwork.cover)
            MangaCoverMetadata.remove(mangaId)
            dbManga.user_cover = artwork.cover
            mangaRepository.updateManga(dbManga)
        }
    }

    /** Reset cover */
    fun resetCover() {
        viewModelScope.launchIO {
            val dbManga = mangaRepository.getMangaById(mangaId)!!
            coverCache.deleteCustomCover(dbManga)
            MangaCoverMetadata.remove(mangaId)
            dbManga.user_cover = null
            mangaRepository.updateManga(dbManga)
        }
    }

    /**
     * share the cover that is written in the destination folder. If a url is passed in then share
     * that one instead of the manga thumbnail url one
     */
    suspend fun shareCover(destDir: UniFile, artwork: Artwork): Uri? {
        return withIOContext {
            try {
                saveCover(destDir, artwork)
            } catch (e: java.lang.Exception) {
                TimberKt.e(e) { "share manga cover exception" }
                null
            }
        }
    }

    fun openComment(context: Context, chapterId: String) {
        viewModelScope.launchIO {
            when (!isOnline()) {
                true ->
                    appSnackbarManager.showSnackbar(
                        SnackbarState(
                            message = "No network connection, cannot open comments",
                            snackBarColor = _mangaDetailScreenState.value.general.snackbarColor,
                        )
                    )

                false -> {
                    _mangaDetailScreenState.update {
                        it.copy(general = it.general.copy(isRefreshing = true))
                    }
                    val threadId =
                        sourceManager.mangaDex
                            .getChapterCommentId(chapterId)
                            .onErr { TimberKt.e { it.message() } }
                            .getOrElse { null }
                    _mangaDetailScreenState.update {
                        it.copy(general = it.general.copy(isRefreshing = false))
                    }
                    if (threadId == null) {
                        appSnackbarManager.showSnackbar(
                            SnackbarState(messageRes = R.string.comments_unavailable)
                        )
                    } else {
                        val url = MdConstants.forumUrl + threadId
                        if (openLinksInBrowser()) {
                            context.openInBrowser(url, forceDefaultBrowser = true)
                        } else {
                            context.openInWebView(url, title = "Comments")
                        }
                    }
                }
            }
        }
    }

    fun getManga(): Manga {
        return runBlocking { mangaRepository.getMangaById(mangaId)!! }
    }

    fun getChapterUrl(chapter: SimpleChapter): String {
        return chapter.getHttpSource(sourceManager).getChapterUrl(chapter)
    }

    fun openLinksInBrowser(): Boolean = preferences.openLinksInBrowser().get()

    fun blockScanlator(blockType: MangaConstants.BlockType, name: String) {
        viewModelScope.launchIO {
            mangaUseCases.blockScanlator.block(blockType, name, this)
            appSnackbarManager.showSnackbar(
                SnackbarState(
                    messageRes = R.string.globally_blocked_group_,
                    message = name,
                    actionLabelRes = R.string.undo,
                    action = {
                        viewModelScope.launchIO {
                            mangaUseCases.blockScanlator.unblock(blockType, name)
                        }
                    },
                    snackBarColor = _mangaDetailScreenState.value.general.snackbarColor,
                )
            )
        }
    }

    fun updateSnackbarColor(snackbarColor: SnackbarColor) {
        viewModelScope.launch {
            _mangaDetailScreenState.update {
                it.copy(general = it.general.copy(snackbarColor = snackbarColor))
            }
        }
    }

    private fun mangaSortMatchesDefault(manga: Manga): Boolean {
        return (manga.sortDescending == mangaDetailsPreferences.chaptersDescAsDefault().get() &&
            manga.sorting == mangaDetailsPreferences.sortChapterOrder().get()) ||
            !manga.usesLocalSort
    }

    private fun mangaFilterMatchesDefault(manga: Manga): Boolean {
        return (manga.readFilter == mangaDetailsPreferences.filterChapterByRead().get() &&
            manga.bookmarkedFilter == mangaDetailsPreferences.filterChapterByBookmarked().get() &&
            manga.hideChapterTitles ==
                mangaDetailsPreferences.hideChapterTitlesByDefault().get()) &&
            manga.availableFilter == mangaDetailsPreferences.filterChapterByAvailable().get() ||
            !manga.usesLocalFilter
    }

    // --- Consolidated logic to apply filter state to a Manga object ---
    private fun MangaFilterState.applyToManga(manga: Manga, prefs: MangaDetailsPreferences) {
        // 1. Apply Sort
        if (sortOption != null) {
            sortOption.applyToManga(manga)
        } else if (forceGlobal) {
            manga.setSortToGlobal()
        }

        // 2. Handle Global Reset (Exit Early)
        if (forceGlobal) {
            manga.setFilterToGlobal()
            // IMPORTANT: Even if resetting global, we might still have scanlator filters!
            // Don't return here if you want scanlators to persist independently of "Global
            // Sort/Filter" mode.
            // However, historically scanlators are independent of "Global Chapter Filters".
            // So we should continue execution, but we need to be careful not to overwrite the
            // global reset above.
            // For now, assuming scanlators are independent of the "Force Global" sort/filter
            // toggle:
        }

        // 3. Apply Explicit Overrides
        if (readFilter != null) manga.readFilter = readFilter
        if (bookmarkedFilter != null) manga.bookmarkedFilter = bookmarkedFilter
        if (availableFilter != null) manga.availableFilter = availableFilter
        if (hideChapterTitles != null) {
            manga.displayMode =
                if (hideChapterTitles) Manga.CHAPTER_DISPLAY_NUMBER else Manga.CHAPTER_DISPLAY_NAME
        }

        // 4. Check if we need to enable Local Mode
        val hasFilterOverrides =
            readFilter != null ||
                bookmarkedFilter != null ||
                availableFilter != null ||
                hideChapterTitles != null

        // 5. Handle Transition (Global -> Local)
        if (!forceGlobal && hasFilterOverrides) {
            val wasGlobal = !manga.usesLocalFilter
            manga.setFilterToLocal()

            if (wasGlobal) {
                if (readFilter == null) manga.readFilter = prefs.filterChapterByRead().get()
                if (bookmarkedFilter == null)
                    manga.bookmarkedFilter = prefs.filterChapterByBookmarked().get()
                if (availableFilter == null)
                    manga.availableFilter = prefs.filterChapterByAvailable().get()
                if (hideChapterTitles == null) manga.hideChapterTitle(prefs)
            }
        }

        // 6. Apply Scanlators & Languages (THIS WAS MISSING)
        this.scanlators?.let {
            manga.filtered_scanlators =
                if (it.isEmpty()) null else ChapterUtil.getScanlatorString(it)
        }
        this.languages?.let {
            manga.filtered_language = if (it.isEmpty()) null else ChapterUtil.getLanguageString(it)
        }
    }

    private suspend fun updateDynamicCover(
        effectiveManga: MangaItem,
        lastReadChapterId: Long?,
        allChapters: List<ChapterItem>,
        artworkList: List<ArtworkImpl>,
    ) {
        if (artworkList.isEmpty()) return

        // 1. Flatten the target volume derivation
        var volumeFromAggregate: String? = null
        if (lastReadChapterId != null) {
            val chapter = allChapters.find { it.chapter.id == lastReadChapterId }?.chapter
            if (chapter != null) {
                val mangaDexChapterId = chapter.mangaDexChapterId
                val chapterNumber = chapter.chapterNumber
                val chapterNumberStr =
                    if (chapterNumber % 1 == 0f) {
                        chapterNumber.toInt().toString()
                    } else {
                        chapterNumber.toString()
                    }

                val mangaId = effectiveManga.id
                var dbAggregate = mangaAggregateRepository.getMangaAggregate(mangaId)

                if (dbAggregate == null) {
                    mangaUseCases.updateMangaAggregate(
                        effectiveManga.id,
                        effectiveManga.url,
                        effectiveManga.favorite,
                    )
                }

                dbAggregate = mangaAggregateRepository.getMangaAggregate(mangaId)

                val volumes: Map<String, AggregateVolume>? =
                    if (dbAggregate != null) {
                        Json.parseToJsonElement(dbAggregate.volumes).asMdMap<AggregateVolume>()
                    } else {
                        null
                    }

                if (volumes != null) {
                    for ((_, volumeInfo) in volumes) {
                        val chaptersInVolume = volumeInfo.chapters.values
                        val matchById = chaptersInVolume.any {
                            it.id == mangaDexChapterId || it.others.contains(mangaDexChapterId)
                        }
                        val matchByNumber = chaptersInVolume.any { it.chapter == chapterNumberStr }

                        if (matchById || matchByNumber) {
                            volumeFromAggregate = volumeInfo.volume
                            break
                        }
                    }
                }
            }
        }

        val targetVolume =
            lastReadChapterId?.let { chapterId ->
                val volume =
                    volumeFromAggregate
                        ?: allChapters.find { it.chapter.id == chapterId }?.chapter?.volume

                when {
                    volume.isNullOrBlank() -> "Vol.1"
                    volume.startsWith("Vol", ignoreCase = true) -> volume
                    else -> "Vol.$volume"
                }
            } ?: "Vol.1"

        val matchedArt = artworkList.firstOrNull { it.volume == targetVolume }

        val dynamicArt =
            matchedArt
                ?: run {
                    // Fallback: If no read history and "Vol.1" is missing, find the lowest numeric
                    // volume
                    if (lastReadChapterId == null) {
                        artworkList.minByOrNull { art ->
                            // Strip non-numeric characters (like "Vol.") so toFloatOrNull() works
                            art.volume.replace(Regex("[^0-9.]"), "").toFloatOrNull()
                                ?: Float.MAX_VALUE
                        }
                    } else {
                        null
                    }
                }
                ?: return // Exit entirely if we still have no artwork to apply

        // 3. Apply the update
        val quality = mangaDexPreferences.coverQuality().get()
        val url = MdUtil.cdnCoverUrl(effectiveManga.uuid(), dynamicArt.fileName, quality)

        if (url != effectiveManga.dynamicCover) {
            val dbManga = effectiveManga.copy(dynamicCover = url).toManga()
            mangaRepository.updateManga(dbManga)
        }
    }
}

private data class AllInfo(
    val mangaItem: MangaItem,
    val dynamicCover: Boolean = false,
    val isRefreshing: Boolean = false,
    val mangaStatusCompleted: Boolean = false,
    val chapterDisplay: MangaConstants.ChapterDisplay = MangaConstants.ChapterDisplay(),
    val chapterScanlatorFilter: MangaConstants.ScanlatorFilter = MangaConstants.ScanlatorFilter(),
    val chapterLanguageFilter: MangaConstants.LanguageFilter = MangaConstants.LanguageFilter(),
    val chapterSortFilter: MangaConstants.SortFilter = MangaConstants.SortFilter(),
    val chapterFilterText: String = "",
    val allChapterInfo: AllChapterInfo = AllChapterInfo(),
    val artwork: Artwork,
    val altArtwork: List<Artwork> = listOf(),
)

private data class AllChapterInfo(
    val nextUnread: NextUnreadChapter = NextUnreadChapter(),
    val missingChapters: MissingChapterHolder = MissingChapterHolder(),
    val activeChapters: List<ChapterItem> = listOf(),
    val allChapters: List<ChapterItem> = listOf(),
    val allScanlators: Set<String> = setOf(),
    val allUploaders: Set<String> = setOf(),
    val allLanguages: Set<String> = setOf(),
)
