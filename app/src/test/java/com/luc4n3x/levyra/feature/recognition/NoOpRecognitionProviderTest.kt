package com.luc4n3x.levyra.feature.recognition

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class NoOpRecognitionProviderTest {

    @Test
    fun alwaysReturnsUnavailableError() = runBlocking {
        val fingerprint = AudioFingerprint(samples = ShortArray(10), sampleRateHz = 16_000, durationMs = 625L)

        val outcome = NoOpRecognitionProvider.identify(fingerprint)

        assertEquals(RecognitionOutcome.Error(RecognitionErrorKind.Unavailable), outcome)
    }

    @Test
    fun exposesStableProviderId() {
        assertEquals("noop", NoOpRecognitionProvider.id)
    }
}
