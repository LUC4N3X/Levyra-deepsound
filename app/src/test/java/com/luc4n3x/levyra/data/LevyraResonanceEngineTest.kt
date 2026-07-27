package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraResonanceEngineTest {
    @Test
    fun temporalDecayLetsRecentTasteOvertakeOldTaste() {
        var now = 1_700_000_000_000L
        val engine = LevyraResonanceEngine { now }
        val state = LevyraResonanceState()
        val oldTrack = track("old", "Old Artist", "Old Album", energy = 45)
        val recentTrack = track("recent", "Recent Artist", "Recent Album", energy = 45)

        repeat(4) { engine.recordCompletion(state, oldTrack) }
        now += 120L * 86_400_000L
        repeat(2) { engine.recordCompletion(state, recentTrack) }

        val profile = engine.toSmartProfile(state)

        assertEquals("Recent Artist", profile.topArtists.first().label)
        assertTrue(profile.topArtists.first().weight > profile.topArtists.last().weight)
    }

    @Test
    fun repeatedEarlySkipsSwitchRankingIntoRecoveryMode() {
        var now = 1_700_000_000_000L
        val engine = LevyraResonanceEngine { now }
        val state = LevyraResonanceState()
        val safe = track("safe", "Trusted Artist", "Trusted Album", energy = 52, replay = 90, cache = 80)
        val novel = track("novel", "Unknown Artist", "Unknown Album", energy = 52, replay = 10)

        engine.recordFavorite(state, safe, true)
        repeat(3) { index ->
            val rejected = track("skip-$index", "Rejected $index", "Rejected", energy = 85)
            engine.recordPlayback(state, rejected)
            engine.recordTransition(state, rejected, positionMs = 2_000L, durationMs = 200_000L)
            now += 1_000L
        }

        val ranked = engine.rankRadioCandidates(
            state = state,
            candidates = listOf(novel, safe),
            currentQueue = emptyList(),
            currentTrack = null
        )

        assertEquals("safe", ranked.first().id)
        assertTrue(state.session.consecutiveSkips >= 3)
        assertTrue(state.session.familiarityNeed > 0.35)
    }

    @Test
    fun queueDiversificationAvoidsRepeatingCurrentArtist() {
        val engine = LevyraResonanceEngine { 1_700_000_000_000L }
        val state = LevyraResonanceState()
        val current = track("current", "Same Artist", "Album A", energy = 60)
        val sameArtist = track("same", "Same Artist", "Album B", energy = 61)
        val otherArtist = track("other", "Other Artist", "Album C", energy = 61)
        val thirdArtist = track("third", "Third Artist", "Album D", energy = 59)

        val ranked = engine.rankRadioCandidates(
            state = state,
            candidates = listOf(sameArtist, otherArtist, thirdArtist),
            currentQueue = listOf(current),
            currentTrack = current
        )

        assertNotEquals("Same Artist", ranked.first().artist)
        assertEquals(3, ranked.map(Track::id).distinct().size)
    }

    @Test
    fun resolverHealthInfluencesOtherwiseEquivalentCandidates() {
        val engine = LevyraResonanceEngine { 1_700_000_000_000L }
        val state = LevyraResonanceState()
        repeat(8) { engine.recordResolverResult(state, "Fast Source", true, 180L) }
        repeat(8) { engine.recordResolverResult(state, "Failing Source", false, 2_500L) }
        val fast = track("fast", "Artist One", "Album", source = "Fast Source")
        val failing = track("failing", "Artist Two", "Album", source = "Failing Source")

        val ranked = engine.rankRadioCandidates(
            state = state,
            candidates = listOf(failing, fast),
            currentQueue = emptyList(),
            currentTrack = null
        )

        assertEquals("fast", ranked.first().id)
    }

    private fun track(
        id: String,
        artist: String,
        album: String,
        energy: Int = 50,
        replay: Int = 50,
        cache: Int = 0,
        source: String = "YouTube Music"
    ): Track = Track(
        id = id,
        title = "Track $id",
        artist = artist,
        album = album,
        durationMs = 200_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = source,
        moodTags = setOf("balanced"),
        energy = energy,
        vocal = 60,
        replayScore = replay,
        cacheScore = cache,
        accentStart = 0,
        accentEnd = 0
    )
}
