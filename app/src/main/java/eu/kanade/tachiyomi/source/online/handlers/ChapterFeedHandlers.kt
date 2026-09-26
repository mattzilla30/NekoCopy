package eu.kanade.tachiyomi.source.online.handlers

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.skydoves.sandwich.ApiResponse
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.models.dto.ChapterListDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.source.online.utils.toSourceManga
import eu.kanade.tachiyomi.util.getOrResultError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.network.ProxyRetrofitQueryMap
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * Pages through a MangaDex chapter feed and turns it into a manga list: one entry per manga, newest
 * chapter first, with that chapter's name as the display text. Subclasses pick the feed.
 */
abstract class ChapterFeedHandler(private val feedName: String) {
    protected val service: MangaDexService by lazy { Injekt.get<NetworkServices>().service }
    private val mangaDexPreferences: MangaDexPreferences by injectLazy()
    private val uniqueManga = mutableSetOf<String>()

    suspend fun getPage(
        page: Int = 1,
        blockedGroupUUIDs: List<String>,
        blockedUploaderUUIDs: List<String>,
        limit: Int = MdConstants.Limits.latest,
    ): Result<MangaListPage, ResultError> {
        if (page == 1) uniqueManga.clear()
        return withContext(Dispatchers.IO) {
            val offset = MdUtil.getLatestChapterListOffset(page)

            val langs = MdUtil.getLangsToShow(mangaDexPreferences)

            val contentRatings = mangaDexPreferences.visibleContentRatings().get().toList()

            return@withContext fetchChapters(
                    limit,
                    offset,
                    langs,
                    contentRatings,
                    blockedGroupUUIDs,
                    blockedUploaderUUIDs,
                )
                .getOrResultError("getting $feedName")
                .andThen { parseChapterFeed(it) }
        }
    }

    protected abstract suspend fun fetchChapters(
        limit: Int,
        offset: Int,
        languages: List<String>,
        contentRatings: List<String>,
        blockedGroups: List<String>,
        blockedUploaders: List<String>,
    ): ApiResponse<ChapterListDto>

    private suspend fun parseChapterFeed(
        chapterListDto: ChapterListDto
    ): Result<MangaListPage, ResultError> {
        return runCatching {
            val result =
                chapterListDto.data
                    .groupBy { chapterListDto ->
                        chapterListDto.relationships
                            .first { relationshipDto ->
                                relationshipDto.type == MdConstants.Types.manga
                            }
                            .id
                    }
                    .filterNot { uniqueManga.contains(it.key) }

            val mangaIds = result.keys.toList()

            uniqueManga.addAll(mangaIds)

            val allContentRating =
                listOf(
                    MdConstants.ContentRating.safe,
                    MdConstants.ContentRating.suggestive,
                    MdConstants.ContentRating.erotica,
                    MdConstants.ContentRating.pornographic,
                )

            val queryParameters =
                mutableMapOf(
                    "ids[]" to mangaIds,
                    "limit" to mangaIds.size,
                    "contentRating[]" to allContentRating,
                )

            service
                .search(ProxyRetrofitQueryMap(queryParameters))
                .getOrResultError("trying to search manga from $feedName")
                .andThen { mangaListDto ->
                    val hasMoreResults =
                        chapterListDto.limit + chapterListDto.offset < chapterListDto.total

                    val mangaDtoMap = mangaListDto.data.associateBy({ it.id }, { it })

                    val thumbQuality = mangaDexPreferences.coverQuality().get()
                    val mangaList =
                        mangaIds
                            .mapNotNull { mangaDtoMap[it] }
                            .sortedByDescending { result[it.id]!!.first().attributes.readableAt }
                            .map {
                                val chapterName =
                                    result[it.id]?.firstOrNull()?.buildChapterName() ?: ""
                                it.toSourceManga(
                                    coverQuality = thumbQuality,
                                    displayText = chapterName,
                                )
                            }

                    Ok(
                        MangaListPage(
                            sourceManga = mangaList.toList(),
                            hasNextPage = hasMoreResults,
                        )
                    )
                }
        }
            .getOrElse { e ->
                if (e !is CancellationException) {
                    TimberKt.e(e) { "Error parsing $feedName" }
                }
                Err(ResultError.Generic(errorString = "Error parsing $feedName response"))
            }
    }
}

/** The latest chapters uploaded to MangaDex. */
class LatestChapterHandler : ChapterFeedHandler("latest chapters") {
    override suspend fun fetchChapters(
        limit: Int,
        offset: Int,
        languages: List<String>,
        contentRatings: List<String>,
        blockedGroups: List<String>,
        blockedUploaders: List<String>,
    ) =
        service.latestChapters(
            limit,
            offset,
            languages,
            contentRatings,
            blockedGroups,
            blockedUploaders,
        )
}
