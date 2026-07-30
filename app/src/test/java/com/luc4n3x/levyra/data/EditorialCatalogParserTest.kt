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
    fun parsesCountryChartWithPublishedCoverAndWithoutFakeIsrc() {
        val snapshot = EditorialCatalogParser.parse(validCatalog, loadedAt = NOW)

        assertNotNull(snapshot)
        val italy = snapshot!!.byMarket.getValue("IT")
        val track = italy.single()
        assertEquals("Prima Canzone", track.title)
        assertEquals("Artista Uno, Artista Due", track.artist)
        assertEquals("Album Uno", track.album)
        assertEquals(181_000L, track.durationMs)
        assertTrue(track.isrc.isBlank())
        assertEquals("https://i.scdn.co/image/ab67616d0000cover", track.thumbnailUrl)
        assertEquals("https://i.scdn.co/image/ab67616d0000cover", track.largeThumbnailUrl)
        assertEquals("2026", track.year)
        assertTrue(track.explicit)
        assertEquals("Levyra Editorial", track.source)

        val legacy = ChartFeedParser.modern(legacyFeed, limit = 1).single()
        assertEquals(legacy.id, track.id)
    }

    @Test
    fun onlyConfiguredTwoLetterMarketsAreExposed() {
        val snapshot = EditorialCatalogParser.parse(validCatalog, loadedAt = NOW)!!

        assertEquals(setOf("IT"), snapshot.byMarket.keys)
        assertFalse(snapshot.byMarket.containsKey("GLOBAL"))
    }

    @Test
    fun ignoresCoverArtworkThatIsNotHttpsOnTheSourceImageCdn() {
        // The catalog arrives from a remote branch, so a tampered or unexpected payload must never
        // reach the image loader. A rejected cover degrades to on-device lookup, it does not drop the row.
        val rejected = listOf(
            "http://i.scdn.co/image/plain-http",
            "https://evil.example/cover.jpg",
            "https://i.scdn.co.evil.example/cover.jpg",
            "https://user:pass@i.scdn.co/image/credentials",
            "https://i.scdn.co:8443/image/custom-port",
            "not-a-url",
            "",
        )
        rejected.forEach { candidate ->
            val snapshot = EditorialCatalogParser.parse(
                validCatalog.replace("https://i.scdn.co/image/ab67616d0000cover", candidate),
                loadedAt = NOW,
            )
            val track = snapshot!!.byMarket.getValue("IT").single()
            assertTrue("Expected $candidate to be rejected", track.thumbnailUrl.isBlank())
            assertTrue("Expected $candidate to be rejected", track.largeThumbnailUrl.isBlank())
            assertEquals("Prima Canzone", track.title)
        }

        val accepted = EditorialCatalogParser.parse(
            validCatalog.replace("https://i.scdn.co/image/ab67616d0000cover", "https://mosaic.scdn.co/640/abc"),
            loadedAt = NOW,
        )!!
        assertEquals("https://mosaic.scdn.co/640/abc", accepted.byMarket.getValue("IT").single().thumbnailUrl)
    }

    @Test
    fun rejectsWrongSchemaMissingTimestampAndStaleCatalogs() {
        assertNull(EditorialCatalogParser.parse("""{"schemaVersion":2,"collections":[]}""", NOW))
        assertNull(EditorialCatalogParser.parse("""{"schemaVersion":1,"collections":[]}""", NOW))

        val stale = EditorialCatalogParser.parse(
            validCatalog.replace("2026-07-29T18:00:00Z", "2026-07-26T18:00:00Z"),
            loadedAt = NOW,
        )
        assertNotNull(stale)
        assertFalse(stale!!.isUsable(NOW))
    }

    private companion object {
        val NOW: Long = Instant.parse("2026-07-29T20:00:00Z").toEpochMilli()
    }

    private val validCatalog = """
        {
          "schemaVersion": 1,
          "generatedAt": "2026-07-29T18:00:00Z",
          "collections": [
            {
              "id": "top-50-global",
              "kind": "chart",
              "market": "GLOBAL",
              "tracks": [{"position":1,"id":"global","title":"Global","artists":[{"name":"Artist"}]}]
            },
            {
              "id": "top-50-it",
              "kind": "chart",
              "market": "IT",
              "tracks": [
                {
                  "position": 1,
                  "id": "source-track-id",
                  "title": "Prima Canzone",
                  "artists": [
                    {"name": "Artista Uno"},
                    {"name": "Artista Due"}
                  ],
                  "album": {
                    "name": "Album Uno",
                    "releaseDate": "2026-07-01"
                  },
                  "durationMs": 181000,
                  "explicit": true,
                  "artworkUrl": "https://i.scdn.co/image/ab67616d0000cover"
                }
              ]
            }
          ]
        }
    """.trimIndent()

    private val legacyFeed = """
        {
          "feed": {
            "results": [
              {
                "name": "Prima Canzone",
                "artistName": "Artista Uno, Artista Due",
                "artworkUrl100": "https://image.example/100x100bb.jpg"
              }
            ]
          }
        }
    """.trimIndent()
}
