package com.luc4n3x.levyra.data

import android.content.Context
import androidx.room.withTransaction
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.ListeningInsightsAggregateRow
import com.luc4n3x.levyra.domain.ListenPlayPolicy
import com.luc4n3x.levyra.domain.ListeningInsightsActivityPoint
import com.luc4n3x.levyra.domain.ListeningInsightsArtist
import com.luc4n3x.levyra.domain.ListeningInsightsHistoryItem
import com.luc4n3x.levyra.domain.ListeningInsightsMetrics
import com.luc4n3x.levyra.domain.ListeningInsightsPeriod
import com.luc4n3x.levyra.domain.ListeningInsightsRange
import com.luc4n3x.levyra.domain.ListeningInsightsRanges
import com.luc4n3x.levyra.domain.ListeningInsightsSnapshot
import com.luc4n3x.levyra.domain.ListeningInsightsTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class ListeningInsightsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = LevyraDatabase.get(appContext)
    private val eventsDao = database.listenEventsDao()
    private val lifetimeDao = database.listenLifetimeDao()
    private val spotifyArtwork = SpotifyArtistArtworkRepository.get(appContext)
    private val appleArtwork = AppleArtistArtworkRepository.get(appContext)

    suspend fun snapshot(
        period: ListeningInsightsPeriod,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): ListeningInsightsSnapshot = withContext(Dispatchers.IO) {
        database.withTransaction {
            val range = ListeningInsightsRanges.current(period, nowMs, zone)
            val isAllTime = period == ListeningInsightsPeriod.AllTime

            val aggregate = resolveWindowAggregate(isAllTime, range)
            val previousMs = resolvePreviousMs(isAllTime, range)
            val days = resolveDailyActivityDays(period, range, zone)
            val activityPoints = resolveActivityPoints(period, range, days, zone)
            val hourBuckets = resolveHourBuckets(range)
            val topTracks = resolveTopTracks(isAllTime, range)
            val topArtists = resolveTopArtists(isAllTime, range)
            val metrics = resolveMetrics(
                period = period,
                range = range,
                nowMs = nowMs,
                aggregate = aggregate,
                previousMs = previousMs,
                hourBuckets = hourBuckets,
                activityPoints = activityPoints,
                days = days
            )

            ListeningInsightsSnapshot(
                period = period,
                metrics = metrics,
                activity = activityPoints,
                hourBuckets = hourBuckets,
                topTracks = topTracks,
                topArtists = topArtists,
                detailedFromMs = if (isAllTime) {
                    days.firstOrNull()?.epochMs ?: range.fromMs
                } else {
                    range.fromMs
                }
            )
        }
    }

    private suspend fun resolveWindowAggregate(
        isAllTime: Boolean,
        range: ListeningInsightsRange
    ): ListeningInsightsAggregateRow? {
        if (isAllTime) return null
        return eventsDao.insightsAggregate(
            range.fromMs,
            range.toMs,
            ListenPlayPolicy.MIN_EVENT_MS,
            ListenPlayPolicy.COUNTED_PLAY_MS,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
        )
    }

    private suspend fun resolvePreviousMs(
        isAllTime: Boolean,
        range: ListeningInsightsRange
    ): Long {
        val fromMs = range.previousFromMs ?: return 0L
        val toMs = range.previousToMs ?: return 0L
        if (isAllTime) return 0L
        return eventsDao.insightsAggregate(
            fromMs,
            toMs,
            ListenPlayPolicy.MIN_EVENT_MS,
            ListenPlayPolicy.COUNTED_PLAY_MS,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
        ).listenedMs
    }

    private suspend fun resolveDailyActivityDays(
        period: ListeningInsightsPeriod,
        range: ListeningInsightsRange,
        zone: ZoneId
    ): List<ListeningInsightsActivityPoint> {
        if (period == ListeningInsightsPeriod.Day) return emptyList()
        return eventsDao.insightsDays(
            range.fromMs,
            range.toMs,
            ListenPlayPolicy.MIN_EVENT_MS,
            ListenPlayPolicy.COUNTED_PLAY_MS,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
        ).map { row ->
            ListeningInsightsActivityPoint(
                epochMs = ListeningInsightsRanges.dayEpoch(row.dayKey, zone),
                listenedMs = row.listenedMs,
                plays = row.countedPlays.toInt()
            )
        }
    }

    private suspend fun resolveDayHourlyActivity(
        range: ListeningInsightsRange,
        zone: ZoneId
    ): List<ListeningInsightsActivityPoint> {
        val hourlyEvents = eventsDao.insightsTimelineEvents(
            range.fromMs,
            range.toMs,
            ListenPlayPolicy.MIN_EVENT_MS,
            ListenPlayPolicy.COUNTED_PLAY_MS,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
        ).groupBy { event ->
            Instant.ofEpochMilli(event.startedAt).atZone(zone).truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli()
        }.map { (epochMs, events) ->
            ListeningInsightsActivityPoint(
                epochMs = epochMs,
                listenedMs = events.sumOf { it.listenedMs },
                plays = events.sumOf { it.countedPlays.toInt() }
            )
        }
        return ListeningInsightsRanges.fillHourlyActivity(range.fromMs, range.toMs, hourlyEvents, zone)
    }

    private suspend fun resolveActivityPoints(
        period: ListeningInsightsPeriod,
        range: ListeningInsightsRange,
        days: List<ListeningInsightsActivityPoint>,
        zone: ZoneId
    ): List<ListeningInsightsActivityPoint> {
        if (period == ListeningInsightsPeriod.Day) {
            return resolveDayHourlyActivity(range, zone)
        }
        val fromMs = if (period == ListeningInsightsPeriod.AllTime) {
            days.firstOrNull()?.epochMs ?: range.fromMs
        } else {
            range.fromMs
        }
        val filledDays = ListeningInsightsRanges.fillDailyActivity(
            fromMs = fromMs,
            toMs = range.toMs,
            daily = days,
            zone = zone
        )
        return ListeningInsightsRanges.aggregateActivity(period, filledDays, zone)
    }

    private suspend fun resolveHourBuckets(range: ListeningInsightsRange): List<Long> {
        val hours = LongArray(24)
        eventsDao.insightsHours(range.fromMs, range.toMs, ListenPlayPolicy.MIN_EVENT_MS)
            .forEach { row -> if (row.hour in 0..23) hours[row.hour] = row.listenedMs }
        return hours.toList()
    }

    private suspend fun resolveTopTracks(
        isAllTime: Boolean,
        range: ListeningInsightsRange
    ): List<ListeningInsightsTrack> {
        if (isAllTime) {
            return lifetimeDao.topTracks(TOP_LIMIT).map { row ->
                val event = row.trackId.takeIf(String::isNotBlank)?.let { eventsDao.findLatestByTrackId(it) }
                ListeningInsightsTrack(
                    trackId = row.trackId,
                    title = row.title,
                    artist = row.artist,
                    artworkUrl = event?.let { it.largeThumbnailUrl.ifBlank { it.thumbnailUrl } }.orEmpty(),
                    album = event?.album.orEmpty(),
                    listenedMs = row.listenedMs,
                    plays = row.countedPlays
                )
            }
        }
        return eventsDao.insightsTopTracks(
            range.fromMs,
            range.toMs,
            TOP_LIMIT,
            ListenPlayPolicy.MIN_EVENT_MS,
            ListenPlayPolicy.COUNTED_PLAY_MS,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
        ).map { row ->
            ListeningInsightsTrack(
                trackId = row.trackId,
                title = row.title,
                artist = row.artist,
                album = row.album,
                artworkUrl = row.largeThumbnailUrl.ifBlank { row.thumbnailUrl },
                listenedMs = row.listenedMs,
                plays = row.countedPlays.toInt()
            )
        }
    }

    private suspend fun resolveTopArtists(
        isAllTime: Boolean,
        range: ListeningInsightsRange
    ): List<ListeningInsightsArtist> {
        if (isAllTime) {
            return lifetimeDao.topArtists(TOP_LIMIT).map { row ->
                ListeningInsightsArtist(
                    name = row.name,
                    artworkUrl = "",
                    listenedMs = row.listenedMs,
                    plays = row.countedPlays,
                    trackCount = 0
                )
            }
        }
        return eventsDao.insightsTopArtists(
            range.fromMs,
            range.toMs,
            TOP_LIMIT,
            ListenPlayPolicy.MIN_EVENT_MS,
            ListenPlayPolicy.COUNTED_PLAY_MS,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
        ).map { row ->
            ListeningInsightsArtist(
                name = row.name,
                artworkUrl = "",
                listenedMs = row.listenedMs,
                plays = row.countedPlays.toInt(),
                trackCount = row.trackCount.toInt()
            )
        }
    }

    private suspend fun resolveLifetimeSnapshot(isAllTime: Boolean): LifetimeSnapshot? {
        if (!isAllTime) return null
        val totals = lifetimeDao.trackTotals()
        return LifetimeSnapshot(
            listenedMs = totals.listenedMs,
            countedPlays = totals.countedPlays,
            completedCount = totals.completedCount,
            eventCount = totals.eventCount,
            distinctTracks = totals.distinctTracks,
            distinctArtists = lifetimeDao.artistCount()
        )
    }

    private suspend fun resolveDiscovery(
        isAllTime: Boolean,
        range: ListeningInsightsRange,
        nowMs: Long,
        distinctTracks: Int
    ): Pair<Int, Int> {
        val discoveryRangeFrom = if (isAllTime) {
            (nowMs - DISCOVERY_REFERENCE_MS).coerceAtLeast(0L)
        } else {
            range.fromMs
        }
        val discovered = lifetimeDao.discoveredTrackCount(discoveryRangeFrom, range.toMs)
        val discoveryTrackCount = if (isAllTime) {
            eventsDao.insightsAggregate(
                discoveryRangeFrom,
                range.toMs,
                ListenPlayPolicy.MIN_EVENT_MS,
                ListenPlayPolicy.COUNTED_PLAY_MS,
                ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
                ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
            ).distinctTracks.toInt()
        } else {
            distinctTracks
        }
        return discovered to discoveryTrackCount
    }

    private suspend fun resolveMetrics(
        period: ListeningInsightsPeriod,
        range: ListeningInsightsRange,
        nowMs: Long,
        aggregate: ListeningInsightsAggregateRow?,
        previousMs: Long,
        hourBuckets: List<Long>,
        activityPoints: List<ListeningInsightsActivityPoint>,
        days: List<ListeningInsightsActivityPoint>
    ): ListeningInsightsMetrics {
        val isAllTime = period == ListeningInsightsPeriod.AllTime
        val lifetime = resolveLifetimeSnapshot(isAllTime)

        val listenedMs = lifetime?.listenedMs ?: aggregate?.listenedMs ?: 0L
        val eventCount = lifetime?.eventCount ?: aggregate?.eventCount?.toInt() ?: 0
        val completedCount = lifetime?.completedCount ?: aggregate?.completedCount?.toInt() ?: 0
        val distinctTracks = lifetime?.distinctTracks ?: aggregate?.distinctTracks?.toInt() ?: 0
        val distinctArtists = lifetime?.distinctArtists ?: aggregate?.distinctArtists?.toInt() ?: 0

        val (discovered, discoveryTrackCount) = resolveDiscovery(isAllTime, range, nowMs, distinctTracks)

        val peakHour = hourBuckets.indices.maxByOrNull { hourBuckets[it] }?.takeIf { hourBuckets[it] > 0L } ?: -1
        val peakDay = if (period == ListeningInsightsPeriod.Day) {
            activityPoints.maxByOrNull { it.listenedMs }?.epochMs ?: 0L
        } else {
            days.maxByOrNull { it.listenedMs }?.epochMs ?: 0L
        }

        return ListeningInsightsMetrics(
            listenedMs = listenedMs,
            countedPlays = lifetime?.countedPlays ?: aggregate?.countedPlays?.toInt() ?: 0,
            distinctTracks = distinctTracks,
            distinctArtists = distinctArtists,
            completionRate = if (eventCount > 0) {
                (completedCount.toLong() * 100L / eventCount.toLong()).coerceIn(0L, 100L).toInt()
            } else {
                0
            },
            discoveryCount = discovered,
            discoveryRate = if (discoveryTrackCount > 0) {
                (discovered * 100 / discoveryTrackCount).coerceIn(0, 100)
            } else {
                0
            },
            peakHour = peakHour,
            peakDayEpochMs = peakDay,
            trendPercent = if (isAllTime) null else {
                ListeningInsightsRanges.trendPercent(listenedMs, previousMs)
            }
        )
    }

    suspend fun historyPage(
        period: ListeningInsightsPeriod,
        query: String,
        cursorStartedAt: Long = Long.MAX_VALUE,
        cursorId: Long = Long.MAX_VALUE,
        limit: Int = HISTORY_PAGE_SIZE,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<ListeningInsightsHistoryItem> = withContext(Dispatchers.IO) {
        val range = ListeningInsightsRanges.current(period, nowMs, zone)
        eventsDao.insightsHistoryPage(
            fromMs = range.fromMs,
            toMs = range.toMs,
            query = query.trim().take(MAX_QUERY_LENGTH),
            beforeStartedAt = cursorStartedAt,
            beforeId = cursorId,
            limit = limit.coerceIn(1, HISTORY_PAGE_SIZE),
            minimumEventMs = ListenPlayPolicy.MIN_EVENT_MS
        ).map { event ->
            ListeningInsightsHistoryItem(
                id = event.id,
                track = ListeningInsightsTrack(
                    trackId = event.trackId,
                    title = event.title,
                    artist = event.artist,
                    album = event.album,
                    artworkUrl = event.largeThumbnailUrl.ifBlank { event.thumbnailUrl },
                    listenedMs = event.listenedMs,
                    plays = if (ListenPlayPolicy.isCountedPlay(event.listenedMs, event.durationMs, event.completed)) 1 else 0
                ),
                startedAt = event.startedAt,
                listenedMs = event.listenedMs,
                completed = event.completed
            )
        }
    }

    suspend fun resolveArtistPortraits(
        artists: List<ListeningInsightsArtist>
    ): List<ListeningInsightsArtist> = withContext(Dispatchers.IO) {
        if (artists.isEmpty()) return@withContext emptyList()
        val semaphore = Semaphore(4)
        coroutineScope {
            artists.map { artist ->
                async {
                    if (artist.name.isBlank() || artist.artworkUrl.isNotBlank()) {
                        artist
                    } else {
                        val portrait = try {
                            semaphore.withPermit {
                                val spotify = spotifyArtwork.resolveArtistPortrait(artist.name)
                                if (spotify.isNotBlank()) spotify else appleArtwork.resolveArtistPortrait(artist.name)
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            ""
                        }
                        if (portrait.isNotBlank()) {
                            artist.copy(artworkUrl = portrait)
                        } else {
                            artist
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private data class LifetimeSnapshot(
        val listenedMs: Long,
        val countedPlays: Int,
        val completedCount: Int,
        val eventCount: Int,
        val distinctTracks: Int,
        val distinctArtists: Int
    )

    companion object {
        const val HISTORY_PAGE_SIZE = 60
        private const val TOP_LIMIT = 10
        private const val MAX_QUERY_LENGTH = 80
        private const val DISCOVERY_REFERENCE_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
