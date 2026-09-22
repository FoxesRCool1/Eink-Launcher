package io.github.foxesrcool1.margin.design.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import io.github.foxesrcool1.margin.design.EinkType
import java.util.Locale

/**
 * A small label in capitals with wide tracking.
 *
 * Used for dates, status lines, section names and control text. The caller
 * passes normal words; this turns them into capitals, so the strings stay
 * readable in code and in the translation files.
 */
@Composable
fun CapsLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = EinkType.caps,
    maxLines: Int = 1,
) {
    EinkText(
        text = text.uppercase(Locale.ROOT),
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
