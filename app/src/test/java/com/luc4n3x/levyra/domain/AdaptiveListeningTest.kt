package com.luc4n3x.levyra.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveListeningTest {
    private var clock = 0L
    private val random = Random(7)

    private fun engine(max: Float = 4f, sensitivity: AdaptiveSensitivity = AdaptiveSensitivity.BALANCED, stored: Float = Float.NaN) =
        AdaptiveListeningEngine(sensitivity, max, stored)

    private fun window(
        level: Float,
        jitter: Float = 0f,
        headphones: Boolean = true,
        output: Float = -20f,
        clipped: Int = 0
    ): AmbientWindow {
        clock += AdaptiveListeningEngine.WINDOW_MS
        val frames = FloatArray(10) { level + if (jitter == 0f) 0f else (random.nextFloat() * 2f - 1f) * jitter }
        return AmbientWindow(frames, clipped, output, headphones, clock)
    }

    private fun AdaptiveListeningEngine.feed(count: Int, block: (Int) -> AmbientWindow): List<AdaptiveListeningSnapshot> =
        (0 until count).map { onWindow(block(it)) }

    @Test
    fun meterMeasuresSineRmsWithinHalfDecibel() {
        val rate = 16_000
        val meter = AmbientFrameMeter(rate)
        val amplitude = 0.25
        val samples = ShortArray(rate) { index -> (sin(2.0 * PI * 1000.0 * index / rate) * amplitude * 32767).toInt().toShort() }
        val levels = ArrayList<Float>()
        meter.process(samples, samples.size) { level, _ -> levels += level }
        val expected = (20.0 * log10(amplitude / sqrt(2.0))).toFloat()
        assertEquals(10, levels.size)
        levels.drop(1).forEach { assertTrue("level $it vs $expected", abs(it - expected) < 0.5f) }
    }

    @Test
    fun meterRejectsDcOffsetAndFlagsClipping() {
        val meter = AmbientFrameMeter(16_000)
        val dc = ShortArray(16_000) { 8_000 }
        val levels = ArrayList<Float>()
        meter.process(dc, dc.size) { level, _ -> levels += level }
        assertTrue(levels.last() < -80f)
        val clipped = ShortArray(1_600) { if (it % 2 == 0) 32_767 else -32_768 }
        var flagged = false
        meter.process(clipped, clipped.size) { _, clip -> flagged = flagged || clip }
        assertTrue(flagged)
    }

    @Test
    fun quietRoomNeverLifts() {
        val monitor = engine()
        val results = monitor.feed(60) { window(-62f, jitter = 1.5f) }
        assertTrue(results.all { it.boostDb == 0f })
        assertEquals(AdaptiveListeningCondition.Listening, results.last().condition)
    }

    @Test
    fun noiseRiseLiftsGraduallyWithinLimitAndLatency() {
        val monitor = engine(max = 4f)
        monitor.feed(20) { window(-60f, jitter = 1f) }
        val rising = monitor.feed(60) { window(-38f, jitter = 1f) }
        val firstLift = rising.indexOfFirst { it.boostDb > 0f }
        assertTrue("first lift after $firstLift windows", firstLift in 1..10)
        val full = rising.indexOfFirst { it.boostDb >= 3.5f }
        assertTrue("full lift after $full windows", full in 1..40)
        assertTrue(rising.all { it.boostDb <= 4f })
        rising.zipWithNext().forEach { (a, b) -> assertTrue(b.boostDb - a.boostDb <= AdaptiveListeningEngine.MAX_RAISE_PER_DECISION_DB) }
    }

    @Test
    fun fluctuatingNoiseDoesNotPumpTheVolume() {
        val monitor = engine(max = 6f)
        monitor.feed(20) { window(-60f, jitter = 1f) }
        monitor.feed(60) { window(-40f, jitter = 1f) }
        val steady = monitor.feed(120) { window(-40f + (random.nextFloat() * 4f - 2f), jitter = 2f) }
        val changes = steady.zipWithNext().count { (a, b) -> a.boostDb != b.boostDb }
        assertTrue("boost changed $changes times", changes <= 2)
    }

    @Test
    fun noiseDropLowersTheLiftQuickerThanItRose() {
        val monitor = engine(max = 4f)
        monitor.feed(20) { window(-60f) }
        monitor.feed(60) { window(-38f) }
        val falling = monitor.feed(30) { window(-60f) }
        val zero = falling.indexOfFirst { it.boostDb == 0f }
        assertTrue("back to zero after $zero windows", zero in 0..15)
    }

    @Test
    fun speakerOutputDisablesAdaptationImmediately() {
        val monitor = engine()
        monitor.feed(20) { window(-60f) }
        monitor.feed(60) { window(-38f) }
        val speaker = monitor.onWindow(window(-38f, headphones = false))
        assertEquals(AdaptiveListeningCondition.SpeakerOutput, speaker.condition)
        assertEquals(0f, speaker.boostDb)
    }

    @Test
    fun playbackBleedIsDetectedAndBlocksLift() {
        val monitor = engine()
        val results = monitor.feed(30) { index ->
            val output = -24f + 6f * sin(index / 2.0).toFloat()
            window(output - 18f, output = output)
        }
        assertEquals(AdaptiveListeningCondition.PlaybackBleed, results.last().condition)
        assertTrue(results.drop(12).all { it.boostDb == 0f })
    }

    @Test
    fun independentNoiseIsNotMistakenForBleed() {
        val monitor = engine()
        val results = monitor.feed(40) { window(-50f + random.nextFloat() * 2f, output = -24f + 6f * sin(it / 2.0).toFloat()) }
        assertTrue(results.none { it.condition == AdaptiveListeningCondition.PlaybackBleed })
    }

    @Test
    fun unstableAndUnreliableConditionsHoldOrDrop() {
        val unstable = engine()
        unstable.feed(20) { window(-60f) }
        val swings = unstable.feed(12) { if (it % 2 == 0) window(-75f) else window(-25f) }
        assertEquals(AdaptiveListeningCondition.Unstable, swings.last().condition)
        assertEquals(0f, swings.last().boostDb)

        val silent = engine()
        val muted = silent.feed(4) { window(-130f) }
        assertEquals(AdaptiveListeningCondition.MicrophoneSilent, muted.last().condition)

        val loud = engine()
        assertEquals(AdaptiveListeningCondition.Overload, loud.onWindow(window(-3f, clipped = 8)).condition)
    }

    @Test
    fun storedQuietReferenceAllowsLiftWhenSessionStartsNoisy() {
        val monitor = engine(stored = -60f)
        val results = monitor.feed(40) { window(-40f, jitter = 1f) }
        assertTrue(results.last().boostDb > 0f)
        assertEquals(-60f, results.last().referenceDb, 0.01f)
    }

    @Test
    fun sensitivityChangesTheAmountOfLift() {
        assertEquals(0f, engine(sensitivity = AdaptiveSensitivity.LOW).desiredBoostDb(9f))
        assertTrue(engine(sensitivity = AdaptiveSensitivity.HIGH).desiredBoostDb(9f) > engine().desiredBoostDb(9f))
        assertEquals(6f, engine(max = 6f, sensitivity = AdaptiveSensitivity.HIGH).desiredBoostDb(60f))
    }

    @Test
    fun processingCostStaysBounded() {
        val meter = AmbientFrameMeter(16_000)
        val second = ShortArray(16_000) { (random.nextInt(-3000, 3000)).toShort() }
        val monitor = engine()
        val started = System.nanoTime()
        repeat(3600) {
            val frames = FloatArray(10)
            var count = 0
            meter.process(second, second.size) { level, _ -> if (count < 10) frames[count++] = level }
            clock += 1000L
            monitor.onWindow(AmbientWindow(frames, 0, -20f, true, clock))
        }
        val perSecondMicros = (System.nanoTime() - started) / 1000.0 / 3600.0
        println("Adaptive listening analysis cost: %.1f us per second of audio".format(perSecondMicros))
        assertTrue(perSecondMicros < 5_000.0)
    }
}
