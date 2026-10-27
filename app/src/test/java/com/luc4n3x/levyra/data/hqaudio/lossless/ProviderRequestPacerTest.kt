package com.luc4n3x.levyra.data.hqaudio.lossless

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderRequestPacerTest {
    @Test
    fun singleRequestIsNotDelayed() = runBlocking {
        val pacer = ProviderRequestPacer(spacingMs = 150L, maxConcurrent = 2, clock = System::currentTimeMillis)
        val startedAt = System.currentTimeMillis()

        pacer.paced { }

        assertTrue(System.currentTimeMillis() - startedAt < 100L)
    }

    @Test
    fun burstIsSpacedAndConcurrencyIsCapped() = runBlocking(Dispatchers.Default) {
        val pacer = ProviderRequestPacer(spacingMs = 60L, maxConcurrent = 2, clock = System::currentTimeMillis)
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val starts = (0 until 5).map {
            async {
                pacer.paced {
                    val now = active.incrementAndGet()
                    peak.updateAndGet { maxOf(it, now) }
                    val startedAt = System.currentTimeMillis()
                    delay(120L)
                    active.decrementAndGet()
                    startedAt
                }
            }
        }.awaitAll().sorted()

        assertTrue(peak.get() <= 2)
        starts.zipWithNext().forEach { (first, second) -> assertTrue(second - first >= 50L) }
    }
}
