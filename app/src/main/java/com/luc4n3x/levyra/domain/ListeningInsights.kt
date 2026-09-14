package com.luc4n3x.levyra.domain

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class ListeningInsightsPeriod {
    Day,
    Week,
    Month,
    HalfYear,
    AllTime
}

@Immutable
data class ListeningInsightsRange(
    val fromMs: Long,
    val toMs: Long,
    val previousFromMs: Long? = null,
    val previousToMs: Long? = null
)

@Immutable
data class ListeningInsightsMetrics(
    val listenedMs: Long = 0L,
    val countedPlays: Int = 0,
    val distinctTracks: Int = 0,
    val distinctArtists: Int = 0,
    val completionRate: Int = 0,
    val discoveryCount: Int = 0,
    val discoveryRate: Int = 0,
    val peakHour: Int = -1,
    val peakDayEpochMs: Long = 0L,
    val trendPercent: Int? = null
)

@Immutable
data class ListeningInsightsActivityPoint(
    val epochMs: Long,
    val listenedMs: Long,
    val plays: Int
)

@Immutable
data class ListeningInsightsTrack(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String = "",
    val listenedMs: Long = 0L,
    val plays: Int = 0
)

@Immutable
data class ListeningInsightsArtist(
    val name: String,
    val artworkUrl: String = "",
    val listenedMs: Long = 0L,
    val plays: Int = 0,
    val trackCount: Int = 0
)

@Immutable
data class ListeningInsightsHistoryItem(
    val id: Long,
    val track: ListeningInsightsTrack,
    val startedAt: Long,
    val listenedMs: Long,
    val completed: Boolean
)

@Immutable
data class ListeningInsightsSnapshot(
    val period: ListeningInsightsPeriod = ListeningInsightsPeriod.Week,
    val metrics: ListeningInsightsMetrics = ListeningInsightsMetrics(),
    val activity: List<ListeningInsightsActivityPoint> = emptyList(),
    val hourBuckets: List<Long> = List(24) { 0L },
    val topTracks: List<ListeningInsightsTrack> = emptyList(),
    val topArtists: List<ListeningInsightsArtist> = emptyList(),
    val detailedFromMs: Long = 0L
) {
    val hasSignal: Boolean get() = metrics.listenedMs > 0L || metrics.countedPlays > 0
}

object ListeningInsightsRanges {
    fun current(period: ListeningInsightsPeriod, nowMs: Long, zone: ZoneId): ListeningInsightsRange {
        if (period == ListeningInsightsPeriod.AllTime) {
            return ListeningInsightsRange(0L, nowMs + 1L)
        }
        val now = Instant.ofEpochMilli(nowMs).atZone(zone)
        val from = when (period) {
            ListeningInsightsPeriod.Day -> now.minusHours(24)
            ListeningInsightsPeriod.Week -> now.toLocalDate().minusDays(6).atStartOfDay(zone)
            ListeningInsightsPeriod.Month -> now.toLocalDate().minusDays(29).atStartOfDay(zone)
            ListeningInsightsPeriod.HalfYear -> now.toLocalDate().minusMonths(6).atStartOfDay(zone)
            ListeningInsightsPeriod.AllTime -> error("Handled above")
        }
        val fromMs = from.toInstant().toEpochMilli()
        val duration = nowMs + 1L - fromMs
        return ListeningInsightsRange(
            fromMs = fromMs,
            toMs = nowMs + 1L,
            previousFromMs = (fromMs - duration).coerceAtLeast(0L),
            previousToMs = fromMs
        )
    }

    fun trendPercent(currentMs: Long, previousMs: Long): Int? {
        if (previousMs <= 0L) return null
        return ((currentMs - previousMs) * 100.0 / previousMs.toDouble())
            .toInt()
            .coerceIn(-999, 999)
    }

    fun aggregateActivity(
        period: ListeningInsightsPeriod,
        daily: List<ListeningInsightsActivityPoint>,
        zone: ZoneId
    ): List<ListeningInsightsActivityPoint> {
        if (period != ListeningInsightsPeriod.HalfYear && period != ListeningInsightsPeriod.AllTime) return daily
        val grouped = daily.groupBy { point ->
            val date = Instant.ofEpochMilli(point.epochMs).atZone(zone).toLocalDate()
            if (period == ListeningInsightsPeriod.HalfYear) {
                date.minusDays(date.dayOfWeek.value.toLong() - 1L)
            } else {
                date.withDayOfMonth(1)
            }
        }
        return grouped.entries.sortedBy { it.key }.map { (date, points) ->
            ListeningInsightsActivityPoint(
                epochMs = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                listenedMs = points.sumOf { it.listenedMs },
                plays = points.sumOf { it.plays }
            )
        }
    }

    fun fillDailyActivity(
        fromMs: Long,
        toMs: Long,
        daily: List<ListeningInsightsActivityPoint>,
        zone: ZoneId
    ): List<ListeningInsightsActivityPoint> {
        if (daily.isEmpty()) return emptyList()
        val start = Instant.ofEpochMilli(fromMs).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli((toMs - 1L).coerceAtLeast(fromMs)).atZone(zone).toLocalDate()
        val byDate = daily.associateBy { Instant.ofEpochMilli(it.epochMs).atZone(zone).toLocalDate() }
        return generateSequence(start) { date -> date.plusDays(1).takeIf { it <= end } }
            .map { date ->
                byDate[date] ?: ListeningInsightsActivityPoint(
                    epochMs = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                    listenedMs = 0L,
                    plays = 0
                )
            }
            .toList()
    }

    fun fillHourlyActivity(
        fromMs: Long,
        toMs: Long,
        hourly: List<ListeningInsightsActivityPoint>,
        zone: ZoneId
    ): List<ListeningInsightsActivityPoint> {
        val start = Instant.ofEpochMilli(fromMs).atZone(zone).truncatedTo(ChronoUnit.HOURS)
        val end = Instant.ofEpochMilli((toMs - 1L).coerceAtLeast(fromMs)).atZone(zone).truncatedTo(ChronoUnit.HOURS)
        val byHourEpoch = hourly.associateBy { point ->
            Instant.ofEpochMilli(point.epochMs).atZone(zone).truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli()
        }
        return generateSequence(start) { time ->
            time.plusHours(1).takeIf { !it.isAfter(end) }
        }.map { hourTime ->
            val epochMs = hourTime.toInstant().toEpochMilli()
            byHourEpoch[epochMs] ?: ListeningInsightsActivityPoint(
                epochMs = epochMs,
                listenedMs = 0L,
                plays = 0
            )
        }.toList()
    }

    fun dayEpoch(dayKey: String, zone: ZoneId): Long =
        LocalDate.parse(dayKey).atStartOfDay(zone).toInstant().toEpochMilli()
}
