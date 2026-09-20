package io.github.foxesrcool1.einklauncher.core.ink

import kotlin.math.max
import kotlin.math.min

/**
 * The hit test of the stroke eraser.
 *
 * The eraser is a disc that moves from one point to the next. A stroke is hit
 * when any of its segments comes closer to that path than the radius of the
 * disc plus half the width of the stroke. A hit takes the whole stroke away:
 * on e-ink, rubbing out part of a line means repainting it many times, and a
 * whole stroke is one repaint.
 *
 * Plain arithmetic, no Android, so the tests can check every case.
 */
object EraserGeometry {

    /** The strokes that the eraser touches while it moves from (x0, y0) to (x1, y1). */
    fun hits(
        strokes: List<InkStroke>,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        radius: Float,
    ): List<InkStroke> {
        val left = min(x0, x1)
        val right = max(x0, x1)
        val top = min(y0, y1)
        val bottom = max(y0, y1)
        return strokes.filter { stroke ->
            // The box test throws out nearly every stroke on the page before
            // any real arithmetic happens.
            stroke.boundsTouch(left, top, right, bottom, margin = radius) &&
                touches(stroke, x0, y0, x1, y1, radius)
        }
    }

    fun touches(stroke: InkStroke, x0: Float, y0: Float, x1: Float, y1: Float, radius: Float): Boolean {
        val reach = radius + stroke.width / 2f
        val reachSquared = reach * reach
        if (stroke.pointCount == 1) {
            return pointToSegmentSquared(stroke.xs[0], stroke.ys[0], x0, y0, x1, y1) <= reachSquared
        }
        for (index in 0 until stroke.pointCount - 1) {
            val distance = segmentToSegmentSquared(
                stroke.xs[index], stroke.ys[index], stroke.xs[index + 1], stroke.ys[index + 1],
                x0, y0, x1, y1,
            )
            if (distance <= reachSquared) return true
        }
        return false
    }

    fun pointToSegmentSquared(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        val t = if (lengthSquared == 0f) 0f else (((px - ax) * dx + (py - ay) * dy) / lengthSquared).coerceIn(0f, 1f)
        val cx = ax + t * dx
        val cy = ay + t * dy
        return (px - cx) * (px - cx) + (py - cy) * (py - cy)
    }

    /** Zero when the two segments cross, else the smallest end to segment distance. */
    fun segmentToSegmentSquared(
        ax: Float, ay: Float, bx: Float, by: Float,
        cx: Float, cy: Float, dx: Float, dy: Float,
    ): Float {
        if (segmentsCross(ax, ay, bx, by, cx, cy, dx, dy)) return 0f
        return min(
            min(pointToSegmentSquared(ax, ay, cx, cy, dx, dy), pointToSegmentSquared(bx, by, cx, cy, dx, dy)),
            min(pointToSegmentSquared(cx, cy, ax, ay, bx, by), pointToSegmentSquared(dx, dy, ax, ay, bx, by)),
        )
    }

    private fun segmentsCross(
        ax: Float, ay: Float, bx: Float, by: Float,
        cx: Float, cy: Float, dx: Float, dy: Float,
    ): Boolean {
        val d1 = cross(cx, cy, dx, dy, ax, ay)
        val d2 = cross(cx, cy, dx, dy, bx, by)
        val d3 = cross(ax, ay, bx, by, cx, cy)
        val d4 = cross(ax, ay, bx, by, dx, dy)
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
    }

    private fun cross(ax: Float, ay: Float, bx: Float, by: Float, px: Float, py: Float): Float =
        (bx - ax) * (py - ay) - (by - ay) * (px - ax)
}
