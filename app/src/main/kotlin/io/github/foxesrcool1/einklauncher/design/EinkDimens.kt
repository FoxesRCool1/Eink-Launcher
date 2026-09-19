package io.github.foxesrcool1.einklauncher.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sizes used across the app.
 *
 * E-ink rule 5: lines are 1 dp or 2 dp, no shadows, no gradients.
 * E-ink rule 6: a touch target is 56 dp or more, with space around it.
 */
object EinkDimens {
    /** The smallest height of anything the finger or the pen can hit. */
    val touchTarget: Dp = 56.dp

    /** Side margin of a full screen. */
    val screenMargin: Dp = 28.dp

    /** Space between two blocks on a screen. */
    val blockGap: Dp = 24.dp

    /** Space between two targets, so a finger cannot hit both. */
    val targetGap: Dp = 12.dp

    /** A fine rule. */
    val hairline: Dp = 1.dp

    /** A heavy rule, and the border of a control. */
    val rule: Dp = 2.dp
}
