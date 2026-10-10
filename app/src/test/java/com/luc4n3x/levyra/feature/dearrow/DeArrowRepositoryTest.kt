package com.luc4n3x.levyra.feature.dearrow

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeArrowRepositoryTest {

    @Test
    fun cacheHitAvoidsSecondFetch() {
        val calls = AtomicInteger(0)
        val repository = DeArrowRepository(
            brandingSource = {
                calls.incrementAndGet()
                successBranding()
            },
            clockMs = { 0L }
        )

        val first = runBlocking { repository.branding("abcdefghijk") }
        val second = runBlocking { repository.branding("abcdefghijk") }

        assertEquals(1, calls.get())
        assertEquals(first, second)
    }

    @Test
    fun failedFetchIsNegativelyCachedAndExpiresAfterTtl() {
        var now = 0L
        val calls = AtomicInteger(0)
        val repository = DeArrowRepository(
            brandingSource = {
                calls.incrementAndGet()
                null
            },
            clockMs = { now }
        )

        assertNull(runBlocking { repository.branding("abcdefghijk") })
        assertNull(runBlocking { repository.branding("abcdefghijk") })
        assertEquals(1, calls.get())

        now += DeArrowRepository.NEGATIVE_TTL_MS + 1L
        assertNull(runBlocking { repository.branding("abcdefghijk") })
        assertEquals(2, calls.get())
    }

    @Test
    fun successfulFetchIsPositivelyCachedAndExpiresAfterTtl() {
        var now = 0L
        val calls = AtomicInteger(0)
        val repository = DeArrowRepository(
            brandingSource = {
                calls.incrementAndGet()
                successBranding()
            },
            clockMs = { now }
        )

        val first = runBlocking { repository.branding("abcdefghijk") }
        assertTrue(first != null)
        assertEquals(1, calls.get())

        now += DeArrowRepository.POSITIVE_TTL_MS - 1L
        runBlocking { repository.branding("abcdefghijk") }
        assertEquals(1, calls.get())

        now += 2L
        runBlocking { repository.branding("abcdefghijk") }
        assertEquals(2, calls.get())
    }

    @Test
    fun inconclusiveFailureIsNotNegativeCached() {
        val calls = AtomicInteger(0)
        val repository = DeArrowRepository(
            brandingSource = { null },
            brandingOutcomeSource = {
                calls.incrementAndGet()
                DeArrowBrandingOutcome.Inconclusive
            }
        )

        assertNull(runBlocking { repository.branding("abcdefghijk") })
        assertNull(runBlocking { repository.branding("abcdefghijk") })
        assertEquals(2, calls.get())
    }

    @Test
    fun disabledRepositoryNeverFetchesAndReturnsNull() {
        val calls = AtomicInteger(0)
        val repository = DeArrowRepository(
            brandingSource = {
                calls.incrementAndGet()
                successBranding()
            },
            enabled = false
        )

        assertNull(runBlocking { repository.branding("abcdefghijk") })
        assertEquals(0, calls.get())
    }

    @Test
    fun invalidVideoIdNeverFetches() {
        val calls = AtomicInteger(0)
        val repository = DeArrowRepository(
            brandingSource = {
                calls.incrementAndGet()
                successBranding()
            }
        )

        assertNull(runBlocking { repository.branding("bad-id") })
        assertEquals(0, calls.get())
    }

    @Test
    fun concurrentRequestsForSameVideoAreDeduplicated() = runBlocking {
        val calls = AtomicInteger(0)
        val repository = DeArrowRepository(
            brandingSource = {
                calls.incrementAndGet()
                delay(50)
                successBranding()
            }
        )

        val results = (1..5).map { async { repository.branding("abcdefghijk") } }.awaitAll()

        assertEquals(1, calls.get())
        assertTrue(results.all { it == results.first() })
    }

    @Test
    fun exceptionFromSourceIsTreatedAsFailureNotThrown() {
        val repository = DeArrowRepository(
            brandingSource = { throw IllegalStateException("boom") }
        )

        assertNull(runBlocking { repository.branding("abcdefghijk") })
    }

    private fun successBranding(): DeArrowBranding = DeArrowBranding(
        titles = listOf(DeArrowTitle("Great Title", locked = true, votes = 5, original = false)),
        thumbnails = listOf(DeArrowThumbnail(timestamp = 3.0, locked = true, votes = 5, original = false))
    )

}
