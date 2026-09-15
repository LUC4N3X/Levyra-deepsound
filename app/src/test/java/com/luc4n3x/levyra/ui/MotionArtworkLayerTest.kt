package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.feature.motion.MotionArtwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkLayerTest {

    @Test
    fun livingArtworkRunsOnlyWhenEveryGateAllowsIt() {
        assertTrue(
            livingArtworkActive(
                enabled = true,
                lifecycleActive = true,
                localAllowed = true,
                isPlaying = true,
                realCanvasReady = false
            )
        )
    }

    @Test
    fun livingArtworkStopsWhenAnyGateCloses() {
        val blockedStates = listOf(
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, true, true, false),
            booleanArrayOf(true, true, false, true, false),
            booleanArrayOf(true, true, true, false, false),
            booleanArrayOf(true, true, true, true, true)
        )

        blockedStates.forEach { state ->
            assertFalse(
                livingArtworkActive(
                    enabled = state[0],
                    lifecycleActive = state[1],
                    localAllowed = state[2],
                    isPlaying = state[3],
                    realCanvasReady = state[4]
                )
            )
        }
    }

    @Test
    fun sameTrackProviderUpgradeKeepsPreviousMotionUntilHandoff() {
        val apple = motion(identity = "track-a", url = "https://apple.example/a.m3u8")
        val community = motion(identity = "track-a", url = "https://canvaz.example/a.mp4")

        assertEquals(apple, retainedMotionArtwork(displayed = apple, incoming = community, gatesOpen = true))
    }

    @Test
    fun trackChangeNeverRetainsPreviousTrackMotion() {
        val previous = motion(identity = "track-a", url = "https://canvaz.example/a.mp4")
        val next = motion(identity = "track-b", url = "https://canvaz.example/b.mp4")

        assertNull(retainedMotionArtwork(displayed = previous, incoming = next, gatesOpen = true))
        assertNull(retainedMotionArtwork(displayed = previous, incoming = null, gatesOpen = true))
    }

    @Test
    fun closedGatesOrSameAssetRetainNothing() {
        val current = motion(identity = "track-a", url = "https://canvaz.example/a.mp4")
        val upgrade = motion(identity = "track-a", url = "https://apple.example/a.m3u8")

        assertNull(retainedMotionArtwork(displayed = current, incoming = upgrade, gatesOpen = false))
        assertNull(retainedMotionArtwork(displayed = current, incoming = current, gatesOpen = true))
        assertNull(retainedMotionArtwork(displayed = null, incoming = upgrade, gatesOpen = true))
    }

    private fun motion(identity: String, url: String): MotionArtwork = MotionArtwork(
        identityKey = identity,
        provider = "test",
        url = url,
        mimeType = "video/mp4",
        width = null,
        height = null,
        confidence = 100,
        expiresAtMs = Long.MAX_VALUE,
        lastVerifiedAtMs = 0L,
        configEpoch = 1L
    )
}
