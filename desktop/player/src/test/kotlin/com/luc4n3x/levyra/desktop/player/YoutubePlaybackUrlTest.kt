package com.luc4n3x.levyra.desktop.player

import org.junit.Assert.assertEquals
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
}
