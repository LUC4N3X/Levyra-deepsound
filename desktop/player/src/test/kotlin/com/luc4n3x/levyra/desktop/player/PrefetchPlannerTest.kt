package com.luc4n3x.levyra.desktop.player

import com.luc4n3x.levyra.desktop.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefetchPlannerTest {

    private fun track(id: String, offlinePath: String = "") = Track(
        id = id,
        title = "Track $id",
        videoUrl = "https://www.youtube.com/watch?v=$id",
        offlinePath = offlinePath
    )

    private val tracks = listOf(track("a"), track("b"), track("c"))

    @Test
    fun `next track is the following queue entry`() {
        val queue = PlayerQueue().replace(tracks, startIndex = 0)
        assertEquals("b", PrefetchPlanner.nextTrack(queue)?.id)
    }

    @Test
    fun `repeat one never prewarms because the same track plays again`() {
        val queue = PlayerQueue(repeat = RepeatMode.ONE).replace(tracks, startIndex = 0)
        assertNull(PrefetchPlanner.nextTrack(queue))
    }

    @Test
    fun `last track of a non repeating queue has nothing to prewarm`() {
        val queue = PlayerQueue().replace(tracks, startIndex = 2)
        assertNull(PrefetchPlanner.nextTrack(queue))
    }

    @Test
    fun `repeat all wraps back to the first track`() {
        val queue = PlayerQueue(repeat = RepeatMode.ALL).replace(tracks, startIndex = 2)
        assertEquals("a", PrefetchPlanner.nextTrack(queue)?.id)
    }

    @Test
    fun `single track queue on repeat all is not prewarmed`() {
        val queue = PlayerQueue(repeat = RepeatMode.ALL).replace(listOf(track("a")), startIndex = 0)
        assertNull(PrefetchPlanner.nextTrack(queue))
    }

    @Test
    fun `offline files need no speculative resolution`() {
        val queue = PlayerQueue().replace(
            listOf(track("a"), track("b", offlinePath = "/music/b.m4a")),
            startIndex = 0
        )
        assertNull(PrefetchPlanner.nextTrack(queue))
    }

    @Test
    fun `empty and unstarted queues are ignored`() {
        assertNull(PrefetchPlanner.nextTrack(PlayerQueue()))
        assertNull(PrefetchPlanner.nextTrack(PlayerQueue(items = tracks, original = tracks, index = -1)))
    }

    @Test
    fun `lead window opens only near the end of the track`() {
        val duration = 180_000L
        assertFalse(PrefetchPlanner.withinLeadWindow(0L, duration))
        assertFalse(PrefetchPlanner.withinLeadWindow(duration - PrefetchPlanner.LEAD_MS - 1L, duration))
        assertTrue(PrefetchPlanner.withinLeadWindow(duration - PrefetchPlanner.LEAD_MS, duration))
        assertTrue(PrefetchPlanner.withinLeadWindow(duration, duration))
    }

    @Test
    fun `unknown duration keeps the window closed`() {
        assertFalse(PrefetchPlanner.withinLeadWindow(10_000L, 0L))
        assertFalse(PrefetchPlanner.withinLeadWindow(10_000L, -1L))
    }

    @Test
    fun `tracks shorter than the lead window prewarm once playback started`() {
        assertFalse(PrefetchPlanner.withinLeadWindow(0L, 20_000L))
        assertTrue(PrefetchPlanner.withinLeadWindow(1L, 20_000L))
    }
}
