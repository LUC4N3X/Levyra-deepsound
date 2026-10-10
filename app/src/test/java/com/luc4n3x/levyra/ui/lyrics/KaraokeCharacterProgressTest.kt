package com.luc4n3x.levyra.ui.lyrics

import androidx.compose.animation.core.LinearEasing
import com.luc4n3x.levyra.domain.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KaraokeCharacterProgressTest {

    @Test
    fun `buildTimedLyricText joins words with spaces for normal text`() {
        val words = listOf(
            LyricWord(text = "Hello", startMs = 1_000L, endMs = 1_500L),
            LyricWord(text = "world", startMs = 1_600L, endMs = 2_000L)
        )
        val timedText = buildTimedLyricText(words)
        assertEquals("Hello world", timedText.text)
        assertEquals(2, timedText.words.size)
        assertEquals(0, timedText.words[0].startIndex)
        assertEquals(5, timedText.words[0].length)
        assertEquals(6, timedText.words[1].startIndex)
        assertEquals(5, timedText.words[1].length)
    }

    @Test
    fun `buildTimedLyricText does not add leading space before punctuation`() {
        val words = listOf(
            LyricWord(text = "Stay", startMs = 1_000L, endMs = 1_500L),
            LyricWord(text = ",", startMs = 1_500L, endMs = 1_600L),
            LyricWord(text = "don't", startMs = 1_700L, endMs = 2_200L),
            LyricWord(text = "go", startMs = 2_300L, endMs = 2_800L),
            LyricWord(text = "!", startMs = 2_800L, endMs = 2_900L)
        )
        val timedText = buildTimedLyricText(words)
        assertEquals("Stay, don't go!", timedText.text)
    }

    @Test
    fun `buildTimedLyricText ignores blank words and enforces minimum duration`() {
        val words = listOf(
            LyricWord(text = "  ", startMs = 100L, endMs = 200L),
            LyricWord(text = "Echo", startMs = 500L, endMs = 500L)
        )
        val timedText = buildTimedLyricText(words)
        assertEquals("Echo", timedText.text)
        assertEquals(1, timedText.words.size)
        assertEquals(501L, timedText.words[0].endMs)
    }

    @Test
    fun `karaokeCharacterProgress returns zero before line starts`() {
        val words = listOf(
            LyricWord(text = "Sing", startMs = 1_000L, endMs = 2_000L)
        )
        val timedText = buildTimedLyricText(words)
        val progress = karaokeCharacterProgress(timedText, positionMs = 500L, easing = LinearEasing)
        assertEquals(0f, progress, 0.0001f)
    }

    @Test
    fun `karaokeCharacterProgress computes mid-word linear character progress`() {
        val words = listOf(
            LyricWord(text = "Sing", startMs = 1_000L, endMs = 2_000L)
        )
        val timedText = buildTimedLyricText(words)
        val progress = karaokeCharacterProgress(timedText, positionMs = 1_500L, easing = LinearEasing)
        assertEquals(2f, progress, 0.0001f)
    }

    @Test
    fun `karaokeCharacterProgress completes previous word during gap`() {
        val words = listOf(
            LyricWord(text = "One", startMs = 1_000L, endMs = 2_000L),
            LyricWord(text = "Two", startMs = 3_000L, endMs = 4_000L)
        )
        val timedText = buildTimedLyricText(words)
        val progress = karaokeCharacterProgress(timedText, positionMs = 2_500L, easing = LinearEasing)
        assertEquals(3f, progress, 0.0001f)
    }

    @Test
    fun `karaokeCharacterProgress caps at full string length when finished`() {
        val words = listOf(
            LyricWord(text = "Done", startMs = 1_000L, endMs = 2_000L)
        )
        val timedText = buildTimedLyricText(words)
        val progress = karaokeCharacterProgress(timedText, positionMs = 10_000L, easing = LinearEasing)
        assertEquals(4f, progress, 0.0001f)
    }

    @Test
    fun `karaokeCharacterProgress returns zero for empty text`() {
        val timedText = buildTimedLyricText(emptyList())
        assertEquals(0f, karaokeCharacterProgress(timedText, 5_000L, LinearEasing), 0.0001f)
    }

    @Test
    fun `lyricsInstrumentalDotIntensity remains within zero to one bounds`() {
        val testSteps = 50
        for (i in 0..testSteps) {
            val progress = i.toFloat() / testSteps.toFloat()
            for (dot in 0 until LYRICS_INSTRUMENTAL_DOT_COUNT) {
                val intensity = lyricsInstrumentalDotIntensity(progress, dot)
                assertTrue("Intensity $intensity at progress $progress dot $dot should be >= 0f", intensity >= 0f)
                assertTrue("Intensity $intensity at progress $progress dot $dot should be <= 1f", intensity <= 1f)
            }
        }
    }
}
