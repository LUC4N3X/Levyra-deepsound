package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSoundtrackHeroTest {
    @Test
    fun `portrait lead track starts the soundtrack queue`() {
        val mix = listOf(track("a", "883"), track("b", "Lazza"))
        val lead = track("lead", "Tananai")
        assertEquals(listOf("lead", "a", "b"), homeSoundtrackQueue(mix, lead).map(Track::id))
    }

    @Test
    fun `lead already in the mix keeps the mix order`() {
        val mix = listOf(track("a", "883"), track("b", "Tananai"))
        assertEquals(mix, homeSoundtrackQueue(mix, mix[1]))
    }

    @Test
    fun `lead search matches the primary artist of a duo portrait`() {
        val primary = homeSoundtrackPrimaryArtist("Miyagi & Andy Panda")
        assertTrue(homeSoundtrackMatchesArtist(track("m", "Miyagi & Andy Panda"), primary, ""))
        assertFalse(homeSoundtrackMatchesArtist(track("x", "Macan"), primary, ""))
    }

    private fun track(id: String, artist: String): Track = Track(
        id = id,
        title = "Track $id",
        artist = artist,
        album = "",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
