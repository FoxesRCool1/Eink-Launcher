package io.github.foxesrcool1.margin.ui.split

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup

/**
 * [SplitLayout] for a screen built from views: the EPUB reader, whose book
 * engine is a fragment.
 *
 * The three children never change places in the view tree, so the book view
 * is never taken off the screen and put back, which a web view does not like.
 * Only where each one is laid out changes.
 */
@SuppressLint("ViewConstructor")
class SplitViewLayout(
    context: Context,
    private val main: View,
    private val pane: View,
) : ViewGroup(context) {

    private val line = View(context).apply { setBackgroundColor(Color.BLACK) }

    var open = false
        set(value) {
            field = value
            val shown = if (value) View.VISIBLE else View.GONE
            line.visibility = shown
            pane.visibility = shown
            requestLayout()
        }

    var swapped = false
        set(value) {
            field = value
            requestLayout()
        }

    init {
        setBackgroundColor(Color.WHITE)
        addView(main)
        addView(line)
        addView(pane)
        open = false
    }

    /** The same rule as [halvesSideBySide], so a view screen and a Compose screen split alike. */
    private val sideBySide: Boolean
        get() = resources.configuration.let { it.screenWidthDp > it.screenHeightDp }

    private val lineWidth: Int
        get() = (resources.displayMetrics.density * 2f).toInt().coerceAtLeast(1)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
        if (!open) {
            main.measure(exactly(width), exactly(height))
            return
        }
        val (mainLength, paneLength) = lengths(if (sideBySide) width else height)
        if (sideBySide) {
            main.measure(exactly(mainLength), exactly(height))
            line.measure(exactly(lineWidth), exactly(height))
            pane.measure(exactly(paneLength), exactly(height))
        } else {
            main.measure(exactly(width), exactly(mainLength))
            line.measure(exactly(width), exactly(lineWidth))
            pane.measure(exactly(width), exactly(paneLength))
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top
        if (!open) {
            main.layout(0, 0, width, height)
            return
        }
        val (mainLength, paneLength) = lengths(if (sideBySide) width else height)
        val mainAt = if (swapped) paneLength + lineWidth else 0
        val lineAt = if (swapped) paneLength else mainLength
        val paneAt = if (swapped) 0 else mainLength + lineWidth
        fun place(view: View, at: Int, length: Int) {
            if (sideBySide) view.layout(at, 0, at + length, height) else view.layout(0, at, width, at + length)
        }
        place(main, mainAt, mainLength)
        place(line, lineAt, lineWidth)
        place(pane, paneAt, paneLength)
    }

    /** The main half first, then the second. The first one gets the odd pixel, as in [SplitLayout]. */
    private fun lengths(along: Int): Pair<Int, Int> {
        val first = (along - lineWidth) / 2
        val second = along - lineWidth - first
        return if (swapped) second to first else first to second
    }

    private fun exactly(size: Int) = MeasureSpec.makeMeasureSpec(size.coerceAtLeast(0), MeasureSpec.EXACTLY)
}
