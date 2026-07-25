package com.luc4n3x.levyra.desktop.player

import com.luc4n3x.levyra.desktop.core.model.Track
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerQueueTest {

    private fun track(id: String) = Track(
        id = id,
        title = "Track $id",
        videoUrl = "https://www.youtube.com/watch?v=$id"
    )

    private val tracks = listOf(track("a"), track("b"), track("c"))

    @Test
    fun `replace keeps insertion order and honours the start index`() {
        val queue = PlayerQueue().replace(tracks, startIndex = 1)
        assertEquals(tracks, queue.items)
        assertEquals("b", queue.current?.id)
        assertEquals(listOf("c"), queue.upcoming.map { it.id })
    }

    @Test
    fun `replace drops duplicated tracks`() {
        val queue = PlayerQueue().replace(tracks + track("a"))
        assertEquals(3, queue.items.size)
    }

    @Test
    fun `advance stops at the end when repeat is off`() {
        val queue = PlayerQueue().replace(tracks, startIndex = 2)
        assertNull(queue.advance(automatic = true))
        assertFalse(queue.hasNext)
    }

    @Test
    fun `advance wraps when repeat all is active`() {
        val queue = PlayerQueue().replace(tracks, startIndex = 2).withRepeat(RepeatMode.ALL)
        assertEquals("a", queue.advance(automatic = true)?.current?.id)
        assertTrue(queue.hasNext)
    }

    @Test
    fun `automatic advance repeats the same track when repeat one is active`() {
        val queue = PlayerQueue().replace(tracks, startIndex = 1).withRepeat(RepeatMode.ONE)
        assertEquals("b", queue.advance(automatic = true)?.current?.id)
        assertEquals("c", queue.advance(automatic = false)?.current?.id)
    }

    @Test
    fun `rewind honours repeat all`() {
        val queue = PlayerQueue().replace(tracks, startIndex = 0)
        assertNull(queue.rewind())
        assertEquals("c", queue.withRepeat(RepeatMode.ALL).rewind()?.current?.id)
    }

    @Test
    fun `shuffle keeps the current track first and can be restored`() {
        val queue = PlayerQueue().replace(tracks, startIndex = 1)
        val shuffled = queue.withShuffle(enabled = true, random = Random(7))

        assertEquals("b", shuffled.current?.id)
        assertEquals(3, shuffled.items.size)
        assertEquals(setOf("a", "b", "c"), shuffled.items.map { it.id }.toSet())

        val restored = shuffled.withShuffle(enabled = false)
        assertEquals(tracks.map { it.id }, restored.items.map { it.id })
        assertEquals("b", restored.current?.id)
    }

    @Test
    fun `enqueue next inserts after the current track`() {
        val queue = PlayerQueue().replace(tracks, startIndex = 0).enqueueNext(listOf(track("z")))
        assertEquals(listOf("a", "z", "b", "c"), queue.items.map { it.id })
        assertEquals("a", queue.current?.id)
    }

    @Test
    fun `enqueue ignores tracks already queued`() {
        val queue = PlayerQueue().replace(tracks).enqueueLast(listOf(track("a"), track("z")))
        assertEquals(listOf("a", "b", "c", "z"), queue.items.map { it.id })
    }

    @Test
    fun `removing a track before the cursor keeps the current track`() {
        val queue = PlayerQueue().replace(tracks, startIndex = 2).removeAt(0)
        assertEquals("c", queue.current?.id)
        assertEquals(1, queue.index)
    }

    @Test
    fun `removing the current track keeps the cursor in range`() {
        val queue = PlayerQueue().replace(tracks, startIndex = 2).removeAt(2)
        assertEquals("b", queue.current?.id)
        assertEquals(1, queue.index)
    }

    @Test
    fun `removing the last remaining track empties the queue`() {
        val queue = PlayerQueue().replace(listOf(track("a"))).removeAt(0)
        assertTrue(queue.isEmpty)
        assertEquals(-1, queue.index)
        assertNull(queue.current)
    }

    @Test
    fun `jump to track selects the matching entry`() {
        val queue = PlayerQueue().replace(tracks).jumpToTrack("c")
        assertEquals(2, queue.index)
        assertEquals(queue, queue.jumpToTrack("missing"))
    }
}
