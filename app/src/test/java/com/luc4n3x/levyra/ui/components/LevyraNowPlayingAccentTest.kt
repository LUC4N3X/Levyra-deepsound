package com.luc4n3x.levyra.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraNowPlayingAccentTest {

    private val fallback = Color(0xFF0A84FF)

    @Test
    fun `monochrome artwork accent falls back to the theme accent`() {
        assertEquals(fallback, nowPlayingAccentOrFallback(Color(0xFF8A817C), fallback))
        assertEquals(fallback, nowPlayingAccentOrFallback(Color(0xFF7F7F7F), fallback))
    }

    @Test
    fun `colorful artwork accent is kept`() {
        val candidate = Color(0xFFE0457B)
        assertEquals(candidate, nowPlayingAccentOrFallback(candidate, fallback))
    }
}
