package com.luc4n3x.levyra.ui.lyrics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsAmbientBackdropTest {

    @Test
    fun `bright artwork accents are toned down below the readability ceiling`() {
        listOf(Color.White, Color.Yellow, Color(0xFFFFC0CB), Color(0xFF00FFAA)).forEach { accent ->
            val toned = lyricsAmbientTone(accent, LYRICS_AMBIENT_MAX_LUMINANCE)
            assertTrue("$accent -> ${toned.luminance()}", toned.luminance() <= LYRICS_AMBIENT_MAX_LUMINANCE + 0.005f)
        }
    }

    @Test
    fun `dark accents keep their colour`() {
        val accent = Color(0xFF1A0F2E)
        val toned = lyricsAmbientTone(accent, LYRICS_AMBIENT_MAX_LUMINANCE)
        assertEquals(accent.red, toned.red, 0.0001f)
        assertEquals(accent.green, toned.green, 0.0001f)
        assertEquals(accent.blue, toned.blue, 0.0001f)
    }

    @Test
    fun `toned accents keep their hue ordering`() {
        val toned = lyricsAmbientTone(Color(0xFFFF8040), LYRICS_AMBIENT_MAX_LUMINANCE)
        assertTrue(toned.red > toned.green)
        assertTrue(toned.green > toned.blue)
    }

    @Test
    fun `the deep tone is darker than the main tone`() {
        val accent = Color(0xFF7A5CFF)
        assertTrue(
            lyricsAmbientTone(accent, LYRICS_AMBIENT_DEEP_LUMINANCE).luminance() <
                lyricsAmbientTone(accent, LYRICS_AMBIENT_MAX_LUMINANCE).luminance()
        )
    }

    @Test
    fun `toned colours are fully opaque`() {
        assertEquals(1f, lyricsAmbientTone(Color.Red.copy(alpha = 0.3f), LYRICS_AMBIENT_MAX_LUMINANCE).alpha, 0.0001f)
    }
}
