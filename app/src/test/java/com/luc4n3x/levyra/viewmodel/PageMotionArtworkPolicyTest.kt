package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.AlbumDetail
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PageMotionArtworkPolicyTest {

    @Test
    fun `artist motion seed skips wrong primary artist and keeps searching`() {
        val wrongPrimary = track(id = "wrong", artist = "Guest feat. Target")
        val matchingLater = track(id = "matching", artist = "Target feat. Guest")

        val selected = selectArtistMotionSeed(
            profileName = "Target",
            tracks = listOf(wrongPrimary, matchingLater),
            isLocal = { false }
        )

        assertEquals("matching", selected?.id)
    }

    @Test
    fun `artist motion seed skips local matching track`() {
        val local = track(id = "local", artist = "Target")
        val remote = track(id = "remote", artist = "Target")

        val selected = selectArtistMotionSeed(
            profileName = "Target",
            tracks = listOf(local, remote),
            isLocal = { it.id == "local" }
        )

        assertEquals("remote", selected?.id)
    }

    @Test
    fun `artist motion seed returns null when no primary artist matches`() {
        val selected = selectArtistMotionSeed(
            profileName = "Target",
            tracks = listOf(
                track(id = "a", artist = "Guest feat. Target"),
                track(id = "b", artist = "Another Artist"),
            ),
            isLocal = { false }
        )

        assertNull(selected)
    }

    @Test
    fun `album motion uses the first remote track matching album and primary artist`() {
        val local = track(id = "local", artist = "Target", album = "Release")
        val wrongAlbum = track(id = "wrong-album", artist = "Target", album = "Other")
        val wrongPrimary = track(id = "wrong-primary", artist = "Guest feat. Target", album = "Release")
        val matching = track(id = "matching", artist = "Target feat. Guest", album = "Release")

        val selected = selectAlbumMotionSeed(
            detail = albumDetail(tracks = listOf(local, wrongAlbum, wrongPrimary, matching)),
            isLocal = { it.id == "local" }
        )

        assertEquals("matching", selected?.id)
    }

    @Test
    fun `album motion returns no seed when the release has no compatible remote track`() {
        val selected = selectAlbumMotionSeed(
            detail = albumDetail(
                tracks = listOf(
                    track(id = "wrong-album", artist = "Target", album = "Other"),
                    track(id = "wrong-artist", artist = "Another", album = "Release")
                )
            ),
            isLocal = { false }
        )

        assertNull(selected)
    }

    @Test
    fun `album motion publishes only for the visible enabled matching album`() {
        val expected = albumDetail(browseId = "MPREb_expected")

        assertTrue(
            canPublishAlbumMotionArtwork(
                visible = expected,
                expected = expected,
                albumVisible = true,
                animationsEnabled = true,
                motionArtworkEnabled = true
            )
        )
        assertFalse(
            canPublishAlbumMotionArtwork(
                visible = expected,
                expected = expected,
                albumVisible = false,
                animationsEnabled = true,
                motionArtworkEnabled = true
            )
        )
        assertFalse(
            canPublishAlbumMotionArtwork(
                visible = expected,
                expected = expected,
                albumVisible = true,
                animationsEnabled = false,
                motionArtworkEnabled = true
            )
        )
        assertFalse(
            canPublishAlbumMotionArtwork(
                visible = expected,
                expected = expected,
                albumVisible = true,
                animationsEnabled = true,
                motionArtworkEnabled = false
            )
        )
        assertFalse(
            canPublishAlbumMotionArtwork(
                visible = albumDetail(browseId = "MPREb_other"),
                expected = expected,
                albumVisible = true,
                animationsEnabled = true,
                motionArtworkEnabled = true
            )
        )
    }

    private fun albumDetail(
        browseId: String = "MPREb_expected",
        tracks: List<Track> = listOf(track(id = "track", artist = "Target", album = "Release"))
    ): AlbumDetail = AlbumDetail(
        album = AlbumHit(
            title = "Release",
            artist = "Target",
            year = "",
            thumbnailUrl = "",
            query = "",
            browseId = browseId
        ),
        description = "",
        tracks = tracks
    )

    private fun track(id: String, artist: String, album: String = "Album"): Track = Track(
        id = id,
        title = "Song",
        artist = artist,
        album = album,
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=abcdefghijk",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
    )
}
