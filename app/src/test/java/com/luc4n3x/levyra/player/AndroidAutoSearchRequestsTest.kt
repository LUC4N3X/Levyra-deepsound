package com.luc4n3x.levyra.player

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AndroidAutoSearchRequestsTest {
    @Test
    fun replacementCancelsOldWorkAndRejectsOldResults() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val requests = AndroidAutoSearchRequests<String, String>(scope) { query ->
                if (query == "old") awaitCancellation() else listOf(query)
            }
            val old = requests.start("car", "old")
            val current = requests.start("car", "new")
            assertTrue(old.isCancelled)
            assertFalse(requests.isCurrent("car", "old", old))
            assertTrue(requests.isCurrent("car", "new", current))
            assertEquals(listOf("new"), requests.result("car", "new"))
            try {
                requests.result("car", "old")
                fail("Old results must be rejected")
            } catch (_: IllegalArgumentException) { }
        } finally { scope.cancel() }
    }

    @Test
    fun repeatedQuerySharesWorkAndKeepsTheSameSnapshot() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val gate = CompletableDeferred<Unit>()
            var calls = 0
            val requests = AndroidAutoSearchRequests<String, String>(scope) {
                calls++
                gate.await()
                listOf("one", "two", "three")
            }
            val first = requests.start("car", "query")
            assertSame(first, requests.start("car", "query"))
            gate.complete(Unit)
            assertEquals(listOf("one", "two", "three"), first.await())
            assertEquals(listOf("one", "two", "three"), requests.result("car", "query"))
            assertEquals(1, calls)
        } finally { scope.cancel() }
    }

    @Test
    fun browsersDoNotCancelEachOtherAndDisconnectReleasesWork() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val requests = AndroidAutoSearchRequests<String, String>(scope) { awaitCancellation() }
            val first = requests.start("one", "query")
            val second = requests.start("two", "query")
            requests.disconnect("one")
            assertTrue(first.isCancelled)
            assertFalse(second.isCancelled)
            requests.disconnect("two")
            assertTrue(second.isCancelled)
        } finally { scope.cancel() }
    }

    @Test
    fun providerTimeoutTerminatesAndCanBeRetried() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var calls = 0
            val cancelled = CompletableDeferred<Unit>()
            val requests = AndroidAutoSearchRequests<String, String>(scope, timeoutMs = 40L) {
                calls++
                if (calls == 1) {
                    try { awaitCancellation() } finally { cancelled.complete(Unit) }
                } else listOf("recovered")
            }
            try {
                requests.start("car", "query").await()
                fail("Provider must time out")
            } catch (_: TimeoutCancellationException) { }
            assertTrue(cancelled.isCompleted)
            assertEquals(listOf("recovered"), requests.start("car", "query").await())
        } finally { scope.cancel() }
    }

    @Test
    fun networkFailureIsNotAnEmptySuccessAndCanBeRetried() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var offline = true
            val requests = AndroidAutoSearchRequests<String, String>(scope) {
                if (offline) throw IOException("offline") else emptyList()
            }
            try {
                requests.start("car", "query").await()
                fail("Network failures must remain failures")
            } catch (_: IOException) { }
            offline = false
            assertTrue(requests.start("car", "query").await().isEmpty())
        } finally { scope.cancel() }
    }

    @Test
    fun requestStorageIsBoundedAndEvictionCancelsWork() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val requests = AndroidAutoSearchRequests<Int, String>(scope) { awaitCancellation() }
            val first = requests.start(0, "query")
            (1..32).forEach { requests.start(it, "query") }
            assertTrue(first.isCancelled)
        } finally { scope.cancel() }
    }
}
