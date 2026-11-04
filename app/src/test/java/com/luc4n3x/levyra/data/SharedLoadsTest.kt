package com.luc4n3x.levyra.data

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedLoadsTest {
    @Test
    fun `concurrent loads for one key run a single producer`() = runBlocking {
        val loads = SharedLoads<String, Int>()
        val calls = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val first = async(start = CoroutineStart.UNDISPATCHED) {
            loads.load("cover") {
                calls.incrementAndGet()
                gate.await()
                7
            }
        }
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            loads.load("cover") {
                calls.incrementAndGet()
                9
            }
        }
        gate.complete(Unit)

        assertEquals(7, first.await())
        assertEquals(7, second.await())
        assertEquals(1, calls.get())
        assertEquals(0, loads.pendingCount)
    }

    @Test
    fun `a waiter takes over when the owner is cancelled`() = runBlocking {
        val loads = SharedLoads<String, Int>()
        val gate = CompletableDeferred<Unit>()
        val owner = async(start = CoroutineStart.UNDISPATCHED) {
            loads.load("cover") {
                gate.await()
                1
            }
        }
        val waiter = async(start = CoroutineStart.UNDISPATCHED) {
            loads.load("cover") { 2 }
        }
        owner.cancel()

        assertEquals(2, waiter.await())
        assertEquals(0, loads.pendingCount)
    }

    @Test
    fun `different keys load independently`() = runBlocking {
        val loads = SharedLoads<String, Int>()

        assertEquals(1, loads.load("a") { 1 })
        assertEquals(2, loads.load("b") { 2 })
        assertEquals(0, loads.pendingCount)
    }
}
