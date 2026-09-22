package io.github.foxesrcool1.margin.core.pdf

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** A box in page units. For a PDF those are points, 72 to the inch, with the origin at the top left. */
data class PageBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val isEmpty: Boolean get() = width <= 0f || height <= 0f
}

/** The fixed zoom steps of plan section 6. There is no free zoom: a pinch needs an animation. */
enum class PdfZoom(val id: String, val label: String) {
    FitPage("page", "Fit page"),
    FitWidth("width", "Fit width"),
    Percent150("150", "150 %"),
    Percent200("200", "200 %"),
    ;

    fun next(): PdfZoom = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromId(id: String?): PdfZoom = entries.firstOrNull { it.id == id } ?: FitPage
    }
}

/**
 * Which parts of a page are shown, one screen at a time.
 *
 * E-ink rule 2 says paginate, do not scroll. So a page that is larger than the
 * screen at the chosen zoom is cut into screens, left to right and then top to
 * bottom, and Next walks through them before it goes to the next page. Two
 * neighbouring screens share a strip, so a line of text that is cut at the
 * edge of one screen is whole on the next.
 *
 * Plain arithmetic, no Android, so every case is a unit test.
 */
object PdfViewport {

    /** How much of the screen two neighbouring screens share. */
    const val OVERLAP = 0.08f

    /**
     * The screens of one page. [content] is the part of the page to show: the
     * whole page, or the box around the ink when margins are cropped.
     */
    fun screens(content: PageBox, viewWidth: Int, viewHeight: Int, zoom: PdfZoom): List<PageBox> {
        if (content.isEmpty || viewWidth <= 0 || viewHeight <= 0) return listOf(content)

        val fitWidthScale = viewWidth / content.width
        val fitPageScale = min(fitWidthScale, viewHeight / content.height)
        val scale = when (zoom) {
            PdfZoom.FitPage -> fitPageScale
            PdfZoom.FitWidth -> fitWidthScale
            PdfZoom.Percent150 -> fitWidthScale * 1.5f
            PdfZoom.Percent200 -> fitWidthScale * 2f
        }

        // How much of the page one screen can hold, in page units.
        val windowWidth = min(content.width, viewWidth / scale)
        val windowHeight = min(content.height, viewHeight / scale)

        val columns = steps(content.width, windowWidth)
        val rows = steps(content.height, windowHeight)

        val boxes = ArrayList<PageBox>(columns * rows)
        for (row in 0 until rows) {
            val top = content.top + offset(row, rows, content.height, windowHeight)
            for (column in 0 until columns) {
                val left = content.left + offset(column, columns, content.width, windowWidth)
                boxes += PageBox(left, top, left + windowWidth, top + windowHeight)
            }
        }
        return boxes
    }

    private fun steps(total: Float, window: Float): Int {
        if (window >= total - 0.5f) return 1
        val stride = window * (1f - OVERLAP)
        return max(2, ceil((total - window) / stride).toInt() + 1)
    }

    /** Screens are spread evenly, so the first starts at the edge and the last ends at the other edge. */
    private fun offset(index: Int, count: Int, total: Float, window: Float): Float =
        if (count <= 1) 0f else (total - window) * index / (count - 1)

    /** The screen that shows the most of [target], so a change of zoom stays where the reader was looking. */
    fun screenNearest(screens: List<PageBox>, target: PageBox): Int {
        val centreX = (target.left + target.right) / 2f
        val centreY = (target.top + target.bottom) / 2f
        var best = 0
        var bestDistance = Float.MAX_VALUE
        screens.forEachIndexed { index, box ->
            val dx = (box.left + box.right) / 2f - centreX
            val dy = (box.top + box.bottom) / 2f - centreY
            val distance = dx * dx + dy * dy
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
            }
        }
        return best
    }
}

/**
 * Finds the box around everything that is not white paper.
 *
 * A scanned book often has wide white margins, and on an 8.2 inch screen they
 * cost a third of the letter size. The page is rendered very small, this looks
 * for the first and last row and column with something dark in it, and the
 * answer is scaled back up to page units.
 */
object CropDetector {

    /** A pixel counts as paper when all three colours are at least this bright. */
    private const val PAPER = 235

    /** Extra room around the content, as a part of the page width. */
    private const val PADDING = 0.015f

    /** A page where the content box covers almost everything is not worth cropping. */
    private const val WORTH_IT = 0.97f

    fun contentBox(pixels: IntArray, width: Int, height: Int, pageWidth: Float, pageHeight: Float): PageBox {
        val whole = PageBox(0f, 0f, pageWidth, pageHeight)
        if (width <= 0 || height <= 0 || pixels.size < width * height) return whole

        var left = width
        var right = -1
        var top = height
        var bottom = -1
        for (y in 0 until height) {
            val rowStart = y * width
            for (x in 0 until width) {
                if (isInk(pixels[rowStart + x])) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        // A blank page: nothing to crop to.
        if (right < left || bottom < top) return whole

        val pad = PADDING * pageWidth
        val box = PageBox(
            left = max(0f, left * pageWidth / width - pad),
            top = max(0f, top * pageHeight / height - pad),
            right = min(pageWidth, (right + 1) * pageWidth / width + pad),
            bottom = min(pageHeight, (bottom + 1) * pageHeight / height + pad),
        )
        val kept = (box.width * box.height) / (pageWidth * pageHeight)
        return if (kept >= WORTH_IT) whole else box
    }

    private fun isInk(argb: Int): Boolean {
        val alpha = argb ushr 24
        if (alpha < 32) return false
        val red = (argb shr 16) and 0xFF
        val green = (argb shr 8) and 0xFF
        val blue = argb and 0xFF
        return red < PAPER || green < PAPER || blue < PAPER
    }
}
