package com.luc4n3x.levyra.data.playlistimport

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportSearchBackoffTest {

    @Test
    fun `consecutive failures grow the shared pause up to the cap`() {
        var now = 0L
        val backoff = ImportSearchBackoff { now }
        assertEquals(0L, backoff.pendingMs())
        val pauses = List(7) {
            backoff.failed()
            backoff.pendingMs().also { now += it }
        }
        assertEquals(listOf(500L, 1_000L, 2_000L, 4_000L, 8_000L, 8_000L, 8_000L), pauses)
    }

    @Test
    fun `a waiter honours a pause extended while it was already waiting`() = runBlocking {
        val backoff = ImportSearchBackoff()
        val started = System.nanoTime()
        backoff.failed()
        val waiter = launch { backoff.await() }
        delay(400L)
        backoff.failed()
        delay(300L)
        assertTrue(waiter.isActive)
        waiter.join()
        assertTrue((System.nanoTime() - started) / 1_000_000L >= 1_350L)
    }

    @Test
    fun `success resets growth without cutting a pause already in progress`() {
        var now = 0L
        val backoff = ImportSearchBackoff { now }
        repeat(3) { backoff.failed() }
        assertEquals(2_000L, backoff.pendingMs())
        backoff.succeeded()
        assertEquals(2_000L, backoff.pendingMs())
        now += 2_000L
        backoff.failed()
        assertEquals(500L, backoff.pendingMs())
    }
}
