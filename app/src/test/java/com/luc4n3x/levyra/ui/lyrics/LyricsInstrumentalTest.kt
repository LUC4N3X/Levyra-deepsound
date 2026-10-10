package com.luc4n3x.levyra.ui.lyrics

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsInstrumentalTest {

    private fun line(startMs: Long, endMs: Long, role: LyricVocalRole = LyricVocalRole.MAIN) =
        LyricLine(startMs = startMs, endMs = endMs, text = "line", role = role)

    @Test
    fun `normal short gaps do not become instrumental breaks`() {
        val lines = listOf(line(0, 2_000), line(2_400, 4_000), line(4_500, 6_000))
        assertTrue(lyricsInstrumentalGaps(lines, synced = true).isEmpty())
    }

    @Test
    fun `a long silence between sung lines is reported`() {
        val lines = listOf(line(0, 2_000), line(20_000, 22_000))
        val gaps = lyricsInstrumentalGaps(lines, synced = true)
        assertEquals(1, gaps.size)
        assertEquals(2_000L, gaps.first().startMs)
        assertEquals(20_000L, gaps.first().endMs)
        assertEquals(1, gaps.first().nextLineIndex)
    }

    @Test
    fun `unsynced lyrics never produce instrumental breaks`() {
        val lines = listOf(line(0, 2_000), line(20_000, 22_000))
        assertTrue(lyricsInstrumentalGaps(lines, synced = false).isEmpty())
    }

    @Test
    fun `background vocals do not open or close a gap`() {
        val lines = listOf(
            line(0, 2_000),
            line(6_000, 6_400, LyricVocalRole.BACKGROUND),
            line(20_000, 22_000)
        )
        val gaps = lyricsInstrumentalGaps(lines, synced = true)
        assertEquals(1, gaps.size)
        assertEquals(2, gaps.first().nextLineIndex)
    }

    @Test
    fun `the indicator only shows inside the gap window`() {
        val gaps = lyricsInstrumentalGaps(listOf(line(0, 2_000), line(20_000, 22_000)), synced = true)
        assertNull(activeLyricsInstrumentalGap(1_000L, gaps))
        assertNull(activeLyricsInstrumentalGap(2_100L, gaps))
        assertEquals(gaps.first(), activeLyricsInstrumentalGap(10_000L, gaps))
        assertNull(activeLyricsInstrumentalGap(19_900L, gaps))
        assertNull(activeLyricsInstrumentalGap(25_000L, gaps))
    }

    @Test
    fun `seeking backwards resolves the correct gap`() {
        val lines = listOf(line(0, 2_000), line(20_000, 22_000), line(60_000, 62_000))
        val gaps = lyricsInstrumentalGaps(lines, synced = true)
        assertEquals(2, gaps.size)
        assertEquals(gaps[1], activeLyricsInstrumentalGap(40_000L, gaps))
        assertEquals(gaps[0], activeLyricsInstrumentalGap(10_000L, gaps))
    }

    @Test
    fun `progress is bounded`() {
        val gap = LyricsInstrumentalGap(startMs = 1_000L, endMs = 11_000L, nextLineIndex = 1)
        assertEquals(0f, lyricsInstrumentalProgress(0L, gap), 0.0001f)
        assertEquals(0.5f, lyricsInstrumentalProgress(6_000L, gap), 0.0001f)
        assertEquals(1f, lyricsInstrumentalProgress(99_000L, gap), 0.0001f)
    }
}
