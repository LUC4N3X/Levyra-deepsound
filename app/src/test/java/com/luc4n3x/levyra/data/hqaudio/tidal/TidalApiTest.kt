package com.luc4n3x.levyra.data.hqaudio.tidal

import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TidalApiTest {
    @Test
    fun onlyTidalCachedManifestsAreClassifiedAsNetworkSources() {
        assertTrue(TidalAudioProvider.isManifestUri("file:///cache/tidal-manifests/test.mpd"))
        assertTrue(!TidalAudioProvider.isManifestUri("file:///music/song.flac"))
        assertTrue(!TidalAudioProvider.isManifestUri("file:///music/offline.mpd"))
        assertTrue(!TidalAudioProvider.isManifestUri("https://example.com/tidal-manifests/test.mpd"))
    }

    private fun response(code: Int, body: String) = ProviderHttpResponse(code, emptyMap(), body.toByteArray())
    private fun authenticated() = response(200, """{"access_token":"synthetic-access","token_type":"Bearer","expires_in":14400,"user":{"countryCode":"US"}}""")

    @Test
    fun cachedSessionAvoidsRepeatedRefreshUntilExpiry() = runBlocking {
        var now = 100_000L
        val requests = CopyOnWriteArrayList<Request>()
        val api = TidalApi(TidalExchange { request, _ ->
            requests += request
            if (request.method == "POST") authenticated() else response(200, "{\"id\":1}")
        }, { now })
        api.get("tracks/1")
        api.get("tracks/1")
        assertEquals(1, requests.count { it.method == "POST" })
        now += 14_400_000L
        api.get("tracks/1")
        assertEquals(2, requests.count { it.method == "POST" })
    }

    @Test
    fun rejectedPrimaryRefreshUsesFallbackOnce() = runBlocking {
        val requests = CopyOnWriteArrayList<Request>()
        var posts = 0
        val api = TidalApi(TidalExchange { request, _ ->
            requests += request
            if (request.method == "POST") {
                posts++
                if (posts == 1) response(400, "{\"error\":\"invalid_grant\"}") else authenticated()
            } else response(200, "{\"id\":1}")
        })
        assertEquals(1, api.get("tracks/1").getInt("id"))
        val forms = requests.filter { it.method == "POST" }.map { it.body as FormBody }
        assertEquals(2, forms.size)
        fun value(form: FormBody): String = (0 until form.size).first { form.name(it) == "refresh_token" }.let(form::value)
        assertTrue(value(forms[0]) != value(forms[1]))
    }

    @Test
    fun serverFailureDoesNotSwitchCredentials() = runBlocking {
        var calls = 0
        val api = TidalApi(TidalExchange { _, _ -> calls++; response(503, "{}") })
        val failure = runCatching { api.get("tracks/1") }.exceptionOrNull()
        assertTrue(failure is TidalFailure && failure.failure == ProviderFailure.HTTP_ERROR)
        assertEquals(1, calls)
    }

    @Test
    fun repeatedUnauthorizedResponseHasOnlyOneRetry() = runBlocking {
        var posts = 0
        var gets = 0
        val api = TidalApi(TidalExchange { request, _ ->
            if (request.method == "POST") { posts++; authenticated() } else { gets++; response(401, "{}") }
        })
        val failure = runCatching { api.get("tracks/1") }.exceptionOrNull()
        assertTrue(failure is TidalFailure && failure.failure == ProviderFailure.FORBIDDEN)
        assertEquals(2, posts)
        assertEquals(2, gets)
    }
}
