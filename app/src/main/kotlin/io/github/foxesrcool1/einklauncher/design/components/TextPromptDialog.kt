package io.github.foxesrcool1.einklauncher.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType

/** Asks for one line of text. Used to add a habit or rename a note. */
@Composable
fun TextPromptDialog(
    title: String,
    initialValue: String = "",
    confirmText: String = "Save",
    cancelText: String = "Cancel",
    /** True when an empty answer is a fine answer. */
    allowEmpty: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .background(EinkColors.Paper)
                .border(width = EinkDimens.rule, color = EinkColors.Ink)
                .padding(EinkDimens.blockGap),
        ) {
            EinkText(text = title, style = EinkType.title, maxLines = 2)
            Spacer(modifier = Modifier.height(EinkDimens.targetGap))

            BasicTextField(
                value = value,
                onValueChange = { value = it },
                textStyle = EinkType.body,
                singleLine = true,
                cursorBrush = SolidColor(EinkColors.Ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = EinkDimens.touchTarget)
                    .border(width = EinkDimens.hairline, color = EinkColors.Ink)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            )

            Spacer(modifier = Modifier.height(EinkDimens.blockGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                InvertPressButton(text = cancelText, onClick = onDismiss)
                Spacer(modifier = Modifier.width(EinkDimens.targetGap))
                InvertPressButton(
                    text = confirmText,
                    enabled = allowEmpty || value.isNotBlank(),
                    onClick = { onConfirm(value.trim()) },
                )
            }
        }
    }
}
