package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.manga.MangaUpdateCoordinator
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.MangaAggregateRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.ScanlatorGroupRepository
import org.nekomanga.data.database.repository.UploaderRepository
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Holds the use cases for Manga handling */
class MangaUseCases(
    chapterRepository: ChapterRepository = Injekt.get(),
    mangaAggregateRepository: MangaAggregateRepository = Injekt.get(),
    mangaRepository: MangaRepository = Injekt.get(),
    preferences: PreferencesHelper = Injekt.get(),
    storageManager: StorageManager = Injekt.get(),
    sourceManager: SourceManager = Injekt.get(),
    scanlatorGroupRepository: ScanlatorGroupRepository = Injekt.get(),
    uploaderRepository: UploaderRepository = Injekt.get(),
    mangaUpdateCoordinator: MangaUpdateCoordinator = Injekt.get(),
    mangaDexPreferences: MangaDexPreferences = Injekt.get(),
) {
    val updateMangaStatusAndMissingCount =
        UpdateMangaStatusAndMissingChapterCount(mangaRepository, chapterRepository)

    val modifyManga = ModifyMangaUseCase(mangaRepository, storageManager)

    val updateMangaAggregate = UpdateMangaAggregate(mangaAggregateRepository, sourceManager)

    val blockScanlator =
        BlockScanlator(
            scanlatorGroupRepository = scanlatorGroupRepository,
            uploaderRepository = uploaderRepository,
            mangaUpdateCoordinator = mangaUpdateCoordinator,
            mangaDexPreferences = mangaDexPreferences,
        )
}
