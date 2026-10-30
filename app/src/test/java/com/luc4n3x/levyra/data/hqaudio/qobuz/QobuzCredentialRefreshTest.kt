package com.luc4n3x.levyra.data.hqaudio.qobuz

import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.hqaudio.AudioCredentials
import com.luc4n3x.levyra.data.hqaudio.AudioCredentialsSource
import com.luc4n3x.levyra.data.hqaudio.credentialDocument
import com.luc4n3x.levyra.data.hqaudio.FixedAudioCredentialsSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class QobuzCredentialRefreshTest {
    @Test fun rejectedUnchangedListIsTriedOnceAndServerErrorsDoNotRotate() = runBlocking {
        for (code in listOf(401, 403, 429, 503)) {
            var calls = 0
            val api = QobuzApi(QobuzExchange { _, _ ->
                calls++
                ProviderHttpResponse(code, emptyMap(), "{}".toByteArray())
            }, credentialSource = FixedAudioCredentialsSource(AudioCredentials.parse(credentialDocument())!!))
            runCatching { api.get("user/get") }
            assertEquals(1, calls)
        }
    }

    @Test fun smallerRemoteAccountSetDoesNotReuseOldPreferredIndex() = runBlocking {
        var active = AudioCredentials.parse(credentialDocument())!!
        val source = object : AudioCredentialsSource {
            override suspend fun current() = active
            override suspend fun refreshAfterRejection(rejected: AudioCredentials) = active
        }
        val tried = mutableListOf<String?>()
        val api = QobuzApi(QobuzExchange { request, _ ->
            tried += request.header("X-User-Auth-Token")
            ProviderHttpResponse(200, emptyMap(), "{}".toByteArray())
        }, credentialSource = source)
        api.get("user/get")
        active = AudioCredentials.parse(credentialDocument(2, "replacement"))!!
        api.get("user/get")
        assertEquals(listOf("new-qobuz", "replacement"), tried)
    }

    @Test fun failedAuthenticationFetchesReplacementAndReusesIt() = runBlocking {
        var active = AudioCredentials.parse(credentialDocument(1, "old-token"))!!
        var refreshes = 0
        val source = object : AudioCredentialsSource {
            override suspend fun current() = active
            override suspend fun refreshAfterRejection(rejected: AudioCredentials): AudioCredentials {
                refreshes++
                active = AudioCredentials.parse(credentialDocument(2))!!
                return active
            }
        }
        val tried = mutableListOf<String?>()
        val api = QobuzApi(QobuzExchange { request, _ ->
            val submittedToken = request.header("X-User-Auth-Token")
            tried += submittedToken
            ProviderHttpResponse(if (submittedToken == "new-qobuz") 200 else 401, emptyMap(), "{}".toByteArray())
        }, credentialSource = source)
        api.get("user/get")
        api.get("user/get")
        assertEquals(listOf("old-token", "new-qobuz", "new-qobuz"), tried)
        assertEquals(1, refreshes)
    }

    @Test fun reachesThirdAccountWhenFirstTwoAreRejected() = runBlocking {
        val tried = mutableListOf<String?>()
        val accounts = (1..3).map { QobuzCredentials.Account("token-$it", "123", "secret", "$it") }
        val api = QobuzApi(QobuzExchange { request, _ ->
            val submittedToken = request.header("X-User-Auth-Token")
            tried += submittedToken
            ProviderHttpResponse(if (submittedToken == "token-3") 200 else 401, emptyMap(), "{}".toByteArray())
        }, credentials = accounts)
        api.get("user/get")
        assertEquals(listOf("token-1", "token-2", "token-3"), tried)
    }
}
