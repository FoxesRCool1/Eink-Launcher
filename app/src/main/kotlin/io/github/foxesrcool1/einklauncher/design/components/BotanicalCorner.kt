package io.github.foxesrcool1.einklauncher.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.LocalBotanicalArtEnabled

/** Where the drawing sits on the screen. */
enum class BotanicalCorner {
    TopStart,
    TopEnd,
    BottomStart,
    BottomEnd,
}

/**
 * One fine line drawing for the corner of a main screen.
 *
 * Original work for this project. The shape is built from cubic curves in
 * code, so it is 1 bit clean at any size and needs no dither. Nothing here is
 * traced from another drawing. The user can turn it off, and
 * [LocalBotanicalArtEnabled] carries that setting.
 */
@Composable
fun BotanicalSprig(
    modifier: Modifier = Modifier,
    corner: BotanicalCorner = BotanicalCorner.BottomEnd,
    drawingSize: Dp = 120.dp,
    color: Color = EinkColors.Ink,
    strokeWidth: Dp = 1.dp,
) {
    if (!LocalBotanicalArtEnabled.current) return

    Canvas(modifier = modifier.size(drawingSize)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)

        // The drawing is written for the bottom end corner and mirrored for
        // the other three, so there is only one shape to keep in order.
        val flipX = corner == BotanicalCorner.TopStart || corner == BotanicalCorner.BottomStart
        val flipY = corner == BotanicalCorner.TopStart || corner == BotanicalCorner.TopEnd

        fun x(value: Float): Float = if (flipX) w - value * w else value * w
        fun y(value: Float): Float = if (flipY) h - value * h else value * h

        // The stem runs from the outer corner towards the middle of the page.
        val stem = Path().apply {
            moveTo(x(1.00f), y(1.00f))
            cubicTo(x(0.82f), y(0.86f), x(0.60f), y(0.70f), x(0.30f), y(0.30f))
        }
        drawPath(path = stem, color = color, style = stroke)

        // Six leaves along the stem. Each leaf is two curves that meet again.
        val leaves = listOf(
            Triple(0.78f, 0.80f, true),
            Triple(0.72f, 0.72f, false),
            Triple(0.62f, 0.64f, true),
            Triple(0.55f, 0.55f, false),
            Triple(0.45f, 0.46f, true),
            Triple(0.38f, 0.38f, false),
        )

        leaves.forEach { (baseX, baseY, outward) ->
            val tipX = if (outward) baseX + 0.22f else baseX - 0.20f
            val tipY = if (outward) baseY - 0.06f else baseY + 0.20f
            val bulge = if (outward) 0.10f else -0.10f

            val leaf = Path().apply {
                moveTo(x(baseX), y(baseY))
                cubicTo(
                    x(baseX + bulge), y(baseY - 0.10f),
                    x(tipX - bulge * 0.4f), y(tipY - 0.08f),
                    x(tipX), y(tipY),
                )
                cubicTo(
                    x(tipX - bulge), y(tipY + 0.08f),
                    x(baseX + bulge * 0.4f), y(baseY + 0.08f),
                    x(baseX), y(baseY),
                )
            }
            drawPath(path = leaf, color = color, style = stroke)
        }

        // A small seed head at the far end of the stem.
        drawCircle(
            color = color,
            radius = strokeWidth.toPx() * 2.5f,
            center = androidx.compose.ui.geometry.Offset(x(0.30f), y(0.30f)),
            style = stroke,
        )
    }
}
