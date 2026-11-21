package com.luc4n3x.levyra.ui.library

import com.materialkolor.contrast.Contrast
import com.materialkolor.hct.Hct
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultLibraryColorTest {
    @Test
    fun missingOrNeutralArtworkPreservesTheSharedSurface() {
        val role = 0xFF53435C.toInt()
        val surface = 0xFF282429.toInt()
        val content = 0xFFEBDDF7.toInt()
        val neutralArtwork = Hct.from(210.0, 4.0, 55.0).toInt()

        assertEquals(surface, libraryArtworkContainer(0, role, surface, content))
        assertEquals(surface, libraryArtworkContainer(neutralArtwork, role, surface, content))
    }

    @Test
    fun saturatedCoversStayDistinctAndReadableInBothModes() {
        val covers = listOf(0xFFFF241F.toInt(), 0xFF007CFF.toInt(), 0xFF03BB52.toInt())
        val modes = listOf(
            Triple(0xFFE4D7ED.toInt(), 0xFFF8F4F8.toInt(), 0xFF21182B.toInt()),
            Triple(0xFF4E4157.toInt(), 0xFF282429.toInt(), 0xFFE4D7ED.toInt())
        )

        for ((role, surface, content) in modes) {
            val surfaceTone = Hct.fromInt(surface).tone
            val contentTone = Hct.fromInt(content).tone
            val colors = covers.map { libraryArtworkContainer(it, role, surface, content) }

            assertEquals(covers.size, colors.toSet().size)
            colors.forEach { color ->
                val tint = Hct.fromInt(color)
                assertNotEquals(surface, color)
                assertTrue(abs(tint.tone - surfaceTone) < 1.0)
                assertTrue(tint.chroma <= if (surfaceTone < 50.0) 11.0 else 14.0)
                assertTrue(Contrast.ratioOfTones(tint.tone, contentTone) >= 4.5)
            }
        }
    }
}
