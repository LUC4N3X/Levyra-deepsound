package com.luc4n3x.levyra.ui

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlacierMoodTileToneTest {
    @Test
    fun everyToneStaysInTheGlacierBlueFamily() {
        for (index in 0 until 24) {
            val tone = glacierTileTone(index)
            assertTrue("index=$index", tone.blue > tone.red && tone.blue >= tone.green)
        }
    }

    @Test
    fun horizontalAndVerticalNeighboursUseDifferentDepths() {
        for (index in 0 until 22) {
            assertNotEquals(glacierTileTone(index), glacierTileTone(index + 1))
            assertNotEquals(glacierTileTone(index), glacierTileTone(index + 2))
        }
    }
}
