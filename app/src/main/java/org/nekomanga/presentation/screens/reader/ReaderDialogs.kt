package org.nekomanga.presentation.screens.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.theme.Size

/** Lets the reader pick single, double, or split pages for the paged viewer. */
@Composable
fun PageLayoutDialog(
    selected: PageLayout,
    onSelect: (PageLayout) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.page_layout)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                listOf(PageLayout.SINGLE_PAGE, PageLayout.DOUBLE_PAGES, PageLayout.SPLIT_PAGES)
                    .forEach { layout ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .selectable(
                                        selected = layout == selected,
                                        role = Role.RadioButton,
                                        onClick = {
                                            onSelect(layout)
                                            onDismiss()
                                        },
                                    )
                                    .padding(vertical = Size.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = layout == selected, onClick = null)
                            Gap(Size.medium)
                            Text(text = stringResource(layout.stringRes))
                        }
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

/** Confirms using a reader page as the manga's cover. */
@Composable
fun SetCoverDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text = stringResource(R.string.use_image_as_cover)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}
