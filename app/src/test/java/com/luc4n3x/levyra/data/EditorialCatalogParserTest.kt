package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EditorialCatalogParserTest {
    @Test
    fun separatesAudioPlaybackFromOfficialVideoCounterpart() {
        val snapshot = EditorialCatalogParser.parse(
            catalog(
                youtubeMusic = """{
                    "audioVideoId": "Audio123456",
                    "audioConfidence": 99,
                    "videoId": "fcnDmrtj6Sk",
                    "videoConfidence": 97,
                    "confidence": 99
                }"""
            ),
            loadedAt = 0L
        )

        assertNotNull(snapshot)
        val track = snapshot!!.byMarket.getValue("IT").single()
        assertEquals("Audio123456", track.id)
        assertEquals("", track.videoUrl)
        assertEquals("fcnDmrtj6Sk", track.counterpartVideoId)
        assertEquals("MUSIC_VIDEO_TYPE_OMV", track.videoType)
        assertEquals(99, track.metadataConfidence)
    }

    @Test
    fun rejectsMalformedUnsupportedAndInvalidTimestampCatalogs() {
        assertNull(EditorialCatalogParser.parse("not-json", loadedAt = 0L))
        assertNull(EditorialCatalogParser.parse(catalog(schemaVersion = 2), loadedAt = 0L))
        assertNull(EditorialCatalogParser.parse(catalog(generatedAt = "not-a-time"), loadedAt = 0L))
    }

    @Test
    fun exposesFreshnessWithoutAcceptingStaleOrFarFutureSnapshots() {
        val stale = EditorialCatalogParser.parse(
            catalog(generatedAt = "2026-07-01T00:00:00Z"),
            loadedAt = 0L
        )!!
        val future = EditorialCatalogParser.parse(
            catalog(generatedAt = "2026-08-01T00:00:00Z"),
            loadedAt = 0L
        )!!
        val now = Instant.parse("2026-07-31T12:00:00Z").toEpochMilli()

        assertFalse(stale.isUsable(now))
        assertFalse(future.isUsable(now))
    }

    @Test
    fun refusesVideoOnlyOrLowConfidenceMappings() {
        val videoOnly = EditorialCatalogParser.parse(
            catalog(
                youtubeMusic = """{
                    "videoId": "fcnDmrtj6Sk",
                    "videoConfidence": 99,
                    "confidence": 99
                }"""
            ),
            loadedAt = 0L
        )!!.byMarket.getValue("IT").single()
        assertTrue(videoOnly.id.startsWith("chart-"))
        assertEquals("", videoOnly.counterpartVideoId)

        val lowConfidence = EditorialCatalogParser.parse(
            catalog(
                youtubeMusic = """{
                    "audioVideoId": "Audio123456",
                    "audioConfidence": 70,
                    "videoId": "fcnDmrtj6Sk",
                    "videoConfidence": 99
                }"""
            ),
            loadedAt = 0L
        )!!.byMarket.getValue("IT").single()
        assertTrue(lowConfidence.id.startsWith("chart-"))
        assertEquals("", lowConfidence.counterpartVideoId)
    }

    @Test
    fun rejectsInvalidYoutubeIdsAndKeepsLegacyChartFallback() {
        val invalid = EditorialCatalogParser.parse(
            catalog(
                youtubeMusic = """{
                    "audioVideoId": "too-short",
                    "audioConfidence": 99,
                    "videoId": "also-invalid",
                    "videoConfidence": 99
                }"""
            ),
            loadedAt = 0L
        )!!.byMarket.getValue("IT").single()
        assertTrue(invalid.id.startsWith("chart-"))
        assertEquals("", invalid.counterpartVideoId)
        assertEquals("Levyra Editorial", invalid.metadataProvider)

        val legacy = EditorialCatalogParser.parse(catalog(), loadedAt = 0L)!!
            .byMarket.getValue("IT").single()
        assertTrue(legacy.id.startsWith("chart-"))
        assertEquals("", legacy.counterpartVideoId)
    }

    @Test
    fun validatesArtworkHostAndScheme() {
        val rejected = EditorialCatalogParser.parse(
            catalog(artworkUrl = "http://evil.example/cover.jpg"),
            loadedAt = 0L
        )!!.byMarket.getValue("IT").single()
        assertEquals("", rejected.thumbnailUrl)

        val acceptedUrl = "https://i.scdn.co/image/ab67616d00001e0203cadf1b3fe324c1dc710ed4"
        val accepted = EditorialCatalogParser.parse(
            catalog(artworkUrl = acceptedUrl),
            loadedAt = 0L
        )!!.byMarket.getValue("IT").single()
        assertEquals(acceptedUrl, accepted.thumbnailUrl)
    }

    @Test
    fun selectsRequestedMarketAndUsesItalyOnlyForInvalidCountryCodes() {
        val body = catalog(
            collections = collection("IT", track(title = "Italia")) + "," +
                collection("US", track(title = "USA"))
        )
        val snapshot = EditorialCatalogParser.parse(body, loadedAt = 0L)!!

        assertEquals("USA", snapshot.tracks("US", 10).single().title)
        assertEquals("Italia", snapshot.tracks("invalid", 10).single().title)
        assertTrue(snapshot.tracks("FR", 10).isEmpty())
    }

    private fun catalog(
        schemaVersion: Int = 1,
        generatedAt: String = "2026-07-31T09:00:00Z",
        artworkUrl: String = "",
        youtubeMusic: String? = null,
        collections: String = collection(
            "IT",
            track(artworkUrl = artworkUrl, youtubeMusic = youtubeMusic)
        )
    ): String = """{
        "schemaVersion": $schemaVersion,
        "generatedAt": "$generatedAt",
        "collections": [$collections]
    }""".trimIndent()

    private fun collection(market: String, track: String): String = """{
        "kind": "chart",
        "market": "$market",
        "tracks": [$track]
    }""".trimIndent()

    private fun track(
        title: String = "Dai Dai",
        artist: String = "Shakira",
        artworkUrl: String = "",
        youtubeMusic: String? = null
    ): String {
        val quote = '"'
        val youtube = youtubeMusic
            ?.let { ",${quote}youtubeMusic${quote}:$it" }
            .orEmpty()
        val artwork = artworkUrl
            .takeIf(String::isNotBlank)
            ?.let { ",${quote}artworkUrl${quote}:${quote}$it${quote}" }
            .orEmpty()
        return """{
            "title": "$title",
            "artists": [{"name": "$artist"}],
            "album": {"name": "$title"},
            "durationMs": 223448$youtube$artwork
        }""".trimIndent()
    }
}
