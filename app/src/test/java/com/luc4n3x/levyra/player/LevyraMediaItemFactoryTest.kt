package com.luc4n3x.levyra.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LevyraMediaItemFactoryTest {
    @Test
    fun detectsHlsAcrossSupportedUrlForms() {
        assertEquals("application/x-mpegURL", LevyraMediaItemFactory.mimeTypeFor("https://example.com/live.m3u8", true))
        assertEquals("application/x-mpegURL", LevyraMediaItemFactory.mimeTypeFor("https://example.com/videoplayback?mime=application%2Fx-mpegurl", true))
        assertEquals("application/x-mpegURL", LevyraMediaItemFactory.mimeTypeFor("https://example.com/videoplayback?mime=application/vnd.apple.mpegurl", true))
    }

    @Test
    fun detectsDashAndDirectVideoContainers() {
        assertEquals("application/dash+xml", LevyraMediaItemFactory.mimeTypeFor("https://example.com/manifest.mpd", true))
        assertEquals("video/webm", LevyraMediaItemFactory.mimeTypeFor("https://example.com/videoplayback?mime=video%2Fwebm", true))
        assertEquals("video/mp4", LevyraMediaItemFactory.mimeTypeFor("https://example.com/videoplayback?mime=video%2Fmp4", true))
    }

    @Test
    fun localContentUriUsesExtractorSniffing() {
        assertNull(LevyraMediaItemFactory.mimeTypeFor("content://media/external/audio/media/42", false))
        assertNull(LevyraMediaItemFactory.mimeTypeFor("file:///storage/emulated/0/Music/download", false))
    }
}
