package com.luc4n3x.levyra.data.hqaudio

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
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
}
