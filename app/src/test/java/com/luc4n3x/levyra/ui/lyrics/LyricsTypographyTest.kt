package com.luc4n3x.levyra.ui.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTypographyTest {

    @Test
    fun `short lines keep the base size on a normal phone`() {
        val size = adaptiveLyricFontSizeSp(
            baseSizeSp = 24f,
            characterCount = 18,
            availableWidthDp = 360f,
            availableHeightDp = 800f,
            fontScale = 1f
        )
        assertEquals(24f, size, 0.01f)
    }

    @Test
    fun `very long lines shrink but stay readable`() {
        val size = adaptiveLyricFontSizeSp(
            baseSizeSp = 24f,
            characterCount = 220,
            availableWidthDp = 360f,
            availableHeightDp = 800f,
            fontScale = 1f
        )
        assertTrue(size < 24f)
        assertTrue(size >= 24f * LYRICS_MAX_SHRINK_FACTOR - 0.01f)
    }

    @Test
    fun `expanded widths get a larger lyric size`() {
        val phone = adaptiveLyricFontSizeSp(24f, 18, 360f, 800f, 1f)
        val tablet = adaptiveLyricFontSizeSp(24f, 18, 900f, 1200f, 1f)
        assertTrue(tablet > phone)
    }

    @Test
    fun `short height landscape reduces the size`() {
        val tall = adaptiveLyricFontSizeSp(24f, 18, 360f, 800f, 1f)
        val short = adaptiveLyricFontSizeSp(24f, 18, 360f, 360f, 1f)
        assertTrue(short < tall)
    }

    @Test
    fun `large accessibility font scales stay above the readable floor`() {
        val size = adaptiveLyricFontSizeSp(24f, 200, 360f, 800f, 2f)
        assertTrue(size >= LYRICS_MIN_FONT_SIZE_SP / 2f)
    }

    @Test
    fun `unknown width falls back to the scaled base size`() {
        assertEquals(24f, adaptiveLyricFontSizeSp(24f, 200, 0f, 800f, 1f), 0.01f)
    }
}
