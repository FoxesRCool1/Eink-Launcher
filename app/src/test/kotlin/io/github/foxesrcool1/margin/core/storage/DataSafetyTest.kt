package io.github.foxesrcool1.margin.core.storage

import io.github.foxesrcool1.margin.core.books.BooksRepository
import io.github.foxesrcool1.margin.core.habits.HabitsRepository
import io.github.foxesrcool1.margin.core.routine.RoutineRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/** Four ways the data folder could lose something. Each one did, before its fix. */
class DataSafetyTest {

    @get:Rule
    val temporary = TemporaryFolder()

    private lateinit var store: LocalFileStore
    private lateinit var data: DataRepository

    private val today = LocalDate.of(2026, 9, 20)

    @Before
    fun setUp() {
        store = LocalFileStore(temporary.newFolder("Margin"))
        data = DataRepository(store)
        data.ensureFolders()
    }

    // -- Two quick taps ------------------------------------------------------------

    @Test
    fun `two habits ticked at the same moment are both ticked`() {
        // Two copies of the class, as the Journal and Today each make their own.
        val first = HabitsRepository(data)
        val second = HabitsRepository(data)

        repeat(40) { round ->
            store.delete(StorageLayout.habitLogPath())
            val go = CountDownLatch(1)
            val a = thread { go.await(); first.toggle("read", today) }
            val b = thread { go.await(); second.toggle("walk", today) }
            go.countDown()
            a.join()
            b.join()
            assertEquals("round $round lost a tick", setOf("read", "walk"), first.loadMarks().map { it.habitId }.toSet())
        }
    }

    @Test
    fun `two routine items ticked at the same moment are both ticked`() {
        val first = RoutineRepository(data)
        val second = RoutineRepository(data)
        val read = first.add("Read").items.last().id
        val walk = first.add("Walk").items.last().id

        repeat(40) { round ->
            store.delete("${StorageLayout.HABITS}/routine-log.csv")
            val go = CountDownLatch(1)
            val a = thread { go.await(); first.toggle(read, today) }
            val b = thread { go.await(); second.toggle(walk, today) }
            go.countDown()
            a.join()
            b.join()
            assertTrue("round $round lost a tick", first.statuses(today).all { it.doneToday })
        }
    }

    // -- A file with a typo in it -----------------------------------------------------

    @Test
    fun `a habits file that cannot be read is kept before a new one goes over it`() {
        val broken = """{ "habits": [ { "id": "read", "name": "Read" """
        store.writeText(StorageLayout.habitsPath(), broken)
        val habits = HabitsRepository(data)

        assertTrue(habits.load().habits.isEmpty())
        habits.load()
        habits.add("Walk", today)

        assertEquals(broken, store.readText("${StorageLayout.HABITS}/habits.unreadable.json"))
        assertFalse("one broken file, one copy", store.exists("${StorageLayout.HABITS}/habits.unreadable 2.json"))
        assertEquals(listOf("Walk"), habits.load().habits.map { it.name })
    }

    @Test
    fun `a routine file that cannot be read is kept too`() {
        store.writeText("${StorageLayout.HABITS}/routine.json", "not json at all")
        RoutineRepository(data).add("Read")
        assertEquals("not json at all", store.readText("${StorageLayout.HABITS}/routine.unreadable.json"))
    }

    // -- A book that will not go --------------------------------------------------------

    private class StubbornStore(private val real: FileStore, private val willNotDelete: String) : FileStore by real {
        override fun delete(relativePath: String): Boolean =
            if (relativePath == willNotDelete) false else real.delete(relativePath)
    }

    @Test
    fun `a book that cannot be deleted keeps its notes`() {
        data.importBook(ByteArray(10).inputStream(), "stuck.epub")
        val stubborn = DataRepository(StubbornStore(store, StorageLayout.bookPath("stuck.epub")))
        val books = BooksRepository(stubborn)
        val book = books.list().single()
        store.writeText(StorageLayout.annotationsPath(book.bookId), """{"highlights":[]}""")
        store.writeText("${StorageLayout.ANNOTATIONS}/${book.bookId}/page-0001.strokes", "ink")

        assertFalse(books.delete(book))

        assertTrue(store.exists(StorageLayout.annotationsPath(book.bookId)))
        assertTrue(store.exists("${StorageLayout.ANNOTATIONS}/${book.bookId}/page-0001.strokes"))
    }

    @Test
    fun `a book that is deleted takes its notes with it`() {
        data.importBook(ByteArray(10).inputStream(), "gone.epub")
        val books = BooksRepository(data)
        val book = books.list().single()
        store.writeText(StorageLayout.annotationsPath(book.bookId), """{"highlights":[]}""")

        assertTrue(books.delete(book))

        assertFalse(store.exists(StorageLayout.bookPath("gone.epub")))
        assertFalse(store.exists(StorageLayout.annotationsPath(book.bookId)))
    }

    // -- Two books, one name ------------------------------------------------------------

    @Test
    fun `a different book with the same name is imported beside the first`() {
        data.importBook(ByteArray(10).inputStream(), "scan.pdf")
        val second = data.importBook(ByteArray(25) { 7 }.inputStream(), "scan.pdf")

        val imported = second as ImportResult.Imported
        assertEquals(StorageLayout.bookPath("scan 2.pdf"), imported.relativePath)
        assertEquals(10L, store.sizeOf(StorageLayout.bookPath("scan.pdf")))
        assertEquals(25L, store.sizeOf(imported.relativePath))
    }

    @Test
    fun `the same book again is still "already there", and leaves no copy behind`() {
        data.importBook(ByteArray(10).inputStream(), "scan.pdf")

        assertTrue(data.importBook(ByteArray(10).inputStream(), "scan.pdf") is ImportResult.AlreadyThere)
        assertTrue(data.importBook(ByteArray(10).inputStream(), "scan.pdf", sizeHintBytes = 10) is ImportResult.AlreadyThere)

        assertEquals(listOf("scan.pdf"), data.listBooks().map { it.name })
    }
}
