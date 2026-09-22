package io.github.foxesrcool1.margin.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import io.github.foxesrcool1.margin.design.EinkColors
import io.github.foxesrcool1.margin.design.EinkDimens
import io.github.foxesrcool1.margin.design.EinkShapes

/**
 * A white panel with a round corner and a black line around it: a dialog, or
 * a card. There is no dim behind a dialog in this app, so the line is the
 * heavy one. It is all that parts the panel from the page under it.
 */
fun Modifier.einkPanel(): Modifier = this
    .clip(EinkShapes.panel)
    .background(EinkColors.Paper)
    .border(width = EinkDimens.rule, color = EinkColors.Ink, shape = EinkShapes.panel)

/** The fine round line around a box the user types in. */
fun Modifier.einkFieldBorder(color: Color = EinkColors.Ink): Modifier = this
    .border(width = EinkDimens.hairline, color = color, shape = EinkShapes.field)
