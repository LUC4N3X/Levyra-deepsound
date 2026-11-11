package com.luc4n3x.levyra.ui.lyrics

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsWordEstimateTest {
    @Test
    fun `line synced lyrics get ordered words inside the line window`() {
        val line = LyricLine(10_000L, 14_000L, "Will dich lieben und verdammen")
        val words = estimatedLyricWords(line, null)

        assertEquals(listOf("Will", "dich", "lieben", "und", "verdammen"), words.map { it.text })
        assertEquals(10_000L, words.first().startMs)
        assertTrue(words.zipWithNext().all { (a, b) -> a.endMs <= b.startMs + 1 && a.startMs < b.startMs })
        assertTrue(words.last().endMs <= 14_000L)
    }

    @Test
    fun `unspaced scripts are timed per character`() {
        val words = estimatedLyricWords(LyricLine(0L, 3_000L, "また会う日まで"), null)
        assertEquals("また会う日まで", words.joinToString("") { it.text })
        assertEquals(7, words.size)
    }

    @Test
    fun `real word timing and metadata are left untouched`() {
        val timed = LyricLine(0L, 1_000L, "Hello", words = listOf(LyricWord(0L, 900L, "Hello")))
        val metadata = LyricLine(0L, 1_000L, "Writer: X", isMetadata = true)
        val lines = listOf(timed, metadata)
        assertSame(lines, lyricsWithEstimatedWords(lines))
    }

    @Test
    fun `missing end time falls back to the next line`() {
        val lines = lyricsWithEstimatedWords(listOf(LyricLine(0L, 0L, "Ich will"), LyricLine(2_000L, 0L, "Wir sind")))
        assertTrue(lines.first().words.last().endMs <= 2_000L)
        assertTrue(lines.last().words.isEmpty())
    }
}
