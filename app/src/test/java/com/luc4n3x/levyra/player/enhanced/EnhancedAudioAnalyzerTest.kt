package com.luc4n3x.levyra.player.enhanced

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EnhancedAudioAnalyzerTest {

    private lateinit var analyzer: EnhancedAudioAnalyzer
    private val sampleRate = 44_100
    private val channels = 2

    @Before
    fun setup() {
        analyzer = EnhancedAudioAnalyzer(EnhancedAudioConfig())
        analyzer.configure(sampleRate, channels)
    }

    @Test
    fun analyze_silence_returnsZeroConfidenceAndBypasses() {
        val frames = 1024
        val buffer = FloatArray(frames * channels) // all zeros

        val metrics = analyzer.analyze(buffer, 0, frames, 0L)

        assertTrue(metrics.bypassed)
        assertEquals(EnhancedAudioBypassReason.INSUFFICIENT_CONFIDENCE, metrics.bypassReason)
        assertEquals(0f, metrics.deficitConfidence, 0.001f)
        assertEquals(0f, metrics.adaptiveResidualGain, 0.001f)
    }

    @Test
    fun analyze_pureLowFrequencySine_hasNoDeficit() {
        val frames = 2048
        val buffer = FloatArray(frames * channels)
        // 200 Hz sine wave
        for (i in 0 until frames) {
            val sample = sin(2.0 * PI * 200.0 * i / sampleRate).toFloat() * 0.5f
            buffer[i * 2] = sample
            buffer[i * 2 + 1] = sample
        }

        val metrics = analyzer.analyze(buffer, 0, frames, 0L)

        assertTrue(metrics.bypassed)
        assertEquals(0f, metrics.deficitConfidence, 0.05f)
        assertEquals(0f, metrics.adaptiveResidualGain, 0.001f)
    }

    @Test
    fun analyze_lossyCutoffSignal_detectsDeficitAboveThreshold() {
        val frames = 2048
        val buffer = FloatArray(frames * channels)
        // Signal with strong energy in 12-16 kHz (harmonics present) but completely dead above 18 kHz
        for (i in 0 until frames) {
            val s1 = sin(2.0 * PI * 12_000.0 * i / sampleRate).toFloat() * 0.3f
            val s2 = sin(2.0 * PI * 15_000.0 * i / sampleRate).toFloat() * 0.3f
            val mono = s1 + s2
            buffer[i * 2] = mono
            buffer[i * 2 + 1] = mono
        }

        // Run multiple blocks to allow temporal smoothing to converge
        var metrics = analyzer.analyze(buffer, 0, frames, 0L)
        repeat(3) {
            metrics = analyzer.analyze(buffer, 0, frames, 0L)
        }

        assertTrue(metrics.deficitConfidence >= 0.15f)
        assertFalse(metrics.bypassed)
        assertTrue(metrics.adaptiveResidualGain > 0f)
        assertTrue(metrics.adaptiveResidualGain <= 0.50f)
    }

    @Test
    fun analyze_stereoCoherence_computesAccurateCorrelation() {
        val frames = 1024
        val monoBuffer = FloatArray(frames * channels)
        for (i in 0 until frames) {
            val s = sin(2.0 * PI * 1000.0 * i / sampleRate).toFloat() * 0.5f
            monoBuffer[i * 2] = s
            monoBuffer[i * 2 + 1] = s
        }

        val monoMetrics = analyzer.analyze(monoBuffer, 0, frames, 0L)
        assertEquals(1.0f, monoMetrics.stereoCoherence, 0.01f)
    }

    @Test
    fun reset_clearsInternalMemory() {
        val frames = 1024
        val buffer = FloatArray(frames * channels) { 0.5f }

        analyzer.analyze(buffer, 0, frames, 0L)
        analyzer.reset()

        val afterReset = analyzer.analyze(FloatArray(frames * channels), 0, frames, 0L)
        assertEquals(0f, afterReset.deficitConfidence, 0.001f)
        assertTrue(afterReset.bypassed)
    }
}
