package org.nekomanga.presentation.screens.reader.viewer

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterTransitionUiModelTest {

    private fun createChapter(id: Long, chapterNumber: Float = id.toFloat()): ReaderChapter {
        val dbChapter =
            Chapter.create().apply {
                this.id = id
                this.url = "/chapter/$id"
                this.name = "Chapter $id"
                this.chapter_number = chapterNumber
                this.mangadex_chapter_id = "md_$id"
            }
        return ReaderChapter(dbChapter)
    }

    @Test
    fun `prev transition across a gap counts the missing chapters`() {
        val transition = ChapterTransition.Prev(from = createChapter(5L), to = createChapter(1L))

        val model = ChapterTransitionUiModel.from(transition) as ChapterTransitionUiModel.Prev

        assertEquals("Chapter 5", model.fromChapterName)
        assertEquals("Chapter 1", model.toChapter?.name)
        assertEquals(3, model.missingChaptersCount)
    }

    @Test
    fun `next transition across a gap counts the missing chapters`() {
        val transition = ChapterTransition.Next(from = createChapter(1L), to = createChapter(5L))

        val model = ChapterTransitionUiModel.from(transition) as ChapterTransitionUiModel.Next

        assertEquals(3, model.missingChaptersCount)
    }

    @Test
    fun `next transition without a next chapter has no target`() {
        val transition = ChapterTransition.Next(from = createChapter(10L), to = null)

        val model = ChapterTransitionUiModel.from(transition) as ChapterTransitionUiModel.Next

        assertEquals("Chapter 10", model.fromChapterName)
        assertNull(model.toChapter)
        assertEquals(0, model.missingChaptersCount)
    }

    @Test
    fun `adjacent chapters have no gap and a ready target`() {
        val transition = ChapterTransition.Next(from = createChapter(1L), to = createChapter(2L))

        val model = ChapterTransitionUiModel.from(transition) as ChapterTransitionUiModel.Next

        assertEquals("Chapter 2", model.toChapter?.name)
        assertEquals(0, model.missingChaptersCount)
        assertEquals(ChapterTransitionUiModel.PreloadState.Ready, model.toChapter?.preloadState)
    }

    @Test
    fun `a target that failed to load carries its error`() {
        val next =
            createChapter(2L).apply {
                state = ReaderChapter.State.Error(Exception("Network timeout"))
            }
        val transition = ChapterTransition.Next(from = createChapter(1L), to = next)

        val model = ChapterTransitionUiModel.from(transition) as ChapterTransitionUiModel.Next
        val preloadState = model.toChapter?.preloadState

        assertTrue(preloadState is ChapterTransitionUiModel.PreloadState.Error)
        assertEquals(
            "Network timeout",
            (preloadState as ChapterTransitionUiModel.PreloadState.Error).message,
        )
    }
}
