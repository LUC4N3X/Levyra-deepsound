package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import com.luc4n3x.levyra.data.hqaudio.AudioQualityTier
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JioSaavnEndpointsTest {
    @Test
    fun searchUsesTheVersionedApiContract() {
        val url = JioSaavnEndpoints.search("blinding lights the weeknd", 10).toHttpUrl()
        assertEquals("https", url.scheme)
        assertEquals(JioSaavnEndpoints.API_HOST, url.host)
        assertEquals("search.getResults", url.queryParameter("__call"))
        assertEquals("4", url.queryParameter("api_version"))
        assertEquals("web6dot0", url.queryParameter("ctx"))
        assertEquals("json", url.queryParameter("_format"))
        assertEquals("10", url.queryParameter("n"))
        assertEquals("blinding lights the weeknd", url.queryParameter("q"))
    }

    @Test
    fun mediaAuthorizationPreservesEncodedTokenCharacters() {
        val mediaToken = "dummy-media-token-payload-ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyT79OrPehdsFwZi/fqnWtzOD6rU74/VgN+wza9=="
        val url = JioSaavnEndpoints.authorizeMedia(mediaToken, AudioQualityTier.KBPS_320).toHttpUrl()
        assertEquals(mediaToken, url.queryParameter("url"))
        assertEquals("320", url.queryParameter("bitrate"))
        assertEquals("song.generateAuthToken", url.queryParameter("__call"))
    }

    @Test
    fun mediaLocationDerivesEveryTierFromAuthorizedPath() {
        val location = JioSaavnMediaLocation.parse(
            "https://web.saavncdn.com/820/5ddb9a79a5218f85ca9bef170f3a461d_320.mp4?Expires=1789151309&Signature=x&Key-Pair-Id=y"
        )!!
        assertEquals(AudioQualityTier.KBPS_320, location.authorizedTier)
        assertEquals(1_789_151_309_000L, location.authorizedExpiresAtMs)
        assertEquals(
            "https://aac.saavncdn.com/820/5ddb9a79a5218f85ca9bef170f3a461d_160.mp4",
            location.openUrl(AudioQualityTier.KBPS_160)
        )
    }

    @Test
    fun unexpectedMediaLocationsAreRejected() {
        assertNull(JioSaavnMediaLocation.parse("http://web.saavncdn.com/820/a_320.mp4"))
        assertNull(JioSaavnMediaLocation.parse("https://web.saavncdn.com/820/a.mp4"))
        assertNull(JioSaavnMediaLocation.parse("false"))
    }

    @Test
    fun realMediaTokenDecodesToAnOpenLocationForEveryTier() {
        val location = JioSaavnMediaLocation.fromMediaToken(
            "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy8IXxuTNJ1oLbvDGDneZj5h25kdaKPCof228ruhJnw7PIr7uKBnaPmxw7tS9a8Gtq"
        )!!
        assertEquals("", location.authorizedUrl)
        assertNull(location.authorizedTier)
        assertNull(location.authorizedExpiresAtMs)
        assertEquals(
            "https://aac.saavncdn.com/396/eca27e31e93211051fa0de18160ea825_320.mp4",
            location.openUrl(AudioQualityTier.KBPS_320)
        )
        assertEquals(
            "https://aac.saavncdn.com/396/eca27e31e93211051fa0de18160ea825_96.mp4",
            location.openUrl(AudioQualityTier.KBPS_96)
        )
    }

    @Test
    fun mediaTokenRoundTripKeepsTheOpenPath() {
        val location = JioSaavnMediaLocation.fromMediaToken(encrypt("https://aac.saavncdn.com/820/5ddb9a79_96.mp4"))!!
        assertEquals("https://aac.saavncdn.com/820/5ddb9a79_160.mp4", location.openUrl(AudioQualityTier.KBPS_160))
    }

    @Test
    fun untrustedOrMalformedMediaTokensAreRejected() {
        assertNull(JioSaavnMediaLocation.fromMediaToken(""))
        assertNull(JioSaavnMediaLocation.fromMediaToken("token-pW-kkdqr"))
        assertNull(JioSaavnMediaLocation.fromMediaToken("ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy"))
        assertNull(JioSaavnMediaLocation.fromMediaToken(encrypt("https://evil.example/820/5ddb9a79_96.mp4")))
        assertNull(JioSaavnMediaLocation.fromMediaToken(encrypt("http://aac.saavncdn.com/820/5ddb9a79_96.mp4")))
        assertNull(JioSaavnMediaLocation.fromMediaToken(encrypt("https://aac.saavncdn.com/5ddb9a79_96.mp4")))
        assertNull(JioSaavnMediaLocation.fromMediaToken(encrypt("https://aac.saavncdn.com/820/a%3Fb_96.mp4")))
        assertNull(JioSaavnMediaLocation.fromMediaToken(encrypt("https://aac.saavncdn.com/820/5ddb9a79.mp4")))
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec("38346591".toByteArray(Charsets.US_ASCII), "DES"))
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }
}
