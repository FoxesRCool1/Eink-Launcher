package io.github.foxesrcool1.margin.ui.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.margin.design.EinkColors
import io.github.foxesrcool1.margin.design.EinkDimens
import io.github.foxesrcool1.margin.design.EinkType
import io.github.foxesrcool1.margin.design.components.CapsLabel
import io.github.foxesrcool1.margin.design.components.ConfirmDialog
import io.github.foxesrcool1.margin.design.components.EinkText
import io.github.foxesrcool1.margin.design.components.HairlineDivider
import io.github.foxesrcool1.margin.design.components.InvertPressButton
import io.github.foxesrcool1.margin.design.components.PagedList
import io.github.foxesrcool1.margin.design.components.IconPressButton
import io.github.foxesrcool1.margin.design.components.Plants
import io.github.foxesrcool1.margin.design.icons.Lucide
import io.github.foxesrcool1.margin.ui.common.ScreenScaffold

/** The rows the demo list pages through. */
private val demoRows: List<String> = (1..26).map { index ->
    "Sample row $index"
}

/**
 * One screen that shows every part of the design system.
 *
 * It is the check for e-ink rule 1 to rule 6: if anything here moves, fades or
 * shows a ripple on the tablet, the rule is broken and the component is wrong.
 */
@Composable
fun DesignDemoScreen(modifier: Modifier = Modifier) {
    var selectedIcon by remember { mutableStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var lastAction by remember { mutableStateOf("Nothing pressed yet") }

    ScreenScaffold(
        title = "Design system",
        overline = "Margin demo",
        plant = Plants.Other,
        modifier = modifier,
    ) {
        CapsLabel(text = "Icon controls")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
            listOf(Lucide.BookOpen, Lucide.PenLine, Lucide.Notebook, Lucide.LayoutGrid).forEachIndexed { index, icon ->
                IconPressButton(
                    icon = icon,
                    label = icon.name,
                    selected = index == selectedIcon,
                    onClick = {
                        selectedIcon = index
                        lastAction = "Icon ${icon.name}"
                    },
                )
            }
            IconPressButton(icon = Lucide.Plus, label = "With a line around it", bordered = true, onClick = { })
            IconPressButton(icon = Lucide.Trash2, label = "Off", enabled = false, onClick = { })
        }

        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
        CapsLabel(text = "Word controls")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
            InvertPressButton(text = "Press me", onClick = { lastAction = "Pressed" })
            InvertPressButton(text = "Delete", icon = Lucide.Trash2, onClick = { showDialog = true })
            InvertPressButton(text = "Off", onClick = { }, enabled = false)
        }
        Spacer(modifier = Modifier.height(8.dp))
        EinkText(text = lastAction, style = EinkType.body.copy(color = EinkColors.Faded))

        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
        CapsLabel(text = "Paged list")
        HairlineDivider(color = EinkColors.Faded)
        PagedList(
            items = demoRows,
            pageSize = 5,
            rowHeight = EinkDimens.rowTwoLines,
            modifier = Modifier.weight(1f),
        ) { index, row ->
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                EinkText(text = row, style = EinkType.rowTitle, maxLines = 1)
                CapsLabel(
                    text = "Index $index",
                    style = EinkType.capsSmall.copy(color = EinkColors.Faded),
                )
            }
        }
    }

    if (showDialog) {
        ConfirmDialog(
            title = "Delete the note?",
            message = "The file goes away and cannot come back.",
            confirmText = "Delete",
            cancelText = "Keep",
            onConfirm = {
                showDialog = false
                lastAction = "Confirmed"
            },
            onDismiss = {
                showDialog = false
                lastAction = "Cancelled"
            },
        )
    }
}
