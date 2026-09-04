package com.luc4n3x.levyra.domain

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgottenFavoritesTest {

    private val now = 1_700_000_000_000L

    @Test
    fun selectsOnlyFavoritesNotPlayedForAtLeastThreshold() {
        val forgotten = track("forgotten")
        val recent = track("recent")

        val result = ForgottenFavorites.select(
            favorites = listOf(recent, forgotten),
            lastPlayedByKey = mapOf(
                "forgotten" to now - days(90),
                "recent" to now - days(3)
            ),
            nowMs = now
        )

        assertEquals(listOf("forgotten"), result.map { it.id })
    }

    @Test
    fun excludesFavoritesWithoutAnyListeningRecord() {
        val neverPlayed = track("never")

        val result = ForgottenFavorites.select(
            favorites = listOf(neverPlayed),
            lastPlayedByKey = mapOf("other" to now - days(365)),
            nowMs = now
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun ordersMostForgottenFirst() {
        val oldest = track("oldest")
        val middle = track("middle")
        val newest = track("newest")

        val result = ForgottenFavorites.select(
            favorites = listOf(middle, newest, oldest),
            lastPlayedByKey = mapOf(
                "oldest" to now - days(400),
                "middle" to now - days(120),
                "newest" to now - days(31)
            ),
            nowMs = now
        )

        assertEquals(listOf("oldest", "middle", "newest"), result.map { it.id })
    }

    @Test
    fun deduplicatesFavoritesSharingTheSameListeningIdentity() {
        val first = track("dup")
        val second = track("dup").copy(title = "Different display title")

        val result = ForgottenFavorites.select(
            favorites = listOf(first, second),
            lastPlayedByKey = mapOf("dup" to now - days(60)),
            nowMs = now
        )

        assertEquals(1, result.size)
    }

    @Test
    fun skipsUnplayableEntries() {
        val blankTitle = track("blank").copy(title = "  ")
        val noPlaybackTarget = Track(
            id = "",
            title = "Orphan",
            artist = "Artist",
            album = "Album",
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

        val result = ForgottenFavorites.select(
            favorites = listOf(blankTitle, noPlaybackTarget),
            lastPlayedByKey = mapOf(
                "blank" to now - days(90),
                ListenIdentity.trackKey("", "Orphan", "Artist") to now - days(90)
            ),
            nowMs = now
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun respectsDisplayLimit() {
        val favorites = (1..30).map { track("t$it") }
        val lastPlayed = favorites.associate { it.id to now - days(60) }

        val result = ForgottenFavorites.select(favorites, lastPlayed, nowMs = now, limit = 5)

        assertEquals(5, result.size)
    }

    @Test
    fun listeningKeysSkipInvalidFavorites() {
        val keys = ForgottenFavorites.listeningKeys(
            listOf(track("a"), track("a"), track("b").copy(title = " "))
        )

        assertEquals(listOf("a"), keys)
    }

    private fun days(value: Long): Long = TimeUnit.DAYS.toMillis(value)

    private fun track(id: String) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://example.invalid/$id",
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
