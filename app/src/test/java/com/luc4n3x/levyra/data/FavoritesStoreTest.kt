package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesStoreTest {
    @Test
    fun albumToggleAddsEveryMissingTrackWithoutDuplicates() {
        val first = track("first", "First")
        val second = track("second", "Second")
        val unrelated = track("other", "Other", album = "Other album")

        val updated = toggleFavoriteTracks(
            current = listOf(first.copy(title = "Old title"), unrelated),
            targets = listOf(first, second, second)
        )

        assertEquals(listOf("first", "second", "other"), updated.map { it.id })
        assertEquals("First", updated.first().title)
    }

    @Test
    fun albumToggleRemovesEveryTrackWhenWholeAlbumIsFavorite() {
        val first = track("first", "First")
        val second = track("second", "Second")
        val unrelated = track("other", "Other", album = "Other album")

        val updated = toggleFavoriteTracks(
            current = listOf(first, second, unrelated),
            targets = listOf(first, second)
        )

        assertEquals(listOf("other"), updated.map { it.id })
    }

    @Test
    fun albumToggleUsesSingleTrackFallbackIdentity() {
        val stored = track("", "Song", artist = "Artist")
        val sameTrack = stored.copy(title = "song", artist = "artist")

        assertTrue(areAllFavoriteTracks(listOf(stored), listOf(sameTrack)))
        assertEquals(emptyList<Track>(), toggleFavoriteTracks(listOf(stored), listOf(sameTrack)))
    }

    @Test
    fun fallbackIdentityDoesNotCollideWhenTextContainsSeparators() {
        val first = track("", "B|C", artist = "A")
        val second = track("", "C", artist = "A|B")

        val updated = toggleFavoriteTracks(emptyList(), listOf(first, second))

        assertEquals(2, updated.size)
        assertTrue(areAllFavoriteTracks(updated, listOf(first, second)))
    }

    @Test
    fun explicitFavoriteStateIsIdempotent() {
        val current = track("current", "Current")
        val other = track("other", "Other")

        val added = favoriteTracksWithMembership(listOf(other), current, favorite = true)
        assertEquals(listOf("current", "other"), added.map { it.id })
        assertSame(added, favoriteTracksWithMembership(added, current.copy(id = " CURRENT "), favorite = true))

        val removed = favoriteTracksWithMembership(added, current, favorite = false)
        assertEquals(listOf("other"), removed.map { it.id })
        assertSame(removed, favoriteTracksWithMembership(removed, current, favorite = false))
    }

    @Test
    fun membershipUsesFavoriteIdentityRules() {
        val byId = track("Abc", "Title")
        val byMetadata = track("", "Song", artist = "Artist")
        val membership = FavoriteMembership.of(listOf(byId, byMetadata))

        assertTrue(membership.contains(byId.copy(id = " abc ", title = "Renamed")))
        assertTrue(membership.contains(track("", " song ", artist = "ARTIST")))
        assertFalse(membership.contains(track("other", "Song", artist = "Artist")))
        assertTrue(membership.sameTracksAs(listOf(byMetadata, byId)))
        assertFalse(membership.sameTracksAs(listOf(byId)))
    }

    private fun track(
        id: String,
        title: String,
        artist: String = "Artist",
        album: String = "Album"
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
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
        accentEnd = 0
    )
}
