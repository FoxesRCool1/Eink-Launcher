package io.github.foxesrcool1.einklauncher.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
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
import java.util.Locale

/**
 * A control.
 *
 * E-ink rule 5: the pressed state is an instant colour invert. There is no
 * ripple, no shadow and no fade, because each of those is an animation or a
 * grey wash, and both look bad on the panel.
 */
@Composable
fun InvertPressButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bordered: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val inverted = pressed && enabled

    val background = if (inverted) EinkColors.Ink else EinkColors.Paper
    val foreground = when {
        !enabled -> EinkColors.Faded
        inverted -> EinkColors.Paper
        else -> EinkColors.Ink
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = EinkDimens.touchTarget)
            .background(background)
            .then(
                if (bordered) {
                    Modifier.border(
                        width = EinkDimens.rule,
                        color = if (enabled) EinkColors.Ink else EinkColors.Faded,
                    )
                } else {
                    Modifier
                },
            )
            .einkClickable(enabled = enabled, interactionSource = interactionSource, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        EinkText(
            text = text.uppercase(Locale.ROOT),
            style = EinkType.button.copy(color = foreground),
            maxLines = 1,
        )
    }
}
