package com.luc4n3x.levyra.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorialCollectionAccentsTest {
    private fun hue(color: Color): Double = Hct.fromInt(color.toArgb()).hue

    private fun hueDistance(first: Double, second: Double): Double {
        val delta = abs(first - second) % 360.0
        return minOf(delta, 360.0 - delta)
    }

    @Test
    fun `same coloured seeds become clearly different cards`() {
        val green = Color(0xFF0F6B4A)
        val accents = distinctEditorialAccents(List(6) { green })
        val hues = accents.map { hue(it.first) }
        for (i in hues.indices) {
            for (j in i + 1 until hues.size) {
                assertTrue("cards $i and $j share a hue", hueDistance(hues[i], hues[j]) >= 20.0)
            }
        }
    }

    @Test
    fun `first card keeps the hue closest to its own seed`() {
        val orange = Color(0xFFFF8A24)
        val accents = distinctEditorialAccents(listOf(orange))
        assertTrue(hueDistance(hue(orange), hue(accents.single().first)) < 25.0)
    }

    @Test
    fun `cards share one tonal family`() {
        val accents = distinctEditorialAccents(
            listOf(Color(0xFFFF8A24), Color(0xFF7C3AED), Color(0xFF0F6B4A), Color(0xFFD81B60))
        )
        accents.forEach { (start, end) ->
            assertEquals(50.0, Hct.fromInt(start.toArgb()).tone, 1.5)
            assertEquals(30.0, Hct.fromInt(end.toArgb()).tone, 1.5)
        }
    }

    @Test
    fun `neighbours never repeat once every hue is used`() {
        val accents = distinctEditorialAccents(List(12) { Color(0xFF246BCE) })
        accents.zipWithNext().forEach { (left, right) -> assertNotEquals(left.first, right.first) }
    }

    @Test
    fun `accents are deterministic`() {
        val seeds = listOf(Color(0xFF0F6B4A), Color(0xFF0F8A78), Color(0xFF238636))
        assertEquals(distinctEditorialAccents(seeds), distinctEditorialAccents(seeds))
    }
}
