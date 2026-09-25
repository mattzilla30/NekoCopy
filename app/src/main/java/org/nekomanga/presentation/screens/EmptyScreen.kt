package org.nekomanga.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.AutoSizeText
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.theme.KittyShapes
import org.nekomanga.presentation.theme.Size

private val EmptyShapeSize = 180.dp

/** Cat faces shown inside the kitty head on empty and error screens. */
private val ErrorFaces =
    listOf(
        "=^･ω･^=",
        "(=ↀωↀ=)",
        "ฅ^•ﻌ•^ฅ",
        "(=^-ω-^=)",
        "=^..^=",
        "(=｀ω´=)",
        "(=;ェ;=)",
        "(ΦωΦ)",
        "(=ＴェＴ=)",
        "(^・ω・^ )",
        "ฅ(=ˇωˇ=)ฅ",
        "(=´ᆺ`=)",
        "(=ʘᆽʘ=)",
        "(=ಠᆽಠ=)",
        "(ᓀ‸ᓂ)",
        "ᓚᘏᗢ",
        "(=◕ᆽ◕ฺ=)",
        "(^◔ᴥ◔^)",
        "=^o.o^=",
        "(=ﾟωﾟ)ﾉ",
    )

@Composable
fun EmptyScreen(
    message: UiText,
    modifier: Modifier = Modifier.fillMaxSize(),
    actions: List<Action> = listOf(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val errorFace = remember { ErrorFaces.random() }
    Column(
        modifier = modifier.padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier.size(EmptyShapeSize)
                    .clip(KittyShapes.Head.toShape())
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    // The ears take the top of the head, so the face sits low in the shape.
                    .padding(
                        start = Size.extraLarge,
                        end = Size.extraLarge,
                        top = Size.extraHuge,
                        bottom = Size.medium,
                    )
                    .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            AutoSizeText(
                text = errorFace,
                style =
                    MaterialTheme.typography.displaySmallEmphasized.copy(
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                textAlign = TextAlign.Center,
            )
        }
        Gap(Size.large)

        Text(
            text = message.asString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Size.medium),
            textAlign = TextAlign.Center,
            color =
                MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
        )
        Gap(Size.large)

        actions.forEach { action ->
            FilledTonalButton(onClick = action.onClick, shapes = ButtonDefaults.shapes()) {
                Text(text = action.text.asString())
            }
            Gap(Size.small)
        }
    }
}

data class Action(val text: UiText, val onClick: () -> Unit = {})
