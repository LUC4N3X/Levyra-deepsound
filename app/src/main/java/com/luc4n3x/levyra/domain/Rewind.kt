package com.luc4n3x.levyra.domain

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class RewindAlbum(
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val listenedMs: Long,
    val plays: Int
)

@Immutable
data class RewindSummary(
    val year: Int,
    val snapshot: ListeningInsightsSnapshot,
    val albums: List<RewindAlbum>,
    val months: List<Long>,
    val activeDays: Int,
    val firstDay: LocalDate?,
    val lastDay: LocalDate?
)

enum class RewindChapter { Overture, Time, Artists, Tracks, Albums, Rhythm, Timeline, Poster }

fun RewindSummary.chapters(): List<RewindChapter> = buildList {
    add(RewindChapter.Overture)
    add(RewindChapter.Time)
    if (snapshot.topArtists.isNotEmpty()) add(RewindChapter.Artists)
    if (snapshot.topTracks.isNotEmpty()) add(RewindChapter.Tracks)
    if (albums.isNotEmpty()) add(RewindChapter.Albums)
    if (snapshot.hourBuckets.any { it > 0L }) add(RewindChapter.Rhythm)
    if (months.any { it > 0L }) add(RewindChapter.Timeline)
    add(RewindChapter.Poster)
}

fun rewindMonths(days: List<Pair<LocalDate, Long>>, year: Int): List<Long> {
    val totals = LongArray(12)
    days.forEach { (date, duration) ->
        if (date.year == year) totals[date.monthValue - 1] += duration.coerceAtLeast(0L)
    }
    return totals.toList()
}
