package com.luc4n3x.levyra.domain

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class FreshDiscoveryTest {

    private val now = 1_700_000_000_000L
    private val enabled = FreshDiscoverySettings(enabled = true)

    @Test
    fun defaultsAreOffWithThirtyDaysAndFavoritesAllowed() {
        val defaults = LevyraAutomationSettings().freshDiscovery
        assertFalse(defaults.enabled)
        assertEquals(FreshDiscoveryWindow.DAYS_30, defaults.window)
        assertEquals(true, defaults.allowFavorites)
        assertEquals(FreshDiscoveryWindow.DAYS_30, FreshDiscoveryWindow.from(null))
        assertEquals(FreshDiscoveryWindow.DAYS_30, FreshDiscoveryWindow.from("unknown"))
    }

    @Test
    fun disabledReturnsTheSameCandidateList() {
        val candidates = listOf(track("a"), track("b"))
        val result = FreshDiscovery.select(
            candidates = candidates,
            lastPlayedByKey = mapOf("a" to now - days(1)),
            settings = FreshDiscoverySettings(),
            isFavorite = { false },
            minimumFresh = 0,
            nowMs = now
        )
        assertSame(candidates, result)
    }

    @Test
    fun excludesTracksHeardInsideTheWindowAndKeepsOrder() {
        val result = select(
            candidates = listOf(track("new1"), track("recent"), track("old"), track("new2")),
            lastPlayed = mapOf("recent" to now - days(3), "old" to now - days(45))
        )
        assertEquals(listOf("new1", "old", "new2"), result.map(Track::id))
    }

    @Test
    fun windowsControlTheCutoff() {
        val candidates = listOf(track("d5"), track("d20"), track("d60"), track("d400"))
        val lastPlayed = mapOf(
            "d5" to now - days(5),
            "d20" to now - days(20),
            "d60" to now - days(60),
            "d400" to now - days(400)
        )
        fun kept(window: FreshDiscoveryWindow) =
            select(candidates, lastPlayed, enabled.copy(window = window)).map(Track::id)

        assertEquals(listOf("d20", "d60", "d400"), kept(FreshDiscoveryWindow.DAYS_7))
        assertEquals(listOf("d60", "d400"), kept(FreshDiscoveryWindow.DAYS_30))
        assertEquals(listOf("d400"), kept(FreshDiscoveryWindow.DAYS_90))
        assertEquals(emptyList<String>(), kept(FreshDiscoveryWindow.ALL_TIME))
    }

    @Test
    fun favoritesAreKeptOnlyWhenAllowed() {
        val candidates = listOf(track("fav"), track("new"))
        val lastPlayed = mapOf("fav" to now - days(1))
        val isFavorite: (Track) -> Boolean = { it.id == "fav" }

        assertEquals(
            listOf("fav", "new"),
            select(candidates, lastPlayed, enabled, isFavorite).map(Track::id)
        )
        assertEquals(
            listOf("new"),
            select(candidates, lastPlayed, enabled.copy(allowFavorites = false), isFavorite).map(Track::id)
        )
    }

    @Test
    fun matchesHistoryRecordedWithoutAnId() {
        val result = select(
            candidates = listOf(track("x", title = "Song", artist = "Band"), track("new")),
            lastPlayed = mapOf("song|band" to now - days(2))
        )
        assertEquals(listOf("new"), result.map(Track::id))
    }

    @Test
    fun dropsAnotherUploadOfAHeardRecording() {
        val result = select(
            candidates = listOf(
                track("official", title = "Song", artist = "Band"),
                track("video", title = "Song (Official Video)", artist = "Band"),
                track("cover", title = "Song", artist = "Other Artist"),
                track("new")
            ),
            lastPlayed = mapOf("official" to now - days(2))
        )
        assertEquals(listOf("cover", "new"), result.map(Track::id))
    }

    @Test
    fun allowedFavoriteSurvivesARecentlyHeardUploadOfTheSameRecording() {
        val result = select(
            candidates = listOf(
                track("heard", title = "Song", artist = "Band"),
                track("favorite", title = "Song (Official Video)", artist = "Band")
            ),
            lastPlayed = mapOf("heard" to now - days(2), "favorite" to now - days(200)),
            isFavorite = { it.id == "favorite" }
        )
        assertEquals(listOf("favorite"), result.map(Track::id))
    }

    @Test
    fun fallsBackToLeastRecentlyHeardWhenFreshTracksAreInsufficient() {
        val result = select(
            candidates = listOf(track("h1"), track("new"), track("h2"), track("h3")),
            lastPlayed = mapOf(
                "h1" to now - days(1),
                "h2" to now - days(10),
                "h3" to now - days(5)
            ),
            minimumFresh = 3
        )
        assertEquals(listOf("new", "h2", "h3"), result.map(Track::id))
    }

    @Test
    fun emptyHistoryLeavesCandidatesUntouched() {
        val candidates = listOf(track("a"))
        assertSame(candidates, select(candidates, emptyMap()))
    }

    @Test
    fun lookupKeysCoverIdAndTextIdentityWithoutDuplicates() {
        val keys = FreshDiscovery.lookupKeys(
            listOf(track("a", title = "Song", artist = "Band"), track("a", title = "Song", artist = "Band"))
        )
        assertEquals(listOf("a", "song|band"), keys)
        assertEquals(emptyList<String>(), FreshDiscovery.lookupKeys(listOf(track("", title = "", artist = ""))))
    }

    private fun select(
        candidates: List<Track>,
        lastPlayed: Map<String, Long>,
        settings: FreshDiscoverySettings = enabled,
        isFavorite: (Track) -> Boolean = { false },
        minimumFresh: Int = 0
    ): List<Track> = FreshDiscovery.select(candidates, lastPlayed, settings, isFavorite, minimumFresh, now)

    private fun days(value: Long) = TimeUnit.DAYS.toMillis(value)

    private fun track(id: String, title: String = "Title $id", artist: String = "Artist") = Track(
        id = id,
        title = title,
        artist = artist,
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
