package com.luc4n3x.levyra.feature.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRadioPlaylistTest {
    @Test
    fun plsPlaylistResolvesFirstPublicEntry() {
        val body = """
            [playlist]
            NumberOfEntries=2
            File1=http://127.0.0.1:8000/private
            Title1=Local relay
            File2=https://tamil.stream.example/live.aac
            Length2=-1
            Version=2
        """.trimIndent()

        assertEquals(
            "https://tamil.stream.example/live.aac",
            liveRadioPlaylistStreamUrl(body, "https://directory.example/station.pls")
        )
    }

    @Test
    fun m3uPlaylistSkipsCommentsAndResolvesRelativeEntries() {
        val body = "﻿#EXTM3U\n#EXTINF:-1,Telugu FM\n/stream/live.mp3\n"

        assertEquals(
            "https://telugu.example/stream/live.mp3",
            liveRadioPlaylistStreamUrl(body, "https://telugu.example/radio/listen.m3u")
        )
    }

    @Test
    fun playlistRejectsPrivateHostsSelfReferencesAndHlsManifests() {
        val base = "https://radio.example/live.m3u"

        assertNull(liveRadioPlaylistStreamUrl("http://192.168.1.4/live\nfile:///sdcard/a.mp3\n$base", base))
        assertNull(liveRadioPlaylistStreamUrl("#EXTM3U\n#EXT-X-TARGETDURATION:6\nchunk.aac", base))
        assertNull(liveRadioPlaylistStreamUrl("<html><body>Not found</body></html>", base))
    }

    @Test
    fun playlistDetectionUsesExtensionOrContentTypeButNeverHlsPaths() {
        assertTrue(isLiveRadioPlaylistResponse("https://radio.example/listen.PLS?sid=1", null))
        assertTrue(isLiveRadioPlaylistResponse("https://radio.example/listen", "audio/x-scpls; charset=UTF-8"))
        assertTrue(isLiveRadioPlaylistResponse("https://radio.example/listen", "audio/x-mpegurl"))
        assertFalse(isLiveRadioPlaylistResponse("https://radio.example/live.m3u8", "audio/x-mpegurl"))
        assertFalse(isLiveRadioPlaylistResponse("https://radio.example/live", "audio/mpeg"))
        assertFalse(isLiveRadioPlaylistResponse("https://radio.example/live", null))
        assertFalse(isResolvableRadioPlaylistUrl("https://radio.example/live.asx"))
    }
}
