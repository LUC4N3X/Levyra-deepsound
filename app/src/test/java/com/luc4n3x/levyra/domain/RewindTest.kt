package com.luc4n3x.levyra.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RewindTest {
    @Test fun monthlyTotalsKeepCalendarBoundariesAndZeroMonths() {
        val months = rewindMonths(listOf(
            LocalDate.of(2025, 12, 31) to 99L,
            LocalDate.of(2026, 1, 1) to 30L,
            LocalDate.of(2026, 1, 31) to 70L,
            LocalDate.of(2026, 3, 1) to -20L,
            LocalDate.of(2026, 12, 31) to 50L
        ), 2026)
        assertEquals(12, months.size)
        assertEquals(100L, months[0])
        assertEquals(0L, months[1])
        assertEquals(0L, months[2])
        assertEquals(50L, months[11])
        assertEquals(150L, months.sum())
    }

    @Test fun limitedHistoryDoesNotCreateArtistAlbumOrRhythmChapters() {
        val summary = RewindSummary(2026, ListeningInsightsSnapshot(), emptyList(), List(12) { 0L }, 0, null, null)
        assertEquals(listOf(RewindChapter.Overture, RewindChapter.Time, RewindChapter.Poster), summary.chapters())
    }

    @Test fun realSignalsEnableOnlyRelevantChapters() {
        val summary = RewindSummary(2026, ListeningInsightsSnapshot(
            topTracks = listOf(ListeningInsightsTrack("a", "Song", "Artist")),
            hourBuckets = List(24) { if (it == 22) 100L else 0L }
        ), emptyList(), List(12) { if (it == 9) 100L else 0L }, 1, null, null)
        assertTrue(RewindChapter.Tracks in summary.chapters())
        assertTrue(RewindChapter.Rhythm in summary.chapters())
        assertTrue(RewindChapter.Timeline in summary.chapters())
        assertFalse(RewindChapter.Albums in summary.chapters())
        assertFalse(RewindChapter.Artists in summary.chapters())
    }
}
