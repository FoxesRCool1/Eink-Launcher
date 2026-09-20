package io.github.foxesrcool1.einklauncher.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType

/** One line in an [OptionsDialog]. */
data class DialogOption(
    val label: String,
    val enabled: Boolean = true,
    val onSelect: () -> Unit,
)

/** A short list of actions, shown after a long press. */
@Composable
fun OptionsDialog(
    title: String,
    options: List<DialogOption>,
    onDismiss: () -> Unit,
    cancelText: String = "Close",
) {
    EinkDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        OptionsDialogContent(
            title = title,
            options = options,
            onDismiss = onDismiss,
            cancelText = cancelText,
            modifier = Modifier.fillMaxWidth(0.86f),
        )
    }
}

@Composable
fun OptionsDialogContent(
    title: String,
    options: List<DialogOption>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = "Close",
) {
    Column(
        modifier = modifier
            .background(EinkColors.Paper)
            .border(width = EinkDimens.rule, color = EinkColors.Ink)
            .padding(EinkDimens.blockGap),
    ) {
        EinkText(text = title, style = EinkType.title, maxLines = 2)
        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
        HairlineDivider()

        options.forEach { option ->
            EinkRow(onClick = { if (option.enabled) option.onSelect() }) { pressed ->
                CapsLabel(
                    text = option.label,
                    style = EinkType.caps.copy(
                        color = when {
                            !option.enabled -> EinkColors.Faded
                            pressed -> EinkColors.Paper
                            else -> EinkColors.Ink
                        },
                    ),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            HairlineDivider(color = EinkColors.Faded)
        }

        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            InvertPressButton(text = cancelText, onClick = onDismiss)
        }
    }
}
