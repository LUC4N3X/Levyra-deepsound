package com.luc4n3x.levyra.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFrameDropMonitorTest {

    @Test
    fun `a burst of dropped frames in a short window is sustained stutter`() {
        assertTrue(isSustainedVideoFrameDrop(droppedFrames = 50, elapsedMs = 4_000L))
        assertTrue(isSustainedVideoFrameDrop(droppedFrames = 50, elapsedMs = 12_000L))
    }

    @Test
    fun `occasional drops spread over a long time are ignored`() {
        assertFalse(isSustainedVideoFrameDrop(droppedFrames = 50, elapsedMs = 60_000L))
        assertFalse(isSustainedVideoFrameDrop(droppedFrames = 40, elapsedMs = 11_000L))
    }

    @Test
    fun `small drop counts never trigger a downgrade`() {
        assertFalse(isSustainedVideoFrameDrop(droppedFrames = 12, elapsedMs = 500L))
    }

    @Test
    fun `drops right after a seek or quality switch are not blamed on the decoder`() {
        assertTrue(frameDropWindowOverlapsTransition(nowMs = 20_000L, elapsedMs = 4_000L, lastTransitionMs = 17_000L))
        assertTrue(frameDropWindowOverlapsTransition(nowMs = 20_000L, elapsedMs = 4_000L, lastTransitionMs = 14_000L))
    }

    @Test
    fun `drops long after the last transition still count`() {
        assertFalse(frameDropWindowOverlapsTransition(nowMs = 40_000L, elapsedMs = 4_000L, lastTransitionMs = 20_000L))
        assertFalse(frameDropWindowOverlapsTransition(nowMs = 40_000L, elapsedMs = 4_000L, lastTransitionMs = Long.MIN_VALUE))
    }

    @Test
    fun `invalid elapsed time is ignored`() {
        assertFalse(isSustainedVideoFrameDrop(droppedFrames = 60, elapsedMs = 0L))
        assertFalse(isSustainedVideoFrameDrop(droppedFrames = 60, elapsedMs = -5L))
    }
}
