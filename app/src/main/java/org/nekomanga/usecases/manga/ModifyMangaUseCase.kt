package org.nekomanga.usecases.manga

import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.api.get

class ModifyMangaUseCase(
    private val mangaRepository: MangaRepository,
    private val storageManager: StorageManager,
) {
    suspend fun setAltTitle(
        mangaId: Long,
        title: String?,
    ): eu.kanade.tachiyomi.data.database.models.Manga? {
        val dbManga = mangaRepository.getMangaById(mangaId) ?: return null
        val previousEffectiveTitle = dbManga.user_title ?: dbManga.title
        val newEffectiveTitle = title ?: dbManga.title

        if (previousEffectiveTitle != newEffectiveTitle) {
            dbManga.user_title = title
            mangaRepository.updateManga(dbManga)

            storageManager.renamePagesAndCoverDirectory(previousEffectiveTitle, newEffectiveTitle)
        }

        return dbManga
    }
}
