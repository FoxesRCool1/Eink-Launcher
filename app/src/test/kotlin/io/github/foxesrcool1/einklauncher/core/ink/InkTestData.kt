package io.github.foxesrcool1.einklauncher.core.ink

import kotlin.math.cos
import kotlin.math.sin

/** Strokes for the tests. The same input always gives the same strokes. */
object InkTestData {

    fun line(x0: Float, y0: Float, x1: Float, y1: Float, width: Float = 5f, tool: InkTool = InkTool.Pen): InkStroke =
        InkStroke(tool, width, floatArrayOf(x0, x1), floatArrayOf(y0, y1), floatArrayOf(0.5f, 0.5f))

    fun dot(x: Float, y: Float, width: Float = 5f): InkStroke =
        InkStroke(InkTool.Pen, width, floatArrayOf(x), floatArrayOf(y), floatArrayOf(1f))

    /** A loop, like a handwritten letter, with pressure that rises and falls. */
    fun squiggle(centreX: Float, centreY: Float, points: Int = 50, size: Float = 30f): InkStroke {
        val xs = FloatArray(points)
        val ys = FloatArray(points)
        val pressures = FloatArray(points)
        for (index in 0 until points) {
            val t = index / points.toFloat() * 6.283f
            xs[index] = centreX + cos(t) * size + index * 0.8f
            ys[index] = centreY + sin(t * 2f) * size * 0.6f
            pressures[index] = 0.3f + 0.6f * sin(t / 2f)
        }
        return InkStroke(InkTool.Pen, 5f, xs, ys, pressures)
    }

    /** A full page of writing: [count] strokes of 50 points, laid out in rows. */
    fun fullPage(count: Int = 2_000): List<InkStroke> = List(count) { index ->
        val column = index % 40
        val row = index / 40
        squiggle(60f + column * 33f, 80f + row * 36f, size = 12f)
    }
}
