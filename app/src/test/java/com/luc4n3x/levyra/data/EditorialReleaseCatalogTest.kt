package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorialReleaseCatalogTest {
    @Test
    fun releaseCollectionsStaySeparateFromCharts() {
        val body = """{
            "schemaVersion": 1,
            "generatedAt": "2026-08-06T12:00:00Z",
            "collections": [
                {
                    "id": "top-50-it",
                    "kind": "chart",
                    "market": "IT",
                    "tracks": [
                        {
                            "position": 1,
                            "id": "levyra-chart",
                            "title": "Chart Song",
                            "artists": [{"name": "Chart Artist"}],
                            "album": {"name": "Chart Album", "releaseDate": "2026-08-01"},
                            "durationMs": 180000,
                            "explicit": false
                        }
                    ]
                },
                {
                    "id": "new-releases-it",
                    "kind": "release",
                    "market": "IT",
                    "tracks": [
                        {
                            "position": 1,
                            "id": "levyra-release",
                            "title": "New Song",
                            "artists": [{"name": "New Artist"}],
                            "album": {"name": "New Album", "releaseDate": "2026-08-06"},
                            "durationMs": 190000,
                            "explicit": false,
                            "youtubeMusic": {
                                "albumBrowseId": "MPRErelease",
                                "artistBrowseId": "UCartist",
                                "confidence": 95,
                                "audioConfidence": 95
                            }
                        }
                    ]
                }
            ]
        }""".trimIndent()

        val snapshot = requireNotNull(EditorialCatalogParser.parse(body, loadedAt = 1L))

        assertEquals(listOf("Chart Song"), snapshot.tracks("it", 10).map { it.title })
        assertEquals(listOf("New Song"), snapshot.newReleases("it", 10).map { it.title })
        assertEquals("MPRErelease", snapshot.newReleases("it", 10).single().albumBrowseId)
    }
}
