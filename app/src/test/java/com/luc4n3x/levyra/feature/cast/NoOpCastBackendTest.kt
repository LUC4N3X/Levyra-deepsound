package com.luc4n3x.levyra.feature.cast

import com.luc4n3x.levyra.domain.RepeatMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoOpCastBackendTest {

    private fun sampleHandoff(): CastHandoff = CastHandoffConverter.toHandoff(
        LocalPlaybackSnapshot(
            queueIds = listOf("a", "b", "c"),
            currentIndex = 1,
            positionMs = 1_000L,
            playing = true,
            shuffle = false,
            repeatMode = RepeatMode.Off
        )
    )

    @Test
    fun reportsUnavailableAndUnavailableState() {
        val backend = NoOpCastBackend()

        assertFalse(backend.available)
        assertEquals(RemotePlaybackAvailability.Unavailable, backend.state.value.availability)
        assertFalse(backend.state.value.connected)
    }

    @Test
    fun devicesFlowIsAlwaysEmpty() = runBlocking {
        val backend = NoOpCastBackend()

        val devices = backend.devices().first()

        assertTrue(devices.isEmpty())
    }

    @Test
    fun everyOperationCompletesWithoutThrowingAndStateStaysUnavailable() = runBlocking {
        val backend = NoOpCastBackend()

        backend.connect(RemoteDevice(id = "dev-1", name = "Living Room"))
        backend.load(sampleHandoff())
        backend.play()
        backend.pause()
        backend.seekTo(5_000L)
        backend.skipToNext()
        backend.skipToPrevious()
        backend.setShuffle(true)
        backend.setRepeatMode(RepeatMode.All)
        backend.stop()
        backend.disconnect()

        assertFalse(backend.available)
        assertEquals(RemotePlaybackAvailability.Unavailable, backend.state.value.availability)
        assertFalse(backend.state.value.connected)
        assertEquals(RemotePlaybackState(), backend.state.value)
    }
}
