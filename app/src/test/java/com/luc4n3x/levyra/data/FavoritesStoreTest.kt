package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
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
