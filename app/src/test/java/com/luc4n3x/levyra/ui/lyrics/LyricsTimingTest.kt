package com.luc4n3x.levyra.ui.lyrics

import androidx.compose.animation.core.Easing
import com.luc4n3x.levyra.data.LyricsLatencyProfiles
import com.luc4n3x.levyra.data.MAX_LYRICS_OFFSET_MS
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LyricsTimingTest {

    private val identityEasing = Easing { it }

    private fun line(startMs: Long, endMs: Long) = LyricLine(
        startMs = startMs,
        endMs = endMs,
        text = "line",
        translated = "translated",
        romanized = "romanized"
    )

    @Test
    fun `positive offset makes lyrics appear later`() {
        val lineStart = 10_000L
        val offset = adjustLyricsOffset(0L, LYRICS_OFFSET_STEP_MS)

        assertEquals(500L, offset)
        assertEquals(lineStart, lyricsOffsetPosition(lineStart + offset, offset))
        assertEquals(lineStart + 200L, lyricsOffsetPosition(lineStart + offset + 200L, offset))
    }

    @Test
    fun `negative offset makes lyrics appear earlier`() {
        val lineStart = 10_000L
        val offset = adjustLyricsOffset(0L, -LYRICS_OFFSET_STEP_MS)

        assertEquals(-500L, offset)
        assertEquals(lineStart, lyricsOffsetPosition(lineStart + offset, offset))
        assertEquals(lineStart + 200L, lyricsOffsetPosition(lineStart + offset + 200L, offset))
    }

    @Test
    fun `reset returns the offset to zero`() {
        var offset = 1_500L
        offset = adjustLyricsOffset(offset, -offset)

        assertEquals(0L, offset)
        assertEquals(12_345L, lyricsOffsetPosition(12_345L, offset))
    }

    @Test
    fun `adjustments are clamped to the latency profile bounds`() {
        var offset = 0L
        repeat(20) { offset = adjustLyricsOffset(offset, LYRICS_OFFSET_STEP_MS) }
        assertEquals(MAX_LYRICS_OFFSET_MS, offset)
        repeat(40) { offset = adjustLyricsOffset(offset, -LYRICS_OFFSET_STEP_MS) }
        assertEquals(-MAX_LYRICS_OFFSET_MS, offset)
    }

    @Test
    fun `the offset shifts which line is active without mutating line timestamps`() {
        val first = line(1_000L, 2_000L)
        val second = line(4_000L, 5_000L)
        val lines = listOf(first, second)
        val offset = adjustLyricsOffset(0L, LYRICS_OFFSET_STEP_MS)

        assertEquals(1, activeLyricIndex(second.startMs, lines))
        assertEquals(0, activeLyricIndex(lyricsOffsetPosition(second.startMs, offset), lines))
        assertEquals(1, activeLyricIndex(lyricsOffsetPosition(second.startMs + offset, offset), lines))
        assertEquals(-1, activeLyricIndex(lyricsOffsetPosition(first.startMs + offset - 1L, offset), lines))
        assertEquals(first.startMs, lines[0].startMs)
        assertEquals(second.startMs, lines[1].startMs)
    }

    @Test
    fun `translation and romanization stay attached to the original line`() {
        val first = line(1_000L, 2_000L)
        val second = line(4_000L, 5_000L)
        val lines = listOf(first, second)
        val offset = adjustLyricsOffset(0L, LYRICS_OFFSET_STEP_MS)

        val active = lines[activeLyricIndex(lyricsOffsetPosition(second.startMs + offset, offset), lines)]

        assertSame(second, active)
        assertEquals("translated", active.translated)
        assertEquals("romanized", active.romanized)
    }

    @Test
    fun `word timing inside the line stays untouched by the offset`() {
        val words = listOf(
            LyricWord(startMs = 4_000L, endMs = 4_400L, text = "Hello"),
            LyricWord(startMs = 4_400L, endMs = 4_900L, text = "world")
        )
        val timedText = buildTimedLyricText(words)
        val offset = adjustLyricsOffset(0L, LYRICS_OFFSET_STEP_MS)

        listOf(0L, 50L, 100L, 200L, 400L, 500L, 900L).forEach { delta ->
            val shifted = karaokeCharacterProgress(
                timedText = timedText,
                positionMs = lyricsOffsetPosition(words.first().startMs + delta + offset, offset),
                easing = identityEasing
            )
            val reference = karaokeCharacterProgress(
                timedText = timedText,
                positionMs = words.first().startMs + delta,
                easing = identityEasing
            )
            assertEquals(shifted, reference, 0.0001f)
        }
    }

    @Test
    fun `adjusted offsets persist through the latency profile round trip`() {
        var offset = 0L
        repeat(3) { offset = adjustLyricsOffset(offset, LYRICS_OFFSET_STEP_MS) }
        val profiles = LyricsLatencyProfiles().withGlobalOffset(offset)

        assertEquals(offset, profiles.resolve(routeKey = null, bluetooth = false))
    }
}
