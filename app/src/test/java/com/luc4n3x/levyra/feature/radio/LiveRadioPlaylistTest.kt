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
    fun realSomaFmPlaylistResolvesItsFirstServer() {
        val body = """
            [playlist]
            numberofentries=3
            File1=https://ice6.somafm.com/groovesalad-128-mp3
            Title1=SomaFM: Groove Salad (#1): A nicely chilled plate of ambient/downtempo beats and grooves.
            Length1=-1
            File2=https://ice2.somafm.com/groovesalad-128-mp3
            Title2=SomaFM: Groove Salad (#2): A nicely chilled plate of ambient/downtempo beats and grooves.
            Length2=-1
            Version=2
        """.trimIndent()

        assertEquals(
            "https://ice6.somafm.com/groovesalad-128-mp3",
            liveRadioPlaylistStreamUrl(body, "https://somafm.com/groovesalad.pls")
        )
        assertEquals(
            "https://c14.radioboss.fm:8033/stream",
            liveRadioPlaylistStreamUrl("https://c14.radioboss.fm:8033/stream\n", "https://c14.radioboss.fm/playlist/33/stream.m3u")
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
        assertFalse(isLiveRadioPlaylistResponse("http://wma08.fluidstream.net:3612/;listen.pls", "audio/mpeg"))
        assertFalse(isLiveRadioPlaylistResponse("http://www.streamingsoundtracks.com/modules/Listen/MP3-hi.pls", "audio/aacp"))
        assertTrue(isLiveRadioPlaylistResponse("https://somafm.com/groovesalad.pls", "audio/x-scpls"))
        assertTrue(isLiveRadioPlaylistResponse("https://c14.radioboss.fm/playlist/33/stream.m3u", "audio/x-mpegurl"))
        assertTrue(isLiveRadioPlaylistResponse("https://somafm.com/groovesalad.pls", "text/plain"))
    }
}
