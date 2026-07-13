package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraLocalIntelligenceTest {
    private val track = Track(
        id = "track-1",
        title = "Luce",
        artist = "Levyra",
        album = "Orbita",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "Test",
        moodTags = setOf("dream", "light"),
        energy = 72,
        vocal = 80,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )

    @Test
    fun analysisStaysLocalAndFindsRepeatedPhrases() {
        val lines = listOf(
            LyricLine(0L, 1_000L, "Siamo luce nella notte", ""),
            LyricLine(1_000L, 2_000L, "Camminiamo insieme", ""),
            LyricLine(2_000L, 3_000L, "Siamo luce nella notte", "")
        )

        val result = LevyraLocalIntelligence().analyze(track, lines)

        assertTrue(result.localOnly)
        assertTrue(result.available)
        assertEquals(3, result.lineCount)
        assertTrue(result.repeatedPhrases.contains("Siamo luce nella notte"))
        assertTrue(result.themes.any { it.equals("Luce", ignoreCase = true) })
    }

    @Test
    fun metadataFallbackWorksWithoutLyrics() {
        val result = LevyraLocalIntelligence().analyze(track, emptyList())

        assertTrue(result.available)
        assertTrue(result.overview.contains("Luce"))
        assertTrue(result.overview.contains("Levyra"))
    }
}
