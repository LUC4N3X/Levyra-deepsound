package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistFavoritesTest {
    private val profile = ArtistProfile(
        browseId = "UC-drake",
        name = "Drake",
        subscribers = "",
        monthlyListeners = "",
        thumbnailUrl = "",
        bannerUrl = "",
        topSongs = emptyList(),
        albums = emptyList(),
        singles = emptyList(),
        accentStart = 0,
        accentEnd = 0
    )

    @Test
    fun zeroFavoritesReturnsNoShelfItems() {
        assertTrue(likedTracksByArtist(emptyList(), profile).isEmpty())
    }

    @Test
    fun canonicalArtistIdentityWinsWhenAvailable() {
        val track = track("1", "Drake feat. 21 Savage", listOf("UC-drake", "UC-21"))
        assertTrue(trackBelongsToArtist(track, "Drake", "UC-drake"))
    }

    @Test
    fun differentCanonicalArtistIsExcludedEvenWhenTextIsMisleading() {
        val track = track("1", "Drake", listOf("UC-someone-else"))
        assertFalse(trackBelongsToArtist(track, "Drake", "UC-drake"))
    }

    @Test
    fun featuringCreditIsMatchedWithoutSubstringFalsePositives() {
        assertTrue(trackBelongsToArtist(track("1", "21 Savage feat. Drake"), "Drake", ""))
        assertFalse(trackBelongsToArtist(track("2", "Drakeford"), "Drake", ""))
    }

    @Test
    fun normalizedCaseAndArticlesAreAcceptedAsFallback() {
        assertTrue(trackBelongsToArtist(track("1", "THE WEEKND"), "The Weeknd", ""))
    }

    @Test
    fun favoritesForOtherArtistsAreExcluded() {
        val favorites = listOf(
            track("1", "Drake", listOf("UC-drake")),
            track("2", "Kendrick Lamar", listOf("UC-kendrick"))
        )
        assertEquals(listOf("1"), likedTracksByArtist(favorites, profile).map { it.id })
    }

    @Test
    fun duplicateFavoriteIdentityIsEmittedOnce() {
        val favorites = listOf(
            track("same", "Drake", listOf("UC-drake")),
            track("same", "Drake", listOf("UC-drake")).copy(title = "Same cached copy")
        )
        assertEquals(1, likedTracksByArtist(favorites, profile).size)
    }

    private fun track(
        id: String,
        artist: String,
        artistBrowseIds: List<String> = emptyList()
    ) = Track(
        id = id,
        title = "Song " + id,
        artist = artist,
        album = "Album",
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
        accentEnd = 0,
        artistBrowseIds = artistBrowseIds
    )
}
