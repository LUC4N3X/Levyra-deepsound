package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsEngineTest {
    private val engine = LyricsEngine()

    @Test
    fun binarySearchHandlesOverlappingBackgroundVocals() {
        val main = LyricLine(1_000L, 4_000L, "Main", "")
        val background = LyricLine(2_000L, 3_000L, "Background", "", role = LyricVocalRole.BACKGROUND)
        val next = LyricLine(4_100L, 6_000L, "Next", "")
        val lines = listOf(main, background, next)

        assertEquals(background, engine.currentLine(2_500L, lines))
        assertEquals(main, engine.currentLine(3_500L, lines))
        assertEquals(next, engine.currentLine(5_000L, lines))
    }

    @Test
    fun returnsLatestStartedLineInsideInstrumentalGap() {
        val first = LyricLine(1_000L, 2_000L, "First", "")
        val second = LyricLine(5_000L, 6_000L, "Second", "")

        assertEquals(first, engine.currentLine(3_500L, listOf(first, second)))
    }

    @Test
    fun returnsNullBeforeFirstLine() {
        assertNull(engine.currentLine(500L, listOf(LyricLine(1_000L, 2_000L, "First", ""))))
    }
}
