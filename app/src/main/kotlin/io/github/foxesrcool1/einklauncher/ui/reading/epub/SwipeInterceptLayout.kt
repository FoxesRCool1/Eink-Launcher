package io.github.foxesrcool1.einklauncher.ui.reading.epub

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Takes horizontal swipes away from the book view.
 *
 * Left alone, the book view lets a page follow the finger and then slides it
 * into place. That is an animation, and e-ink rule 1 has no exceptions. This
 * layout watches the finger instead. Once a drag is clearly sideways it takes
 * the gesture, and when the finger lifts it asks for exactly one page turn
 * with no movement in between.
 *
 * Taps and long presses pass through untouched, so tap zones and text
 * selection keep working. While text is selected [swipesEnabled] is false,
 * because dragging a selection handle is also a sideways drag.
 */
@SuppressLint("ViewConstructor")
class SwipeInterceptLayout(context: Context) : FrameLayout(context) {

    var swipesEnabled: Boolean = true

    /** -1 for the page before, +1 for the page after. */
    var onSwipe: ((direction: Int) -> Unit)? = null

    private val slop = ViewConfiguration.get(context).scaledTouchSlop * 2
    private val minDistance = 56f * resources.displayMetrics.density
    private var downX = 0f
    private var downY = 0f
    private var taken = false

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!swipesEnabled || event.pointerCount > 1) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                taken = false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - downX)
                val dy = abs(event.y - downY)
                if (dx > slop && dx > dy * 1.5f) {
                    taken = true
                    return true
                }
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!taken) return false
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val dx = event.x - downX
            if (abs(dx) >= minDistance) onSwipe?.invoke(if (dx < 0) 1 else -1)
            taken = false
        } else if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
            taken = false
        }
        return true
    }
}
