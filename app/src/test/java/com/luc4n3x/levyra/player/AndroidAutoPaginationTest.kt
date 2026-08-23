package com.luc4n3x.levyra.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAutoPaginationTest {
    @Test
    fun firstPageStartsAtTheBeginning() {
        val window = AndroidAutoPageWindow.of(page = 0, pageSize = 20)
        assertEquals(0, window.offset)
        assertEquals(20, window.limit)
        assertEquals((0 until 20).toList(), items(120).androidAutoWindow(window))
    }

    @Test
    fun nextPageContinuesWhereThePreviousOneEnded() {
        val window = AndroidAutoPageWindow.of(page = 1, pageSize = 20)
        assertEquals(20, window.offset)
        assertEquals((20 until 40).toList(), items(120).androidAutoWindow(window))
    }

    @Test
    fun lastPageIsPartialAndFurtherPagesAreEmpty() {
        val items = items(45)
        assertEquals((40 until 45).toList(), items.androidAutoWindow(AndroidAutoPageWindow.of(page = 2, pageSize = 20)))
        assertTrue(items.androidAutoWindow(AndroidAutoPageWindow.of(page = 3, pageSize = 20)).isEmpty())
    }

    @Test
    fun emptyListStaysEmptyOnEveryPage() {
        assertTrue(items(0).androidAutoWindow(AndroidAutoPageWindow.of(page = 0, pageSize = 20)).isEmpty())
        assertTrue(items(0).androidAutoWindow(AndroidAutoPageWindow.of(page = 7, pageSize = 20)).isEmpty())
    }

    @Test
    fun smallPageSizeReturnsExactlyOneItemPerPage() {
        val items = items(5)
        assertEquals(listOf(3), items.androidAutoWindow(AndroidAutoPageWindow.of(page = 3, pageSize = 1)))
    }

    @Test
    fun largeCatalogIsPagedWithoutLossOrDuplicates() {
        val items = items(1_500)
        val pageSize = 40
        val collected = ArrayList<Int>(items.size)
        var page = 0
        while (true) {
            val window = items.androidAutoWindow(AndroidAutoPageWindow.of(page, pageSize))
            if (window.isEmpty()) break
            collected += window
            page++
        }
        assertEquals(items, collected)
        assertEquals(items.size, collected.toSet().size)
    }

    @Test
    fun missingPageSizeKeepsTheWholeDirectory() {
        val items = items(300)
        assertEquals(items, items.androidAutoWindow(AndroidAutoPageWindow.of(page = 0, pageSize = 0)))
    }

    @Test
    fun hugePageIndexDoesNotOverflowIntoAValidWindow() {
        val window = AndroidAutoPageWindow.of(page = Int.MAX_VALUE, pageSize = 50)
        assertEquals(0, window.limit)
        assertTrue(items(100).androidAutoWindow(window).isEmpty())
    }

    @Test
    fun limitedWindowKeepsTheDerivedDirectoryCap() {
        val items = items(300)
        assertEquals((0 until 80).toList(), items.androidAutoWindow(AndroidAutoPageWindow.limited(80)))
    }

    @Test
    fun windowsAreClampedToABoundedNumberOfItems() {
        val clamped = AndroidAutoPageWindow.of(page = 0, pageSize = 0, maxItems = 100)
        assertEquals(0, clamped.offset)
        assertEquals(100, clamped.limit)
        assertEquals(100, items(4_000).androidAutoWindow(clamped).size)
    }

    @Test
    fun pageSizeAboveTheCapStillPagesWithoutLossOrDuplicates() {
        val items = items(450)
        val first = items.androidAutoWindow(AndroidAutoPageWindow.of(page = 0, pageSize = 200, maxItems = 100))
        val second = items.androidAutoWindow(AndroidAutoPageWindow.of(page = 1, pageSize = 200, maxItems = 100))
        assertEquals((0 until 100).toList(), first)
        assertEquals((100 until 200).toList(), second)
        assertTrue(first.intersect(second.toSet()).isEmpty())
    }

    @Test
    fun cappedPagingWalksTheWholeCatalogExactlyOnce() {
        val items = items(450)
        val collected = ArrayList<Int>(items.size)
        var page = 0
        while (true) {
            val window = items.androidAutoWindow(AndroidAutoPageWindow.of(page, pageSize = 1_000, maxItems = 100))
            if (window.isEmpty()) break
            collected += window
            page++
        }
        assertEquals(items, collected)
    }

    private fun items(count: Int): List<Int> = (0 until count).toList()
}
