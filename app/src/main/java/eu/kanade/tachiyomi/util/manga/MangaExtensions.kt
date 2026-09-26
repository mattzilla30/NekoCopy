package eu.kanade.tachiyomi.util.manga

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.lang.capitalizeWords
import org.nekomanga.constants.MdConstants
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.screens.browse.HomePageManga

/** Takes a SourceManga and converts to a display manga */
suspend fun SourceManga.toDisplayManga(
    mangaRepository: MangaRepository,
    sourceId: Long,
): DisplayManga {
    var localManga = mangaRepository.getMangaByUrlAndSource(this@toDisplayManga.url, sourceId)
    if (localManga == null) {
        val newManga = Manga.create(this.url, this.title, sourceId)
        newManga.apply { this.thumbnail_url = currentThumbnail }
        newManga.id = mangaRepository.insertManga(newManga)
        localManga = newManga
    } else if (localManga.title.isBlank()) {
        localManga.title = this.title
        mangaRepository.updateManga(localManga)
    }
    return localManga.toDisplayManga(this.displayText, this.displayTextRes)
}

/** Takes a list of SourceManga and converts to a list of display manga in bulk */
suspend fun Iterable<SourceManga>.toDisplayManga(
    mangaRepository: MangaRepository,
    sourceId: Long,
): List<DisplayManga> {
    if (!any()) return emptyList()

    val sourceMangas = this.toList()
    val urls = sourceMangas.map { it.url }.distinct()

    // The repository chunks internally to respect the SQLite bind-variable limit.
    val existingMangas = mangaRepository.getMangaByUrls(urls).associateBy { it.url }.toMutableMap()

    val newMangasList = mutableListOf<Manga>()
    val updateMangasList = mutableListOf<Manga>()
    val newlyCreatedByUrl = mutableMapOf<String, Manga>()

    for (sourceManga in sourceMangas) {
        val url = sourceManga.url
        var localManga = existingMangas[url] ?: newlyCreatedByUrl[url]
        if (localManga == null) {
            val newManga =
                Manga.create(sourceManga.url, sourceManga.title, sourceId).apply {
                    this.thumbnail_url = sourceManga.currentThumbnail
                }
            newMangasList.add(newManga)
            newlyCreatedByUrl[url] = newManga
        } else if (localManga.title.isBlank() && sourceManga.title.isNotBlank()) {
            localManga.title = sourceManga.title
            updateMangasList.add(localManga)
        }
    }

    if (newMangasList.isNotEmpty()) {
        val insertedIds = mangaRepository.insertMangaList(newMangasList)
        newMangasList.forEachIndexed { index, manga -> manga.id = insertedIds[index] }
    }
    if (updateMangasList.isNotEmpty()) {
        mangaRepository.updateMangaList(updateMangasList)
    }

    val allMangasByUrl = existingMangas + newlyCreatedByUrl

    return sourceMangas.mapNotNull { sourceManga ->
        val localManga = allMangasByUrl[sourceManga.url]
        if (localManga == null) {
            TimberKt.e { "Manga not found for url: ${sourceManga.url}" }
            return@mapNotNull null
        }
        localManga.toDisplayManga(sourceManga.displayText, sourceManga.displayTextRes)
    }
}

fun Manga.toDisplayManga(
    displayText: String = "",
    @StringRes displayTextRes: Int? = null,
): DisplayManga {
    return DisplayManga(
        mangaId = this.id!!,
        url = this.url,
        originalTitle = this.title,
        userTitle = this.user_title ?: "",
        displayText = displayText.replace("_", " ").capitalizeWords(),
        displayTextRes = displayTextRes,
        currentArtwork =
            Artwork(
                cover = this.user_cover ?: "",
                dynamicCover = this.dynamic_cover ?: "",
                mangaId = this.id!!,
                originalCover = this.thumbnail_url ?: MdConstants.noCoverUrl,
            ),
    )
}

fun SManga.getSlug(): String {
    val title =
        this.title
            .trim()
            .lowercase()
            .replace("[^a-z0-9]+".toRegex(), "-")
            .replace("-+$".toRegex(), "")

    val wordList = title.split('-')
    val slug = mutableListOf<String>()

    for (i in wordList) {
        if ((slug.joinToString("-", "", "-") + i).count() < 100) {
            slug.add(i)
        } else {
            break
        }
    }

    return slug.joinToString("-")
}

/** resync homepage manga with db manga */
suspend fun List<HomePageManga>.resyncHomePageManga(
    mangaRepository: MangaRepository
): List<HomePageManga> {
    return this.map { homePageManga ->
            homePageManga.copy(
                displayManga =
                    homePageManga.displayManga.resyncDisplayManga(mangaRepository).toList()
            )
        }
        .toList()
}

suspend fun List<DisplayManga>.resyncDisplayManga(
    mangaRepository: MangaRepository
): List<DisplayManga> {
    if (this.isEmpty()) return emptyList()

    // The repository chunks internally to respect the SQLite bind-variable limit.
    val mangaIds = this.map { it.mangaId }.distinct()
    val existingMangas = mangaRepository.getMangaByIds(mangaIds).associateBy { it.id }

    return this.mapNotNull { displayManga ->
        val dbManga = existingMangas[displayManga.mangaId]
        when (dbManga == null) {
            true -> null
            else ->
                displayManga.copy(
                    originalTitle = dbManga.title,
                    userTitle = dbManga.user_title ?: "",
                    currentArtwork =
                        displayManga.currentArtwork.copy(
                            cover = dbManga.user_cover ?: "",
                            originalCover = dbManga.thumbnail_url ?: MdConstants.noCoverUrl,
                        ),
                )
        }
    }
}

fun List<DisplayManga>.unique(): List<DisplayManga> {
    return this.distinctBy { it.url }
}
