package com.luc4n3x.levyra.feature.motion

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TidalVideoCoverProviderTest {
    @Test
    fun trackQueriesPreferAlbumArtistTitleThenUseBoundedFallback() {
        val queries = tidalSearchQueries(identity(album = "After Hours"), "TRACKS")

        assertEquals(listOf("After Hours The Weeknd Blinding Lights", "Blinding Lights The Weeknd"), queries)
    }

    @Test
    fun rejectedTrackOrReleaseCandidatesKeepBothVariantsAndAlbumFallbackAvailable() {
        val requested = identity(album = "After Hours")
        val wrongTrack = albumCandidate(title = "Save Your Tears", album = "After Hours")
        val wrongRelease = albumCandidate(album = "Dawn FM")

        assertEquals(2, tidalSearchQueries(requested, "TRACKS").size)
        assertTrue(shouldSearchTidalAlbumsAfterTracks(requested, listOf(wrongTrack), DEFAULT_MOTION_ARTWORK_MINIMUM_CONFIDENCE))
        assertTrue(shouldSearchTidalAlbumsAfterTracks(requested, listOf(wrongRelease), DEFAULT_MOTION_ARTWORK_MINIMUM_CONFIDENCE))
        assertFalse(shouldSearchTidalAlbumsAfterTracks(requested, listOf(albumCandidate()), DEFAULT_MOTION_ARTWORK_MINIMUM_CONFIDENCE))
    }

    @Test
    fun structurallyAcceptedCandidateBelowConfiguredThresholdKeepsAlbumFallbackAvailable() {
        val requested = identity(album = "After Hours The Original Motion Picture Soundtrack")
        val candidate = albumCandidate(album = "After Hours The Original Motion Picture Soundtrack Official").copy(
            identity = identity("After Hours The Original Motion Picture Soundtrack Official").copy(
                title = "",
                durationMs = 0L
            )
        )

        val match = CanonicalTrackMatcher.match(requested, candidate)

        assertTrue(match.accepted)
        assertTrue(match.score in 70..83)
        assertTrue(shouldSearchTidalAlbumsAfterTracks(requested, listOf(candidate), 84))
        assertFalse(shouldSearchTidalAlbumsAfterTracks(requested, listOf(candidate), 70))
    }

    @Test
    fun albumQueriesRemainBoundedAndIgnoreBlankValues() {
        assertEquals(listOf("After Hours The Weeknd", "The Weeknd After Hours"), tidalSearchQueries(identity(), "ALBUMS"))
        assertTrue(tidalSearchQueries(identity(album = ""), "ALBUMS").isNotEmpty())
    }

    @Test
    fun rawPrefilterNoLongerAcceptsAnyResultJustBecauseItHasAnAlbumId() {
        val requested = identity()

        assertFalse(tidalRawTrackMetadataCouldMatch(requested, "TRACKS", "Blinding Lights", listOf("The Weeknd"), "Dawn FM", ""))
        assertTrue(tidalRawTrackMetadataCouldMatch(requested, "TRACKS", "Blinding Lights", listOf("The Weeknd"), "After Hours", ""))
        assertFalse(tidalRawTrackMetadataCouldMatch(requested, "ALBUMS", "Dawn FM", listOf("The Weeknd"), "Dawn FM", ""))
        assertTrue(tidalRawTrackMetadataCouldMatch(requested, "ALBUMS", "After Hours", listOf("The Weeknd"), "After Hours", ""))
    }

    @Test
    fun hydrationCeilingReservesCapacityForLaterQueriesAndAlbumFallback() {
        assertEquals(1, tidalHydrationCeiling(laterQueries = 1, albumFallbackPending = true))
        assertEquals(2, tidalHydrationCeiling(laterQueries = 0, albumFallbackPending = true))
        assertEquals(2, tidalHydrationCeiling(laterQueries = 1, albumFallbackPending = false))
        assertEquals(3, tidalHydrationCeiling(laterQueries = 0, albumFallbackPending = false))
    }

    @Test
    fun albumHydrationIsDeduplicatedAndBounded() = runBlocking {
        val lookup = TidalMotionLookup()
        val fetched = mutableListOf<String>()
        val fetch: suspend (String) -> TidalAlbumMotion = { albumId ->
            fetched += albumId
            TidalAlbumMotion(albumId, listOf("The Weeknd"), VIDEO_COVER, "", "")
        }

        assertNotNull(lookup.hydrate("first", 3, fetch))
        assertNotNull(lookup.hydrate("first", 3, fetch))
        assertNotNull(lookup.hydrate("second", 3, fetch))
        assertEquals(null, lookup.hydrate("third", 2, fetch))
        assertNotNull(lookup.hydrate("third", 3, fetch))
        assertEquals(null, lookup.hydrate("fourth", 10, fetch))
        assertEquals(listOf("first", "second", "third"), fetched)
    }

    @Test
    fun rankedHydrationReachesCorrectTrackBehindWeakerResults() = runBlocking {
        val weak = (1..4).map { index -> trackItem("weak-$index", "Lights Out", "After Dark $index") }
        val transport = FakeTidalTransport(
            search = { _, type -> if (type == "TRACKS") tracksPage(weak + trackItem("correct", "Blinding Lights", "After Hours")) else tracksPage(emptyList()) },
            album = { albumId -> albumDetails(if (albumId == "correct") "After Hours" else "After Dark") }
        )

        val result = provider(transport).find(identity())

        assertEquals("correct", (result as MotionArtworkProviderResult.Found).candidates.single().identity.albumId)
        assertEquals(listOf("correct"), transport.albums)
    }

    @Test
    fun weakTrackResultsCannotStarveTheAlbumFallback() = runBlocking {
        val weak = (1..4).map { index -> trackItem("weak-$index", "Lights Out", "After Dark $index") }
        val transport = FakeTidalTransport(
            search = { _, type ->
                if (type == "TRACKS") tracksPage(weak) else albumsPage(listOf(albumItem("album-ok", "After Hours")))
            },
            album = { albumId -> albumDetails(if (albumId == "album-ok") "After Hours" else "After Dark") }
        )

        val result = provider(transport).find(identity())

        assertEquals("album-ok", (result as MotionArtworkProviderResult.Found).candidates.single().identity.albumId)
        assertTrue(transport.albums.size <= 3)
        assertEquals("album-ok", transport.albums.last())
    }

    @Test
    fun matchingFirstCandidateDoesNotSuppressLaterTidalAlternativeForVerifier() = runBlocking {
        val transport = FakeTidalTransport(
            search = { query, type ->
                when {
                    type != "TRACKS" -> albumsPage(emptyList())
                    query.startsWith("After Hours") ->
                        tracksPage(listOf(trackItem("stale", "Blinding Lights", "After Hours", VIDEO_COVER)))
                    else ->
                        tracksPage(listOf(trackItem("valid", "Blinding Lights", "After Hours", ALT_VIDEO_COVER)))
                }
            },
            album = { error("hydration not expected") }
        )

        val result = provider(transport).find(identity())
        val found = result as MotionArtworkProviderResult.Found

        assertEquals(listOf("stale", "valid"), found.candidates.map { it.identity.albumId })
        assertEquals(2, found.candidates.size)
    }

    @Test
    fun albumHydrationFailureOnOneCandidateDoesNotFailTheProvider() = runBlocking {
        val transport = FakeTidalTransport(
            search = { _, type ->
                if (type == "TRACKS") {
                    tracksPage(
                        listOf(
                            trackItem("broken", "Blinding Lights", "After Hours"),
                            trackItem("healthy", "Blinding Lights", "After Hours")
                        )
                    )
                } else {
                    albumsPage(emptyList())
                }
            },
            album = { albumId -> if (albumId == "broken") throw IOException("Tidal HTTP 503") else albumDetails("After Hours") }
        )

        val result = provider(transport).find(identity())

        assertEquals("healthy", (result as MotionArtworkProviderResult.Found).candidates.single().identity.albumId)
        assertEquals(listOf("broken", "healthy"), transport.albums)
    }

    @Test
    fun failedTrackSearchesStillTryTheAlbumFallback() = runBlocking {
        val transport = FakeTidalTransport(
            search = { _, type ->
                if (type == "TRACKS") throw IOException("Invalid Tidal response")
                albumsPage(listOf(albumItem("album-ok", "After Hours", videoCover = VIDEO_COVER)))
            },
            album = { error("hydration not expected") }
        )

        val result = provider(transport).find(identity())

        assertEquals("album-ok", (result as MotionArtworkProviderResult.Found).candidates.single().identity.albumId)
        assertEquals(listOf("TRACKS", "TRACKS", "ALBUMS"), transport.searches.map { it.second })
    }

    @Test
    fun partialFailureWithoutMatchIsReportedAsFailureAndNeverNegativeCached() = runBlocking {
        var first = true
        val transport = FakeTidalTransport(
            search = { _, type ->
                if (type == "TRACKS" && first) {
                    first = false
                    throw IOException("timeout")
                }
                if (type == "TRACKS") tracksPage(emptyList()) else albumsPage(emptyList())
            },
            album = { error("hydration not expected") }
        )

        val result = provider(transport).find(identity())

        assertTrue(result is MotionArtworkProviderResult.Failed)
        assertEquals(4, transport.searches.size)
        assertFalse(
            shouldNegativeCacheMotionArtwork(
                providerFailed = result is MotionArtworkProviderResult.Failed,
                verifierFailed = false,
                verificationExhaustive = true
            )
        )
    }

    @Test
    fun cleanMissStaysAConclusiveNoMatch() = runBlocking {
        val transport = FakeTidalTransport(
            search = { _, type -> if (type == "TRACKS") tracksPage(emptyList()) else albumsPage(emptyList()) },
            album = { error("hydration not expected") }
        )

        assertEquals(MotionArtworkProviderResult.NoMatch, provider(transport).find(identity()))
    }

    @Test
    fun cancellationDuringAlbumHydrationIsPropagated() {
        val transport = FakeTidalTransport(
            search = { _, _ -> tracksPage(listOf(trackItem("album", "Blinding Lights", "After Hours"))) },
            album = { throw CancellationException("cancelled") }
        )

        val thrown = runBlocking {
            try {
                provider(transport).find(identity())
                null
            } catch (error: CancellationException) {
                error
            }
        }

        assertNotNull(thrown)
    }

    private class FakeTidalTransport(
        private val search: (String, String) -> JSONObject,
        private val album: (String) -> JSONObject
    ) : TidalMotionTransport {
        val searches = mutableListOf<Pair<String, String>>()
        val albums = mutableListOf<String>()

        override suspend fun search(query: String, type: String, country: String): JSONObject {
            searches += query to type
            return search.invoke(query, type)
        }

        override suspend fun album(albumId: String, country: String): JSONObject {
            albums += albumId
            return album.invoke(albumId)
        }
    }

    private fun provider(transport: TidalMotionTransport) =
        TidalVideoCoverProvider(DEFAULT_MOTION_ARTWORK_MINIMUM_CONFIDENCE, transport)

    private fun artists(): JSONArray = JSONArray().put(JSONObject().put("name", "The Weeknd"))

    private fun trackItem(
        albumId: String,
        title: String,
        albumTitle: String,
        videoCover: String = ""
    ): JSONObject = JSONObject()
        .put("id", "track-$albumId")
        .put("title", title)
        .put("duration", 200)
        .put("artists", artists())
        .put(
            "album",
            JSONObject()
                .put("id", albumId)
                .put("title", albumTitle)
                .put("videoCover", videoCover)
        )

    private fun albumItem(id: String, title: String, videoCover: String = ""): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("artists", artists())
        .put("videoCover", videoCover)

    private fun albumDetails(title: String): JSONObject = JSONObject()
        .put("title", title)
        .put("artists", artists())
        .put("videoCover", VIDEO_COVER)

    private fun tracksPage(items: List<JSONObject>): JSONObject =
        JSONObject().put("tracks", JSONObject().put("items", JSONArray(items)))

    private fun albumsPage(items: List<JSONObject>): JSONObject =
        JSONObject().put("albums", JSONObject().put("items", JSONArray(items)))

    private fun identity(album: String = "After Hours") = MotionTrackIdentity(
        title = "Blinding Lights",
        artists = listOf("The Weeknd"),
        album = album,
        durationMs = 200_000L,
        isrc = "",
        upc = "",
        year = "",
        trackId = "track",
        albumId = "album"
    )

    private fun albumCandidate(
        title: String = "Blinding Lights",
        album: String = "After Hours"
    ) = MotionArtworkCandidate(
        provider = "tidal-video-cover",
        scope = MotionArtworkScope.ALBUM,
        identity = identity(album).copy(title = title),
        url = "https://example.invalid/video.mp4",
        mimeType = "video/mp4",
        expiresAtMs = Long.MAX_VALUE
    )

    private companion object {
        const val VIDEO_COVER = "11111111-2222-3333-4444-555555555555"
        const val ALT_VIDEO_COVER = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    }
}
