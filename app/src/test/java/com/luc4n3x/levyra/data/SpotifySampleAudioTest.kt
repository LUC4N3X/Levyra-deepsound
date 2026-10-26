package com.luc4n3x.levyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import com.luc4n3x.levyra.domain.Track
import org.junit.Test

class SpotifySampleAudioTest {
    @Test
    fun originalPreviewReplacesFullSongAndClearsYoutubePairing() {
        val track = Track(
            id = "spotify-clip-example", title = "Without Me", artist = "Eminem", album = "",
            durationMs = 290_320L, streamUrl = "https://youtube.test/audio", videoUrl = "https://youtube.test/video",
            thumbnailUrl = "", largeThumbnailUrl = "", source = "YouTube Shorts", moodTags = emptySet(),
            energy = 50, vocal = 50, replayScore = 50, cacheScore = 50, accentStart = 0, accentEnd = 0,
            videoStreamUrl = "https://youtube.test/video", audioVideoId = "oldaudio001", counterpartVideoId = "oldvideo001"
        )
        val audio = SpotifySampleAudio("7lQ8MOhq6IN2w8EYcFNSUk", "https://p.scdn.co/mp3-preview/35fc7456c892c11867fe1513b92dc44e9664eaba")
        val result = track.withSpotifySampleAudio(audio)

        assertEquals(track.id, result.id)
        assertEquals(audio.url, result.streamUrl)
        assertEquals("https://open.spotify.com/track/${audio.trackId}", result.videoUrl)
        assertEquals("", result.videoStreamUrl)
        assertEquals("", result.audioVideoId)
        assertEquals("", result.counterpartVideoId)
        assertEquals(0L, result.durationMs)
    }

    @Test
    fun acceptsOriginalSpotifyPreviewWithoutAFileExtension() {
        assertTrue(isSpotifySampleAudioUrl("https://p.scdn.co/mp3-preview/35fc7456c892c11867fe1513b92dc44e9664eaba"))
    }

    @Test
    fun rejectsOtherHostsCredentialsAndNonPreviewMedia() {
        listOf(
            "https://p.scdn.co.evil.test/mp3-preview/35fc7456c892c11867fe1513b92dc44e9664eaba",
            "https://user@p.scdn.co/mp3-preview/35fc7456c892c11867fe1513b92dc44e9664eaba",
            "http://p.scdn.co/mp3-preview/35fc7456c892c11867fe1513b92dc44e9664eaba",
            "https://p.scdn.co/other/35fc7456c892c11867fe1513b92dc44e9664eaba",
            "https://canvaz.scdn.co/video.cnvs.mp4"
        ).forEach { assertFalse(isSpotifySampleAudioUrl(it)) }
    }
}
