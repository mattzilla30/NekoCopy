package org.nekomanga.presentation.screens.feed

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapError
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.scanlatorList
import eu.kanade.tachiyomi.util.manga.toDisplayManga
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.usecases.chapters.ChapterUseCases
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedRepository(
    private val chapterRepository: ChapterRepository = Injekt.get(),
    private val historyRepository: HistoryRepository = Injekt.get(),
    private val mangaRepository: MangaRepository = Injekt.get(),
    private val chapterUseCases: ChapterUseCases = Injekt.get(),
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
) {

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val bySeriesSet = mutableSetOf<Long>()

    suspend fun getUpdatedFeedMangaForHistoryBySeries(feedManga: FeedManga): FeedManga {
        val blockedGroups = mangaDexPreferences.blockedGroups().get()
        val blockedUploaders = mangaDexPreferences.blockedUploaders().get()

        val chapterHistories = historyRepository.getChapterHistoryByMangaId(feedManga.mangaId)
        val simpleChapters =
            chapterHistories
                .mapNotNull { chpHistory ->
                    val chapter = chpHistory.chapter
                    if (blockedGroups.isNotEmpty() || blockedUploaders.isNotEmpty()) {
                        if (
                            !chapterUseCases.validateChapterNotBlocked(
                                chapter.scanlatorList(),
                                chapter.uploader,
                                blockedGroups,
                                blockedUploaders,
                            )
                        ) {
                            return@mapNotNull null
                        }
                    }

                    chapter.toSimpleChapter(chpHistory.history.last_read)?.toChapterItem()
                }
                .toList()

        return feedManga.copy(chapters = simpleChapters)
    }

    suspend fun getHistoryPage(
        searchQuery: String = "",
        offset: Int,
        limit: Int = FeedViewModel.HISTORY_ENDLESS_LIMIT,
        group: FeedHistoryGroup,
    ): Result<Pair<Boolean, List<FeedManga>>, ResultError.Generic> {
        if (offset > 0) {
            delay(300L)
        }
        return com.github.michaelbull.result
            .runCatching {
                val chapters =
                    when (group) {
                        FeedHistoryGroup.Series -> {
                            if (offset == 0) {
                                bySeriesSet.clear()
                            }
                            val recentManga =
                                historyRepository.getRecentMangaLimit(
                                    search = searchQuery,
                                    offset = offset,
                                    limit = limit,
                                )
                            val mangaIds = recentManga.mapNotNull { it.manga.id }.distinct()
                            val allHistories =
                                historyRepository.getChapterHistoryByMangaIds(mangaIds)
                            val historiesByMangaId = allHistories.groupBy { it.manga.id }

                            recentManga.mapNotNull { history ->
                                history.manga.id ?: return@mapNotNull null
                                history.chapter.id ?: return@mapNotNull null
                                if (bySeriesSet.contains(history.manga.id)) {
                                    return@mapNotNull null
                                }

                                val chapterHistories =
                                    (historiesByMangaId[history.manga.id!!] ?: emptyList()).take(25)
                                val chapterItems =
                                    chapterHistories
                                        .mapNotNull { chpHistory ->
                                            ChapterItem(
                                                chpHistory.chapter.toSimpleChapter(
                                                    chpHistory.history.last_read
                                                )!!
                                            )
                                        }
                                        .toList()

                                bySeriesSet.add(history.manga.id!!)

                                FeedManga(
                                    mangaId = history.manga.id!!,
                                    mangaTitle = history.manga.displayTitle(),
                                    date = history.history.last_read,
                                    artwork = history.manga.toDisplayManga().currentArtwork,
                                    chapters = chapterItems,
                                )
                            }
                        }
                        FeedHistoryGroup.Day,
                        FeedHistoryGroup.Week -> {
                            val pattern =
                                when (group == FeedHistoryGroup.Week) {
                                    true -> "yyyy-w"
                                    false -> "yyyy-MM-dd"
                                }
                            val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
                            val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) % 7 + 1
                            dateFormat.calendar.firstDayOfWeek = dayOfWeek
                            historyRepository
                                .getRecentHistoryUngrouped(
                                    search = searchQuery,
                                    offset = offset,
                                    limit = limit,
                                )
                                .groupBy {
                                    val date = it.history.last_read
                                    it.manga to
                                        (if (date <= 0L) "-1" else dateFormat.format(Date(date)))
                                }
                                .mapNotNull { (manga, matches) ->
                                    val chapterItems =
                                        matches
                                            .map {
                                                ChapterItem(
                                                    it.chapter.toSimpleChapter(
                                                        it.history.last_read
                                                    )!!
                                                )
                                            }
                                            .toList()
                                    FeedManga(
                                        mangaId = manga.first.id!!,
                                        mangaTitle = manga.first.displayTitle(),
                                        date = 0L,
                                        artwork = manga.first.toDisplayManga().currentArtwork,
                                        chapters = chapterItems,
                                    )
                                }
                        }
                        else -> {
                            historyRepository
                                .getRecentHistoryUngrouped(
                                    search = searchQuery,
                                    offset = offset,
                                    limit = limit,
                                )
                                .mapNotNull {
                                    it.manga.id ?: return@mapNotNull null
                                    it.chapter.id ?: return@mapNotNull null
                                    val chapterItem =
                                        ChapterItem(
                                            it.chapter.toSimpleChapter(it.history.last_read)!!
                                        )
                                    it.history.last_read
                                    FeedManga(
                                        mangaId = it.manga.id!!,
                                        mangaTitle = it.manga.displayTitle(),
                                        date = it.history.last_read,
                                        artwork = it.manga.toDisplayManga().currentArtwork,
                                        chapters = listOf(chapterItem),
                                    )
                                }
                        }
                    }

                Pair(chapters.isNotEmpty(), chapters)
            }
            .mapError { err ->
                TimberKt.e(err)
                ResultError.Generic("Error : ${err.message}")
            }
    }

    suspend fun deleteAllHistory() {
        historyRepository.deleteAllHistory()
    }

    suspend fun deleteAllHistoryForManga(mangaId: Long) {
        val history = historyRepository.getHistoryByMangaId(mangaId)
        history.forEach {
            it.last_read = 0L
            it.time_read = 0L
        }
        historyRepository.upsertHistoryList(history)
    }

    suspend fun deleteHistoryForChapter(chapterUrl: String) {
        val history = historyRepository.getHistoryByChapterUrl(chapterUrl)
        history ?: return
        history.last_read = 0L
        history.time_read = 0L
        historyRepository.upsertHistory(history)
    }

    /** Marks the chapter and returns it as stored afterwards. */
    suspend fun markChapter(chapterItem: ChapterItem, markAction: ChapterMarkActions): ChapterItem {
        chapterUseCases.markChapters(markAction, listOf(chapterItem))

        val simpleChapter =
            chapterRepository.getChapterById(chapterItem.chapter.id)!!.toSimpleChapter()!!
        return chapterItem.copy(chapter = simpleChapter)
    }

    companion object {
        suspend fun getRecentlyReadManga(): List<Manga> {
            val feedRepository = FeedRepository()
            val page = feedRepository.getHistoryPage(offset = 0, group = FeedHistoryGroup.Series)
            val feedMangaList = page.get()?.second ?: return emptyList()
            val mangaIds = feedMangaList.map { it.mangaId }
            val mangas = feedRepository.mangaRepository.getMangaByIds(mangaIds)
            val mangasMap = mangas.associateBy { it.id!! }
            return mangaIds.mapNotNull { mangasMap[it] }
        }
    }
}
