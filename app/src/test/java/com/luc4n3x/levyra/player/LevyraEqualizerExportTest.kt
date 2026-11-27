package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.AutoEqImporter
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.ParametricBiquad
import com.luc4n3x.levyra.domain.ParametricEqBand
import com.luc4n3x.levyra.domain.ParametricEqProfile
import com.luc4n3x.levyra.domain.ParametricFilterType
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraEqualizerExportTest {
    private val levels = listOf(40, 25, 10, 0, -10, -20, 0, 15, 30, 50)

    @Test
    fun graphicProfileMatchesTheProcessorFilters() {
        val profile = LevyraEqualizerExport.graphicProfile(levels, bassBoost = 50, preampDb = -2f, name = "Mine")
        assertEquals(LevyraEqualizerAudioProcessor.BAND_COUNT, profile.bands.size)
        val first = profile.bands.first()
        assertEquals(31f, first.frequencyHz, 0f)
        assertEquals(LevyraEqualizerAudioProcessor.Q, first.q, 0f)
        assertEquals(ParametricFilterType.PEAK, first.filterType)
        val expectedFirst = 0.4f * LevyraEqualizerAudioProcessor.MAX_BAND_DB + 0.5f * LevyraEqualizerAudioProcessor.BASS_MAX_DB
        assertEquals(expectedFirst, first.gainDb, 0.0001f)
        val highest = profile.bands.maxOf { it.gainDb }
        assertEquals(equalizerInputGainDb(-2f, highest), profile.preampDb, 0.0001f)
    }

    @Test
    fun parametricTextRoundTripsThroughTheImporter() {
        val profile = LevyraEqualizerExport.graphicProfile(levels, bassBoost = 30, preampDb = 0f, name = "Mine")
        val text = LevyraEqualizerExport.parametricEqText(profile)
        val parsed = AutoEqImporter.parseParametric(text) as AutoEqImporter.ParametricParseResult.Success
        assertEquals(profile.preampDb, parsed.profile.preampDb, 0.05f)
        profile.bands.zip(parsed.profile.bands).forEach { (expected, actual) ->
            assertEquals(expected.frequencyHz, actual.frequencyHz, 0.05f)
            assertEquals(expected.gainDb, actual.gainDb, 0.05f)
            assertEquals(expected.q, actual.q, 0.005f)
            assertEquals(expected.filterType, actual.filterType)
        }
    }

    @Test
    fun parametricTextKeepsShelvesAndDisabledBands() {
        val profile = ParametricEqProfile(
            id = "parametric_custom_test",
            name = "Shelves",
            preampDb = -4.25f,
            bands = listOf(
                ParametricEqBand(105f, 5.5f, 0.7f, ParametricFilterType.LOW_SHELF),
                ParametricEqBand(2_450.5f, -3f, 2.4f, ParametricFilterType.PEAK, enabled = false),
                ParametricEqBand(10_000f, 2f, 0.7f, ParametricFilterType.HIGH_SHELF)
            )
        )
        val text = LevyraEqualizerExport.parametricEqText(profile)
        assertTrue(text, text.startsWith("Preamp: -4.3 dB\n"))
        assertTrue(text, "Filter 1: ON LSC Fc 105 Hz Gain 5.5 dB Q 0.70" in text)
        assertTrue(text, "Filter 2: OFF PK Fc 2450.5 Hz Gain -3.0 dB Q 2.40" in text)
        assertTrue(text, "Filter 3: ON HSC Fc 10000 Hz Gain 2.0 dB Q 0.70" in text)
    }

    @Test
    fun graphicTextIsTheExactCascadeResponseWithPreamp() {
        val profile = LevyraEqualizerExport.graphicProfile(levels, bassBoost = 0, preampDb = -3f, name = "Mine")
        val text = LevyraEqualizerExport.graphicEqText(profile)
        val points = text.removePrefix("GraphicEQ: ").trim().split("; ").map { pair ->
            val (frequency, gain) = pair.split(' ')
            frequency.toFloat() to gain.toFloat()
        }
        assertEquals(LevyraEqualizerExport.GRAPHIC_POINTS, points.size)
        assertEquals(20f, points.first().first, 0f)
        assertEquals(20_000f, points.last().first, 0f)
        assertTrue(points.zipWithNext().all { (a, b) -> b.first > a.first })
        val expected = FloatArray(points.size)
        ParametricBiquad.responseDb(profile, LevyraEqualizerExport.graphicFrequenciesHz, LevyraEqualizerExport.RESPONSE_SAMPLE_RATE, expected)
        points.forEachIndexed { index, (_, gain) -> assertEquals(expected[index], gain, 0.051f) }
        val parsed = AutoEqImporter.parse(text) as AutoEqImporter.ParseResult.Success
        assertEquals(LevyraEqualizerExport.GRAPHIC_POINTS, parsed.profile.pointCount)
    }

    @Test
    fun flatCurveHasNoNegativeZeroAndIgnoresDeviceLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val profile = LevyraEqualizerExport.graphicProfile(List(10) { 0 }, 0, 0f, "Flat")
            val graphic = LevyraEqualizerExport.graphicEqText(profile)
            val parametric = LevyraEqualizerExport.parametricEqText(profile)
            assertFalse(graphic, "-0.0" in graphic || "," in graphic.removePrefix("GraphicEQ: ").replace("; ", ""))
            assertTrue(parametric, parametric.startsWith("Preamp: 0.0 dB"))
            assertFalse(parametric, "-0.0" in parametric)
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun currentProfilePrefersActiveParametricAndNeedsAnEnabledEqualizer() {
        assertNull(LevyraEqualizerExport.currentProfile(LevyraAudioSettings(equalizerEnabled = false), "x"))
        val graphic = LevyraEqualizerExport.currentProfile(LevyraAudioSettings(equalizerEnabled = true), "x")
        assertEquals(LevyraEqualizerAudioProcessor.BAND_COUNT, graphic?.bands?.size)
        val parametric = ParametricEqProfile(
            id = "parametric_custom_a",
            name = "A",
            preampDb = -1f,
            bands = listOf(ParametricEqBand(1_000f, 3f, 1f, ParametricFilterType.PEAK))
        )
        val settings = LevyraAudioSettings(parametricEqualizerEnabled = true, activeParametricProfile = parametric)
        assertEquals(parametric.normalized(), LevyraEqualizerExport.currentProfile(settings, "x"))
    }

    @Test
    fun fileNamesAreSafe() {
        assertEquals("My EQ v2 GraphicEQ.txt", LevyraEqualizerExport.fileName("My EQ / v2", EqualizerExportFormat.GRAPHIC_EQ))
        assertEquals("Levyra EQ ParametricEQ.txt", LevyraEqualizerExport.fileName("  ", EqualizerExportFormat.PARAMETRIC_EQ))
    }
}
