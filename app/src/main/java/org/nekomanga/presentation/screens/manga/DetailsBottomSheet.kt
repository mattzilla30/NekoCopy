package org.nekomanga.presentation.screens.manga

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import org.nekomanga.presentation.components.sheets.ArtworkSheet
import org.nekomanga.presentation.components.sheets.ExternalLinksSheet
import org.nekomanga.presentation.components.sheets.FilterChapterSheet
import org.nekomanga.presentation.components.theme.ThemeColorState

/** Sealed class that holds the types of bottom sheets the details screen can show */
sealed class DetailsBottomSheetScreen {
    object ExternalLinksSheet : DetailsBottomSheetScreen()

    object ArtworkSheet : DetailsBottomSheetScreen()

    object FilterChapterSheet : DetailsBottomSheetScreen()
}

@Composable
fun DetailsBottomSheet(
    currentScreen: DetailsBottomSheetScreen,
    themeColorState: ThemeColorState,
    mangaDetailScreenState: MangaConstants.MangaDetailScreenState,
    openInWebView: (String, String) -> Unit,
    coverActions: MangaConstants.CoverActions,
    chapterFilterActions: MangaConstants.ChapterFilterActions,
    onNavigate: (DetailsBottomSheetScreen?) -> Unit,
) {
    val context = LocalContext.current
    when (currentScreen) {
        is DetailsBottomSheetScreen.ExternalLinksSheet -> {
            ExternalLinksSheet(
                themeColorState = themeColorState,
                externalLinks = mangaDetailScreenState.manga.externalLinks,
                onLinkClick = { url, title ->
                    onNavigate(null)
                    openInWebView(url, title)
                },
            )
        }
        is DetailsBottomSheetScreen.ArtworkSheet -> {
            ArtworkSheet(
                themeColorState = themeColorState,
                alternativeArtwork = mangaDetailScreenState.manga.alternativeArtwork,
                saveClick = { artwork ->
                    onNavigate(null)
                    coverActions.save(artwork)
                },
                shareClick = { url -> coverActions.share(context, url) },
                setClick = { url ->
                    onNavigate(null)
                    coverActions.set(url)
                },
                resetClick = {
                    onNavigate(null)
                    coverActions.reset()
                },
            )
        }
        is DetailsBottomSheetScreen.FilterChapterSheet -> {
            FilterChapterSheet(
                themeColorState = themeColorState,
                sortFilter = mangaDetailScreenState.chapters.chapterSortFilter,
                changeSort = chapterFilterActions.changeSort,
                changeFilter = chapterFilterActions.changeFilter,
                filter = mangaDetailScreenState.chapters.chapterFilter,
                scanlatorFilter = mangaDetailScreenState.chapters.chapterScanlatorFilter,
                languageFilter = mangaDetailScreenState.chapters.chapterLanguageFilter,
                changeScanlatorFilter = chapterFilterActions.changeScanlator,
                changeLanguageFilter = chapterFilterActions.changeLanguage,
                setAsGlobal = chapterFilterActions.setAsGlobal,
            )
        }
    }
}
