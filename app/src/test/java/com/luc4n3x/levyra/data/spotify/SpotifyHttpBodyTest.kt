package com.luc4n3x.levyra.data.spotify

import okhttp3.MediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyHttpBodyTest {
    @Test
    fun acceptsBoundedJsonBody() {
        val body = "{\"ok\":true}".toResponseBody(null)

        assertEquals("{\"ok\":true}", body.readSpotifyJsonBody())
    }

    @Test
    fun rejectsOversizedDeclaredBody() {
        val body = "x".repeat((SPOTIFY_MAX_JSON_RESPONSE_BYTES + 1L).toInt()).toResponseBody(null)

        assertNull(body.readSpotifyJsonBody())
    }

    @Test
    fun rejectsOversizedStreamWhenLengthIsUnknown() {
        val payload = Buffer().write(ByteArray((SPOTIFY_MAX_JSON_RESPONSE_BYTES + 1L).toInt()))
        val body = object : ResponseBody() {
            override fun contentType(): MediaType? = null
            override fun contentLength(): Long = -1L
            override fun source(): BufferedSource = payload
        }

        assertNull(body.readSpotifyJsonBody())
    }
}
