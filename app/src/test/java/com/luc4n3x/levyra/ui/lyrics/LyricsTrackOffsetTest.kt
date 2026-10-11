package com.luc4n3x.levyra.ui.lyrics

import androidx.compose.animation.core.Easing
import com.luc4n3x.levyra.data.LyricsLatencyProfiles
import com.luc4n3x.levyra.data.MAX_LYRICS_TRACK_OFFSET_MS
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricWord
import com.luc4n3x.levyra.ui.i18n.lyricsSyncCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LyricsTrackOffsetTest {

    private val identityEasing = Easing { it }

    private fun line(startMs: Long, endMs: Long) = LyricLine(
        startMs = startMs,
        endMs = endMs,
        text = "line",
        translated = "translated",
        romanized = "romanized"
    )

    @Test
    fun `track adjustments step by half a second and clamp at thirty seconds`() {
        var offset = 0L
        repeat(3) { offset = adjustLyricsTrackOffset(offset, LYRICS_OFFSET_STEP_MS) }
        assertEquals(1_500L, offset)
        offset = adjustLyricsTrackOffset(offset, LYRICS_OFFSET_JUMP_MS)
        assertEquals(6_500L, offset)
        repeat(100) { offset = adjustLyricsTrackOffset(offset, LYRICS_OFFSET_STEP_MS) }
        assertEquals(MAX_LYRICS_TRACK_OFFSET_MS, offset)
        repeat(200) { offset = adjustLyricsTrackOffset(offset, -LYRICS_OFFSET_STEP_MS) }
        assertEquals(-MAX_LYRICS_TRACK_OFFSET_MS, offset)
        assertEquals(0L, adjustLyricsTrackOffset(offset, -offset))
    }

    @Test
    fun `slider values snap to the half second grid inside the range`() {
        assertEquals(0L, snapLyricsTrackOffset(240L))
        assertEquals(500L, snapLyricsTrackOffset(260L))
        assertEquals(-15_000L, snapLyricsTrackOffset(-14_990L))
        assertEquals(15_000L, snapLyricsTrackOffset(15_010L))
        assertEquals(MAX_LYRICS_TRACK_OFFSET_MS, snapLyricsTrackOffset(45_000L))
        assertEquals(-MAX_LYRICS_TRACK_OFFSET_MS, snapLyricsTrackOffset(-45_000L))
    }

    @Test
    fun `manual input accepts signed seconds with dot or comma`() {
        assertEquals(15_000L, parseLyricsOffsetSeconds("15"))
        assertEquals(15_000L, parseLyricsOffsetSeconds("+15"))
        assertEquals(-15_000L, parseLyricsOffsetSeconds("-15"))
        assertEquals(-2_500L, parseLyricsOffsetSeconds("−2,5"))
        assertEquals(1_250L, parseLyricsOffsetSeconds(" 1.25 s "))
        assertEquals(500L, parseLyricsOffsetSeconds(".5"))
        assertEquals(30_000L, parseLyricsOffsetSeconds("30"))
        assertEquals(-30_000L, parseLyricsOffsetSeconds("-30.0"))
        assertEquals(0L, parseLyricsOffsetSeconds("0"))
    }

    @Test
    fun `manual input rejects invalid or out of range values`() {
        listOf("", " ", "-", "+", "abc", "1e3", "12.3456", "1..2", "--3", "30.5", "-31", "100", "NaN", "Infinity", "5 5")
            .forEach { input -> assertNull(input, parseLyricsOffsetSeconds(input)) }
    }

    @Test
    fun `offset labels make the sign explicit`() {
        assertEquals("+15.0 s", formatLyricsOffsetSeconds(15_000L))
        assertEquals("−30.0 s", formatLyricsOffsetSeconds(-30_000L))
        assertEquals("0.0 s", formatLyricsOffsetSeconds(0L))
        assertEquals("+0.5 s", formatLyricsOffsetSeconds(500L))
        assertEquals("15", formatLyricsOffsetInput(15_000L))
        assertEquals("-2.5", formatLyricsOffsetInput(-2_500L))
        assertEquals("1.25", formatLyricsOffsetInput(1_250L))
        assertEquals(15_000L, parseLyricsOffsetSeconds(formatLyricsOffsetInput(15_000L)))
        assertEquals(-2_500L, parseLyricsOffsetSeconds(formatLyricsOffsetInput(-2_500L)))
    }

    @Test
    fun `state labels describe direction in plain words`() {
        val copy = lyricsSyncCopy("it")
        assertEquals("Testo ritardato di \u206615.0 s\u2069", lyricsSyncStateLabel(copy, 15_000L))
        assertEquals("Testo anticipato di \u20662.5 s\u2069", lyricsSyncStateLabel(copy, -2_500L))
        assertEquals("Testo sincronizzato", lyricsSyncStateLabel(copy, 0L))
    }

    @Test
    fun `calibration stores only the part not already covered by the latency profile`() {
        val latency = 300L
        val track = calibratedLyricsTrackOffset(positionMs = 64_300L, lineStartMs = 50_000L, latencyOffsetMs = latency)

        assertEquals(14_000L, track)
        assertEquals(50_000L, lyricsOffsetPosition(64_300L, latency + track))
        assertEquals(MAX_LYRICS_TRACK_OFFSET_MS, calibratedLyricsTrackOffset(120_000L, 1_000L, 0L))
    }

    @Test
    fun `large corrections at every requested value shift the active line`() {
        val lines = listOf(line(20_000L, 24_000L), line(40_000L, 44_000L), line(60_000L, 64_000L))
        listOf(-30_000L, -15_000L, 0L, 15_000L, 30_000L).forEach { trackOffset ->
            val profiles = LyricsLatencyProfiles(globalOffsetMs = 200L).withTrackOffset("song", trackOffset)
            val offset = profiles.effectiveOffsetMs("song", routeKey = null, bluetooth = false)
            val audible = lines[1].startMs + offset

            assertEquals(trackOffset.toString(), 1, activeLyricIndex(lyricsOffsetPosition(audible, offset), lines))
            assertEquals(40_000L, lines[1].startMs)
        }
    }

    @Test
    fun `early lines never see a negative lyrics position`() {
        val lines = listOf(line(0L, 2_000L), line(2_000L, 4_000L))
        val offset = 30_000L

        assertEquals(0L, lyricsOffsetPosition(1_000L, offset))
        assertEquals(0, activeLyricIndex(lyricsOffsetPosition(1_000L, offset), lines))
        assertEquals(0L, lyricsOffsetPosition(0L, 0L))
        assertEquals(31_000L, lyricsOffsetPosition(1_000L, -30_000L))
    }

    @Test
    fun `tapping a line seeks the audio where that line plays with the correction`() {
        val target = line(42_000L, 45_000L)
        listOf(-30_000L, -15_000L, 0L, 15_000L, 30_000L).forEach { offset ->
            val seekMs = (target.startMs + offset).coerceAtLeast(0L)
            val lines = listOf(line(0L, 1_000L), target)
            assertEquals(1, activeLyricIndex(lyricsOffsetPosition(seekMs, offset), lines))
        }
    }

    @Test
    fun `translations and romanizations follow the corrected line`() {
        val second = line(40_000L, 44_000L)
        val lines = listOf(line(10_000L, 14_000L), second)
        val offset = -15_000L

        val active = lines[activeLyricIndex(lyricsOffsetPosition(second.startMs + offset, offset), lines)]

        assertSame(second, active)
        assertEquals("translated", active.translated)
        assertEquals("romanized", active.romanized)
    }

    @Test
    fun `word timed karaoke follows large corrections exactly`() {
        val words = listOf(
            LyricWord(startMs = 40_000L, endMs = 40_400L, text = "Hello"),
            LyricWord(startMs = 40_400L, endMs = 40_900L, text = "world")
        )
        val timedText = buildTimedLyricText(words)
        listOf(-30_000L, 15_000L, 30_000L).forEach { offset ->
            listOf(0L, 100L, 400L, 900L).forEach { delta ->
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
    }
}
