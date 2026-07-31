package com.luc4n3x.levyra.feature.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun albumDirectoryEntryAlsoCoversTheRestOfTheAlbum() {
        val albumUrl = "https://vivimusicanvas.mkmdevilmi.workers.dev/Album/dawn.m3u8"
        val entries = parseCommunityCanvasCatalog(
            """
            {
              "items": [
                {
                  "song": "Listed Song",
                  "artist": "Exact Artist",
                  "album": "Exact Album",
                  "url": "$albumUrl"
                },
                {
                  "song": "Other Song",
                  "artist": "Exact Artist",
                  "album": "Other Album",
                  "url": "https://vivimusicanvas.mkmdevilmi.workers.dev/Song/7.mp4"
                }
              ]
            }
            """.trimIndent()
        )
        val identity = MotionTrackIdentity(
            title = "Unlisted Song",
            artists = listOf("Exact Artist"),
            album = "Exact Album",
            durationMs = 180_000L,
            isrc = "",
            upc = "",
            year = "",
            trackId = "unlisted-song",
            albumId = "exact-album"
        )

        assertEquals(MotionArtworkScope.TRACK, entries.single { it.song == "Listed Song" }.scope)
        assertEquals(
            MotionArtworkScope.ALBUM,
            entries.single { it.song == "Listed Song" }.pathScope
        )

        val candidates = communityCanvasCandidates(identity, entries, nowMs = 1_000L)

        assertTrue(candidates.any { it.scope == MotionArtworkScope.ALBUM && it.url == albumUrl })
        assertTrue(candidates.none { it.url.contains("/Song/7.mp4") })
    }

    @Test
    fun declaredScopeWinsOverThePathDirectory() {
        val entries = parseCommunityCanvasCatalog(
            """
            {
              "version": 1,
              "generatedAt": "2026-07-31T00:00:00Z",
              "items": [
                {
                  "song": "Album Canvas",
                  "artist": "Exact Artist",
                  "album": "Exact Album",
                  "url": "https://vivimusicanvas.mkmdevilmi.workers.dev/Song/9.mp4",
                  "scope": "album",
                  "isrc": "USUM71703861",
                  "width": 1080,
                  "height": 1920
                }
              ]
            }
            """.trimIndent()
        )

        val entry = entries.single()
        assertEquals(MotionArtworkScope.ALBUM, entry.scope)
        assertNull(entry.pathScope)
        assertEquals("USUM71703861", entry.isrc)
        assertEquals(1080, entry.width)
        assertEquals(1920, entry.height)
    }

    @Test
    fun malformedIsrcIsDiscardedInsteadOfBlockingTheMatch() {
        val entries = parseCommunityCanvasCatalog(
            """
            {
              "items": [
                {
                  "song": "Exact Song",
                  "artist": "Exact Artist",
                  "album": "Exact Album",
                  "url": "https://vivimusicanvas.mkmdevilmi.workers.dev/Song/1.mp4",
                  "isrc": "not-an-isrc"
                }
              ]
            }
            """.trimIndent()
        )
        val identity = MotionTrackIdentity(
            title = "Exact Song",
            artists = listOf("Exact Artist"),
            album = "Exact Album",
            durationMs = 210_000L,
            isrc = "USUM71703861",
            upc = "",
            year = "",
            trackId = "exact-song",
            albumId = "exact-album"
        )

        assertEquals("", entries.single().isrc)
        assertNotNull(communityCanvasCandidates(identity, entries, nowMs = 1_000L).firstOrNull())
    }

    @Test
    fun conflictingIsrcStillRejectsTheEntry() {
        val entries = listOf(
            CommunityCanvasEntry(
                song = "Exact Song",
                artist = "Exact Artist",
                album = "Exact Album",
                url = "https://vivimusicanvas.mkmdevilmi.workers.dev/Song/1.mp4",
                scope = MotionArtworkScope.TRACK,
                isrc = "GBAYE0601498"
            )
        )
        val identity = MotionTrackIdentity(
            title = "Exact Song",
            artists = listOf("Exact Artist"),
            album = "Exact Album",
            durationMs = 210_000L,
            isrc = "USUM71703861",
            upc = "",
            year = "",
            trackId = "exact-song",
            albumId = "exact-album"
        )

        assertNull(communityCanvasCandidates(identity, entries, nowMs = 1_000L).firstOrNull())
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
