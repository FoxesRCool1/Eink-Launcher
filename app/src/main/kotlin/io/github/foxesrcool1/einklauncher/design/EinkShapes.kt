package io.github.foxesrcool1.einklauncher.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The shapes of the app. There is no square corner in it.
 *
 * The owner asked for that: a hard right angle is not calm, and the corner
 * drawings are all curves. A round corner costs nothing on e-ink. It is still
 * one flat colour with one line around it, with no shadow and no gradient, so
 * e-ink rule 5 holds. See docs/decisions/0013-icons-and-round-shapes.md.
 */
object EinkShapes {

    /** A control. Fully round ends, like a pebble. */
    val control: Shape = RoundedCornerShape(percent = 50)

    /** A panel: a dialog, a card, a box of text. */
    val panel: Shape = RoundedCornerShape(24.dp)

    /** A text field, and a row in a list while it is pressed. */
    val field: Shape = RoundedCornerShape(16.dp)
}
