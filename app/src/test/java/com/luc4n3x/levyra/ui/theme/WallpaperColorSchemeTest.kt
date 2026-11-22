package com.luc4n3x.levyra.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperColorSchemeTest {
    private val seeds = listOf(0xFF1E88E5.toInt(), 0xFFE53935.toInt(), 0xFF43A047.toInt(), 0xFF8E24AA.toInt(), 0xFF9E9E9E.toInt(), 0xFFFFEB3B.toInt())

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return maxOf(la, lb) / minOf(la, lb)
    }

    @Test
    fun textStaysReadableInLightDarkAndAmoled() {
        seeds.forEach { seed ->
            listOf(false to false, true to false, true to true).forEach { (dark, amoled) ->
                val scheme = wallpaperColorScheme(seed, dark, amoled)
                assertTrue("onSurface $seed dark=$dark", contrast(scheme.onSurface, scheme.surface) >= 4.5f)
                assertTrue("onPrimary $seed dark=$dark", contrast(scheme.onPrimary, scheme.primary) >= 4.5f)
                assertTrue("onSurfaceVariant $seed dark=$dark", contrast(scheme.onSurfaceVariant, scheme.surface) >= 4.5f)
            }
        }
    }

    @Test
    fun amoledUsesTrueBlackSurfaces() {
        val scheme = wallpaperColorScheme(seeds.first(), dark = true, pureBlack = true)
        assertEquals(Color.Black, scheme.background)
        assertEquals(Color.Black, scheme.surface)
        assertTrue(scheme.surfaceContainer.luminance() > scheme.surface.luminance())
    }

    @Test
    fun lightSchemeIgnoresAmoled() {
        val scheme = wallpaperColorScheme(seeds.first(), dark = false, pureBlack = true)
        assertTrue(scheme.background.luminance() > 0.8f)
    }
}
