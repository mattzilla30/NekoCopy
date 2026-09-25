package eu.kanade.tachiyomi.ui.manga

import androidx.room.withTransaction
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.util.chapter.getChapterNum
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.manga.MangaShortcutManager
import eu.kanade.tachiyomi.util.manga.shouldDownloadNewChapters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.data.database.AppDatabase
import org.nekomanga.data.database.repository.ArtworkRepository
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.ScanlatorGroupRepository
import org.nekomanga.data.database.repository.UploaderRepository
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.manga.MangaItem
import org.nekomanga.domain.manga.toManga
import org.nekomanga.domain.manga.toMangaItem
import org.nekomanga.domain.manga.uuid
import org.nekomanga.domain.network.message
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.manga.MangaUseCases
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * Class that updates the database with the manga, chapter, information from the source returning a
 * MangaResult at each stage of the process
 */
class MangaUpdateCoordinator {
    private val appDatabase: AppDatabase by injectLazy()

    private val artworkRepository: ArtworkRepository by injectLazy()

    private val categoryRepository: CategoryRepository by injectLazy()

    private val chapterRepository: ChapterRepository by injectLazy()
    private val mangaRepository: MangaRepository by injectLazy()

    private val scanlatorGroupRepository: ScanlatorGroupRepository by injectLazy()

    private val uploaderRepository: UploaderRepository by injectLazy()

    private val preferences: PreferencesHelper by injectLazy()

    private val mangaDexPreferences: MangaDexPreferences by injectLazy()

    private val coverCache: CoverCache by injectLazy()
    private val sourceManager: SourceManager by lazy { Injekt.get() }
    private val downloadManager: DownloadManager by injectLazy()
    private val mangaShortcutManager: MangaShortcutManager by injectLazy()
    private val mangaUseCases: MangaUseCases by injectLazy()

    fun update(mangaItem: MangaItem) = channelFlow {
        if (!sourceManager.mangaDex.checkIfUp()) {
            send(MangaResult.Error(R.string.site_down))
            return@channelFlow
        }

        TimberKt.d { "Starting update for ${mangaItem.title}" }

        val mangaWasInitialized = mangaItem.initialized

        // Run manga details and chapter updates in parallel.
        try {
            coroutineScope {
                launch { updateMangaDetailsAndPersist(mangaItem) }
                launch { updateChapters(mangaItem, mangaWasInitialized) }
            }
        } catch (e: UpdateError) {
            return@channelFlow
        }

        send(MangaResult.Success)
    }
        .flowOn(Dispatchers.IO)

    /** Fetches and persists manga details and artwork from the source. */
    private suspend fun ProducerScope<MangaResult>.updateMangaDetailsAndPersist(
        mangaItem: MangaItem
    ) {
        sourceManager.mangaDex
            .getMangaDetails(mangaItem.uuid())
            .onErr {
                send(MangaResult.Error(text = "Error getting manga from MangaDex"))
                throw UpdateError()
            }
            .onOk { (networkManga, sourceArtwork) ->
                val currentManga = mangaItem.toManga()
                currentManga.copyFrom(networkManga)
                currentManga.initialized = true

                var updatedMangaItem = currentManga.toMangaItem()

                if (
                    updatedMangaItem.userTitle.isNotEmpty() &&
                        updatedMangaItem.userTitle != updatedMangaItem.title &&
                        updatedMangaItem.userTitle !in updatedMangaItem.altTitles
                ) {
                    updatedMangaItem = updatedMangaItem.copy(userTitle = "")
                }

                if (
                    networkManga.thumbnail_url != null &&
                        networkManga.thumbnail_url != mangaItem.coverUrl
                ) {
                    coverCache.deleteFromCache(mangaItem.coverUrl, mangaItem.favorite)
                }

                val mangaForDb = updatedMangaItem.toManga()

                mangaUseCases.updateMangaAggregate(
                    mangaForDb.id!!,
                    mangaForDb.url,
                    mangaForDb.favorite,
                )

                appDatabase.withTransaction {
                    mangaRepository.updateManga(mangaForDb)
                    send(MangaResult.UpdatedManga)

                    if (sourceArtwork.isNotEmpty()) {
                        val artworkImpls = sourceArtwork.map {
                            it.toArtworkImpl(updatedMangaItem.id)
                        }
                        artworkRepository.deleteArtworkByMangaId(mangaForDb.id!!)
                        artworkRepository.insertArtworks(artworkImpls)
                        send(MangaResult.UpdatedArtwork)
                    }
                }
            }
    }

