package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.viewer.calculateChapterDifference
import eu.kanade.tachiyomi.ui.reader.viewer.hasMissingChapters

/** Presentation model for the chapter transition pages in the Compose readers. */
@Immutable
sealed interface ChapterTransitionUiModel {
    val fromChapterName: String

    @Immutable
    data class Prev(
        override val fromChapterName: String,
        val toChapter: TargetChapterInfo? = null,
        val missingChaptersCount: Int = 0,
    ) : ChapterTransitionUiModel

    @Immutable
    data class Next(
        override val fromChapterName: String,
        val toChapter: TargetChapterInfo? = null,
        val missingChaptersCount: Int = 0,
    ) : ChapterTransitionUiModel

    @Immutable
    data class TargetChapterInfo(
        val chapterId: Long,
        val name: String,
        val preloadState: PreloadState = PreloadState.Ready,
    )

    @Immutable
    sealed interface PreloadState {
        @Immutable data object Ready : PreloadState

        @Immutable data object Loading : PreloadState

        @Immutable data class Error(val message: String = "") : PreloadState
    }

    companion object {
        fun from(transition: ChapterTransition): ChapterTransitionUiModel {
            val from = transition.from
            val to = transition.to
            // Missing chapters sit between the earlier and the later of the two chapters.
            val (earlier, later) =
                when (transition) {
                    is ChapterTransition.Prev -> to to from
                    is ChapterTransition.Next -> from to to
                }
            val missingChaptersCount =
                if (earlier != null && later != null && hasMissingChapters(later, earlier)) {
                    calculateChapterDifference(later, earlier).toInt()
                } else {
                    0
                }
            val toChapter = to?.let {
                TargetChapterInfo(
                    chapterId = it.chapter.id ?: -1L,
                    name = it.chapter.name,
                    preloadState =
                        when (val state = it.state) {
                            is ReaderChapter.State.Loading -> PreloadState.Loading
                            is ReaderChapter.State.Error ->
                                PreloadState.Error(state.error.message ?: "")
                            else -> PreloadState.Ready
                        },
                )
            }
            return when (transition) {
                is ChapterTransition.Prev ->
                    Prev(from.chapter.name, toChapter, missingChaptersCount)
                is ChapterTransition.Next ->
                    Next(from.chapter.name, toChapter, missingChaptersCount)
            }
        }
    }
}
