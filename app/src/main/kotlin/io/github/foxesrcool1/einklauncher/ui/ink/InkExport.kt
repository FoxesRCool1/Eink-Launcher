package io.github.foxesrcool1.einklauncher.ui.ink

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import io.github.foxesrcool1.einklauncher.core.ink.InkNote
import io.github.foxesrcool1.einklauncher.core.ink.InkPageData
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Pictures and PDF files made from ink. All of it goes through [InkRenderer],
 * so an export looks like the screen.
 */
object InkExport {

    /** The small picture kept inside the `.inknote` file. Half size is plenty for a list and costs a quarter of the time. */
    const val PREVIEW_SCALE = 0.5f

    fun pagePng(note: InkNote, page: InkPageData, scale: Float = 1f, backdrop: Bitmap? = null): ByteArray {
        val bitmap = pageBitmap(note, page, scale, backdrop)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    fun pageBitmap(note: InkNote, page: InkPageData, scale: Float, backdrop: Bitmap? = null): Bitmap {
        val width = (note.pageWidth * scale).roundToInt().coerceAtLeast(1)
        val height = (note.pageHeight * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.scale(scale, scale)
        backdrop?.let {
            canvas.drawBitmap(it, null, android.graphics.RectF(0f, 0f, note.pageWidth, note.pageHeight), null)
        }
        val renderer = InkRenderer()
        renderer.drawTemplate(canvas, note.template, note.pageWidth, note.pageHeight)
        renderer.drawStrokes(canvas, page.strokes)
        return bitmap
    }

    /**
     * A PDF with one page per note page. The strokes go in as lines, not as a
     * picture, so the file is small and stays sharp when printed.
     *
     * A PDF page is measured in points, 72 to the inch. The page is scaled so
     * its width is A5, which is close to the size of the tablet.
     */
    fun notePdf(note: InkNote): ByteArray {
        val pdfWidth = 420
        val scale = pdfWidth / note.pageWidth
        val pdfHeight = (note.pageHeight * scale).roundToInt()
        val document = PdfDocument()
        val renderer = InkRenderer()
        try {
            note.pages.forEachIndexed { index, page ->
                val info = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, index + 1).create()
                val pdfPage = document.startPage(info)
                val canvas = pdfPage.canvas
                canvas.scale(scale, scale)
                renderer.drawTemplate(canvas, note.template, note.pageWidth, note.pageHeight)
                renderer.drawStrokes(canvas, page.strokes)
                document.finishPage(pdfPage)
            }
            val out = ByteArrayOutputStream()
            document.writeTo(out)
            return out.toByteArray()
        } finally {
            document.close()
        }
    }
}
