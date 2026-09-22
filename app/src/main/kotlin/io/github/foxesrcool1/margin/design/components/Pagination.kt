package io.github.foxesrcool1.margin.design.components

/**
 * Page maths for [PagedList].
 *
 * Kept free of Android and Compose so the unit tests can check every edge:
 * an empty list, a part filled last page, a page index that went out of range
 * after the list shrank.
 */
object Pagination {

    /** How many pages a list of [itemCount] items needs. Never less than one. */
    fun pageCount(itemCount: Int, pageSize: Int): Int {
        require(pageSize > 0) { "pageSize must be more than zero" }
        if (itemCount <= 0) return 1
        return (itemCount + pageSize - 1) / pageSize
    }

    /** Brings [page] back inside the range. */
    fun clampPage(page: Int, itemCount: Int, pageSize: Int): Int =
        page.coerceIn(0, pageCount(itemCount, pageSize) - 1)

    /** The index of the first item on [page]. */
    fun firstIndexOnPage(page: Int, itemCount: Int, pageSize: Int): Int =
        clampPage(page, itemCount, pageSize) * pageSize

    /** The items that belong on [page]. */
    fun <T> pageItems(items: List<T>, page: Int, pageSize: Int): List<T> {
        val start = firstIndexOnPage(page, items.size, pageSize)
        if (start >= items.size) return emptyList()
        val end = minOf(start + pageSize, items.size)
        return items.subList(start, end)
    }

    /** The page that holds the item at [itemIndex]. */
    fun pageOfItem(itemIndex: Int, itemCount: Int, pageSize: Int): Int {
        require(pageSize > 0) { "pageSize must be more than zero" }
        if (itemIndex <= 0) return 0
        return clampPage(itemIndex / pageSize, itemCount, pageSize)
    }

    fun hasPrevious(page: Int): Boolean = page > 0

    fun hasNext(page: Int, itemCount: Int, pageSize: Int): Boolean =
        page < pageCount(itemCount, pageSize) - 1
}
