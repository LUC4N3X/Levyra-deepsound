package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStageTest {

    @Test
    fun `veil settles exactly where the console content starts`() {
        val metrics = playerStageMetrics(availableHeightDp = 920f, consoleHeightDp = 336f)
        val consoleTop = 920f - 336f
        assertEquals(consoleTop / metrics.stageHeightDp, metrics.veilSettleFraction, 0.0001f)
    }

    @Test
    fun `stage height only depends on the layout, never on canvas availability`() {
        val first = playerStageMetrics(availableHeightDp = 920f, consoleHeightDp = 450f)
        val second = playerStageMetrics(availableHeightDp = 920f, consoleHeightDp = 450f)
        assertEquals(first.stageHeightDp, second.stageHeightDp, 0.0001f)
        assertEquals(920f * PlayerStageHeightFraction, first.stageHeightDp, 0.0001f)
    }

    @Test
    fun `stage always overlaps the console so the handover never leaves a gap`() {
        val metrics = playerStageMetrics(availableHeightDp = 640f, consoleHeightDp = 400f)
        val consoleTop = 640f - 400f
        assertTrue(metrics.stageHeightDp >= consoleTop + PlayerStageMinOverlapDp)
    }

    @Test
    fun `stage never exceeds the available height`() {
        val metrics = playerStageMetrics(availableHeightDp = 520f, consoleHeightDp = 500f)
        assertTrue(metrics.stageHeightDp <= 520f)
        assertTrue(metrics.veilSettleFraction in 0.35f..0.98f)
    }

    @Test
    fun `degenerate constraints stay finite`() {
        val metrics = playerStageMetrics(availableHeightDp = 0f, consoleHeightDp = 300f)
        assertEquals(0f, metrics.stageHeightDp, 0.0001f)
        assertTrue(metrics.veilSettleFraction.isFinite())
    }

    @Test
    fun `a nine by sixteen canvas covers the staged player area without side bars`() {
        val fit = motionArtworkFit(
            videoWidth = 1080,
            videoHeight = 1920,
            pixelWidthHeightRatio = 1f,
            containerWidth = 1080,
            containerHeight = 1551,
            maxZoom = MotionArtworkStageMaxZoom
        )
        val epsilon = 0.0001f
        assertTrue(fit.scaleX >= 1f - epsilon)
        assertTrue(fit.scaleY >= 1f - epsilon)
    }

    @Test
    fun `the crop budgets stay pinned`() {
        assertEquals(1.32f, MotionArtworkImmersiveMaxZoom, 0.0001f)
        assertEquals(2.6f, MotionArtworkCardMaxZoom, 0.0001f)
        assertEquals(1.56f, MotionArtworkStageMaxZoom, 0.0001f)
    }
}
