package io.github.foxesrcool1.einklauncher.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.BotanicalCorner
import io.github.foxesrcool1.einklauncher.design.components.BotanicalSprig
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider

/**
 * The shape every screen shares: a title block, a rule, the content, and one
 * corner drawing. Nothing moves and nothing scrolls.
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    corner: BotanicalCorner? = BotanicalCorner.BottomEnd,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (corner != null) {
            BotanicalSprig(
                corner = corner,
                modifier = Modifier.align(
                    when (corner) {
                        BotanicalCorner.TopStart -> Alignment.TopStart
                        BotanicalCorner.TopEnd -> Alignment.TopEnd
                        BotanicalCorner.BottomStart -> Alignment.BottomStart
                        BotanicalCorner.BottomEnd -> Alignment.BottomEnd
                    },
                ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(EinkDimens.screenMargin),
        ) {
            if (overline != null) {
                CapsLabel(text = overline, style = EinkType.capsSmall)
                Spacer(modifier = Modifier.height(6.dp))
            }
            EinkText(text = title, style = EinkType.title, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(EinkDimens.targetGap))
            HairlineDivider(thickness = EinkDimens.rule)
            Spacer(modifier = Modifier.height(EinkDimens.blockGap))
            content()
        }
    }
}
