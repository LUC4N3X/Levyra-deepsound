package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.ArtistBiography
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.feature.search.searchTestTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class ArtistProfileFallbackCacheTest {

    private fun profile(browseId: String, name: String, songCount: Int = 0) = ArtistProfile(
        browseId = browseId,
        name = name,
        subscribers = "",
        monthlyListeners = "",
        thumbnailUrl = "",
        bannerUrl = "",
        topSongs = List(songCount) { index -> searchTestTrack("song-$browseId-$index", "Song $index", name) },
        albums = emptyList(),
        singles = emptyList(),
        accentStart = 0,
        accentEnd = 0
    )

    @Test
    fun fallbackWithSongsIsReturnedAndKept() {
        val fallbacks = ArtistFallbackProfiles(capacity = 4)
        val withSongs = profile("", "Artist", songCount = 3)

        assertSame(withSongs, fallbacks.settle("artist", withSongs))
        assertSame(withSongs, fallbacks.settle("artist", null))
    }

    @Test
    fun emptyOrFailedFallbackKeepsTheLastSongs() {
        val fallbacks = ArtistFallbackProfiles(capacity = 4)
        val withSongs = profile("", "Artist", songCount = 3)
        fallbacks.settle("artist", withSongs)

        assertSame(withSongs, fallbacks.settle("artist", profile("", "Artist")))
        assertSame(withSongs, fallbacks.settle("artist", null))
    }

    @Test
    fun emptyFallbackWithoutHistoryIsReturnedAsIs() {
        val fallbacks = ArtistFallbackProfiles(capacity = 4)
        val empty = profile("", "Artist")

        assertSame(empty, fallbacks.settle("artist", empty))
        assertNull(fallbacks.settle("other", null))
    }

    @Test
    fun verifiedLookupForgetsTheFallback() {
        val fallbacks = ArtistFallbackProfiles(capacity = 4)
        fallbacks.settle("artist", profile("", "Artist", songCount = 3))

        fallbacks.forget("artist")

        assertNull(fallbacks.settle("artist", null))
    }

    @Test
    fun oldestFallbackIsEvictedAtCapacity() {
        val fallbacks = ArtistFallbackProfiles(capacity = 2)
        fallbacks.settle("one", profile("", "One", songCount = 1))
        fallbacks.settle("two", profile("", "Two", songCount = 1))
        fallbacks.settle("three", profile("", "Three", songCount = 1))

        assertNull(fallbacks.settle("one", null))
        assertEquals("Three", fallbacks.settle("three", null)?.name)
    }

    @Test
    fun biographyOfAFallbackNeverReplacesTheVerifiedProfileOrEntersTheCache() {
        val verified = profile("UC_ARTIST", "Artist", songCount = 5)
        val fallback = profile("", "Artist", songCount = 2)
        val cache = ConcurrentHashMap(mapOf("artist" to verified, "browse:uc_artist" to verified))

        refreshArtistProfileAliases(cache, fallback, fallback.copy(biography = biography()))

        assertSame(verified, cache["artist"])
        assertSame(verified, cache["browse:uc_artist"])
        assertEquals(2, cache.size)
    }

    @Test
    fun biographyRefreshesEveryAliasOfTheCachedProfileWithoutAddingKeys() {
        val verified = profile("UC_ARTIST", "Artist", songCount = 5)
        val cache = ConcurrentHashMap(mapOf("artist" to verified, "browse:uc_artist" to verified))
        val updated = verified.copy(biography = biography())

        refreshArtistProfileAliases(cache, verified, updated)

        assertSame(updated, cache["artist"])
        assertSame(updated, cache["browse:uc_artist"])
        assertFalse(cache.containsKey("other"))
        assertEquals(2, cache.size)
    }

    private fun biography() = ArtistBiography(text = "Biography", sourceLabel = "Wikipedia")
}
