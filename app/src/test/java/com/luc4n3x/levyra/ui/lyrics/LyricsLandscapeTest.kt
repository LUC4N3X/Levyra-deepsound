package com.luc4n3x.levyra.ui.lyrics

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsLandscapeTest {

    @Test
    fun `portrait keeps the existing lyrics layout`() {
        assertFalse(lyricsLandscapeLayoutActive(Configuration.ORIENTATION_PORTRAIT, 412, 915))
    }

    @Test
    fun `landscape phones switch to the two pane layout`() {
        assertTrue(lyricsLandscapeLayoutActive(Configuration.ORIENTATION_LANDSCAPE, 915, 412))
        assertTrue(lyricsLandscapeLayoutActive(Configuration.ORIENTATION_LANDSCAPE, 640, 360))
    }

    @Test
    fun `narrow landscape windows keep the single column`() {
        assertFalse(lyricsLandscapeLayoutActive(Configuration.ORIENTATION_LANDSCAPE, 420, 300))
        assertFalse(lyricsLandscapeLayoutActive(Configuration.ORIENTATION_LANDSCAPE, 600, 700))
    }

    @Test
    fun `lyrics always keep a comfortable reading width on phones`() {
        listOf(
            640f to 360f,
            732f to 360f,
            800f to 360f,
            915f to 412f,
            592f to 320f
        ).forEach { (width, height) ->
            val metrics = lyricsLandscapeMetrics(width, height)
            assertTrue("lyrics pane too narrow for $width x $height", metrics.lyricsPaneWidthDp >= 360f)
            assertTrue("lyrics pane smaller than artwork pane", metrics.lyricsPaneWidthDp > metrics.artworkPaneWidthDp)
            assertEquals(width, metrics.artworkPaneWidthDp + metrics.lyricsPaneWidthDp, 0.001f)
        }
    }

    @Test
    fun `artwork pane stays within the target proportion`() {
        listOf(800f to 360f, 915f to 412f, 1280f to 800f, 2208f to 1768f).forEach { (width, height) ->
            val fraction = lyricsLandscapeMetrics(width, height).artworkPaneWidthDp / width
            assertTrue("fraction $fraction for $width", fraction in 0.32f..0.46f)
        }
    }

    @Test
    fun `artwork fits the pane and leaves room for metadata`() {
        listOf(640f to 360f, 800f to 360f, 915f to 412f, 1280f to 800f).forEach { (width, height) ->
            val metrics = lyricsLandscapeMetrics(width, height)
            assertTrue(metrics.artworkSizeDp <= metrics.artworkPaneWidthDp - 56f)
            assertTrue(metrics.artworkSizeDp <= height - 168f)
            assertTrue(metrics.artworkSizeDp >= 140f)
        }
    }

    @Test
    fun `artwork is capped on large displays`() {
        assertEquals(420f, lyricsLandscapeMetrics(1280f, 800f).artworkSizeDp, 0.001f)
        assertEquals(420f, lyricsLandscapeMetrics(2208f, 1768f).artworkSizeDp, 0.001f)
    }
}
