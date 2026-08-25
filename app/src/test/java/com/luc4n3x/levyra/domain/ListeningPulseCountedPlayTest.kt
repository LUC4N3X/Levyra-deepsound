package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningPulseCountedPlayTest {

    private val nowMs = 1_700_000_000_000L
    private val engine = ListeningPulseEngine()

    private fun event(
        listenedMs: Long,
        durationMs: Long = 0L,
        completed: Boolean = false,
        startedAt: Long = nowMs - 1_000L,
        trackId: String = "t1",
        title: String = "Title",
        artist: String = "Artist"
    ) = ListenEvent(
        trackId = trackId,
        title = title,
        artist = artist,
        listenedMs = listenedMs,
        trackDurationMs = durationMs,
        completed = completed,
        startedAt = startedAt
    )

    @Test
    fun `15s event contributes listened time but is not a counted play`() {
        val pulse = engine.build(listOf(event(listenedMs = 15_000L)), nowMs)
        assertEquals(15_000L, pulse.totalListenMs)
        assertEquals(0, pulse.plays)
    }

    @Test
    fun `45s event is a counted play`() {
        val pulse = engine.build(listOf(event(listenedMs = 45_000L)), nowMs)
        assertEquals(1, pulse.plays)
    }

    @Test
    fun `hourBuckets has 24 entries and sums to totalListenMs`() {
        val events = listOf(
            event(listenedMs = 45_000L, startedAt = nowMs - 3_600_000L, trackId = "a"),
            event(listenedMs = 20_000L, startedAt = nowMs - 7_200_000L, trackId = "b"),
            event(listenedMs = 6_000L, startedAt = nowMs, trackId = "c")
        )
        val pulse = engine.build(events, nowMs)
        assertEquals(24, pulse.hourBuckets.size)
        assertEquals(pulse.totalListenMs, pulse.hourBuckets.sum())
    }

    @Test
    fun `empty input yields week size 7 and 24 zeroed hour buckets without exception`() {
        val pulse = engine.build(emptyList(), nowMs)
        assertEquals(7, pulse.week.size)
        assertEquals(24, pulse.hourBuckets.size)
        assertTrue(pulse.hourBuckets.all { it == 0L })
    }
}
