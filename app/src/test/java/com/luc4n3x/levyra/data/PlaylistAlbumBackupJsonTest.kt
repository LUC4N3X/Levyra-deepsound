package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.domain.Track
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistAlbumBackupJsonTest {
    private fun track(id: String) = Track(
        id = id, title = "T$id", artist = "A", album = "Al", durationMs = 1000L, streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id", thumbnailUrl = "", largeThumbnailUrl = "", source = "YouTube Music",
        moodTags = setOf("music"), energy = 60, vocal = 50, replayScore = 84, cacheScore = 78, accentStart = 1, accentEnd = 2,
        trackNumber = 3
    )

    @Test
    fun albumEntryRoundTripsIdentityPositionAndTracks() {
        val entry = PlaylistAlbumBackup(
            entryId = "entry-1",
            slot = 2,
            album = AlbumHit(
                title = "Album", artist = "Artist", year = "2021", thumbnailUrl = "https://img", query = "q",
                browseId = "MPRE1", artistBrowseId = "UC1", audioPlaylistId = "OLAK1", explicit = true,
                releaseType = ReleaseType.Ep
            ),
            tracks = listOf(track("a"), track("b")),
            addedAt = 42L
        )
        val restored = PlaylistAlbumBackupJson.parseArray(JSONArray().put(PlaylistAlbumBackupJson.toJson(entry)), 0L)
        assertEquals(1, restored.size)
        val copy = restored.single()
        assertEquals(entry.entryId, copy.entryId)
        assertEquals(entry.slot, copy.slot)
        assertEquals(entry.album, copy.album)
        assertEquals(entry.addedAt, copy.addedAt)
        assertEquals(listOf("a", "b"), copy.tracks.map { it.id })
        assertEquals(3, copy.tracks.first().trackNumber)
    }

    @Test
    fun malformedAlbumEntriesAreIgnored() {
        val array = JSONArray()
            .put(org.json.JSONObject().put("entryId", "x").put("title", "No tracks"))
            .put(org.json.JSONObject().put("title", "No id").put("tracks", JSONArray()))
        assertEquals(emptyList<PlaylistAlbumBackup>(), PlaylistAlbumBackupJson.parseArray(array, 0L))
        assertEquals(emptyList<PlaylistAlbumBackup>(), PlaylistAlbumBackupJson.parseArray(null, 0L))
        assertNull(PlaylistAlbumBackupJson.fromJson(org.json.JSONObject(), 0L))
    }

    @Test
    fun restoredLayoutPlacesAlbumsAtTheirSlotsAndTracksAround() {
        val layout = restoredPlaylistLayout(trackCount = 3, albumSlots = listOf(1, 4))
        assertEquals(listOf(1, 4), layout.albumPositions)
        assertEquals(listOf(0, 2, 3), layout.trackPositions)
        val clamped = restoredPlaylistLayout(trackCount = 1, albumSlots = listOf(99, 99))
        assertEquals(listOf(1, 2), clamped.albumPositions)
        assertEquals(listOf(0), clamped.trackPositions)
        val legacy = restoredPlaylistLayout(trackCount = 2, albumSlots = emptyList())
        assertEquals(listOf(0, 1), legacy.trackPositions)
    }
}
