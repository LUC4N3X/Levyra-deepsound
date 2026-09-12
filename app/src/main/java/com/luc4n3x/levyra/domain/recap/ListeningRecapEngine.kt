package com.luc4n3x.levyra.domain.recap

import com.luc4n3x.levyra.domain.LifetimeListening
import com.luc4n3x.levyra.domain.ListenEvent
import com.luc4n3x.levyra.domain.ListenIdentity
import com.luc4n3x.levyra.domain.ListenPlayPolicy
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.primaryArtistSegment
import com.luc4n3x.levyra.domain.primaryArtistCredit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit

object ListeningRecapEngine {

    private const val TOP_LIMIT = 5
    private const val DISCOVERY_REFERENCE_DAYS = 30L

    fun periodCutoffMs(period: ListeningRecapPeriod, today: LocalDate, zone: ZoneId): Long {
        if (period.days <= 0) return 0L
        val startDate = today.minusDays(period.days - 1L)
        return startDate.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun build(
        events: List<ListenEvent>,
        period: ListeningRecapPeriod,
        lifetime: LifetimeListening? = null,
        firstPlayedMap: Map<String, Long> = emptyMap(),
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): ListeningRecapSummary {
        val valid = events.filter {
            ListenPlayPolicy.isRecordableEvent(it.listenedMs) && it.startedAt in 1..nowMs
        }

        val today = dayOf(nowMs, zone)
        val cutoff = periodCutoffMs(period, today, zone)
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

        val totalListenMs = scoped.sumOf { it.listenedMs }
        val completedCount = scoped.count { it.completed }
        val completionRate = if (scoped.isNotEmpty()) (completedCount * 100) / scoped.size else 0

        val totalPlays = scoped.count { ListenPlayPolicy.isCountedPlay(it) }
        val uniqueTrackKeys = scoped.map { ListenIdentity.trackKey(it) }.toSet()
        val uniqueArtists = scoped.mapNotNull { event ->
            primaryArtistIdentityKey(event).takeIf { it.isNotBlank() }
        }.toSet().size
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
        ).filter { it.second > 0L }.maxByOrNull { it.second }?.first

        val allTrackStats = allTrackStats(scoped)
        val topTracks = allTrackStats
            .sortedWith(compareByDescending<TopTrackStat> { it.listenedMs }.thenByDescending { it.plays })
            .take(TOP_LIMIT)
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
        val topArtists = topArtists(scoped)
        val topAlbums = topAlbums(scoped)

        val mostReplayedTrack = allTrackStats
            .filter { it.plays >= 2 }
            .maxWithOrNull(compareBy<TopTrackStat> { it.plays }.thenBy { it.listenedMs })

        val hasLifetimeOlderHistory = period == ListeningRecapPeriod.AllTime &&
            lifetime != null && lifetime.hasSignal && lifetime.totalListenMs > totalListenMs

        val isDiscoveryPeriod = period == ListeningRecapPeriod.Days7 || period == ListeningRecapPeriod.Days30
        val hasAuthoritativeFirstPlayed = isDiscoveryPeriod &&
            uniqueTrackKeys.isNotEmpty() &&
            uniqueTrackKeys.all { key ->
                (firstPlayedMap[key] ?: 0L) > 0L
            }
        val hasPriorHistory = hasAuthoritativeFirstPlayed && (
            firstPlayedMap.values.any { it in 1 until cutoff } || valid.any { it.startedAt in 1 until cutoff }
        )

        val (discoveryRate, repeatRate) = if (hasAuthoritativeFirstPlayed && hasPriorHistory) {
            val discoveredCount = uniqueTrackKeys.count { key ->
                (firstPlayedMap[key] ?: 0L) >= cutoff
            }
            val disc = ((discoveredCount * 100) / uniqueTrackKeys.size).coerceIn(0, 100)
            val rep = (100 - disc).coerceIn(0, 100)
            disc to rep
        } else {
            -1 to -1
        }

        val dailyActivity = generateDailyActivity(byDay, period, today)

        var finalListenMs = totalListenMs
        var finalPlays = totalPlays
        var finalTracks = uniqueTrackKeys.size
        var finalArtists = uniqueArtists
        var finalTopTracks = topTracks
        var finalTopArtists = topArtists
        var finalTopAlbums = topAlbums
        var finalUniqueAlbums = uniqueAlbums
        var finalBestStreak = bestStreak
        var finalFavoriteDaypart: Daypart? = favoriteDaypart
        var finalFavoriteHour = peakHour
        var finalMostActiveDayDate: LocalDate? = mostActiveDayDate
        var finalMostActiveDayMinutes = mostActiveDayMinutes
        var finalMostReplayedTrack = mostReplayedTrack
        var finalCompletionRate = completionRate
        var finalAverageMinutesPerDay = averageMinutesPerDay

        if (period == ListeningRecapPeriod.AllTime && lifetime != null && lifetime.hasSignal) {
            if (lifetime.eventCount > 0) {
                finalCompletionRate = ((lifetime.completedCount.toLong() * 100L) / lifetime.eventCount.toLong()).toInt().coerceIn(0, 100)
            }
            if (lifetime.totalListenMs > finalListenMs) {
                finalListenMs = lifetime.totalListenMs
                finalPlays = lifetime.countedPlays
                finalTracks = maxOf(finalTracks, lifetime.distinctTracks)
                finalArtists = maxOf(finalArtists, lifetime.distinctArtists)
            }
            if ((finalTopTracks.isEmpty() || hasLifetimeOlderHistory) && lifetime.tracks.isNotEmpty()) {
                val thumbMap = scoped.filter { it.thumbnailUrl.isNotBlank() }
                    .associate { ListenIdentity.trackKey(it) to it.thumbnailUrl }
                finalTopTracks = lifetime.tracks.take(TOP_LIMIT).mapIndexed { idx, t ->
                    TopTrackStat(
                        rank = idx + 1,
                        trackId = t.trackId,
                        title = t.title,
                        artist = t.artist,
                        thumbnailUrl = thumbMap[ListenIdentity.trackKey(t.trackId, t.title, t.artist)] ?: "",
                        plays = t.plays,
                        listenedMs = t.listenedMs
                    )
                }
            }
            if ((finalTopArtists.isEmpty() || hasLifetimeOlderHistory) && lifetime.artists.isNotEmpty()) {
                finalTopArtists = lifetime.artists.take(TOP_LIMIT).mapIndexed { idx, a ->
                    TopArtistStat(
                        rank = idx + 1,
                        name = a.name,
                        thumbnailUrl = "",
                        plays = a.countedPlays,
                        listenedMs = a.listenedMs
                    )
                }
            }
            if (hasLifetimeOlderHistory) {
                finalTopAlbums = emptyList()
                finalUniqueAlbums = 0
                finalBestStreak = 0
                finalFavoriteDaypart = null
                finalFavoriteHour = -1
                finalMostActiveDayDate = null
                finalMostActiveDayMinutes = 0L
                finalMostReplayedTrack = null
                finalAverageMinutesPerDay = calculateLifetimeDailyAverage(finalListenMs, lifetime.firstPlayedAt, today, zone)
            }
        }

        val highlights = RecapHighlightStat(
            currentStreakDays = currentStreak,
            bestStreakDays = finalBestStreak,
            averageMinutesPerDay = finalAverageMinutesPerDay,
            mostActiveDayDate = finalMostActiveDayDate,
            mostActiveDayMinutes = finalMostActiveDayMinutes,
            favoriteHour = finalFavoriteHour,
            favoriteDaypart = finalFavoriteDaypart,
            mostReplayedTrack = finalMostReplayedTrack,
            discoveryRate = discoveryRate,
            repeatRate = repeatRate,
            isWindowBounded = hasLifetimeOlderHistory
        )

        return ListeningRecapSummary(
            period = period,
            totalListenMs = finalListenMs,
            totalPlays = finalPlays,
            uniqueTracks = finalTracks,
            uniqueArtists = finalArtists,
            uniqueAlbums = finalUniqueAlbums,
            completionRate = finalCompletionRate,
            highlights = highlights,
            topTracks = finalTopTracks,
            topArtists = finalTopArtists,
            topAlbums = finalTopAlbums,
            dailyActivity = dailyActivity
        )
    }

