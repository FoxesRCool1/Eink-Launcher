package io.github.foxesrcool1.margin.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.margin.design.EinkColors
import io.github.foxesrcool1.margin.design.EinkType
import io.github.foxesrcool1.margin.design.components.CapsLabel
import io.github.foxesrcool1.margin.design.components.EinkIcon
import io.github.foxesrcool1.margin.design.components.EinkRow
import io.github.foxesrcool1.margin.design.components.EinkText
import io.github.foxesrcool1.margin.design.components.HairlineDivider
import io.github.foxesrcool1.margin.design.components.PagedList
import io.github.foxesrcool1.margin.design.icons.Lucide
import io.github.foxesrcool1.margin.design.icons.LucideIcon

/**
 * One row of Settings: what it is called, what it does, and what a tap does.
 *
 * [help] is the help text. It is one or two short lines in plain words, and
 * every row has one. [value] is what the setting is set to now, for a setting
 * with more than two states. [trailing] is the icon at the end of the row: an
 * arrow for a row that leads somewhere, a switch for on and off, nothing for a
 * row that just does its job when pressed.
 */
class Setting(
    val title: String,
    val help: String,
    val icon: LucideIcon? = null,
    val enabled: Boolean = true,
    val value: String? = null,
    val trailing: LucideIcon? = Lucide.ChevronRight,
    val onClick: () -> Unit,
) {
    companion object {
        /** A row that is on or off. The switch at its end shows which. */
        fun switch(
            title: String,
            help: String,
            on: Boolean,
            onChange: (Boolean) -> Unit,
            icon: LucideIcon? = null,
            enabled: Boolean = true,
        ) = Setting(
            title = title,
            help = help,
            icon = icon,
            enabled = enabled,
            value = if (on) "On" else "Off",
            trailing = if (on) Lucide.ToggleRight else Lucide.ToggleLeft,
            onClick = { onChange(!on) },
        )
    }
}

/**
 * A title, the lines of help, the padding of the row and the rule under it,
 * and a few dp to spare. Text that is even one pixel too tall for its row
 * loses its last line, so the row is never cut to the exact sum.
 */
private fun settingRowHeight(helpLines: Int) = (54 + 22 * helpLines).dp

/**
 * One page of settings. It works out by itself how many rows fit.
 *
 * [helpLines] is 2 for a page of settings, and 1 for the menu, where six short
 * rows on one page are worth more than six long ones on two.
 */
@Composable
fun SettingsList(rows: List<Setting>, modifier: Modifier = Modifier, helpLines: Int = 2) {
    PagedList(
        items = rows,
        pageSize = 4,
        rowHeight = settingRowHeight(helpLines),
        modifier = modifier,
    ) { _, row ->
        SettingRow(row, helpLines)
    }
}

@Composable
private fun SettingRow(row: Setting, helpLines: Int) {
    EinkRow(onClick = { if (row.enabled) row.onClick() }) { pressed ->
        val strong = when {
            pressed && row.enabled -> EinkColors.Paper
            row.enabled -> EinkColors.Ink
            else -> EinkColors.Faded
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (row.icon != null) EinkIcon(icon = row.icon, color = strong)
            Column(modifier = Modifier.weight(1f)) {
                EinkText(text = row.title, style = EinkType.rowTitle.copy(color = strong), maxLines = 1)
                EinkText(text = row.help, style = EinkType.help.copy(color = strong), maxLines = helpLines)
            }
            if (row.value != null) CapsLabel(text = row.value, style = EinkType.capsSmall.copy(color = strong))
            if (row.trailing != null) {
                val isSwitch = row.trailing === Lucide.ToggleRight || row.trailing === Lucide.ToggleLeft
                EinkIcon(icon = row.trailing, size = if (isSwitch) 34.dp else 22.dp, color = strong)
            }
        }
    }
    HairlineDivider(color = EinkColors.Faded)
}
