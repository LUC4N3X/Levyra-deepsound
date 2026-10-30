package com.luc4n3x.levyra.data.hqaudio.tidal

import com.luc4n3x.levyra.data.hqaudio.AudioCredentials
import com.luc4n3x.levyra.data.hqaudio.AudioCredentialsSource
import com.luc4n3x.levyra.data.hqaudio.FixedAudioCredentialsSource
import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.hqaudio.credentialDocument
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import org.junit.Assert.*
import org.junit.Test

class TidalCredentialRefreshTest {
    private fun response(code: Int, body: String) = ProviderHttpResponse(code, emptyMap(), body.toByteArray())
    private fun authenticated(refresh: String = "rotated") = response(200,
        """{"access_token":"access","token_type":"Bearer","expires_in":14400,"refresh_token":"$refresh","user":{"countryCode":"US"}}""")

    @Test fun replacedClientCredentialsCanRetryTheSameRefreshToken() = runBlocking {
        var active = AudioCredentials.parse(credentialDocument())!!
        val next = AudioCredentials.parse(credentialDocument(2).replace("tv-id", "replacement-id"))!!
        val source = object : AudioCredentialsSource {
            override suspend fun current() = active
            override suspend fun refreshAfterRejection(rejected: AudioCredentials): AudioCredentials {
                active = next
                return next
            }
        }
        var posts = 0
        val api = TidalApi(TidalExchange { request, _ ->
            if (request.method == "POST") {
                posts++
                val form = request.body as FormBody
                val clientId = form.value((0 until form.size).first { form.name(it) == "client_id" })
                if (clientId == "replacement-id") authenticated() else response(401, "{}")
            } else response(200, "{}")
        }, credentialSource = source)
        api.get("tracks/1")
        assertEquals(2, posts)
    }

    @Test fun rejectedRefreshTokensRecoverUsingRemoteReplacement() = runBlocking {
        val old = AudioCredentials.parse(credentialDocument().replace("new-tidal", "old-tidal"))!!
        val next = AudioCredentials.parse(credentialDocument(2))!!
        var active = old
        var refreshes = 0
        val source = object : AudioCredentialsSource {
            override suspend fun current() = active
            override suspend fun refreshAfterRejection(rejected: AudioCredentials): AudioCredentials {
                refreshes++
                active = next
                return next
            }
        }
        val tried = mutableListOf<String>()
        val api = TidalApi(TidalExchange { request, _ ->
            if (request.method == "POST") {
                val form = request.body as FormBody
                val submittedToken = form.value((0 until form.size).first { form.name(it) == "refresh_token" })
                tried += submittedToken
                if (submittedToken == "new-tidal") authenticated() else response(400, """{"error":"invalid_grant"}""")
            } else response(200, "{}")
        }, credentialSource = source)
        api.get("tracks/1")
        assertEquals(listOf("old-tidal", "new-tidal"), tried)
        assertEquals(1, refreshes)
    }

    @Test fun qobuzOnlyUpdatePreservesTidalSessionAndRotatedRefreshToken() = runBlocking {
        var active = AudioCredentials.parse(credentialDocument())!!
        var now = 1_000L
        val source = object : AudioCredentialsSource {
            override suspend fun current() = active
            override suspend fun refreshAfterRejection(rejected: AudioCredentials) = active
        }
        val tokens = mutableListOf<String>()
        val api = TidalApi(TidalExchange { request, _ ->
            if (request.method == "POST") {
                val form = request.body as FormBody
                tokens += form.value((0 until form.size).first { form.name(it) == "refresh_token" })
                authenticated()
            } else response(200, "{}")
        }, { now }, source)
        api.get("tracks/1")
        active = AudioCredentials.parse(credentialDocument(2, "other-qobuz"))!!
        api.get("tracks/1")
        assertEquals(1, tokens.size)
        now += 14_400_000L
        api.get("tracks/1")
        assertEquals(listOf("new-tidal", "rotated"), tokens)
    }

    @Test fun changedTidalSetInvalidatesPreviouslyCachedSession() = runBlocking {
        var active = AudioCredentials.parse(credentialDocument())!!
        val source = object : AudioCredentialsSource {
            override suspend fun current() = active
            override suspend fun refreshAfterRejection(rejected: AudioCredentials) = active
        }
        var posts = 0
        val clientIds = mutableListOf<String?>()
        val api = TidalApi(TidalExchange { request, _ ->
            if (request.method == "POST") { posts++; authenticated() }
            else { clientIds += request.header("X-Tidal-Token"); response(200, "{}") }
        }, credentialSource = source)
        api.get("tracks/1")
        active = AudioCredentials.parse(credentialDocument(2).replace("tv-id", "replacement-id"))!!
        api.get("tracks/1")
        assertEquals(2, posts)
        assertEquals(listOf("tv-id", "replacement-id"), clientIds)
    }

    @Test fun invalidGrantWithoutNewCredentialsStopsAfterEachTokenOnce() = runBlocking {
        var calls = 0
        val api = TidalApi(TidalExchange { _, _ ->
            calls++
            response(400, """{"error":"invalid_grant"}""")
        }, credentialSource = FixedAudioCredentialsSource(AudioCredentials.parse(credentialDocument())!!))
        val failure = runCatching { api.get("tracks/1") }.exceptionOrNull()
        assertTrue(failure is TidalFailure && failure.failure == ProviderFailure.FORBIDDEN)
        assertEquals(1, calls)
    }
}
