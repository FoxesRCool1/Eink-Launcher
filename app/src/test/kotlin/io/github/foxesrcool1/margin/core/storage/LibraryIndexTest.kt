package io.github.foxesrcool1.margin.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LibraryIndexTest {

    @get:Rule
    val temporary = TemporaryFolder()

    private lateinit var store: LocalFileStore

    @Before
    fun setUp() {
        store = LocalFileStore(temporary.newFolder("data"))
    }

    private fun fill() {
        store.write("books/war.epub", ByteArray(10))
        store.write("books/scan.pdf", ByteArray(20))
        store.writeText("books/cover-notes.txt", "not a book")
        store.writeText("notes/plan.md", "plan")
        store.writeText("notes/ideas/sketch.inknote", "ink")
        store.writeText("notes/ideas/scratch.txt", "other")
        store.writeText("journal/2026/2026-09-19.md", "today")
        store.writeText("journal/2026/2026-09-18.inknote", "yesterday")
    }

    @Test
    fun `the index works out what every file is`() {
        fill()
        val snapshot = LibraryIndex.build(store, nowMillis = 1_000L)

        assertEquals(
            listOf("books/scan.pdf", "books/war.epub"),
            snapshot.of(IndexKind.Book).map { it.relativePath },
        )
        assertEquals(listOf("notes/plan.md"), snapshot.of(IndexKind.TypedNote).map { it.relativePath })
        assertEquals(
            listOf("notes/ideas/sketch.inknote"),
            snapshot.of(IndexKind.InkNote).map { it.relativePath },
        )
        assertEquals(
            listOf("journal/2026/2026-09-18.inknote", "journal/2026/2026-09-19.md"),
            snapshot.of(IndexKind.JournalEntry).map { it.relativePath },
        )
        assertEquals(1_000L, snapshot.builtAtMillis)
    }

    @Test
    fun `a file in books that is not a book is left out`() {
        fill()
        val snapshot = LibraryIndex.build(store, nowMillis = 1L)
        assertTrue(snapshot.entries.none { it.relativePath == "books/cover-notes.txt" })
    }

    @Test
    fun `a file in notes that is neither typed nor ink is still listed`() {
        fill()
        val snapshot = LibraryIndex.build(store, nowMillis = 1L)
        assertEquals(
            listOf("notes/ideas/scratch.txt"),
            snapshot.of(IndexKind.Other).map { it.relativePath },
        )
    }

    @Test
    fun `the title is the file name without the extension`() {
        store.writeText("notes/my plan.md", "x")
        val snapshot = LibraryIndex.build(store, nowMillis = 1L)
        assertEquals("my plan", snapshot.of(IndexKind.TypedNote).single().title)
    }

    @Test
    fun `an empty folder makes an empty index, not a failure`() {
        val snapshot = LibraryIndex.build(store, nowMillis = 7L)
        assertTrue(snapshot.entries.isEmpty())
        assertEquals(7L, snapshot.builtAtMillis)
    }

    @Test
    fun `an index survives being written and read back`() {
        fill()
        val original = LibraryIndex.build(store, nowMillis = 42L)
        val restored = LibraryIndex.deserialise(LibraryIndex.serialise(original))
        assertEquals(original, restored)
    }

    @Test
    fun `text that is not an index reads as null`() {
        assertNull(LibraryIndex.deserialise(""))
        assertNull(LibraryIndex.deserialise("hello"))
        assertNull(LibraryIndex.deserialise("eink-index-1"))
        assertNull(LibraryIndex.deserialise("eink-index-1\nnot-a-number\n"))
    }

    @Test
    fun `one damaged line is dropped and the rest still loads`() {
        fill()
        val text = LibraryIndex.serialise(LibraryIndex.build(store, nowMillis = 1L))
        val lines = text.lines().toMutableList()
        lines[3] = "this line is broken"
        val restored = LibraryIndex.deserialise(lines.joinToString("\n"))

        assertTrue(restored != null)
        // One of the seven rows was destroyed. The other six still load.
        assertEquals(6, restored!!.entries.size)
    }

    @Test
    fun `a row that points outside the data folder is dropped on load`() {
        val text = buildString {
            append("eink-index-1\n")
            append("1\n")
            append("TypedNote\u001f../escape.md\u001f1\u001f1\u001fescape\n")
            append("TypedNote\u001fnotes/good.md\u001f1\u001f2\u001fgood\n")
        }
        val restored = LibraryIndex.deserialise(text)
        assertEquals(listOf("notes/good.md"), restored!!.entries.map { it.relativePath })
    }

    @Test
    fun `sorting by title ignores case`() {
        store.writeText("notes/zebra.md", "z")
        store.writeText("notes/Apple.md", "a")
        store.writeText("notes/mango.md", "m")

        val snapshot = LibraryIndex.build(store, nowMillis = 1L)
        assertEquals(
            listOf("Apple", "mango", "zebra"),
            snapshot.byTitle(IndexKind.TypedNote).map { it.title },
        )
    }

    @Test
    fun `sorting by recent puts the newest first`() {
        // The times are set here rather than read off the disk. File systems
        // report the time at their own resolution, and a test must not depend
        // on that.
        val snapshot = IndexSnapshot(
            entries = listOf(
                IndexEntry(IndexKind.TypedNote, "notes/old.md", "old", 100L, 1L),
                IndexEntry(IndexKind.TypedNote, "notes/new.md", "new", 900L, 1L),
                IndexEntry(IndexKind.Book, "books/b.epub", "b", 999L, 1L),
            ),
            builtAtMillis = 1L,
        )
        assertEquals(
            listOf("new", "old"),
            snapshot.byRecent(IndexKind.TypedNote).map { it.title },
        )
    }
}
