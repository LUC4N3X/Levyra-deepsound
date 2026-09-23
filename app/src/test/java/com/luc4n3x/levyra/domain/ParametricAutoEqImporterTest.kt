package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParametricAutoEqImporterTest {
    private fun success(text: String): ParametricEqProfile {
        val result = AutoEqImporter.parseParametric(text)
        assertTrue("Expected success, got $result", result is AutoEqImporter.ParametricParseResult.Success)
        return (result as AutoEqImporter.ParametricParseResult.Success).profile
    }

    private fun error(text: String): AutoEqImporter.ParametricParseError {
        val result = AutoEqImporter.parseParametric(text)
        assertTrue("Expected error, got $result", result is AutoEqImporter.ParametricParseResult.Error)
        return (result as AutoEqImporter.ParametricParseResult.Error).error
    }

    @Test
    fun `parses preamp supported filters and disabled bands`() {
        val profile = success(
            """
            # Name: Studio headphones
            Preamp: -6.3 dB
            Filter 1: ON LSC Fc 105 Hz Gain 4.2 dB Q 0.70
            Filter 2: OFF PK Fc 950 Hz Gain -2.5 dB Q 1.40
            Filter 3: ON HSC Fc 10000 Hz Gain -1.0 dB Q 0.71
            """.trimIndent()
        )

        assertEquals("Studio headphones", profile.name)
        assertEquals(-6.3f, profile.preampDb)
        assertEquals(listOf(ParametricFilterType.LOW_SHELF, ParametricFilterType.PEAK, ParametricFilterType.HIGH_SHELF), profile.bands.map { it.filterType })
        assertFalse(profile.bands[1].enabled)
        assertTrue(profile.id.startsWith(ParametricEqualizer.IMPORTED_PROFILE_PREFIX))
    }

    @Test
    fun `unsupported filters are rejected rather than mapped to peak`() {
        assertEquals(
            AutoEqImporter.ParametricParseError.UNSUPPORTED_FILTER,
            error("Filter 1: ON LPQ Fc 1000 Hz Gain 0 dB Q 0.7")
        )
    }

    @Test
    fun `duplicate filter numbers and unsafe values are rejected`() {
        val duplicate = "Filter 1: ON PK Fc 100 Hz Gain 1 dB Q 1\nFilter 1: ON PK Fc 200 Hz Gain 1 dB Q 1"
        assertEquals(AutoEqImporter.ParametricParseError.DUPLICATE_FILTER, error(duplicate))
        assertEquals(AutoEqImporter.ParametricParseError.INVALID_Q, error("Filter 1: ON PK Fc 100 Hz Gain 1 dB Q 0"))
        assertEquals(AutoEqImporter.ParametricParseError.INVALID_FREQUENCY, error("Filter 1: ON PK Fc -10 Hz Gain 1 dB Q 1"))
        assertEquals(AutoEqImporter.ParametricParseError.INVALID_GAIN, error("Filter 1: ON PK Fc 100 Hz Gain 99 dB Q 1"))
        assertEquals(AutoEqImporter.ParametricParseError.INVALID_FILTER, error("Filter 1: ON PK Fc NaN Hz Gain 1 dB Q 1"))
        assertEquals(
            AutoEqImporter.ParametricParseError.INVALID_FILTER,
            error("Unexpected header\nFilter 1: ON PK Fc 100 Hz Gain 1 dB Q 1")
        )
        assertEquals(
            AutoEqImporter.ParametricParseError.INVALID_PREAMP,
            error("Preamp: -2 dB\nPreamp: -3 dB\nFilter 1: ON PK Fc 100 Hz Gain 1 dB Q 1")
        )
    }

    @Test
    fun `input size and band count are bounded`() {
        assertEquals(
            AutoEqImporter.ParametricParseError.TOO_LARGE,
            error("x".repeat(AutoEqImporter.MAX_INPUT_CHARS + 1))
        )
        val filters = (1..(ParametricEqualizer.MAX_BANDS + 1)).joinToString("\n") { index ->
            "Filter $index: ON PK Fc ${100 + index} Hz Gain 0 dB Q 1"
        }
        assertEquals(AutoEqImporter.ParametricParseError.TOO_MANY_FILTERS, error(filters))
    }

    @Test
    fun `parametric parsing does not change graphic eq behavior`() {
        val graphic = AutoEqImporter.parse("GraphicEQ: 31 0; 62 1")
        val parametric = AutoEqImporter.parseParametric("GraphicEQ: 31 0; 62 1")

        assertTrue(graphic is AutoEqImporter.ParseResult.Success)
        assertEquals(
            AutoEqImporter.ParametricParseError.NO_PARAMETRIC_EQ,
            (parametric as AutoEqImporter.ParametricParseResult.Error).error
        )
    }
}
