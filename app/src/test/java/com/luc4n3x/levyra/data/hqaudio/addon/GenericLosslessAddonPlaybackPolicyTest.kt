package com.luc4n3x.levyra.data.hqaudio.addon

import com.luc4n3x.levyra.data.hqaudio.ProviderHttpExchange
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpRequest
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.hqaudio.ProviderSearchOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamOutcome
import com.luc4n3x.levyra.data.hqaudio.StreamRejection
import com.luc4n3x.levyra.domain.AudioQualityPreference
import com.luc4n3x.levyra.domain.AudioQualityRequest
import com.luc4n3x.levyra.domain.AudioStreamPurpose
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericLosslessAddonPlaybackPolicyTest {
    @Test
    fun playbackCandidateIsRejectedWhenProtectedPolicyCannotBeInstalled() = runBlocking {
        val now = 1_800_000_200_000L
        val mediaHost = "cdn-00.example.org"
        val extraHosts = (1..32).map { index -> "cdn-%02d.example.org".format(index) }
        val allowedHosts = listOf(mediaHost) + extraHosts
        val allowedHostsJson = allowedHosts.joinToString(",") { "\"$it\"" }
        val exchange = ProviderHttpExchange { request: ProviderHttpRequest ->
            when {
                request.url == "https://addon.example.org/manifest.json" -> json(
                    """{"resources":["search","stream"],"allowedHosts":[$allowedHostsJson]}"""
                )
                request.url.startsWith("https://addon.example.org/search?") -> json(
                    """{"tracks":[{"id":"track-1","title":"Track","artist":"Artist","duration":200}]}"""
                )
                request.url.startsWith("https://addon.example.org/stream/track-1?") -> json(
                    """{"url":"https://$mediaHost/audio/track.flac","expiresAtMs":${now + 600_000L}}"""
                )
                request.url == "https://$mediaHost/audio/track.flac" -> flacResponse()
                else -> ProviderHttpResponse(404, emptyMap(), ByteArray(0))
            }
        }
        val provider = GenericLosslessAddonProvider(
            exchange = exchange,
            enabled = { true },
            baseUrl = { "https://addon.example.org" },
            clock = { now }
        )
        val request = AudioQualityRequest(
            preference = AudioQualityPreference.CD_LOSSLESS,
            purpose = AudioStreamPurpose.PLAYBACK,
            losslessEnabled = true,
            allowUpgrade = false
        )
        val candidate = (provider.search("Track Artist", request) as ProviderSearchOutcome.Found).candidates.single()

        val outcome = provider.resolveStream(candidate, request)

        assertTrue(outcome is ProviderStreamOutcome.Unavailable)
        assertTrue(StreamRejection.TRANSPORT in (outcome as ProviderStreamOutcome.Unavailable).rejections)
    }

    private fun json(body: String) = ProviderHttpResponse(
        200,
        mapOf("Content-Type" to "application/json"),
        body.toByteArray()
    )

    private fun flacResponse(): ProviderHttpResponse {
        val bytes = ByteArray(42)
        "fLaC".toByteArray().copyInto(bytes)
        bytes[7] = 34
        val packed = (44_100L shl 44) or (1L shl 41) or (15L shl 36)
        for (index in 0 until 8) {
            val shift = (7 - index) * 8
            bytes[18 + index] = (packed ushr shift).toByte()
        }
        return ProviderHttpResponse(
            206,
            mapOf("Content-Type" to "audio/flac", "Content-Range" to "bytes 0-41/12000000"),
            bytes
        )
    }
}
