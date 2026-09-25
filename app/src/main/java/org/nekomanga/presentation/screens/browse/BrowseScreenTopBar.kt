package org.nekomanga.presentation.screens.browse

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.nekomanga.R
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.functions.getTopAppBarColor
import org.nekomanga.presentation.theme.Size

/**
 * The Browse top bar: a search bar that opens the search filters, then Follows (when logged in),
 * display settings and the main menu. Away from the home page, the search bar's back arrow returns
 * to it.
 */
@Composable
fun BrowseScreenTopBar(
    browseScreenState: BrowseScreenState,
    scrollBehavior: TopAppBarScrollBehavior,
    mainDropDown: AppBar.MainDropdown,
    openSheetClick: () -> Unit,
    searchClick: () -> Unit,
    homeClick: () -> Unit,
    followsClick: () -> Unit,
) {
    val (color, _, _) = getTopAppBarColor(true, false)
    val screenType = browseScreenState.screenType
    val onHomepage = screenType == BrowseScreenType.Homepage

    TopAppBar(
        title = {
            BrowseSearchBar(
                text =
                    when (screenType) {
                        BrowseScreenType.Follows -> stringResource(R.string.follows)
                        BrowseScreenType.Filter ->
                            browseScreenState.filters.query.text.ifBlank {
                                stringResource(R.string.search_results)
                            }
                        else -> null
                    },
                onHomepage = onHomepage,
                searchClick = searchClick,
                homeClick = homeClick,
            )
        },
        actions = {
            AppBarActions(
                actions =
                    buildList {
                        if (browseScreenState.isLoggedIn) {
                            add(
                                AppBar.Action(
                                    title = UiText.StringResource(R.string.follows),
                                    icon =
                                        if (screenType == BrowseScreenType.Follows) {
                                            Icons.Filled.Bookmarks
                                        } else {
                                            Icons.Outlined.Bookmarks
                                        },
                                    onClick = followsClick,
                                )
                            )
                        }
                        add(
                            AppBar.Action(
                                title = UiText.StringResource(R.string.settings),
                                icon = Icons.Outlined.Tune,
                                onClick = openSheetClick,
                            )
                        )
                        add(mainDropDown)
                    }
            )
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = color,
                scrolledContainerColor = color,
            ),
        scrollBehavior = scrollBehavior,
    )
}

/**
 * A pill that looks like a search field. Tapping it opens the search filters. [text] shows what the
 * page below holds, or the search hint on the home page.
 */
@Composable
private fun BrowseSearchBar(
    text: String?,
    onHomepage: Boolean,
    searchClick: () -> Unit,
    homeClick: () -> Unit,
) {
    Surface(
        onClick = searchClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onHomepage) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.padding(start = Size.medium).size(Size.large),
                )
            } else {
                IconButton(onClick = homeClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.home_page),
                    )
                }
            }
            Text(
                text = text ?: stringResource(R.string.search_mangadex),
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (text == null) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier.weight(1f)
                        .padding(
                            start = if (onHomepage) Size.smedium else Size.none,
                            end = Size.medium,
                        ),
            )
        }
    }
}
