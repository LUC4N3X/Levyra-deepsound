package com.luc4n3x.levyra.player

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossfadeRampTest {

    @Test
    fun audibleStartNeedsTwoAdvancingSamplesWhilePlaying() {
        val detector = AudibleStartDetector()
        assertFalse(detector.observe(1_000L, playing = true))
        assertFalse(detector.observe(1_000L, playing = true))
        assertFalse(detector.observe(1_020L, playing = true))
        assertFalse(detector.observe(1_020L, playing = false))
        assertFalse(detector.observe(1_040L, playing = true))
        assertFalse(detector.observe(1_060L, playing = true))
        assertTrue(detector.observe(1_085L, playing = true))
        assertEquals(1_085L, detector.audibleFromMs)
    }

    @Test
    fun trackCrossfadeStaysContinuousAcrossIncomingStartLatencies() {
        for (latency in longArrayOf(0L, 40L, 150L, 400L, 900L)) {
            val run = simulateTrackCrossfade(incomingLatencyMs = latency)
            assertTrue("latency $latency completed", run.completed)
            assertNoArtifacts(run.levelsDb, label = "latency $latency")
        }
    }

    @Test
    fun trackCrossfadeHoldsOutgoingWhileIncomingStallsMidRamp() {
        val run = simulateTrackCrossfade(incomingLatencyMs = 120L, incomingStall = 2_500L until 3_100L)
        assertTrue(run.completed)
        val stalled = run.levelsDb.subList(2_550 / WINDOW_MS, 3_050 / WINDOW_MS)
        assertTrue("stall never goes silent ${stalled.min()}", stalled.all { it > -6.5 })
    }

    @Test
    fun incomingThatNeverStartsAbortsWithoutDippingTheOutgoingTrack() {
        val run = simulateTrackCrossfade(incomingLatencyMs = 60_000L)
        assertFalse(run.completed)
        assertTrue(run.silentAbort)
        val beforeAbort = run.levelsDb.take(1_500 / WINDOW_MS)
        assertTrue(beforeAbort.all { abs(it) < 0.1 })
    }

    @Test
    fun handoffAlignsPrimaryAndFadesCorrelatedCopiesWithoutLevelChange() {
        val run = simulateHandoff(prepareMs = 400L, restartLatencyMs = 120L, curve = CrossfadeCurve.EqualGain)
        assertTrue(run.completed)
        assertTrue("residual offset ${run.residualOffsetMs}", abs(run.residualOffsetMs) <= HANDOFF_ALIGN_TOLERANCE_MS)
        assertNoArtifacts(run.levelsDb, label = "handoff")
    }

    @Test
    fun equalPowerOnCorrelatedCopiesWouldSwellSoHandoffUsesEqualGain() {
        val run = simulateHandoff(prepareMs = 400L, restartLatencyMs = 120L, curve = CrossfadeCurve.EqualPower)
        assertTrue("swell ${run.levelsDb.max()}", run.levelsDb.max() > 2.5)
    }

    @Test
    fun benchDetectsTheDipOfARampThatTrustsReadyState() {
        val naive = simulateHandoff(
            prepareMs = 400L,
            restartLatencyMs = 300L,
            curve = CrossfadeCurve.EqualGain,
            trustReadyState = true
        )
        assertTrue("naive dip ${naive.levelsDb.min()}", naive.levelsDb.min() < -10.0)
    }

    @Test
    fun alignerMeasuresMedianOffsetSoBurstyPositionsDoNotTriggerSeeks() {
        val aligner = HandoffAligner()
        val jitter = longArrayOf(0L, 280L, -260L, 10L, 5L, -300L, 290L)
        var result: HandoffAlignStep = HandoffAlignStep.Wait
        var now = 0L
        var index = 0
        while (result == HandoffAlignStep.Wait) {
            now += 25L
            val secondary = 40_000L + now + jitter[index++ % jitter.size]
            result = aligner.step(now, 40_000L + now, true, secondary, 200_000L)
        }
        assertTrue("median ignores bursts: $result", result is HandoffAlignStep.Aligned)
    }

    @Test
    fun alignerGivesUpAfterMaxSeeksAndReportsSilentPrimary() {
        val aligner = HandoffAligner(toleranceMs = 30L, maxSeeks = 1, audibleTimeoutMs = 500L)
        var now = 0L
        var position = 0L
        var step: HandoffAlignStep = HandoffAlignStep.Wait
        while (step == HandoffAlignStep.Wait) {
            now += 25L
            position += 25L
            step = aligner.step(now, position, true, position + 400L, 200_000L)
        }
        assertTrue(step is HandoffAlignStep.Seek)
        step = HandoffAlignStep.Wait
        while (step == HandoffAlignStep.Wait) {
            now += 25L
            position += 25L
            step = aligner.step(now, position, true, position + 400L, 200_000L)
        }
        assertTrue(step is HandoffAlignStep.Unaligned)

        val silent = HandoffAligner(audibleTimeoutMs = 500L)
        var result: HandoffAlignStep = HandoffAlignStep.Wait
        for (t in 0L..600L step 25L) result = silent.step(t, 5_000L, true, 5_000L + t, 200_000L)
        assertEquals(HandoffAlignStep.PrimarySilent, result)
    }

    private class Deck(
        private val frequencyHz: Double,
        startPositionMs: Long,
        var durationMs: Long = Long.MAX_VALUE
    ) {
        var samples = startPositionMs * SAMPLES_PER_MS
        var gain = 0f
        var playing = false
        var audibleAtMs = Long.MAX_VALUE
        var stall: LongRange = LongRange.EMPTY

        val positionMs: Long get() = samples / SAMPLES_PER_MS
        val remainingMs: Long get() = (durationMs - positionMs).coerceAtLeast(0L)

        fun play(nowMs: Long, latencyMs: Long) {
            playing = true
            audibleAtMs = nowMs + latencyMs
        }

        fun seek(positionMs: Long, nowMs: Long, latencyMs: Long) {
            samples = positionMs * SAMPLES_PER_MS
            audibleAtMs = nowMs + latencyMs
        }

        fun reportsPlaying(nowMs: Long) = playing && nowMs !in stall

        fun nextSample(nowMs: Long): Double {
            if (!playing || nowMs < audibleAtMs || nowMs in stall || positionMs >= durationMs) return 0.0
            val value = sin(2.0 * PI * frequencyHz * samples / SAMPLE_RATE) * gain
            samples++
            return value
        }
    }

    private data class Run(
        val levelsDb: List<Double>,
        val completed: Boolean,
        val silentAbort: Boolean = false,
        val residualOffsetMs: Long = 0L
    )

    private fun simulateTrackCrossfade(incomingLatencyMs: Long, incomingStall: LongRange = LongRange.EMPTY): Run {
        val outgoing = Deck(1_040.0, startPositionMs = 4_000L, durationMs = 10_000L).apply {
            gain = 1f
            play(0L, 0L)
        }
        val incoming = Deck(1_600.0, startPositionMs = 0L).apply {
            play(0L, incomingLatencyMs)
            stall = incomingStall
        }
        val ramp = CrossfadeRamp(CrossfadeCurve.EqualPower, fixedSpanMs = null, startTimeoutMs = 1_500L)
        var controlling = true
        var completed = false
        var silentAbort = false
        val levels = render(totalMs = 8_000L, outgoing, incoming) { now ->
            if (!controlling) return@render
            when (val step = ramp.step(now, incoming.positionMs, incoming.reportsPlaying(now), outgoing.remainingMs)) {
                is CrossfadeRampStep.Gains -> {
                    outgoing.gain = step.outgoing
                    incoming.gain = step.incoming
                }
                CrossfadeRampStep.Complete -> {
                    outgoing.gain = 0f
                    incoming.gain = 1f
                    controlling = false
                    completed = true
                }
                CrossfadeRampStep.IncomingSilent -> {
                    incoming.gain = 0f
                    controlling = false
                    silentAbort = true
                }
            }
        }
        return Run(levels, completed, silentAbort)
    }

    private fun simulateHandoff(
        prepareMs: Long,
        restartLatencyMs: Long,
        curve: CrossfadeCurve,
        trustReadyState: Boolean = false
    ): Run {
        val secondary = Deck(1_040.0, startPositionMs = 30_000L).apply {
            gain = 1f
            play(0L, 0L)
        }
        val primary = Deck(1_040.0, startPositionMs = 30_000L)
        val aligner = HandoffAligner()
        var ramp: CrossfadeRamp? = null
        var naiveStartMs = -1L
        var completed = false
        var residual = 0L
        val levels = render(totalMs = 3_000L, primary, secondary) { now ->
            if (completed) return@render
            if (now < prepareMs) return@render
            if (!primary.playing) primary.play(now, restartLatencyMs)
            if (trustReadyState) {
                if (naiveStartMs < 0L) naiveStartMs = now
                val gains = crossfadeGains(curve, ((now - naiveStartMs) / 240f))
                secondary.gain = gains.outgoing
                primary.gain = gains.incoming
                completed = now - naiveStartMs >= 240L
                return@render
            }
            val activeRamp = ramp
            if (activeRamp == null) {
                when (
                    val step = aligner.step(
                        now,
                        primary.positionMs,
                        primary.reportsPlaying(now),
                        secondary.positionMs,
                        200_000L
                    )
                ) {
                    is HandoffAlignStep.Seek -> primary.seek(step.positionMs, now, restartLatencyMs)
                    is HandoffAlignStep.Aligned -> {
                        residual = step.offsetMs
                        ramp = CrossfadeRamp(curve, fixedSpanMs = 240L, startTimeoutMs = 1_500L)
                    }
                    is HandoffAlignStep.Unaligned, HandoffAlignStep.PrimarySilent -> error("handoff failed: $step")
                    HandoffAlignStep.Wait -> Unit
                }
                return@render
            }
            when (val step = activeRamp.step(now, primary.positionMs, primary.reportsPlaying(now), secondary.remainingMs)) {
                is CrossfadeRampStep.Gains -> {
                    secondary.gain = step.outgoing
                    primary.gain = step.incoming
                }
                CrossfadeRampStep.Complete -> {
                    secondary.gain = 0f
                    primary.gain = 1f
                    completed = true
                }
                CrossfadeRampStep.IncomingSilent -> error("primary silent")
            }
        }
        return Run(levels, completed, residualOffsetMs = residual)
    }

    private fun render(totalMs: Long, first: Deck, second: Deck, tick: (Long) -> Unit): List<Double> {
        val levels = ArrayList<Double>()
        var sumSquares = 0.0
        var count = 0
        for (now in 0L until totalMs) {
            if (now % TICK_MS == 0L) tick(now)
            repeat(SAMPLES_PER_MS.toInt()) {
                val mixed = first.nextSample(now) + second.nextSample(now)
                sumSquares += mixed * mixed
                count++
            }
            if ((now + 1) % WINDOW_MS == 0L) {
                val rms = sqrt(sumSquares / count)
                levels += 20.0 * log10(rms.coerceAtLeast(1e-9) / NOMINAL_RMS)
                sumSquares = 0.0
                count = 0
            }
        }
        return levels
    }

    private fun assertNoArtifacts(levelsDb: List<Double>, label: String) {
        levelsDb.forEachIndexed { index, level ->
            assertTrue("$label window $index silent at $level dB", level > SILENCE_DB)
            assertTrue("$label window $index level $level dB", abs(level) <= MAX_LEVEL_DEVIATION_DB)
            if (index > 0) {
                val jump = abs(level - levelsDb[index - 1])
                assertTrue("$label window $index jumped $jump dB", jump <= MAX_STEP_DB)
            }
        }
    }

    private companion object {
        const val SAMPLE_RATE = 8_000.0
        const val SAMPLES_PER_MS = 8L
        const val TICK_MS = 25L
        const val WINDOW_MS = 25
        val NOMINAL_RMS = 1.0 / sqrt(2.0)
        const val SILENCE_DB = -30.0
        const val MAX_LEVEL_DEVIATION_DB = 0.5
        const val MAX_STEP_DB = 0.5
    }
}
