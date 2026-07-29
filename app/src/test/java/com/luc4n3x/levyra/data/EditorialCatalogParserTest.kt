package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorialCatalogParserTest {

    @Test
    fun parsesCountryChartWithStableIdentityAndMetadata() {
        val snapshot = EditorialCatalogParser.parse(validCatalog, loadedAt = 123L)

        assertNotNull(snapshot)
        val italy = snapshot!!.byMarket.getValue("IT")
        assertEquals(1, italy.size)
        val track = italy.single()
        assertEquals("Prima Canzone", track.title)
        assertEquals("Artista Uno, Artista Due", track.artist)
        assertEquals("Album Uno", track.album)
        assertEquals(181_000L, track.durationMs)
        assertEquals("ITABC2600001", track.isrc)
        assertEquals("2026", track.year)
        assertTrue(track.explicit)
        assertEquals("Levyra Editorial", track.source)
        assertEquals("Levyra Editorial", track.metadataProvider)
        assertEquals("https://image.example/album.jpg", track.thumbnailUrl)

        val legacy = ChartFeedParser.modern(legacyFeed, limit = 1).single()
        assertEquals(legacy.id, track.id)
    }

    @Test
    fun ignoresUnsupportedMarketsAndKeepsMissingCountriesAvailableForFallback() {
        val snapshot = EditorialCatalogParser.parse(validCatalog, loadedAt = 123L)!!

        assertFalse(snapshot.byMarket.containsKey("GLOBAL"))
        assertFalse(snapshot.byMarket.containsKey("CN"))
        assertFalse(snapshot.byMarket.containsKey("RU"))
    }

    @Test
    fun rejectsWrongSchemaAndNonHttpsArtwork() {
        assertNull(EditorialCatalogParser.parse("""{"schemaVersion":2,"collections":[]}""", 0L))

        val snapshot = EditorialCatalogParser.parse(catalogWithUnsafeArtwork, loadedAt = 1L)!!
        assertTrue(snapshot.byMarket.getValue("US").single().thumbnailUrl.isBlank())
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
              "tracks": []
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
                    {"id": "artist1", "name": "Artista Uno"},
                    {"id": "artist2", "name": "Artista Due"}
                  ],
                  "album": {
                    "id": "album1",
                    "name": "Album Uno",
                    "releaseDate": "2026-07-01",
                    "artworkUrl": "https://image.example/album.jpg"
                  },
                  "durationMs": 181000,
                  "explicit": true,
                  "isrc": "ITABC2600001"
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

    private val catalogWithUnsafeArtwork = """
        {
          "schemaVersion": 1,
          "collections": [
            {
              "id": "top-50-us",
              "kind": "chart",
              "market": "US",
              "tracks": [
                {
                  "position": 1,
                  "id": "track1",
                  "title": "Unsafe Image",
                  "artists": [{"name": "Artist"}],
                  "album": {
                    "name": "Album",
                    "artworkUrl": "http://image.example/album.jpg"
                  },
                  "artworkUrl": "javascript:alert(1)",
                  "durationMs": 180000
                }
              ]
            }
          ]
        }
    """.trimIndent()
}
