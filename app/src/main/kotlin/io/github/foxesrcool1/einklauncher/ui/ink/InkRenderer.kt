package io.github.foxesrcool1.einklauncher.ui.ink

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import io.github.foxesrcool1.einklauncher.core.ink.InkStroke
import io.github.foxesrcool1.einklauncher.core.ink.InkStrokeBuilder
import io.github.foxesrcool1.einklauncher.core.ink.InkTool
import io.github.foxesrcool1.einklauncher.core.ink.PageTemplate
import io.github.foxesrcool1.einklauncher.core.ink.TemplateGeometry
import kotlin.math.abs

/**
 * Paints strokes and page templates.
 *
 * Everything is drawn in page units. The caller scales the canvas first, so
 * the screen, the preview picture, the PNG export and the PDF export all come
 * from this one piece of code and cannot drift apart.
 *
 * The pen is black and its width follows the pressure. The highlighter is a
 * flat grey, painted in DARKEN mode: grey over white gives grey, grey over
 * grey gives the same grey, and grey over black leaves the black. So it never
 * hides pen strokes or the text of a PDF, the order of painting does not
 * matter, and a line that crosses itself does not get darker.
 */
class InkRenderer {

    private val penPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.BLACK
    }

    private val dotPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private val highlighterPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        strokeJoin = Paint.Join.ROUND
        color = HIGHLIGHTER_GREY
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)
    }

    private val templatePaint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = TEMPLATE_GREY
    }

    private val templateDotPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = TEMPLATE_DOT_GREY
    }

    private val path = Path()

    fun drawTemplate(canvas: Canvas, template: PageTemplate, pageWidth: Float, pageHeight: Float) {
        when (template) {
            PageTemplate.Blank -> Unit

            PageTemplate.Lined -> TemplateGeometry.lineYs(pageHeight).forEach { y ->
                canvas.drawLine(
                    TemplateGeometry.SIDE_MARGIN, y,
                    pageWidth - TemplateGeometry.SIDE_MARGIN, y,
                    templatePaint,
                )
            }

            PageTemplate.DotGrid -> {
                val rows = TemplateGeometry.dotRows(pageHeight)
                TemplateGeometry.dotColumns(pageWidth).forEach { x ->
                    rows.forEach { y -> canvas.drawCircle(x, y, 2.2f, templateDotPaint) }
                }
            }
        }
    }

    fun drawStrokes(canvas: Canvas, strokes: List<InkStroke>) {
        strokes.forEach { drawStroke(canvas, it) }
    }

    fun drawStroke(canvas: Canvas, stroke: InkStroke) {
        drawRange(
            canvas, stroke.tool, stroke.width, stroke.pointCount, 0, stroke.pointCount - 1,
            { stroke.xs[it] }, { stroke.ys[it] }, { stroke.pressures[it] },
        )
    }

    /** Paints the part of a stroke in progress from point [from] to point [to]. */
    fun drawLive(canvas: Canvas, builder: InkStrokeBuilder, from: Int, to: Int) {
        drawRange(
            canvas, builder.tool, builder.width, builder.size, from, to,
            builder::x, builder::y, builder::pressure,
        )
    }

    private inline fun drawRange(
        canvas: Canvas,
        tool: InkTool,
        width: Float,
        count: Int,
        from: Int,
        to: Int,
        x: (Int) -> Float,
        y: (Int) -> Float,
        pressure: (Int) -> Float,
    ) {
        if (count == 0 || to < from) return

        if (count == 1) {
            // A tap of the pen: a dot.
            if (tool == InkTool.Pen) {
                canvas.drawCircle(x(0), y(0), penWidth(width, pressure(0)) / 2f, dotPaint)
            } else {
                highlighterPaint.strokeWidth = width
                canvas.drawPoint(x(0), y(0), highlighterPaint)
            }
            return
        }

        if (tool == InkTool.Highlighter) {
            path.rewind()
            path.moveTo(x(from), y(from))
            for (index in from + 1..to) path.lineTo(x(index), y(index))
            highlighterPaint.strokeWidth = width
            canvas.drawPath(path, highlighterPaint)
            return
        }

        // The width changes along a pen stroke. One draw call per segment is
        // slow on a page of 100 000 points, so neighbours of nearly the same
        // width go into one path.
        var runWidth = -1f
        path.rewind()
        for (index in from until to) {
            val segmentWidth = quantise((penWidth(width, pressure(index)) + penWidth(width, pressure(index + 1))) / 2f)
            if (abs(segmentWidth - runWidth) > 0.001f) {
                if (runWidth > 0f) {
                    penPaint.strokeWidth = runWidth
                    canvas.drawPath(path, penPaint)
                }
                path.rewind()
                path.moveTo(x(index), y(index))
                runWidth = segmentWidth
            }
            path.lineTo(x(index + 1), y(index + 1))
        }
        if (runWidth > 0f) {
            penPaint.strokeWidth = runWidth
            canvas.drawPath(path, penPaint)
        }
    }

    companion object {
        const val HIGHLIGHTER_GREY: Int = 0xFFC4C4C4.toInt()
        private const val TEMPLATE_GREY: Int = 0xFF9A9A9A.toInt()
        private const val TEMPLATE_DOT_GREY: Int = 0xFF6E6E6E.toInt()

        /** A light touch still leaves a line that an e-ink panel can show. */
        fun penWidth(fullWidth: Float, pressure: Float): Float =
            fullWidth * (0.4f + 0.6f * pressure.coerceIn(0f, 1f))

        private fun quantise(width: Float): Float = (width * 2f).toInt().coerceAtLeast(1) / 2f
    }
}
