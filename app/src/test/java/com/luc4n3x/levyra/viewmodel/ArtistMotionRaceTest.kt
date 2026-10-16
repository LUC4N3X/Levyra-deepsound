package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.feature.motion.MotionArtwork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistMotionRaceTest {
    @Test
    fun `fast track canvas is not held back by a slow artist lookup`() = runBlocking {
        val slowArtistCancelled = CompletableDeferred<Unit>()
        val result = withTimeout(2_000L) {
            firstAvailableMotionArtwork(
                listOf(
                    {
                        try {
                            awaitCancellation()
                        } catch (cancelled: CancellationException) {
                            slowArtistCancelled.complete(Unit)
                            throw cancelled
                        }
                    },
                    { artwork("track") }
                )
            )
        }

        assertEquals("track", result?.provider)
        withTimeout(1_000L) { slowArtistCancelled.await() }
        assertTrue(slowArtistCancelled.isCompleted)
    }

    @Test
    fun `misses are skipped until a real artwork arrives`() = runBlocking {
        val result = firstAvailableMotionArtwork(
            listOf(
                { null },
                {
                    delay(20L)
                    artwork("artist")
                },
                { null }
            )
        )

        assertEquals("artist", result?.provider)
    }

    @Test
    fun `all misses resolve to no motion artwork`() = runBlocking {
        assertNull(firstAvailableMotionArtwork(listOf({ null }, { null })))
        assertNull(firstAvailableMotionArtwork(emptyList()))
    }

    private fun artwork(provider: String) = MotionArtwork(
        identityKey = "id-$provider",
        provider = provider,
        url = "https://media.example/$provider.m3u8",
        mimeType = "application/x-mpegURL",
        width = 1080,
        height = 1080,
        confidence = 90,
        expiresAtMs = Long.MAX_VALUE,
        lastVerifiedAtMs = 0L,
        configEpoch = 1L
    )
}
