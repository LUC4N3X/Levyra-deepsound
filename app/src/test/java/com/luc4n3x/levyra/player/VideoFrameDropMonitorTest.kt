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
    fun `invalid elapsed time is ignored`() {
        assertFalse(isSustainedVideoFrameDrop(droppedFrames = 60, elapsedMs = 0L))
        assertFalse(isSustainedVideoFrameDrop(droppedFrames = 60, elapsedMs = -5L))
    }
}
