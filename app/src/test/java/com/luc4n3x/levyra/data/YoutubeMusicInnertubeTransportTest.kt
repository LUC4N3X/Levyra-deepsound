package com.luc4n3x.levyra.data

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.brotli.BrotliInterceptor
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicInnertubeTransportTest {

    @Test
    fun innertubeRequestLeavesContentNegotiationToTheClient() {
        val request = request(mobile = false).build()

        assertNull(request.header("Accept-Encoding"))
        assertEquals("67", request.header("X-Youtube-Client-Name"))
    }

    @Test
    fun compressedWatchResponseIsDecodedIntoJson() {
        val payload = """{"contents":{"radio":true}}"""
        var negotiated: String? = null
        val server = Interceptor { chain ->
            negotiated = chain.request().header("Accept-Encoding")
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("OK")
                .header("Content-Encoding", "gzip")
                .body(gzip(payload).toResponseBody("application/json".toMediaType()))
                .build()
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(BrotliInterceptor)
            .addInterceptor(server)
            .build()

        val body = client.newCall(request(mobile = false).build()).execute().use { it.body.string() }

        assertTrue(negotiated.orEmpty().contains("br"))
        assertTrue(JSONObject(body).getJSONObject("contents").getBoolean("radio"))
    }

    @Test
    fun podcastEpisodesAreNotMusicCandidates() {
        assertTrue(isNonMusicVideoType("MUSIC_VIDEO_TYPE_PODCAST_EPISODE"))
        assertFalse(isNonMusicVideoType("MUSIC_VIDEO_TYPE_ATV"))
        assertFalse(isNonMusicVideoType("MUSIC_VIDEO_TYPE_OMV"))
        assertFalse(isNonMusicVideoType(""))
    }

    private fun request(mobile: Boolean) = YoutubeMusicWatchRepository.innertubeRequestBuilder(
        url = "https://music.youtube.com/youtubei/v1/next?prettyPrint=false",
        body = "{}".toRequestBody("application/json".toMediaType()),
        mobile = mobile
    )

    private fun gzip(value: String): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(value.toByteArray()) }
        return output.toByteArray()
    }
}
