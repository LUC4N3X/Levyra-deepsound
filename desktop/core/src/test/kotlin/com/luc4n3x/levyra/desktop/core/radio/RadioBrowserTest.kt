package com.luc4n3x.levyra.desktop.core.radio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioBrowserTest {

    @Test
    fun parsesStationsAndRejectsUnsafeEntries() {
        val body = """[
            {"stationuuid":"a","name":"Rai Radio 1","url_resolved":"https://x/live.mp3","favicon":"https://x/i.png","country":"Italy","tags":"news,talk","codec":"MP3","bitrate":256},
            {"stationuuid":"b","name":"No stream","url_resolved":"ftp://x"},
            {"stationuuid":"c","name":"Insecure icon","url":"http://y/live","favicon":"http://y/i.png"},
            {"stationuuid":"a","name":"Dup","url_resolved":"https://x/2"}
        ]"""
        val stations = RadioBrowser.parse(body)
        assertEquals(listOf("a", "c"), stations.map { it.uuid })
        assertEquals("Italy · news · MP3 / 256 kbps", stations.first().details)
        assertEquals("", stations[1].favicon)
        val track = stations.first().toTrack()
        assertEquals("radio:a", track.id)
        assertTrue(track.live)
    }

    @Test
    fun invalidJsonYieldsEmptyList() {
        assertTrue(RadioBrowser.parse("not json").isEmpty())
    }
}
