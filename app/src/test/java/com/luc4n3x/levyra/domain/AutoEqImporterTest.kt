package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoEqImporterTest {

    private fun success(text: String): AutoEqImporter.ImportedProfile {
        val result = AutoEqImporter.parse(text)
        assertTrue("Expected success, got $result", result is AutoEqImporter.ParseResult.Success)
        return (result as AutoEqImporter.ParseResult.Success).profile
    }

    private fun error(text: String): AutoEqImporter.ParseError {
        val result = AutoEqImporter.parse(text)
        assertTrue("Expected error, got $result", result is AutoEqImporter.ParseResult.Error)
        return (result as AutoEqImporter.ParseResult.Error).error
    }

    @Test
    fun `valid graphic eq with exact levyra bands`() {
        val profile = success(
            "GraphicEQ: 31 0; 62 3; 125 -2.4; 250 0; 500 4.8; 1000 0; 2000 -6; 4000 0; 8000 6; 16000 12"
        )

        assertEquals(listOf(0, 25, -20, 0, 40, 0, -50, 0, 50, 100), profile.bandLevels)
        assertEquals(10, profile.bandGainDb.size)
        assertEquals(6f, profile.bandGainDb[8])
        assertEquals(12f, profile.bandGainDb[9])
        assertFalse(profile.clamped)
        assertFalse(profile.interpolated)
        assertEquals(0, profile.skippedPoints)
        assertEquals(10, profile.pointCount)
    }

    @Test
    fun `preamp line is parsed and applied`() {
        val profile = success("Preamp: -4.5 dB\nGraphicEQ: 31 0; 62 0")

        assertEquals(-4.5f, profile.preampDb)
    }

    @Test
    fun `preamp clamps to levyra limits`() {
        val tooHigh = success("Preamp: 9 dB\nGraphicEQ: 31 0; 62 0")
        val tooLow = success("Preamp: -30 dB\nGraphicEQ: 31 0; 62 0")

        assertEquals(3f, tooHigh.preampDb)
        assertTrue(tooHigh.clamped)
        assertEquals(-12f, tooLow.preampDb)
        assertTrue(tooLow.clamped)
    }

    @Test
    fun `exact input values are preserved on levyra bands`() {
        val profile = success("GraphicEQ: 20 -4; 31 5; 62 2; 125 -3; 250 1; 500 -1; 1000 0; 2000 2; 4000 -2; 8000 3; 16000 4; 20000 0")

        assertFalse(profile.interpolated)
        assertEquals(Math.round(5.0 * 100 / 12).toInt(), profile.bandLevels[0])
        assertEquals(Math.round(2.0 * 100 / 12).toInt(), profile.bandLevels[1])
        assertEquals(Math.round(4.0 * 100 / 12).toInt(), profile.bandLevels[9])
    }

    @Test
    fun `interpolates gain in log frequency space`() {
        val profile = success("GraphicEQ: 20 0; 40 6")

        val expectedDb = 6.0 * (Math.log10(31.0) - Math.log10(20.0)) / (Math.log10(40.0) - Math.log10(20.0))
        val expectedLevel = Math.round(expectedDb / 12.0 * 100.0).toInt()
        assertEquals(expectedLevel, profile.bandLevels[0])
        assertTrue(profile.interpolated)
    }

    @Test
    fun `endpoint frequencies hold nearest point gain`() {
        val below = success("GraphicEQ: 40 3; 62 6")
        val above = success("GraphicEQ: 31 2; 62 0")

        assertEquals(Math.round(3.0 * 100 / 12).toInt(), below.bandLevels[0])
        assertEquals(0, above.bandLevels[9])
    }

    @Test
    fun `gain beyond levyra band limit is clamped`() {
        val profile = success("GraphicEQ: 31 20; 62 0")
        val negative = success("GraphicEQ: 31 -15; 62 0")

        assertEquals(100, profile.bandLevels[0])
        assertEquals(12f, profile.bandGainDb[0])
        assertTrue(profile.clamped)
        assertEquals(-100, negative.bandLevels[0])
        assertTrue(negative.clamped)
    }

    @Test
    fun `malformed graphic eq is rejected`() {
        assertEquals(AutoEqImporter.ParseError.INVALID_POINT, error("GraphicEQ: hello world"))
        assertEquals(AutoEqImporter.ParseError.INVALID_POINT, error("GraphicEQ: 31 0 0; 62 0"))
        assertEquals(AutoEqImporter.ParseError.INVALID_POINT, error("GraphicEQ:"))
    }

    @Test
    fun `empty input is rejected`() {
        assertEquals(AutoEqImporter.ParseError.EMPTY, error(""))
        assertEquals(AutoEqImporter.ParseError.EMPTY, error("   \n  "))
    }

    @Test
    fun `nan and infinity are rejected`() {
        assertEquals(AutoEqImporter.ParseError.NON_FINITE_VALUE, error("GraphicEQ: 31 NaN; 62 0"))
        assertEquals(AutoEqImporter.ParseError.NON_FINITE_VALUE, error("GraphicEQ: 31 Infinity; 62 0"))
        assertEquals(AutoEqImporter.ParseError.NON_FINITE_VALUE, error("GraphicEQ: 31 0; 62 -Infinity"))
    }

    @Test
    fun `duplicate frequencies use the last occurrence`() {
        val profile = success("GraphicEQ: 31 2; 31 4; 62 0")

        assertEquals(Math.round(4.0 * 100 / 12).toInt(), profile.bandLevels[0])
        assertEquals(2, profile.pointCount)
    }

    @Test
    fun `impossible frequency is rejected and high frequency is skipped safely`() {
        assertEquals(AutoEqImporter.ParseError.INVALID_POINT, error("GraphicEQ: 0 5; 62 0"))

        val profile = success("GraphicEQ: 31 0; 62 0; 200000 5")
        assertEquals(1, profile.skippedPoints)
        assertEquals(2, profile.pointCount)
    }

    @Test
    fun `partially valid input keeps usable profile and skips unknown lines`() {
        val profile = success("some junk line\nGraphicEQ: 31 0; 62 6\nFilter 1: ON PK Fc 100 Hz Gain -2 dB Q 0.7")

        assertEquals(2, profile.skippedPoints)
        assertEquals(0, profile.bandLevels[0])
        assertEquals(50, profile.bandLevels[1])
    }

    @Test
    fun `unknown lines without graphic eq are rejected`() {
        assertEquals(AutoEqImporter.ParseError.NO_GRAPHIC_EQ, error("hello\nworld"))
        assertEquals(AutoEqImporter.ParseError.NO_GRAPHIC_EQ, error("Preamp: -4 dB"))
    }

    @Test
    fun `insufficient points are rejected`() {
        assertEquals(AutoEqImporter.ParseError.INSUFFICIENT_POINTS, error("GraphicEQ: 31 0"))
    }

    @Test
    fun `too many points are rejected`() {
        val points = (0..200).joinToString("; ") { "$it 0" }
        assertEquals(AutoEqImporter.ParseError.TOO_MANY_POINTS, error("GraphicEQ: $points"))
    }

    @Test
    fun `oversized input is rejected`() {
        val text = "x".repeat(AutoEqImporter.MAX_INPUT_CHARS + 1)
        assertEquals(AutoEqImporter.ParseError.TOO_LARGE, error(text))
    }

    @Test
    fun `profile name is extracted from comment`() {
        val profile = success("# Name: Sennheiser HD600\nGraphicEQ: 31 0; 62 0")

        assertEquals("Sennheiser HD600", profile.name)
    }

    @Test
    fun `trailing semicolon and windows line endings are accepted`() {
        val profile = success("Preamp: -2 dB\r\nGraphicEQ: 31 0; 62 0;\r\n")

        assertEquals(-2f, profile.preampDb)
        assertEquals(2, profile.pointCount)
    }

    @Test
    fun `custom preset id is stable and namespaced`() {
        val profile = success("GraphicEQ: 31 1; 62 2")

        val first = AutoEqImporter.customPresetId("My tune", profile)
        val second = AutoEqImporter.customPresetId("My tune", profile)
        val other = AutoEqImporter.customPresetId("Other", profile)

        assertEquals(first, second)
        assertTrue(first.startsWith(LevyraAudioPresets.CUSTOM_PRESET_PREFIX))
        assertTrue(first != other)
        assertNotNull(first)
    }
}
