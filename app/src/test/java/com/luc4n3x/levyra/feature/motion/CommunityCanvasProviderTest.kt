package com.luc4n3x.levyra.feature.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityCanvasProviderTest {
    @Test
    fun parserKeepsOnlyApprovedHttpsMedia() {
        val entries = parseCommunityCanvasCatalog(
            """
            {
              "items": [
                {
                  "song": "Exact Song",
                  "artist": "Exact Artist",
                  "album": "Exact Album",
                  "url": "https://vivimusicanvas.mkmdevilmi.workers.dev/Song/1.mp4"
                },
                {
                  "song": "Bad Host",
                  "artist": "Exact Artist",
                  "album": "Exact Album",
                  "url": "https://example.com/Song/2.mp4"
                },
                {
                  "song": "Bad Type",
                  "artist": "Exact Artist",
                  "album": "Exact Album",
                  "url": "https://vivimusicanvas.mkmdevilmi.workers.dev/Song/3.txt"
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(1, entries.size)
        assertEquals("Exact Song", entries.single().song)
    }

    @Test
    fun repeatedAlbumCanvasIsInferredAsAlbumScope() {
        val url = "https://vivimusicanvas.mkmdevilmi.workers.dev/Album/dawn.m3u8"
        val entries = listOf(
            CommunityCanvasEntry("Track One", "Exact Artist", "Exact Album", url, MotionArtworkScope.TRACK),
            CommunityCanvasEntry("Track Two", "Exact Artist", "Exact Album", url, MotionArtworkScope.TRACK)
        )
        val identity = MotionTrackIdentity(
            title = "Track Three",
            artists = listOf("Exact Artist"),
            album = "Exact Album",
            durationMs = 180_000L,
            isrc = "",
            upc = "",
            year = "",
            trackId = "track-three",
            albumId = "album-one"
        )

        val candidates = communityCanvasCandidates(identity, entries, nowMs = 1_000L)

        assertTrue(candidates.any { it.scope == MotionArtworkScope.ALBUM && it.url == url })
    }

    @Test
    fun exactTrackCanvasProducesPlayableMimeType() {
        val entries = listOf(
            CommunityCanvasEntry(
                song = "Exact Song",
                artist = "Exact Artist",
                album = "Exact Album",
                url = "https://vivimusicanvas.mkmdevilmi.workers.dev/Song/1.m3u8",
                scope = MotionArtworkScope.TRACK
            )
        )
        val identity = MotionTrackIdentity(
            title = "Exact Song",
            artists = listOf("Exact Artist"),
            album = "Exact Album",
            durationMs = 210_000L,
            isrc = "",
            upc = "",
            year = "",
            trackId = "exact-song",
            albumId = "exact-album"
        )

        val candidate = communityCanvasCandidates(identity, entries, nowMs = 1_000L).single()

        assertEquals(MotionArtworkScope.TRACK, candidate.scope)
        assertEquals("application/x-mpegURL", candidate.mimeType)
    }
}
