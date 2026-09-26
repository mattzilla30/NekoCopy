package org.nekomanga.domain.manga

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable data class SimpleManga(val title: String, val id: Long)

@Immutable
data class SourceManga(
    val currentThumbnail: String,
    val url: String,
    val title: String,
    val displayText: String = "",
    @param:StringRes val displayTextRes: Int? = null,
)

@Immutable
data class DisplayManga(
    val mangaId: Long,
    val currentArtwork: Artwork,
    val url: String,
    val originalTitle: String,
    val userTitle: String,
    val displayText: String = "",
    @param:StringRes val displayTextRes: Int? = null,
) {

    fun getTitle(): String {
        return userTitle.ifEmpty { originalTitle }
    }
}

@Immutable
data class Artwork(
    val cover: String = "",
    val dynamicCover: String = "",
    val originalCover: String = "",
    val mangaId: Long,
    val description: String = "",
    val volume: String = "",
    val active: Boolean = false,
)
