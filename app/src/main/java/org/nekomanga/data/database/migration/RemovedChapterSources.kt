package org.nekomanga.data.database.migration

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
}
