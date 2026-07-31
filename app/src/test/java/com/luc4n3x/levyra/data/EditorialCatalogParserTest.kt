package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EditorialCatalogParserTest {
    @Test
    fun separatesAudioPlaybackFromOfficialVideoCounterpart() {
        val body = """
            {
              "schemaVersion": 1,
              "generatedAt": "2026-07-31T09:00:00Z",
              "collections": [
                {
                  "kind": "chart",
                  "market": "IT",
                  "tracks": [
                    {
                      "title": "Dai Dai",
                      "artists": [
                        {"name": "Shakira"},
                        {"name": "Burna Boy"}
                      ],
                      "album": {"name": "Dai Dai"},
                      "durationMs": 223448,
                      "youtubeMusic": {
                        "audioVideoId": "Audio123456",
                        "audioConfidence": 99,
                        "videoId": "fcnDmrtj6Sk",
                        "videoConfidence": 97,
                        "confidence": 99
                      }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val snapshot = EditorialCatalogParser.parse(body, loadedAt = 0L)
        assertNotNull(snapshot)
        val track = snapshot!!.byMarket.getValue("IT").single()

        assertEquals("Audio123456", track.id)
        assertEquals("", track.videoUrl)
        assertEquals("fcnDmrtj6Sk", track.counterpartVideoId)
        assertEquals("MUSIC_VIDEO_TYPE_OMV", track.videoType)
        assertEquals(99, track.metadataConfidence)
    }
}
