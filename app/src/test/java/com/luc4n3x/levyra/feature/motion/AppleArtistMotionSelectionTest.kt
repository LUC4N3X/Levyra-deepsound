package com.luc4n3x.levyra.feature.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleArtistMotionSelectionTest {
    @Test
    fun artistMotionChecksUsBeforeLocaleStorefront() {
        assertEquals(listOf("us", "it"), appleArtistMotionStorefronts("IT"))
        assertEquals(listOf("us"), appleArtistMotionStorefronts("us"))
    }

    @Test
    fun appleMusicScriptDiscoveryAcceptsCurrentAndLegacyBundles() {
        val html = """
            <html>
              <script src="/assets/index-main.abc123.js"></script>
              <script src="https://music.apple.com/assets/chunk-player.def456.js"></script>
              <script src="https://evil.example/assets/index.js"></script>
            </html>
        """.trimIndent()

        assertEquals(
            listOf(
                "https://music.apple.com/assets/index-main.abc123.js",
                "https://music.apple.com/assets/chunk-player.def456.js"
            ),
            appleMusicScriptUrls(html)
        )
    }

    @Test
    fun applePlayerGetsLongerProviderTimeoutWithoutSlowingOtherProviders() {
        assertEquals(25_000L, motionArtworkProviderTimeoutMs("apple-motion", 6_500L))
        assertEquals(6_500L, motionArtworkProviderTimeoutMs("community-canvas", 6_500L))
        assertEquals(6_500L, motionArtworkProviderTimeoutMs("tidal-video-cover", 6_500L))
        assertTrue(motionArtworkProviderTimeoutMs("apple-motion", 30_000L) >= 30_000L)
    }
}
