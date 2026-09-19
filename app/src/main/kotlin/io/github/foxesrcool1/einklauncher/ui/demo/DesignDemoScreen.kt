package io.github.foxesrcool1.einklauncher.ui.demo

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
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.BotanicalCorner
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.ConfirmDialog
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.design.components.PagedList
import io.github.foxesrcool1.einklauncher.design.components.WordMenu
import io.github.foxesrcool1.einklauncher.design.components.WordMenuItem
import io.github.foxesrcool1.einklauncher.ui.common.ScreenScaffold

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
    var selectedWord by remember { mutableStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var lastAction by remember { mutableStateOf("Nothing pressed yet") }

    ScreenScaffold(
        title = "Design system",
        overline = "Eink Launcher demo",
        corner = BotanicalCorner.BottomEnd,
        modifier = modifier,
    ) {
        CapsLabel(text = "Word menu")
        Spacer(modifier = Modifier.height(8.dp))
        WordMenu(
            items = listOf(
                WordMenuItem("Read"),
                WordMenuItem("Write"),
                WordMenuItem("Journal"),
                WordMenuItem("Apps", enabled = false),
            ),
            selectedIndex = selectedWord,
            onSelect = { index ->
                selectedWord = index
                lastAction = "Word $index"
            },
        )

        Spacer(modifier = Modifier.height(EinkDimens.blockGap))
        HairlineDivider()
        Spacer(modifier = Modifier.height(EinkDimens.blockGap))

        CapsLabel(text = "Controls")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
            InvertPressButton(text = "Press me", onClick = { lastAction = "Pressed" })
            InvertPressButton(text = "Delete", onClick = { showDialog = true })
            InvertPressButton(text = "Off", onClick = { }, enabled = false)
        }
        Spacer(modifier = Modifier.height(8.dp))
        EinkText(text = lastAction, style = EinkType.body.copy(color = EinkColors.Faded))

        Spacer(modifier = Modifier.height(EinkDimens.blockGap))
        HairlineDivider()
        Spacer(modifier = Modifier.height(EinkDimens.blockGap))

        CapsLabel(text = "Paged list")
        Spacer(modifier = Modifier.height(8.dp))
        PagedList(
            items = demoRows,
            pageSize = 5,
            modifier = Modifier.weight(1f),
        ) { index, row ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
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
