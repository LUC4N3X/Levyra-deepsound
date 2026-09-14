package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.ListenPlayPolicy
import com.luc4n3x.levyra.domain.ListeningInsightsActivityPoint
import com.luc4n3x.levyra.domain.ListeningInsightsArtist
import com.luc4n3x.levyra.domain.ListeningInsightsHistoryItem
import com.luc4n3x.levyra.domain.ListeningInsightsMetrics
import com.luc4n3x.levyra.domain.ListeningInsightsPeriod
import com.luc4n3x.levyra.domain.ListeningInsightsRanges
import com.luc4n3x.levyra.domain.ListeningInsightsSnapshot
import com.luc4n3x.levyra.domain.ListeningInsightsTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId

class ListeningInsightsRepository(context: Context) {
    private val database = LevyraDatabase.get(context.applicationContext)
    private val eventsDao = database.listenEventsDao()
    private val lifetimeDao = database.listenLifetimeDao()

    suspend fun snapshot(
        period: ListeningInsightsPeriod,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): ListeningInsightsSnapshot = withContext(Dispatchers.IO) {
        val range = ListeningInsightsRanges.current(period, nowMs, zone)
        val aggregate = eventsDao.insightsAggregate(
            range.fromMs,
            range.toMs,
            ListenPlayPolicy.MIN_EVENT_MS,
            ListenPlayPolicy.COUNTED_PLAY_MS,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
        )
        val previousMs = if (range.previousFromMs != null && range.previousToMs != null) {
            eventsDao.insightsAggregate(
                range.previousFromMs,
                range.previousToMs,
                ListenPlayPolicy.MIN_EVENT_MS,
                ListenPlayPolicy.COUNTED_PLAY_MS,
                ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
                ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
            ).listenedMs
        } else {
            0L
        }
        val days = eventsDao.insightsDays(
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
        val filledDays = ListeningInsightsRanges.fillDailyActivity(
            fromMs = if (period == ListeningInsightsPeriod.AllTime) {
                days.firstOrNull()?.epochMs ?: range.fromMs
            } else {
                range.fromMs
            },
            toMs = range.toMs,
            daily = days,
            zone = zone
        )
        val hours = LongArray(24)
        eventsDao.insightsHours(range.fromMs, range.toMs, ListenPlayPolicy.MIN_EVENT_MS)
            .forEach { row -> if (row.hour in 0..23) hours[row.hour] = row.listenedMs }
        val windowTracks = eventsDao.insightsTopTracks(
            range.fromMs,
            range.toMs,
            TOP_LIMIT,
            ListenPlayPolicy.MIN_EVENT_MS,
            ListenPlayPolicy.COUNTED_PLAY_MS,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
        )
        val windowArtists = eventsDao.insightsTopArtists(
            range.fromMs,
            range.toMs,
            TOP_LIMIT,
            ListenPlayPolicy.MIN_EVENT_MS,
            ListenPlayPolicy.COUNTED_PLAY_MS,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
            ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
        )
        val lifetime = if (period == ListeningInsightsPeriod.AllTime) {
            val totals = lifetimeDao.trackTotals()
            LifetimeSnapshot(
                listenedMs = totals.listenedMs,
                countedPlays = totals.countedPlays,
                completedCount = totals.completedCount,
                eventCount = totals.eventCount,
                distinctTracks = totals.distinctTracks,
                distinctArtists = lifetimeDao.artistCount()
            )
        } else {
            null
        }
        val topTracks = if (period == ListeningInsightsPeriod.AllTime) {
            lifetimeDao.topTracks(TOP_LIMIT).map { row ->
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
        } else {
            windowTracks.map { row ->
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
        val topArtists = if (period == ListeningInsightsPeriod.AllTime) {
            lifetimeDao.topArtists(TOP_LIMIT).map { row ->
                val event = eventsDao.findLatestByArtist(row.name)
                ListeningInsightsArtist(
                    name = row.name,
                    artworkUrl = event?.let { it.largeThumbnailUrl.ifBlank { it.thumbnailUrl } }.orEmpty(),
                    listenedMs = row.listenedMs,
                    plays = row.countedPlays
                )
            }
        } else {
            windowArtists.map { row ->
                ListeningInsightsArtist(
                    name = row.name,
                    artworkUrl = row.thumbnailUrl,
                    listenedMs = row.listenedMs,
                    plays = row.countedPlays.toInt(),
                    trackCount = row.trackCount.toInt()
                )
            }
        }
        val listenedMs = lifetime?.listenedMs ?: aggregate.listenedMs
        val eventCount = lifetime?.eventCount ?: aggregate.eventCount.toInt()
        val completedCount = lifetime?.completedCount ?: aggregate.completedCount.toInt()
        val discoveryRangeFrom = if (period == ListeningInsightsPeriod.AllTime) {
            (nowMs - DISCOVERY_REFERENCE_MS).coerceAtLeast(0L)
        } else {
            range.fromMs
        }
        val discovered = lifetimeDao.discoveredTrackCount(discoveryRangeFrom, range.toMs)
        val discoveryTrackCount = if (period == ListeningInsightsPeriod.AllTime) {
            eventsDao.insightsAggregate(
                discoveryRangeFrom,
                range.toMs,
                ListenPlayPolicy.MIN_EVENT_MS,
                ListenPlayPolicy.COUNTED_PLAY_MS,
                ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
                ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
            ).distinctTracks.toInt()
        } else {
            lifetime?.distinctTracks ?: aggregate.distinctTracks.toInt()
        }
        val distinctTracks = lifetime?.distinctTracks ?: aggregate.distinctTracks.toInt()
        val peakHour = hours.indices.maxByOrNull { hours[it] }?.takeIf { hours[it] > 0L } ?: -1
        val peakDay = days.maxByOrNull { it.listenedMs }?.epochMs ?: 0L
        ListeningInsightsSnapshot(
            period = period,
            metrics = ListeningInsightsMetrics(
                listenedMs = listenedMs,
                countedPlays = lifetime?.countedPlays ?: aggregate.countedPlays.toInt(),
                distinctTracks = distinctTracks,
                distinctArtists = lifetime?.distinctArtists ?: aggregate.distinctArtists.toInt(),
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
                trendPercent = if (period == ListeningInsightsPeriod.AllTime) null else {
                    ListeningInsightsRanges.trendPercent(listenedMs, previousMs)
                }
            ),
            activity = ListeningInsightsRanges.aggregateActivity(period, filledDays, zone),
            hourBuckets = hours.toList(),
            topTracks = topTracks,
            topArtists = topArtists,
            detailedFromMs = days.firstOrNull()?.epochMs ?: range.fromMs
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
