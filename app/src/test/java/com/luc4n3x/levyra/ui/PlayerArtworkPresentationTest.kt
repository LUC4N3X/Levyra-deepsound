package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerArtworkPresentationTest {
    @Test
    fun `prefers album artwork over a youtube frame and upgrades spotify size`() {
        val track = Track(
            id = "track",
            title = "Song",
            artist = "Artist",
            album = "Album",
            durationMs = 180_000L,
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = "https://i.scdn.co/image/ab67616d00001e02abcdef",
            largeThumbnailUrl = "https://i.ytimg.com/vi/abcdefghijk/maxresdefault.jpg",
            source = "test",
            moodTags = emptySet(),
            energy = 0,
            vocal = 0,
            replayScore = 0,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0
        )

        assertEquals(
            "https://i.scdn.co/image/ab67616d0000b273abcdef",
            preferredPlayerArtworkUrl(track)
        )
    }

    @Test
    fun `requests full size apple artwork`() {
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/example/1200x1200bb.jpg",
            highResolutionPlayerArtworkUrl(
                "https://is1-ssl.mzstatic.com/image/thumb/example/{w}x{h}bb.jpg"
            )
        )
    }
}
