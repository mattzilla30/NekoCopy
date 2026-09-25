package org.nekomanga.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.nekomanga.data.database.entity.HistoryEntity
import org.nekomanga.data.database.model.MangaChapterHistory

@Dao
interface HistoryDao {
    companion object {
        private const val RECENT_HISTORY_UNGROUPED_QUERY =
            """
        SELECT manga.*,
               ${QueryColumns.CHAPTER},
               ${QueryColumns.HISTORY}
        FROM manga
        JOIN chapters ON manga.id = chapters.manga_id
        JOIN history ON chapters.id = history.chapter_id
        WHERE history.last_read > 0
        AND LOWER(manga.title) LIKE :search
        ORDER BY history.last_read DESC
        LIMIT :limit OFFSET :offset
    """

        private const val RECENT_MANGA_LIMIT_QUERY =
            """
        SELECT manga.*,
               ${QueryColumns.CHAPTER},
               ${QueryColumns.HISTORY}
        FROM manga
        JOIN chapters ON manga.id = chapters.manga_id
        JOIN history ON chapters.id = history.chapter_id
        JOIN (
            SELECT chapters.manga_id, chapters.id as history_chapter_id, MAX(history.last_read) as history_last_read
            FROM chapters JOIN history ON chapters.id = history.chapter_id
            GROUP BY chapters.manga_id
        ) AS max_last_read
        ON chapters.manga_id = max_last_read.manga_id
        AND max_last_read.history_chapter_id = history.chapter_id
        AND max_last_read.history_last_read > 0
        AND LOWER(manga.title) LIKE :search
        ORDER BY max_last_read.history_last_read DESC
        LIMIT :limit OFFSET :offset
    """

        private const val HISTORY_BY_MANGA_ID_QUERY =
            """
        SELECT history.*
        FROM history
        JOIN chapters ON history.chapter_id = chapters.id
        WHERE chapters.manga_id = :mangaId
    """

        private const val CHAPTER_HISTORY_BY_MANGA_ID_QUERY =
            """
        SELECT manga.*,
               ${QueryColumns.CHAPTER},
               ${QueryColumns.HISTORY}
        FROM manga
        JOIN chapters ON manga.id = chapters.manga_id
        JOIN history ON chapters.id = history.chapter_id
        AND history.last_read > 0
        WHERE manga.id = :mangaId
        ORDER BY history.last_read DESC
        LIMIT 25
    """
    }

    @Query(RECENT_HISTORY_UNGROUPED_QUERY)
    fun observeRecentHistoryUngrouped(
        search: String,
        limit: Int,
        offset: Int,
    ): Flow<List<MangaChapterHistory>>

    @Query(RECENT_HISTORY_UNGROUPED_QUERY)
    suspend fun getRecentHistoryUngrouped(
        search: String,
        limit: Int,
        offset: Int,
    ): List<MangaChapterHistory>

    @Query(RECENT_MANGA_LIMIT_QUERY)
    fun observeRecentMangaLimit(
        search: String,
        limit: Int,
        offset: Int,
    ): Flow<List<MangaChapterHistory>>

    @Query(RECENT_MANGA_LIMIT_QUERY)
    suspend fun getRecentMangaLimit(
        search: String,
        limit: Int,
        offset: Int,
    ): List<MangaChapterHistory>

    @Query(
        """
        SELECT * FROM
        (SELECT manga.*, ${QueryColumns.CHAPTER},
               ${QueryColumns.HISTORY}
        FROM (
            SELECT manga.*
            FROM manga
            LEFT JOIN (
                SELECT manga_id, COUNT(*) AS unread
                FROM chapters
                WHERE read = 0
                GROUP BY manga_id
            ) AS C
            ON manga.id = C.manga_id
            WHERE (:includeRead = 1 OR C.unread > 0)
            GROUP BY manga.id
            ORDER BY manga.title
        ) AS manga
        JOIN chapters
        ON manga.id = chapters.manga_id
        JOIN history
        ON chapters.id = history.chapter_id
         JOIN (
            SELECT chapters.manga_id, chapters.id as history_chapter_id, MAX(history.last_read) as history_last_read
            FROM chapters JOIN history ON chapters.id = history.chapter_id
            GROUP BY chapters.manga_id) AS max_last_read
        ON chapters.manga_id = max_last_read.manga_id
        AND max_last_read.history_chapter_id = history.chapter_id
        AND max_last_read.history_last_read > 0
        AND LOWER(manga.title) LIKE :search)
        UNION
        SELECT * FROM
        (SELECT manga.*, ${QueryColumns.CHAPTER},
            Null as hi_id,
            Null as hi_chapter_id,
            chapters.date_fetch as hi_last_read,
            Null as hi_time_read
        FROM manga
        JOIN chapters
        ON manga.id = chapters.manga_id
        JOIN (
            SELECT chapters.manga_id, chapters.id as history_chapter_id, MAX(chapters.date_upload)
            FROM chapters JOIN manga
            ON manga.id = chapters.manga_id
            WHERE chapters.read = 0
            GROUP BY chapters.manga_id) AS newest_chapter
        ON chapters.manga_id = newest_chapter.manga_id
        WHERE manga.favorite = 1
        AND newest_chapter.history_chapter_id = chapters.id
        AND chapters.date_fetch > manga.date_added
        AND LOWER(manga.title) LIKE :search)
        UNION
        SELECT * FROM
        (SELECT manga.*,
            Null as ch_id,
            Null as ch_manga_id,
            Null as ch_url,
            Null as ch_name,
            Null as ch_chapter_txt,
            Null as ch_chapter_title,
            Null as ch_vol,
            Null as ch_scanlator,
            Null as ch_uploader,
            Null as ch_unavailable,
            Null as ch_read,
            Null as ch_bookmark,
            Null as ch_last_page_read,
            Null as ch_pages_left,
            Null as ch_chapter_number,
            Null as ch_source_order,
            Null as ch_smart_order,
            Null as ch_date_fetch,
            Null as ch_date_upload,
            Null as ch_mangadex_chapter_id,
            Null as ch_language,
            Null as hi_id,
            Null as hi_chapter_id,
            manga.date_added as hi_last_read,
            Null as hi_time_read
            FROM manga
        WHERE manga.favorite = 1
        AND LOWER(manga.title) LIKE :search)
        ORDER BY hi_last_read DESC
        LIMIT :limit OFFSET :offset
    """
    )
    fun observeAllRecentsTypes(
        search: String,
        includeRead: Boolean,
        limit: Int,
        offset: Int,
    ): Flow<List<MangaChapterHistory>>

