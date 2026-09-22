package io.github.foxesrcool1.margin.core.pdf

import io.github.foxesrcool1.margin.core.ink.InkTestData
import io.github.foxesrcool1.margin.core.json.JsonObject
import io.github.foxesrcool1.margin.core.json.jsonOf
import io.github.foxesrcool1.margin.core.storage.DataRepository
import io.github.foxesrcool1.margin.core.storage.LocalFileStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PdfViewportTest {

    @get:Rule
    val temporary = TemporaryFolder()

    // US Letter in points, and the canvas of the tablet under one toolbar row.
    private val letter = PageBox(0f, 0f, 612f, 792f)
    private val viewWidth = 1440
    private val viewHeight = 1700

    @Test
    fun `fit page is one screen and it is the whole page`() {
        assertEquals(listOf(letter), PdfViewport.screens(letter, viewWidth, viewHeight, PdfZoom.FitPage))
    }

    @Test
    fun `fit width cuts a tall page into screens that cover all of it`() {
        val tall = PageBox(0f, 0f, 400f, 1000f)
        val screens = PdfViewport.screens(tall, viewWidth, viewHeight, PdfZoom.FitWidth)
        assertTrue(screens.size >= 2)
        assertEquals(0f, screens.first().top, 0.01f)
        assertEquals(1000f, screens.last().bottom, 0.01f)
        screens.forEach { assertEquals(400f, it.width, 0.01f) }
        // No gap between one screen and the next.
        screens.zipWithNext().forEach { (a, b) -> assertTrue(b.top <= a.bottom) }
    }

    @Test
    fun `at 200 percent the screens go left to right and then down`() {
        val screens = PdfViewport.screens(letter, viewWidth, viewHeight, PdfZoom.Percent200)
        assertTrue(screens.size >= 4)
        assertEquals(0f, screens[0].left, 0.01f)
        assertTrue(screens[1].left > screens[0].left)
        assertEquals(screens[0].top, screens[1].top, 0.01f)
        assertEquals(612f, screens.maxOf { it.right }, 0.01f)
        assertEquals(792f, screens.maxOf { it.bottom }, 0.01f)
        // Each screen shows half the page width: that is what 200 percent means.
        screens.forEach { assertEquals(306f, it.width, 0.5f) }
    }

    @Test
    fun `neighbouring screens share a strip so no line is lost at the cut`() {
        val screens = PdfViewport.screens(letter, viewWidth, viewHeight, PdfZoom.Percent150)
        val first = screens[0]
        val second = screens[1]
        assertTrue("screens must overlap", second.left < first.right)
    }

    @Test
    fun `a screen never reaches outside the content box`() {
        val cropped = PageBox(70f, 90f, 540f, 700f)
        PdfZoom.entries.forEach { zoom ->
            PdfViewport.screens(cropped, viewWidth, viewHeight, zoom).forEach { box ->
                assertTrue(box.left >= cropped.left - 0.01f && box.right <= cropped.right + 0.01f)
                assertTrue(box.top >= cropped.top - 0.01f && box.bottom <= cropped.bottom + 0.01f)
                assertFalse(box.isEmpty)
            }
        }
    }

    @Test
    fun `a change of zoom lands on the screen nearest to where the reader was`() {
        val screens = PdfViewport.screens(letter, viewWidth, viewHeight, PdfZoom.Percent200)
        val bottomRight = PageBox(400f, 600f, 612f, 792f)
        assertEquals(screens.size - 1, PdfViewport.screenNearest(screens, bottomRight))
        assertEquals(0, PdfViewport.screenNearest(screens, PageBox(0f, 0f, 100f, 100f)))
    }

    @Test
    fun `nonsense sizes give one screen and do not throw`() {
        assertEquals(1, PdfViewport.screens(letter, 0, 0, PdfZoom.FitWidth).size)
        assertEquals(1, PdfViewport.screens(PageBox(0f, 0f, 0f, 0f), 100, 100, PdfZoom.FitWidth).size)
    }

    // -- Crop ---------------------------------------------------------------------------

    private fun page(width: Int, height: Int, ink: (x: Int, y: Int) -> Boolean): IntArray =
        IntArray(width * height) { index ->
            if (ink(index % width, index / width)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }

    @Test
    fun `the margins of a page are found`() {
        // Text block from 20 to 80 percent across and 10 to 90 percent down.
        val pixels = page(100, 100) { x, y -> x in 20..79 && y in 10..89 }
        val box = CropDetector.contentBox(pixels, 100, 100, 600f, 800f)
        assertEquals(120f - 9f, box.left, 1f)
        assertEquals(480f + 9f, box.right, 1f)
        assertEquals(80f - 9f, box.top, 1f)
        assertEquals(720f + 9f, box.bottom, 1f)
    }

    @Test
    fun `a blank page and a full page are left alone`() {
        val whole = PageBox(0f, 0f, 600f, 800f)
        assertEquals(whole, CropDetector.contentBox(page(50, 50) { _, _ -> false }, 50, 50, 600f, 800f))
        assertEquals(whole, CropDetector.contentBox(page(50, 50) { _, _ -> true }, 50, 50, 600f, 800f))
    }

    @Test
    fun `the grey of a scan is paper and a stray array is not a crash`() {
        val grey = IntArray(2500) { 0xFFF2F2F2.toInt() }
        assertEquals(PageBox(0f, 0f, 600f, 800f), CropDetector.contentBox(grey, 50, 50, 600f, 800f))
        assertEquals(PageBox(0f, 0f, 600f, 800f), CropDetector.contentBox(IntArray(3), 50, 50, 600f, 800f))
    }

    // -- Sidecar files ----------------------------------------------------------------------

    @Test
    fun `ink is kept per page beside the book and listed`() {
        val store = LocalFileStore(temporary.newFolder("Margin"))
        val data = DataRepository(store).also { it.ensureFolders() }
        val ink = PdfInkRepository(data, "manual-1234")

        assertTrue((ink.load(3) as PageInkLoad.Loaded).strokes.isEmpty())

        ink.save(3, listOf(InkTestData.line(10f, 10f, 200f, 10f)))
        ink.save(12, listOf(InkTestData.dot(5f, 5f), InkTestData.dot(9f, 9f)))
        assertTrue(store.exists("annotations/manual-1234/page-0003.strokes"))
        assertEquals(listOf(AnnotatedPage(3, 1), AnnotatedPage(12, 2)), ink.annotatedPages())
        assertTrue(ink.markdown("Manual").contains("- Page 12: 2 pen strokes"))

        // Rubbing everything out takes the file away again.
        ink.save(3, emptyList())
        assertFalse(store.exists("annotations/manual-1234/page-0003.strokes"))
        assertEquals(listOf(12), ink.annotatedPages().map { it.pageNumber })
    }

    @Test
    fun `a damaged ink file is reported and not treated as empty`() {
        val store = LocalFileStore(temporary.newFolder("Margin2"))
        val data = DataRepository(store).also { it.ensureFolders() }
        store.write("annotations/manual-1/page-0001.strokes", "rubbish".toByteArray())
        assertTrue(PdfInkRepository(data, "manual-1").load(1) is PageInkLoad.Damaged)
    }

    @Test
    fun `the position survives a round trip and bad numbers are pulled in`() {
        val position = PdfPosition(pageIndex = 41, zoom = PdfZoom.Percent150, cropMargins = true)
        assertEquals(position, PdfPosition.fromJson(position.toJson(), pageCount = 100))
        assertEquals(9, PdfPosition.fromJson(position.toJson(), pageCount = 10).pageIndex)
        assertEquals(PdfPosition(), PdfPosition.fromJson(null, 10))
        val odd = JsonObject.of("page" to jsonOf(-5), "zoom" to jsonOf("huge"))
        assertEquals(PdfPosition(0, PdfZoom.FitPage, false), PdfPosition.fromJson(odd, 10))
    }
}
