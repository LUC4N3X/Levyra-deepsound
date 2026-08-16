package com.luc4n3x.levyra.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerAmbienceTest {

    private fun level(color: Color): Float =
        0.299f * color.red + 0.587f * color.green + 0.114f * color.blue

    @Test
    fun `ambience darkens from tint to base`() {
        val ambience = playerAmbienceOf(Color(0xFF3A7BD5), Color(0xFF00D2FF))
        assertTrue(level(ambience.tint) > level(ambience.elevated))
        assertTrue(level(ambience.elevated) > level(ambience.base))
        assertTrue(level(ambience.base) > 0f)
    }

    @Test
    fun `base stays dark enough for white content`() {
        listOf(
            Color.White to Color.White,
            Color(0xFFFFEB3B) to Color(0xFFFF5722),
            Color(0xFF101010) to Color(0xFF050505)
        ).forEach { (primary, secondary) ->
            val base = playerAmbienceOf(primary, secondary).base
            assertTrue("base too bright for $primary", level(base) < 0.05f)
        }
    }

    @Test
    fun `saturated artwork keeps a trace of its hue`() {
        val ambience = playerAmbienceOf(Color(0xFF1DB954), Color(0xFF1DB954))
        assertTrue(ambience.tint.green > ambience.tint.red)
        assertTrue(ambience.tint.green > ambience.tint.blue)
        assertTrue(ambience.base.green >= ambience.base.red)
    }

    @Test
    fun `pure black artwork degrades to a neutral surface`() {
        val ambience = playerAmbienceOf(Color.Black, Color.Black)
        assertEquals(ambience.base.red, ambience.base.green, 0.0001f)
        assertEquals(ambience.base.green, ambience.base.blue, 0.0001f)
        assertTrue(level(ambience.base) > 0f)
    }

    @Test
    fun `every ambience channel stays opaque and in range`() {
        val ambience = playerAmbienceOf(Color(0xFFFF00FF), Color(0xFF00FF00))
        listOf(ambience.tint, ambience.elevated, ambience.base).forEach { color ->
            assertEquals(1f, color.alpha, 0.0001f)
            listOf(color.red, color.green, color.blue).forEach {
                assertTrue(it in 0f..1f)
            }
        }
    }

    @Test
    fun `mix interpolates between both accents`() {
        val mixed = Color.Black.playerAmbienceMix(Color.White, 0.5f)
        assertEquals(0.5f, mixed.red, 0.005f)
        assertEquals(0f, Color.Black.playerAmbienceMix(Color.White, -1f).red, 0.0001f)
        assertEquals(1f, Color.Black.playerAmbienceMix(Color.White, 4f).red, 0.0001f)
    }
}
