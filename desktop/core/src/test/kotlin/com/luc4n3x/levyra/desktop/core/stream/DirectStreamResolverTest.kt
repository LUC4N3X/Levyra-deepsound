package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.desktop.core.model.Track
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectStreamResolverTest {

    private fun track(url: String) = Track(id = "x", title = "T", videoUrl = url)

    @Test
    fun recognizesDirectStreams() {
        assertTrue(DirectStreamResolver.isDirect(track("https://icecast.example.org/live.mp3")))
        assertTrue(DirectStreamResolver.isDirect(track("http://stream.example.com:8000/radio")))
    }

    @Test
    fun leavesYoutubeAndInvalidUrlsToDelegate() {
        assertFalse(DirectStreamResolver.isDirect(track("https://www.youtube.com/watch?v=abc")))
        assertFalse(DirectStreamResolver.isDirect(track("https://music.youtube.com/watch?v=abc")))
        assertFalse(DirectStreamResolver.isDirect(track("https://youtu.be/abc")))
        assertFalse(DirectStreamResolver.isDirect(track("file:///C:/music.mp3")))
        assertFalse(DirectStreamResolver.isDirect(track("not a url")))
    }
}
