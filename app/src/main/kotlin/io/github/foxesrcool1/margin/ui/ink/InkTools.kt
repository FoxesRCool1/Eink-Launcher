package io.github.foxesrcool1.margin.ui.ink

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.margin.design.EinkColors
import io.github.foxesrcool1.margin.design.EinkDimens
import io.github.foxesrcool1.margin.design.EinkShapes
import io.github.foxesrcool1.margin.design.components.IconPressButton
import io.github.foxesrcool1.margin.design.einkClickable
import io.github.foxesrcool1.margin.design.icons.Lucide

/** Pen, marker and eraser. The one in use stays black. Every ink screen has these three. */
@Composable
fun InkModeButtons(mode: InkMode, onPick: (InkMode) -> Unit) {
    IconPressButton(icon = Lucide.PenLine, label = "Pen", selected = mode == InkMode.Pen, onClick = { onPick(InkMode.Pen) })
    IconPressButton(icon = Lucide.Highlighter, label = "Marker", selected = mode == InkMode.Highlighter, onClick = { onPick(InkMode.Highlighter) })
    IconPressButton(icon = Lucide.Eraser, label = "Eraser", selected = mode == InkMode.Eraser, onClick = { onPick(InkMode.Eraser) })
}

/**
 * The pen width, shown as a dot of that width. A tap goes to the next width.
 * No icon says "fine, medium, bold" as plainly as the dot itself does.
 */
@Composable
fun PenWidthButton(width: Float, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val dot = when (width) {
        PenWidths.FINE -> 5.dp
        PenWidths.BOLD -> 16.dp
        else -> 10.dp
    }
    Box(
        modifier = modifier
            .size(EinkDimens.touchTarget)
            .clip(EinkShapes.control)
            .background(if (pressed) EinkColors.Ink else EinkColors.Paper)
            .einkClickable(interactionSource = interactionSource, onClick = onNext)
            .semantics {
                contentDescription = "Pen width: ${PenWidths.label(width)}"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(dot)
                .background(if (pressed) EinkColors.Paper else EinkColors.Ink, CircleShape),
        )
    }
}
