package com.luc4n3x.levyra.ui.artwork

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LivingArtworkShaderTest {

    @Test
    fun seedIsStableForSamePalette() {
        val tones = listOf(
            Color(0xFF1B4DFF),
            Color(0xFFFF6B35),
            Color(0xFF7B2CBF),
            Color(0xFF121826),
            Color(0xFFFFC857)
        )

        assertEquals(livingArtworkSeed(tones), livingArtworkSeed(tones), 0f)
    }

    @Test
    fun seedChangesWithPalette() {
        val first = listOf(
            Color(0xFF1B4DFF),
            Color(0xFFFF6B35),
            Color(0xFF7B2CBF),
            Color(0xFF121826),
            Color(0xFFFFC857)
        )
        val second = listOf(
            Color(0xFF00A896),
            Color(0xFFF0F3BD),
            Color(0xFF05668D),
            Color(0xFF028090),
            Color(0xFF02C39A)
        )

        assertNotEquals(livingArtworkSeed(first), livingArtworkSeed(second))
    }

    @Test
    fun seedRemainsInsideOnePhaseRotation() {
        val palettes = listOf(
            emptyList(),
            listOf(Color.Black),
            listOf(Color.White),
            listOf(Color.Red, Color.Green, Color.Blue)
        )

        palettes.forEach { tones ->
            val seed = livingArtworkSeed(tones)
            assertTrue(seed >= 0f)
            assertTrue(seed <= 6.2831855f)
        }
    }
}
