package com.luc4n3x.levyra.player.enhanced

import kotlin.math.PI
import kotlin.math.abs
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
    fun process_activeDeficit_generatesBoundedResidual() {
        val frames = 1024
        val input = FloatArray(frames * channels)
        // Signal with energy in 10 kHz
        for (i in 0 until frames) {
            val s = sin(2.0 * PI * 10_000.0 * i / sampleRate).toFloat() * 0.4f
            input[i * 2] = s
            input[i * 2 + 1] = s
        }
        val output = FloatArray(frames * channels)

        val metrics = EnhancedAudioMetrics(
            deficitConfidence = 0.8f,
            adaptiveResidualGain = 0.40f,
            bypassed = false
        )

        // Warm up filters
        engine.process(input, output, 0, frames, metrics)
        val success = engine.process(input, output, 0, frames, metrics)
        assertTrue(success)

        var differenceDetected = false
        for (i in input.indices) {
            val diff = abs(output[i] - input[i])
            if (diff > 1e-4f) differenceDetected = true
            // Residual must be bounded and subtle
            assertTrue("Output diverged too much: diff=$diff", diff < 0.25f)
        }
        assertTrue("Expected subtle residual to be added", differenceDetected)
    }

    @Test
    fun process_hotInputSignal_neverClipsCeiling() {
        val frames = 512
        // Hot input right below 1.0 (0.98f)
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
}
