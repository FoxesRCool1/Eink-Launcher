package io.github.foxesrcool1.einklauncher.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.icons.Lucide
import io.github.foxesrcool1.einklauncher.design.icons.LucideIcon

/** One line in an [OptionsDialog]. */
data class DialogOption(
    val label: String,
    val enabled: Boolean = true,
    val icon: LucideIcon? = null,
    val onSelect: () -> Unit,
)

/** The widest a dialog gets. In landscape 86 % of the screen is far too wide to read. */
internal val DialogMaxWidth = 440.dp

/** A short list of actions, shown after a long press. */
@Composable
fun OptionsDialog(
    title: String,
    options: List<DialogOption>,
    onDismiss: () -> Unit,
) {
    EinkDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        OptionsDialogContent(
            title = title,
            options = options,
            onDismiss = onDismiss,
            modifier = Modifier.widthIn(max = DialogMaxWidth).fillMaxWidth(0.86f),
        )
    }
}

@Composable
fun OptionsDialogContent(
    title: String,
    options: List<DialogOption>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .einkPanel()
            .padding(EinkDimens.blockGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EinkText(text = title, style = EinkType.title, maxLines = 2, modifier = Modifier.weight(1f))
            IconPressButton(icon = Lucide.X, label = "Close", onClick = onDismiss)
        }
        Spacer(modifier = Modifier.height(EinkDimens.targetGap))

        options.forEach { option ->
            EinkRow(onClick = { if (option.enabled) option.onSelect() }) { pressed ->
                val colour = when {
                    !option.enabled -> EinkColors.Faded
                    pressed -> EinkColors.Paper
                    else -> EinkColors.Ink
                }
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (option.icon != null) EinkIcon(icon = option.icon, color = colour)
                    EinkText(text = option.label, style = EinkType.rowTitle.copy(color = colour), maxLines = 1)
                }
            }
        }
    }
}
