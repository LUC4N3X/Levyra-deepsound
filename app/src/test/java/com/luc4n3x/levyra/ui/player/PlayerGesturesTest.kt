package com.luc4n3x.levyra.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerGesturesTest {

    @Test
    fun `edge zones map to brightness and volume and the middle stays free`() {
        assertEquals(PlayerGestureZone.BrightnessEdge, playerGestureZone(0.02f))
        assertEquals(PlayerGestureZone.Center, playerGestureZone(0.5f))
        assertEquals(PlayerGestureZone.VolumeEdge, playerGestureZone(0.97f))
    }

    @Test
    fun `edge zones mirror in right to left layouts`() {
        assertEquals(PlayerGestureZone.VolumeEdge, playerGestureZone(0.02f, rightToLeft = true))
        assertEquals(PlayerGestureZone.BrightnessEdge, playerGestureZone(0.97f, rightToLeft = true))
    }

    @Test
    fun `out of range and invalid positions are normalized safely`() {
        assertEquals(PlayerGestureZone.BrightnessEdge, playerGestureZone(-4f))
        assertEquals(PlayerGestureZone.VolumeEdge, playerGestureZone(9f))
        assertEquals(PlayerGestureZone.Center, playerGestureZone(Float.NaN))
    }

    @Test
    fun `the drag axis stays undecided below the slop`() {
        assertEquals(PlayerDragAxis.Undecided, resolvePlayerDragAxis(2f, 3f))
        assertEquals(PlayerDragAxis.Horizontal, resolvePlayerDragAxis(40f, 6f))
        assertEquals(PlayerDragAxis.Vertical, resolvePlayerDragAxis(6f, 40f))
        assertEquals(PlayerDragAxis.Undecided, resolvePlayerDragAxis(Float.NaN, Float.NaN))
    }

    @Test
    fun `a dominant axis wins even when both exceed the slop`() {
        assertEquals(PlayerDragAxis.Horizontal, resolvePlayerDragAxis(60f, 59f))
        assertEquals(PlayerDragAxis.Vertical, resolvePlayerDragAxis(59f, 60f))
    }

    @Test
    fun `short swipes settle back instead of changing track`() {
        assertEquals(PlayerSwipeResult.Settle, resolvePlayerSwipe(40f, 20f, 1000f))
        assertEquals(PlayerSwipeResult.Settle, resolvePlayerSwipe(-40f, -20f, 1000f))
    }

    @Test
    fun `long swipes change track in the dragged direction`() {
        assertEquals(PlayerSwipeResult.Next, resolvePlayerSwipe(-300f, 0f, 1000f))
        assertEquals(PlayerSwipeResult.Previous, resolvePlayerSwipe(300f, 0f, 1000f))
    }

    @Test
    fun `a committed distance cannot be reversed by a release counter flick`() {
        assertEquals(PlayerSwipeResult.Next, resolvePlayerSwipe(-300f, 900f, 1000f))
        assertEquals(PlayerSwipeResult.Previous, resolvePlayerSwipe(300f, -900f, 1000f))
    }

    @Test
    fun `a fling changes track when distance has not committed`() {
        assertEquals(PlayerSwipeResult.Next, resolvePlayerSwipe(-10f, -1500f, 1000f))
        assertEquals(PlayerSwipeResult.Previous, resolvePlayerSwipe(10f, 1500f, 1000f))
    }

    @Test
    fun `invalid swipe samples settle instead of selecting a track`() {
        assertEquals(PlayerSwipeResult.Settle, resolvePlayerSwipe(Float.NaN, Float.NaN, Float.NaN))
    }

    @Test
    fun `the mini player needs a deliberate downward swipe to be dismissed`() {
        assertEquals(PlayerVerticalResult.Settle, resolveMiniPlayerDismiss(-90f, -900f, 200f))
        assertEquals(PlayerVerticalResult.Settle, resolveMiniPlayerDismiss(40f, 200f, 200f))
        assertEquals(PlayerVerticalResult.Collapse, resolveMiniPlayerDismiss(160f, 0f, 200f))
        assertEquals(PlayerVerticalResult.Collapse, resolveMiniPlayerDismiss(20f, 1400f, 200f))
    }

    @Test
    fun `a small downward nudge on the mini player is ignored`() {
        assertEquals(PlayerVerticalResult.Settle, resolveMiniPlayerDismiss(60f, 0f, 200f))
        assertEquals(PlayerVerticalResult.Settle, resolveMiniPlayerDismiss(0f, 0f, 200f))
        assertEquals(PlayerVerticalResult.Settle, resolveMiniPlayerDismiss(Float.NaN, Float.NaN, Float.NaN))
    }

    @Test
    fun `double tap sides seek backwards and forwards`() {
        assertEquals(PlayerTapSide.Leading, playerTapSide(0.2f))
        assertEquals(PlayerTapSide.Trailing, playerTapSide(0.8f))
        assertEquals(-10_000L, playerSeekDeltaMs(PlayerTapSide.Leading, 10))
        assertEquals(10_000L, playerSeekDeltaMs(PlayerTapSide.Trailing, 10))
    }

    @Test
    fun `seek direction mirrors in right to left layouts`() {
        assertEquals(10_000L, playerSeekDeltaMs(PlayerTapSide.Leading, 10, rightToLeft = true))
        assertEquals(-10_000L, playerSeekDeltaMs(PlayerTapSide.Trailing, 10, rightToLeft = true))
    }

    @Test
    fun `the seek step honours the persisted bounds`() {
        assertEquals(5_000L, playerSeekDeltaMs(PlayerTapSide.Trailing, 1))
        assertEquals(30_000L, playerSeekDeltaMs(PlayerTapSide.Trailing, 240))
    }

    @Test
    fun `the swipe follows the finger but stops at a third of the width`() {
        assertEquals(-120f, playerSwipeContentOffset(-120f, 1000f), 0.0001f)
        assertEquals(340f, playerSwipeContentOffset(900f, 1000f), 0.0001f)
        assertEquals(-340f, playerSwipeContentOffset(-900f, 1000f), 0.0001f)
        assertEquals(0f, playerSwipeContentOffset(Float.NaN, Float.NaN), 0.0001f)
    }

    @Test
    fun `the swiped content never disappears completely`() {
        assertEquals(1f, playerSwipeContentAlpha(0f, 1000f), 0.0001f)
        assertTrue(playerSwipeContentAlpha(340f, 1000f) >= 0.5f)
        assertEquals(1f, playerSwipeContentAlpha(Float.NaN, Float.NaN), 0.0001f)
    }
}
