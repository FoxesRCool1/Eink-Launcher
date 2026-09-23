package io.github.foxesrcool1.margin.design.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.margin.design.EinkColors
import io.github.foxesrcool1.margin.design.EinkType

/**
 * A text field with a cursor that does not blink.
 *
 * The platform cursor blinks twice a second. On e-ink each blink is a repaint
 * of a small box, for ever, for as long as the field has the focus. That is an
 * animation, and it also wears a grey mark into the panel. So the real cursor
 * is made invisible, and a plain black line is drawn where it stands. The line
 * only moves when the cursor does.
 *
 * Every text field in the app goes through this. Do not call `BasicTextField`
 * directly in a user screen.
 */
@Composable
fun EinkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = EinkType.body,
    singleLine: Boolean = false,
    enabled: Boolean = true,
    /**
     * What the Enter key does in a field of one line: the key on a Bluetooth
     * keyboard, and the Done key on the screen keyboard. Null leaves Enter
     * doing nothing, as before.
     */
    onSubmit: (() -> Unit)? = null,
) {
    val state = rememberTextFieldState(initialText = value)
    val latestValue by rememberUpdatedState(value)
    val latestOnChange by rememberUpdatedState(onValueChange)
    val latestOnSubmit by rememberUpdatedState(onSubmit)
    val reported = remember { ReportedValues() }

    // From outside in: a note that finished loading, or a field that was reset.
    LaunchedEffect(value) {
        // A value this field reported itself is only the caller catching up.
        // The field may be a letter or two ahead of it by now, and pushing the
        // older text back in would eat those letters.
        val echo = reported.isEcho(value)
        if (echo || state.text.toString() == value) return@LaunchedEffect
        reported.clear()
        state.setTextAndPlaceCursorAtEnd(value)
    }
    // From inside out: what the user typed.
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { typed ->
            if (typed != latestValue) {
                reported.record(typed)
                latestOnChange(typed)
            }
        }
    }

    val scroll: ScrollState = rememberScrollState()
    var focused by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf<(() -> TextLayoutResult?)?>(null) }
    val cursorWidth = 2.dp

    BasicTextField(
        state = state,
        enabled = enabled,
        textStyle = textStyle,
        lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.Default,
        keyboardOptions = if (singleLine && onSubmit != null) {
            KeyboardOptions(imeAction = ImeAction.Done)
        } else {
            KeyboardOptions.Default
        },
        onKeyboardAction = if (onSubmit != null) KeyboardActionHandler { latestOnSubmit?.invoke() } else null,
        cursorBrush = SolidColor(Color.Transparent),
        scrollState = scroll,
        onTextLayout = { getResult -> layout = getResult },
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .drawWithContent {
                drawContent()
                val selection = state.selection
                val result = layout?.invoke()
                if (focused && enabled && selection.collapsed && result != null) {
                    val offset = selection.start.coerceIn(0, result.layoutInput.text.length)
                    val box = result.getCursorRect(offset)
                    // The field scrolls its text when it is longer than the
                    // box. The line has to move with the text.
                    val dx = if (singleLine) scroll.value.toFloat() else 0f
                    val dy = if (singleLine) 0f else scroll.value.toFloat()
                    val x = box.left - dx
                    val top = box.top - dy
                    val bottom = box.bottom - dy
                    if (bottom > 0f && top < size.height && x >= -1f && x <= size.width + 1f) {
                        drawLine(
                            color = EinkColors.Ink,
                            start = Offset(x, top),
                            end = Offset(x, bottom),
                            strokeWidth = cursorWidth.toPx(),
                        )
                    }
                }
            },
    )
}

/**
 * The texts a field has reported and not yet seen come back.
 *
 * The field tells the caller what was typed, and the caller hands the same
 * text back as the new value a moment later. If the user types in that
 * moment, the text that comes back is already old. This is how the field
 * tells that echo apart from a value the caller really set from outside.
 */
internal class ReportedValues(private val limit: Int = 32) {

    private val waiting = ArrayDeque<String>()

    fun record(text: String) {
        waiting.addLast(text)
        while (waiting.size > limit) waiting.removeFirst()
    }

    /**
     * True when [value] is one this field reported. It is forgotten then, and
     * so is every older one: the caller may skip values, it never goes back.
     */
    fun isEcho(value: String): Boolean {
        val index = waiting.indexOf(value)
        if (index < 0) return false
        repeat(index + 1) { waiting.removeFirst() }
        return true
    }

    fun clear() = waiting.clear()
}
