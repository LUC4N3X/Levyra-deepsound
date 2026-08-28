package com.luc4n3x.levyra.feature.recognition

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LayeredRecognitionProviderTest {

    @Test
    fun fallbackIsNotCalledAfterAValidPrimaryMatch() = runBlocking {
        val calls = AtomicInteger(0)
        val expected = RecognitionOutcome.Match(RecognitionResult("Song", "Artist"))
        val provider = LayeredRecognitionProvider(
            provider(expected),
            provider { calls.incrementAndGet(); RecognitionOutcome.NoMatch }
        )

        assertEquals(expected, provider.identify(fingerprint()))
        assertEquals(0, calls.get())
    }

    @Test
    fun fallbackRunsForNoMatchAndEligibleProviderFailures() = runBlocking {
        listOf(
            RecognitionOutcome.NoMatch,
            RecognitionOutcome.Error(RecognitionErrorKind.Network),
            RecognitionOutcome.Error(RecognitionErrorKind.Timeout),
            RecognitionOutcome.Error(RecognitionErrorKind.Unavailable)
        ).forEach { primary ->
            val calls = AtomicInteger(0)
            val fallback = RecognitionOutcome.Match(RecognitionResult("Fallback", "Artist"))
            val layered = LayeredRecognitionProvider(
                provider(primary),
                provider { calls.incrementAndGet(); fallback }
            )

            assertEquals(fallback, layered.identify(fingerprint()))
            assertEquals(1, calls.get())
        }
    }

    @Test
    fun localAndCancellationFailuresNeverReachFallback() = runBlocking {
        listOf(
            RecognitionErrorKind.PermissionDenied,
            RecognitionErrorKind.Fingerprint,
            RecognitionErrorKind.Cancelled
        ).forEach { kind ->
            val calls = AtomicInteger(0)
            val primary = RecognitionOutcome.Error(kind)
            val layered = LayeredRecognitionProvider(
                provider(primary),
                provider { calls.incrementAndGet(); RecognitionOutcome.NoMatch }
            )

            assertEquals(primary, layered.identify(fingerprint()))
            assertEquals(0, calls.get())
        }
    }

    @Test
    fun coroutineCancellationIsRethrownWithoutFallback() {
        assertThrows(CancellationException::class.java) {
            runBlocking {
                val layered = LayeredRecognitionProvider(
                    provider { throw CancellationException("cancelled") },
                    provider(RecognitionOutcome.NoMatch)
                )

                layered.identify(fingerprint())
            }
        }
    }

    private fun provider(outcome: RecognitionOutcome): RecognitionProvider = provider { outcome }

    private fun provider(block: suspend () -> RecognitionOutcome): RecognitionProvider =
        object : RecognitionProvider {
            override val id = "fake"
            override suspend fun identify(fingerprint: AudioFingerprint): RecognitionOutcome = block()
        }

    private fun fingerprint() = AudioFingerprint(ShortArray(256), 16_000, 16L)
}
