package com.luc4n3x.levyra.domain

import com.luc4n3x.levyra.data.decodeSpeedDialPins
import com.luc4n3x.levyra.data.encodeSpeedDialPins
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedDialTest {
    private fun track(id: String, stream: String = "https://stream.example/$id") = Track(
        id = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = stream,
        videoUrl = "https://music.example/watch?v=$id",
        thumbnailUrl = "https://img.example/$id.jpg",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = setOf("music"),
        energy = 50,
        vocal = 50,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 1,
        accentEnd = 2,
        artistBrowseIds = listOf("UC123")
    )

    private fun playlist(id: String, name: String = "Mix") =
        Playlist(id = id, name = name, coverUrl = "", tracks = emptyList(), createdAt = 0L, updatedAt = 0L)

    @Test
    fun sameIdAcrossKindsNeverCollides() {
        val song = requireNotNull(SpeedDial.song(track("abc"), 1L))
        val playlist = requireNotNull(SpeedDial.playlist(playlist("abc"), 2L))
        val pins = SpeedDial.toggle(SpeedDial.toggle(emptyList(), song), playlist)
        assertEquals(2, pins.size)
        assertNotEquals(pins[0].key, pins[1].key)
    }

    @Test
    fun toggleRemovesExistingPinAndPrependsNewOnesWithoutReorderingOthers() {
        val a = requireNotNull(SpeedDial.song(track("a"), 1L))
        val b = requireNotNull(SpeedDial.song(track("b"), 2L))
        val c = requireNotNull(SpeedDial.song(track("c"), 3L))
        val pins = SpeedDial.toggle(listOf(a, b), c)
        assertEquals(listOf("song:c", "song:a", "song:b"), pins.map { it.key })
        assertEquals(listOf("song:c", "song:b"), SpeedDial.toggle(pins, a).map { it.key })
    }

    @Test
    fun toggleRespectsTheLimit() {
        val full = (0 until SpeedDial.MAX_PINS).mapNotNull { SpeedDial.song(track("t$it"), it.toLong()) }
        val extra = requireNotNull(SpeedDial.song(track("extra"), 99L))
        assertEquals(full.map { it.key }, SpeedDial.toggle(full, extra).map { it.key })
    }

    @Test
    fun reorderKeepsPinsMissingFromAStaleGesture() {
        val pins = listOf("a", "b", "c").mapNotNull { SpeedDial.song(track(it), 0L) }
        val reordered = SpeedDial.reorder(pins, listOf("song:c", "song:a", "song:unknown"))
        assertEquals(listOf("song:c", "song:a", "song:b"), reordered.map { it.key })
    }

    @Test
    fun visibleHidesDeletedPlaylistsAndMissingLocalFilesAndFollowsRenames() {
        val kept = requireNotNull(SpeedDial.playlist(playlist("p1", "Old"), 0L))
        val deleted = requireNotNull(SpeedDial.playlist(playlist("p2"), 0L))
        val local = requireNotNull(SpeedDial.song(track("local:gone", "content://media/1"), 0L))
        val remote = requireNotNull(SpeedDial.song(track("remote"), 0L))
        val visible = SpeedDial.visible(
            pins = listOf(kept, deleted, local, remote),
            playlists = listOf(playlist("p1", "Renamed")),
            localTrackIds = emptySet()
        )
        assertEquals(listOf("playlist:p1", "song:remote"), visible.map { it.key })
        assertEquals("Renamed", visible.first().title)
        assertEquals(2, SpeedDial.visible(listOf(local, remote), emptyList(), localTrackIds = null).size)
    }

    @Test
    fun songPinsDropExpiringStreamsButKeepLocalUris() {
        assertEquals("", SpeedDial.song(track("r"), 0L)?.track?.streamUrl)
        assertEquals("content://media/9", SpeedDial.song(track("l", "content://media/9"), 0L)?.track?.streamUrl)
    }

    @Test
    fun invalidInputsDoNotCreatePins() {
        assertNull(SpeedDial.song(track(" "), 0L))
        assertNull(SpeedDial.artist(" ", "", "", 0L))
        assertNull(SpeedDial.playlist(playlist(""), 0L))
    }

    @Test
    fun codecRoundTripsEveryKindAndSkipsCorruptEntries() {
        val album = AlbumHit(title = "Album", artist = "Artist", year = "2024", thumbnailUrl = "u", query = "q", browseId = "MPRE1")
        val pins = listOfNotNull(
            SpeedDial.song(track("s"), 1L),
            SpeedDial.album(album, 2L),
            SpeedDial.artist("Artist", "UC1", "a", 3L),
            SpeedDial.artist("Nameless", "", "", 4L),
            SpeedDial.playlist(playlist("p"), 5L)
        )
        val decoded = decodeSpeedDialPins(encodeSpeedDialPins(pins))
        assertEquals(pins.map { it.key }, decoded.map { it.key })
        assertEquals(pins.map { it.title }, decoded.map { it.title })
        assertEquals(pins[0].track?.id, decoded[0].track?.id)
        assertEquals(album, decoded[1].album)
        assertEquals("UC1", SpeedDial.artistBrowseId(decoded[2]))
        assertEquals("", SpeedDial.artistBrowseId(decoded[3]))

        val corrupt = """[{"kind":"song","targetId":"x","title":"No payload"},{"kind":"bogus"},42,""" +
            """{"kind":"playlist","targetId":"p","title":"Ok"}]"""
        assertEquals(listOf("playlist:p"), decodeSpeedDialPins(corrupt).map { it.key })
        assertTrue(decodeSpeedDialPins("not json").isEmpty())
    }
}
