package io.github.foxesrcool1.einklauncher.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType

/**
 * Asks before something cannot be undone.
 *
 * A black line around a white panel with round corners. No shadow, no dim behind it, no fade in.
 * The panel changes as few pixels as it can, which is e-ink rule 7.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Yes",
    cancelText: String = "No",
) {
    EinkDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        ConfirmDialogContent(
            title = title,
            message = message,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            confirmText = confirmText,
            cancelText = cancelText,
            modifier = Modifier.widthIn(max = DialogMaxWidth).fillMaxWidth(0.86f),
        )
    }
}

/**
 * The panel inside the dialog window. Kept apart so a screenshot test can draw
 * it without a second window.
 */
@Composable
fun ConfirmDialogContent(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Yes",
    cancelText: String = "No",
) {
    Column(
        modifier = modifier
            .einkPanel()
            .padding(EinkDimens.blockGap),
    ) {
        EinkText(text = title, style = EinkType.title)
        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
        EinkText(text = message, style = EinkType.body)
        Spacer(modifier = Modifier.height(EinkDimens.blockGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            InvertPressButton(text = cancelText, onClick = onDismiss)
            Spacer(modifier = Modifier.width(EinkDimens.targetGap))
            InvertPressButton(text = confirmText, onClick = onConfirm)
        }
    }
}
