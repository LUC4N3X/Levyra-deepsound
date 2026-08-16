package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkScalingTest {

    @Test
    fun `unknown video size keeps the surface untouched`() {
        assertEquals(
            MotionArtworkFitIdentity,
            motionArtworkFit(0, 0, 1f, 1080, 2400, MotionArtworkImmersiveMaxZoom)
        )
        assertEquals(
            MotionArtworkFitIdentity,
            motionArtworkFit(1080, 1920, 1f, 0, 0, MotionArtworkImmersiveMaxZoom)
        )
    }

    @Test
    fun `matching aspect ratio needs no correction`() {
        val fit = motionArtworkFit(1080, 1920, 1f, 540, 960, MotionArtworkImmersiveMaxZoom)
        assertEquals(1f, fit.scaleX, 0.0001f)
        assertEquals(1f, fit.scaleY, 0.0001f)
    }

    @Test
    fun `nine by sixteen canvas covers a taller screen without distortion`() {
        val fit = motionArtworkFit(1080, 1920, 1f, 1080, 2340, MotionArtworkImmersiveMaxZoom)
        val videoAspect = 1080f / 1920f
        val containerAspect = 1080f / 2340f
        assertEquals(videoAspect / containerAspect, fit.scaleX / fit.scaleY, 0.0001f)
        assertEquals(1f, fit.scaleY, 0.0001f)
        assertTrue(fit.scaleX > 1f)
    }

    @Test
    fun `square canvas on a tall screen stops at the crop budget instead of filling`() {
        val fit = motionArtworkFit(1080, 1080, 1f, 1080, 2340, MotionArtworkImmersiveMaxZoom)
        val containerAspect = 1080f / 2340f
        assertEquals(1f / containerAspect, fit.scaleX / fit.scaleY, 0.0001f)
        assertEquals(MotionArtworkImmersiveMaxZoom, fit.scaleX, 0.0001f)
        assertEquals(MotionArtworkImmersiveMaxZoom * containerAspect, fit.scaleY, 0.0001f)
        assertTrue(fit.scaleY < 1f)
    }

    @Test
    fun `card presentation still covers a nine by sixteen canvas`() {
        val fit = motionArtworkFit(720, 1280, 1f, 900, 900, MotionArtworkCardMaxZoom)
        assertEquals(1f, fit.scaleX, 0.0001f)
        assertEquals(1280f / 720f, fit.scaleY, 0.0001f)
    }

    @Test
    fun `non square pixels are folded into the video aspect ratio`() {
        val fit = motionArtworkFit(960, 1080, 2f, 1080, 1080, MotionArtworkCardMaxZoom)
        assertEquals(2f * 960f / 1080f, fit.scaleX / fit.scaleY, 0.0001f)
    }

    @Test
    fun `invalid pixel ratio falls back to square pixels`() {
        val fit = motionArtworkFit(1080, 1920, 0f, 1080, 1920, MotionArtworkCardMaxZoom)
        assertEquals(1f, fit.scaleX, 0.0001f)
        assertEquals(1f, fit.scaleY, 0.0001f)
    }

    @Test
    fun `max zoom below one never shrinks past the contain fit`() {
        val fit = motionArtworkFit(1080, 1080, 1f, 1080, 2340, 0.2f)
        assertEquals(1f, fit.scaleX, 0.0001f)
        assertEquals(1080f / 2340f, fit.scaleY, 0.0001f)
    }
}
