package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SameAlbumArtworkFallbackTest {
    @Test
    fun `featured credit in the title still finds the same album recording`() {
        val seed = track("LA FINE DEL MONDO", "Sgribaz feat. Disme", "Emotional Kid")
        val spotify = track("La Fine Del Mondo (feat. Disme)", "Sgribaz, Disme", "Emotional Kid")
        val other = track("La Fine Del Mondo", "Gianni Vezzosi", "La Fine Del Mondo")

        assertEquals(spotify, sameAlbumRecordingForArtwork(seed, listOf(other, spotify)))
    }

    @Test
    fun `versions, other artists and unknown albums never borrow artwork`() {
        val seed = track("Song", "Artist", "Album")

        assertNull(sameAlbumRecordingForArtwork(seed, listOf(track("Song (Live)", "Artist", "Album"))))
        assertNull(sameAlbumRecordingForArtwork(seed, listOf(track("Song", "Someone Else", "Album"))))
        assertNull(sameAlbumRecordingForArtwork(seed, listOf(track("Song", "Artist", "Greatest Hits"))))
        assertNull(sameAlbumRecordingForArtwork(seed.copy(album = ""), listOf(track("Song", "Artist", "Album"))))
    }

    private fun track(title: String, artist: String, album: String) = Track(
        id = "$title|$artist",
        title = title,
        artist = artist,
        album = album,
        durationMs = 0L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "spotify",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
