package com.luc4n3x.levyra.desktop.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackTest {

    @Test
    fun `watch urls expose the video id`() {
        assertEquals("dQw4w9WgXcQ", Track.videoIdOf("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", Track.videoIdOf("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=RD"))
    }

    @Test
    fun `short and shorts urls expose the video id`() {
        assertEquals("abc123", Track.videoIdOf("https://youtu.be/abc123"))
        assertEquals("abc123", Track.videoIdOf("https://youtu.be/abc123?t=10"))
        assertEquals("abc123", Track.videoIdOf("https://www.youtube.com/shorts/abc123"))
    }

    @Test
    fun `unknown urls resolve to the track id`() {
        val track = Track(id = "fallback", title = "Titolo", videoUrl = "https://example.com/song")
        assertEquals("fallback", track.videoId)
    }

    @Test
    fun `subtitle merges artist and album`() {
        val track = Track(id = "a", title = "T", artist = "Artista", album = "Album", videoUrl = "")
        assertEquals("Artista · Album", track.displaySubtitle)
    }
}
