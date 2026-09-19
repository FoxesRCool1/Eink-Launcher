package io.github.foxesrcool1.einklauncher.design.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import io.github.foxesrcool1.einklauncher.design.LocalEinkTextStyle

/**
 * Text.
 *
 * It wraps BasicText, not Material Text, because this app does not depend on
 * Material. Material brings ripples, elevation and animated indication, and
 * none of that belongs on e-ink.
 */
@Composable
fun EinkText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalEinkTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow,
    )
}
