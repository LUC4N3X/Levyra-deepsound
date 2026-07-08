package com.luc4n3x.levyra.domain

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningPulseEngineTest {
    private val zone = ZoneOffset.UTC
    private val engine = ListeningPulseEngine(zone)
    private val now = ZonedDateTime.of(2026, 7, 8, 21, 30, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun emptyEventsProduceNoSignalWithFullWeekAxis() {
        val pulse = engine.build(emptyList(), now)

        assertFalse(pulse.hasSignal)
        assertEquals(7, pulse.week.size)
        assertEquals(0L, pulse.totalListenMs)
        assertEquals(-1, pulse.peakHour)
    }

    @Test
    fun shortListensAreIgnored() {
        val pulse = engine.build(listOf(event(listenedMs = 3_000L, startedAt = now - 1_000L)), now)

        assertFalse(pulse.hasSignal)
    }

    @Test
    fun futureEventsAreIgnored() {
        val pulse = engine.build(listOf(event(startedAt = now + 60_000L)), now)

        assertFalse(pulse.hasSignal)
    }

    @Test
    fun totalsAndDistinctCountsAreAggregated() {
        val events = listOf(
            event(trackId = "a", artist = "Artist One", listenedMs = 60_000L, startedAt = hoursAgo(1)),
            event(trackId = "a", artist = "Artist One", listenedMs = 30_000L, startedAt = hoursAgo(2)),
            event(trackId = "b", artist = "Artist Two", listenedMs = 90_000L, startedAt = hoursAgo(3))
        )

        val pulse = engine.build(events, now)

        assertEquals(180_000L, pulse.totalListenMs)
        assertEquals(3, pulse.plays)
        assertEquals(2, pulse.distinctTracks)
        assertEquals(2, pulse.distinctArtists)
        assertEquals(3L, pulse.totalMinutes)
    }

    @Test
    fun completionRateReflectsCompletedShare() {
        val events = listOf(
            event(trackId = "a", completed = true, startedAt = hoursAgo(1)),
            event(trackId = "b", completed = true, startedAt = hoursAgo(2)),
            event(trackId = "c", completed = false, startedAt = hoursAgo(3)),
            event(trackId = "d", completed = false, startedAt = hoursAgo(4))
        )

        val pulse = engine.build(events, now)

        assertEquals(50, pulse.completionRate)
    }

    @Test
    fun topTracksAreOrderedByListenedTime() {
        val events = listOf(
            event(trackId = "a", title = "Alpha", listenedMs = 30_000L, startedAt = hoursAgo(1)),
            event(trackId = "b", title = "Beta", listenedMs = 120_000L, startedAt = hoursAgo(2)),
            event(trackId = "a", title = "Alpha", listenedMs = 30_000L, startedAt = hoursAgo(3))
        )

        val pulse = engine.build(events, now)

        assertEquals("Beta", pulse.topTracks.first().title)
        assertEquals(2, pulse.topTracks.last().plays)
        assertEquals(60_000L, pulse.topTracks.last().listenedMs)
    }

    @Test
    fun topArtistsMergeCaseInsensitively() {
        val events = listOf(
            event(trackId = "a", artist = "Nova", listenedMs = 40_000L, startedAt = hoursAgo(1)),
            event(trackId = "b", artist = "nova", listenedMs = 40_000L, startedAt = hoursAgo(2)),
            event(trackId = "c", artist = "Other", listenedMs = 50_000L, startedAt = hoursAgo(3))
        )

        val pulse = engine.build(events, now)

        assertEquals(2, pulse.topArtists.size)
        assertEquals(80_000L, pulse.topArtists.first().listenedMs)
        assertEquals(2, pulse.topArtists.first().plays)
    }

    @Test
    fun streakCountsConsecutiveDaysEndingToday() {
        val events = listOf(
            event(trackId = "a", startedAt = daysAgo(0)),
            event(trackId = "b", startedAt = daysAgo(1)),
            event(trackId = "c", startedAt = daysAgo(2)),
            event(trackId = "d", startedAt = daysAgo(4))
        )

        val pulse = engine.build(events, now)

        assertEquals(3, pulse.streakDays)
    }

    @Test
    fun streakSurvivesWhenTodayHasNoListensYet() {
        val events = listOf(
            event(trackId = "a", startedAt = daysAgo(1)),
            event(trackId = "b", startedAt = daysAgo(2))
        )

        val pulse = engine.build(events, now)

        assertEquals(2, pulse.streakDays)
    }

    @Test
    fun weekBucketsCoverSevenDaysOldestFirst() {
        val events = listOf(
            event(trackId = "a", listenedMs = 60_000L, startedAt = daysAgo(0)),
            event(trackId = "b", listenedMs = 30_000L, startedAt = daysAgo(6)),
            event(trackId = "c", listenedMs = 10_000L, startedAt = daysAgo(9))
        )

        val pulse = engine.build(events, now)

        assertEquals(7, pulse.week.size)
        assertEquals(30_000L, pulse.week.first().listenedMs)
        assertEquals(60_000L, pulse.week.last().listenedMs)
        assertTrue(pulse.week.first().date.isBefore(pulse.week.last().date))
        assertEquals(60_000L, pulse.weekPeakMs)
    }

    @Test
    fun peakHourPicksTheHeaviestListeningHour() {
        val at22 = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
            .withHour(22).withMinute(0).toInstant().toEpochMilli() - 86_400_000L
        val at9 = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
            .withHour(9).withMinute(0).toInstant().toEpochMilli() - 86_400_000L
        val events = listOf(
            event(trackId = "a", listenedMs = 200_000L, startedAt = at22),
            event(trackId = "b", listenedMs = 50_000L, startedAt = at9)
        )

        val pulse = engine.build(events, now)

        assertEquals(22, pulse.peakHour)
    }

    @Test
    fun blankTrackIdFallsBackToTitleAndArtistKey() {
        val events = listOf(
            event(trackId = "", title = "Same", artist = "One", listenedMs = 20_000L, startedAt = hoursAgo(1)),
            event(trackId = "", title = "Same", artist = "One", listenedMs = 20_000L, startedAt = hoursAgo(2))
        )

        val pulse = engine.build(events, now)

        assertEquals(1, pulse.distinctTracks)
        assertEquals(2, pulse.topTracks.first().plays)
    }

    private fun hoursAgo(hours: Int): Long = now - hours * 3_600_000L

    private fun daysAgo(days: Int): Long = now - days * 86_400_000L

    private fun event(
        trackId: String = "track",
        title: String = "Title",
        artist: String = "Artist",
        listenedMs: Long = 30_000L,
        durationMs: Long = 180_000L,
        completed: Boolean = false,
        startedAt: Long
    ): ListenEvent = ListenEvent(
        trackId = trackId,
        title = title,
        artist = artist,
        listenedMs = listenedMs,
        trackDurationMs = durationMs,
        completed = completed,
        startedAt = startedAt
    )
}
