package org.nekomanga.presentation.components.bars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.ToolTipButton
import org.nekomanga.presentation.components.icons.IncognitoIcon
import org.nekomanga.presentation.extensions.runOnEnterKeyPressed
import org.nekomanga.presentation.theme.Size

/** An M3 Expressive top app bar whose title slot turns into a search field on demand. */
@Composable
fun SearchTopAppBar(
    onSearchText: (String?) -> Unit,
    searchPlaceHolder: String,
    color: Color,
    navigationIconLabel: String,
    navigationIcon: ImageVector,
    incognitoMode: Boolean = false,
    onNavigationIconClicked: () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var showTextField by rememberSaveable { mutableStateOf(false) }
    var alreadyRequestedFocus by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    TopAppBar(
        title = {
            if (showTextField) {
                TextField(
                    modifier =
                        Modifier.fillMaxWidth()
                            .focusRequester(focusRequester)
                            .runOnEnterKeyPressed { onSearchText(searchText) },
                    value = searchText,
                    placeholder = { Text(text = stringResource(id = R.string.search_chapters)) },
                    onValueChange = {
                        searchText = it
                        onSearchText(it)
                    },
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            cursorColor = LocalContentColor.current,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = searchText.isNotBlank(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            ToolTipButton(
                                toolTipLabel = stringResource(id = R.string.clear),
                                icon = Icons.Filled.Close,
                                onClick = {
                                    onSearchText("")
                                    searchText = ""
                                },
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchText(searchText) }),
                )
                LaunchedEffect(Unit) {
                    if (!alreadyRequestedFocus) {
                        focusRequester.requestFocus()
                        alreadyRequestedFocus = true
                    }
                    if (searchText.isNotBlank()) {
                        onSearchText(searchText)
                    }
                }
            }
        },
        navigationIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ToolTipButton(
                    toolTipLabel = navigationIconLabel,
                    icon = navigationIcon,
                    onClick = onNavigationIconClicked,
                )
                if (incognitoMode) {
                    Gap(Size.small)
                    IncognitoIcon()
                    Gap(Size.small)
                }
            }
        },
        actions = {
            ToolTipButton(
                toolTipLabel = searchPlaceHolder,
                icon = if (showTextField) Icons.Filled.SearchOff else Icons.Filled.Search,
                onClick = {
                    searchText = ""
                    alreadyRequestedFocus = false
                    onSearchText(null)
                    showTextField = !showTextField
                },
            )
            actions()
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = color,
                scrolledContainerColor = Color.Transparent,
            ),
        scrollBehavior = scrollBehavior,
    )
}
