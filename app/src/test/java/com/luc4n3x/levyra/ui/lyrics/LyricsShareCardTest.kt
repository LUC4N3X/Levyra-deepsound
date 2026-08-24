package com.luc4n3x.levyra.ui.lyrics

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsShareCardTest {
    @Test
    fun cardSizeIsSquare1080() {
        assertEquals(1080, LyricsShareCard.CARD_SIZE)
    }

    @Test
    fun coverSamplingBoundsLargeSourceDimensions() {
        assertEquals(1, LyricsShareCard.coverSampleSize(250, 250))
        assertEquals(4, LyricsShareCard.coverSampleSize(1_200, 1_200))
        assertEquals(32, LyricsShareCard.coverSampleSize(10_000, 100))
    }

    @Test
    fun buildShareTextFormatsTitleArtistAndLyrics() {
        val track = Track(
            id = "test-track-1",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            album = "A Night at the Opera",
            durationMs = 354_000L,
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "LEVYRA",
            moodTags = emptySet(),
            energy = 0,
            vocal = 0,
            replayScore = 0,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0
        )
        val lyrics = "Is this the real life?\nIs this just fantasy?"
        val shareText = LyricsShareCard.buildShareText(track, lyrics)

        assertTrue(shareText.contains("Bohemian Rhapsody"))
        assertTrue(shareText.contains("Queen"))
        assertTrue(shareText.contains("Is this the real life?"))
    }
}
