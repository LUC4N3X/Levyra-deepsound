package com.luc4n3x.levyra.domain

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListeningInsightsRangesTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val now = LocalDateTime.of(2026, 9, 14, 18, 30).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun weekStartsAtLocalMidnightSixDaysBeforeToday() {
        val range = ListeningInsightsRanges.current(ListeningInsightsPeriod.Week, now, zone)

        assertEquals(
            LocalDateTime.of(2026, 9, 8, 0, 0).atZone(zone).toInstant().toEpochMilli(),
            range.fromMs
        )
        assertEquals(range.fromMs, range.previousToMs)
    }

    @Test
    fun allTimeHasNoPreviousWindow() {
        val range = ListeningInsightsRanges.current(ListeningInsightsPeriod.AllTime, now, zone)

        assertEquals(0L, range.fromMs)
        assertNull(range.previousFromMs)
        assertNull(range.previousToMs)
    }

    @Test
    fun trendIsBoundedAndUnavailableWithoutBaseline() {
        assertEquals(25, ListeningInsightsRanges.trendPercent(125L, 100L))
        assertEquals(-50, ListeningInsightsRanges.trendPercent(50L, 100L))
        assertEquals(999, ListeningInsightsRanges.trendPercent(10_000L, 1L))
        assertNull(ListeningInsightsRanges.trendPercent(100L, 0L))
    }

    @Test
    fun halfYearActivityCollapsesIntoWeeks() {
        val monday = LocalDateTime.of(2026, 9, 7, 0, 0).atZone(zone).toInstant().toEpochMilli()
        val tuesday = LocalDateTime.of(2026, 9, 8, 0, 0).atZone(zone).toInstant().toEpochMilli()
        val nextMonday = LocalDateTime.of(2026, 9, 14, 0, 0).atZone(zone).toInstant().toEpochMilli()

        val result = ListeningInsightsRanges.aggregateActivity(
            ListeningInsightsPeriod.HalfYear,
            listOf(
                ListeningInsightsActivityPoint(monday, 10L, 1),
                ListeningInsightsActivityPoint(tuesday, 20L, 2),
                ListeningInsightsActivityPoint(nextMonday, 30L, 3)
            ),
            zone
        )

        assertEquals(2, result.size)
        assertEquals(30L, result.first().listenedMs)
        assertEquals(3, result.first().plays)
    }

    @Test
    fun dailyActivityKeepsSilentDaysInTheTimeline() {
        val monday = LocalDateTime.of(2026, 9, 7, 0, 0).atZone(zone).toInstant().toEpochMilli()
        val wednesday = LocalDateTime.of(2026, 9, 9, 0, 0).atZone(zone).toInstant().toEpochMilli()

        val result = ListeningInsightsRanges.fillDailyActivity(
            fromMs = monday,
            toMs = LocalDateTime.of(2026, 9, 9, 23, 59).atZone(zone).toInstant().toEpochMilli(),
            daily = listOf(
                ListeningInsightsActivityPoint(monday, 10L, 1),
                ListeningInsightsActivityPoint(wednesday, 30L, 3)
            ),
            zone = zone
        )

        assertEquals(3, result.size)
        assertEquals(0L, result[1].listenedMs)
        assertEquals(0, result[1].plays)
    }

    @Test
    fun dailyActivityKeepsLeadingSilentDaysForFixedPeriods() {
        val monday = LocalDateTime.of(2026, 9, 7, 0, 0).atZone(zone).toInstant().toEpochMilli()
        val wednesday = LocalDateTime.of(2026, 9, 9, 0, 0).atZone(zone).toInstant().toEpochMilli()

        val result = ListeningInsightsRanges.fillDailyActivity(
            fromMs = monday,
            toMs = LocalDateTime.of(2026, 9, 9, 23, 59).atZone(zone).toInstant().toEpochMilli(),
            daily = listOf(ListeningInsightsActivityPoint(wednesday, 30L, 3)),
            zone = zone
        )

        assertEquals(3, result.size)
        assertEquals(listOf(0L, 0L, 30L), result.map { it.listenedMs })
    }
}
