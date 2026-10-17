package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioCandidatePoolTest {

    @Test
    fun poolDominatedByTheSeedArtistIsHomogeneous() {
        val pool = (1..8).map { track("b$it", "BTS") } + (1..4).map { track("o$it", "Other $it") }

        assertTrue(isArtistHomogeneous(pool))
    }

    @Test
    fun poolWithManyArtistsIsNotHomogeneous() {
        val pool = (1..12).map { track("a$it", "Artist $it") } + track("a1b", "Artist 1")

        assertFalse(isArtistHomogeneous(pool))
    }

    @Test
    fun featuringCreditsCountTheirLeadArtist() {
        val pool = (1..6).map { track("d$it", "Dua Lipa feat. Guest $it") } + (1..6).map { track("o$it", "Other $it") }

        assertTrue(isArtistHomogeneous(pool))
    }

    @Test
    fun emptyPoolNeedsMoreSources() {
        assertTrue(isArtistHomogeneous(emptyList()))
    }

    private fun track(id: String, artist: String) = Track(
        id = id,
        title = "Title $id",
        artist = artist,
        album = "",
        durationMs = 0L,
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
