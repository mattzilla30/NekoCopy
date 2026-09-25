package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.model.isLocalSource
import kotlin.math.floor
import org.nekomanga.constants.Constants
import org.nekomanga.domain.chapter.ChapterItem

data class MissingChapterHolder(val count: String = "", val estimatedChapters: String = "")

fun List<ChapterItem>.getMissingChapters(): MissingChapterHolder {

    val chapterNumbers =
        this.asSequence()
            .filter { it.isAvailable() }
            .distinctBy {
                if (it.chapter.chapterText.isNotEmpty()) {
                    "${it.chapter.volume}-${it.chapter.chapterText}"
                } else {
                    it.chapter.name
                }
            }
            .mapNotNull { runCatching { floor(it.chapter.chapterNumber).toInt() }.getOrNull() }
            .sorted()
            .toList()

    if (chapterNumbers.isEmpty()) {
        return MissingChapterHolder()
    }

    val missingRanges = mutableListOf<String>()
    var totalMissing = 0

    (listOf(0) + chapterNumbers).zipWithNext { prev, curr ->
        // Check if there is a gap of one or more chapters between the current and previous
        if (curr - prev > 1) {
            val startOfGap = prev + 1
            val endOfGap = curr - 1

            totalMissing += (endOfGap - startOfGap + 1)
            val rangeString =
                if (startOfGap == endOfGap) {
                    "Ch.$startOfGap"
                } else {
                    "Ch.$startOfGap ${Constants.RIGHT_ARROW_SEPARATOR} Ch.$endOfGap"
                }
            missingRanges.add(rangeString)
        }
    }

    return MissingChapterHolder(
        count = totalMissing.takeIf { it > 0 }?.toString() ?: "",
        estimatedChapters =
            missingRanges.takeIf { it.isNotEmpty() }?.joinToString(Constants.SEPARATOR) ?: "",
    )
}

fun ChapterItem.isAvailable(): Boolean {
    return !this.chapter.isUnavailable || this.chapter.isLocalSource() || this.isDownloaded
}

fun Chapter.isAvailable(downloadManager: DownloadManager, manga: Manga): Boolean {
    return !this.isUnavailable ||
        this.isLocalSource() ||
        downloadManager.isChapterDownloaded(this, manga)
}

fun Chapter.isAvailable(isDownloaded: Boolean): Boolean {
    return isDownloaded || !this.isUnavailable || this.isLocalSource()
}