    private fun allTrackStats(events: List<ListenEvent>): List<TopTrackStat> {
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
    }

    private fun topTracks(events: List<ListenEvent>): List<TopTrackStat> {
        return allTrackStats(events)
            .sortedWith(compareByDescending<TopTrackStat> { it.listenedMs }.thenByDescending { it.plays })
            .take(TOP_LIMIT)
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    private fun topArtists(events: List<ListenEvent>): List<TopArtistStat> {
        return events.filter { primaryArtistIdentityKey(it).isNotBlank() }
            .groupBy(::primaryArtistIdentityKey)
            .map { (_, group) ->
                val newest = group.maxBy { it.startedAt }
                val primaryName = primaryArtistCredit(newest.artist, newest.artistBrowseIds)
                    .ifBlank { newest.artist.trim() }
                TopArtistStat(
                    rank = 0,
                    name = primaryName,
                    browseId = newest.artistBrowseIds.firstOrNull().orEmpty().trim(),
                    lookupName = newest.artist.trim(),
                    plays = group.count { ListenPlayPolicy.isCountedPlay(it) },
                    listenedMs = group.sumOf { it.listenedMs },
                    thumbnailUrl = "",
                    trackCount = group.map { ListenIdentity.trackKey(it) }.toSet().size
                )
            }
            .sortedWith(compareByDescending<TopArtistStat> { it.listenedMs }.thenByDescending { it.plays })
            .take(TOP_LIMIT)
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    private fun primaryArtistIdentityKey(event: ListenEvent): String {
        val browseId = event.artistBrowseIds.firstOrNull().orEmpty().trim()
        if (browseId.isNotBlank()) return "id:${browseId.lowercase(Locale.ROOT)}"
        val primaryName = primaryArtistCredit(event.artist, event.artistBrowseIds)
        return artistIdentityKey(primaryName)
    }

    private fun topAlbums(events: List<ListenEvent>): List<TopAlbumStat> {
        return events.filter { it.album.isNotBlank() && !isUnknownAlbum(it.album) }
            .groupBy { albumKey(it.album, it.artist) }
            .filterKeys { it.isNotBlank() }
            .map { (_, group) ->
                val newest = group.maxBy { it.startedAt }
                val canonicalArtist = group.map { primaryArtistSegment(it.artist).trim() }
                    .filter { it.isNotBlank() }
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }?.key ?: newest.artist.trim()
                TopAlbumStat(
                    rank = 0,
                    title = newest.album.trim(),
                    artist = canonicalArtist,
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
                lookupName = a.name,
                plays = a.countedPlays,
                listenedMs = a.listenedMs
            )
        }
        val completionRate = if (lifetime.eventCount > 0) {
            ((lifetime.completedCount * 100) / lifetime.eventCount).coerceIn(0, 100)
        } else {
            0
        }
        val averageMinutesPerDay = calculateLifetimeDailyAverage(
            lifetime.totalListenMs,
            lifetime.firstPlayedAt,
            today,
            zone
        )

        return ListeningRecapSummary(
            period = period,
            totalListenMs = lifetime.totalListenMs,
            totalPlays = lifetime.countedPlays,
            uniqueTracks = lifetime.distinctTracks,
            uniqueArtists = lifetime.distinctArtists,
            uniqueAlbums = 0,
            completionRate = completionRate,
            highlights = RecapHighlightStat(
                averageMinutesPerDay = averageMinutesPerDay,
                isWindowBounded = true
            ),
            topTracks = topTracks,
            topArtists = topArtists,
            topAlbums = emptyList(),
            dailyActivity = emptyDailyPoints(period, today)
        )
    }

    private fun calculateLifetimeDailyAverage(
        totalListenMs: Long,
        firstPlayedAt: Long,
        today: LocalDate,
        zone: ZoneId
    ): Long {
        if (firstPlayedAt <= 0L) return 0L
        val firstDay = dayOf(firstPlayedAt, zone)
        if (firstDay.isAfter(today)) return 0L
        val spanDays = maxOf(1L, java.time.temporal.ChronoUnit.DAYS.between(firstDay, today) + 1L)
        return (totalListenMs / 60_000L) / spanDays
    }

    private fun dayOf(epochMs: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    private fun albumKey(album: String, artist: String): String {
        val cleanAlbum = album.trim().lowercase(Locale.ROOT)
        if (cleanAlbum.isEmpty() || isUnknownAlbum(cleanAlbum)) return ""
        val primary = primaryArtistSegment(artist).ifBlank { artist.trim() }
        val artistKey = artistIdentityKey(primary)
        return "$cleanAlbum|$artistKey"
    }

    private fun isUnknownAlbum(album: String): Boolean {
        val lower = album.trim().lowercase()
        return lower == "unknown" || lower == "unknown album" || lower == "untitled" || lower == "none"
    }
}
