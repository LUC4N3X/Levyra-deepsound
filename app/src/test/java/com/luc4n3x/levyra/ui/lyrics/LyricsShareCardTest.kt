package com.luc4n3x.levyra.ui.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsShareCardTest {
    @Test
    fun coverSamplingBoundsLargeSourceDimensions() {
        assertEquals(1, LyricsShareCard.coverSampleSize(300, 300))
        assertEquals(2, LyricsShareCard.coverSampleSize(1_200, 1_200))
        assertEquals(32, LyricsShareCard.coverSampleSize(10_000, 100))
    }

    @Test
    fun textDirectionFollowsTheFirstStrongUnicodeCharacter() {
        assertTrue(isRtlText("مرحبا بالعالم 🎵"))
        assertTrue(isRtlText("שלום עולם"))
        assertFalse(isRtlText("Hello عالم"))
    }

    @Test
    fun unicodeTruncationNeverSplitsASurrogatePair() {
        assertEquals("A🎵", boundedLyricsShareText("A🎵B", maxCodePoints = 2))
    }
}
