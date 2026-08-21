package com.luc4n3x.levyra.data

import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.stream.AudioTrackType
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeExtractorFallbackContractTest {
    @Test
    fun visionOsIdentityMatchesUpstreamWatchMobileProfile() {
        val client = applyVisionOsClientIdentity(JSONObject())

        assertEquals("Apple", client.getString("deviceMake"))
        assertEquals("RealityDevice17,1", client.getString("deviceModel"))
        assertEquals("visionOS", client.getString("osName"))
        assertEquals("26.6.0.23O770", client.getString("osVersion"))
        assertEquals("MOBILE", client.getString("platform"))
        assertEquals("WATCH", client.getString("clientScreen"))
        assertEquals(
            "com.google.visionos.youtube/1.04(RealityDevice17,1; U; CPU visionOS 26_6_0 like Mac OS X; IT)",
            visionOsUserAgent("it")
        )
    }

    @Test
    fun androidReelRequestMatchesUpstreamContract() {
        val body = buildAndroidReelRequestBody(
            videoId = "abcdefghijk",
            cpn = "ABCDEFGHIJKLMNOP",
            hl = "it",
            gl = "it",
            visitorData = "visitor-123"
        )
        val context = body.getJSONObject("context")
        val client = context.getJSONObject("client")
        val playerRequest = body.getJSONObject("playerRequest")

        assertEquals("ANDROID", client.getString("clientName"))
        assertEquals("21.03.36", client.getString("clientVersion"))
        assertEquals("WATCH", client.getString("clientScreen"))
        assertEquals("MOBILE", client.getString("platform"))
        assertEquals("Android", client.getString("osName"))
        assertEquals("16", client.getString("osVersion"))
        assertEquals(36, client.getInt("androidSdkVersion"))
        assertEquals("it", client.getString("hl"))
        assertEquals("IT", client.getString("gl"))
        assertEquals("visitor-123", client.getString("visitorData"))
        assertTrue(context.getJSONObject("request").getBoolean("useSsl"))
        assertFalse(context.getJSONObject("user").getBoolean("lockedSafetyMode"))
        assertEquals("abcdefghijk", playerRequest.getString("videoId"))
        assertEquals("ABCDEFGHIJKLMNOP", playerRequest.getString("cpn"))
        assertTrue(playerRequest.getBoolean("contentCheckOk"))
        assertTrue(playerRequest.getBoolean("racyCheckOk"))
        assertFalse(body.getBoolean("disablePlayerResponse"))
        assertEquals(
            "com.google.android.youtube/21.03.36 (Linux; U; Android 15; IT) gzip",
            androidReelUserAgent("it")
        )
    }

    @Test
    fun androidReelRequestAcceptsServerProvidedClientVersion() {
        val body = buildAndroidReelRequestBody(
            videoId = "abcdefghijk",
            cpn = "ABCDEFGHIJKLMNOP",
            hl = "it",
            gl = "it",
            visitorData = "visitor-123",
            clientVersion = "21.04.00"
        )

        assertEquals(
            "21.04.00",
            body.getJSONObject("context").getJSONObject("client").getString("clientVersion")
        )
        assertEquals(
            "com.google.android.youtube/21.04.00 (Linux; U; Android 15; IT) gzip",
            androidReelUserAgent("it", "21.04.00")
        )
    }

    @Test
    fun androidReelFormatsAreReadOnlyFromNestedPlayerResponse() {
        val expected = JSONArray().put(JSONObject().put("itag", 18))
        val response = JSONObject()
            .put(
                "streamingData",
                JSONObject().put("formats", JSONArray().put(JSONObject().put("itag", 999)))
            )
            .put(
                "playerResponse",
                JSONObject().put("streamingData", JSONObject().put("formats", expected))
            )

        val formats = androidReelFormats(response)

        assertEquals(1, formats.length())
        assertEquals(18, formats.getJSONObject(0).getInt("itag"))
    }


    @Test
    fun directAudioSelectionUsesNonOverlappingTrackTiers() {
        val autoDubbed = JSONObject().put(
            "audioTrack",
            JSONObject().put("isAutoDubbed", true).put("audioIsDefault", true)
        )
        val originalDefault = JSONObject().put(
            "audioTrack",
            JSONObject().put("isAutoDubbed", false).put("audioIsDefault", true)
        )

        assertEquals(0, youtubeAudioTrackTier(autoDubbed))
        assertEquals(2, youtubeAudioTrackTier(originalDefault))
        assertEquals(1, youtubeAudioTrackTier(JSONObject()))
        assertTrue(
            strictAudioSelectionScore(youtubeAudioTrackTier(originalDefault), -4_999_999) >
                strictAudioSelectionScore(youtubeAudioTrackTier(autoDubbed), 4_999_999)
        )
    }

    @Test
    fun extractorTrackTierKeepsOriginalAndHumanAudioAboveAutoGeneratedAudio() {
        assertTrue(
            extractorAudioTrackTier(AudioTrackType.ORIGINAL, false) >
                extractorAudioTrackTier(AudioTrackType.DUBBED, false)
        )
        assertTrue(
            extractorAudioTrackTier(AudioTrackType.DUBBED, false) >
                extractorAudioTrackTier(AudioTrackType.DUBBED, true)
        )
    }

    @Test
    fun youtubeJsonReadAcceptsSmallPayloads() {
        val body = object : ResponseBody() {
            override fun contentType(): MediaType? = null
            override fun contentLength(): Long = -1L
            override fun source(): BufferedSource = Buffer().writeUtf8("{\"ok\":true}")
        }

        assertEquals("{\"ok\":true}", readBoundedYoutubeJsonBody(body, 64))
    }

    @Test
    fun youtubeJsonReadRejectsUnknownLengthBodiesPastTheLimit() {
        val body = object : ResponseBody() {
            override fun contentType(): MediaType? = null
            override fun contentLength(): Long = -1L
            override fun source(): BufferedSource = Buffer().writeUtf8("12345")
        }

        val error = runCatching { readBoundedYoutubeJsonBody(body, 4) }.exceptionOrNull()

        assertTrue(error is IOException)
    }

    @Test
    fun youtubeNoncesUseTheExpectedAlphabetAndLengths() {
        val cpn = generateYoutubeNonce(16)
        val t = generateYoutubeNonce(12)
        val pattern = Regex("[A-Za-z0-9_-]+")

        assertEquals(16, cpn.length)
        assertEquals(12, t.length)
        assertTrue(pattern.matches(cpn))
        assertTrue(pattern.matches(t))
    }
}
