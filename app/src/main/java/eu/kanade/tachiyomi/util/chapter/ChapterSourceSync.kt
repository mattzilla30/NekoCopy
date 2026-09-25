package eu.kanade.tachiyomi.util.chapter

import androidx.room.withTransaction
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.model.SChapter
import java.util.Date
import java.util.TreeSet
import org.nekomanga.data.database.AppDatabase
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Helper method for syncing the list of chapters from the source with the ones from the database.
 *
 * @param rawSourceChapters the MangaDex chapters for the manga.
 * @param manga the manga of the chapters.
 * @return a pair of new insertions and deletions.
 */
suspend fun syncChaptersWithSource(
    appDatabase: AppDatabase,
    chapterRepository: ChapterRepository,
    mangaRepository: MangaRepository,
    rawSourceChapters: List<SChapter>,
    manga: Manga,
): Pair<List<Chapter>, List<Chapter>> {
    val downloadManager: DownloadManager = Injekt.get()

    val sourceChapters = rawSourceChapters.mapIndexed { i, sChapter ->
        Chapter.create().apply {
            copyFrom(sChapter)
            manga_id = manga.id
            source_order = i
        }
    }

    // Chapters from db.
    val dbChapters = chapterRepository.getChaptersForManga(manga.id!!)
    val dbChaptersByUrl = dbChapters.associateBy { it.url }
    val sourceChaptersByUrl = sourceChapters.associateBy { it.url }

    val finalChapters =
        reorderChapters(sourceChapters).mapIndexed { i, chapter ->
            Chapter.create().apply {
                copyFrom(chapter)
                TimberKt.d {
                    "ChapterSourceSync ${this.scanlator} ${this.chapter_txt} sourceOrder=${this.source_order} smartOrder=${i}"
                }
                smart_order = i
            }
        }

    // Chapters from the source not in db.
    val toAdd = mutableListOf<Chapter>()

    // Chapters whose metadata have changed.
    val toChange = mutableListOf<Chapter>()

    for (sourceChapter in finalChapters) {
        val dbChapter = dbChaptersByUrl[sourceChapter.url]

        // Add the chapter if not in db already, or update if the metadata changed.

        if (dbChapter == null) {
            toAdd.add(sourceChapter)
        } else {
            ChapterRecognition.parseChapterNumber(sourceChapter, manga)
            if (shouldUpdateDbChapter(dbChapter, sourceChapter)) {
                if (
                    dbChapter.name != sourceChapter.name &&
                        downloadManager.isChapterDownloaded(dbChapter, manga)
                ) {
                    downloadManager.renameChapter(manga, dbChapter, sourceChapter)
                }
                dbChapter.scanlator = sourceChapter.scanlator
                dbChapter.uploader = sourceChapter.uploader
                dbChapter.name = sourceChapter.name
                dbChapter.vol = sourceChapter.vol
                dbChapter.chapter_txt = sourceChapter.chapter_txt
                dbChapter.chapter_title = sourceChapter.chapter_title
                if (sourceChapter.date_upload != 0L) {
                    dbChapter.date_upload = sourceChapter.date_upload
                }
                dbChapter.chapter_number = sourceChapter.chapter_number
                dbChapter.mangadex_chapter_id = sourceChapter.mangadex_chapter_id
                dbChapter.language = sourceChapter.language
                dbChapter.isUnavailable = sourceChapter.isUnavailable
                dbChapter.source_order = sourceChapter.source_order
                dbChapter.smart_order = sourceChapter.smart_order
                toChange.add(dbChapter)
            }
        }
    }
    // Recognize number for new chapters.
    toAdd.forEach { ChapterRecognition.parseChapterNumber(it, manga) }

    // Chapters from the db not in the source.
    var toDelete = dbChapters.filterNot { dbChapter -> sourceChaptersByUrl[dbChapter.url] != null }

    val dupes =
        dbChapters
            .groupBy { it.url }
            .mapNotNull { entry ->
                if (entry.value.size > 1) {
                    entry.value.firstOrNull { !it.read } ?: entry.value.first()
                } else {
                    null
                }
            }
            .toMutableList()
    if (dupes.isNotEmpty()) {
        dupes.addAll(toDelete)
        toDelete = dupes.toList()
    }

    // Return if there's nothing to add, delete or change, avoiding unnecessary db transactions.
    if (toAdd.isEmpty() && toDelete.isEmpty() && toChange.isEmpty()) {
        val topChapters = dbChapters.sortedByDescending { it.date_upload }.take(4)
        val newestDate = topChapters.getOrNull(0)?.date_upload ?: 0L

        // Recalculate update rate if unset and enough chapters are present
        if (manga.next_update == 0L && topChapters.size > 1) {
            var delta = 0L
            for (i in 0 until topChapters.size - 1) {
                delta += (topChapters[i].date_upload - topChapters[i + 1].date_upload)
            }
            delta /= topChapters.size - 1
            manga.next_update = newestDate + delta
            mangaRepository.updateNextUpdated(manga.id!!, newestDate + delta)
        }

        if (newestDate != 0L && newestDate != manga.last_update) {
            manga.last_update = newestDate
            mangaRepository.updateLastUpdated(manga.id!!, newestDate)
        }
        return Pair(emptyList(), emptyList())
    }

    val readded = mutableListOf<Chapter>()

    appDatabase.withTransaction {
        val deletedChapterNumbers = TreeSet<Float>()
        val deletedReadChapterNumbers = TreeSet<Float>()
        if (toDelete.isNotEmpty()) {
            for (c in toDelete) {
                if (c.read) {
                    deletedReadChapterNumbers.add(c.chapter_number)
                }
                deletedChapterNumbers.add(c.chapter_number)
            }
            chapterRepository.deleteChapters(toDelete)
        }

        if (toAdd.isNotEmpty()) {
            // Set the date fetch for new items in reverse order to allow another sorting method.
            // Sources MUST return the chapters from most to less recent, which is common.
            var now = Date().time

            for (i in toAdd.indices.reversed()) {
                val chapter = toAdd[i]
                chapter.date_fetch = now++
                if (chapter.isRecognizedNumber && chapter.chapter_number in deletedChapterNumbers) {
                    // Try to mark already read chapters as read when the source deletes them
                    if (chapter.chapter_number in deletedReadChapterNumbers) {
                        chapter.read = true
                    }
                    // Try to to use the fetch date it originally had to not pollute 'Updates' tab
                    toDelete
                        .filter { it.chapter_number == chapter.chapter_number }
                        .minByOrNull { it.date_fetch }
                        ?.let { chapter.date_fetch = it.date_fetch }

                    readded.add(chapter)
                }
            }
            val chapterIds = chapterRepository.insertChapters(toAdd)
            toAdd.forEachIndexed { index, chapter -> chapter.id = chapterIds[index] }
        }

        if (toChange.isNotEmpty()) {
            chapterRepository.updateChapters(toChange)
        }
        val topChapters =
            chapterRepository
                .getChaptersForManga(manga.id!!)
                .sortedByDescending { it.date_upload }
                .take(4)
        // Recalculate next update since chapters were changed
        if (topChapters.size > 1) {
            var delta = 0L
            for (i in 0 until topChapters.size - 1) {
                delta += (topChapters[i].date_upload - topChapters[i + 1].date_upload)
            }
            delta /= topChapters.size - 1
            manga.next_update = topChapters[0].date_upload + delta
            mangaRepository.updateNextUpdated(manga.id!!, topChapters[0].date_upload + delta)
        }

        // Set this manga as updated since chapters were changed
        val newestChapter = topChapters.getOrNull(0)
        val dateFetch = newestChapter?.date_upload ?: manga.last_update
        if (dateFetch == 0L) {
            if (toAdd.isNotEmpty()) {
                manga.last_update = Date().time
            }
        } else {
            manga.last_update = dateFetch
        }
        mangaRepository.updateLastUpdated(manga.id!!, manga.last_update)
    }
    val newChapters = toAdd.subtract(readded.toSet()).toList().filter { !it.isUnavailable }

    return Pair(newChapters, toDelete - readded.toSet())
}

// checks if the chapter in db needs updated
private fun shouldUpdateDbChapter(dbChapter: Chapter, sourceChapter: Chapter): Boolean {
    return dbChapter.scanlator != sourceChapter.scanlator ||
        dbChapter.uploader != sourceChapter.uploader ||
        dbChapter.name != sourceChapter.name ||
        (dbChapter.date_upload != sourceChapter.date_upload && sourceChapter.date_upload != 0L) ||
        dbChapter.chapter_number != sourceChapter.chapter_number ||
        dbChapter.vol != sourceChapter.vol ||
        dbChapter.chapter_title != sourceChapter.chapter_title ||
        dbChapter.chapter_txt != sourceChapter.chapter_txt ||
        dbChapter.mangadex_chapter_id != sourceChapter.mangadex_chapter_id ||
        dbChapter.language != sourceChapter.language ||
        dbChapter.isUnavailable != sourceChapter.isUnavailable ||
        dbChapter.source_order != sourceChapter.source_order ||
        dbChapter.smart_order != sourceChapter.smart_order
}
