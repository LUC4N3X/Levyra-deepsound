package com.luc4n3x.levyra.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LyricsFontInspectorTest {
    @get:Rule val folder = TemporaryFolder()

    private fun inspect(bytes: ByteArray) = LyricsFontInspector.inspect(File(folder.root, "font.bin").apply { writeBytes(bytes) })

    @Test
    fun trueTypeFontIsAccepted() {
        val result = inspect(LyricsFontTestFonts.trueType("Noto Sans TC"))!!
        assertEquals(LyricsFontFormat.TRUE_TYPE, result.format)
        assertEquals("Noto Sans TC", result.familyName)
        assertNull(result.variableWeight)
    }

    @Test
    fun openTypeCffFontIsAccepted() {
        assertEquals(LyricsFontFormat.OPEN_TYPE, inspect(LyricsFontTestFonts.openType())!!.format)
    }

    @Test
    fun variableWeightAxisIsDetected() {
        assertEquals(100..900, inspect(LyricsFontTestFonts.trueType(weightAxis = 100..900))!!.variableWeight)
    }

    @Test
    fun corruptedOrUnsupportedFilesAreRejected() {
        val valid = LyricsFontTestFonts.trueType()
        assertNull(inspect("not a font at all".toByteArray()))
        assertNull(inspect(ByteArray(0)))
        assertNull(inspect(valid.copyOf(40)))
        assertNull(inspect(byteArrayOf(0x74, 0x74, 0x63, 0x66) + valid.copyOfRange(4, valid.size)))
        assertNull(inspect(LyricsFontTestFonts.sfnt(0x00010000, "X", null, outlines = emptyList())))
        assertNull(inspect(LyricsFontTestFonts.sfnt(0x00010000, "X", null, listOf("glyf", "loca"), headMagic = 0)))
    }

    @Test
    fun fontNamesAreSanitized() {
        assertEquals("My Font", sanitizeLyricsFontName("  My\u0000\n  Font  "))
        assertEquals(64, sanitizeLyricsFontName("a".repeat(200)).length)
    }
}
