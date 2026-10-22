package com.luc4n3x.levyra.feature.radio

import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class RadioRedirectInterceptorTest {
    @Test
    fun followsRelativePublicRedirectsAndStripsCredentials() {
        val requests = mutableListOf<Request>()
        val client = client { request ->
            requests.add(request)
            if (requests.size == 1) response(request, 302, "/audio") else response(request, 200)
        }
        client.newCall(Request.Builder().url("https://radio.example/start").header("Authorization", "synthetic").build()).execute().use {
            assertEquals(200, it.code)
        }
        assertEquals("/audio", requests.last().url.encodedPath)
        assertNull(requests.last().header("Authorization"))
    }

    @Test
    fun preservesMultipleChoicesRedirectCompatibility() {
        var calls = 0
        val client = client { request ->
            calls++
            if (calls == 1) response(request, 300, "/audio") else response(request, 200)
        }
        client.newCall(Request.Builder().url("https://radio.example/start").build()).execute().use { assertEquals(200, it.code) }
        assertEquals(2, calls)
    }

    @Test
    fun rejectsPrivateRedirectBeforeIssuingSecondRequest() {
        listOf("http://127.0.0.1/", "http://192.168.1.2/", "http://[::1]/", "file:///etc/passwd").forEach { target ->
            var calls = 0
            val client = client { request -> calls++; response(request, 302, target) }
            try {
                client.newCall(Request.Builder().url("https://radio.example/start").build()).execute().close()
                fail("Unsafe redirect accepted: $target")
            } catch (_: IOException) { assertEquals(1, calls) }
        }
    }

    @Test
    fun boundsRedirectCycles() {
        var calls = 0
        val client = client { request -> calls++; response(request, 302, "/loop") }
        try {
            client.newCall(Request.Builder().url("https://radio.example/start").build()).execute().close()
            fail("Cycle accepted")
        } catch (_: IOException) { assertEquals(6, calls) }
    }

    private fun client(reply: (Request) -> Response) = OkHttpClient.Builder().followRedirects(false)
        .addInterceptor(RadioRedirectInterceptor()).addInterceptor { reply(it.request()) }.build()

    private fun response(request: Request, code: Int, location: String? = null): Response = Response.Builder().request(request)
        .protocol(Protocol.HTTP_1_1).code(code).message("Fixture").body("".toResponseBody())
        .apply { location?.let { header("Location", it) } }.build()
}
