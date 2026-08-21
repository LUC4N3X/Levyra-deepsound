package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

private enum class SampleStrategy { InnerTube, WebEmbedded, Android, IosClient }

class PlaybackStrategyHealthTest {

    @Test
    fun orderIsAlwaysAPermutationOfInput() {
        val strategies = SampleStrategy.values().toList()
        val now = 1_000_000L

        val scenarios = listOf(
            emptyMap<String, PlaybackStrategyStats>(),
            mapOf(SampleStrategy.WebEmbedded.name to openStatsAt(now, kind = PlaybackFailureKind.Forbidden)),
            strategies.associate { it.name to openStatsAt(now, kind = PlaybackFailureKind.Gone) }
        )

        scenarios.forEach { statsMap ->
            val result = orderPlaybackStrategiesByHealth(
                strategies = strategies,
                statsFor = { statsMap[it] },
                nowMs = now
            )
            assertEquals(strategies.size, result.size)
            assertEquals(strategies.sortedBy { it.ordinal }, result.sortedBy { it.ordinal })
        }
    }

    @Test
    fun emptyHealthReturnsInputUnchanged() {
        val strategies = SampleStrategy.values().toList()
        val result = orderPlaybackStrategiesByHealth(
            strategies = strategies,
            statsFor = { null },
            nowMs = 1_000L
        )
        assertSame(strategies, result)
    }

    @Test
    fun unseenPolicyFirstStrategyGetsCanaryBeforePreviouslyHealthyFallback() {
        val strategies = listOf(SampleStrategy.InnerTube, SampleStrategy.WebEmbedded)
        val statsMap = mapOf(
            SampleStrategy.WebEmbedded.name to PlaybackStrategyStats(successes = 20)
        )

        val result = orderPlaybackStrategiesByHealth(
            strategies = strategies,
            statsFor = { statsMap[it] },
            nowMs = 1_000L
        )

        assertEquals(strategies, result)
    }

    @Test
    fun threeConsecutiveForbiddenFailuresMovesStrategyToBack() {
        val now = 5_000_000L
        val failing = statsAfterConsecutiveFailures(
            count = 3,
            kind = PlaybackFailureKind.Forbidden,
            startAtMs = now - 3_000L
        )
        assertEquals(PlaybackStrategyCircuit.OPEN, failing.circuitAt(now))

        val strategies = listOf(SampleStrategy.WebEmbedded, SampleStrategy.InnerTube, SampleStrategy.Android)
        val statsMap = mapOf(SampleStrategy.WebEmbedded.name to failing)

        val result = orderPlaybackStrategiesByHealth(
            strategies = strategies,
            statsFor = { statsMap[it] },
            nowMs = now
        )

        assertEquals(strategies.toSet(), result.toSet())
        assertEquals(SampleStrategy.WebEmbedded, result.last())
    }

    @Test
    fun afterQuarantineExpiryStrategyIsHalfOpenAndCanaryAllowed() {
        val openedAt = 10_000_000L
        val opened = statsAfterConsecutiveFailures(
            count = 3,
            kind = PlaybackFailureKind.RateLimited,
            startAtMs = openedAt - 3_000L
        )
        val stillQuarantined = opened.quarantineUntilMs - 1L
        assertEquals(PlaybackStrategyCircuit.OPEN, opened.circuitAt(stillQuarantined))

        val afterExpiry = opened.quarantineUntilMs + 1L
        assertEquals(PlaybackStrategyCircuit.HALF_OPEN, opened.circuitAt(afterExpiry))

        val strategies = listOf(SampleStrategy.InnerTube, SampleStrategy.WebEmbedded, SampleStrategy.Android)
        val stillOpenOther = statsAfterConsecutiveFailures(
            count = 3,
            kind = PlaybackFailureKind.Forbidden,
            startAtMs = afterExpiry - 3_000L
        )
        val statsMap = mapOf(
            SampleStrategy.WebEmbedded.name to opened,
            SampleStrategy.Android.name to stillOpenOther
        )

        val result = orderPlaybackStrategiesByHealth(
            strategies = strategies,
            statsFor = { statsMap[it] },
            nowMs = afterExpiry
        )

        assertEquals(strategies.toSet(), result.toSet())
        assertTrue("half-open canary must not be dropped", result.contains(SampleStrategy.WebEmbedded))
        assertTrue(result.indexOf(SampleStrategy.WebEmbedded) < result.indexOf(SampleStrategy.Android))
    }

    @Test
    fun hardRuntimeFailureOpensCircuitImmediately() {
        val now = 15_000_000L
        val updated = playbackStrategyStatsAfterRuntimeFailure(
            current = null,
            nowMs = now,
            kind = PlaybackFailureKind.Forbidden
        )

        assertEquals(PlaybackStrategyCircuit.OPEN, updated.circuitAt(now))
        assertTrue(updated.quarantineUntilMs > now)
    }

    @Test
    fun transientRuntimeFailureDoesNotOpenCircuitImmediately() {
        val now = 16_000_000L
        val updated = playbackStrategyStatsAfterRuntimeFailure(
            current = null,
            nowMs = now,
            kind = PlaybackFailureKind.ServerError
        )

        assertEquals(PlaybackStrategyCircuit.CLOSED, updated.circuitAt(now))
    }

