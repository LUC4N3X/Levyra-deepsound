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
                  "url": "https://canvaz.scdn.co/upload/artist/video/1.cnvs.mp4"
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
                  "url": "https://canvaz.scdn.co/upload/artist/video/3.txt"
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
        val albumUrl = "https://canvaz.scdn.co/Album/dawn.m3u8"
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
                  "url": "https://canvaz.scdn.co/upload/artist/video/7.cnvs.mp4"
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
        assertTrue(candidates.none { it.url.contains("7.cnvs.mp4") })
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
                  "url": "https://canvaz.scdn.co/upload/artist/video/9.cnvs.mp4",
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
                  "url": "https://canvaz.scdn.co/upload/artist/video/1.cnvs.mp4",
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
    fun albumCanvasIgnoresTheIsrcOfTheListedTrack() {
        val albumUrl = "https://canvaz.scdn.co/Album/dawn.m3u8"
        val entries = parseCommunityCanvasCatalog(
            """
            {
              "items": [
                {
                  "song": "Track A",
                  "artist": "Exact Artist",
                  "album": "Exact Album",
                  "url": "$albumUrl",
                  "isrc": "USUM71703861"
                }
              ]
            }
            """.trimIndent()
        )
        val identity = MotionTrackIdentity(
            title = "Track B",
            artists = listOf("Exact Artist"),
            album = "Exact Album",
            durationMs = 195_000L,
            isrc = "GBAYE0601498",
            upc = "",
            year = "",
            trackId = "track-b",
            albumId = "exact-album"
        )

        val candidate = communityCanvasCandidates(identity, entries, nowMs = 1_000L)
            .single { it.scope == MotionArtworkScope.ALBUM }

        assertEquals(albumUrl, candidate.url)
        assertEquals("", candidate.identity.isrc)
        assertTrue(CanonicalTrackMatcher.match(identity, candidate).accepted)
    }

    @Test
    fun repeatedAlbumScopeEntriesCollapseIntoOneCandidate() {
        val albumUrl = "https://canvaz.scdn.co/Album/dawn.m3u8"
        val entries = parseCommunityCanvasCatalog(
            """
            {
              "items": [
                {
                  "song": "One",
                  "artist": "Exact Artist",
                  "album": "Exact Album",
                  "url": "$albumUrl",
                  "scope": "album"
                },
                {
                  "song": "Two",
                  "artist": "Exact Artist",
                  "album": "Exact Album",
                  "url": "$albumUrl",
                  "scope": "album"
                },
                {
                  "song": "Three",
                  "artist": "Exact Artist",
                  "album": "Exact Album",
                  "url": "$albumUrl",
                  "scope": "album"
                }
              ]
            }
            """.trimIndent()
        )
        val identity = MotionTrackIdentity(
            title = "Four",
            artists = listOf("Exact Artist"),
            album = "Exact Album",
            durationMs = 195_000L,
            isrc = "",
            upc = "",
            year = "",
            trackId = "four",
            albumId = "exact-album"
        )

        assertEquals(3, entries.size)
        assertEquals(1, communityCanvasCandidates(identity, entries, nowMs = 1_000L).count { it.url == albumUrl })
    }

    @Test
    fun fuzzyCatalogMatchingPreservesMetadataVariantCoverage() {
        val entries = listOf(
            CommunityCanvasEntry(
                song = "Flowers",
                artist = "Miley Cyrus",
                album = "Endless Summer Vacation",
                url = "https://canvaz.scdn.co/upload/artist/video/flowers.cnvs.mp4",
                scope = MotionArtworkScope.TRACK,
            )
        )
        val identity = MotionTrackIdentity(
            title = "Flowers",
            artists = listOf("Miley Cyrus"),
            album = "Endless Summer Vacation (Deluxe)",
            durationMs = 200_000L,
            isrc = "",
            upc = "",
            year = "",
            trackId = "flowers",
            albumId = "endless-summer-vacation",
        )

        assertTrue(communityCanvasCandidates(identity, entries, nowMs = 1_000L).isNotEmpty())
    }

    @Test
    fun mirrorCatalogVersionIsExposedForUsabilityChecks() {
        val document = parseCommunityCanvasDocument(
            """
            {
              "version": 1,
              "generatedAt": "2026-07-31T04:37:00Z",
              "items": [
                {
                  "song": "Exact Song",
                  "artist": "Exact Artist",
                  "album": "Exact Album",
                  "url": "https://canvaz.scdn.co/upload/artist/video/1.cnvs.mp4"
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(1, document.version)
        assertEquals(1, document.entries.size)
        assertEquals(0, parseCommunityCanvasDocument("{\"items\":[]}").version)
        assertEquals(0, parseCommunityCanvasDocument("{ truncated").version)
    }

    @Test
    fun conflictingIsrcStillRejectsTheEntry() {
        val entries = listOf(
            CommunityCanvasEntry(
                song = "Exact Song",
                artist = "Exact Artist",
                album = "Exact Album",
                url = "https://canvaz.scdn.co/upload/artist/video/1.cnvs.mp4",
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
        val url = "https://canvaz.scdn.co/Album/dawn.m3u8"
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
                url = "https://canvaz.scdn.co/upload/artist/video/1.m3u8",
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

    @Test
    fun indexBudgetReservesCatalogFallbackTime() {
        assertEquals(2_500L, communityCanvasIndexBudgetMs(MotionArtworkConfig().requestTimeoutMs))
        assertEquals(0L, communityCanvasIndexBudgetMs(2_500L))
        assertEquals(4_500L, communityCanvasIndexBudgetMs(12_000L))
    }
}
