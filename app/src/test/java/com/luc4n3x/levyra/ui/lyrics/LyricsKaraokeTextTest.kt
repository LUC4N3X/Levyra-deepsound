package com.luc4n3x.levyra.ui.lyrics

import androidx.compose.animation.core.Easing
import com.luc4n3x.levyra.domain.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsKaraokeTextTest {
    private val linear = Easing { it }

    @Test
    fun syllablesWithoutSourceSpacesStayJoined() {
        val timed = buildTimedLyricText(
            "Hello world",
            listOf(
                LyricWord(0L, 200L, "Hel"),
                LyricWord(200L, 400L, "lo "),
                LyricWord(400L, 800L, "world")
            )
        )

        assertEquals("Hello world", timed.text)
        assertEquals(listOf(0, 3, 6), timed.words.map { it.startIndex })
        assertEquals(listOf(3, 2, 5), timed.words.map { it.length })
    }

    @Test
    fun cjkCharactersAreNotSeparatedBySpaces() {
        val timed = buildTimedLyricText(
            "我爱你",
            listOf(LyricWord(0L, 100L, "我"), LyricWord(100L, 200L, "爱"), LyricWord(200L, 300L, "你"))
        )

        assertEquals("我爱你", timed.text)
        assertEquals(listOf(0, 1, 2), timed.words.map { it.startIndex })
    }

    @Test
    fun trailingPunctuationFillsTogetherWithItsWord() {
        val timed = buildTimedLyricText(
            "Hello, world!",
            listOf(LyricWord(0L, 500L, "Hello"), LyricWord(500L, 1_000L, "world"))
        )

        assertEquals("Hello, world!", timed.text)
        assertEquals(6, timed.words[0].length)
        assertEquals(timed.text.length.toFloat(), karaokeCharacterProgress(timed, 1_000L, linear), 0.001f)
    }

    @Test
    fun leadingPunctuationInsideTokenStillAligns() {
        val timed = buildTimedLyricText(
            "(oh) yeah",
            listOf(LyricWord(0L, 300L, "(oh) "), LyricWord(300L, 600L, "yeah"))
        )

        assertEquals("(oh) yeah", timed.text)
        assertEquals(0, timed.words[0].startIndex)
        assertEquals(5, timed.words[1].startIndex)
    }

    @Test
    fun mismatchedLineTextFallsBackToWordText() {
        val timed = buildTimedLyricText(
            "Completely different",
            listOf(LyricWord(0L, 300L, "Hello "), LyricWord(300L, 600L, "world"))
        )

        assertEquals("Hello world", timed.text)
    }

    @Test
    fun wordsWithoutAnySpacingInformationKeepLegacySeparation() {
        val timed = buildTimedLyricText(listOf(LyricWord(0L, 300L, "Hello"), LyricWord(300L, 600L, "world"), LyricWord(600L, 700L, "!")))

        assertEquals("Hello world!", timed.text)
    }

    @Test
    fun progressIsMonotonicAcrossGapsAndOverlaps() {
        val timed = buildTimedLyricText(
            "One two three",
            listOf(
                LyricWord(0L, 400L, "One "),
                LyricWord(300L, 700L, "two "),
                LyricWord(1_200L, 1_200L, "three")
            )
        )
        var previous = 0f
        for (position in 0L..1_400L step 20L) {
            val progress = karaokeCharacterProgress(timed, position, linear)
            assertEquals(true, progress >= previous)
            previous = progress
        }
        assertEquals(timed.text.length.toFloat(), previous, 0.001f)
    }

    @Test
    fun manualOffsetShiftsWordProgressExactlyOnce() {
        val words = listOf(LyricWord(10_000L, 10_400L, "Hello "), LyricWord(10_400L, 10_900L, "world"))
        val timed = buildTimedLyricText("Hello world", words)
        val offsetMs = 250L

        val reported = 10_450L + offsetMs
        val shifted = karaokeCharacterProgress(timed, lyricsOffsetPosition(reported, offsetMs), linear)
        val reference = karaokeCharacterProgress(timed, 10_450L, linear)

        assertEquals(reference, shifted, 0.0001f)
    }
}
