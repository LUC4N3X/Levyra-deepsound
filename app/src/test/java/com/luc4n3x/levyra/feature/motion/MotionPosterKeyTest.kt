package com.luc4n3x.levyra.feature.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MotionPosterKeyTest {

    private fun artwork(url: String, mimeType: String, posterUrl: String?) = MotionArtwork(
        identityKey = "key", provider = "p", url = url, mimeType = mimeType, width = null, height = null,
        confidence = 90, expiresAtMs = 1L, lastVerifiedAtMs = 1L, configEpoch = 1L, posterUrl = posterUrl
    )

    @Test
    fun publishedPosterWins() {
        assertEquals("https://a/p.jpg", MotionPosterCache.posterKey(artwork("https://a/v.m3u8", "application/x-mpegURL", "https://a/p.jpg")))
    }

    @Test
    fun mp4WithoutPosterUsesItsFirstFrame() {
        assertEquals("https://a/v.mp4", MotionPosterCache.posterKey(artwork("https://a/v.mp4", "video/mp4", null)))
        assertEquals("https://a/v.mp4", MotionPosterCache.posterKey(artwork("https://a/v.mp4", "video/mp4", "")))
    }

    @Test
    fun streamsWithoutPosterAndInsecureUrlsHaveNone() {
        assertNull(MotionPosterCache.posterKey(artwork("https://a/v.m3u8", "application/x-mpegURL", null)))
        assertNull(MotionPosterCache.posterKey(artwork("http://a/v.mp4", "video/mp4", null)))
        assertNull(MotionPosterCache.posterKey(null))
    }
}
