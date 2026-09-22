package io.github.foxesrcool1.margin.ui.writing

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import io.github.foxesrcool1.margin.R
import java.io.ByteArrayOutputStream

/**
 * Turns a typed note into a PDF.
 *
 * The text is set in Literata, the same face the editor uses, on A5 pages.
 * The Markdown is printed as it is written. A note is mostly plain sentences,
 * and a half finished Markdown renderer would print worse than none.
 */
object TypedNoteExport {

    // A5 in PDF points, 72 to the inch.
    private const val PAGE_WIDTH = 420
    private const val PAGE_HEIGHT = 595
    private const val MARGIN = 44

    fun pdf(context: Context, text: String): ByteArray {
        val face = runCatching { ResourcesCompat.getFont(context, R.font.literata) }.getOrNull() ?: Typeface.SERIF
        val paint = TextPaint().apply {
            isAntiAlias = true
            color = Color.BLACK
            typeface = face
            textSize = 11f
        }
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, PAGE_WIDTH - 2 * MARGIN)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.35f)
            .setIncludePad(false)
            .build()

        val document = PdfDocument()
        try {
            pageBreaks(layout, PAGE_HEIGHT - 2 * MARGIN).forEachIndexed { index, (firstLine, lastLine) ->
                val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(info)
                val top = layout.getLineTop(firstLine)
                val bottom = layout.getLineBottom(lastLine)
                page.canvas.save()
                page.canvas.translate(MARGIN.toFloat(), MARGIN.toFloat() - top)
                page.canvas.clipRect(0, top, layout.width, bottom)
                layout.draw(page.canvas)
                page.canvas.restore()
                document.finishPage(page)
            }
            val out = ByteArrayOutputStream()
            document.writeTo(out)
            return out.toByteArray()
        } finally {
            document.close()
        }
    }

    /** First and last line of every page. A line is never cut in half. */
    private fun pageBreaks(layout: StaticLayout, usableHeight: Int): List<Pair<Int, Int>> {
        if (layout.lineCount == 0) return listOf(0 to 0)
        val pages = ArrayList<Pair<Int, Int>>()
        var first = 0
        for (line in 0 until layout.lineCount) {
            if (layout.getLineBottom(line) - layout.getLineTop(first) > usableHeight && line > first) {
                pages += first to line - 1
                first = line
            }
        }
        pages += first to layout.lineCount - 1
        return pages
    }
}
