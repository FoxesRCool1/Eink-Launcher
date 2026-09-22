package io.github.foxesrcool1.margin.core.ink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EraserGeometryTest {

    private val horizontal = InkTestData.line(100f, 100f, 300f, 100f, width = 4f)

    @Test
    fun `an eraser path that crosses a stroke hits it`() {
        assertTrue(EraserGeometry.touches(horizontal, 200f, 50f, 200f, 150f, radius = 1f))
    }

    @Test
    fun `an eraser that passes close by hits within its radius plus half the stroke`() {
        // The stroke is 4 wide, so its edge is 2 from its centre line.
        assertTrue(EraserGeometry.touches(horizontal, 150f, 111f, 250f, 111f, radius = 10f))
        assertFalse(EraserGeometry.touches(horizontal, 150f, 113f, 250f, 113f, radius = 10f))
    }

    @Test
    fun `an eraser past the end of a stroke misses it`() {
        assertFalse(EraserGeometry.touches(horizontal, 330f, 90f, 330f, 110f, radius = 10f))
        assertTrue(EraserGeometry.touches(horizontal, 310f, 90f, 310f, 110f, radius = 10f))
    }

    @Test
    fun `a tap of the eraser works, not only a drag`() {
        assertTrue(EraserGeometry.touches(horizontal, 200f, 105f, 200f, 105f, radius = 10f))
        assertFalse(EraserGeometry.touches(horizontal, 200f, 130f, 200f, 130f, radius = 10f))
    }

    @Test
    fun `a dot can be erased`() {
        val dot = InkTestData.dot(500f, 500f, width = 6f)
        assertTrue(EraserGeometry.touches(dot, 490f, 500f, 510f, 500f, radius = 5f))
        assertFalse(EraserGeometry.touches(dot, 490f, 520f, 510f, 520f, radius = 5f))
    }

    @Test
    fun `only the touched strokes are returned, in page order`() {
        val far = InkTestData.line(100f, 900f, 300f, 900f)
        val near = InkTestData.line(100f, 120f, 300f, 120f)
        val hits = EraserGeometry.hits(listOf(far, horizontal, near), 200f, 90f, 200f, 130f, radius = 4f)
        assertEquals(listOf(horizontal, near), hits)
    }

    @Test
    fun `a fat highlighter stroke is hit from further away`() {
        val marker = InkTestData.line(100f, 100f, 300f, 100f, width = 44f, tool = InkTool.Highlighter)
        assertTrue(EraserGeometry.touches(marker, 150f, 125f, 250f, 125f, radius = 5f))
    }

    @Test
    fun `distance from a point to a segment`() {
        assertEquals(25f, EraserGeometry.pointToSegmentSquared(5f, 5f, 0f, 0f, 10f, 0f), 0.001f)
        assertEquals(25f, EraserGeometry.pointToSegmentSquared(15f, 0f, 0f, 0f, 10f, 0f), 0.001f)
        assertEquals(0f, EraserGeometry.pointToSegmentSquared(3f, 0f, 0f, 0f, 10f, 0f), 0.001f)
    }

    @Test
    fun `erasing across a full page is fast`() {
        val page = InkTestData.fullPage(2_000)
        val started = System.nanoTime()
        var total = 0
        repeat(100) { step ->
            total += EraserGeometry.hits(page, 100f + step * 10f, 100f, 110f + step * 10f, 1800f, radius = 18f).size
        }
        val millis = (System.nanoTime() - started) / 1_000_000
        assertTrue(total > 0)
        assertTrue("100 eraser moves took $millis ms", millis < 1_000)
    }
}
