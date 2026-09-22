package io.github.foxesrcool1.margin.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The note that opens beside a book in the split screen. */
class ReadingNotePathTest {

    @Test
    fun `the note of a book is a plain note in the notes folder, named after the book`() {
        assertEquals("notes/Reading notes/Walden.md", StorageLayout.readingNotePath("Walden", handwritten = false))
        assertEquals("notes/Reading notes/Walden.inknote", StorageLayout.readingNotePath("Walden", handwritten = true))
    }

    @Test
    fun `a title that is not a safe file name still gives a path inside the data folder`() {
        val path = StorageLayout.readingNotePath("../../etc/passwd: a \"novel\"?", handwritten = false)
        assertTrue(path.startsWith("notes/Reading notes/"))
        assertEquals(path, RelativePaths.normalise(path))
        assertEquals(3, path.split('/').size)
    }
}
