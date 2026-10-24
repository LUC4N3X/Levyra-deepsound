package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsFocusTest {

    @Test
    fun `unsynced lyrics stay fully readable`() {
        assertEquals(1f, lyricsFocusAlpha(4, focusMode = true, compact = true, synced = false), 0.0001f)
        assertEquals(0f, lyricsFocusBlurDp(4, focusMode = true, synced = false, blurEnabled = true), 0.0001f)
    }

    @Test
    fun `focus mode off keeps every line lit outside compact mode`() {
        assertEquals(1f, lyricsFocusAlpha(3, focusMode = false, compact = false, synced = true), 0.0001f)
        assertTrue(lyricsFocusAlpha(3, focusMode = false, compact = true, synced = true) < 1f)
    }

    @Test
    fun `the active line is always fully opaque and sharp`() {
        assertEquals(1f, lyricsFocusAlpha(0, focusMode = true, compact = false, synced = true), 0.0001f)
        assertEquals(0f, lyricsFocusBlurDp(0, focusMode = true, synced = true, blurEnabled = true), 0.0001f)
        assertEquals(0f, lyricsFocusBlurDp(1, focusMode = true, synced = true, blurEnabled = true), 0.0001f)
    }

    @Test
    fun `distant lines dim and blur progressively`() {
        val near = lyricsFocusAlpha(1, focusMode = true, compact = false, synced = true)
        val mid = lyricsFocusAlpha(2, focusMode = true, compact = false, synced = true)
        val far = lyricsFocusAlpha(6, focusMode = true, compact = false, synced = true)
        assertTrue(near > mid)
        assertTrue(mid > far)
        assertTrue(lyricsFocusBlurDp(2, true, true, true) < lyricsFocusBlurDp(3, true, true, true))
        assertTrue(lyricsFocusBlurDp(3, true, true, true) < lyricsFocusBlurDp(9, true, true, true))
    }

    @Test
    fun `the reading window around the active line is never blurred`() {
        assertEquals(0f, lyricsFocusBlurDp(2, focusMode = true, synced = true, blurEnabled = true), 0.0001f)
        assertTrue(lyricsFocusBlurDp(3, focusMode = true, synced = true, blurEnabled = true) > 0f)
    }

    @Test
    fun `dimmed lines stay readable`() {
        assertTrue(lyricsFocusAlpha(9, focusMode = true, compact = false, synced = true) >= 0.3f)
        assertTrue(lyricsFocusAlpha(9, focusMode = true, compact = true, synced = true) >= 0.25f)
    }

    @Test
    fun `karaoke feather collapses as the fill reaches the line end`() {
        assertEquals(30f, karaokeFeatherWidth(featherPx = 30f, remainingToLineEndPx = 200f), 0.0001f)
        assertEquals(12f, karaokeFeatherWidth(featherPx = 30f, remainingToLineEndPx = 12f), 0.0001f)
        assertEquals(0f, karaokeFeatherWidth(featherPx = 30f, remainingToLineEndPx = 0f), 0.0001f)
    }

    @Test
    fun `negative distances are treated as the active line`() {
        assertEquals(1f, lyricsFocusAlpha(-3, focusMode = true, compact = true, synced = true), 0.0001f)
        assertEquals(0f, lyricsFocusBlurDp(-3, focusMode = true, synced = true, blurEnabled = true), 0.0001f)
    }

    @Test
    fun `disabled animations remove the focus blur but keep the dimming`() {
        assertEquals(0f, lyricsFocusBlurDp(6, focusMode = true, synced = true, blurEnabled = false), 0.0001f)
        assertTrue(lyricsFocusAlpha(6, focusMode = true, compact = false, synced = true) < 1f)
    }

    @Test
    fun `the cinema backdrop only shows in cinema mode`() {
        assertEquals(0f, lyricsBackdropAlpha(focusMode = true, cinema = false), 0.0001f)
        assertTrue(lyricsBackdropAlpha(focusMode = true, cinema = true) > lyricsBackdropAlpha(focusMode = false, cinema = true))
    }
}