    @Test
    fun resolutionSuccessCannotCloseAnOpenCircuitBeforeCooldown() {
        val now = 18_000_000L
        val opened = playbackStrategyStatsAfterRuntimeFailure(
            current = null,
            nowMs = now,
            kind = PlaybackFailureKind.Forbidden
        )
        val stillOpenAt = now + 1_000L

        val resolvedAgain = playbackStrategyStatsAfterSuccess(opened, stillOpenAt, latencyMs = 120L)

        assertEquals(PlaybackStrategyCircuit.OPEN, resolvedAgain.circuitAt(stillOpenAt))
        assertEquals(opened.quarantineUntilMs, resolvedAgain.quarantineUntilMs)
    }

    @Test
    fun successOnHalfOpenStrategyReturnsToClosed() {
        val openedAt = 20_000_000L
        val opened = statsAfterConsecutiveFailures(
            count = 4,
            kind = PlaybackFailureKind.Signature,
            startAtMs = openedAt - 4_000L
        )
        val afterExpiry = opened.quarantineUntilMs + 1L
        assertEquals(PlaybackStrategyCircuit.HALF_OPEN, opened.circuitAt(afterExpiry))

        val recovered = playbackStrategyStatsAfterSuccess(opened, afterExpiry, latencyMs = 250L)

        assertEquals(PlaybackStrategyCircuit.CLOSED, recovered.circuitAt(afterExpiry))
        assertEquals(0, recovered.consecutiveFailures)
        assertEquals(0L, recovered.quarantineUntilMs)
    }

    @Test
    fun marginallyWorseScoringStrategyDoesNotLeapfrogPolicyOrder() {
        val now = 1_000L
        val policyFirstLowerScore = PlaybackStrategyStats(successes = 8, failures = 2)
        val policySecondHigherScore = PlaybackStrategyStats(successes = 9, failures = 1)
        assertTrue(policySecondHigherScore.score - policyFirstLowerScore.score < 25.0)

        val strategies = listOf(SampleStrategy.WebEmbedded, SampleStrategy.InnerTube)
        val statsMap = mapOf(
            SampleStrategy.WebEmbedded.name to policyFirstLowerScore,
            SampleStrategy.InnerTube.name to policySecondHigherScore
        )

        val result = orderPlaybackStrategiesByHealth(
            strategies = strategies,
            statsFor = { statsMap[it] },
            nowMs = now
        )

        assertEquals(strategies, result)
    }

    @Test
    fun materialScoreGapDoesReorderWithinSameCircuit() {
        val now = 1_000L
        val policyFirstMuchWorse = PlaybackStrategyStats(successes = 5, failures = 5)
        val policySecondMuchBetter = PlaybackStrategyStats(successes = 9, failures = 1)
        assertTrue(policySecondMuchBetter.score - policyFirstMuchWorse.score >= 25.0)

        val strategies = listOf(SampleStrategy.WebEmbedded, SampleStrategy.InnerTube)
        val statsMap = mapOf(
            SampleStrategy.WebEmbedded.name to policyFirstMuchWorse,
            SampleStrategy.InnerTube.name to policySecondMuchBetter
        )

        val result = orderPlaybackStrategiesByHealth(
            strategies = strategies,
            statsFor = { statsMap[it] },
            nowMs = now
        )

        assertEquals(listOf(SampleStrategy.InnerTube, SampleStrategy.WebEmbedded), result)
    }

    @Test
    fun corruptPersistedJsonDegradesToEmptyHealth() {
        assertEquals(emptyMap<String, PlaybackStrategyStats>(), parsePlaybackStrategyHealthSnapshot("{not valid json"))
        assertEquals(emptyMap<String, PlaybackStrategyStats>(), parsePlaybackStrategyHealthSnapshot("[1,2,3]"))
        assertEquals(emptyMap<String, PlaybackStrategyStats>(), parsePlaybackStrategyHealthSnapshot(""))
        assertEquals(emptyMap<String, PlaybackStrategyStats>(), parsePlaybackStrategyHealthSnapshot(null))
    }

    @Test
    fun validPersistedJsonRoundTrips() {
        val stats = statsAfterConsecutiveFailures(
            count = 3,
            kind = PlaybackFailureKind.LoginRequired,
            startAtMs = 1_000L
        )
        val json = playbackStrategyStatsToJson(stats)
        val root = JSONObject().put("audio::WebEmbedded", json)
        val restored = parsePlaybackStrategyHealthSnapshot(root.toString())

        val restoredStats = restored["audio::WebEmbedded"]
        assertNotNull(restoredStats)
        assertEquals(stats.consecutiveFailures, restoredStats!!.consecutiveFailures)
        assertEquals(stats.quarantineUntilMs, restoredStats.quarantineUntilMs)
        assertEquals(PlaybackFailureKind.LoginRequired, restoredStats.lastFailureKind)
    }

    @Test
    fun unparseableFailureKindDegradesToNull() {
        val entry = JSONObject().put("lastFailureKind", "NotARealKind")
        val root = JSONObject().put("audio::Android", entry)
        val restored = parsePlaybackStrategyHealthSnapshot(root.toString())
        assertNull(restored["audio::Android"]!!.lastFailureKind)
    }

    private fun openStatsAt(nowMs: Long, kind: PlaybackFailureKind): PlaybackStrategyStats {
        return statsAfterConsecutiveFailures(count = 3, kind = kind, startAtMs = nowMs - 3_000L)
    }

    private fun statsAfterConsecutiveFailures(
        count: Int,
        kind: PlaybackFailureKind,
        startAtMs: Long
    ): PlaybackStrategyStats {
        var stats: PlaybackStrategyStats? = null
        repeat(count) { attempt ->
            stats = playbackStrategyStatsAfterFailure(stats, startAtMs + attempt, latencyMs = null, kind = kind)
        }
        return stats!!
    }
}
