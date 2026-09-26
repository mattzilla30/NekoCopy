package org.nekomanga.presentation.screens.similar

import androidx.compose.runtime.Immutable
import org.nekomanga.domain.manga.DisplayManga

@Immutable
data class SimilarScreenState(
    val isRefreshing: Boolean = false,
    val incognitoMode: Boolean = false,
    val allDisplayManga: Map<Int, List<DisplayManga>> = mapOf(),
    val error: String? = null,
    val isList: Boolean,
    val outlineCovers: Boolean,
    val dynamicCovers: Boolean,
)
