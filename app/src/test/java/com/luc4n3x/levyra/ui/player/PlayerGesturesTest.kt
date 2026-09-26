package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.domain.PlayerDoubleTapAction
import com.luc4n3x.levyra.domain.PlayerLongPressAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val leftVelocity = playerSwipeReleaseVelocity(-300f, 900f, 24f)
        val rightVelocity = playerSwipeReleaseVelocity(300f, -900f, 24f)
        assertEquals(PlayerSwipeResult.Next, resolvePlayerSwipe(-300f, leftVelocity, 1000f))
        assertEquals(PlayerSwipeResult.Previous, resolvePlayerSwipe(300f, rightVelocity, 1000f))
    }

    @Test
    fun `a fling changes track once it travels past the fling distance`() {
        val leftVelocity = playerSwipeReleaseVelocity(-80f, -1500f, 24f)
        val rightVelocity = playerSwipeReleaseVelocity(80f, 1500f, 24f)
        assertEquals(-1500f, leftVelocity, 0f)
        assertEquals(1500f, rightVelocity, 0f)
        assertEquals(PlayerSwipeResult.Next, resolvePlayerSwipe(-80f, leftVelocity, 1000f))
        assertEquals(PlayerSwipeResult.Previous, resolvePlayerSwipe(80f, rightVelocity, 1000f))
    }

    @Test
    fun `a fast micro movement never changes track`() {
        val leftVelocity = playerSwipeReleaseVelocity(-10f, -1500f, 24f)
        val rightVelocity = playerSwipeReleaseVelocity(10f, 1500f, 24f)
        assertEquals(0f, leftVelocity, 0f)
        assertEquals(0f, rightVelocity, 0f)
        assertEquals(PlayerSwipeResult.Settle, resolvePlayerSwipe(-10f, leftVelocity, 1000f))
        assertEquals(PlayerSwipeResult.Settle, resolvePlayerSwipe(10f, rightVelocity, 1000f))
    }

    @Test
    fun `a fling against the travel direction settles`() {
        val leftVelocity = playerSwipeReleaseVelocity(-80f, 1500f, 24f)
        val rightVelocity = playerSwipeReleaseVelocity(80f, -1500f, 24f)
        assertEquals(0f, leftVelocity, 0f)
        assertEquals(0f, rightVelocity, 0f)
        assertEquals(PlayerSwipeResult.Settle, resolvePlayerSwipe(-80f, leftVelocity, 1000f))
        assertEquals(PlayerSwipeResult.Settle, resolvePlayerSwipe(80f, rightVelocity, 1000f))
    }

    @Test
    fun `invalid swipe samples settle instead of selecting a track`() {
        assertEquals(PlayerSwipeResult.Settle, resolvePlayerSwipe(Float.NaN, Float.NaN, Float.NaN))
        assertEquals(0f, playerSwipeReleaseVelocity(Float.NaN, 1500f, 24f), 0f)
        assertEquals(0f, playerSwipeReleaseVelocity(80f, Float.NaN, 24f), 0f)
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
    fun `double tap preferences map to one shared command set`() {
        assertEquals(
            PlayerGestureCommand.SeekLeading,
            playerDoubleTapCommand(PlayerDoubleTapAction.Seek, PlayerTapSide.Leading)
        )
        assertEquals(
            PlayerGestureCommand.SeekTrailing,
            playerDoubleTapCommand(PlayerDoubleTapAction.Seek, PlayerTapSide.Trailing)
        )
        assertEquals(
            PlayerGestureCommand.TogglePlayback,
            playerDoubleTapCommand(PlayerDoubleTapAction.PlayPause, PlayerTapSide.Leading)
        )
        assertEquals(
            PlayerGestureCommand.ToggleFavorite,
            playerDoubleTapCommand(PlayerDoubleTapAction.Favorite, PlayerTapSide.Trailing)
        )
        assertEquals(
            PlayerGestureCommand.None,
            playerDoubleTapCommand(PlayerDoubleTapAction.Disabled, PlayerTapSide.Leading)
        )
    }

    @Test
    fun `long press preferences map without depending on a player skin`() {
        assertEquals(PlayerGestureCommand.TemporarySpeed, playerLongPressCommand(PlayerLongPressAction.Speed))
        assertEquals(PlayerGestureCommand.ToggleFavorite, playerLongPressCommand(PlayerLongPressAction.Favorite))
        assertEquals(PlayerGestureCommand.OpenQueue, playerLongPressCommand(PlayerLongPressAction.Queue))
        assertEquals(PlayerGestureCommand.OpenLyrics, playerLongPressCommand(PlayerLongPressAction.Lyrics))
        assertEquals(PlayerGestureCommand.None, playerLongPressCommand(PlayerLongPressAction.Disabled))
    }

    @Test
    fun `mini player track swipes require both gesture switches and a non live track`() {
        assertTrue(miniPlayerHorizontalGesturesEnabled(true, true, liveRadio = false))
        assertFalse(miniPlayerHorizontalGesturesEnabled(true, false, liveRadio = false))
        assertFalse(miniPlayerHorizontalGesturesEnabled(false, true, liveRadio = false))
        assertFalse(miniPlayerHorizontalGesturesEnabled(true, true, liveRadio = true))
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
