package com.luc4n3x.levyra.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerExpansionTest {

    @Test
    fun `dragging up raises the expansion and dragging down lowers it`() {
        assertEquals(0.5f, playerExpansionFromDrag(0f, -500f, 1000f), 0.0001f)
        assertEquals(0.5f, playerExpansionFromDrag(1f, 500f, 1000f), 0.0001f)
    }

    @Test
    fun `expansion stays inside the collapsed and expanded bounds`() {
        assertEquals(PlayerExpansionExpanded, playerExpansionFromDrag(0.9f, -4000f, 1000f), 0.0001f)
        assertEquals(PlayerExpansionCollapsed, playerExpansionFromDrag(0.1f, 4000f, 1000f), 0.0001f)
    }

    @Test
    fun `a degenerate or invalid drag keeps a safe expansion`() {
        assertEquals(0.4f, playerExpansionFromDrag(0.4f, -900f, 0f), 0.0001f)
        assertEquals(0.4f, playerExpansionFromDrag(0.4f, Float.NaN, 1000f), 0.0001f)
        assertEquals(PlayerExpansionCollapsed, playerExpansionFromDrag(Float.NaN, -900f, 1000f), 0.0001f)
    }

    @Test
    fun `a fast upward fling expands regardless of distance`() {
        assertEquals(
            PlayerExpansionExpanded,
            resolvePlayerExpansionTarget(expansion = 0.05f, velocityPx = -1800f, wasExpanded = false),
            0.0001f
        )
    }

    @Test
    fun `a fast downward fling collapses regardless of distance`() {
        assertEquals(
            PlayerExpansionCollapsed,
            resolvePlayerExpansionTarget(expansion = 0.95f, velocityPx = 1800f, wasExpanded = true),
            0.0001f
        )
    }

    @Test
    fun `opening commits earlier than closing`() {
        assertEquals(
            PlayerExpansionExpanded,
            resolvePlayerExpansionTarget(expansion = 0.4f, velocityPx = 0f, wasExpanded = false),
            0.0001f
        )
        assertEquals(
            PlayerExpansionExpanded,
            resolvePlayerExpansionTarget(expansion = 0.75f, velocityPx = 0f, wasExpanded = true),
            0.0001f
        )
        assertEquals(
            PlayerExpansionCollapsed,
            resolvePlayerExpansionTarget(expansion = 0.6f, velocityPx = 0f, wasExpanded = true),
            0.0001f
        )
        assertEquals(
            PlayerExpansionCollapsed,
            resolvePlayerExpansionTarget(expansion = 0.2f, velocityPx = 0f, wasExpanded = false),
            0.0001f
        )
    }

    @Test
    fun `the shared motion curve is smooth symmetric and bounded`() {
        assertEquals(0f, playerMotionProgress(-1f), 0.0001f)
        assertEquals(0.5f, playerMotionProgress(0.5f), 0.0001f)
        assertEquals(1f, playerMotionProgress(2f), 0.0001f)
        assertEquals(0f, playerMotionProgress(Float.NaN), 0.0001f)
        assertEquals(1f - playerMotionProgress(0.25f), playerMotionProgress(0.75f), 0.0001f)
    }

    @Test
    fun `the mini player remains visible during the handoff`() {
        assertEquals(1f, playerChromeAlpha(0f), 0.0001f)
        assertEquals(1f, playerChromeAlpha(0.05f), 0.0001f)
        assertTrue(playerChromeAlpha(0.3f) > 0.65f)
        assertEquals(0f, playerChromeAlpha(0.72f), 0.0001f)
        assertEquals(0f, playerChromeAlpha(1f), 0.0001f)
    }

    @Test
    fun `the full player emerges early behind the flying artwork`() {
        assertEquals(0f, playerSurfaceAlpha(0f), 0.0001f)
        assertEquals(0f, playerSurfaceAlpha(0.015f), 0.0001f)
        assertTrue(playerSurfaceAlpha(0.25f) in 0.45f..0.55f)
        assertEquals(1f, playerSurfaceAlpha(0.5f), 0.0001f)
        assertEquals(1f, playerSurfaceAlpha(1f), 0.0001f)
    }

    @Test
    fun `the full player rises and scales into its final surface`() {
        assertEquals(0.94f, playerSurfaceScale(0f), 0.0001f)
        assertEquals(0.97f, playerSurfaceScale(0.5f), 0.0001f)
        assertEquals(1f, playerSurfaceScale(1f), 0.0001f)
        assertEquals(0.055f, playerSurfaceLiftFraction(0f), 0.0001f)
        assertTrue(playerSurfaceLiftFraction(0.5f) in 0.013f..0.014f)
        assertEquals(0f, playerSurfaceLiftFraction(1f), 0.0001f)
    }

    @Test
    fun `the artwork morph uses almost the entire gesture`() {
        assertFalse(playerMorphActive(0f))
        assertTrue(playerMorphActive(0.2f))
        assertTrue(playerMorphActive(0.99f))
        assertFalse(playerMorphActive(1f))
        assertEquals(1f, playerMorphFraction(0.96f), 0.0001f)
        assertEquals(1f, playerMorphFraction(1f), 0.0001f)
        assertEquals(0.5f, playerMorphFraction(0.48f), 0.0001f)
        assertTrue(playerMorphFraction(0.9f) < 1f)
    }

    @Test
    fun `the background depth stays subtle and readable`() {
        assertEquals(1f, playerBackgroundScale(0f), 0.0001f)
        assertEquals(0.972f, playerBackgroundScale(1f), 0.0001f)
        assertTrue(playerBackgroundScale(0.5f) > 0.98f)
    }
}
