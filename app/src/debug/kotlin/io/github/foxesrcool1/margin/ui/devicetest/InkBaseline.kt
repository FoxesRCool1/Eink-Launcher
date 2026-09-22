package io.github.foxesrcool1.margin.ui.devicetest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.Stroke
import io.github.foxesrcool1.margin.core.log.AppLog

private const val TAG = "InkBaseline"

/**
 * A plain Jetpack Ink canvas, for the latency comparison in Step 2.
 *
 * Debug builds only. The release build has a stub of the same name that
 * returns null, so Jetpack Ink and its native library stay out of the APK a
 * user installs.
 */
object InkBaseline {

    fun create(context: Context): View? = runCatching { JetpackInkCanvas(context) }
        .onFailure { AppLog.e(TAG, "Could not make the Jetpack Ink canvas", it) }
        .getOrNull()

    fun clear(view: View?) {
        (view as? JetpackInkCanvas)?.clear()
    }
}

@SuppressLint("ViewConstructor")
private class JetpackInkCanvas(context: Context) : FrameLayout(context), InProgressStrokesFinishedListener {

    private val strokesView = InProgressStrokesView(context)
    private val finished = mutableSetOf<InProgressStrokeId>()
    private var current: InProgressStrokeId? = null

    private val brush: Brush = Brush.createWithColorIntArgb(
        family = StockBrushes.pressurePen(),
        colorIntArgb = Color.BLACK,
        size = 5f,
        epsilon = 0.1f,
    )

    init {
        setBackgroundColor(Color.WHITE)
        addView(strokesView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        strokesView.addFinishedStrokesListener(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val tool = event.getToolType(0)
        if (tool != MotionEvent.TOOL_TYPE_STYLUS && tool != MotionEvent.TOOL_TYPE_ERASER) return false
        val pointerId = event.getPointerId(0)

        runCatching {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    requestUnbufferedDispatch(event)
                    current = strokesView.startStroke(event, pointerId, brush)
                }

                MotionEvent.ACTION_MOVE -> current?.let { strokesView.addToStroke(event, pointerId, it) }

                MotionEvent.ACTION_UP -> {
                    current?.let { strokesView.finishStroke(event, pointerId, it) }
                    current = null
                }

                MotionEvent.ACTION_CANCEL -> {
                    current?.let { strokesView.cancelStroke(it, event) }
                    current = null
                }
            }
        }.onFailure { AppLog.e(TAG, "Jetpack Ink refused an event", it) }
        return true
    }

    // The finished strokes are left in the view on purpose. A real app would
    // take them out and draw them itself. For a latency test that is noise.
    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
        finished += strokes.keys
        AppLog.i(TAG, "Jetpack Ink finished ${strokes.size} stroke, ${finished.size} held")
    }

    fun clear() {
        strokesView.removeFinishedStrokes(finished.toSet())
        finished.clear()
    }
}
