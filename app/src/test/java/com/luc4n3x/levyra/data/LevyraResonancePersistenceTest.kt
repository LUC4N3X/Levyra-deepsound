package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraResonancePersistenceTest {
    @Test
    fun naturalEndGraceDoesNotCountAsSkip() {
        val engine = LevyraResonanceEngine { 1_700_000_000_000L }
        val state = LevyraResonanceState()
        val completed = track("completed", "Finished Artist", "Finished Album")

        engine.recordPlayback(state, completed)
        engine.recordTransition(state, completed, positionMs = 193_000L, durationMs = 200_000L)

        assertEquals(0, state.skipSignals)
        assertEquals(0, state.session.consecutiveSkips)
    }

    @Test
    fun rankingLimitIsConsistentForEveryPath() {
        val engine = LevyraResonanceEngine { 1_700_000_000_000L }
        val state = LevyraResonanceState()
        val candidates = (0 until 30).map { index ->
            track("candidate-$index", "Artist $index", "Album $index")
        }

        val ranked = engine.rankRadioCandidates(
            state = state,
            candidates = candidates,
            currentQueue = emptyList(),
            currentTrack = null,
            limit = 30
        )

        assertEquals(LEVYRA_MAX_RANK_LIMIT, ranked.size)
    }

    @Test
    fun persistenceCodecRoundTripPreservesGraphAndSession() {
        val state = LevyraResonanceState(
            plays = 9,
            completedPlays = 5,
            artists = linkedMapOf(
                "artist one" to ResonanceNode(
                    weight = 42.5,
                    halfLifeDays = 90.0,
                    positiveSignals = 7,
                    negativeSignals = 2,
                    updatedAt = 1_700_000_000_000L
                )
            ),
            resolverArms = linkedMapOf(
                "fast source" to ResolverArmState(
                    successes = 8,
                    failures = 1,
                    averageLatencyMs = 180.0,
                    updatedAt = 1_700_000_000_000L
                )
            ),
            recentTrackIds = arrayListOf("id:one", "id:two"),
            recentArtists = arrayListOf("artist one"),
            session = SessionDna(
                explorationPressure = 0.72,
                familiarityNeed = 0.28,
                averageCompletion = 0.81,
                skipMomentum = 0.16,
                consecutiveSkips = 1,
                lastEnergy = 64,
                startedAt = 1_699_999_000_000L,
                lastEventAt = 1_700_000_000_000L
            ),
            lastUpdated = 1_700_000_000_000L
        )

        val restored = decodeLevyraResonanceState(
            encodeLevyraResonanceState(state),
            now = 1_700_000_000_000L
        )

        assertEquals(9, restored.plays)
        assertEquals(42.5, restored.artists.getValue("artist one").weight)
        assertEquals(0.72, restored.session.explorationPressure)
        assertEquals(8, restored.resolverArms.getValue("fast source").successes)
        assertEquals(listOf("id:one", "id:two"), restored.recentTrackIds)
    }

    @Test
    fun legacyNumericProfileMigratesAndNormalizesKeys() {
        val legacy = """
            {
              "plays": 3,
              "lastUpdated": 1700000000000,
              "artists": {
                "  OLD   ARTIST  ": 18
              },
              "albums": {
                "Old Album • Old Artist": 12
              },
              "moods": {
                "  CHILL  ": 7
              }
            }
        """.trimIndent()

        val restored = decodeLevyraResonanceState(legacy, now = 1_700_000_000_000L)

        assertEquals(18.0, restored.artists.getValue("old artist").weight)
        assertTrue(restored.albums.containsKey("old album|old artist"))
        assertEquals("old artist", normalizeLevyraStoredKey("  OLD   ARTIST  "))
        assertEquals(60.0, restored.artists.getValue("old artist").halfLifeDays)
    }

    private fun track(
        id: String,
        artist: String,
        album: String
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
        source = "YouTube Music",
        moodTags = setOf("balanced"),
        energy = 50,
        vocal = 60,
        replayScore = 50,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
