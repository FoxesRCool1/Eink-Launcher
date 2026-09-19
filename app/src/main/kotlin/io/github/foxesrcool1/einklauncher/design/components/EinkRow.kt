package io.github.foxesrcool1.einklauncher.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens

/**
 * A row in a list. It inverts while pressed and it can hold a long press.
 *
 * It uses `detectTapGestures` rather than `clickable` or `combinedClickable`,
 * so there is no ripple and no indication node at all, and the long press
 * comes free. The caller draws the content and gets the pressed state, so a
 * row can invert its own text.
 */
@Composable
fun EinkRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    minHeight: Dp = EinkDimens.touchTarget,
    content: @Composable ColumnScope.(pressed: Boolean) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }

    // detectTapGestures wants ((Offset) -> Unit)?, so the parameter type has
    // to be written out. Without it the lambda has nothing to infer from.
    val longPress: ((Offset) -> Unit)? = onLongClick?.let { action ->
        { _: Offset -> action() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .background(if (pressed) EinkColors.Ink else EinkColors.Paper)
            .pointerInput(onClick, longPress) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = longPress,
                )
            }
            .padding(vertical = 10.dp),
    ) {
        content(pressed)
    }
}
