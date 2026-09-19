package io.github.foxesrcool1.einklauncher.design.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaginationTest {

    @Test
    fun `an empty list still has one page`() {
        assertEquals(1, Pagination.pageCount(itemCount = 0, pageSize = 5))
        assertEquals(emptyList<String>(), Pagination.pageItems(emptyList<String>(), 0, 5))
    }

    @Test
    fun `a full page does not start a new page`() {
        assertEquals(1, Pagination.pageCount(itemCount = 5, pageSize = 5))
        assertEquals(2, Pagination.pageCount(itemCount = 6, pageSize = 5))
    }

    @Test
    fun `the last page can be part full`() {
        val items = (1..7).toList()
        assertEquals(listOf(6, 7), Pagination.pageItems(items, page = 1, pageSize = 5))
    }

    @Test
    fun `a page index above the end comes back to the last page`() {
        val items = (1..7).toList()
        assertEquals(1, Pagination.clampPage(page = 9, itemCount = items.size, pageSize = 5))
        assertEquals(listOf(6, 7), Pagination.pageItems(items, page = 9, pageSize = 5))
    }

    @Test
    fun `a page index below zero comes back to the first page`() {
        assertEquals(0, Pagination.clampPage(page = -3, itemCount = 7, pageSize = 5))
    }

    @Test
    fun `the page of an item is found`() {
        assertEquals(0, Pagination.pageOfItem(itemIndex = 0, itemCount = 26, pageSize = 5))
        assertEquals(0, Pagination.pageOfItem(itemIndex = 4, itemCount = 26, pageSize = 5))
        assertEquals(1, Pagination.pageOfItem(itemIndex = 5, itemCount = 26, pageSize = 5))
        assertEquals(5, Pagination.pageOfItem(itemIndex = 25, itemCount = 26, pageSize = 5))
    }

    @Test
    fun `previous and next know where the ends are`() {
        assertFalse(Pagination.hasPrevious(0))
        assertTrue(Pagination.hasPrevious(1))
        assertTrue(Pagination.hasNext(page = 0, itemCount = 7, pageSize = 5))
        assertFalse(Pagination.hasNext(page = 1, itemCount = 7, pageSize = 5))
        assertFalse(Pagination.hasNext(page = 0, itemCount = 0, pageSize = 5))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a page size of zero is refused`() {
        Pagination.pageCount(itemCount = 3, pageSize = 0)
    }
}
