package io.github.foxesrcool1.einklauncher.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.einkClickable

/** One large word in a [WordMenu]. */
data class WordMenuItem(
    val label: String,
    val enabled: Boolean = true,
)

/**
 * The main menu: a short column of large serif words on the left.
 *
 * The selected word carries a short rule in front of it. There is no colour,
 * no box and no icon, because words are the whole design.
 */
@Composable
fun WordMenu(
    items: List<WordMenuItem>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    verticalGap: androidx.compose.ui.unit.Dp = 4.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalGap),
    ) {
        items.forEachIndexed { index, item ->
            WordMenuRow(
                item = item,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun WordMenuRow(
    item: WordMenuItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val inverted = pressed && item.enabled

    val foreground = when {
        !item.enabled -> EinkColors.Faded
        inverted -> EinkColors.Paper
        else -> EinkColors.Ink
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = EinkDimens.touchTarget)
            .background(if (inverted) EinkColors.Ink else EinkColors.Paper)
            .einkClickable(
                enabled = item.enabled,
                interactionSource = interactionSource,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(EinkDimens.rule)
                .background(if (selected) foreground else EinkColors.Paper),
        )
        Spacer(modifier = Modifier.width(14.dp))
        EinkText(
            text = item.label,
            style = EinkType.word.copy(color = foreground),
            maxLines = 1,
        )
    }
}
