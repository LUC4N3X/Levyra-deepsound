package com.luc4n3x.levyra.desktop.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePlaybackUrlTest {
    @Test
    fun addsRequestNumberToProgressiveGoogleVideo() {
        val input = "https://rr1---sn.example.googlevideo.com/videoplayback?expire=2000000000&id=abc"
        val output = youtubePlaybackUrl(input)

        assertTrue(output.startsWith(input + "&rn="))
    }

    @Test
    fun preservesExistingRequestNumber() {
        val input = "https://rr1---sn.example.googlevideo.com/videoplayback?expire=2000000000&rn=77&id=abc"

        assertEquals(input, youtubePlaybackUrl(input))
    }

    @Test
    fun segmentedGoogleVideoIsNotModified() {
        val input = "https://rr1---sn.example.googlevideo.com/videoplayback?expire=2000000000&sq=5&id=abc"

        assertEquals(input, youtubePlaybackUrl(input))
    }

    @Test
    fun nonYoutubeUrlIsNotModified() {
        val input = "https://example.com/audio.m4a?x=1"

        assertEquals(input, youtubePlaybackUrl(input))
    }

    @Test
    fun onlyHttpsGoogleVideoUsesLocalBridge() {
        assertTrue(
            shouldBridgeYoutubePlayback(
                "https://rr1---sn.example.googlevideo.com/videoplayback?expire=2000000000&id=abc"
            )
        )
        assertTrue(!shouldBridgeYoutubePlayback("http://rr1---sn.example.googlevideo.com/videoplayback?id=abc"))
        assertTrue(!shouldBridgeYoutubePlayback("https://example.com/audio.webm"))
        assertTrue(!shouldBridgeYoutubePlayback("file:///C:/Music/song.webm"))
    }

    @Test
    fun replacementGuardSuppressesOldTerminalEventsUntilNewMediaOpens() {
        val guard = MediaReplacementGuard()

        assertFalse(guard.shouldSuppressTerminalEvent())
        guard.begin()
        assertTrue(guard.shouldSuppressTerminalEvent())
        guard.opened()
        assertFalse(guard.shouldSuppressTerminalEvent())
    }
}
