package com.luc4n3x.levyra.feature.cast

import com.luc4n3x.levyra.domain.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CastHandoffTest {

    private fun snapshot(
        size: Int,
        currentIndex: Int,
        positionMs: Long = 42_000L,
        playing: Boolean = true,
        shuffle: Boolean = true,
        repeatMode: RepeatMode = RepeatMode.All
    ): LocalPlaybackSnapshot = LocalPlaybackSnapshot(
        queueIds = (0 until size).map { "track-$it" },
        currentIndex = currentIndex,
        positionMs = positionMs,
        playing = playing,
        shuffle = shuffle,
        repeatMode = repeatMode
    )

    @Test
    fun roundTripPreservesCurrentTrackAndTransportStateWhenQueueFitsInWindow() {
        val local = snapshot(size = 5, currentIndex = 2)

        val handoff = CastHandoffConverter.toHandoff(local, windowRadius = 10)
        val resumed = CastHandoffConverter.toLocalResumeState(handoff, local.queueIds)

        assertEquals(local.queueIds, resumed.queueIds)
        assertEquals(local.currentIndex, resumed.currentIndex)
        assertEquals(local.positionMs, resumed.positionMs)
        assertEquals(local.playing, resumed.playing)
        assertEquals(local.shuffle, resumed.shuffle)
        assertEquals(local.repeatMode, resumed.repeatMode)
        assertEquals(local.queueIds[local.currentIndex], resumed.queueIds[resumed.currentIndex])
    }

    @Test
    fun roundTripPreservesCurrentTrackIdentityWhenWindowIsTrimmedAtListStart() {
        val local = snapshot(size = 30, currentIndex = 0)

        val handoff = CastHandoffConverter.toHandoff(local, windowRadius = 10)
        val resumed = CastHandoffConverter.toLocalResumeState(handoff, local.queueIds)

        assertEquals(0, handoff.windowStartIndex)
        assertEquals(11, handoff.queueWindowIds.size)
        assertEquals("track-0", resumed.queueIds[resumed.currentIndex])
        assertEquals(local.positionMs, resumed.positionMs)
        assertEquals(local.playing, resumed.playing)
        assertEquals(local.shuffle, resumed.shuffle)
        assertEquals(local.repeatMode, resumed.repeatMode)
    }

    @Test
    fun roundTripPreservesCurrentTrackIdentityWhenWindowIsCenteredInListMiddle() {
        val local = snapshot(size = 30, currentIndex = 15)

        val handoff = CastHandoffConverter.toHandoff(local, windowRadius = 10)
        val resumed = CastHandoffConverter.toLocalResumeState(handoff, local.queueIds)

        assertEquals(5, handoff.windowStartIndex)
        assertEquals(21, handoff.queueWindowIds.size)
        assertEquals("track-15", resumed.queueIds[resumed.currentIndex])
    }

    @Test
    fun roundTripPreservesCurrentTrackIdentityWhenWindowIsTrimmedAtListEnd() {
        val local = snapshot(size = 30, currentIndex = 29)

        val handoff = CastHandoffConverter.toHandoff(local, windowRadius = 10)
        val resumed = CastHandoffConverter.toLocalResumeState(handoff, local.queueIds)

        assertEquals(19, handoff.windowStartIndex)
        assertEquals(11, handoff.queueWindowIds.size)
        assertEquals("track-29", resumed.queueIds[resumed.currentIndex])
    }

    @Test
    fun handoffWindowNeverExceedsConfiguredRadiusOnEitherSide() {
        val local = snapshot(size = 100, currentIndex = 50)

        val handoff = CastHandoffConverter.toHandoff(local, windowRadius = 3)

        assertEquals(listOf("track-47", "track-48", "track-49", "track-50", "track-51", "track-52", "track-53"), handoff.queueWindowIds)
        assertEquals(3, handoff.currentIndex)
    }

    @Test
    fun handoffDefaultWindowRadiusIsTen() {
        assertEquals(10, CastHandoffConverter.DEFAULT_QUEUE_WINDOW_RADIUS)
    }

    @Test
    fun emptyQueueProducesEmptyHandoffAndSafeResumeState() {
        val local = snapshot(size = 0, currentIndex = 0)

        val handoff = CastHandoffConverter.toHandoff(local)
        val resumed = CastHandoffConverter.toLocalResumeState(handoff, local.queueIds)

        assertTrue(handoff.queueWindowIds.isEmpty())
        assertEquals(-1, handoff.currentIndex)
        assertTrue(resumed.queueIds.isEmpty())
        assertEquals(-1, resumed.currentIndex)
    }
}
