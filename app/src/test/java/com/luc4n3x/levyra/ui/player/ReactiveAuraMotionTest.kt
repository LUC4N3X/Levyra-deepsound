package com.luc4n3x.levyra.ui.player

import androidx.compose.ui.graphics.Color
import com.luc4n3x.levyra.domain.ReactiveAuraPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactiveAuraMotionTest {

    @Test
    fun clockStaysStillWithoutAudioAndResumesWithSignal() {
        val clock = ReactiveAuraClock()
        val tuning = ReactiveAuraTuning.of(ReactiveAuraStyle(ReactiveAuraPreset.Pulse, 100))
        val silence = FloatArray(60)
        repeat(120) { clock.step(1f / 30f, tuning, silence, advancePhase = true) }
        assertEquals(0f, clock.phase, 0f)

        val loud = FloatArray(60) { 0.5f }
        repeat(30) { clock.step(1f / 30f, tuning, loud, advancePhase = true) }
        assertTrue(clock.phase > 0f)
        assertTrue(clock.loudness > 0f)
        assertTrue(clock.onset > 0f)

        repeat(300) { clock.step(1f / 30f, tuning, silence, advancePhase = true) }
        assertTrue(clock.settled)
        val silentPhase = clock.phase
        repeat(60) { clock.step(1f / 30f, tuning, silence, advancePhase = true) }
        assertEquals(silentPhase, clock.phase, 0f)
        assertEquals(0f, clock.loudness, 0f)
        assertEquals(0f, clock.onset, 0f)

        clock.step(1f / 30f, tuning, loud, advancePhase = true)
        assertTrue(clock.phase > silentPhase)
        val playingPhase = clock.phase
        repeat(300) { clock.step(1f / 30f, tuning, floatArrayOf(), advancePhase = false) }
        assertEquals(playingPhase, clock.phase, 0f)
        assertTrue(clock.settled)
    }

    @Test
    fun maximumPulseIntensityPreservesLoudnessAndTransientResponse() {
        val tuning = ReactiveAuraTuning.of(ReactiveAuraStyle(ReactiveAuraPreset.Pulse, 100))
        val steady = auraGlow(tuning, tuning.nearAlpha, 0.6f, 0f, 1f)
        val louder = auraGlow(tuning, tuning.nearAlpha, 0.9f, 0f, 1f)
        val attack = auraGlow(tuning, tuning.nearAlpha, 0.9f, 0.8f, 1f)

        assertTrue(louder > steady)
        assertTrue(attack > louder)
        assertTrue(attack < 0.62f)
        assertEquals(0f, auraGlow(tuning, tuning.nearAlpha, 1f, 1f, 0f), 0f)
    }

    @Test
    fun tuningRespectsPresetCharacteristicsAndIntensityScaling() {
        val calmDefault = ReactiveAuraTuning.of(ReactiveAuraStyle(ReactiveAuraPreset.Calm, 50))
        val flowDefault = ReactiveAuraTuning.of(ReactiveAuraStyle(ReactiveAuraPreset.Flow, 50))
        val pulseDefault = ReactiveAuraTuning.of(ReactiveAuraStyle(ReactiveAuraPreset.Pulse, 50))

        assertEquals(24, calmDefault.maxFps)
        assertEquals(30, flowDefault.maxFps)
        assertEquals(30, pulseDefault.maxFps)

        // Calm is slowest and lowest energy
        assertTrue(calmDefault.cycleSeconds > flowDefault.cycleSeconds)
        assertTrue(flowDefault.cycleSeconds > pulseDefault.cycleSeconds)
        assertEquals(0f, calmDefault.onsetGain, 0.001f)
        assertTrue(pulseDefault.onsetGain > flowDefault.onsetGain)

        // Intensity scaling increases gains within bounds
        val flowMin = ReactiveAuraTuning.of(ReactiveAuraStyle(ReactiveAuraPreset.Flow, 0))
        val flowMax = ReactiveAuraTuning.of(ReactiveAuraStyle(ReactiveAuraPreset.Flow, 100))
        assertTrue(flowMax.loudnessGain > flowMin.loudnessGain)
        assertTrue(flowMax.tempoGain > flowMin.tempoGain)
        assertTrue(flowMax.nearAlpha <= 0.55f)
        assertTrue(flowMax.farAlpha <= 0.32f)
    }

    @Test
    fun auraLoudnessComputesNormalizedEnergy() {
        assertEquals(0f, auraLoudness(floatArrayOf()), 0.0001f)

        val silent = FloatArray(64) { 0f }
        assertEquals(0f, auraLoudness(silent), 0.0001f)

        val quiet = FloatArray(64) { 0.05f }
        val loud = FloatArray(64) { 0.9f }
        val quietLevel = auraLoudness(quiet)
        val loudLevel = auraLoudness(loud)

        assertTrue(quietLevel > 0f)
        assertTrue(loudLevel > quietLevel)
        assertTrue(loudLevel <= 1f)
    }

    @Test
    fun reactiveAuraSignalTracksLoudnessAndOnsetAndSettles() {
        val signal = ReactiveAuraSignal()
        assertTrue(signal.settled)
        assertFalse(signal.hasSignal)

        // Feed quiet signal
        val quiet = FloatArray(128) { 0.02f }
        for (i in 0 until 10) {
            signal.advance(quiet, 0.033f)
        }
        assertTrue(signal.hasSignal)
        assertTrue(signal.loudness > 0f)

        // Sudden transient spike -> onset increases
        val loud = FloatArray(128) { 0.85f }
        signal.advance(loud, 0.033f)
        assertTrue(signal.onset > 0f)

        // Reset
        signal.reset()
        assertEquals(0f, signal.loudness, 0.0001f)
        assertEquals(0f, signal.onset, 0.0001f)
        assertFalse(signal.hasSignal)
        assertTrue(signal.settled)
    }

    @Test
    fun advanceAuraPhaseWrapsWithinTwoPi() {
        var phase = 0f
        val cycle = 100f
        val tempo = 1f

        // Advance small steps
        for (i in 0 until 100) {
            phase = advanceAuraPhase(phase, 1f, cycle, tempo)
            assertTrue(phase >= 0f)
            assertTrue(phase < AuraTwoPi)
        }

        // Advance huge step
        val wrapped = advanceAuraPhase(0.5f, 300f, cycle, tempo)
        assertTrue(wrapped >= 0f)
        assertTrue(wrapped < AuraTwoPi)
    }

    @Test
    fun placeAuraLobeProducesStableCoordinates() {
        val frame = AuraLobeFrame()
        for (lobe in AuraNearLobes) {
            placeAuraLobe(lobe, phase = 1.5f, direction = 1f, focusY = 0.5f, out = frame)
            assertTrue(frame.x in -0.5f..1.5f)
            assertTrue(frame.y in -0.5f..1.5f)
            assertTrue(frame.stretch >= 1f)
        }

        placeAuraLobe(AuraFarLobe, phase = 3.0f, direction = -1f, focusY = 0.5f, out = frame)
        assertTrue(frame.x in -0.5f..1.5f)
        assertTrue(frame.y in -0.5f..1.5f)

        placeAuraLobe(AuraSheenLobe, phase = 0.8f, direction = 1f, focusY = 0.2f, out = frame)
        assertTrue(frame.x in -0.5f..1.5f)
        assertTrue(frame.y in -0.5f..1.5f)
    }

    @Test
    fun auraToneClampsExcessiveSaturationAndElevatesDarkTones() {
        // Pure neon magenta (1f, 0f, 1f)
        val neonMagenta = Color(1f, 0f, 1f, 1f)
        val tonedMagenta = auraTone(neonMagenta)
        // Red and blue remain dominant, green is zero or lower, but saturation capped
        assertTrue(tonedMagenta.red > tonedMagenta.green)
        assertTrue(tonedMagenta.blue > tonedMagenta.green)

        // Pure black gets lifted above AuraMinValue (0.42f)
        val pureBlack = Color(0f, 0f, 0f, 1f)
        val tonedBlack = auraTone(pureBlack)
        assertTrue(tonedBlack.red >= 0.40f)
        assertTrue(tonedBlack.green >= 0.40f)
        assertTrue(tonedBlack.blue >= 0.40f)

        // Pure white gets capped below AuraMaxValue (0.80f)
        val pureWhite = Color(1f, 1f, 1f, 1f)
        val tonedWhite = auraTone(pureWhite)
        assertTrue(tonedWhite.red <= 0.81f)
        assertTrue(tonedWhite.green <= 0.81f)
        assertTrue(tonedWhite.blue <= 0.81f)
    }
}
