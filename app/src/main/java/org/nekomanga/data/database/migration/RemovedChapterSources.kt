package org.nekomanga.data.database.migration

import org.nekomanga.constants.Constants

/**
 * Chapter sources Kitty no longer supports. The database migrations delete their chapters, and
 * backup restore skips them.
 */
object RemovedChapterSources {
    /** Merged sources marked their chapters with a scanlator that starts with the source's name. */
    val mergeSourceNames =
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

    /** Local chapters, from files on the device, used this scanlator and were unavailable. */
    const val LOCAL_SCANLATOR = "Local"

    fun isRemovedChapter(scanlator: String?, isUnavailable: Boolean): Boolean {
        if (scanlator == null) return false
        if (scanlator == LOCAL_SCANLATOR && isUnavailable) return true
        return scanlator.substringBefore(Constants.SCANLATOR_SEPARATOR) in mergeSourceNames
    }
}
