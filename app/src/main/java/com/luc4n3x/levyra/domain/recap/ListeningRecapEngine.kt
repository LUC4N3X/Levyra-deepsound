package com.luc4n3x.levyra.domain.recap

import com.luc4n3x.levyra.domain.LifetimeListening
import com.luc4n3x.levyra.domain.ListenEvent
import com.luc4n3x.levyra.domain.ListenIdentity
import com.luc4n3x.levyra.domain.ListenPlayPolicy
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object ListeningRecapEngine {

    private const val TOP_LIMIT = 5
    private const val DISCOVERY_REFERENCE_DAYS = 30L

    fun build(
        events: List<ListenEvent>,
        period: ListeningRecapPeriod,
        lifetime: LifetimeListening? = null,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): ListeningRecapSummary {
        val valid = events.filter {
            ListenPlayPolicy.isRecordableEvent(it.listenedMs) && it.startedAt in 1..nowMs
        }

        val today = dayOf(nowMs, zone)
        val cutoff = if (period.days <= 0) 0L else nowMs - TimeUnit.DAYS.toMillis(period.days.toLong())
        val scoped = if (cutoff <= 0L) valid else valid.filter { it.startedAt >= cutoff }

        if (scoped.isEmpty()) {
            return if (period == ListeningRecapPeriod.AllTime && lifetime != null && lifetime.hasSignal) {
                buildFromLifetime(lifetime, period, nowMs, zone)
            } else {
                ListeningRecapSummary(
                    period = period,
                    dailyActivity = emptyDailyPoints(period, today)
                )
            }
        }

        val firstSeenMap = HashMap<String, Long>(valid.size)
        for (event in valid) {
            val key = ListenIdentity.trackKey(event)
            val prev = firstSeenMap[key]
            if (prev == null || event.startedAt < prev) {
                firstSeenMap[key] = event.startedAt
            }
        }

        val totalListenMs = scoped.sumOf { it.listenedMs }
        val completedCount = scoped.count { it.completed }
        val completionRate = if (scoped.isNotEmpty()) (completedCount * 100) / scoped.size else 0

        val totalPlays = scoped.count { ListenPlayPolicy.isCountedPlay(it) }
        val uniqueTrackKeys = scoped.map { ListenIdentity.trackKey(it) }.toSet()
        val uniqueArtists = scoped.map { ListenIdentity.artistKey(it.artist) }.filter { it.isNotBlank() }.toSet().size
        val uniqueAlbums = scoped.map { albumKey(it.album, it.artist) }.filter { it.isNotBlank() }.toSet().size

        val byDay = scoped.groupBy { dayOf(it.startedAt, zone) }
        val activeDates = byDay.keys
        val currentStreak = calculateStreak(activeDates, today)
        val bestStreak = calculateLongestStreak(activeDates)

        val mostActiveDayEntry = byDay.entries.maxByOrNull { it.value.sumOf { ev -> ev.listenedMs } }
        val mostActiveDayDate = mostActiveDayEntry?.key
        val mostActiveDayMinutes = (mostActiveDayEntry?.value?.sumOf { it.listenedMs } ?: 0L) / 60_000L

        val averageMinutesPerDay = when {
            period.days > 0 -> (totalListenMs / 60_000L) / period.days.toLong()
            activeDates.isNotEmpty() -> (totalListenMs / 60_000L) / activeDates.size.toLong()
            else -> 0L
        }

        val hourlyBuckets = LongArray(24)
        var morningMs = 0L
        var afternoonMs = 0L
        var eveningMs = 0L
        var nightMs = 0L

        for (event in scoped) {
            val hour = Instant.ofEpochMilli(event.startedAt).atZone(zone).hour
            if (hour in 0..23) {
                hourlyBuckets[hour] += event.listenedMs
                when (hour) {
                    in 5..11 -> morningMs += event.listenedMs
                    in 12..16 -> afternoonMs += event.listenedMs
                    in 17..21 -> eveningMs += event.listenedMs
                    else -> nightMs += event.listenedMs
                }
            }
        }

        var peakHour = -1
        var peakHourMs = 0L
        for (h in 0..23) {
            if (hourlyBuckets[h] > peakHourMs) {
                peakHourMs = hourlyBuckets[h]
                peakHour = h
            }
        }

        val favoriteDaypart = listOf(
            Daypart.Morning to morningMs,
            Daypart.Afternoon to afternoonMs,
            Daypart.Evening to eveningMs,
            Daypart.Night to nightMs
        ).maxByOrNull { it.second }?.first ?: Daypart.Afternoon

        val topTracks = topTracks(scoped)
        val topArtists = topArtists(scoped)
        val topAlbums = topAlbums(scoped)

        val mostReplayedTrack = topTracks.maxByOrNull { it.plays }?.takeIf { it.plays >= 2 }

        val discoveryThreshold = if (cutoff > 0L) cutoff else nowMs - TimeUnit.DAYS.toMillis(DISCOVERY_REFERENCE_DAYS)
        val discoveredCount = uniqueTrackKeys.count { (firstSeenMap[it] ?: 0L) >= discoveryThreshold }
        val discoveryRate = if (uniqueTrackKeys.isNotEmpty()) {
            ((discoveredCount * 100) / uniqueTrackKeys.size).coerceIn(0, 100)
        } else {
            0
        }
        val repeatRate = (100 - discoveryRate).coerceIn(0, 100)

        val dailyActivity = generateDailyActivity(byDay, period, today)

        val highlights = RecapHighlightStat(
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            averageMinutesPerDay = averageMinutesPerDay,
            mostActiveDayDate = mostActiveDayDate,
            mostActiveDayMinutes = mostActiveDayMinutes,
            favoriteHour = peakHour,
            favoriteDaypart = favoriteDaypart,
            mostReplayedTrack = mostReplayedTrack,
            discoveryRate = discoveryRate,
            repeatRate = repeatRate
        )

        var finalListenMs = totalListenMs
        var finalPlays = totalPlays
        var finalTracks = uniqueTrackKeys.size
        var finalArtists = uniqueArtists
        var finalTopTracks = topTracks
        var finalTopArtists = topArtists

        if (period == ListeningRecapPeriod.AllTime && lifetime != null && lifetime.hasSignal) {
            if (lifetime.totalListenMs > finalListenMs) {
                finalListenMs = lifetime.totalListenMs
                finalPlays = lifetime.countedPlays
                finalTracks = maxOf(finalTracks, lifetime.distinctTracks)
                finalArtists = maxOf(finalArtists, lifetime.distinctArtists)
            }
            if ((finalTopTracks.isEmpty() || lifetime.totalListenMs > totalListenMs) && lifetime.tracks.isNotEmpty()) {
                val thumbMap = scoped.associate { it.trackId to it.thumbnailUrl }
                finalTopTracks = lifetime.tracks.take(TOP_LIMIT).mapIndexed { idx, t ->
                    TopTrackStat(
                        rank = idx + 1,
                        trackId = t.trackId,
                        title = t.title,
                        artist = t.artist,
                        thumbnailUrl = thumbMap[t.trackId] ?: "",
                        plays = t.plays,
                        listenedMs = t.listenedMs
                    )
                }
            }
            if ((finalTopArtists.isEmpty() || lifetime.totalListenMs > totalListenMs) && lifetime.artists.isNotEmpty()) {
                val artistThumbMap = scoped.associate { it.artist.trim().lowercase() to it.thumbnailUrl }
                finalTopArtists = lifetime.artists.take(TOP_LIMIT).mapIndexed { idx, a ->
                    TopArtistStat(
                        rank = idx + 1,
                        name = a.name,
                        thumbnailUrl = artistThumbMap[a.name.trim().lowercase()] ?: "",
                        plays = a.countedPlays,
                        listenedMs = a.listenedMs
                    )
                }
            }
        }

        return ListeningRecapSummary(
            period = period,
            totalListenMs = finalListenMs,
            totalPlays = finalPlays,
            uniqueTracks = finalTracks,
            uniqueArtists = finalArtists,
            uniqueAlbums = uniqueAlbums,
            completionRate = completionRate,
            highlights = highlights,
            topTracks = finalTopTracks,
            topArtists = finalTopArtists,
            topAlbums = topAlbums,
            dailyActivity = dailyActivity
        )
    }

    private fun topTracks(events: List<ListenEvent>): List<TopTrackStat> {
        return events.groupBy { ListenIdentity.trackKey(it) }
            .map { (_, group) ->
                val newest = group.maxBy { it.startedAt }
                TopTrackStat(
                    rank = 0,
                    trackId = newest.trackId,
                    title = newest.title,
                    artist = newest.artist,
                    album = newest.album,
                    thumbnailUrl = group.firstOrNull { it.thumbnailUrl.isNotBlank() }?.thumbnailUrl ?: newest.thumbnailUrl,
                    plays = group.count { ListenPlayPolicy.isCountedPlay(it) },
                    listenedMs = group.sumOf { it.listenedMs }
                )
            }
            .sortedWith(compareByDescending<TopTrackStat> { it.listenedMs }.thenByDescending { it.plays })
            .take(TOP_LIMIT)
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    private fun topArtists(events: List<ListenEvent>): List<TopArtistStat> {
        return events.filter { it.artist.isNotBlank() }
            .groupBy { ListenIdentity.artistKey(it.artist) }
            .map { (_, group) ->
                val newest = group.maxBy { it.startedAt }
                TopArtistStat(
                    rank = 0,
                    name = newest.artist.trim(),
                    plays = group.count { ListenPlayPolicy.isCountedPlay(it) },
                    listenedMs = group.sumOf { it.listenedMs },
                    thumbnailUrl = group.firstOrNull { it.thumbnailUrl.isNotBlank() }?.thumbnailUrl ?: "",
                    trackCount = group.map { ListenIdentity.trackKey(it) }.toSet().size
                )
            }
            .sortedWith(compareByDescending<TopArtistStat> { it.listenedMs }.thenByDescending { it.plays })
            .take(TOP_LIMIT)
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    private fun topAlbums(events: List<ListenEvent>): List<TopAlbumStat> {
        return events.filter { it.album.isNotBlank() && !isUnknownAlbum(it.album) }
            .groupBy { albumKey(it.album, it.artist) }
            .map { (_, group) ->
                val newest = group.maxBy { it.startedAt }
                TopAlbumStat(
                    rank = 0,
                    title = newest.album.trim(),
                    artist = newest.artist.trim(),
                    thumbnailUrl = group.firstOrNull { it.thumbnailUrl.isNotBlank() }?.thumbnailUrl ?: "",
                    plays = group.count { ListenPlayPolicy.isCountedPlay(it) },
                    listenedMs = group.sumOf { it.listenedMs },
                    trackCount = group.map { ListenIdentity.trackKey(it) }.toSet().size
                )
            }
            .sortedWith(compareByDescending<TopAlbumStat> { it.listenedMs }.thenByDescending { it.plays })
            .take(TOP_LIMIT)
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    private fun generateDailyActivity(
        byDay: Map<LocalDate, List<ListenEvent>>,
        period: ListeningRecapPeriod,
        today: LocalDate
    ): List<ListeningPulsePoint> {
        val daysCount = when (period) {
            ListeningRecapPeriod.Days7 -> 7
            ListeningRecapPeriod.Days30 -> 30
            ListeningRecapPeriod.Days365 -> 30
            ListeningRecapPeriod.AllTime -> 30
        }

        return (daysCount - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dayEvents = byDay[date] ?: emptyList()
            ListeningPulsePoint(
                date = date,
                listenedMs = dayEvents.sumOf { it.listenedMs },
                plays = dayEvents.count { ListenPlayPolicy.isCountedPlay(it) }
            )
        }
    }

    private fun emptyDailyPoints(period: ListeningRecapPeriod, today: LocalDate): List<ListeningPulsePoint> {
        val daysCount = when (period) {
            ListeningRecapPeriod.Days7 -> 7
            ListeningRecapPeriod.Days30 -> 30
            ListeningRecapPeriod.Days365 -> 30
            ListeningRecapPeriod.AllTime -> 30
        }
        return (daysCount - 1 downTo 0).map { offset ->
            ListeningPulsePoint(date = today.minusDays(offset.toLong()), listenedMs = 0L, plays = 0)
        }
    }

    private fun calculateStreak(activeDays: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in activeDays) today else today.minusDays(1)
        var count = 0
        while (cursor in activeDays) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    private fun calculateLongestStreak(activeDays: Set<LocalDate>): Int {
        if (activeDays.isEmpty()) return 0
        val ordered = activeDays.sorted()
        var longest = 1
        var current = 1
        for (i in 1 until ordered.size) {
            current = if (ordered[i - 1].plusDays(1) == ordered[i]) current + 1 else 1
            if (current > longest) longest = current
        }
        return longest
    }

    private fun buildFromLifetime(
        lifetime: LifetimeListening,
        period: ListeningRecapPeriod,
        nowMs: Long,
        zone: ZoneId
    ): ListeningRecapSummary {
        val today = dayOf(nowMs, zone)
        val topTracks = lifetime.tracks.take(TOP_LIMIT).mapIndexed { idx, t ->
            TopTrackStat(
                rank = idx + 1,
                trackId = t.trackId,
                title = t.title,
                artist = t.artist,
                plays = t.plays,
                listenedMs = t.listenedMs
            )
        }
        val topArtists = lifetime.artists.take(TOP_LIMIT).mapIndexed { idx, a ->
            TopArtistStat(
                rank = idx + 1,
                name = a.name,
                plays = a.countedPlays,
                listenedMs = a.listenedMs
            )
        }
        val completionRate = if (lifetime.eventCount > 0) {
            ((lifetime.completedCount * 100) / lifetime.eventCount).coerceIn(0, 100)
        } else {
            0
        }

        return ListeningRecapSummary(
            period = period,
            totalListenMs = lifetime.totalListenMs,
            totalPlays = lifetime.countedPlays,
            uniqueTracks = lifetime.distinctTracks,
            uniqueArtists = lifetime.distinctArtists,
            uniqueAlbums = 0,
            completionRate = completionRate,
            highlights = RecapHighlightStat(),
            topTracks = topTracks,
            topArtists = topArtists,
            topAlbums = emptyList(),
            dailyActivity = emptyDailyPoints(period, today)
        )
    }

    private fun dayOf(epochMs: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    private fun albumKey(album: String, artist: String): String {
        val a = album.trim().lowercase()
        val ar = artist.trim().lowercase()
        return if (a.isEmpty()) "" else "$a|$ar"
    }

    private fun isUnknownAlbum(album: String): Boolean {
        val lower = album.trim().lowercase()
        return lower == "unknown" || lower == "unknown album" || lower == "untitled" || lower == "none"
    }
}
