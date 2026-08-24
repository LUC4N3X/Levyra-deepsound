package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.ListenAllTimeAggregateEntity
import com.luc4n3x.levyra.data.local.toListenEvent
import com.luc4n3x.levyra.data.local.toListenEventEntity
import com.luc4n3x.levyra.data.local.toTrack
import com.luc4n3x.levyra.domain.ListenEvent
import com.luc4n3x.levyra.domain.ListeningPulseEngine
import com.luc4n3x.levyra.domain.PersonalizedArtistCandidate
import com.luc4n3x.levyra.domain.SmartPlaylistListen
import com.luc4n3x.levyra.domain.isCountedPlay
import com.luc4n3x.levyra.domain.rankPersonalizedArtistCandidates
import com.luc4n3x.levyra.domain.rankMostPlayedTracks
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ListeningPulseStore(context: Context) {
    private val database = LevyraDatabase.get(context.applicationContext)
    private val dao = database.listenEventsDao()
    private val aggregatesDao = database.listenAllTimeAggregatesDao()
    private val preferences = LevyraPreferences(context.applicationContext)
    private val writeLock = Mutex()

    suspend fun record(track: Track, listenedMs: Long, completed: Boolean, startedAt: Long) {
        if (listenedMs < ListeningPulseEngine.MIN_LISTEN_MS || track.title.isBlank()) return
        val cappedMs = if (track.durationMs > 0L) listenedMs.coerceAtMost(track.durationMs * MAX_LOOPS) else listenedMs
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                runCatching {
                    ensureAggregatesBackfilled()
                    val cleanTrack = track.copy(streamUrl = "", videoStreamUrl = "")
                    val artistIds = cleanTrack.artistBrowseIds.filter(String::isNotBlank).joinToString("\u001F")
                    val updated = dao.updateSession(
                        trackId = cleanTrack.id,
                        startedAt = startedAt,
                        listenedMs = cappedMs,
                        completed = if (completed) 1 else 0,
                        artistBrowseIds = artistIds
                    )
                    if (updated == 0) {
                        dao.insert(cleanTrack.toListenEventEntity(cappedMs, completed, startedAt))
                    }

                    // Update compact all-time aggregates
                    val trackKey = cleanTrack.id.trim().ifBlank {
                        "${cleanTrack.title.trim().lowercase()}|${cleanTrack.artist.trim().lowercase()}"
                    }
                    val event = ListenEvent(
                        trackId = cleanTrack.id,
                        title = cleanTrack.title,
                        artist = cleanTrack.artist,
                        listenedMs = cappedMs,
                        trackDurationMs = cleanTrack.durationMs,
                        completed = completed,
                        startedAt = startedAt,
                        artistBrowseIds = cleanTrack.artistBrowseIds
                    )
                    val isPlay = isCountedPlay(event)
                    val existing = aggregatesDao.get(trackKey)
                    if (existing != null) {
                        val playIncrement = if (isPlay && !isCountedPlay(event.copy(listenedMs = (cappedMs - 5_000L).coerceAtLeast(0L), completed = false))) 1 else 0
                        val newCompleted = if (completed && existing.completedCount == 0) 1 else existing.completedCount
                        aggregatesDao.insertOrUpdate(
                            existing.copy(
                                countedPlays = if (updated == 0 && isPlay) existing.countedPlays + 1 else existing.countedPlays + playIncrement,
                                listenedMs = if (updated == 0) existing.listenedMs + cappedMs else (existing.listenedMs + (cappedMs - listenedMs).coerceAtLeast(0L)),
                                completedCount = newCompleted,
                                lastStartedAt = maxOf(existing.lastStartedAt, startedAt),
                                artistBrowseIds = if (existing.artistBrowseIds.isBlank()) artistIds else existing.artistBrowseIds
                            )
                        )
                    } else {
                        aggregatesDao.insertOrUpdate(
                            ListenAllTimeAggregateEntity(
                                trackKey = trackKey,
                                trackId = cleanTrack.id,
                                title = cleanTrack.title,
                                artist = cleanTrack.artist,
                                countedPlays = if (isPlay) 1 else 0,
                                listenedMs = cappedMs,
                                completedCount = if (completed) 1 else 0,
                                firstStartedAt = startedAt,
                                lastStartedAt = startedAt,
                                artistBrowseIds = artistIds
                            )
                        )
                    }

                    pruneIfDue(System.currentTimeMillis())
                }.onFailure { Timber.w(it, "Listen event write failed") }
            }
        }
    }

    private suspend fun ensureAggregatesBackfilled() {
        if (aggregatesDao.count() == 0 && dao.count() > 0) {
            val allEvents = dao.all()
            if (allEvents.isNotEmpty()) {
                val aggregates = allEvents.groupBy {
                    it.trackId.trim().ifBlank { "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}" }
                }.map { (key, list) ->
                    val first = list.minByOrNull { it.startedAt } ?: list.first()
                    val last = list.maxByOrNull { it.startedAt } ?: list.first()
                    val domainEvents = list.map { it.toListenEvent() }
                    ListenAllTimeAggregateEntity(
                        trackKey = key,
                        trackId = first.trackId,
                        title = first.title,
                        artist = first.artist,
                        countedPlays = domainEvents.count { isCountedPlay(it) },
                        listenedMs = list.sumOf { it.listenedMs },
                        completedCount = list.count { it.completed },
                        firstStartedAt = first.startedAt,
                        lastStartedAt = last.startedAt,
                        artistBrowseIds = first.artistBrowseIds
                    )
                }
                aggregatesDao.insertAll(aggregates)
            }
        }
    }

    private suspend fun pruneIfDue(now: Long) {
        val lastPrune = preferences.listeningPulseLastPruneMs()
        if (now - lastPrune >= PRUNE_INTERVAL_MS) {
            dao.prune(now - RETENTION_MS)
            preferences.setListeningPulseLastPruneMs(now)
        }
    }

    fun recordSync(track: Track, listenedMs: Long, completed: Boolean, startedAt: Long) {
        runCatching {
            runBlocking {
                withTimeout(RECORD_SYNC_TIMEOUT_MS) {
                    record(track, listenedMs, completed, startedAt)
                }
            }
        }.onFailure { Timber.w(it, "Timed out while flushing listen event") }
    }

    suspend fun eventsWindow(days: Int = RETENTION_DAYS): List<ListenEvent> = withContext(Dispatchers.IO) {
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        runCatching {
            ensureAggregatesBackfilled()
            dao.since(since).map { it.toListenEvent() }
        }
            .onFailure { Timber.w(it, "Listen events load failed") }
            .getOrDefault(emptyList())
    }

    suspend fun allTimeAggregates(): List<ListenAllTimeAggregateEntity> = withContext(Dispatchers.IO) {
        runCatching {
            ensureAggregatesBackfilled()
            aggregatesDao.all()
        }
            .onFailure { Timber.w(it, "All-time aggregates load failed") }
            .getOrDefault(emptyList())
    }

    suspend fun personalizedArtists(
        days: Int = PERSONALIZED_ARTIST_DAYS,
        limit: Int = PERSONALIZED_ARTIST_LIMIT
    ): List<PersonalizedArtistCandidate> = withContext(Dispatchers.IO) {
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.coerceAtLeast(1).toLong())
        runCatching {
            rankPersonalizedArtistCandidates(
                events = dao.since(since).map { it.toListenEvent() },
                limit = limit.coerceIn(1, 32)
            )
        }
            .onFailure { Timber.w(it, "Personalized artists load failed") }
            .getOrDefault(emptyList())
    }

    suspend fun recentTracks(limit: Int = RECENT_LIMIT): List<Track> = withContext(Dispatchers.IO) {
        runCatching {
            dao.latest(limit * OVERSCAN)
                .map { it.toTrack() }
                .distinctBy { it.id.ifBlank { "${it.title.lowercase()}|${it.artist.lowercase()}" } }
                .take(limit)
        }
            .onFailure { Timber.w(it, "Recent listens load failed") }
            .getOrDefault(emptyList())
    }

    suspend fun mostPlayedTracks(
        days: Int = MOST_PLAYED_DAYS,
        limit: Int = RECENT_LIMIT
    ): List<Track> = withContext(Dispatchers.IO) {
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.coerceAtLeast(1).toLong())
        try {
            rankMostPlayedTracks(
                listens = dao.since(since).map { event ->
                    SmartPlaylistListen(
                        track = event.toTrack(),
                        listenedMs = event.listenedMs,
                        startedAt = event.startedAt
                    )
                },
                limit = limit
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Most-played smart playlist load failed")
            emptyList()
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        runCatching {
            dao.clear()
            aggregatesDao.clear()
        }.onFailure { Timber.w(it, "Listen events clear failed") }
        Unit
    }

    private companion object {
        const val RETENTION_DAYS = 365
        const val RECENT_LIMIT = 40
        const val MOST_PLAYED_DAYS = 30
        const val PERSONALIZED_ARTIST_DAYS = 180
        const val PERSONALIZED_ARTIST_LIMIT = 16
        const val OVERSCAN = 4
        const val MAX_LOOPS = 6L
        val RETENTION_MS = TimeUnit.DAYS.toMillis(RETENTION_DAYS.toLong())
        val PRUNE_INTERVAL_MS = TimeUnit.HOURS.toMillis(24L)
        val RECORD_SYNC_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2L)
    }
}
