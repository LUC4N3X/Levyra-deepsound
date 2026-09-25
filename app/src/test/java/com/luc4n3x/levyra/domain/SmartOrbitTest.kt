package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartOrbitTest {

    private val now = 1_750_000_000_000L

    private fun track(id: String, artist: String = "Artist $id", title: String = "Title $id"): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Album $id",
        durationMs = 200_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "https://lh3.googleusercontent.com/$id=w544-h544",
        largeThumbnailUrl = "https://lh3.googleusercontent.com/$id=w1200-h1200",
        source = "youtube",
        moodTags = setOf("music"),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0
    )

    private fun event(id: String, artist: String, listenedMs: Long): ListenEvent = ListenEvent(
        trackId = id,
        title = "Title $id",
        artist = artist,
        listenedMs = listenedMs,
        trackDurationMs = 200_000L,
        completed = false,
        startedAt = now
    )

    @Test
    fun candidateRelatedToSeveralListenedSeedsGainsWeight() {
        var pool = SmartOrbitPool.Empty
        pool = SmartOrbitEngine.accumulate(pool, track("a"), listOf(track("x"), track("y")), now)
        pool = SmartOrbitEngine.accumulate(pool, track("b"), listOf(track("x")), now)
        pool = SmartOrbitEngine.accumulate(pool, track("c"), listOf(track("x"), track("z")), now)

        val bonus = pool.bonusScores
        assertEquals(SmartOrbitEngine.coOccurrenceBonus(3), bonus.getValue("x"))
        assertTrue(bonus.getValue("x") > bonus.getValue("y"))
        assertEquals(listOf("x"), SmartOrbitEngine.discoveries(pool, null, { false }).map { it.id })
    }

    @Test
    fun repeatedSeedDoesNotInflateCoOccurrence() {
        var pool = SmartOrbitPool.Empty
        repeat(5) { pool = SmartOrbitEngine.accumulate(pool, track("a"), listOf(track("x")), now) }

        assertEquals(1, pool.candidates.single().coOccurrence)
        assertTrue(SmartOrbitEngine.discoveries(pool, null, { false }).isEmpty())
    }

    @Test
    fun seedAndItsAlternateUploadsAreNeverCandidates() {
        val seed = track("a")
        val pool = SmartOrbitEngine.accumulate(
            SmartOrbitPool.Empty,
            seed,
            listOf(track("a"), track("a-live", artist = "Artist a", title = "Title a"), track("x")),
            now
        )

        assertEquals(listOf("x"), pool.candidates.map { it.track.id })
    }

    @Test
    fun alreadyListenedAndBlockedCandidatesAreExcluded() {
        var pool = SmartOrbitPool.Empty
        listOf("a", "b").forEach { seed ->
            pool = SmartOrbitEngine.accumulate(pool, track(seed), listOf(track("heard"), track("blocked"), track("fresh")), now)
        }
        val profile = ListeningSignalEngine.build(events = listOf(event("heard", "Artist heard", 60_000L)), nowMs = now)

        val discoveries = SmartOrbitEngine.discoveries(pool, profile, isBlocked = { it.id == "blocked" })

        assertEquals(listOf("fresh"), discoveries.map { it.id })
    }

    @Test
    fun earlySkippedArtistIsDemotedButNotBlacklisted() {
        var pool = SmartOrbitPool.Empty
        listOf("a", "b").forEach { seed ->
            pool = SmartOrbitEngine.accumulate(pool, track(seed), listOf(track("x1", artist = "Skipped"), track("y1", artist = "Loved")), now)
        }
        val profile = ListeningSignalEngine.build(
            events = listOf(event("s1", "Skipped", 6_000L), event("l1", "Loved", 200_000L)),
            nowMs = now
        )

        val discoveries = SmartOrbitEngine.discoveries(pool, profile, { false })

        assertEquals(listOf("y1", "x1"), discoveries.map { it.id })
    }

    @Test
    fun discoveriesKeepArtistDiversity() {
        var pool = SmartOrbitPool.Empty
        listOf("a", "b").forEach { seed ->
            pool = SmartOrbitEngine.accumulate(
                pool,
                track(seed),
                listOf(track("s1", "Same"), track("s2", "Same"), track("s3", "Same"), track("o1", "Other")),
                now
            )
        }

        val artists = SmartOrbitEngine.discoveries(pool, null, { false }).map { it.artist }

        assertEquals(2, artists.size)
        assertEquals(setOf("Same", "Other"), artists.toSet())
    }

    @Test
    fun poolIsBoundedAndExpires() {
        var pool = SmartOrbitPool.Empty
        repeat(40) { index ->
            pool = SmartOrbitEngine.accumulate(pool, track("seed$index"), (0 until 12).map { track("c$index-$it") }, now)
        }
        assertEquals(SmartOrbitEngine.MAX_CANDIDATES, pool.candidates.size)

        assertTrue(SmartOrbitEngine.prune(pool, now + SmartOrbitEngine.CANDIDATE_TTL_MS).isEmpty)
    }

    @Test
    fun rankerAppliesCoOccurrenceBonus() {
        val ranked = ListeningSignalRanker.rank(
            candidates = listOf(track("plain"), track("recurring")),
            profile = ListeningSignalProfile(),
            bonusScores = mapOf("recurring" to SmartOrbitEngine.coOccurrenceBonus(3))
        )

        assertEquals(listOf("recurring", "plain"), ranked.map { it.id })
    }

    @Test
    fun orbitReservesSlotsForDiscoveries() {
        val history = (1..20).map { track("h$it") }

        val orbit = LevyraPersonalOrbit.build(
            currentTrack = null,
            recentSearches = history,
            favorites = emptyList(),
            tracks = emptyList(),
            homeSections = emptyList(),
            charts = emptyList(),
            limit = 20,
            languageCode = "en",
            discoveries = listOf(track("d1"), track("d2"))
        )

        assertEquals(20, orbit.size)
        assertTrue(orbit.map { it.id }.containsAll(listOf("d1", "d2")))
        assertEquals("h1", orbit.first().id)
    }

    @Test
    fun orbitExclusionRemovesRetiredCandidatesButKeepsHistory() {
        val orbit = LevyraPersonalOrbit.build(
            currentTrack = null,
            recentSearches = listOf(track("h1")),
            favorites = emptyList(),
            tracks = emptyList(),
            homeSections = emptyList(),
            charts = emptyList(),
            cachedOrbit = listOf(track("rejected"), track("kept")),
            limit = 20,
            languageCode = "en",
            excluded = { it.id == "rejected" || it.id == "h1" }
        )

        assertEquals(listOf("h1", "kept"), orbit.map { it.id })
    }

    @Test
    fun significantProgressUsesCountedPlayCriterion() {
        assertFalse(ListenPlayPolicy.isSignificantProgress(29_000L, 200_000L))
        assertTrue(ListenPlayPolicy.isSignificantProgress(30_000L, 200_000L))
        assertFalse(ListenPlayPolicy.isSignificantProgress(4_000L, 5_000L))
        assertTrue(ListenPlayPolicy.isSignificantProgress(16_000L, 20_000L))
        assertFalse(ListenPlayPolicy.isSignificantProgress(15_000L, 20_000L))
        assertFalse(ListenPlayPolicy.isSignificantProgress(20_000L, 0L))
    }
}
