package io.github.foxesrcool1.margin.design

import androidx.compose.ui.graphics.Color

/**
 * Three colours and no more.
 *
 * E-ink rule 3: pure black on pure white. A cream or beige background turns
 * into dirty grey dither on the panel. The panel is already paper coloured.
 * E-ink rule 4: grey is for large inactive text only.
 */
object EinkColors {
    val Ink: Color = Color(0xFF000000)
    val Paper: Color = Color(0xFFFFFFFF)
    val Faded: Color = Color(0xFF808080)
}
