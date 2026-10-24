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

    @Test
    fun nativeOnlyReleasesNeverBorrowAnotherMarket() {
        val body = """{
            "schemaVersion": 1,
            "generatedAt": "2026-10-24T04:42:26Z",
            "collections": [
                {
                    "id": "top-50-us",
                    "kind": "chart",
                    "market": "US",
                    "tracks": [
                        {
                            "position": 1,
                            "id": "levyra-us-chart",
                            "title": "US Chart Song",
                            "artists": [{"name": "Chart Artist"}],
                            "album": {"name": "Chart Album", "releaseDate": "2026-10-01"},
                            "durationMs": 180000,
                            "explicit": false
                        }
                    ]
                },
                {
                    "id": "new-releases-us",
                    "kind": "release",
                    "market": "US",
                    "tracks": [
                        {
                            "position": 1,
                            "id": "levyra-us-release",
                            "title": "US Song",
                            "artists": [{"name": "US Artist"}],
                            "album": {"name": "US Album", "releaseDate": "2026-10-23"},
                            "durationMs": 171104,
                            "explicit": false
                        }
                    ]
                }
            ]
        }""".trimIndent()

        val snapshot = requireNotNull(EditorialCatalogParser.parse(body, loadedAt = 1L))

        assertEquals(listOf("US Song"), snapshot.newReleases("ru", 10).map { it.title })
        assertEquals(emptyList<String>(), snapshot.newReleases("ru", 10, nativeOnly = true).map { it.title })
        assertEquals(listOf("US Song"), snapshot.newReleases("us", 10, nativeOnly = true).map { it.title })
    }
}
