package com.luc4n3x.levyra.feature.motion

import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.LevyraCanvasSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionCanvasQualityPolicyTest {

    private val healthy = MotionCanvasConditions(
        unmetered = true,
        dataSaverActive = false,
        batterySaverActive = false,
        lowRamDevice = false
    )

    @Test
    fun `immersive auto preserves the main playback baseline`() {
        val profile = MotionCanvasQualityPolicy.profile(
            LevyraCanvasQuality.Auto,
            MotionCanvasSurface.Immersive,
            healthy
        )
        assertEquals(1_920, profile.maxDimensionPx)
        assertEquals(8_000_000, profile.maxBitrateBps)
        assertFalse(profile.forceHighestSupportedBitrate)
    }

    @Test
    fun `card auto preserves the main playback baseline`() {
        val profile = MotionCanvasQualityPolicy.profile(
            LevyraCanvasQuality.Auto,
            MotionCanvasSurface.Card,
            healthy
        )
        assertEquals(1_280, profile.maxDimensionPx)
        assertEquals(4_000_000, profile.maxBitrateBps)
        assertFalse(profile.forceHighestSupportedBitrate)
    }

    @Test
    fun `card stays more conservative than immersive at the same quality`() {
        val card = MotionCanvasQualityPolicy.profile(
            LevyraCanvasQuality.Auto,
            MotionCanvasSurface.Card,
            healthy
        )
        val immersive = MotionCanvasQualityPolicy.profile(
            LevyraCanvasQuality.Auto,
            MotionCanvasSurface.Immersive,
            healthy
        )
        assertTrue(card.maxDimensionPx < immersive.maxDimensionPx)
        assertTrue(card.maxBitrateBps < immersive.maxBitrateBps)
    }

    @Test
    fun `high quality raises the immersive ceiling above auto`() {
        val auto = MotionCanvasQualityPolicy.profile(
            LevyraCanvasQuality.Auto,
            MotionCanvasSurface.Immersive,
            healthy
        )
        val high = MotionCanvasQualityPolicy.profile(
            LevyraCanvasQuality.High,
            MotionCanvasSurface.Immersive,
            healthy
        )
        assertTrue(high.maxDimensionPx > auto.maxDimensionPx)
        assertTrue(high.maxBitrateBps > auto.maxBitrateBps)
        assertTrue(high.forceHighestSupportedBitrate)
    }

    @Test
    fun `auto falls back to data saver on a metered network`() {
        val profile = MotionCanvasQualityPolicy.profile(
            LevyraCanvasQuality.Auto,
            MotionCanvasSurface.Immersive,
            healthy.copy(unmetered = false)
        )
        assertEquals(720, profile.maxDimensionPx)
        assertFalse(profile.forceHighestSupportedBitrate)
    }

    @Test
    fun `high is capped to the auto tier on a metered network`() {
        val metered = MotionCanvasQualityPolicy.profile(
            LevyraCanvasQuality.High,
            MotionCanvasSurface.Immersive,
            healthy.copy(unmetered = false)
        )
        val auto = MotionCanvasQualityPolicy.profile(
            LevyraCanvasQuality.Auto,
            MotionCanvasSurface.Immersive,
            healthy
        )
        assertEquals(auto, metered)
    }

    @Test
    fun `battery saver forces the data saver tier even when high is requested`() {
        assertEquals(
            LevyraCanvasQuality.DataSaver,
            MotionCanvasQualityPolicy.effectiveQuality(
                LevyraCanvasQuality.High,
                healthy.copy(batterySaverActive = true)
            )
        )
    }

    @Test
    fun `low ram devices force the data saver tier`() {
        assertEquals(
            LevyraCanvasQuality.DataSaver,
            MotionCanvasQualityPolicy.effectiveQuality(
                LevyraCanvasQuality.High,
                healthy.copy(lowRamDevice = true)
            )
        )
    }

    @Test
    fun `android data saver forces the data saver tier`() {
        assertEquals(
            LevyraCanvasQuality.DataSaver,
            MotionCanvasQualityPolicy.effectiveQuality(
                LevyraCanvasQuality.High,
                healthy.copy(dataSaverActive = true)
            )
        )
    }

    @Test
    fun `unknown stored quality falls back to auto`() {
        assertEquals(LevyraCanvasQuality.Auto, LevyraCanvasQuality.from("ultra"))
        assertEquals(LevyraCanvasQuality.High, LevyraCanvasQuality.from("high"))
    }

    @Test
    fun `auto source keeps the configured provider order`() {
        val order = listOf("community-canvas", "apple-motion", "tidal-video-cover")
        assertEquals(order, motionArtworkProviderOrder(order, LevyraCanvasSource.Auto))
    }

    @Test
    fun `forced source restricts resolution to that provider`() {
        val order = listOf("community-canvas", "apple-motion", "tidal-video-cover")
        assertEquals(listOf("apple-motion"), motionArtworkProviderOrder(order, LevyraCanvasSource.Apple))
        assertEquals(listOf("tidal-video-cover"), motionArtworkProviderOrder(order, LevyraCanvasSource.Tidal))
        assertEquals(listOf("community-canvas"), motionArtworkProviderOrder(order, LevyraCanvasSource.Community))
    }

    @Test
    fun `forced source that is not configured resolves to no provider`() {
        assertEquals(emptyList<String>(), motionArtworkProviderOrder(listOf("community-canvas"), LevyraCanvasSource.Apple))
    }

    @Test
    fun `cache keys are namespaced per forced source`() {
        assertEquals("abc", motionArtworkCacheKey("abc", LevyraCanvasSource.Auto))
        assertEquals("abc#apple", motionArtworkCacheKey("abc", LevyraCanvasSource.Apple))
        assertEquals("abc#tidal", motionArtworkCacheKey("abc", LevyraCanvasSource.Tidal))
    }

    @Test
    fun `unknown stored source falls back to auto`() {
        assertEquals(LevyraCanvasSource.Auto, LevyraCanvasSource.from(""))
        assertEquals(LevyraCanvasSource.Community, LevyraCanvasSource.from("community"))
    }
}
