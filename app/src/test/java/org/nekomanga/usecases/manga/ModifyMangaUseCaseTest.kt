package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.database.models.Manga
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.storage.StorageManager

class ModifyMangaUseCaseTest {

    private val mangaRepository: MangaRepository = mockk()
    private val storageManager: StorageManager = mockk()
    private val modifyMangaUseCase = ModifyMangaUseCase(mangaRepository, storageManager)

    @Test
    fun `given alt title different from previous when setting alt title then changes are saved and directories renamed`() =
        runTest {
            val mangaId = 1L
            val oldTitle = "Old Title"
            val newTitle = "New Title"

            val manga = mockk<Manga>(relaxed = true)
            every { manga.title } returns oldTitle
            every { manga.user_title } returns null
            coEvery { mangaRepository.getMangaById(mangaId) } returns manga
            coEvery { mangaRepository.updateManga(manga) } just runs
            every { storageManager.renamePagesAndCoverDirectory(any(), any()) } returns Unit

            val result = modifyMangaUseCase.setAltTitle(mangaId, newTitle)

            assertNotNull(result)
            verify(exactly = 1) { manga.user_title = newTitle }
            verify(exactly = 1) { storageManager.renamePagesAndCoverDirectory(oldTitle, newTitle) }
        }
}
