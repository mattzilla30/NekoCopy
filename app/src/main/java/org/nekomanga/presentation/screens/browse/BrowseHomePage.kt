package org.nekomanga.presentation.screens.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.source.latest.SerializableDisplayScreenType
import eu.kanade.tachiyomi.ui.source.latest.toSerializable
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.MangaCover
import org.nekomanga.presentation.components.MangaGridSubtitle
import org.nekomanga.presentation.components.MangaGridTitle
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun BrowseHomePage(
    browseHomePageManga: List<HomePageManga>,
    shouldOutlineCover: Boolean,
    dynamicCovers: Boolean,
    useVividColorHeaders: Boolean,
    onClick: (Long) -> Unit,
    titleClick: (SerializableDisplayScreenType) -> Unit,
    randomClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp

    val headerColor =
        when (useVividColorHeaders) {
            true -> MaterialTheme.colorScheme.primary
            false -> MaterialTheme.colorScheme.onSurface
        }

    val coverSize =
        remember(screenWidth, screenHeight) { (maxOf(screenHeight, screenWidth) / 5).dp }

    LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = contentPadding) {
        items(
            items = browseHomePageManga,
            key = { homePageManga -> homePageManga.displayScreenType.hashCode() },
        ) { homePageManga ->
            val headerText = homePageManga.displayScreenType.title.asString()
            val mangaList = homePageManga.displayManga

            if (mangaList.isNotEmpty()) {
                TextButton(
                    onClick = { titleClick(homePageManga.displayScreenType.toSerializable()) },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = headerText,
                            color = headerColor,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Gap(Size.tiny)
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowForward,
                            modifier = Modifier.size(Size.large),
                            tint = headerColor,
                            contentDescription = null,
                        )
                    }
                }
                Gap(Size.tiny)
                LazyRow(
                    modifier = Modifier.wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Size.small),
                    contentPadding = PaddingValues(horizontal = Size.small),
                ) {
                    items(items = mangaList, key = { displayManga -> displayManga.mangaId }) {
                        displayManga ->
                        Column(
                            modifier =
                                Modifier.width(coverSize)
                                    .clip(RoundedCornerShape(Shapes.coverRadius))
                                    .clickable { onClick(displayManga.mangaId) }
                        ) {
                            MangaCover.Square(
                                artwork = displayManga.currentArtwork,
                                shouldOutlineCover = shouldOutlineCover,
                                dynamicCover = dynamicCovers,
                                modifier = Modifier.requiredHeight(coverSize),
                            )
                            MangaGridTitle(title = displayManga.getTitle())
                            MangaGridSubtitle(subtitleText = displayManga.displayText)
                        }
                    }
                }
            }
        }
        item {
            TextButton(onClick = randomClick, shapes = ButtonDefaults.shapes()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.random_manga),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Gap(Size.tiny)
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowForward,
                        modifier = Modifier.size(Size.large),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
