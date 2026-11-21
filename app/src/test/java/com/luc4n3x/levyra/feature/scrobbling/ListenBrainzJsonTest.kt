package com.luc4n3x.levyra.feature.scrobbling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenBrainzJsonTest {
    @Test
    fun readsCreatedForPlaylistIdsAndTitles() {
        val json = """{"playlists":[{"playlist":{"identifier":"https://listenbrainz.org/playlist/c5183e6e-9caa-45aa-9921-72a1c517afd2","title":"Weekly Jams for luca","track":[]}},{"playlist":{"identifier":"","title":"broken"}}]}"""
        assertEquals(
            listOf(ListenBrainzPlaylist("c5183e6e-9caa-45aa-9921-72a1c517afd2", "Weekly Jams for luca", ListenBrainzPlaylistKind.CreatedFor)),
            ListenBrainzJson.playlists(json, ListenBrainzPlaylistKind.CreatedFor)
        )
    }

    @Test
    fun readsPlaylistTracksWithRecordingMbid() {
        val json = """{"playlist":{"track":[{"title":"Lido","creator":"Bicep","identifier":["https://musicbrainz.org/recording/418bce6c-1195-4cf2-b5c0-4cf163acbd46"]},{"title":"","creator":"x"}]}}"""
        assertEquals(
            listOf(ListenBrainzTrack("Lido", "Bicep", "418bce6c-1195-4cf2-b5c0-4cf163acbd46")),
            ListenBrainzJson.playlistTracks(json)
        )
    }

    @Test
    fun readsTopRecordingsFromStats() {
        val json = """{"payload":{"recordings":[{"track_name":"Buena","artist_name":"Morphine","recording_mbid":"8c502831-3eab-43c0-afc1-b340b53b6a58"}]}}"""
        assertEquals(listOf(ListenBrainzTrack("Buena", "Morphine", "8c502831-3eab-43c0-afc1-b340b53b6a58")), ListenBrainzJson.topRecordings(json))
    }

    @Test
    fun createPayloadIsPrivateJspfWithRecordingIdentifiers() {
        val payload = ListenBrainzJson.createPayload("Mix", listOf("abc")).getJSONObject("playlist")
        assertEquals("Mix", payload.getString("title"))
        assertEquals("https://musicbrainz.org/recording/abc", payload.getJSONArray("track").getJSONObject(0).getString("identifier"))
        assertEquals(false, payload.getJSONObject("extension").getJSONObject("https://musicbrainz.org/doc/jspf#playlist").getBoolean("public"))
    }

    @Test
    fun xspfEscapesTitlesForTheImportPipeline() {
        val xspf = ListenBrainzJson.xspf("Rock & <Roll>", listOf(ListenBrainzTrack("A \"B\"", "C & D")))
        assertTrue(xspf.contains("<title>Rock &amp; &lt;Roll&gt;</title>"))
        assertTrue(xspf.contains("<track><title>A &quot;B&quot;</title><creator>C &amp; D</creator></track>"))
        assertTrue(xspf.contains("xspf.org"))
    }
}
