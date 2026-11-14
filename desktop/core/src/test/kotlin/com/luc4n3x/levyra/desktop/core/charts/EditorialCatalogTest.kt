package com.luc4n3x.levyra.desktop.core.charts

import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class EditorialCatalogTest {
    private val now = Instant.parse("2026-11-14T12:00:00Z").toEpochMilli()
    private fun recording(confidence: Int = 90, artwork: String = "https://i.scdn.co/image/cover") = JSONObject()
        .put("id", "recording-id").put("title", "Recording").put("artists", JSONArray().put(JSONObject().put("name", "Artist")))
        .put("album", JSONObject().put("name", "Album")).put("durationMs", 200_000)
        .put("explicit", false).put("isrc", "TEST000001").put("artworkUrl", artwork)
        .put("youtubeMusic", JSONObject().put("audioVideoId", "abcdefghijk").put("audioConfidence", confidence))
    private fun catalog(recording: JSONObject = recording(), generated: Long = now): String {
        val collections = JSONArray()
        for (kind in listOf("chart", "release", "genre")) {
            collections.put(JSONObject().put("kind", kind).put("market", "IN").put("title", "Genre")
                .put("youtubeParams", "genre-params").put("tracks", JSONArray().put(recording)))
        }
        return JSONObject().put("schemaVersion", 1).put("generatedAt", Instant.ofEpochMilli(generated).toString())
            .put("collections", collections).toString()
    }

    @Test fun samePublishedChartReleasesAndGenresPreserveRecordingIdentity() {
        val feed = EditorialCatalog.parse(catalog(), now)!!
        val track = feed.charts.getValue("IN").single()
        assertEquals("recording-id", track.id)
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", track.videoUrl)
        assertEquals(false, track.explicit)
        assertEquals("TEST000001", track.isrc)
        assertEquals(track, feed.releases.getValue("IN").single())
        assertEquals(track, feed.genres.single().tracks.single())
    }

    @Test fun uncertainMappingAndUntrustedArtworkRequireSafeFallbacks() {
        val track = EditorialCatalog.parse(catalog(recording(81, "https://127.0.0.1/private")), now)!!.charts.getValue("IN").single()
        assertEquals("", track.videoUrl)
        assertEquals("", track.artworkUrl)
    }

    @Test fun staleFutureAndUnsupportedCatalogsAreRejected() {
        assertNull(EditorialCatalog.parse(catalog(generated = now - 172_800_001), now))
        assertNull(EditorialCatalog.parse(catalog(generated = now + 600_001), now))
        assertNull(EditorialCatalog.parse(catalog().replace("\"schemaVersion\":1", "\"schemaVersion\":2"), now))
        assertNull(EditorialCatalog.parse("invalid", now))
    }
}
