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

    private data class ScopeStats(
        val totalListenMs: Long,
        val completionRate: Int,
        val totalPlays: Int,
        val uniqueTrackKeys: Set<String>,
        val uniqueArtists: Int,
        val uniqueAlbums: Int,
        val byDay: Map<LocalDate, List<ListenEvent>>,
        val currentStreak: Int,
        val bestStreak: Int,
        val mostActiveDayDate: LocalDate?,
        val mostActiveDayMinutes: Long,
        val averageMinutesPerDay: Long
    )

    private data class ListeningTimeProfile(
        val favoriteHour: Int,
        val favoriteDaypart: Daypart?
    )

    private data class DiscoveryMix(
        val discoveryRate: Int,
        val repeatRate: Int
    )

    private data class RecapFinalState(
        val totalListenMs: Long,
        val totalPlays: Int,
        val uniqueTracks: Int,
        val uniqueArtists: Int,
        val uniqueAlbums: Int,
        val completionRate: Int,
        val bestStreak: Int,
        val averageMinutesPerDay: Long,
        val mostActiveDayDate: LocalDate?,
        val mostActiveDayMinutes: Long,
        val favoriteHour: Int,
        val favoriteDaypart: Daypart?,
        val mostReplayedTrack: TopTrackStat?,
        val topTracks: List<TopTrackStat>,
        val topArtists: List<TopArtistStat>,
        val topAlbums: List<TopAlbumStat>
    )

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
            return emptyOrLifetimeSummary(period, lifetime, nowMs, zone, today)
        }

        val stats = scopedStats(scoped, period, today, zone)
        val timeProfile = listeningTimeProfile(scoped, zone)
        val allTracks = allTrackStats(scoped)
        val topTracks = allTracks
            .sortedWith(compareByDescending<TopTrackStat> { it.listenedMs }.thenByDescending { it.plays })
            .take(TOP_LIMIT)
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
        val topArtists = topArtists(scoped)
        val topAlbums = topAlbums(scoped)
        val mostReplayedTrack = allTracks
            .filter { it.plays >= 2 }
            .maxWithOrNull(compareBy<TopTrackStat> { it.plays }.thenBy { it.listenedMs })

        val hasLifetimeOlderHistory = period == ListeningRecapPeriod.AllTime &&
            lifetime != null && lifetime.hasSignal && lifetime.totalListenMs > stats.totalListenMs
        val discovery = discoveryMix(
            period = period,
            uniqueTrackKeys = stats.uniqueTrackKeys,
            firstPlayedMap = firstPlayedMap,
            validEvents = valid,
            cutoff = cutoff
        )

        val finalState = mergeLifetimeStats(
            base = RecapFinalState(
                totalListenMs = stats.totalListenMs,
                totalPlays = stats.totalPlays,
                uniqueTracks = stats.uniqueTrackKeys.size,
                uniqueArtists = stats.uniqueArtists,
                uniqueAlbums = stats.uniqueAlbums,
                completionRate = stats.completionRate,
                bestStreak = stats.bestStreak,
                averageMinutesPerDay = stats.averageMinutesPerDay,
                mostActiveDayDate = stats.mostActiveDayDate,
                mostActiveDayMinutes = stats.mostActiveDayMinutes,
                favoriteHour = timeProfile.favoriteHour,
                favoriteDaypart = timeProfile.favoriteDaypart,
                mostReplayedTrack = mostReplayedTrack,
                topTracks = topTracks,
                topArtists = topArtists,
                topAlbums = topAlbums
            ),
            period = period,
            lifetime = lifetime,
            scoped = scoped,
            hasLifetimeOlderHistory = hasLifetimeOlderHistory,
            today = today,
            zone = zone
        )

        return ListeningRecapSummary(
            period = period,
            totalListenMs = finalState.totalListenMs,
            totalPlays = finalState.totalPlays,
            uniqueTracks = finalState.uniqueTracks,
            uniqueArtists = finalState.uniqueArtists,
            uniqueAlbums = finalState.uniqueAlbums,
            completionRate = finalState.completionRate,
            highlights = RecapHighlightStat(
                currentStreakDays = stats.currentStreak,
                bestStreakDays = finalState.bestStreak,
                averageMinutesPerDay = finalState.averageMinutesPerDay,
                mostActiveDayDate = finalState.mostActiveDayDate,
                mostActiveDayMinutes = finalState.mostActiveDayMinutes,
                favoriteHour = finalState.favoriteHour,
                favoriteDaypart = finalState.favoriteDaypart,
                mostReplayedTrack = finalState.mostReplayedTrack,
                discoveryRate = discovery.discoveryRate,
                repeatRate = discovery.repeatRate,
                isWindowBounded = hasLifetimeOlderHistory
            ),
            topTracks = finalState.topTracks,
            topArtists = finalState.topArtists,
            topAlbums = finalState.topAlbums,
            dailyActivity = generateDailyActivity(stats.byDay, period, today)
        )
    }

    private fun emptyOrLifetimeSummary(
        period: ListeningRecapPeriod,
        lifetime: LifetimeListening?,
        nowMs: Long,
        zone: ZoneId,
        today: LocalDate
    ): ListeningRecapSummary {
        return if (period == ListeningRecapPeriod.AllTime && lifetime != null && lifetime.hasSignal) {
            buildFromLifetime(lifetime, period, nowMs, zone)
        } else {
            ListeningRecapSummary(
                period = period,
                dailyActivity = emptyDailyPoints(period, today)
            )
        }
    }

    private fun scopedStats(
        scoped: List<ListenEvent>,
        period: ListeningRecapPeriod,
        today: LocalDate,
        zone: ZoneId
    ): ScopeStats {
        val totalListenMs = scoped.sumOf { it.listenedMs }
        val completedCount = scoped.count { it.completed }
        val uniqueTrackKeys = scoped.map { ListenIdentity.trackKey(it) }.toSet()
        val byDay = scoped.groupBy { dayOf(it.startedAt, zone) }
        val activeDates = byDay.keys
        val mostActiveDayEntry = byDay.entries.maxByOrNull { entry ->
            entry.value.sumOf { event -> event.listenedMs }
        }
        val totalMinutes = totalListenMs / 60_000L
        val averageMinutesPerDay = when {
            period.days > 0 -> totalMinutes / period.days.toLong()
            activeDates.isNotEmpty() -> totalMinutes / activeDates.size.toLong()
            else -> 0L
        }

        return ScopeStats(
            totalListenMs = totalListenMs,
            completionRate = (completedCount * 100) / scoped.size,
            totalPlays = scoped.count { ListenPlayPolicy.isCountedPlay(it) },
            uniqueTrackKeys = uniqueTrackKeys,
            uniqueArtists = scoped.mapNotNull { event ->
                primaryArtistIdentityKey(event).takeIf { it.isNotBlank() }
            }.toSet().size,
            uniqueAlbums = scoped.map { albumKey(it.album, it.artist) }
                .filter { it.isNotBlank() }
                .toSet()
                .size,
            byDay = byDay,
            currentStreak = calculateStreak(activeDates, today),
            bestStreak = calculateLongestStreak(activeDates),
            mostActiveDayDate = mostActiveDayEntry?.key,
            mostActiveDayMinutes = (mostActiveDayEntry?.value?.sumOf { it.listenedMs } ?: 0L) / 60_000L,
            averageMinutesPerDay = averageMinutesPerDay
        )
    }

    private fun listeningTimeProfile(events: List<ListenEvent>, zone: ZoneId): ListeningTimeProfile {
        val hourlyBuckets = LongArray(24)
        val daypartBuckets = LongArray(Daypart.entries.size)

        events.forEach { event ->
            val hour = Instant.ofEpochMilli(event.startedAt).atZone(zone).hour
            if (hour !in 0..23) return@forEach
            hourlyBuckets[hour] += event.listenedMs
            val daypart = when (hour) {
                in 5..11 -> Daypart.Morning
                in 12..16 -> Daypart.Afternoon
                in 17..21 -> Daypart.Evening
                else -> Daypart.Night
            }
            daypartBuckets[daypart.ordinal] += event.listenedMs
        }

        val favoriteHour = hourlyBuckets.indices
            .filter { hourlyBuckets[it] > 0L }
            .maxByOrNull { hourlyBuckets[it] }
            ?: -1
        val favoriteDaypart = Daypart.entries
            .filter { daypartBuckets[it.ordinal] > 0L }
            .maxByOrNull { daypartBuckets[it.ordinal] }

        return ListeningTimeProfile(
            favoriteHour = favoriteHour,
            favoriteDaypart = favoriteDaypart
        )
    }

    private fun discoveryMix(
        period: ListeningRecapPeriod,
        uniqueTrackKeys: Set<String>,
        firstPlayedMap: Map<String, Long>,
        validEvents: List<ListenEvent>,
        cutoff: Long
    ): DiscoveryMix {
        val isDiscoveryPeriod = period == ListeningRecapPeriod.Days7 ||
            period == ListeningRecapPeriod.Days30
        if (!isDiscoveryPeriod || uniqueTrackKeys.isEmpty()) return DiscoveryMix(-1, -1)

        val hasAuthoritativeFirstPlayed = uniqueTrackKeys.all { key ->
            (firstPlayedMap[key] ?: 0L) > 0L
        }
        if (!hasAuthoritativeFirstPlayed) return DiscoveryMix(-1, -1)

        val hasPriorHistory = firstPlayedMap.values.any { it in 1 until cutoff } ||
            validEvents.any { it.startedAt in 1 until cutoff }
        if (!hasPriorHistory) return DiscoveryMix(-1, -1)

        val discoveredCount = uniqueTrackKeys.count { key ->
            (firstPlayedMap[key] ?: 0L) >= cutoff
        }
        val discoveryRate = ((discoveredCount * 100) / uniqueTrackKeys.size).coerceIn(0, 100)
        return DiscoveryMix(
            discoveryRate = discoveryRate,
            repeatRate = (100 - discoveryRate).coerceIn(0, 100)
        )
    }

    private fun mergeLifetimeStats(
        base: RecapFinalState,
        period: ListeningRecapPeriod,
        lifetime: LifetimeListening?,
        scoped: List<ListenEvent>,
        hasLifetimeOlderHistory: Boolean,
        today: LocalDate,
        zone: ZoneId
    ): RecapFinalState {
        if (period != ListeningRecapPeriod.AllTime || lifetime == null || !lifetime.hasSignal) {
            return base
        }

        var merged = base
        if (lifetime.eventCount > 0) {
            merged = merged.copy(
                completionRate = (
                    (lifetime.completedCount.toLong() * 100L) / lifetime.eventCount.toLong()
                ).toInt().coerceIn(0, 100)
            )
        }
        if (lifetime.totalListenMs > merged.totalListenMs) {
            merged = merged.copy(
                totalListenMs = lifetime.totalListenMs,
                totalPlays = lifetime.countedPlays,
                uniqueTracks = maxOf(merged.uniqueTracks, lifetime.distinctTracks),
                uniqueArtists = maxOf(merged.uniqueArtists, lifetime.distinctArtists)
            )
        }
        if ((merged.topTracks.isEmpty() || hasLifetimeOlderHistory) && lifetime.tracks.isNotEmpty()) {
            merged = merged.copy(topTracks = lifetimeTopTracks(lifetime, scoped))
        }
        if ((merged.topArtists.isEmpty() || hasLifetimeOlderHistory) && lifetime.artists.isNotEmpty()) {
            merged = merged.copy(topArtists = lifetimeTopArtists(lifetime))
        }
        if (!hasLifetimeOlderHistory) return merged

        return merged.copy(
            uniqueAlbums = 0,
            bestStreak = 0,
            averageMinutesPerDay = calculateLifetimeDailyAverage(
                merged.totalListenMs,
                lifetime.firstPlayedAt,
                today,
                zone
            ),
            mostActiveDayDate = null,
            mostActiveDayMinutes = 0L,
            favoriteHour = -1,
            favoriteDaypart = null,
            mostReplayedTrack = null,
            topAlbums = emptyList()
        )
    }

    private fun lifetimeTopTracks(
        lifetime: LifetimeListening,
        scoped: List<ListenEvent>
    ): List<TopTrackStat> {
        val thumbnailMap = scoped
            .filter { it.thumbnailUrl.isNotBlank() }
            .associate { ListenIdentity.trackKey(it) to it.thumbnailUrl }
        return lifetime.tracks.take(TOP_LIMIT).mapIndexed { index, track ->
            TopTrackStat(
                rank = index + 1,
                trackId = track.trackId,
                title = track.title,
                artist = track.artist,
                thumbnailUrl = thumbnailMap[
                    ListenIdentity.trackKey(track.trackId, track.title, track.artist)
                ] ?: "",
                plays = track.plays,
                listenedMs = track.listenedMs
            )
        }
    }

    private fun lifetimeTopArtists(lifetime: LifetimeListening): List<TopArtistStat> {
        return lifetime.artists.take(TOP_LIMIT).mapIndexed { index, artist ->
            TopArtistStat(
                rank = index + 1,
                name = artist.name,
                thumbnailUrl = "",
                plays = artist.countedPlays,
                listenedMs = artist.listenedMs
            )
        }
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
