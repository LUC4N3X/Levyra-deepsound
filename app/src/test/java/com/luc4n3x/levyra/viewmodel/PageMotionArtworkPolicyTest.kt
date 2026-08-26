package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private fun track(id: String, artist: String): Track = Track(
        id = id,
        title = "Song",
        artist = artist,
        album = "Album",
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
