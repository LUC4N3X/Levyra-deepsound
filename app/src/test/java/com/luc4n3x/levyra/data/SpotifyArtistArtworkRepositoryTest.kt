package com.luc4n3x.levyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyArtistArtworkRepositoryTest {
    @Test
    fun acceptsSpotifyArtistAvatarArtwork() {
        assertTrue(
            isAllowedSpotifyArtistArtworkUrl(
                "https://i.scdn.co/image/ab6761610000e5eb0123456789abcdef01234567"
            )
        )
    }

    @Test
    fun rejectsSpotifyAlbumAndSingleArtwork() {
        assertFalse(
            isAllowedSpotifyArtistArtworkUrl(
                "https://i.scdn.co/image/ab67616d0000b2730123456789abcdef01234567"
            )
        )
    }

    @Test
    fun rejectsNonSpotifyArtworkHosts() {
        assertFalse(isAllowedSpotifyArtistArtworkUrl("https://example.com/artist.jpg"))
        assertFalse(isAllowedSpotifyArtistArtworkUrl("http://i.scdn.co/image/ab6761610000e5eb0123"))
    }
}