    private suspend fun ProducerScope<MangaResult>.updateChapters(
        mangaItem: MangaItem,
        mangaWasAlreadyInitialized: Boolean,
    ) {
        val manga = mangaItem.toManga()

        val allChapters = fetchChapters(manga)
        val (newChapters, removedChapters) =
            syncChaptersWithSource(
                appDatabase = appDatabase,
                chapterRepository = chapterRepository,
                mangaRepository = mangaRepository,
                rawSourceChapters = allChapters,
                manga = manga,
            )

        if (newChapters.isNotEmpty()) {
            if (
                preferences.downloadNewChapters().get() &&
                    mangaWasAlreadyInitialized &&
                    manga.shouldDownloadNewChapters(categoryRepository, preferences)
            ) {
                val chaptersToDownload =
                    newChapters
                        .mapNotNull { it.toSimpleChapter()?.toChapterItem() }
                        .sortedBy { it.chapter.chapterNumber }
                downloadChapters(manga, chaptersToDownload)
            }
            mangaShortcutManager.updateShortcuts()
        }

        if (removedChapters.isNotEmpty()) {
            send(MangaResult.ChaptersRemoved(removedChapters.mapNotNull { it.id }))
        }

        send(MangaResult.UpdatedChapters)
    }

    /** Fetches the chapter list from MangaDex, sorted by chapter number. */
    private suspend fun ProducerScope<MangaResult>.fetchChapters(manga: Manga): List<SChapter> =
        sourceManager.mangaDex
            .fetchChapterList(manga)
            .onErr {
                send(MangaResult.Error(text = "MangaDex chapter fetch failed: ${it.message()}"))
                throw UpdateError()
            }
            .getOrElse { emptyList() }
            .sortedWith(compareBy { getChapterNum(it) })

    /** Filters and downloads the given list of chapters. */
    fun downloadChapters(manga: Manga, chapters: List<ChapterItem>) {
        val blockedGroups = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

        val chaptersToDownload = chapters.mapNotNull { item ->
            if (isChapterDownloadable(item, blockedGroups, blockedUploaders)) {
                item.chapter.toDbChapter()
            } else {
                null
            }
        }

        downloadManager.downloadChapters(manga, chaptersToDownload)
    }

    private fun isChapterDownloadable(
        item: ChapterItem,
        blockedGroups: Set<String>,
        blockedUploaders: Set<String>,
    ): Boolean {
        if (item.isDownloaded) return false
        val scanlators = item.chapter.scanlatorList()
        if (scanlators.any { it in blockedGroups }) return false

        // Download if it has a group OR if the uploader isn't blocked for "no group" chapters
        val isNoGroup = Constants.NO_GROUP in scanlators
        return !isNoGroup || item.chapter.uploader !in blockedUploaders
    }

    suspend fun updateGroup(group: String) {
        sourceManager.mangaDex.getScanlatorGroup(group).onOk {
            val scanlatorGroupImpl = it.toScanlatorGroupImpl()
            if (group == scanlatorGroupImpl.name) {
                scanlatorGroupRepository.insertScanlatorGroups(listOf(scanlatorGroupImpl))
            }
        }
    }

    suspend fun updateUploader(uploader: String) {
        sourceManager.mangaDex.getUploader(uploader).onOk {
            uploaderRepository.insertUploaders(listOf(it.toUploaderImpl()))
        }
    }
}

/** Represents the state of the manga update process. */
sealed class MangaResult {
    class Error(val id: Int? = null, val text: String? = null) : MangaResult()

    object UpdatedManga : MangaResult()

    object UpdatedArtwork : MangaResult()

    object UpdatedChapters : MangaResult()

    class ChaptersRemoved(val chapterIdsRemoved: List<Long>) : MangaResult()

    object Success : MangaResult()
}

private class UpdateError : Exception()
