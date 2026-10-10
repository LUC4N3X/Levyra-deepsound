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
    fun artistChangeNeverRetainsPreviousArtistMotion() {
        val previous = motion(identity = "artist:a", url = "https://apple.example/a.m3u8")
        val next = motion(identity = "artist:b", url = "https://apple.example/b.m3u8")

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

    @Test
    fun handoffComposesOnlyOneVideoPlayerAtATime() {
        val outgoing = motion(identity = "track-a", url = "https://apple.example/a.m3u8")
        val incoming = motion(identity = "track-a", url = "https://canvaz.example/a.mp4")

        assertEquals(outgoing, motionVideoSlot(retained = outgoing, incoming = incoming, handoffCaptured = false))
        assertEquals(incoming, motionVideoSlot(retained = outgoing, incoming = incoming, handoffCaptured = true))
        assertEquals(incoming, motionVideoSlot(retained = null, incoming = incoming, handoffCaptured = true))
        assertNull(motionVideoSlot(retained = null, incoming = null, handoffCaptured = true))
    }

    @Test
    fun pageMotionShowsStaticCoverImmediatelyWhileCanvasIsResolving() {
        assertTrue(
            motionStaticBedVisible(motionVisible = false)
        )
    }

    @Test
    fun pageMotionKeepsStaticCoverVisibleUntilFirstVideoFrame() {
        assertTrue(
            motionStaticBedVisible(motionVisible = false)
        )
    }

    @Test
    fun pageMotionReplacesStaticCoverAfterFirstVideoFrame() {
        assertFalse(
            motionStaticBedVisible(motionVisible = true)
        )
    }

    @Test
    fun artistHeroCanDisableStaticKenBurnsWithoutDisablingMotionVideo() {
        assertFalse(
            staticArtworkMotionActive(
                enabled = true,
                staticArtworkMotionEnabled = false,
                decorativeMotion = true,
                lifecycleActive = true,
                localAllowed = true,
                layerActive = true,
                staticBedVisible = true
            )
        )
        assertTrue(
            staticArtworkMotionActive(
                enabled = true,
                staticArtworkMotionEnabled = true,
                decorativeMotion = true,
                lifecycleActive = true,
                localAllowed = true,
                layerActive = true,
                staticBedVisible = true
            )
        )
    }

    @Test
    fun pageMotionShowsStaticCoverAfterConclusiveMissOrVideoFailure() {
        assertTrue(
            motionStaticBedVisible(motionVisible = false)
        )
        assertTrue(
            motionStaticBedVisible(motionVisible = false)
        )
    }

    @Test
    fun nowPlayingStaticCoverBehaviorDoesNotChange() {
        assertTrue(
            motionStaticBedVisible(motionVisible = false)
        )
        assertFalse(
            motionStaticBedVisible(motionVisible = true)
        )
    }

    @Test
    fun dynamicBackdropPaletteTracksCanvasFrameBands() {
        val red = 0xFFFF2020.toInt()
        val blue = 0xFF2040FF.toInt()
        val palette = motionBackdropPalette(
            identityKey = "track-a",
            pixels = intArrayOf(red, red, blue, blue),
            width = 2,
            height = 2
        )

        assertEquals("track-a", palette?.identityKey)
        assertTrue((palette?.primary?.red ?: 0f) > (palette?.primary?.blue ?: 1f))
        assertTrue((palette?.secondary?.blue ?: 0f) > (palette?.secondary?.red ?: 1f))
    }

    @Test
    fun dynamicBackdropPaletteRejectsBlankFrames() {
        assertNull(
            motionBackdropPalette(
                identityKey = "track-a",
                pixels = intArrayOf(
                    0xFF000000.toInt(),
                    0x00000000,
                    0xFF000000.toInt(),
                    0x00000000
                ),
                width = 2,
                height = 2
            )
        )
    }

    @Test
    fun cinematicZoomDoesNotChangeArtistImmersiveCrop() {
        assertEquals(1.32f, motionArtworkMaxZoom(MotionArtworkPresentation.Immersive), 0f)
        assertEquals(MotionArtworkCinematicMaxZoom, motionArtworkMaxZoom(MotionArtworkPresentation.Cinematic), 0f)
        assertEquals(MotionArtworkCardMaxZoom, motionArtworkMaxZoom(MotionArtworkPresentation.Card), 0f)
        assertTrue(MotionArtworkCinematicMaxZoom > MotionArtworkImmersiveMaxZoom)
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
