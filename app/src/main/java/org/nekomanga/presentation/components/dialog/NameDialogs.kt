package org.nekomanga.presentation.components.dialog

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.extensions.runOnEnterKeyPressed

@Composable
fun AddEditCategoryDialog(
    themeColorState: ThemeColorState = defaultThemeColorState(),
    categorySelected: String = "",
    currentCategories: List<CategoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    NameDialog(
        title =
            stringResource(
                if (categorySelected.isBlank()) R.string.new_category else R.string.edit_category
            ),
        label = null,
        nameTakenError = stringResource(R.string.category_with_name_exists),
        initialName = categorySelected,
        isTaken = { name -> currentCategories.any { it.name.equals(name, true) } },
        themeColorState = themeColorState,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
fun SaveFilterDialog(
    themeColorState: ThemeColorState,
    currentSavedFilters: List<BrowseFilterImpl>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    NameDialog(
        title = stringResource(R.string.save_filter),
        label = stringResource(R.string.name),
        nameTakenError = stringResource(R.string.filter_with_name_exists),
        initialName = "",
        isTaken = { name -> currentSavedFilters.any { it.name.equals(name, true) } },
        themeColorState = themeColorState,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

/**
 * Asks for a name. Save stays disabled while the name is blank, unchanged from [initialName], or
 * [isTaken]. A taken name shows [nameTakenError] under the field.
 */
@Composable
private fun NameDialog(
    title: String,
    label: String?,
    nameTakenError: String,
    initialName: String,
    isTaken: (String) -> Boolean,
    themeColorState: ThemeColorState,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val changed = name.isNotBlank() && name != initialName
    val taken = changed && isTaken(name)
    val canSave = changed && !taken
    val saveAction = {
        if (canSave) {
            onConfirm(name)
            onDismiss()
        }
    }
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = themeColorState.primaryColor)

    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration,
        LocalTextSelectionColors provides themeColorState.textSelectionColors,
    ) {
        AlertDialog(
            title = { Text(text = title) },
            text = {
                OutlinedTextField(
                    modifier = Modifier.runOnEnterKeyPressed(saveAction),
                    value = name,
                    onValueChange = { name = it },
                    label = label?.let { { Text(text = it) } },
                    singleLine = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            cursorColor = themeColorState.primaryColor,
                            focusedLabelColor = themeColorState.primaryColor,
                            focusedBorderColor = themeColorState.primaryColor,
                        ),
                    isError = taken,
                    supportingText = { if (taken) Text(text = nameTakenError) },
                )
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = saveAction,
                    enabled = canSave,
                    colors = buttonColors,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = buttonColors,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}
