package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.model.MangaChapter

@Dao
interface ChapterDao {
    companion object {
        private const val RECENT_CHAPTERS_QUERY =
            """
        SELECT manga.*,
               ${QueryColumns.CHAPTER}
        FROM manga JOIN chapters ON manga.id = chapters.manga_id
        WHERE manga.favorite = 1
        AND chapters.date_fetch > manga.date_added
        AND LOWER(manga.title) LIKE :search
        AND chapters.unavailable = 0
        AND (chapters.scanlator IS NULL OR chapters.scanlator != 'Local')
        ORDER BY
            CASE WHEN :sortByFetched = 1 THEN chapters.date_fetch ELSE chapters.date_upload END DESC
        LIMIT :limit OFFSET :offset
    """
    }

    @Query("SELECT * FROM chapters WHERE manga_id = :mangaId")
    fun observeChaptersForManga(mangaId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE manga_id = :mangaId")
    suspend fun getChaptersForManga(mangaId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE manga_id IN (:mangaIds)")
    suspend fun getChaptersForMangaIds(mangaIds: List<Long>): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Long): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE url = :url")
    suspend fun getChapterByUrl(url: String): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE url = :url AND manga_id = :mangaId")
    suspend fun getChapterByUrlAndMangaId(url: String, mangaId: Long): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>): List<Long>

    @Update suspend fun updateChapters(chapters: List<ChapterEntity>)

    @Delete suspend fun deleteChapter(chapter: ChapterEntity)

    @Delete suspend fun deleteChapters(chapters: List<ChapterEntity>)

    @Query(
        """
        UPDATE chapters SET
        read = :read,
        bookmark = :bookmark,
        last_page_read = :lastPage,
        pages_left = :pagesLeft
        WHERE id = :id
    """
    )
    suspend fun updateProgress(
        id: Long,
        read: Boolean,
        bookmark: Boolean,
        lastPage: Int,
        pagesLeft: Int,
    )

    @Query(
        "UPDATE chapters SET source_order = :order WHERE mangadex_chapter_id = :chapterId AND manga_id = :mangaId"
    )
    suspend fun updateSourceOrder(chapterId: String, mangaId: Long, order: Int)

    @Query(RECENT_CHAPTERS_QUERY)
    fun observeRecentChapters(
        search: String,
        limit: Int,
        offset: Int,
        sortByFetched: Int,
    ): Flow<List<MangaChapter>>

    @Query(RECENT_CHAPTERS_QUERY)
    suspend fun getRecentChapters(
        search: String,
        limit: Int,
        offset: Int,
        sortByFetched: Int,
    ): List<MangaChapter>

    // =========================================================================
    // LEGACY PUT RESOLVER MIGRATIONS (Partial Column Updates)
    // =========================================================================

    // 1. Replicates ChapterBackupPutResolver
    @Query(
        "UPDATE chapters SET read = :read, bookmark = :bookmark, last_page_read = :lastPageRead WHERE mangadex_chapter_id = :mangadexChapterId"
    )
    suspend fun updateChapterBackupByMangadexId(
        mangadexChapterId: String,
        read: Boolean,
        bookmark: Boolean,
        lastPageRead: Int,
    )

    @Transaction
    suspend fun updateChaptersBackup(chapters: List<ChapterEntity>) {
        chapters.forEach { chapter ->
            chapter.mangadexChapterId?.let {
                updateChapterBackupByMangadexId(
                    it,
                    chapter.read,
                    chapter.bookmark,
                    chapter.lastPageRead,
                )
            }
        }
    }

    // 2. Replicates ChapterKnownBackupPutResolver
    @Query(
        "UPDATE chapters SET read = :read, bookmark = :bookmark, last_page_read = :lastPageRead WHERE id = :id"
    )
    suspend fun updateKnownChapterBackupById(
        id: Long,
        read: Boolean,
        bookmark: Boolean,
        lastPageRead: Int,
    )

    @Transaction
    suspend fun updateKnownChaptersBackup(chapters: List<ChapterEntity>) {
        chapters.forEach {
            updateKnownChapterBackupById(it.id, it.read, it.bookmark, it.lastPageRead)
        }
    }

    // 3. Replicates ChapterProgressPutResolver (Batch Version)
    @Transaction
    suspend fun updateChaptersProgress(chapters: List<ChapterEntity>) {
        chapters.forEach {
            updateProgress(it.id, it.read, it.bookmark, it.lastPageRead, it.pagesLeft)
        }
    }

    // 4. Replicates ChapterSourceOrderPutResolver (Batch Version)
    @Transaction
    suspend fun fixChaptersSourceOrder(chapters: List<ChapterEntity>) {
        chapters.forEach { chapter ->
            chapter.mangadexChapterId?.let {
                updateSourceOrder(it, chapter.mangaId, chapter.sourceOrder)
            }
        }
    }
}
