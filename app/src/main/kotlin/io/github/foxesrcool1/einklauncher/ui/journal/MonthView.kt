package io.github.foxesrcool1.einklauncher.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.einkClickable
import java.time.LocalDate

/**
 * A month, as whole weeks starting on Monday.
 *
 * A day with an entry carries a rule under its number. There is no colour and
 * no fill, because a filled cell on e-ink is a large black block that ghosts.
 * The whole month fits on one screen, so there is nothing to scroll.
 */
@Composable
fun MonthView(
    month: LocalDate,
    daysWithEntries: Set<LocalDate>,
    today: LocalDate,
    selected: LocalDate,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            JournalStrings.weekdayInitials().forEach { initial ->
                CapsLabel(
                    text = initial,
                    style = EinkType.capsSmall.copy(
                        color = EinkColors.Faded,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        HairlineDivider(color = EinkColors.Faded)
        Spacer(modifier = Modifier.height(4.dp))

        JournalStrings.monthGrid(month).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        hasEntry = day != null && daysWithEntries.contains(day),
                        isToday = day == today,
                        isSelected = day == selected,
                        onSelect = { if (day != null) onSelectDay(day) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate?,
    hasEntry: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (day == null) {
        Box(modifier = modifier.height(EinkDimens.touchTarget))
        return
    }

    Column(
        modifier = modifier
            .height(EinkDimens.touchTarget)
            .then(
                if (isSelected) {
                    Modifier.border(EinkDimens.rule, EinkColors.Ink)
                } else {
                    Modifier
                },
            )
            .einkClickable(onClick = onSelect)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EinkText(
            text = day.dayOfMonth.toString(),
            style = EinkType.body.copy(
                color = if (isToday) EinkColors.Ink else EinkColors.Faded,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(EinkDimens.rule)
                .then(if (hasEntry) Modifier.fillMaxWidth(0.5f) else Modifier)
                .background(if (hasEntry) EinkColors.Ink else EinkColors.Paper),
        )
    }
}
