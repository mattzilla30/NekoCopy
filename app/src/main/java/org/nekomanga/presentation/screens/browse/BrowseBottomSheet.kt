package org.nekomanga.presentation.screens.browse

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.nekomanga.presentation.components.sheets.FilterBrowseSheet

/** The search filters sheet of the Browse screen. */
@Composable
fun BrowseFilterSheet(
    browseScreenState: BrowseScreenState,
    filterActions: FilterActions,
    closeSheet: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    FilterBrowseSheet(
        filters = browseScreenState.filters,
        savedFilters = browseScreenState.savedFilters,
        defaultContentRatings = browseScreenState.defaultContentRatings,
        filterClick = {
            keyboardController?.hide()
            closeSheet()
            filterActions.filterClick()
        },
        resetClick = {
            keyboardController?.hide()
            filterActions.resetClick()
        },
        filterChanged = filterActions.filterChanged,
        saveClick = filterActions.saveFilterClick,
        deleteFilterClick = filterActions.deleteFilterClick,
        filterDefaultClick = filterActions.filterDefaultClick,
        loadFilter = filterActions.loadFilter,
    )
}
