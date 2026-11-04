package com.luc4n3x.levyra.data.playlistexport

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistTextFormat
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistTextParsers
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistExportTest {
    private fun track(id: String, title: String, artist: String, durationMs: Long = 200_000L, audioVideoId: String = "") = Track(
        id = id, title = title, artist = artist, album = "Album, Deluxe", durationMs = durationMs,
        streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "test",
        moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0,
        audioVideoId = audioVideoId
    )

    private val tracks = listOf(
        track("dQw4w9WgXcQ", "Never Gonna Give You Up", "Rick Astley"),
        track("local:42", "Home Demo", "Me"),
        track("9bZkp7q19f0", "Gangnam Style", "PSY"),
        track("dQw4w9WgXcQ", "Never Gonna Give You Up", "Rick Astley"),
        track("spotify:abc", "Mapped", "Artist", audioVideoId = "kJQP7kiw5Fk")
    )

    @Test
    fun planKeepsOrderSkipsDuplicatesAndReportsUnmatched() {
        val plan = planRemotePlaylistExport(tracks)
        assertEquals(listOf("dQw4w9WgXcQ", "9bZkp7q19f0", "kJQP7kiw5Fk"), plan.videoIds)
        assertEquals(listOf("Home Demo"), plan.unmatched.map { it.title })
        assertEquals(1, plan.duplicates)
    }

    @Test
    fun emptyPlaylistPlansNothing() {
        val plan = planRemotePlaylistExport(emptyList())
        assertEquals(emptyList<String>(), plan.videoIds)
        assertEquals(0, plan.duplicates)
    }

    @Test
    fun m3uRoundTripsTitleArtistAndOrder() {
        val text = writePlaylistFile("Road \"Trip\"", tracks, PlaylistFileFormat.M3U8)
        val parsed = PlaylistTextParsers.parse(PlaylistTextFormat.M3U, text)
        assertEquals("Road \"Trip\"", parsed.descriptor.title)
        assertEquals(tracks.map { it.title }, parsed.tracks.map { it.title })
        assertEquals(tracks.map { listOf(it.artist) }, parsed.tracks.map { it.artists })
        assertEquals("dQw4w9WgXcQ", parsed.tracks.first().directCatalogId)
        assertEquals(200_000L, parsed.tracks.first().durationMs)
    }

    @Test
    fun csvRoundTripsWithQuotedFieldsAndOrder() {
        val text = writePlaylistFile("List", tracks, PlaylistFileFormat.CSV)
        val parsed = PlaylistTextParsers.parse(PlaylistTextFormat.CSV, text)
        assertEquals(tracks.map { it.title }, parsed.tracks.map { it.title })
        assertEquals(tracks.map { listOf(it.artist) }, parsed.tracks.map { it.artists })
        assertEquals("Album, Deluxe", parsed.tracks.first().album)
    }

    @Test
    fun csvNeutralisesFormulaCells() {
        val text = writePlaylistFile("List", listOf(track("x", "=HYPERLINK(1)", "A")), PlaylistFileFormat.CSV)
        assertEquals("'=HYPERLINK(1)", text.lines()[1].substringBefore(','))
    }

    @Test
    fun fileNameDropsReservedCharacters() {
        assertEquals("a b c.m3u8", playlistFileName("a/b:c", PlaylistFileFormat.M3U8))
        assertEquals("Levyra playlist.csv", playlistFileName("  ", PlaylistFileFormat.CSV))
    }
}
