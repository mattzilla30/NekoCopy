package org.nekomanga.presentation.components.sheets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.nekomanga.presentation.theme.Size

/** Tracks which bottom sheet of type [T] a screen shows, if any. */
@Stable
class SheetHost<T : Any>(val state: SheetState, private val scope: CoroutineScope) {
    var current: T? by mutableStateOf(null)
        private set

    fun open(sheet: T) {
        current = sheet
    }

    /** Slides the sheet away, then forgets it. */
    fun close() {
        scope.launch {
            state.hide()
            current = null
        }
    }

    internal fun dismissed() {
        current = null
    }
}

@Composable
fun <T : Any> rememberSheetHost(): SheetHost<T> {
    val scope = rememberCoroutineScope()
    val state =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
    return remember(state, scope) { SheetHost(state, scope) }
}

/** Shows the open sheet, if any, in a modal bottom sheet. Back or a swipe down closes it. */
@Composable
fun <T : Any> SheetHost<T>.Sheet(
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    content: @Composable (T) -> Unit,
) {
    val sheet = current ?: return
    LaunchedEffect(sheet) { state.show() }
    BackHandler(enabled = state.isVisible) { close() }
    ModalBottomSheet(sheetState = state, shape = shape, onDismissRequest = ::dismissed) {
        Box(modifier = Modifier.defaultMinSize(minHeight = Size.extraExtraTiny)) { content(sheet) }
    }
}
