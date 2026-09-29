package com.luc4n3x.levyra.data.hqaudio

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderDestinationPolicyTest {
    @Test
    fun jioSaavnAndSaavnCdnHostsAreAllowed() {
        assertTrue(ProviderDestinationPolicy.allows("https://www.jiosaavn.com/api.php".toHttpUrl()))
        assertTrue(ProviderDestinationPolicy.allows("https://aac.saavncdn.com/820/song_320.mp4".toHttpUrl()))
        assertTrue(ProviderDestinationPolicy.allows("https://ac.cf.saavncdn.com/path".toHttpUrl()))
    }

    @Test
    fun localPlaintextAndLookalikeHostsAreRejected() {
        assertFalse(ProviderDestinationPolicy.allows("http://www.jiosaavn.com/api.php".toHttpUrl()))
        assertFalse(ProviderDestinationPolicy.allows("https://127.0.0.1/".toHttpUrl()))
        assertFalse(ProviderDestinationPolicy.allows("https://10.0.0.1/".toHttpUrl()))
        assertFalse(ProviderDestinationPolicy.allows("https://localhost/".toHttpUrl()))
        assertFalse(ProviderDestinationPolicy.allows("https://jiosaavn.com.evil.example/".toHttpUrl()))
        assertFalse(ProviderDestinationPolicy.allows("https://saavncdn.com.evil.example/".toHttpUrl()))
    }

    @Test
    fun configurableProviderUsesAnExactHttpsHostAllowlist() {
        val allowed = setOf("addon.example.org", "cdn.example.org")

        assertTrue(
            ConfigurableProviderDestinationPolicy.allows(
                "https://cdn.example.org/audio/track.flac".toHttpUrl(),
                allowed
            )
        )
        assertFalse(
            ConfigurableProviderDestinationPolicy.allows(
                "https://cdn.example.org.evil.test/audio/track.flac".toHttpUrl(),
                allowed
            )
        )
        assertFalse(
            ConfigurableProviderDestinationPolicy.allows(
                "http://cdn.example.org/audio/track.flac".toHttpUrl(),
                allowed
            )
        )
        assertFalse(
            ConfigurableProviderDestinationPolicy.allows(
                "https://user:secret@cdn.example.org/audio/track.flac".toHttpUrl(),
                allowed
            )
        )
    }

    @Test
    fun playbackPolicyCarriesAllowlistToChildHostsAndExpires() {
        val now = 1_800_000_000_000L
        val allowed = setOf("cdn.example.org", "segments.example.org")
        ConfigurableProviderPlaybackPolicy.register(
            "https://cdn.example.org/manifest.mpd".toHttpUrl(),
            allowed,
            now + 60_000L
        )

        assertEquals(
            allowed,
            ConfigurableProviderPlaybackPolicy.allowedHostsFor(
                "https://segments.example.org/audio/0001.m4s",
                now
            )
        )
        assertNull(
            ConfigurableProviderPlaybackPolicy.allowedHostsFor(
                "https://segments.example.org.evil.test/audio/0001.m4s",
                now
            )
        )
        assertNull(
            ConfigurableProviderPlaybackPolicy.allowedHostsFor(
                "https://segments.example.org/audio/0001.m4s",
                now + 60_001L
            )
        )
    }
}
