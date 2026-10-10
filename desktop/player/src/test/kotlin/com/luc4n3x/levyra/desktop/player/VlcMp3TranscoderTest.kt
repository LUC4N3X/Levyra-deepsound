package com.luc4n3x.levyra.desktop.player

import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class VlcMp3TranscoderTest {
    @Test
    fun transcodeOptionsCreateARealMp3Stream() {
        val options = vlcMp3TranscodeOptions(Path.of("music", "Artist - Track.mp3"))
        val sout = options.first { it.startsWith(":sout=") }

        assertTrue(sout.contains("acodec=mp3"))
        assertTrue(sout.contains("ab=256"))
        assertTrue(sout.contains("channels=2"))
        assertTrue(sout.contains("mux=raw"))
        assertTrue(sout.contains("Artist - Track.mp3"))
    }

    @Test
    fun soutPathUsesPortableSeparatorsAndEscapesQuotes() {
        val escaped = escapeVlcSoutPath("C:\\Music\\Artist's Track.mp3")

        assertTrue(escaped.contains("C:/Music/"))
        assertTrue(escaped.contains("Artist\\'s Track.mp3"))
    }
}
