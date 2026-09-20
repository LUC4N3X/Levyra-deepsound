package com.luc4n3x.levyra.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsRequestGenerationTest {
    @Test
    fun translatedResultAppliesOnlyToTheCurrentGenerationAndTrack() {
        assertTrue(isCurrentLyricsRequest(7L, 7L, "track-a", "track-a"))
        assertFalse(isCurrentLyricsRequest(6L, 7L, "track-a", "track-a"))
        assertFalse(isCurrentLyricsRequest(7L, 7L, "track-a", "track-b"))
        assertFalse(isCurrentLyricsRequest(7L, 7L, "track-a", null))
    }
}
