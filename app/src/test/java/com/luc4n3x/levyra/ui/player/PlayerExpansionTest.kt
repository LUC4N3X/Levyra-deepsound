package com.luc4n3x.levyra.ui.player

import androidx.compose.ui.geometry.Rect
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
    fun `the mini player stays readable deeper into the handoff`() {
        assertEquals(1f, playerChromeAlpha(0f), 0.0001f)
        assertEquals(1f, playerChromeAlpha(0.02f), 0.0001f)
        assertTrue(playerChromeAlpha(0.2f) in 0.68f..0.74f)
        assertTrue(playerChromeAlpha(0.4f) in 0.10f..0.20f)
        assertEquals(0f, playerChromeAlpha(0.52f), 0.0001f)
        assertEquals(0f, playerChromeAlpha(1f), 0.0001f)
    }

    @Test
    fun `the full player becomes opaque early so the growing container reads as one surface`() {
        assertEquals(0f, playerSurfaceAlpha(0f), 0.0001f)
        assertEquals(0.5f, playerSurfaceAlpha(0.1f), 0.0001f)
        assertEquals(1f, playerSurfaceAlpha(0.2f), 0.0001f)
        assertEquals(1f, playerSurfaceAlpha(1f), 0.0001f)
    }

    @Test
    fun `the player container grows out of the mini player card`() {
        val card = Rect(24f, 2400f, 1056f, 2560f)
        assertEquals(card, playerContainerBounds(0f, card, 1080f, 2400f + 600f))
        assertEquals(Rect(0f, 0f, 1080f, 3000f), playerContainerBounds(1f, card, 1080f, 3000f))
        assertEquals(Rect(12f, 1200f, 1068f, 2780f), playerContainerBounds(0.5f, card, 1080f, 3000f))
        assertEquals(Rect(0f, 0f, 1080f, 3000f), playerContainerBounds(2f, card, 1080f, 3000f))
        assertEquals(Rect(0f, 2640f, 1080f, 3000f), playerContainerBounds(0f, null, 1080f, 3000f))
    }

    @Test
    fun `the container sheds the pill corners as it opens`() {
        val card = Rect(24f, 2400f, 1056f, 2560f)
        assertEquals(80f, playerContainerCornerPx(0f, card), 0.0001f)
        assertEquals(40f, playerContainerCornerPx(0.5f, card), 0.0001f)
        assertEquals(0f, playerContainerCornerPx(1f, card), 0.0001f)
        assertEquals(0f, playerContainerCornerPx(0.3f, null), 0.0001f)
    }

    @Test
    fun `player anchors follow the container while it travels`() {
        val container = Rect(12f, 1200f, 1068f, 2780f)
        val artwork = Rect(108f, 300f, 972f, 1164f)
        val placed = artwork.placedInContainer(container, 1080f)
        val scale = 1056f / 1080f
        assertEquals(12f + 108f * scale, placed.left, 0.01f)
        assertEquals(1200f + 300f * scale, placed.top, 0.01f)
        assertEquals(artwork.width * scale, placed.width, 0.01f)
        assertEquals(artwork, artwork.placedInContainer(Rect(0f, 0f, 1080f, 3000f), 1080f))
        assertEquals(artwork, artwork.placedInContainer(container, 0f))
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

    @Test
    fun `the idle flight cover stays decoded at the landing size`() {
        val anchors = PlayerMorphAnchors()
        assertEquals(Rect(0f, 0f, 1080f, 1080f), anchors.idleBounds(1080f))
        assertEquals(null, anchors.idleBounds(0f))
        assertEquals(null, anchors.idleBounds(Float.NaN))

        val full = Rect(60f, 300f, 1020f, 1260f)
        anchors.update(PlayerMorphSlot.Full, full)
        assertEquals(full, anchors.idleBounds(1080f))

        val stage = Rect(0f, 0f, 1080f, 1350f)
        anchors.update(PlayerMorphSlot.Stage, stage)
        anchors.updateStageActive(true)
        assertEquals(stage, anchors.idleBounds(1080f))
    }
}
