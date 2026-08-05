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
    fun `a degenerate travel keeps the current expansion`() {
        assertEquals(0.4f, playerExpansionFromDrag(0.4f, -900f, 0f), 0.0001f)
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
    fun `the bottom chrome fades out before the expansion completes`() {
        assertEquals(1f, playerChromeAlpha(0f), 0.0001f)
        assertTrue(playerChromeAlpha(0.3f) < 0.6f)
        assertEquals(0f, playerChromeAlpha(0.625f), 0.0001f)
        assertEquals(0f, playerChromeAlpha(1f), 0.0001f)
    }

    @Test
    fun `the player surface stays hidden at the start of the drag`() {
        assertEquals(0f, playerSurfaceAlpha(0f), 0.0001f)
        assertEquals(0f, playerSurfaceAlpha(0.08f), 0.0001f)
        assertTrue(playerSurfaceAlpha(0.3f) > 0f)
        assertEquals(1f, playerSurfaceAlpha(1f), 0.0001f)
    }

    @Test
    fun `the artwork morph runs between the collapsed and expanded ends`() {
        assertFalse(playerMorphActive(0f))
        assertTrue(playerMorphActive(0.2f))
        assertTrue(playerMorphActive(0.99f))
        assertFalse(playerMorphActive(1f))
        assertEquals(1f, playerMorphFraction(0.86f), 0.0001f)
        assertEquals(1f, playerMorphFraction(1f), 0.0001f)
        assertTrue(playerMorphFraction(0.43f) in 0.49f..0.51f)
    }

    @Test
    fun `the background never scales below the readable floor`() {
        assertEquals(1f, playerBackgroundScale(0f), 0.0001f)
        assertEquals(0.94f, playerBackgroundScale(1f), 0.0001f)
    }
}
