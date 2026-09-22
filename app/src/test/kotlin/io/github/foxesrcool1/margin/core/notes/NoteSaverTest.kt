package io.github.foxesrcool1.margin.core.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class NoteSaverTest {

    @Test
    fun `text that the file already holds is not written again`() {
        val written = mutableListOf<String>()
        val saver = NoteSaver { written += it; true }
        saver.loaded("hello")

        assertTrue(saver.save { "hello" })
        assertTrue(saver.save { "hello there" })
        assertTrue(saver.save { "hello there" })

        assertEquals(listOf("hello there"), written)
    }

    @Test
    fun `a write that fails is tried again by the next save`() {
        var works = false
        val written = mutableListOf<String>()
        val saver = NoteSaver { text -> works.also { if (it) written += text } }
        saver.loaded("")

        assertFalse(saver.save { "draft" })
        assertEquals("", saver.savedText)
        works = true
        assertTrue(saver.save { "draft" })
        assertEquals(listOf("draft"), written)
    }

    @Test
    fun `when two saves cross, the newest text is what ends up in the file`() {
        val written = CopyOnWriteArrayList<String>()
        val autosaveIsWriting = CountDownLatch(1)
        val letTheAutosaveFinish = CountDownLatch(1)
        val saver = NoteSaver { text ->
            if (written.isEmpty()) {
                autosaveIsWriting.countDown()
                letTheAutosaveFinish.await(5, TimeUnit.SECONDS)
            }
            written += text
            true
        }
        saver.loaded("")

        val text = java.util.concurrent.atomic.AtomicReference("The last")
        val autosave = thread { saver.save { text.get() } }
        assertTrue(autosaveIsWriting.await(5, TimeUnit.SECONDS))

        // Two more words, then "Done", while the autosave is still on the disk.
        text.set("The last two words")
        val onTheWayOut = thread { saver.save { text.get() } }
        letTheAutosaveFinish.countDown()
        autosave.join(5000)
        onTheWayOut.join(5000)

        assertEquals(listOf("The last", "The last two words"), written.toList())
        assertEquals("The last two words", saver.savedText)
    }
}
