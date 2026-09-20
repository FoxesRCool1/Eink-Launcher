package io.github.foxesrcool1.einklauncher.ui.devicetest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val TAG = "SpikeCanvas"

/**
 * The plainest pen canvas that can answer the Step 2 questions.
 *
 * It has two ways to work:
 *
 * - [deviceDrawsLive] false: this view draws every segment the moment it
 *   arrives. That is the baseline, and it is all a generic device ever gets.
 * - [deviceDrawsLive] true: the tablet paints the stroke by itself and this
 *   view stays still while the pen is down. After the pen lifts it waits
 *   [redrawDelayMillis] and only then paints the real stroke, because the
 *   public notes say the fast overlay clears about 800 ms after pen-up.
 *
 * The real ink engine is Step 5. This one keeps no model, only pixels.
 */
@SuppressLint("ViewConstructor")
class SpikeCanvasView(context: Context) : View(context) {

    var deviceDrawsLive: Boolean = false
    var redrawDelayMillis: Long = 900L
    var strokeWidthPx: Float = 4f
    var erasing: Boolean = false

    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null

    private val paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.BLACK
    }

    /** Segments the device drew and this view still owes: x0, y0, x1, y1, width, colour. */
    private val owed = ArrayList<FloatArray>()
    private val paintOwed = Runnable { paintOwedSegments() }

    private var lastX = 0f
    private var lastY = 0f
    private var events = 0
    private var points = 0
    private var handlingNanos = 0L
    private var longestGapMillis = 0L
    private var lastEventAt = 0L
    private var downAt = 0L

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        val fresh = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        fresh.eraseColor(Color.WHITE)
        bitmap?.let { Canvas(fresh).drawBitmap(it, 0f, 0f, null) }
        bitmap = fresh
        bitmapCanvas = Canvas(fresh)
    }

    override fun onDraw(canvas: Canvas) {
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    fun clear() {
        removeCallbacks(paintOwed)
        owed.clear()
        bitmap?.eraseColor(Color.WHITE)
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val tool = event.getToolType(0)
        val isPen = tool == MotionEvent.TOOL_TYPE_STYLUS || tool == MotionEvent.TOOL_TYPE_ERASER
        // A finger never draws. Returning false lets a finger still reach
        // whatever lies under the canvas.
        if (!isPen) return false

        val started = System.nanoTime()
        val eraser = erasing || tool == MotionEvent.TOOL_TYPE_ERASER

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                removeCallbacks(paintOwed)
                lastX = event.x
                lastY = event.y
                events = 0
                points = 0
                handlingNanos = 0L
                longestGapMillis = 0L
                downAt = SystemClock.uptimeMillis()
                lastEventAt = downAt
            }

            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                val now = SystemClock.uptimeMillis()
                longestGapMillis = max(longestGapMillis, now - lastEventAt)
                lastEventAt = now
                for (index in 0 until event.historySize) {
                    segmentTo(
                        event.getHistoricalX(index),
                        event.getHistoricalY(index),
                        event.getHistoricalPressure(index),
                        eraser,
                    )
                }
                segmentTo(event.x, event.y, event.pressure, eraser)
            }
        }

        events++
        handlingNanos += System.nanoTime() - started

        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            if (deviceDrawsLive) postDelayed(paintOwed, redrawDelayMillis)
            val length = SystemClock.uptimeMillis() - downAt
            AppLog.i(
                TAG,
                "stroke: $events events, $points points, ${length} ms long, " +
                    "longest gap $longestGapMillis ms, " +
                    "handling ${handlingNanos / 1000 / max(1, events)} us per event, " +
                    "deviceDrawsLive=$deviceDrawsLive eraser=$eraser",
            )
        }
        return true
    }

    private fun segmentTo(x: Float, y: Float, pressure: Float, eraser: Boolean) {
        points++
        val width = if (eraser) strokeWidthPx * 6f else strokeWidthPx * (0.5f + pressure.coerceIn(0f, 1f))
        val colour = if (eraser) Color.WHITE else Color.BLACK
        if (deviceDrawsLive) {
            owed += floatArrayOf(lastX, lastY, x, y, width, colour.toFloat())
        } else {
            drawSegment(lastX, lastY, x, y, width, colour)
            // Rule 7: change as few pixels as possible. Only the box around
            // this one segment is handed to the panel.
            val pad = width + 2f
            invalidate(
                floor(min(lastX, x) - pad).toInt(),
                floor(min(lastY, y) - pad).toInt(),
                ceil(max(lastX, x) + pad).toInt(),
                ceil(max(lastY, y) + pad).toInt(),
            )
        }
        lastX = x
        lastY = y
    }

    private fun drawSegment(x0: Float, y0: Float, x1: Float, y1: Float, width: Float, colour: Int) {
        paint.strokeWidth = width
        paint.color = colour
        bitmapCanvas?.drawLine(x0, y0, x1, y1, paint)
    }

    private fun paintOwedSegments() {
        if (owed.isEmpty()) return
        owed.forEach { drawSegment(it[0], it[1], it[2], it[3], it[4], it[5].toInt()) }
        AppLog.i(TAG, "painted ${owed.size} owed segments ${redrawDelayMillis} ms after pen-up")
        owed.clear()
        invalidate()
    }
}
