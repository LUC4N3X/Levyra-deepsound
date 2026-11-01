package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistAlbumsTest {
    private fun track(id: String, entry: String = "") = Track(
        id = id, title = "T$id", artist = "A", album = "Al", durationMs = 1000L, streamUrl = "",
        videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "", moodTags = emptySet(),
        energy = 0, vocal = 0, replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0,
        playlistEntryId = entry
    )

    private fun album(entryId: String, offset: Int, vararg ids: String) = PlaylistAlbumEntry(
        entryId = entryId,
        album = AlbumHit(title = "Album $entryId", artist = "B", year = "2020", thumbnailUrl = "", query = "", browseId = "MPRE$entryId"),
        tracks = ids.map { track(it) },
        trackOffset = offset,
        addedAt = 1L
    )

    private val identity: (Track) -> String = { it.id }

    @Test
    fun mixedPlaylistInterleavesAlbumsAtTheirOffsets() {
        val items = mergePlaylistItems(listOf(track("a", "e1"), track("c", "e2")), listOf(album("B", 1, "b1", "b2")))
        assertEquals(listOf("song:e1", "album:B", "song:e2"), items.map { it.key })
    }

    @Test
    fun queueExpandsAlbumInOriginalOrderBetweenSongs() {
        val items = mergePlaylistItems(listOf(track("a", "e1"), track("c", "e2")), listOf(album("B", 1, "b1", "b2", "b3")))
        val plan = expandPlaylistQueue(items, null, identity)
        assertEquals(listOf("a", "b1", "b2", "b3", "c"), plan.tracks.map { it.id })
    }

    @Test
    fun startingFromAlbumPointsAtItsFirstTrack() {
        val items = mergePlaylistItems(listOf(track("a", "e1"), track("c", "e2")), listOf(album("B", 1, "b1", "b2")))
        val plan = expandPlaylistQueue(items, "album:B", identity)
        assertEquals("b1", plan.tracks[plan.startIndex].id)
        val fromLast = expandPlaylistQueue(items, "song:e2", identity)
        assertEquals("c", fromLast.tracks[fromLast.startIndex].id)
    }

    @Test
    fun duplicateTracksCollapseWithoutLosingTheStartPoint() {
        val items = mergePlaylistItems(listOf(track("x", "e1")), listOf(album("B", 1, "x", "y")))
        val plan = expandPlaylistQueue(items, "album:B", identity)
        assertEquals(listOf("x", "y"), plan.tracks.map { it.id })
        assertEquals("y", plan.tracks[plan.startIndex].id)
        val fullyDuplicated = expandPlaylistQueue(mergePlaylistItems(listOf(track("x", "e1")), listOf(album("C", 1, "x"))), "album:C", identity)
        assertEquals(0, fullyDuplicated.startIndex)
    }

    @Test
    fun unavailableAlbumIsSkippedWithoutBlockingThePlaylist() {
        val items = mergePlaylistItems(listOf(track("a", "e1")), listOf(album("B", 0)))
        val plan = expandPlaylistQueue(items, "album:B", identity)
        assertEquals(listOf("a"), plan.tracks.map { it.id })
        assertEquals(0, plan.startIndex)
    }

    @Test
    fun layoutKeepsAlbumsAnchoredToTheirPreviousTrack() {
        val previous = listOf(
            PlaylistLayoutSlot("t1", false), PlaylistLayoutSlot("A", true),
            PlaylistLayoutSlot("t2", false), PlaylistLayoutSlot("B", true), PlaylistLayoutSlot("t3", false)
        )
        val removedFirst = layoutAlbumsAroundTracks(previous, listOf("t2", "t3"))
        assertEquals(listOf("A", "t2", "B", "t3"), removedFirst.map { it.entryId })
        val reordered = layoutAlbumsAroundTracks(previous, listOf("t3", "t2", "t1"))
        assertEquals(listOf("t3", "t2", "B", "t1", "A"), reordered.map { it.entryId })
        val appended = layoutAlbumsAroundTracks(previous, listOf("t1", "t2", "t3", "t4"))
        assertEquals(listOf("t1", "A", "t2", "B", "t3", "t4"), appended.map { it.entryId })
    }

    @Test
    fun albumIdentityPrefersStableIdsAndDetectsDuplicates() {
        val hit = AlbumHit(title = "X", artist = "Y", year = "1999", thumbnailUrl = "", query = "", browseId = "MPREabc")
        assertEquals("browse:mpreabc", playlistAlbumIdentity(hit))
        assertEquals("meta:y|x|1999", playlistAlbumIdentity(hit.copy(browseId = "")))
        val playlist = Playlist("p", "P", "", emptyList(), 0L, 0L, albums = listOf(album("abc", 0, "1")))
        assertTrue(playlist.containsAlbum(hit.copy(browseId = "MPREABC")))
        assertFalse(playlist.containsAlbum(hit.copy(browseId = "other")))
        assertEquals(1, playlist.albumCount)
        assertEquals(1, playlist.playableTrackCount)
    }
}
