package org.nekomanga.data.database.migration

import org.nekomanga.constants.Constants

/**
 * Merged sources were removed. Their chapters were marked by a scanlator that starts with the
 * source's name. The database migration deletes those chapters, and backup restore skips them.
 */
object RemovedMergeSources {
    val names =
        listOf(
            "Komga",
            "Toonily",
            "Weeb Central",
            "Suwayomi",
            "Manga Ball",
            "Project Suki",
            "Comix",
            "Atsumaru",
            "Kagane",
        )

    fun isMergedChapter(scanlator: String?): Boolean {
        if (scanlator == null) return false
        val source = scanlator.substringBefore(Constants.SCANLATOR_SEPARATOR)
        return source in names
    }
}
