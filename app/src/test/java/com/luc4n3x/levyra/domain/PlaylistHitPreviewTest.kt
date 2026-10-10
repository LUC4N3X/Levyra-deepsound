package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistHitPreviewTest {
    private fun track(id: String) = Track(
        id = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
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
        accentEnd = 2
    )

    private fun hit(
        playlistId: String = "PL1",
        author: String = "Curator",
        thumbnailUrl: String = "https://img.example/cover.jpg",
        trackCountLabel: String = ""
    ) = PlaylistHit(
        title = "Chill Hits",
        author = author,
        thumbnailUrl = thumbnailUrl,
        playlistId = playlistId,
        trackCountLabel = trackCountLabel
    )

    private fun format(count: Int) = "$count tracks"

    @Test
    fun `metadata track count label is shown as provided`() {
        assertEquals("24 songs", hit(trackCountLabel = "24 songs").displayTrackCount(loadedTracks = 0, format = ::format))
    }

    @Test
    fun `metadata label wins over the loaded track count`() {
        assertEquals("300 songs", hit(trackCountLabel = "300 songs").displayTrackCount(loadedTracks = 150, format = ::format))
    }

    @Test
    fun `missing metadata label falls back to the loaded track count`() {
        assertEquals("12 tracks", hit().displayTrackCount(loadedTracks = 12, format = ::format))
    }

    @Test
    fun `blank metadata label is treated as missing`() {
        assertEquals("7 tracks", hit(trackCountLabel = "   ").displayTrackCount(loadedTracks = 7, format = ::format))
    }

    @Test
    fun `no label and no loaded tracks omits the count`() {
        assertEquals("", hit().displayTrackCount(loadedTracks = 0, format = ::format))
    }

    @Test
    fun `a new preview starts loading with no tracks and no failure`() {
        val preview = PlaylistHitPreview(hit = hit())

        assertTrue(preview.loading)
        assertFalse(preview.failed)
        assertTrue(preview.tracks.isEmpty())
    }

    @Test
    fun `resolving publishes the tracks and stops loading`() {
        val tracks = listOf(track("a"), track("b"))

        val resolved = requireNotNull(
            PlaylistHitPreview(hit = hit()).resolvedWith("PL1", author = "", thumbnailUrl = "", tracks = tracks)
        )

        assertEquals(tracks, resolved.tracks)
        assertFalse(resolved.loading)
        assertFalse(resolved.failed)
    }

    @Test
    fun `resolving with no tracks marks the preview as failed`() {
        val resolved = requireNotNull(
            PlaylistHitPreview(hit = hit()).resolvedWith("PL1", author = "", thumbnailUrl = "", tracks = emptyList())
        )

        assertFalse(resolved.loading)
        assertTrue(resolved.failed)
    }

    @Test
    fun `a late response for another playlist is discarded`() {
        val current = PlaylistHitPreview(hit = hit(playlistId = "PL2"))

        assertNull(current.resolvedWith("PL1", author = "", thumbnailUrl = "", tracks = listOf(track("a"))))
    }

    private fun loaded(vararg ids: String) =
        PlaylistHitPreview(hit = hit(), tracks = ids.map(::track), loading = false)

    @Test
    fun `next track follows the current track inside the playlist`() {
        assertEquals("b", loaded("a", "b", "c").nextTrackAfter("a")?.id)
    }

    @Test
    fun `there is no next track after the last one`() {
        assertNull(loaded("a", "b", "c").nextTrackAfter("c"))
    }

    @Test
    fun `a current track outside the playlist points to the first playlist track`() {
        assertEquals("a", loaded("a", "b", "c").nextTrackAfter("elsewhere")?.id)
    }

    @Test
    fun `no current track points to the first playlist track`() {
        assertEquals("a", loaded("a", "b", "c").nextTrackAfter(null)?.id)
    }

    @Test
    fun `an empty playlist has no next track`() {
        assertNull(PlaylistHitPreview(hit = hit()).nextTrackAfter("a"))
    }

    @Test
    fun `resolving fills a missing author and cover from the detail`() {
        val resolved = requireNotNull(
            PlaylistHitPreview(hit = hit(author = "", thumbnailUrl = ""))
                .resolvedWith("PL1", author = "Detail Author", thumbnailUrl = "https://img.example/detail.jpg", tracks = listOf(track("a")))
        )

        assertEquals("Detail Author", resolved.hit.author)
        assertEquals("https://img.example/detail.jpg", resolved.hit.thumbnailUrl)
    }

    @Test
    fun `resolving keeps the author and cover already known from search`() {
        val resolved = requireNotNull(
            PlaylistHitPreview(hit = hit())
                .resolvedWith("PL1", author = "Detail Author", thumbnailUrl = "https://img.example/detail.jpg", tracks = listOf(track("a")))
        )

        assertEquals("Curator", resolved.hit.author)
        assertEquals("https://img.example/cover.jpg", resolved.hit.thumbnailUrl)
    }
}
