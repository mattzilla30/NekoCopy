package org.nekomanga.presentation.screens.browse

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.utils.getBlockedScanlatorGroupUUIDs
import eu.kanade.tachiyomi.source.online.utils.getBlockedUploaderUUIDs
import eu.kanade.tachiyomi.util.manga.toDisplayManga
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.ScanlatorGroupRepository
import org.nekomanga.data.database.repository.UploaderRepository
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowseRepository(
    private val mangaDex: MangaDex = Injekt.get<SourceManager>().mangaDex,
    private val mangaRepository: MangaRepository = Injekt.get(),
    private val scanlatorGroupRepository: ScanlatorGroupRepository = Injekt.get(),
    private val uploaderRepository: UploaderRepository = Injekt.get(),
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
) {

    suspend fun getRandomManga(): Result<DisplayManga, ResultError> {
        return mangaDex.getRandomManga().andThen {
            val displayManga = it.toDisplayManga(mangaRepository, mangaDex.id)
            Ok(displayManga)
        }
    }

    suspend fun getSearchPage(
        page: Int,
        filters: DexFilters,
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex.search(page, filters).andThen { mangaListPage ->
            val displayMangaList =
                mangaListPage.sourceManga.toDisplayManga(mangaRepository, mangaDex.id)
            Ok(Pair(mangaListPage.hasNextPage, displayMangaList))
        }
    }

    suspend fun getHomePage(): Result<List<HomePageManga>, ResultError> {
        val blockedGroupUUIDs =
            getBlockedScanlatorGroupUUIDs(mangaDexPreferences, scanlatorGroupRepository, mangaDex)
        val blockedUploaderUUIDs =
            getBlockedUploaderUUIDs(mangaDexPreferences, uploaderRepository, mangaDex)

        return mangaDex.fetchHomePageInfo(blockedGroupUUIDs, blockedUploaderUUIDs).andThen {
            listResults ->
            Ok(
                listResults.map { listResult ->
                    HomePageManga(
                        displayScreenType = listResult.displayScreenType,
                        displayManga =
                            listResult.sourceManga
                                .toDisplayManga(mangaRepository, mangaDex.id)
                                .distinctBy { it.url }
                                .toList(),
                    )
                }
            )
        }
    }
}
