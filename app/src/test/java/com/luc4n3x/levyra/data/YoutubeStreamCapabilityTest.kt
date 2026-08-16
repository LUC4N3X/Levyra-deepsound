package com.luc4n3x.levyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeStreamCapabilityTest {
    private val adaptiveAudioWithoutToken =
        "https://rr3---sn-example.googlevideo.com/videoplayback?expire=1786904043&itag=251&" +
            "source=youtube&requiressl=yes&mime=audio%2Fwebm&gir=yes&clen=3168361&c=ANDROID_VR&lsig=abc"

    private val progressiveMuxed =
        "https://rr3---sn-example.googlevideo.com/videoplayback?expire=1786904043&itag=18&" +
            "source=youtube&requiressl=yes&mime=video%2Fmp4&ratebypass=yes&clen=15857332&c=ANDROID_VR&lsig=abc"

    private val adaptiveAudioWithToken = "$adaptiveAudioWithoutToken&pot=token-value"

    @Test
    fun `adaptive stream without ratebypass or proof of origin cannot serve a full track`() {
        assertFalse(YoutubeStreamCapability.servesCompleteStream(adaptiveAudioWithoutToken))
    }

    @Test
    fun `progressive ratebypass stream serves a full track`() {
        assertTrue(YoutubeStreamCapability.servesCompleteStream(progressiveMuxed))
    }

    @Test
    fun `adaptive stream with a proof of origin token serves a full track`() {
        assertTrue(YoutubeStreamCapability.servesCompleteStream(adaptiveAudioWithToken))
    }

    @Test
    fun `non googlevideo and manifest urls stay eligible`() {
        assertTrue(YoutubeStreamCapability.servesCompleteStream("https://example.com/audio.m4a"))
        assertTrue(
            YoutubeStreamCapability.servesCompleteStream(
                "https://rr3---sn-example.googlevideo.com/videoplayback/hls_playlist/index.m3u8?itag=251"
            )
        )
    }

    @Test
    fun `googlevideo lookalike host is not treated as googlevideo`() {
        assertTrue(
            YoutubeStreamCapability.servesCompleteStream(
                "https://notgooglevideo.com/videoplayback?itag=251&mime=audio%2Fwebm"
            )
        )
    }

    @Test
    fun `extensionless and mime signaled hls urls stay eligible`() {
        assertTrue(
            YoutubeStreamCapability.servesCompleteStream(
                "https://rr3---sn-example.googlevideo.com/videoplayback/hls_playlist?itag=251"
            )
        )
        assertTrue(
            YoutubeStreamCapability.servesCompleteStream(
                "https://rr3---sn-example.googlevideo.com/videoplayback?itag=251&mime=application%2Fx-mpegURL"
            )
        )
    }

    @Test
    fun `a blank url is never eligible`() {
        assertFalse(YoutubeStreamCapability.servesCompleteStream(""))
    }
}
