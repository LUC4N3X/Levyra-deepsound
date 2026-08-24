package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningDnaEngineTest {

    private val dayMs = 24L * 60L * 60L * 1000L

    private fun listen(
        id: String,
        artist: String,
        startedAt: Long,
        listenedMs: Long = 120_000L,
        completed: Boolean = true
    ) = ListenEvent(
        trackId = id,
        title = "Title " + id,
        artist = artist,
        listenedMs = listenedMs,
        trackDurationMs = 180_000L,
        completed = completed,
        startedAt = startedAt
    )

    @Test
    fun emptyHistoryHasNoSignalAndFullHourGrid() {
        val dna = ListeningDnaEngine.build(emptyList(), ListeningDnaPeriod.Month, nowMs = 1_000_000_000L)
        assertFalse(dna.hasSignal)
        assertEquals(24, dna.hourBuckets.size)
    }

    @Test
    fun periodWindowExcludesOlderEvents() {
        val now = 100L * dayMs
        val events = listOf(
            listen("recent", "A", now - 2L * dayMs),
            listen("old", "B", now - 60L * dayMs)
        )
        val week = ListeningDnaEngine.build(events, ListeningDnaPeriod.Week, nowMs = now)
        val all = ListeningDnaEngine.build(events, ListeningDnaPeriod.AllTime, nowMs = now)
        assertEquals(1, week.plays)
        assertEquals(2, all.plays)
    }

    @Test
    fun artistWeightsSumToOneAcrossTopArtists() {
        val now = 100L * dayMs
        val events = listOf(
            listen("a1", "Alpha", now - dayMs, listenedMs = 300_000L),
            listen("b1", "Beta", now - dayMs, listenedMs = 100_000L)
        )
        val dna = ListeningDnaEngine.build(events, ListeningDnaPeriod.Month, nowMs = now)
        assertEquals(2, dna.artists.size)
        val total = dna.artists.sumOf { it.weight.toDouble() }
        assertEquals(1.0, total, 0.001)
        assertEquals("Alpha", dna.artists.first().name)
    }

    @Test
    fun discoveryRateCountsFirstListensInsideWindow() {
        val now = 100L * dayMs
        val events = listOf(
            listen("known", "Alpha", now - 60L * dayMs),
            listen("known", "Alpha", now - dayMs),
            listen("new", "Beta", now - dayMs)
        )
        val dna = ListeningDnaEngine.build(events, ListeningDnaPeriod.Week, nowMs = now)
        assertEquals(2, dna.plays)
        assertEquals(50, dna.discoveryRate)
    }

    @Test
    fun shortListensBelowThresholdAreIgnored() {
        val now = 100L * dayMs
        val events = listOf(listen("tiny", "Alpha", now - dayMs, listenedMs = 1_000L))
        val dna = ListeningDnaEngine.build(events, ListeningDnaPeriod.Month, nowMs = now)
        assertFalse(dna.hasSignal)
    }

    @Test
    fun peakHourTracksTheHeaviestBucket() {
        val now = 100L * dayMs
        val base = now - dayMs
        val events = listOf(
            listen("a", "Alpha", base, listenedMs = 60_000L),
            listen("b", "Alpha", base + 3_600_000L, listenedMs = 600_000L)
        )
        val dna = ListeningDnaEngine.build(events, ListeningDnaPeriod.Month, nowMs = now)
        assertTrue(dna.peakHour in 0..23)
        assertEquals(dna.hourBuckets.max(), dna.hourBuckets[dna.peakHour])
    }
}
