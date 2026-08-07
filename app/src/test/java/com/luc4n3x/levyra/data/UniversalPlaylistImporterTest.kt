package com.luc4n3x.levyra.data

import java.io.ByteArrayInputStream
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalPlaylistImporterTest {

    @Test
    fun parsesSpotifyPlaylistTitleAndTrackUrlsWithoutDependingOnAttributeOrder() {
        val html = """
            <html><head>
            <meta content="Road &amp; Night | Spotify" property="og:title">
            <meta name="music:song" content="https://open.spotify.com/track/trackOne?si=abc">
            <meta content="spotify:track:trackTwo" name="music:song">
            <meta property="music:song" content="https://open.spotify.com/track/trackOne?si=duplicate">
            </head></html>
        """.trimIndent()

        val page = parseSpotifyPlaylistPage(html)

        assertEquals("Road & Night", page.title)
        assertEquals(
            listOf(
                "https://open.spotify.com/track/trackOne",
                "https://open.spotify.com/track/trackTwo"
            ),
            page.trackUrls
        )
    }

    @Test
    fun parsesSpotifyTrackMetadata() {
        val html = """
            <html><head>
            <meta property="og:title" content="A &amp; B">
            <meta content="Artist Name" name="music:musician_description">
            <meta name="music:duration" content="213">
            <meta content="https://image.test/cover.jpg" property="og:image">
            </head></html>
        """.trimIndent()

        val track = parseSpotifyTrackPage(html)

        assertNotNull(track)
        assertEquals("A & B", track?.title)
        assertEquals("Artist Name", track?.artist)
        assertEquals(213_000L, track?.durationMs)
        assertEquals("https://image.test/cover.jpg", track?.artworkUrl)
    }

    @Test
    fun fallsBackToSpotifyDescriptionForArtist() {
        val html = """
            <meta property="og:title" content="Song Title">
            <meta name="twitter:description" content="Fallback Artist · Song Title · Song · 2026">
        """.trimIndent()

        val track = parseSpotifyTrackPage(html)

        assertEquals("Fallback Artist", track?.artist)
    }

    @Test
    fun prefersExplicitDurationMsAndSupportsSecondOrMillisecondExports() {
        assertEquals(201_234L, importedDurationMs(JSONObject().put("durationMs", 201_234L).put("duration", 10L)))
        assertEquals(201_000L, importedDurationMs(JSONObject().put("duration", 201L)))
        assertEquals(201_234L, importedDurationMs(JSONObject().put("duration", 201_234L)))
    }

    @Test
    fun rejectsJsonImportAboveConfiguredTrackLimit() {
        assertTrue(jsonImportTrackCountAccepted(MAX_JSON_IMPORT_TRACKS))
        assertFalse(jsonImportTrackCountAccepted(MAX_JSON_IMPORT_TRACKS + 1))
    }

    @Test
    fun importCoordinatorRejectsSecondConcurrentImport() {
        PlaylistImportCoordinator.finish()
        try {
            assertTrue(PlaylistImportCoordinator.tryBegin())
            assertFalse(PlaylistImportCoordinator.tryBegin())
        } finally {
            PlaylistImportCoordinator.finish()
        }
        assertTrue(PlaylistImportCoordinator.tryBegin())
        PlaylistImportCoordinator.finish()
    }

    @Test
    fun spotifyImportRequiresHttpsAndSpotifyOwnedHost() {
        assertNotNull(validateSpotifyImportUrl("https://open.spotify.com/playlist/example"))
        assertNotNull(validateSpotifyImportUrl("https://spotify.link/example"))
        assertNull(validateSpotifyImportUrl("http://open.spotify.com/playlist/example"))
        assertNull(validateSpotifyImportUrl("https://spotify.example.com/playlist/example"))
        assertNull(validateSpotifyImportUrl("https://example.com/playlist/example"))
    }

    @Test
    fun boundedReaderStopsBeforeOversizedBodyIsParsed() {
        assertEquals("hello", readUtf8Bounded(ByteArrayInputStream("hello".toByteArray()), 5L))
        assertNull(readUtf8Bounded(ByteArrayInputStream("hello!".toByteArray()), 5L))
    }
}
