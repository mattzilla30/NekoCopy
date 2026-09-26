package org.nekomanga.presentation.screens.manga

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import org.nekomanga.R
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.theme.ThemeColorState

@Composable
fun MangaDetailsAppBarActions(
    chapterActions: MangaConstants.ChapterActions,
    themeColorState: ThemeColorState,
    chapters: List<ChapterItem>,
) {
    AppBarActions(
        themeColorState = themeColorState,
        actions =
            listOf(
                AppBar.OverflowAction(
                    title = UiText.StringResource(R.string.mark_all_as),
                    children =
                        listOf(
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.read),
                                onClick = {
                                    chapterActions.mark(chapters, ChapterMarkActions.Read(true))
                                },
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.unread),
                                onClick = {
                                    chapterActions.mark(chapters, ChapterMarkActions.Unread(true))
                                },
                            ),
                        ),
                )
            ),
    )
}
