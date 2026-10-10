package com.luc4n3x.levyra.ui.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsStatusTest {
    @Test
    fun providerNameDropsHostsVariantsAndDecorations() {
        assertEquals("LyricsPlus", lyricsProviderDisplayName("LyricsPlus · lyricsplus.binimum.org"))
        assertEquals("Binimum", lyricsProviderDisplayName("Binimum · Word"))
        assertEquals("Apple Music", lyricsProviderDisplayName("Apple Music • TTML"))
        assertEquals("LRCLIB", lyricsProviderDisplayName("✨ LRCLIB"))
        assertEquals("Musixmatch", lyricsProviderDisplayName("  Musixmatch  "))
    }

    @Test
    fun blankOrDecorativeProviderProducesNoLabel() {
        assertEquals("", lyricsProviderDisplayName(""))
        assertEquals("", lyricsProviderDisplayName("✨"))
    }

    @Test
    fun selectionModeNeverFollowsTheActiveLine() {
        assertTrue(lyricsShouldFollowActiveLine(autoScrollEnabled = true, selectionMode = false))
        assertFalse(lyricsShouldFollowActiveLine(autoScrollEnabled = true, selectionMode = true))
        assertFalse(lyricsShouldFollowActiveLine(autoScrollEnabled = false, selectionMode = false))
    }
}
