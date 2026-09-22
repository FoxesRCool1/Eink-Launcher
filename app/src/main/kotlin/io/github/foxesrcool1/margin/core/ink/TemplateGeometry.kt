package io.github.foxesrcool1.margin.core.ink

/**
 * Where the lines and the dots of a page template go, in page units.
 *
 * Kept apart from the drawing so a test can check that the screen, the PNG
 * export and the PDF export all print the same page.
 */
object TemplateGeometry {

    /** About 8 mm on the 292 PPI panel when one unit is one pixel. */
    const val LINE_SPACING = 92f

    const val DOT_SPACING = 69f

    const val TOP_MARGIN = 160f

    const val SIDE_MARGIN = 80f

    /** The y of every ruled line. */
    fun lineYs(pageHeight: Float): List<Float> {
        val ys = ArrayList<Float>()
        var y = TOP_MARGIN
        while (y < pageHeight - LINE_SPACING / 2f) {
            ys += y
            y += LINE_SPACING
        }
        return ys
    }

    /** The x of every column of dots, and the y of every row. */
    fun dotColumns(pageWidth: Float): List<Float> = steps(DOT_SPACING, pageWidth - DOT_SPACING / 2f)

    fun dotRows(pageHeight: Float): List<Float> = steps(DOT_SPACING, pageHeight - DOT_SPACING / 2f)

    private fun steps(start: Float, end: Float): List<Float> {
        val values = ArrayList<Float>()
        var value = start
        while (value < end) {
            values += value
            value += DOT_SPACING
        }
        return values
    }
}
