package eu.kanade.tachiyomi.source.online.utils

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.models.dto.MangaAttributesDto
import eu.kanade.tachiyomi.source.online.models.dto.MangaDataDto
import eu.kanade.tachiyomi.source.online.models.dto.asMdMap
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.manga.SourceManga

fun MangaDataDto.toBasicManga(coverQuality: Int = 0, useNoCoverUrl: Boolean = true): SManga {
    return SManga.create().apply {
        url = "/title/" + this@toBasicManga.id
        title =
            MdUtil.cleanString(
                MdUtil.getTitle(
                    this@toBasicManga.attributes.title,
                    this@toBasicManga.attributes.originalLanguage,
                    this@toBasicManga.attributes.altTitleMaps(),
                )
            )

        thumbnail_url =
            this@toBasicManga.relationships
                .firstOrNull { relationshipDto ->
                    relationshipDto.type == MdConstants.Types.coverArt
                }
                ?.attributes
                ?.fileName
                ?.let { coverFileName ->
                    MdUtil.cdnCoverUrl(this@toBasicManga.id, coverFileName, coverQuality)
                } ?: if (useNoCoverUrl) MdConstants.noCoverUrl else null
    }
}

fun MangaDataDto.toSourceManga(
    coverQuality: Int = 0,
    useNoCoverUrl: Boolean = true,
    displayText: String = "",
    @StringRes displayTextRes: Int? = null,
): SourceManga {
    val thumbnail =
        this@toSourceManga.relationships
            .firstOrNull { relationshipDto -> relationshipDto.type == MdConstants.Types.coverArt }
            ?.attributes
            ?.fileName
            ?.let { coverFileName ->
                MdUtil.cdnCoverUrl(this@toSourceManga.id, coverFileName, coverQuality)
            } ?: if (useNoCoverUrl) MdConstants.noCoverUrl else ""

    return SourceManga(
        url = "/title/" + this@toSourceManga.id,
        title =
            MdUtil.cleanString(
                MdUtil.getTitle(
                    this@toSourceManga.attributes.title,
                    this@toSourceManga.attributes.originalLanguage,
                    this@toSourceManga.attributes.altTitleMaps(),
                )
            ),
        displayText = displayText,
        displayTextRes = displayTextRes,
        currentThumbnail = thumbnail,
    )
}

private fun MangaAttributesDto.altTitleMaps(): List<Map<String, String?>> =
    altTitles.orEmpty().map { it.asMdMap<String?>() }
