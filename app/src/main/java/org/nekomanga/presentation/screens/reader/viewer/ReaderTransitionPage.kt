package org.nekomanga.presentation.screens.reader.viewer

import android.graphics.PointF
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import org.nekomanga.R
import org.nekomanga.presentation.theme.NekoTheme
import org.nekomanga.presentation.theme.Size

/**
 * Pure, stateless interstitial transition page between adjacent chapters. Consumes an immutable
 * [ChapterTransitionUiModel] prepared by the domain layer.
 */
@Composable
fun ReaderTransitionPage(
    uiModel: ChapterTransitionUiModel,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onTap: ((PointF) -> Unit)? = null,
    onCardClick: (() -> Unit)? = null,
) {
    val tapModifier =
        if (onTap != null) {
            Modifier.pointerInput(onTap) {
                detectTapGestures(
                    onTap = { offset ->
                        val screenWidth = size.width.toFloat()
                        val screenHeight = size.height.toFloat()
                        if (screenWidth > 0 && screenHeight > 0) {
                            val pos = PointF(offset.x / screenWidth, offset.y / screenHeight)
                            onTap(pos)
                        } else {
                            onTap(PointF(0.5f, 0.5f))
                        }
                    }
                )
            }
        } else {
            Modifier
        }

    val isPrevWithoutTo = uiModel is ChapterTransitionUiModel.Prev && uiModel.toChapter == null

    Box(
        modifier =
            modifier
                .then(tapModifier)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    start = Size.mediumLarge,
                    end = Size.mediumLarge,
                    top =
                        if (isPrevWithoutTo) {
                            Size.appBarHeight + Size.large
                        } else {
                            Size.small
                        },
                    bottom = Size.large,
                ),
        contentAlignment = Alignment.Center,
    ) {
        val toChapter =
            when (uiModel) {
                is ChapterTransitionUiModel.Prev -> uiModel.toChapter
                is ChapterTransitionUiModel.Next -> uiModel.toChapter
            }

        ElevatedCard(
            shape = RoundedCornerShape(Size.mediumLarge),
            elevation =
                CardDefaults.elevatedCardElevation(defaultElevation = Size.small - Size.extraTiny),
            modifier =
                Modifier.fillMaxWidth()
                    .then(
                        if (onCardClick != null && toChapter != null) {
                            Modifier.clickable(onClick = onCardClick)
                        } else {
                            Modifier
                        }
                    ),
        ) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = Size.large, vertical = Size.largePlus),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (uiModel) {
                    is ChapterTransitionUiModel.Prev -> PrevChapterTransitionContent(uiModel)
                    is ChapterTransitionUiModel.Next -> NextChapterTransitionContent(uiModel)
                }

                val missingCount =
                    when (uiModel) {
                        is ChapterTransitionUiModel.Prev -> uiModel.missingChaptersCount
                        is ChapterTransitionUiModel.Next -> uiModel.missingChaptersCount
                    }
                MissingChapterWarningSection(missingChaptersCount = missingCount)

                if (toChapter != null) {
                    ChapterPreloadStatusSection(
                        preloadState = toChapter.preloadState,
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
}

/**
 * Builds the [ChapterTransitionUiModel] for [transition], including the load state of the chapter
 * it leads to.
 */
@Composable
fun rememberChapterTransitionUiModel(transition: ChapterTransition): ChapterTransitionUiModel {
    val targetState = transition.to?.stateFlow?.collectAsStateWithLifecycle()?.value
    return remember(transition, targetState) { ChapterTransitionUiModel.from(transition) }
}

@Composable
private fun PrevChapterTransitionContent(uiModel: ChapterTransitionUiModel.Prev) {
    val prevChapter = uiModel.toChapter
    if (prevChapter != null) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                text = stringResource(R.string.previous_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Size.small - Size.extraTiny),
            ) {
                Text(
                    text = prevChapter.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }

            Spacer(modifier = Modifier.height(Size.mediumLarge))

            Text(
                text = stringResource(R.string.current_chapter),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = uiModel.fromChapterName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Size.small - Size.extraTiny),
            )
        }
    } else {
        Text(
            text = stringResource(R.string.theres_no_previous_chapter),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = Size.smedium),
        )
    }
}

@Composable
private fun NextChapterTransitionContent(uiModel: ChapterTransitionUiModel.Next) {
    val nextChapter = uiModel.toChapter
    if (nextChapter != null) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                text = stringResource(R.string.finished_chapter),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = uiModel.fromChapterName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Size.small - Size.extraTiny),
            )

            Spacer(modifier = Modifier.height(Size.mediumLarge))

            Text(
                text = stringResource(R.string.next_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Size.small - Size.extraTiny),
            ) {
                Text(
                    text = nextChapter.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    } else {
        Text(
            text = stringResource(R.string.theres_no_next_chapter),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = Size.smedium),
        )
    }
}

@Composable
private fun MissingChapterWarningSection(missingChaptersCount: Int) {
    if (missingChaptersCount <= 0) return

    Spacer(modifier = Modifier.height(Size.medium))
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(Size.small),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Size.smedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Size.large),
            )
            Spacer(modifier = Modifier.width(Size.small))
            Text(
                text =
                    pluralStringResource(
                        R.plurals.missing_chapters_warning,
                        missingChaptersCount,
                        missingChaptersCount,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun ChapterPreloadStatusSection(
    preloadState: ChapterTransitionUiModel.PreloadState,
    onRetry: () -> Unit,
) {
    if (preloadState is ChapterTransitionUiModel.PreloadState.Error) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = Size.mediumLarge),
        ) {
            Text(
                text =
                    stringResource(
                        R.string.failed_to_load_pages_,
                        preloadState.message,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Size.small))
            Button(onClick = onRetry, shapes = ButtonDefaults.shapes()) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

@Preview
@Composable
private fun PrevTransitionPreview() {
    NekoTheme {
        ReaderTransitionPage(
            uiModel =
                ChapterTransitionUiModel.Prev(
                    fromChapterName = "Chapter 2",
                    toChapter =
                        ChapterTransitionUiModel.TargetChapterInfo(
                            chapterId = 1L,
                            name = "Chapter 1",
                        ),
                    missingChaptersCount = 0,
                ),
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun NextTransitionPreview() {
    NekoTheme {
        ReaderTransitionPage(
            uiModel =
                ChapterTransitionUiModel.Next(
                    fromChapterName = "Chapter 1",
                    toChapter =
                        ChapterTransitionUiModel.TargetChapterInfo(
                            chapterId = 2L,
                            name = "Chapter 2",
                        ),
                    missingChaptersCount = 0,
                ),
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun MissingChaptersWarningPreview() {
    NekoTheme {
        ReaderTransitionPage(
            uiModel =
                ChapterTransitionUiModel.Next(
                    fromChapterName = "Chapter 1",
                    toChapter =
                        ChapterTransitionUiModel.TargetChapterInfo(
                            chapterId = 5L,
                            name = "Chapter 5",
                        ),
                    missingChaptersCount = 3,
                ),
            onRetry = {},
        )
    }
}
