package com.luc4n3x.levyra.data.hqaudio

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

internal fun credentialDocument(revision: Long = 1, token: String = "new-qobuz"): String = """
    {"schemaVersion":1,"revision":$revision,
     "qobuz":{"accounts":[{"userAuthToken":"$token","appId":"123","appSecret":"test-secret","userId":"42"}]},
     "tidal":{"refreshTokens":["new-tidal"],"tvClientId":"tv-id","tvClientSecret":"tv-secret","mobileClientId":"mobile-id"}}
""".trimIndent()

internal class FixedAudioCredentialsSource(
    private val snapshot: AudioCredentials = AudioCredentials.embedded()
) : AudioCredentialsSource {
    override suspend fun current() = snapshot
    override suspend fun refreshAfterRejection(rejected: AudioCredentials) = snapshot
}

class AudioCredentialsStoreTest {
    private class Cache(var value: String? = null) : AudioCredentialsCache {
        override fun read() = value
        override fun write(document: String) { value = document }
    }

    @Test fun cachedSnapshotLoadsWithoutWaitingForNetwork() = runBlocking {
        var fetches = 0
        val store = AudioCredentialsStore({ fetches++; error("Offline") }, Cache(credentialDocument(2)))
        assertEquals("new-qobuz", store.current().qobuz.single().userAuthToken)
        assertEquals(0, fetches)
    }

    @Test fun validRefreshPersistsForNextProcess() = runBlocking {
        val cache = Cache()
        val store = AudioCredentialsStore({ credentialDocument(2) }, cache)
        assertEquals(2L, store.refresh().revision)
        assertEquals(2L, AudioCredentialsStore({ error("Offline") }, cache).current().revision)
    }

    @Test fun malformedAndOldUpdatesPreserveLastGoodSnapshot() = runBlocking {
        for (document in listOf("{}", credentialDocument(1), credentialDocument(3).replace("new-qobuz", ""), "x".repeat(65_537))) {
            val store = AudioCredentialsStore({ document }, Cache(credentialDocument(2)))
            assertEquals(2L, store.refresh().revision)
        }
    }

    @Test fun invalidCacheFallsBackToEmbeddedAccounts() = runBlocking {
        val store = AudioCredentialsStore({ error("Offline") }, Cache("broken"))
        assertEquals(0L, store.current().revision)
        assertTrue(store.current().qobuz.isNotEmpty())
    }

    @Test fun simultaneousRefreshesFetchOnce() = runBlocking {
        var calls = 0
        val store = AudioCredentialsStore({ calls++; delay(20); credentialDocument() })
        (1..8).map { async { store.refresh() } }.awaitAll()
        assertEquals(1, calls)
    }

    @Test fun authFailureRefreshBypassesHourlyIntervalButIsRateLimited() = runBlocking {
        var now = 1_000L
        var calls = 0
        val store = AudioCredentialsStore({ calls++; credentialDocument(calls.toLong()) }, clock = { now })
        val first = store.refresh()
        val second = store.refreshAfterRejection(first)
        assertEquals(2L, second.revision)
        assertEquals(2L, store.refreshAfterRejection(second).revision)
        assertEquals(2, calls)
        now += 60_000L
        assertEquals(3L, store.refreshAfterRejection(second).revision)
        assertEquals(3, calls)
    }

    @Test fun staleRejectionDoesNotFetchAgain() = runBlocking {
        var calls = 0
        val store = AudioCredentialsStore({ calls++; credentialDocument() })
        val embedded = store.current()
        store.refresh()
        assertEquals(1L, store.refreshAfterRejection(embedded).revision)
        assertEquals(1, calls)
    }

    @Test fun backgroundRefreshDoesNotBlockCachedCredentials() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val store = AudioCredentialsStore({ started.complete(Unit); finish.await(); credentialDocument(2) }, Cache(credentialDocument()))
        val refresh = async { store.refresh() }
        started.await()
        assertEquals(1L, store.current().revision)
        finish.complete(Unit)
        assertEquals(2L, refresh.await().revision)
    }

    @Test fun networkAndDiskErrorsKeepPlaybackUsable() = runBlocking {
        val brokenCache = object : AudioCredentialsCache {
            override fun read(): String? = throw IOException("disk")
            override fun write(document: String) { throw IOException("disk") }
        }
        assertEquals(1L, AudioCredentialsStore({ credentialDocument() }, brokenCache).refresh().revision)
        assertEquals(0L, AudioCredentialsStore({ throw IOException("offline") }, brokenCache).refresh().revision)
    }

    @Test fun cancellationPropagatesWithoutReplacingSnapshot() = runBlocking {
        var cancel = true
        val store = AudioCredentialsStore({ if (cancel) throw CancellationException("cancelled") else credentialDocument() })
        try { store.refresh(); fail("Cancellation swallowed") } catch (_: CancellationException) { }
        assertEquals(0L, store.current().revision)
        cancel = false
        assertEquals(1L, store.refresh().revision)
    }

    @Test fun offlineRefreshRetriesAfterOneMinuteRatherThanOneHour() = runBlocking {
        var now = 1_000L
        var calls = 0
        val store = AudioCredentialsStore({ calls++; if (calls == 1) throw IOException("offline") else credentialDocument() }, clock = { now })
        store.refresh()
        store.refresh()
        assertEquals(1, calls)
        now += 60_000L
        assertEquals(1L, store.refresh().revision)
        assertEquals(2, calls)
    }

    @Test fun parserRejectsUnsupportedSchemasEmptySetsAndUnsafeValues() {
        val base = JSONObject(credentialDocument())
        val documents = listOf(
            base.toString().replace("\"schemaVersion\":1", "\"schemaVersion\":2"),
            credentialDocument().replace("new-qobuz", "bad token"),
            credentialDocument().replace("\"userId\":\"42\"", "\"userId\":\"../42\""),
            credentialDocument().replace("[\"new-tidal\"]", "[]"),
            credentialDocument().replace("\"new-tidal\"", (1..11).joinToString(",") { "\"token-$it\"" }),
            credentialDocument().replace("\"revision\":1", "\"revision\":\"1\""),
            credentialDocument().replace("new-qobuz", "x".repeat(4097))
        )
        documents.forEach { assertNull(AudioCredentials.parse(it)) }
    }
}
