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
        val buffer = FloatArray(frames * channels)

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
        val limited = multitone(topHz = 16_000.0, seconds = 3)
        val metrics = streamInBlocks(analyzer, limited)

        assertFalse("metrics=${metrics.toSnapshot()}", metrics.bypassed)
        assertTrue("cutoff=${metrics.spectralCutoffHz}", metrics.spectralCutoffHz in 15_600f..16_400f)
        assertTrue("confidence=${metrics.deficitConfidence}", metrics.deficitConfidence >= 0.5f)
        assertTrue("gain=${metrics.adaptiveResidualGain}", metrics.adaptiveResidualGain > 0f && metrics.adaptiveResidualGain <= 1f)

        val fullBand = EnhancedAudioAnalyzer().apply { configure(sampleRate, channels) }
        val open = multitone(topHz = 21_000.0, seconds = 3)
        val fullMetrics = streamInBlocks(fullBand, open)
        assertTrue("full=${fullMetrics.toSnapshot()}", fullMetrics.bypassed)
        assertEquals(0f, fullMetrics.adaptiveResidualGain)
    }

    private fun streamInBlocks(target: EnhancedAudioAnalyzer, signal: FloatArray): MutableEnhancedAudioMetrics {
        val metrics = MutableEnhancedAudioMetrics()
        val blockFrames = 1_024
        var frame = 0
        val frames = signal.size / channels
        while (frame + blockFrames <= frames) {
            target.analyze(signal, frame * channels, blockFrames, 0L, metrics)
            frame += blockFrames
        }
        return metrics
    }

    private fun multitone(topHz: Double, seconds: Int): FloatArray {
        val frames = sampleRate * seconds
        val buffer = FloatArray(frames * channels)
        val random = java.util.Random(11)
        var frequency = 100.0
        while (frequency <= topHz) {
            val phase = random.nextDouble() * 2.0 * PI
            val amplitude = (0.02 * Math.sqrt(1_000.0 / frequency)).toFloat()
            for (i in 0 until frames) {
                val s = sin(2.0 * PI * frequency * i / sampleRate + phase).toFloat() * amplitude
                buffer[i * 2] += s
                buffer[i * 2 + 1] += s
            }
            frequency += 97.0
        }
        return buffer
    }

    @Test
    fun analyze_naturalTopEndRolloff_doesNotClassifyCodecCutoff() {
        val frames = 4096
        val buffer = FloatArray(frames * channels)
        for (i in 0 until frames) {
            val mono =
                sin(2.0 * PI * 10_000.0 * i / sampleRate).toFloat() * 0.35f +
                    sin(2.0 * PI * 12_500.0 * i / sampleRate).toFloat() * 0.22f +
                    sin(2.0 * PI * 15_500.0 * i / sampleRate).toFloat() * 0.08f +
                    sin(2.0 * PI * 18_000.0 * i / sampleRate).toFloat() * 0.03f +
                    sin(2.0 * PI * 20_000.0 * i / sampleRate).toFloat() * 0.015f
            buffer[i * 2] = mono
            buffer[i * 2 + 1] = mono
        }

        var metrics = analyzer.analyze(buffer, 0, frames, 0L)
        repeat(5) {
            metrics = analyzer.analyze(buffer, 0, frames, 0L)
        }

        assertTrue("Smooth spectral rolloff must stay bypassed", metrics.bypassed)
        assertEquals(0f, metrics.adaptiveResidualGain, 0.001f)
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
