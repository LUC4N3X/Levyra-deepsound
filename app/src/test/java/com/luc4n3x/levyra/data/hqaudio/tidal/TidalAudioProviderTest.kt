package com.luc4n3x.levyra.data.hqaudio.tidal

import com.luc4n3x.levyra.data.hqaudio.ProviderHttpResponse
import com.luc4n3x.levyra.data.hqaudio.ProviderSearchOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TidalAudioProviderTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun catalogSearchFinishesWhileStreamingAuthorizationIsStillPending() = runBlocking {
        val streamRequested = CompletableDeferred<Unit>()
        val exchange = TidalExchange { request, _ ->
            when (request.url.host) {
                "hifi.odskyler.com" -> {
                    streamRequested.complete(Unit)
                    awaitCancellation()
                }
                "auth.odskyler.workers.dev" -> response(200, "{\"access_token\":\"synthetic-test-token\",\"expires_in\":3600}")
                "proxy.odskyler.workers.dev" -> response(503, "")
                "api.tidal.com" -> {
                    assertEquals("/v1/search/tracks", request.url.encodedPath)
                    response(200, "{\"items\":[]}")
                }
                else -> error("Unexpected Tidal destination")
            }
        }
        val provider = TidalAudioProvider(temporaryFolder.root, TidalApi(exchange))
        val warming = launch { provider.prewarm() }
        try {
            withTimeout(2_000L) {
                streamRequested.await()
                assertEquals(ProviderSearchOutcome.Found(emptyList()), provider.search("Fluffing a Duck", 1))
                assertTrue(warming.isActive)
            }
        } finally {
            warming.cancelAndJoin()
        }
    }

    private fun response(code: Int, payload: String) = ProviderHttpResponse(code, emptyMap(), payload.toByteArray())
}
