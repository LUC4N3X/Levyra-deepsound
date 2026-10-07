package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeChartLayoutTest {
    @Test
    fun `top twenty remainder fills two complete columns`() {
        assertEquals(5, chartRowsPerColumn(trackCount = 10))
    }

    @Test
    fun `top fifty remainder keeps compact four row columns`() {
        assertEquals(4, chartRowsPerColumn(trackCount = 40))
    }

    @Test
    fun `irregular chart size keeps bounded four row columns`() {
        assertEquals(4, chartRowsPerColumn(trackCount = 11))
    }
}
