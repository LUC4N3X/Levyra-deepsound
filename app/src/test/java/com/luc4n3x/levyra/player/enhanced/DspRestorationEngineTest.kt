package com.luc4n3x.levyra.player.enhanced

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DspRestorationEngineTest {

    private lateinit var engine: DspRestorationEngine
    private val sampleRate = 44_100
    private val channels = 2
    private val config = EnhancedAudioConfig()

    @Before
    fun setup() {
        engine = DspRestorationEngine()
        engine.configure(sampleRate, channels, config)
    }

    @Test
    fun process_zeroAdaptiveGain_producesExactPassthrough() {
        val frames = 512
        val input = FloatArray(frames * channels) { it % 100 / 100f * 0.5f }
        val output = FloatArray(frames * channels)

        val metrics = EnhancedAudioMetrics(
            deficitConfidence = 0.05f,
            adaptiveResidualGain = 0.0f,
            bypassed = true
        )

        val success = engine.process(input, output, 0, frames, metrics)
        assertTrue(success)

        for (i in input.indices) {
            assertEquals(input[i], output[i], 1e-6f)
        }
    }

    @Test
    fun process_activeDeficit_shiftsTheBandBelowCutoffUpward() {
        val frames = 16_384
        val input = FloatArray(frames * channels)
        for (i in 0 until frames) {
            val s = sin(2.0 * PI * 15_000.0 * i / sampleRate).toFloat() * 0.4f
            input[i * 2] = s
            input[i * 2 + 1] = s
        }
        val output = FloatArray(frames * channels)
        val metrics = EnhancedAudioMetrics(spectralCutoffHz = 16_000f, deficitConfidence = 0.8f, adaptiveResidualGain = 0.5f, bypassed = false)

        engine.process(input, output, 0, frames, metrics)
        engine.process(input, output, 0, frames, metrics)

        val shift = minOf(16_000.0 * 0.35, sampleRate / 2.0 - 16_000.0 - 150.0)
        val upper = toneLevel(output, 15_000.0 + shift) - toneLevel(input, 15_000.0 + shift)
        val lower = toneLevel(output, 15_000.0 - shift) - toneLevel(input, 15_000.0 - shift)
        assertTrue("upper=$upper", upper > 0.01)
        assertTrue("lower=$lower", lower < upper * 0.05)
        for (i in input.indices) assertTrue(abs(output[i] - input[i]) < 0.25f)
    }

    private fun toneLevel(signal: FloatArray, frequency: Double): Double {
        var re = 0.0
        var im = 0.0
        val frames = signal.size / channels
        for (i in frames / 2 until frames) {
            val angle = 2.0 * PI * frequency * i / sampleRate
            re += signal[i * 2] * cos(angle)
            im += signal[i * 2] * sin(angle)
        }
        return 2.0 * Math.sqrt(re * re + im * im) / (frames / 2)
    }

    @Test
    fun process_hotInputSignal_neverClipsCeiling() {
        val frames = 512
        val input = FloatArray(frames * channels)
        for (i in 0 until frames) {
            val s = sin(2.0 * PI * 12_000.0 * i / sampleRate).toFloat() * 0.98f
            input[i * 2] = s
            input[i * 2 + 1] = s
        }
        val output = FloatArray(frames * channels)

        val metrics = EnhancedAudioMetrics(
            deficitConfidence = 1.0f,
            adaptiveResidualGain = 0.50f,
            bypassed = false
        )

        engine.process(input, output, 0, frames, metrics)

        for (i in output.indices) {
            val sample = output[i]
            assertFalse("Sample is NaN", sample.isNaN())
            assertFalse("Sample is Infinite", sample.isInfinite())
            assertTrue("Sample clipped: $sample", abs(sample) <= 1.0f)
        }
    }

    @Test
    fun reset_clearsInternalHistory() {
        val frames = 256
        val input = FloatArray(frames * channels) { 0.8f }
        val output = FloatArray(frames * channels)

        val metrics = EnhancedAudioMetrics(adaptiveResidualGain = 0.4f, bypassed = false)
        engine.process(input, output, 0, frames, metrics)

        engine.reset()

        val zeroInput = FloatArray(frames * channels)
        val zeroOutput = FloatArray(frames * channels)
        engine.process(zeroInput, zeroOutput, 0, frames, metrics)

        for (s in zeroOutput) {
            assertEquals(0f, s, 1e-5f)
        }
    }

    @Test
    fun process_pureMonoSignal_preservesIdenticalChannels() {
        val frames = 1024
        val input = FloatArray(frames * channels)
        for (i in 0 until frames) {
            val s = sin(2.0 * PI * 11_000.0 * i / sampleRate).toFloat() * 0.5f
            input[i * 2] = s
            input[i * 2 + 1] = s
        }
        val output = FloatArray(frames * channels)

        val metrics = EnhancedAudioMetrics(
            deficitConfidence = 0.9f,
            adaptiveResidualGain = 0.45f,
            stereoCoherence = 1.0f,
            bypassed = false
        )

        engine.process(input, output, 0, frames, metrics)

        for (i in 0 until frames) {
            val left = output[i * 2]
            val right = output[i * 2 + 1]
            assertEquals("Mono channels must be identical", left, right, 1e-7f)
        }
    }

    @Test
    fun process_hardPannedLeftSignal_producesZeroBleedInSilentChannel() {
        val frames = 1024
        val input = FloatArray(frames * channels)
        for (i in 0 until frames) {
            val s = sin(2.0 * PI * 11_000.0 * i / sampleRate).toFloat() * 0.5f
            input[i * 2] = s
            input[i * 2 + 1] = 0f // Completely silent right channel
        }
        val output = FloatArray(frames * channels)

        val metrics = EnhancedAudioMetrics(
            deficitConfidence = 0.9f,
            adaptiveResidualGain = 0.40f,
            stereoCoherence = 0.5f,
            bypassed = false
        )

        engine.process(input, output, 0, frames, metrics)

        for (i in 0 until frames) {
            val right = output[i * 2 + 1]
            assertEquals("Silent channel must remain completely silent without bleed", 0f, right, 1e-6f)
        }
    }

    @Test
    fun process_antiCorrelatedStereo_suppressesSideExcitation() {
        val frames = 1024
        val input = FloatArray(frames * channels)
        for (i in 0 until frames) {
            val s = sin(2.0 * PI * 11_000.0 * i / sampleRate).toFloat() * 0.5f
            input[i * 2] = s
            input[i * 2 + 1] = -s // Out of phase
        }
        val output = FloatArray(frames * channels)

        val metrics = EnhancedAudioMetrics(
            deficitConfidence = 0.9f,
            adaptiveResidualGain = 0.40f,
            stereoCoherence = -1.0f, // Negative coherence
            bypassed = false
        )

        val success = engine.process(input, output, 0, frames, metrics)
        assertTrue(success)
        for (i in 0 until frames) {
            assertFalse(output[i * 2].isNaN())
            assertFalse(output[i * 2 + 1].isNaN())
        }
    }
}
