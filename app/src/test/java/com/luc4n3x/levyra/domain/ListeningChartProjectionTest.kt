package com.luc4n3x.levyra.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningChartProjectionTest {

    private fun day(ms: Long, date: LocalDate = LocalDate.of(2024, 1, 1)) = PulseDay(date, ms)

    @Test
    fun `weekFractions empty week returns 7 zeros`() {
        val fractions = ListeningChartProjection.weekFractions(emptyList(), minPeakMs = 0L)
        assertEquals(7, fractions.size)
        assertTrue(fractions.all { it == 0f })
    }

    @Test
    fun `weekFractions all-zero week returns all zero`() {
        val week = List(7) { day(0L) }
        val fractions = ListeningChartProjection.weekFractions(week, minPeakMs = 0L)
        assertEquals(7, fractions.size)
        assertTrue(fractions.all { it == 0f })
    }

    @Test
    fun `weekFractions one active day is 1f and others are 0f`() {
        val week = listOf(day(0L), day(0L), day(60_000L), day(0L), day(0L), day(0L), day(0L))
        val fractions = ListeningChartProjection.weekFractions(week, minPeakMs = 0L)
        assertEquals(1f, fractions[2])
        fractions.forEachIndexed { index, value -> if (index != 2) assertEquals(0f, value) }
    }

    @Test
    fun `peakDayIndex is correct and -1 when all zero`() {
        val allZero = List(7) { day(0L) }
        assertEquals(-1, ListeningChartProjection.peakDayIndex(allZero))

        val withPeak = listOf(day(0L), day(1_000L), day(90_000L), day(0L), day(500L), day(0L), day(0L))
        assertEquals(2, ListeningChartProjection.peakDayIndex(withPeak))
    }

    @Test
    fun `hourFractions with 24 empty hours returns 24 zeros`() {
        val fractions = ListeningChartProjection.hourFractions(emptyList())
        assertEquals(24, fractions.size)
        assertTrue(fractions.all { it == 0f })
    }

    @Test
    fun `hourFractions one peak hour is 1f at that hour`() {
        val buckets = List(24) { hour -> if (hour == 9) 10_000L else 0L }
        val fractions = ListeningChartProjection.hourFractions(buckets)
        assertEquals(1f, fractions[9])
        fractions.forEachIndexed { index, value -> if (index != 9) assertEquals(0f, value) }
    }

    @Test
    fun `hourFractions all-equal hours are all 1f`() {
        val buckets = List(24) { 5_000L }
        val fractions = ListeningChartProjection.hourFractions(buckets)
        assertTrue(fractions.all { it == 1f })
    }

    @Test
    fun `peakHourIndex is -1 when empty`() {
        assertEquals(-1, ListeningChartProjection.peakHourIndex(emptyList()))
    }

    @Test
    fun `artistShares fractions sum to about 1 and are capped and non-negative`() {
        val artists = (1..8).map { PulseArtist(name = "Artist $it", plays = it, listenedMs = it * 10_000L) }
        val shares = ListeningChartProjection.artistShares(artists)

        assertTrue(shares.size <= ListeningChartProjection.MAX_RING_ARTISTS)
        val total = shares.sumOf { it.fraction.toDouble() }
        assertTrue(Math.abs(total - 1.0) < 0.001)
        shares.forEach { share ->
            assertFalse(share.fraction.isNaN())
            assertTrue(share.fraction >= 0f)
        }
    }

    @Test
    fun `artistShares empty or zero input returns empty list`() {
        assertTrue(ListeningChartProjection.artistShares(emptyList()).isEmpty())
        val zeroed = listOf(PulseArtist("Artist", 0, 0L))
        assertTrue(ListeningChartProjection.artistShares(zeroed).isEmpty())
    }
}
