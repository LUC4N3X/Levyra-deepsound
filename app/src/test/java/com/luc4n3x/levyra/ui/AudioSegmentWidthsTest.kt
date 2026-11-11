package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioSegmentWidthsTest {
    @Test
    fun spareSpaceIsSharedSoTheStripFillsTheRow() {
        val widths = audioSegmentWidths(listOf(100, 200, 50), availableWidth = 400, gap = 5)
        assertEquals(400 - 10, widths.sum())
        assertEquals(listOf(114, 213, 63), widths)
    }

    @Test
    fun labelsWiderThanTheRowKeepTheirNaturalWidth() {
        assertEquals(listOf(300, 260, 120), audioSegmentWidths(listOf(300, 260, 120), availableWidth = 400, gap = 5))
    }

    @Test
    fun emptyStripHasNoSegments() {
        assertEquals(emptyList<Int>(), audioSegmentWidths(emptyList(), availableWidth = 400, gap = 5))
    }
}