    @Query(HISTORY_BY_MANGA_ID_QUERY)
    suspend fun getHistoryByMangaId(mangaId: Long): List<HistoryEntity>

    @Query(HISTORY_BY_MANGA_ID_QUERY)
    fun observeHistoryByMangaId(mangaId: Long): Flow<List<HistoryEntity>>

    @Query(
        """
        SELECT history.*
        FROM history
        JOIN chapters ON history.chapter_id = chapters.id
        WHERE chapters.manga_id IN (:mangaIds)
    """
    )
    suspend fun getHistoryByMangaIds(mangaIds: List<Long>): List<HistoryEntity>

    @Query(CHAPTER_HISTORY_BY_MANGA_ID_QUERY)
    fun observeChapterHistoryByMangaId(mangaId: Long): Flow<List<MangaChapterHistory>>

    @Query(CHAPTER_HISTORY_BY_MANGA_ID_QUERY)
    suspend fun getChapterHistoryByMangaId(mangaId: Long): List<MangaChapterHistory>

    /**
     * MACRO-LEVEL PERFORMANCE OPTIMIZATION (Overclock):
     *
     * Why: Resolves N+1 database queries when displaying history feed or recently read manga.
     * Instead of sequentially querying history details for each manga ID, this queries the database
     * once in a bulk `IN` select, reducing database thread contention and latency.
     */
    @Query(
        """
        SELECT manga.*,
               ${QueryColumns.CHAPTER},
               ${QueryColumns.HISTORY}
        FROM manga
        JOIN chapters ON manga.id = chapters.manga_id
        JOIN history ON chapters.id = history.chapter_id
        AND history.last_read > 0
        WHERE manga.id IN (:mangaIds)
        ORDER BY history.last_read DESC
    """
    )
    @RewriteQueriesToDropUnusedColumns
    suspend fun getChapterHistoryByMangaIds(mangaIds: List<Long>): List<MangaChapterHistory>

    @Query(
        """
        SELECT history.*
        FROM history
        JOIN chapters ON history.chapter_id = chapters.id
        WHERE chapters.url = :chapterUrl
        LIMIT 1
    """
    )
    suspend fun getHistoryByChapterUrl(chapterUrl: String): HistoryEntity?

    @Query("SELECT SUM(time_read) FROM history") suspend fun getTotalReadDuration(): Long

    @Query("SELECT * FROM history WHERE chapter_id = :chapterId")
    suspend fun getHistoryByChapterId(chapterId: Long): HistoryEntity?

    @Upsert suspend fun insertHistory(history: HistoryEntity): Long

    @Query(
        "UPDATE history SET last_read = :lastRead, time_read = :timeRead WHERE chapter_id = :chapterId"
    )
    suspend fun updateHistoryLastRead(chapterId: Long, lastRead: Long, timeRead: Long)

    @Transaction
    suspend fun upsertHistory(history: HistoryEntity) {
        val exists = getHistoryByChapterId(history.chapterId) != null
        if (exists) {
            updateHistoryLastRead(history.chapterId, history.lastRead, history.timeRead)
        } else {
            insertHistory(history)
        }
    }

    @Transaction
    suspend fun upsertHistoryList(historyList: List<HistoryEntity>) {
        historyList.forEach { upsertHistory(it) }
    }

    @Query("DELETE FROM history") suspend fun deleteAllHistory()

    @Query("DELETE FROM history WHERE last_read = 0") suspend fun deleteHistoryNoLastRead()
}
