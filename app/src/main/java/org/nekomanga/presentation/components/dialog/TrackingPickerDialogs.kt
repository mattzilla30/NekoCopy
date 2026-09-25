package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import org.nekomanga.R
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.presentation.components.ExpressivePicker
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun TrackingChapterDialog(
    themeColorState: ThemeColorState,
    track: TrackItem,
    onDismiss: () -> Unit,
    trackChapterChanged: (Int) -> Unit,
) {
    val last = if (track.totalChapters > 0) track.totalChapters else 10000
    TrackingPickerDialog(
        title = stringResource(R.string.chapters),
        themeColorState = themeColorState,
        items = (0..last).toList(),
        initial = track.lastChapterRead.toInt(),
        onDismiss = onDismiss,
        onConfirm = trackChapterChanged,
    )
}

@Composable
fun TrackingScoreDialog(
    themeColorState: ThemeColorState,
    trackAndService: TrackingConstants.TrackAndService,
    onDismiss: () -> Unit,
    trackScoreChange: (Int) -> Unit,
) {
    val scores = trackAndService.service.scoreList
    val displayedScore = trackAndService.service.displayScore(trackAndService.track)
    TrackingPickerDialog(
        title = stringResource(R.string.score),
        themeColorState = themeColorState,
        items = scores,
        initial = scores.getOrElse(scores.indexOf(displayedScore)) { scores[0] },
        onDismiss = onDismiss,
        onConfirm = { score -> trackScoreChange(scores.indexOf(score)) },
    )
}

/** A dialog with one scroll wheel over [items]. OK passes the picked item to [onConfirm]. */
@Composable
private fun <T> TrackingPickerDialog(
    title: String,
    themeColorState: ThemeColorState,
    items: List<T>,
    initial: T,
    onDismiss: () -> Unit,
    onConfirm: (T) -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration,
        LocalTextSelectionColors provides themeColorState.textSelectionColors,
    ) {
        var current by remember { mutableStateOf(initial) }
        val buttonColors =
            ButtonDefaults.textButtonColors(contentColor = themeColorState.primaryColor)

        AlertDialog(
            title = {
                Text(text = title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(Size.medium).fillMaxWidth(),
                ) {
                    ExpressivePicker(
                        value = current,
                        themeColorState = themeColorState,
                        items = items,
                        onValueChange = { current = it },
                        modifier = Modifier.fillMaxWidth(.4f),
                    )
                }
            },
            onDismissRequest = onDismiss,
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = buttonColors,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(current)
                        onDismiss()
                    },
                    colors = buttonColors,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
        )
    }
}
