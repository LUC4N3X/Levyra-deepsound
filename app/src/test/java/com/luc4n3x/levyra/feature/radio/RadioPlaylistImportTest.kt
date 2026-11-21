package com.luc4n3x.levyra.feature.radio

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioPlaylistImportTest {
    @Test
    fun parsesExtendedM3uNamesLogosAndPlainUrls() {
        val text = "﻿#EXTM3U\r\n" +
            "#EXTINF:-1 tvg-logo=\"https://radio.example/a.png\" group-title=\"Pop\",Radio Uno\r\n" +
            "https://radio.example/uno\r\n" +
            "#EXTINF:-1,\r\n" +
            "http://relay.example:8000/due.mp3\r\n" +
            "not a url\r\n" +
            "https://radio.example/uno\r\n"
        assertEquals(
            listOf(
                RadioPlaylistEntry("Radio Uno", "https://radio.example/uno", "https://radio.example/a.png"),
                RadioPlaylistEntry("relay.example", "http://relay.example:8000/due.mp3", ""),
                RadioPlaylistEntry("radio.example", "https://radio.example/uno", "")
            ),
            parseRadioPlaylist(text)
        )
    }

    @Test
    fun hlsMediaPlaylistIsAStreamNotAStationList() {
        val hls = "#EXTM3U\n#EXT-X-TARGETDURATION:10\n#EXTINF:10,\nsegment1.ts\n"
        assertTrue(parseRadioPlaylist(hls).isEmpty())
    }

    @Test
    fun importSkipsStationsAlreadySavedAndStopsAtTheLimit() = runBlocking {
        val store = RadioStore(MemoryRadioPreferences())
        store.saveCustom(customRadioStation("Saved", "https://radio.example/saved", ""))
        val entries = listOf(
            RadioPlaylistEntry("Saved again", "https://radio.example/saved", ""),
            RadioPlaylistEntry("New", "https://radio.example/new", ""),
            RadioPlaylistEntry("New twice", "https://radio.example/new", "")
        )
        assertEquals(RadioImportResult(added = 1, skipped = 2), store.importCustom(entries))
        assertEquals(listOf("Saved", "New"), store.customStations().map { it.name })
        val flood = (1..300).map { RadioPlaylistEntry("S$it", "https://radio.example/s$it", "") }
        val result = store.importCustom(flood)
        assertEquals(200, store.customStations().size)
        assertEquals(300, result.added + result.skipped)
    }
}
