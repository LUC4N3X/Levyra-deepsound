package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistProLogicTest {

    @Test
    fun searchMatchesTitleArtistAndAlbumWithoutChangingPlaylistOrder() {
        val tracks = listOf(
            track("a", "Però", "Mina", "Sera"),
            track("b", "Elsewhere", "Siamo", "Blu"),
            track("c", "Third", "Someone", "Città")
        )
        val index = buildPlaylistSearchIndex(tracks)

        assertEquals(listOf("a"), filterPlaylistTracks(tracks, "PERO", index).map { it.id })
        assertEquals(listOf("b"), filterPlaylistTracks(tracks, "siamo", index).map { it.id })
        assertEquals(listOf("c"), filterPlaylistTracks(tracks, "citta", index).map { it.id })
        assertEquals(listOf("a", "b", "c"), filterPlaylistTracks(tracks, "", index).map { it.id })
        assertTrue(filterPlaylistTracks(tracks, "missing", index).isEmpty())
    }

    @Test
    fun selectionTogglesClearsAndSelectsAllUsingStableTrackIdentity() {
        val original = listOf(track("a"), track("b"), track("c"))
        val refreshed = original.map { it.copy(title = "Updated ${it.title}") }
        val all = selectAllPlaylistTrackKeys(original)
        val withoutMiddle = togglePlaylistTrackSelection(all, playlistEntryKey(original[1]))

        assertEquals(3, all.size)
        assertEquals(listOf("a", "c"), selectedPlaylistTracks(refreshed, withoutMiddle).map { it.id })
        assertEquals(all, togglePlaylistTrackSelection(emptySet(), playlistEntryKey(original[0])) + all.drop(1))
        assertTrue(clearPlaylistTrackSelection().isEmpty())
    }

    private fun track(
        id: String,
        title: String = "Track $id",
        artist: String = "Artist",
        album: String = "Album"
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
