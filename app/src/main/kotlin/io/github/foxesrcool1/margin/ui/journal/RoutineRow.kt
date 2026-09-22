package io.github.foxesrcool1.margin.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.margin.core.routine.RoutineStatus
import io.github.foxesrcool1.margin.design.EinkColors
import io.github.foxesrcool1.margin.design.EinkDimens
import io.github.foxesrcool1.margin.design.EinkType
import io.github.foxesrcool1.margin.design.components.CapsLabel
import io.github.foxesrcool1.margin.design.components.EinkRow
import io.github.foxesrcool1.margin.design.components.EinkText
import io.github.foxesrcool1.margin.design.components.HairlineDivider
import io.github.foxesrcool1.margin.design.components.IconPressButton
import io.github.foxesrcool1.margin.design.icons.Lucide

/**
 * One item in the routine.
 *
 * A tap ticks it off. The order is changed with two arrows rather than by
 * dragging: a drag needs a moving picture under the finger, and rule 1 has no
 * exceptions.
 */
@Composable
fun RoutineRow(
    status: RoutineStatus,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EinkRow(onClick = onToggle, onLongClick = onRemove, modifier = modifier) { pressed ->
        val foreground = if (pressed) EinkColors.Paper else EinkColors.Ink
        val faded = if (pressed) EinkColors.Paper else EinkColors.Faded

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .then(
                        if (status.doneToday) {
                            Modifier.background(foreground, CircleShape)
                        } else {
                            Modifier.border(EinkDimens.hairline, foreground, CircleShape)
                        },
                    ),
            )
            Spacer(modifier = Modifier.width(14.dp))

            EinkText(
                text = status.item.label,
                style = EinkType.rowTitle.copy(
                    color = if (status.doneToday) faded else foreground,
                ),
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconPressButton(
                    icon = Lucide.ArrowUp,
                    label = "Move up",
                    enabled = !isFirst,
                    onClick = { onMove(-1) },
                )
                IconPressButton(
                    icon = Lucide.ArrowDown,
                    label = "Move down",
                    enabled = !isLast,
                    onClick = { onMove(1) },
                )
            }
        }

        if (status.item.target.isNotBlank()) {
            CapsLabel(
                text = "Opens ${status.item.target}",
                style = EinkType.capsSmall.copy(color = faded),
                maxLines = 1,
            )
        }
    }
    HairlineDivider(color = EinkColors.Faded)
}
