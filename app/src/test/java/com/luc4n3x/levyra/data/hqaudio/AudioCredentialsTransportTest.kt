package com.luc4n3x.levyra.data.hqaudio

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class AudioCredentialsTransportTest {
    @Test fun onlyTheFixedRepositoryDocumentIsAllowed() {
        assertTrue(AudioCredentialsTransport.allows(AudioCredentialsTransport.URL.toHttpUrl()))
        for (url in listOf(
            AudioCredentialsTransport.URL.replace("https:", "http:"),
            AudioCredentialsTransport.URL.replace("/main/", "/attacker/"),
            AudioCredentialsTransport.URL + "?redirect=1",
            AudioCredentialsTransport.URL.replace("raw.githubusercontent.com", "raw.githubusercontent.com.evil.test")
        )) assertFalse(AudioCredentialsTransport.allows(url.toHttpUrl()))
    }

    @Test fun oversizedResponsesAreRejectedInsteadOfTruncated() {
        assertEquals("a".repeat(65_536), AudioCredentialsTransport.readBounded(Buffer().writeUtf8("a".repeat(65_536))))
        val failure = runCatching { AudioCredentialsTransport.readBounded(Buffer().writeUtf8("a".repeat(65_537))) }.exceptionOrNull()
        assertTrue(failure is IOException)
    }

    @Test fun downloadsOnlySuccessfulBoundedBodiesWithoutProviderHeaders() = runBlocking {
        var calls = 0
        val document = credentialDocument()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            assertNull(chain.request().header("Authorization"))
            assertNull(chain.request().header("X-User-Auth-Token"))
            assertEquals("no-cache", chain.request().header("Cache-Control"))
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(document.toResponseBody()).build()
        }.build()
        try {
            assertEquals(document, AudioCredentialsTransport { client }.fetch())
            assertEquals(1, calls)
        } finally { client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll() }
    }

    @Test fun redirectsAndServerErrorsAreNotAcceptedAsConfiguration() = runBlocking {
        for (code in listOf(302, 401, 429, 500)) {
            val client = OkHttpClient.Builder().followRedirects(false).addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(code).message("Unavailable").body(credentialDocument().toResponseBody()).build()
            }.build()
            try {
                assertTrue(runCatching { AudioCredentialsTransport { client }.fetch() }.exceptionOrNull() is IOException)
            } finally { client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll() }
        }
    }
}
