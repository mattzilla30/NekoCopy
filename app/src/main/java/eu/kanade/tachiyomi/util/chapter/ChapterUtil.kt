package eu.kanade.tachiyomi.util.chapter

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.contextCompatColor
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants

class ChapterUtil {
    companion object {

        fun relativeDate(chapter: Chapter): String? {
            return when (chapter.date_upload > 0) {
                true -> chapter.date_upload.timeSpanFromNow
                false -> null
            }
        }

        fun relativeDate(chapterDate: Long): String? {
            return when (chapterDate > 0) {
                true -> chapterDate.timeSpanFromNow
                false -> null
            }
        }

        fun chapterColor(context: Context, chapter: Chapter, hideStatus: Boolean = false): Int {
            return when {
                hideStatus -> unreadColor(context)
                chapter.read -> readColor(context)
                else -> unreadColor(context)
            }
        }

        fun bookmarkColor(context: Context, chapter: Chapter): Int {
            return when {
                chapter.bookmark -> bookmarkedColor(context)
                else -> readColor(context)
            }
        }

        private fun readColor(context: Context): Int =
            context.contextCompatColor(R.color.read_chapter)

        private fun unreadColor(context: Context): Int =
            context.getResourceColor(R.attr.colorOnBackground)

        private fun bookmarkedColor(context: Context): Int =
            context.getResourceColor(R.attr.colorSecondary)

        fun getScanlators(scanlators: String?): List<String> {
            if (scanlators.isNullOrBlank()) return emptyList()
            if (!scanlators.contains(Constants.SCANLATOR_SEPARATOR)) {
                return listOf(scanlators)
            }
            return scanlators.split(Constants.SCANLATOR_SEPARATOR).distinct()
        }

        fun getScanlatorString(scanlators: Set<String>): String {
            return scanlators.toList().sorted().joinToString(Constants.SCANLATOR_SEPARATOR)
        }

        fun getLanguages(language: String?): List<String> {
            if (language.isNullOrBlank()) return emptyList()
            if (!language.contains(Constants.SCANLATOR_SEPARATOR)) {
                return listOf(language)
            }
            return language.split(Constants.SCANLATOR_SEPARATOR).distinct()
        }

        fun getLanguageString(languages: Set<String>): String {
            return languages.toList().sorted().joinToString(Constants.SCANLATOR_SEPARATOR)
        }

        /**
         * Returns true when [sourceName] is one of the [filteredSources] and the chapter comes from
         * that source: MangaDex for every non-local chapter, Local for local ones.
         */
        fun filteredBySource(
            sourceName: String,
            isLocal: Boolean,
            filteredSources: Set<String>,
        ): Boolean {
            if (sourceName !in filteredSources) {
                return false
            }
            return when (sourceName) {
                MdConstants.name -> !isLocal
                Constants.LOCAL_SOURCE -> isLocal
                else -> false
            }
        }

        /**
         * returns true for a list filter, if the language of the chapter exists in the filtered
         * language set
         */
        fun filterByLanguage(languages: List<String>, filteredLanguages: Set<String>): Boolean {
            return when {
                filteredLanguages.isEmpty() -> false
                else -> {
                    languages.any { language -> language in filteredLanguages }
                }
            }
        }

        /**
         * returns true for a list filter, if the group of the chapter exists in the group set and
         * the any filter is used. if the all filter is used then all the groups for the chapter
         * need to be in the group set.
         */
        fun filterByScanlator(
            scanlators: List<String>,
            uploader: String,
            all: Boolean,
            filteredGroups: Set<String>,
            filteredUploaders: Set<String> = emptySet(),
        ): Boolean {
            if (filteredGroups.isEmpty() && filteredUploaders.isEmpty()) return false

            var nonMergeCount = 0
            for (s in scanlators) {
                if (s !in SourceManager.sourceScanlatorNames) {
                    nonMergeCount++
                }
            }

            if (nonMergeCount == 0 && uploader.isEmpty()) return false

            var hasNoGroup = false
            if (nonMergeCount > 0) {
                for (s in scanlators) {
                    if (s !in SourceManager.sourceScanlatorNames && s == Constants.NO_GROUP) {
                        hasNoGroup = true
                        break
                    }
                }
            }

            val needsUploaderCheck = uploader.isNotEmpty() && hasNoGroup

            if (all) {
                val skipNoGroup =
                    uploader.isNotEmpty() && Constants.NO_GROUP !in filteredGroups && hasNoGroup

                if (needsUploaderCheck) {
                    val uploaderIsFiltered =
                        uploader in filteredGroups || uploader in filteredUploaders
                    if (!uploaderIsFiltered) return false
                }

                for (s in scanlators) {
                    if (s in SourceManager.sourceScanlatorNames) continue
                    if (s == Constants.NO_GROUP && skipNoGroup) continue

                    val isFiltered =
                        s in filteredGroups || (needsUploaderCheck && s in filteredUploaders)
                    if (!isFiltered) return false
                }
                return true
            } else {
                if (needsUploaderCheck) {
                    val uploaderIsFiltered =
                        uploader in filteredGroups || uploader in filteredUploaders
                    if (uploaderIsFiltered) return true
                }

                for (s in scanlators) {
                    if (s in SourceManager.sourceScanlatorNames) continue
                    val isFiltered =
                        s in filteredGroups || (needsUploaderCheck && s in filteredUploaders)
                    if (isFiltered) return true
                }
                return false
            }
        }
    }
}
