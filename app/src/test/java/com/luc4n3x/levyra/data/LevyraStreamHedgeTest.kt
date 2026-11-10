package com.luc4n3x.levyra.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraStreamHedgeTest {
    @Test
    fun playbackExtractorIsDelayedBehindAttestedResolver() {
        val innerTubeDelay = LevyraResolverLatency.innerTubeFallbackDelayMs(
            isVideoMode = false,
            preferMp4Audio = false
        )
        val extractorDelay = LevyraResolverLatency.extractorHedgeDelayMs(
            isVideoMode = false,
            preferMp4Audio = false
        )

        assertEquals(0L, innerTubeDelay)
        assertEquals(600L, extractorDelay)
        assertTrue(extractorDelay > innerTubeDelay)
    }

    @Test
    fun playbackInnerTubeStartsInsideTapLatencyBudget() {
        assertTrue(LevyraResolverLatency.innerTubeFallbackDelayMs(isVideoMode = false, preferMp4Audio = false) <= 120L)
    }

    @Test
    fun innerTubeProfilesFanOutInsideLowLatencyBudget() {
        assertTrue(LevyraResolverLatency.INNER_TUBE_HEDGE_BUDGET_MS <= 150L)
    }

    @Test
    fun offlineMp4ExtractorStartsImmediately() {
        assertEquals(0L, LevyraResolverLatency.extractorHedgeDelayMs(isVideoMode = false, preferMp4Audio = true))
    }

    @Test
    fun muxedVideoFallbackComesAfterTheAudioOnlyHedgeOutsideLowQuality() {
        val bundled = PlaybackCompatibilityPolicy.bundled().audioStrategies

        assertEquals(
            listOf(
                PlaybackAudioStrategy.REEL_AUDIO,
                PlaybackAudioStrategy.PERSISTED,
                PlaybackAudioStrategy.DIRECT,
                PlaybackAudioStrategy.REEL_MUXED,
                PlaybackAudioStrategy.SEARCH
            ),
            orderAudioStrategiesForQuality(bundled, "High")
        )
        assertEquals(orderAudioStrategiesForQuality(bundled, "High"), orderAudioStrategiesForQuality(bundled, "Auto"))
        assertEquals(bundled, orderAudioStrategiesForQuality(bundled, "Low"))
    }

    @Test
    fun healthStillPromotesTheMuxedFallbackWhenAudioOnlyPathsKeepFailing() {
        val preferred = orderAudioStrategiesForQuality(PlaybackCompatibilityPolicy.bundled().audioStrategies, "High")
        val failing = PlaybackStrategyStats(successes = 0, failures = 13, consecutiveFailures = 13, averageLatencyMs = 1_900L)
        val working = PlaybackStrategyStats(successes = 134, failures = 3, consecutiveFailures = 0, averageLatencyMs = 400L)
        val blockedAudioOnly = mapOf(
            "REEL_AUDIO" to failing,
            "PERSISTED" to failing,
            "DIRECT" to failing,
            "REEL_MUXED" to working
        )
        val healthy = mapOf(
            "REEL_AUDIO" to working,
            "PERSISTED" to working,
            "DIRECT" to working,
            "REEL_MUXED" to working
        )

        val whenBlocked = orderPlaybackStrategiesByHealth(preferred, blockedAudioOnly::get, 0L, retryStaleDemotions = true)
        val whenHealthy = orderPlaybackStrategiesByHealth(preferred, healthy::get, 0L, retryStaleDemotions = true)

        assertEquals(PlaybackAudioStrategy.REEL_MUXED, whenBlocked.first())
        assertTrue(whenHealthy.indexOf(PlaybackAudioStrategy.DIRECT) < whenHealthy.indexOf(PlaybackAudioStrategy.REEL_MUXED))
    }

    @Test
    fun recoveredAudioOnlyHedgeIsRetriedBeforeMuxedOnceItsFailuresAreStale() {
        val now = 30L * 60_000L
        val preferred = orderAudioStrategiesForQuality(PlaybackCompatibilityPolicy.bundled().audioStrategies, "High")
        val freshFailure = PlaybackStrategyStats(failures = 16, consecutiveFailures = 16, lastFailureAtMs = now - 60_000L)
        val staleFailures = PlaybackStrategyStats(successes = 1, failures = 15, lastFailureAtMs = now - 15L * 60_000L)
        val muxed = PlaybackStrategyStats(successes = 140, failures = 6, consecutiveFailures = 3, lastFailureAtMs = now - 60_000L)
        val stats = mapOf(
            "REEL_AUDIO" to freshFailure,
            "PERSISTED" to freshFailure,
            "DIRECT" to staleFailures,
            "REEL_MUXED" to muxed,
            "SEARCH" to PlaybackStrategyStats(successes = 2, failures = 2, consecutiveFailures = 1, lastFailureAtMs = now - 60_000L)
        )

        val order = orderPlaybackStrategiesByHealth(preferred, stats::get, now, retryStaleDemotions = true)

        assertEquals(listOf(PlaybackAudioStrategy.DIRECT, PlaybackAudioStrategy.REEL_MUXED), order.take(2))
        assertEquals(preferred.toSet(), order.toSet())
    }

    @Test
    fun strategyOrderWithoutBothStrategiesOrAlreadyDemotedIsUnchanged() {
        val demoted = listOf(PlaybackAudioStrategy.DIRECT, PlaybackAudioStrategy.REEL_MUXED)
        val noDirect = listOf(PlaybackAudioStrategy.REEL_MUXED, PlaybackAudioStrategy.SEARCH)

        assertEquals(demoted, orderAudioStrategiesForQuality(demoted, "High"))
        assertEquals(noDirect, orderAudioStrategiesForQuality(noDirect, "High"))
    }

    private fun candidateQuality(quality: String, videoMode: Boolean = false) =
        audioHedgeQuality<Pair<Int, Boolean>>(
            quality,
            videoMode,
            audioItagOf = { it.first },
            isDirectDelivery = { it.second }
        )

    @Test
    fun audioHedgeQualityRanksOpusAboveAacAndMuxedAndIsOffForLowAndVideo() {
        val quality = candidateQuality("High")!!

        assertTrue(quality.rank(251 to false) > quality.rank(140 to true))
        assertTrue(quality.rank(140 to true) > quality.rank(18 to true))
        assertTrue(quality.rank(251 to true) > quality.rank(251 to false))
        assertTrue(quality.rank(251 to true) >= quality.sufficientRank)
        assertTrue(quality.rank(251 to false) < quality.sufficientRank)
        assertEquals(LevyraResolverLatency.AUDIO_QUALITY_GRACE_MS, quality.graceMs)
        assertNull(candidateQuality("Low"))
        assertNull(candidateQuality("High", videoMode = true))
    }

    @Test
    fun hedgePrefersAnOpusStreamArrivingInsideTheGrace() = runBlocking {
        val aacFirst: suspend () -> Pair<Int, Boolean>? = { 140 to true }
        val opusLater: suspend () -> Pair<Int, Boolean>? = { delay(20L); 251 to true }

        val result = hedgedAudioWithRecovery(
            listOf(listOf(aacFirst), listOf(opusLater)), 0L, recovery = null,
            quality = candidateQuality("High")
        )

        assertEquals(251 to true, result)
    }

    @Test
    fun hedgePrefersADirectOpusStreamOverASabrOneAtTheSameItag() = runBlocking {
        val sabrFirst: suspend () -> Pair<Int, Boolean>? = { 251 to false }
        val directLater: suspend () -> Pair<Int, Boolean>? = { delay(20L); 251 to true }

        val result = hedgedAudioWithRecovery(
            listOf(listOf(sabrFirst), listOf(directLater)), 0L, recovery = null,
            quality = candidateQuality("High")
        )

        assertEquals(251 to true, result)
    }

    @Test
    fun qualityGraceStaysInsideTheTapLatencyBudget() {
        assertTrue(LevyraResolverLatency.AUDIO_QUALITY_GRACE_MS <= 300L)
    }
}
