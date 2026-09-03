package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePlayerConfigVerificationTest {

    private val signatureInput = YoutubePlayerConfigVerifier.SIGNATURE_PROBES[0]
    private val secondSignatureInput = YoutubePlayerConfigVerifier.SIGNATURE_PROBES[1]
    private val throttlingInput = YoutubePlayerConfigVerifier.THROTTLING_PROBES[0]
    private val secondThrottlingInput = YoutubePlayerConfigVerifier.THROTTLING_PROBES[1]

    private fun shuffled(value: String): String = value.reversed()

    private fun healthySignatures() = listOf(
        YoutubeCipherProbe(signatureInput, shuffled(signatureInput)),
        YoutubeCipherProbe(secondSignatureInput, shuffled(secondSignatureInput))
    )

    private fun healthyThrottling() = listOf(
        YoutubeCipherProbe(throttlingInput, "Kd93nchTQ7Bm2Xz1Lp"),
        YoutubeCipherProbe(secondThrottlingInput, "Ws81mzpQ4Rc6Yv0Nk")
    )

    @Test
    fun `a working cipher is accepted`() {
        val result = YoutubePlayerConfigVerifier.verify(healthySignatures(), healthyThrottling())
        assertEquals(YoutubeConfigVerdict.ACCEPTED, result.verdict)
        assertTrue(result.accepted)
        assertFalse(result.provesConfigWrong)
    }

    @Test
    fun `no probe evidence is inconclusive rather than a rejection`() {
        val result = YoutubePlayerConfigVerifier.verify(emptyList(), emptyList())
        assertEquals(YoutubeConfigVerdict.INCONCLUSIVE, result.verdict)
        assertFalse(result.provesConfigWrong)
    }

    @Test
    fun `partial probe coverage is inconclusive`() {
        val result = YoutubePlayerConfigVerifier.verify(healthySignatures(), emptyList())
        assertEquals(YoutubeConfigVerdict.INCONCLUSIVE, result.verdict)
    }

    @Test
    fun `an empty signature output is rejected`() {
        val probes = listOf(YoutubeCipherProbe(signatureInput, ""))
        val result = YoutubePlayerConfigVerifier.verify(probes, healthyThrottling())
        assertTrue(result.provesConfigWrong)
    }

    @Test
    fun `an unchanged signature output is rejected`() {
        val probes = listOf(YoutubeCipherProbe(signatureInput, signatureInput))
        val result = YoutubePlayerConfigVerifier.verify(probes, healthyThrottling())
        assertTrue(result.provesConfigWrong)
    }

    @Test
    fun `a spliced signature output stays acceptable`() {
        val spliced = listOf(
            YoutubeCipherProbe(signatureInput, shuffled(signatureInput).drop(20)),
            YoutubeCipherProbe(secondSignatureInput, shuffled(secondSignatureInput).drop(18))
        )
        val result = YoutubePlayerConfigVerifier.verify(spliced, healthyThrottling())
        assertEquals(YoutubeConfigVerdict.ACCEPTED, result.verdict)
    }

    @Test
    fun `a signature output longer than the input is rejected`() {
        val probes = listOf(YoutubeCipherProbe(signatureInput, signatureInput + "AB"))
        val result = YoutubePlayerConfigVerifier.verify(probes, healthyThrottling())
        assertTrue(result.provesConfigWrong)
    }

    @Test
    fun `a signature output with foreign characters is rejected`() {
        val corrupted = shuffled(signatureInput).dropLast(1) + "%"
        val probes = listOf(YoutubeCipherProbe(signatureInput, corrupted))
        val result = YoutubePlayerConfigVerifier.verify(probes, healthyThrottling())
        assertTrue(result.provesConfigWrong)
    }

    @Test
    fun `an unchanged n output is rejected`() {
        val probes = listOf(YoutubeCipherProbe(throttlingInput, throttlingInput))
        val result = YoutubePlayerConfigVerifier.verify(healthySignatures(), probes)
        assertTrue(result.provesConfigWrong)
    }

    @Test
    fun `a transform that collapses distinct inputs is rejected`() {
        val collapsed = listOf(
            YoutubeCipherProbe(throttlingInput, "SameOutput1"),
            YoutubeCipherProbe(secondThrottlingInput, "SameOutput1")
        )
        val result = YoutubePlayerConfigVerifier.verify(healthySignatures(), collapsed)
        assertTrue(result.provesConfigWrong)
    }

    @Test
    fun `aggregator returns null without any valid sample`() {
        assertNull(YoutubePlayerSampleAggregator.aggregate(emptyList()))
        assertNull(
            YoutubePlayerSampleAggregator.aggregate(
                listOf(YoutubePlayerSample("not-a-hash", "iframe_api"))
            )
        )
    }

    @Test
    fun `a single agreed player is not reported as rotating`() {
        val observation = YoutubePlayerSampleAggregator.aggregate(
            listOf(
                YoutubePlayerSample("0a1b2c3d", "iframe_api"),
                YoutubePlayerSample("0a1b2c3d", "embed")
            )
        )
        assertEquals("0a1b2c3d", observation?.dominantHash)
        assertFalse(observation!!.rotating)
    }

    @Test
    fun `disagreeing samples expose the alternate player`() {
        val observation = YoutubePlayerSampleAggregator.aggregate(
            listOf(
                YoutubePlayerSample("0a1b2c3d", "iframe_api"),
                YoutubePlayerSample("ffee9911", "embed")
            )
        )
        assertTrue(observation!!.rotating)
        assertEquals("0a1b2c3d", observation.dominantHash)
        assertEquals(listOf("ffee9911"), observation.alternateHashes)
        assertEquals(listOf("0a1b2c3d", "ffee9911"), observation.allHashes)
    }

    @Test
    fun `the most frequently observed player wins over sample order`() {
        val observation = YoutubePlayerSampleAggregator.aggregate(
            listOf(
                YoutubePlayerSample("0a1b2c3d", "iframe_api"),
                YoutubePlayerSample("ffee9911", "embed"),
                YoutubePlayerSample("ffee9911", "watch")
            )
        )
        assertEquals("ffee9911", observation?.dominantHash)
        assertEquals(listOf("0a1b2c3d"), observation?.alternateHashes)
    }

    @Test
    fun `hashes are normalised before aggregation`() {
        val observation = YoutubePlayerSampleAggregator.aggregate(
            listOf(
                YoutubePlayerSample(" 0A1B2C3D ", "iframe_api"),
                YoutubePlayerSample("0a1b2c3d", "embed")
            )
        )
        assertEquals("0a1b2c3d", observation?.dominantHash)
        assertFalse(observation!!.rotating)
    }
}
