package org.nekomanga.usecases.chapters

import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.site.MangaDexPreferences

class MarkChaptersRemoteTest {

    private lateinit var statusHandler: StatusHandler
    private lateinit var mangaDexPreferences: MangaDexPreferences
    private lateinit var markChaptersRemote: MarkChaptersRemote

    @Before
    fun setup() {
        statusHandler = mockk()
        mangaDexPreferences = mockk()
        markChaptersRemote = MarkChaptersRemote(statusHandler, mangaDexPreferences)
    }

    @Test
    fun `given skipSync true when marking mixed chapters then ignores readingSync and skips status calls`() =
        runTest {
            // Arrange
            val mangaUuid = "manga-uuid-123"
            val markAction = ChapterMarkActions.Read(canUndo = false)

            val firstChapter =
                SimpleChapter.create()
                    .copy(id = 1L, mangaDexChapterId = "md-chapter-1", scanlator = "Some Scanlator")
            val secondChapter =
                SimpleChapter.create()
                    .copy(
                        id = 2L,
                        mangaDexChapterId = "md-chapter-2",
                        scanlator = "Komga",
                    )

            val chapterItems =
                listOf(
                    ChapterItem(chapter = firstChapter),
                    ChapterItem(chapter = secondChapter),
                )

            // Mock reading sync true just to be sure skipSync supersedes it
            every { mangaDexPreferences.readingSync().get() } returns true

            // Act
            markChaptersRemote(markAction, mangaUuid, chapterItems, skipSync = true)

            // Assert
            coVerify(exactly = 0) { statusHandler.markChaptersStatus(any(), any(), any()) }
        }

    @Test
    fun `given read action with sync true when marking chapters then syncs all of them to MangaDex`() =
        runTest {
            // Arrange
            val mangaUuid = "manga-uuid-123"
            val markAction = ChapterMarkActions.Read(canUndo = false)

            val firstChapter =
                SimpleChapter.create()
                    .copy(id = 1L, mangaDexChapterId = "md-chapter-1", scanlator = "Some Scanlator")
            val secondChapter =
                SimpleChapter.create()
                    .copy(
                        id = 2L,
                        mangaDexChapterId = "md-chapter-2",
                        scanlator = "Komga",
                    )

            val chapterItems =
                listOf(
                    ChapterItem(chapter = firstChapter),
                    ChapterItem(chapter = secondChapter),
                )

            every { mangaDexPreferences.readingSync().get() } returns true

            coEvery { statusHandler.markChaptersStatus(any(), any(), any()) } returns Unit

            // Act
            markChaptersRemote(markAction, mangaUuid, chapterItems, skipSync = false)

            // Assert
            coVerify(exactly = 1) {
                statusHandler.markChaptersStatus(
                    mangaId = mangaUuid,
                    chapterIds = listOf("md-chapter-1", "md-chapter-2"),
                    read = true,
                )
            }
        }
}
