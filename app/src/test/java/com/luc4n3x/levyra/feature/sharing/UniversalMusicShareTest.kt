package com.luc4n3x.levyra.feature.sharing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalMusicShareTest {
    private val share = UniversalMusicShare()
    @Test fun `accepts public music sources`() = assertTrue(share.isSupportedSource("https://music.youtube.com/watch?v=x"))
    @Test fun `rejects private or credentialed sources`() {
        assertFalse(share.isSupportedSource("https://127.0.0.1/watch?v=x"))
        assertFalse(share.isSupportedSource("https://user@example.com/x"))
    }
    @Test fun `only accepts canonical odesli links`() = assertTrue(share.isOdesliPageUrl("https://song.link/x"))
}
