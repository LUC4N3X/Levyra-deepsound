package com.luc4n3x.levyra.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ListenEvent(
    val trackId: String,
    val title: String,
    val artist: String,
    val listenedMs: Long,
    val trackDurationMs: Long,
    val completed: Boolean,
    val startedAt: Long,
    val artistBrowseIds: List<String> = emptyList()
)

data class PulseTrack(
    val trackId: String,
    val title: String,
    val artist: String,
    val plays: Int,
    val listenedMs: Long
)

data class PulseArtist(
    val name: String,
    val plays: Int,
    val listenedMs: Long
)

data class PulseDay(
    val date: LocalDate,
    val listenedMs: Long
)

data class PulsePeriod(
    val totalListenMs: Long = 0L,
    val plays: Int = 0,
    val distinctTracks: Int = 0,
    val distinctArtists: Int = 0,
    val completionRate: Int = 0,
    val topTracks: List<PulseTrack> = emptyList(),
    val topArtists: List<PulseArtist> = emptyList()
) {
    val totalMinutes: Long
        get() = totalListenMs / 60_000L
}

data class ListeningPulse(
    val totalListenMs: Long = 0L,
    val plays: Int = 0,
    val distinctTracks: Int = 0,
    val distinctArtists: Int = 0,
    val completionRate: Int = 0,
    val streakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val activeDays: Int = 0,
    val peakHour: Int = -1,
    val topDay: PulseDay? = null,
    val topTracks: List<PulseTrack> = emptyList(),
    val topArtists: List<PulseArtist> = emptyList(),
    val last30Days: PulsePeriod = PulsePeriod(),
    val last365Days: PulsePeriod = PulsePeriod(),
    val week: List<PulseDay> = emptyList()
) {
    val hasSignal: Boolean
        get() = plays > 0

    val totalMinutes: Long
        get() = totalListenMs / 60_000L

    val weekPeakMs: Long
        get() = week.maxOfOrNull { it.listenedMs } ?: 0L
}

class ListeningPulseEngine(private val zone: ZoneId = ZoneId.systemDefault()) {

    fun build(events: List<ListenEvent>, nowMs: Long = System.currentTimeMillis()): ListeningPulse {
        val valid = events.filter { it.listenedMs >= MIN_LISTEN_MS && it.startedAt in 1..nowMs }
        if (valid.isEmpty()) return ListeningPulse(week = emptyWeek(nowMs))

        val today = dayOf(nowMs)
        val totalListenMs = valid.sumOf { it.listenedMs }
        val completedCount = valid.count { it.completed }
        val byDay = valid.groupBy { dayOf(it.startedAt) }
        val activeDates = byDay.keys
        val days30 = valid.filter { dayOf(it.startedAt) >= today.minusDays(LAST_30_DAYS - 1L) }
        val days365 = valid.filter { dayOf(it.startedAt) >= today.minusDays(LAST_365_DAYS - 1L) }

        return ListeningPulse(
            totalListenMs = totalListenMs,
            plays = valid.size,
            distinctTracks = valid.map { trackKey(it) }.toSet().size,
            distinctArtists = valid.map { artistKey(it.artist) }.filter { it.isNotBlank() }.toSet().size,
            completionRate = (completedCount * 100) / valid.size,
            streakDays = streak(activeDates, today),
            longestStreakDays = longestStreak(activeDates),
            activeDays = activeDates.size,
            peakHour = peakHour(valid),
            topDay = byDay.entries
                .map { (date, dayEvents) -> PulseDay(date, dayEvents.sumOf { it.listenedMs }) }
                .maxByOrNull { it.listenedMs },
            topTracks = topTracks(valid),
            topArtists = topArtists(valid),
            last30Days = period(days30),
            last365Days = period(days365),
            week = week(byDay, nowMs)
        )
    }

    private fun period(events: List<ListenEvent>): PulsePeriod {
        if (events.isEmpty()) return PulsePeriod()
        val artists = events.map { artistKey(it.artist) }.filter { it.isNotBlank() }
        return PulsePeriod(
            totalListenMs = events.sumOf { it.listenedMs },
            plays = events.size,
            distinctTracks = events.map { trackKey(it) }.toSet().size,
            distinctArtists = artists.toSet().size,
            completionRate = (events.count { it.completed } * 100) / events.size,
            topTracks = topTracks(events),
            topArtists = topArtists(events)
        )
    }

    private fun topTracks(events: List<ListenEvent>): List<PulseTrack> =
        events.groupBy { trackKey(it) }
            .map { (_, group) ->
                val newest = group.maxBy { it.startedAt }
                PulseTrack(
                    trackId = newest.trackId,
                    title = newest.title,
                    artist = newest.artist,
                    plays = group.size,
                    listenedMs = group.sumOf { it.listenedMs }
                )
            }
            .sortedWith(compareByDescending<PulseTrack> { it.listenedMs }.thenByDescending { it.plays })
            .take(TOP_LIMIT)

    private fun topArtists(events: List<ListenEvent>): List<PulseArtist> =
        events.filter { artistKey(it.artist).isNotBlank() }
            .groupBy { artistKey(it.artist) }
            .map { (_, group) ->
                PulseArtist(
                    name = group.maxBy { it.startedAt }.artist.trim(),
                    plays = group.size,
                    listenedMs = group.sumOf { it.listenedMs }
                )
            }
            .sortedWith(compareByDescending<PulseArtist> { it.listenedMs }.thenByDescending { it.plays })
            .take(TOP_LIMIT)

    private fun week(byDay: Map<LocalDate, List<ListenEvent>>, nowMs: Long): List<PulseDay> {
        val today = dayOf(nowMs)
        return (WEEK_DAYS - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            PulseDay(date, byDay[date]?.sumOf { it.listenedMs } ?: 0L)
        }
    }

    private fun streak(activeDays: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in activeDays) today else today.minusDays(1)
        var count = 0
        while (cursor in activeDays) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    private fun longestStreak(activeDays: Set<LocalDate>): Int {
        if (activeDays.isEmpty()) return 0
        val ordered = activeDays.sorted()
        var longest = 1
        var current = 1
        for (index in 1 until ordered.size) {
            current = if (ordered[index - 1].plusDays(1) == ordered[index]) current + 1 else 1
            if (current > longest) longest = current
        }
        return longest
    }

    private fun peakHour(events: List<ListenEvent>): Int =
        events.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).hour }
            .maxByOrNull { (_, group) -> group.sumOf { it.listenedMs } }
            ?.key ?: -1

    private fun dayOf(epochMs: Long): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    private fun trackKey(event: ListenEvent): String =
        event.trackId.trim().ifBlank { "${event.title.trim().lowercase()}|${artistKey(event.artist)}" }

    private fun artistKey(artist: String): String = artist.trim().lowercase()

    private fun emptyWeek(nowMs: Long): List<PulseDay> {
        val today = dayOf(nowMs)
        return (WEEK_DAYS - 1 downTo 0).map { PulseDay(today.minusDays(it.toLong()), 0L) }
    }

    companion object {
        const val MIN_LISTEN_MS = 5_000L
        private const val TOP_LIMIT = 5
        private const val WEEK_DAYS = 7
        private const val LAST_30_DAYS = 30L
        private const val LAST_365_DAYS = 365L
    }
}
