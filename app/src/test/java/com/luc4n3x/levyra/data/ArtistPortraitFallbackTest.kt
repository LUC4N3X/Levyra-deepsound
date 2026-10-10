package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistPortraitFallbackTest {

    private fun solid(color: Int, count: Int = 256): IntArray = IntArray(count) { color }

    @Test
    fun `a solid colour portrait is detected as flat`() {
        assertTrue(isFlatArtworkSample(solid(0xFFC33028.toInt())))
    }

    @Test
    fun `an almost solid portrait within tolerance is still flat`() {
        val pixels = IntArray(256) { index ->
            val jitter = index % (FLAT_ARTWORK_CHANNEL_SPREAD + 1)
            (0xFF shl 24) or ((0xC0 + jitter) shl 16) or (0x30 shl 8) or 0x28
        }
        assertTrue(isFlatArtworkSample(pixels))
    }

    @Test
    fun `a real photograph with colour range is not flat`() {
        val pixels = IntArray(256) { index ->
            (0xFF shl 24) or ((index % 256) shl 16) or ((255 - index % 256) shl 8) or (index % 128)
        }
        assertFalse(isFlatArtworkSample(pixels))
    }

    @Test
    fun `an empty sample is never reported as flat`() {
        assertFalse(isFlatArtworkSample(IntArray(0)))
    }

    @Test
    fun `apple artwork host allowlist accepts mzstatic over https only`() {
        assertTrue(isAllowedAppleArtistArtworkUrl("https://is1-ssl.mzstatic.com/image/thumb/a/b/1200x1200bb.png"))
        assertFalse(isAllowedAppleArtistArtworkUrl("http://is1-ssl.mzstatic.com/image/thumb/a/b/1200x1200bb.png"))
        assertFalse(isAllowedAppleArtistArtworkUrl("https://evil.example.com/image.png"))
        assertFalse(isAllowedAppleArtistArtworkUrl("https://mzstatic.com.evil.example/image.png"))
    }

    @Test
    fun `apple artist page allowlist requires the artist path on music apple com`() {
        assertTrue(isAllowedAppleArtistPageUrl("https://music.apple.com/it/artist/sfera-ebbasta/881651714"))
        assertFalse(isAllowedAppleArtistPageUrl("https://music.apple.com/it/album/x/123"))
        assertFalse(isAllowedAppleArtistPageUrl("https://evil.example.com/it/artist/x/1"))
        assertFalse(isAllowedAppleArtistPageUrl("http://music.apple.com/it/artist/x/1"))
    }

    @Test
    fun `apple portrait extraction upgrades the requested size`() {
        val html = """<meta property="og:image" content="https://is1-ssl.mzstatic.com/image/thumb/AMCArtistImages221/v4/b5/0d/07/file_cropped.png/1200x630cw.png">"""
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/AMCArtistImages221/v4/b5/0d/07/file_cropped.png/1200x1200bb.png",
            extractAppleArtistPortrait(html)
        )
    }

    @Test
    fun `apple portrait extraction rejects a page without a usable image`() {
        assertEquals("", extractAppleArtistPortrait("<html><body>no artwork here</body></html>"))
        assertEquals("", extractAppleArtistPortrait("<img src=\"https://evil.example.com/a/1200x630cw.png\">"))
    }

    @Test
    fun `apple portrait extraction prefers the open graph image over album covers`() {
        val html = """
            <img src="https://is5-ssl.mzstatic.com/image/thumb/Music116/v4/aa/bb/cc/album_cover.jpg/300x300bb.jpg">
            <meta property="og:image" content="https://is1-ssl.mzstatic.com/image/thumb/AMCArtistImages221/v4/b5/0d/07/file_cropped.png/1200x630cw.png">
        """.trimIndent()
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/AMCArtistImages221/v4/b5/0d/07/file_cropped.png/1200x1200bb.png",
            extractAppleArtistPortrait(html)
        )
    }

    @Test
    fun `apple portrait extraction falls back to an artist image when open graph is absent`() {
        val html = """
            <img src="https://is5-ssl.mzstatic.com/image/thumb/Music116/v4/aa/bb/cc/album_cover.jpg/300x300bb.jpg">
            <img src="https://is1-ssl.mzstatic.com/image/thumb/AMCArtistImages112/v4/d1/e2/f3/artist.png/486x486bb.png">
        """.trimIndent()
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/AMCArtistImages112/v4/d1/e2/f3/artist.png/1200x1200bb.png",
            extractAppleArtistPortrait(html)
        )
    }

    @Test
    fun `apple portrait extraction ignores a page with only album covers`() {
        val html = """<img src="https://is5-ssl.mzstatic.com/image/thumb/Music116/v4/aa/bb/cc/album.jpg/300x300bb.jpg">"""
        assertEquals("", extractAppleArtistPortrait(html))
    }
}
