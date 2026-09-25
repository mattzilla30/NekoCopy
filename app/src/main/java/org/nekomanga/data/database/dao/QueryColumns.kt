package org.nekomanga.data.database.dao

/**
 * Column lists shared by queries that join manga, chapters and history into one row. The ch_ and
 * hi_ prefixes keep chapter and history columns apart from the manga columns.
 */
object QueryColumns {
    const val CHAPTER =
        """
        chapters.id AS ch_id,
        chapters.manga_id AS ch_manga_id,
        chapters.url AS ch_url,
        chapters.name AS ch_name,
        chapters.chapter_txt AS ch_chapter_txt,
        chapters.chapter_title AS ch_chapter_title,
        chapters.vol AS ch_vol,
        chapters.scanlator AS ch_scanlator,
        chapters.uploader AS ch_uploader,
        chapters.unavailable AS ch_unavailable,
        chapters.read AS ch_read,
        chapters.bookmark AS ch_bookmark,
        chapters.last_page_read AS ch_last_page_read,
        chapters.pages_left AS ch_pages_left,
        chapters.chapter_number AS ch_chapter_number,
        chapters.source_order AS ch_source_order,
        chapters.smart_order AS ch_smart_order,
        chapters.date_fetch AS ch_date_fetch,
        chapters.date_upload AS ch_date_upload,
        chapters.mangadex_chapter_id AS ch_mangadex_chapter_id,
        chapters.language AS ch_language
        """

    const val HISTORY =
        """
        history.id AS hi_id, history.chapter_id AS hi_chapter_id, history.last_read AS hi_last_read, history.time_read AS hi_time_read
        """
}
