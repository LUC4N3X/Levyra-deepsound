package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyClipsAudioSelectionTest {
    @Test
    fun onlyClipsWithOriginalSpotifyAudioEnterTheFeed() {
        val ready = clip("ready")
        val missingAudio = clip("missing").copy(audioPreviewUrl = "")
        val missingIdentity = clip("unknown").copy(spotifyTrackId = "")
        val foreignAudio = clip("foreign").copy(audioPreviewUrl = "https://example.com/preview.mp3")
        val catalog = SpotifyClipsCatalog(emptyMap(), emptyMap(), listOf(missingAudio, ready, missingIdentity, foreignAudio))

        assertEquals(listOf(ready), rankSpotifyClips(catalog, "en", emptyList(), 12, 1L))
    }

    @Test
    fun noPlayablePreviewAllowsExistingNativeSamplesFallback() {
        val catalog = SpotifyClipsCatalog(emptyMap(), emptyMap(), listOf(clip("missing").copy(audioPreviewUrl = "")))

        assertTrue(rankSpotifyClips(catalog, "en", emptyList(), 12, 1L).isEmpty())
    }

    private fun clip(id: String) = SpotifyClip(
        id = id, genre = "pop", title = id, artist = "Artist", artworkUrl = "",
        canvasUrl = "https://canvaz.scdn.co/example.cnvs.mp4", audioVideoId = "",
        spotifyTrackId = "7lQ8MOhq6IN2w8EYcFNSUk",
        audioPreviewUrl = "https://p.scdn.co/mp3-preview/35fc7456c892c11867fe1513b92dc44e9664eaba"
    )
}
