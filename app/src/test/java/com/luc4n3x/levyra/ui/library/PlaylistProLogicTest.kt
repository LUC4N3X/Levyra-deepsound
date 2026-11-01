package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun selectAllAddsOnlyFilteredResultsAndPreservesHiddenSelection() {
        val tracks = listOf(
            track("a", "Already selected"),
            track("b", "Needle one"),
            track("c", "Needle two"),
            track("d", "Not selected")
        )
        val filtered = filterPlaylistTracks(tracks, "needle", buildPlaylistSearchIndex(tracks))
        val selected = selectPlaylistTracks(setOf(playlistEntryKey(tracks.first())), filtered)

        assertEquals(listOf("a", "b", "c"), selectedPlaylistTracks(tracks, selected).map { it.id })
        assertTrue(areAllPlaylistTracksSelected(filtered, selected))
        assertFalse(areAllPlaylistTracksSelected(tracks, selected))
    }

    @Test
    fun sortChangesOnlyTheReturnedViewAndCustomKeepsStoredOrder() {
        val tracks = listOf(
            track("a", "Beta").copy(durationMs = 200_000L, playlistAddedAt = 10L),
            track("b", "alpha").copy(durationMs = 0L, playlistAddedAt = 30L),
            track("c", "Ålpha").copy(durationMs = 100_000L, playlistAddedAt = 20L),
            track("d", "").copy(durationMs = 300_000L, playlistAddedAt = 20L)
        )
        val stored = tracks.toList()

        assertEquals(listOf("b", "c", "a", "d"), sortPlaylistTracks(tracks, PlaylistTrackSort.TitleAscending).map { it.id })
        assertEquals(listOf("a", "b", "c", "d"), sortPlaylistTracks(tracks, PlaylistTrackSort.TitleDescending).map { it.id })
        assertEquals(listOf("b", "d", "c", "a"), sortPlaylistTracks(tracks, PlaylistTrackSort.NewestFirst).map { it.id })
        assertEquals(listOf("a", "c", "d", "b"), sortPlaylistTracks(tracks, PlaylistTrackSort.OldestFirst).map { it.id })
        assertEquals(listOf("c", "a", "d", "b"), sortPlaylistTracks(tracks, PlaylistTrackSort.ShortestFirst).map { it.id })
        assertEquals(listOf("d", "a", "c", "b"), sortPlaylistTracks(tracks, PlaylistTrackSort.LongestFirst).map { it.id })
        assertTrue(sortPlaylistTracks(tracks, PlaylistTrackSort.Custom) === tracks)
        assertEquals(stored, tracks)
    }

    @Test
    fun sortIsStableForDuplicateKeysInLargePlaylists() {
        val tracks = (0 until 600).map { index ->
            track("t$index", "Same").copy(playlistAddedAt = 5L, durationMs = 180_000L)
        }

        assertEquals(tracks.map { it.id }, sortPlaylistTracks(tracks, PlaylistTrackSort.TitleAscending).map { it.id })
        assertEquals(tracks.map { it.id }, sortPlaylistTracks(tracks, PlaylistTrackSort.OldestFirst).map { it.id })
        assertEquals(tracks.reversed().map { it.id }, sortPlaylistTracks(tracks, PlaylistTrackSort.NewestFirst).map { it.id })
        assertEquals(tracks.map { it.id }, sortPlaylistTracks(tracks, PlaylistTrackSort.LongestFirst).map { it.id })
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
