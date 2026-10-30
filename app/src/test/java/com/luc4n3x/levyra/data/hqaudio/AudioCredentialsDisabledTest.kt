package com.luc4n3x.levyra.data.hqaudio

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCredentialsDisabledTest {
    @Test fun noSharedAccountCredentialsInBundledOrRuntimeConfiguration() = runBlocking {
        val bundled = AudioCredentials.embedded()
        assertTrue(bundled.qobuz.isEmpty())
        assertTrue(bundled.tidal.refreshTokens.isEmpty())
        assertTrue(bundled.tidal.tvClientId.isEmpty())
        assertTrue(bundled.tidal.tvClientSecret.isEmpty())
        assertTrue(bundled.tidal.mobileClientId.isEmpty())

        val runtime = AudioCredentialsRuntime.current()
        assertTrue(runtime.qobuz.isEmpty())
        assertTrue(runtime.tidal.refreshTokens.isEmpty())
        assertEquals(runtime, AudioCredentialsRuntime.refreshAfterRejection(runtime))
    }
}
