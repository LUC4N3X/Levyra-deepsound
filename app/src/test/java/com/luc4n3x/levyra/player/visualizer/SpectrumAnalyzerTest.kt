package com.luc4n3x.levyra.player.visualizer

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumAnalyzerTest {
    private val rate = 48_000.0
    private val analyzer = SpectrumAnalyzer()

    private fun tone(vararg components: Pair<Double, Double>): FloatArray = FloatArray(analyzer.fftSize) { index ->
        components.sumOf { (frequency, amplitude) -> amplitude * sin(2.0 * PI * frequency * index / rate) }.toFloat()
    }

    private fun bandContaining(edges: FloatArray, frequency: Double): Int =
        (0 until edges.size - 1).first { edges[it] <= frequency && frequency < edges[it + 1] }

    @Test
    fun binCenteredFullScaleSineReadsZeroDbfsInPeakAndThirdOctaveBands() {
        val frequency = 85 * rate / analyzer.fftSize
        analyzer.analyze(tone(frequency to 1.0), rate)
        val edges = SpectrumAnalyzer.logEdges(48, 30f, 16_000f)
        val peaks = FloatArray(48)
        analyzer.peakBandsDb(edges, peaks)
        assertEquals(0f, peaks[bandContaining(edges, frequency)], 0.05f)
        val rta = FloatArray(31)
        analyzer.bandPowerDb(SpectrumAnalyzer.thirdOctaveEdgesHz, rta)
        assertEquals(0f, rta[17], 0.05f)
    }

    @Test
    fun levelsScaleLinearlyInDecibels() {
        val frequency = 170 * rate / analyzer.fftSize
        analyzer.analyze(tone(frequency to 0.1), rate)
        val edges = SpectrumAnalyzer.logEdges(48, 30f, 16_000f)
        val peaks = FloatArray(48)
        analyzer.peakBandsDb(edges, peaks)
        assertEquals(-20f, peaks[bandContaining(edges, frequency)], 0.05f)
    }

    @Test
    fun offBinSineStaysWithinHannScallopingLoss() {
        val frequency = 85.5 * rate / analyzer.fftSize
        analyzer.analyze(tone(frequency to 1.0), rate)
        val edges = SpectrumAnalyzer.logEdges(48, 30f, 16_000f)
        val peaks = FloatArray(48)
        analyzer.peakBandsDb(edges, peaks)
        val value = peaks[bandContaining(edges, frequency)]
        assertTrue("$value", value in -1.5f..0.05f)
        val rta = FloatArray(31)
        analyzer.bandPowerDb(SpectrumAnalyzer.thirdOctaveEdgesHz, rta)
        assertEquals(0f, rta[17], 0.1f)
    }

    @Test
    fun distantBandsStayQuietAndSilenceHitsTheFloor() {
        analyzer.analyze(tone(100.0 to 1.0, 5_000.0 to 1.0), rate)
        val rta = FloatArray(31)
        analyzer.bandPowerDb(SpectrumAnalyzer.thirdOctaveEdgesHz, rta)
        assertTrue("${rta[17]}", rta[17] < -60f)
        assertEquals(0f, rta[7], 0.3f)
        analyzer.analyze(FloatArray(analyzer.fftSize), rate)
        analyzer.bandPowerDb(SpectrumAnalyzer.thirdOctaveEdgesHz, rta)
        assertTrue(rta.all { it == SpectrumAnalyzer.FLOOR_DB })
    }

    @Test
    fun thirdOctaveCentersFollowTheIsoSeries() {
        assertEquals(1_000f, SpectrumAnalyzer.thirdOctaveCentersHz[17], 0.01f)
        assertEquals(19.7f, SpectrumAnalyzer.thirdOctaveCentersHz[0], 0.1f)
        assertEquals(20_159f, SpectrumAnalyzer.thirdOctaveCentersHz[30], 1f)
    }

    @Test
    fun processorReleasesSmoothlyAndHoldsPeaks() {
        val processor = SpectrumProcessor()
        val frequency = 85 * rate / analyzer.fftSize
        tone(frequency to 1.0).copyInto(processor.buffer())
        processor.analyze(rate, 0.016f)
        val frame = SpectrumFrame()
        processor.publishInto(frame)
        val band = frame.bars.indices.maxBy { frame.bars[it] }
        val loud = frame.bars[band]
        assertEquals(1f, loud, 0.01f)
        processor.decay(0.1f)
        processor.publishInto(frame)
        assertTrue(frame.bars[band] < loud)
        assertEquals(loud, frame.barPeaks[band], 0.0001f)
        repeat(20) { processor.decay(0.1f) }
        processor.publishInto(frame)
        assertTrue(frame.barPeaks[band] < loud)
        assertTrue(frame.bars[band] < 0.01f)
    }
}
