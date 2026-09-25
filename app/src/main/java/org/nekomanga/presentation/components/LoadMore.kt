package org.nekomanga.presentation.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

/**
 * Calls [loadMore] once the last visible item comes within [itemsFromEnd] of the end. The effect
 * restarts when [itemCount] changes, so a page too short to scroll still loads the next one.
 */
@Composable
fun LazyListState.LoadMoreNearEnd(
    enabled: Boolean,
    itemCount: Int,
    itemsFromEnd: Int = 1,
    loadMore: () -> Unit,
) {
    LoadMoreNearEnd(this, enabled, itemCount, itemsFromEnd, loadMore) {
        layoutInfo.visibleItemsInfo.lastOrNull()?.index to layoutInfo.totalItemsCount
    }
}

@Composable
fun LazyGridState.LoadMoreNearEnd(
    enabled: Boolean,
    itemCount: Int,
    itemsFromEnd: Int = 1,
    loadMore: () -> Unit,
) {
    LoadMoreNearEnd(this, enabled, itemCount, itemsFromEnd, loadMore) {
        layoutInfo.visibleItemsInfo.lastOrNull()?.index to layoutInfo.totalItemsCount
    }
}

@Composable
private fun LoadMoreNearEnd(
    state: Any,
    enabled: Boolean,
    itemCount: Int,
    itemsFromEnd: Int,
    loadMore: () -> Unit,
    position: () -> Pair<Int?, Int>,
) {
    if (!enabled) return
    LaunchedEffect(state, itemCount) {
        snapshotFlow {
            val (lastVisible, total) = position()
            (lastVisible ?: 0) >= total - itemsFromEnd
        }
            .collect { atEnd -> if (atEnd) loadMore() }
    }
}
