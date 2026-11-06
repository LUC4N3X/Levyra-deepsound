package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.domain.LibrarySort
import com.luc4n3x.levyra.domain.LibrarySortDirection
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SavedPlaylist
import com.luc4n3x.levyra.feature.discovery.track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryBrowseSnapshotTest {
    private val favorites = (1..2_000).map { index -> track("Song $index", "Artist ${index % 40}").copy(album = "Album ${index % 120}") }
    private val input = LibraryBrowseInput(
        favorites = favorites,
        favoriteTimestamps = favorites.mapIndexed { index, track -> track.id to index.toLong() }.toMap(),
        playlists = emptyList(),
        downloads = emptyList(),
        recentListens = emptyList(),
        mostPlayedTracks = emptyList(),
        followedArtists = emptyList(),
        sort = LibrarySort.Title,
        direction = LibrarySort.Title.defaultDirection
    )

    @Test
    fun snapshotPrecomputesTheSameListsTheScreenWouldSort() {
        val snapshot = input.buildSnapshot()
        val catalog = snapshot.catalog

        assertEquals(filterLibraryTracks(catalog.tracks, "", input.sort, input.direction, catalog::recencyOf), snapshot.tracks)
        assertEquals(filterLibraryAlbums(catalog.albums, "", input.sort, input.direction), snapshot.albums)
        assertEquals(filterLibraryArtists(catalog.artists, "", input.sort, input.direction), snapshot.artists)
        assertEquals(2_000, snapshot.tracks.size)
    }

    @Test
    fun presortedListsAreReusedOnlyForTheMatchingUnfilteredView() {
        val snapshot = input.buildSnapshot()

        assertTrue(snapshot.presortedFor("", LibrarySort.Title, input.direction))
        assertFalse(snapshot.presortedFor("song 1", LibrarySort.Title, input.direction))
        assertFalse(snapshot.presortedFor("", LibrarySort.Artist, input.direction))
        val flipped = if (input.direction == LibrarySortDirection.Ascending) LibrarySortDirection.Descending else LibrarySortDirection.Ascending
        assertFalse(snapshot.presortedFor("", LibrarySort.Title, flipped))
    }

    @Test
    fun savedPlaylistsFilterByTitleAndCurator() {
        val saved = listOf(
            SavedPlaylist(PlaylistHit("Rock Classics", "Levyra", "", playlistId = "a"), savedAt = 2L),
            SavedPlaylist(PlaylistHit("Chill", "Radiohead fans", "", playlistId = "b"), savedAt = 1L)
        )

        assertEquals(saved, filterSavedPlaylists(saved, ""))
        assertEquals(listOf("a"), filterSavedPlaylists(saved, "rock").map { it.hit.playlistId })
        assertEquals(listOf("b"), filterSavedPlaylists(saved, "radiohead").map { it.hit.playlistId })
    }

    @Test
    fun savedPlaylistsFollowTheSelectedSort() {
        val saved = listOf(
            SavedPlaylist(PlaylistHit("Beta", "Zed", "", playlistId = "b"), savedAt = 3L),
            SavedPlaylist(PlaylistHit("alpha", "Amy", "", playlistId = "a"), savedAt = 1L),
            SavedPlaylist(PlaylistHit("Gamma", "Max", "", playlistId = "g"), savedAt = 2L)
        )

        assertEquals(listOf("b", "g", "a"), filterSavedPlaylists(saved, "", LibrarySort.Recent, LibrarySortDirection.Descending).map { it.hit.playlistId })
        assertEquals(listOf("a", "b", "g"), filterSavedPlaylists(saved, "", LibrarySort.Title, LibrarySortDirection.Ascending).map { it.hit.playlistId })
        assertEquals(listOf("g", "b", "a"), filterSavedPlaylists(saved, "", LibrarySort.Title, LibrarySortDirection.Descending).map { it.hit.playlistId })
        assertEquals(listOf("a", "g", "b"), filterSavedPlaylists(saved, "", LibrarySort.Artist, LibrarySortDirection.Ascending).map { it.hit.playlistId })
    }
}
