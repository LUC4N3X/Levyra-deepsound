package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HomeSectionLazyKeyTest {
    @Test
    fun positionalFallbackKeepsDuplicateSectionsUnique() {
        val first = homeSectionLazyKey(
            position = 0,
            title = "Daily mix",
            trackIds = listOf("a", "b", "c", "d")
        )
        val second = homeSectionLazyKey(
            position = 1,
            title = "Daily mix",
            trackIds = listOf("a", "b", "c", "e")
        )

        assertNotEquals(first, second)
    }

    @Test
    fun contentRefreshDoesNotChangeSectionKey() {
        val first = homeSectionLazyKey(
            position = 2,
            title = "For You",
            trackIds = listOf("a", "b", "c", "d")
        )
        val second = homeSectionLazyKey(
            position = 2,
            title = "For You",
            trackIds = listOf("x", "y", "z", "w")
        )

        assertEquals(first, second)
    }
}
