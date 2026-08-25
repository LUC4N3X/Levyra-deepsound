package com.luc4n3x.levyra.data

import android.content.Context
import androidx.room.withTransaction
import com.luc4n3x.levyra.data.local.ARTIST_ID_SEPARATOR
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.ListenEventEntity
import com.luc4n3x.levyra.data.local.toListenEvent
import com.luc4n3x.levyra.data.local.toListenEventEntity
import com.luc4n3x.levyra.data.local.toTrack
import com.luc4n3x.levyra.domain.LifetimeArtist
import com.luc4n3x.levyra.domain.LifetimeListening
import com.luc4n3x.levyra.domain.ListenEvent
import com.luc4n3x.levyra.domain.ListenIdentity
import com.luc4n3x.levyra.domain.ListenPlayPolicy
import com.luc4n3x.levyra.domain.PulseTrack
import com.luc4n3x.levyra.domain.PersonalizedArtistCandidate
import com.luc4n3x.levyra.domain.SmartPlaylistListen
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
    private val lifetimeDao = database.listenLifetimeDao()
    private val preferences = LevyraPreferences(context.applicationContext)
    private val writeLock = Mutex()

    suspend fun record(track: Track, listenedMs: Long, completed: Boolean, startedAt: Long) {
        if (!ListenPlayPolicy.isRecordableEvent(listenedMs) || track.title.isBlank()) return
        val cappedMs = if (track.durationMs > 0L) listenedMs.coerceAtMost(track.durationMs * MAX_LOOPS) else listenedMs
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                try {
                    val cleanTrack = track.copy(streamUrl = "", videoStreamUrl = "")
                    database.withTransaction {
                        applySessionSnapshot(cleanTrack, cappedMs, completed, startedAt)
                    }
                    pruneIfDue(System.currentTimeMillis())
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.w(error, "Listen event write failed")
                }
            }
        }
    }

    private suspend fun applySessionSnapshot(
        track: Track,
        listenedMs: Long,
        completed: Boolean,
        startedAt: Long
    ) {
        val existing = dao.findSession(track.id, startedAt)
        val previousMs = existing?.listenedMs ?: 0L
        val previousCompleted = existing?.completed == true
        val durationMs = existing?.durationMs?.takeIf { it > 0L } ?: track.durationMs
        val newMs = maxOf(previousMs, listenedMs)
        val newCompleted = previousCompleted || completed

        if (existing == null) {
            dao.insert(track.toListenEventEntity(newMs, newCompleted, startedAt))
        } else {
            dao.updateSession(
                trackId = track.id,
                startedAt = startedAt,
                listenedMs = newMs,
                completed = if (newCompleted) 1 else 0,
                artistBrowseIds = track.artistBrowseIds
                    .filter(String::isNotBlank)
                    .joinToString(ARTIST_ID_SEPARATOR)
            )
        }

        val previousCounted = existing != null &&
            ListenPlayPolicy.isCountedPlay(previousMs, durationMs, previousCompleted)
        val newCounted = ListenPlayPolicy.isCountedPlay(newMs, durationMs, newCompleted)
        addLifetimeDelta(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            listenedMsDelta = newMs - previousMs,
            playDelta = if (!previousCounted && newCounted) 1 else 0,
            completionDelta = if (!previousCompleted && newCompleted) 1 else 0,
            eventDelta = if (existing == null) 1 else 0,
            playedAt = startedAt
        )
    }

    private suspend fun addLifetimeDelta(
        trackId: String,
        title: String,
        artist: String,
        listenedMsDelta: Long,
        playDelta: Int,
        completionDelta: Int,
        eventDelta: Int,
        playedAt: Long
    ) {
        if (listenedMsDelta == 0L && playDelta == 0 && completionDelta == 0 && eventDelta == 0) return
        lifetimeDao.addTrackDelta(
            trackKey = ListenIdentity.trackKey(trackId, title, artist),
            trackId = trackId.trim(),
            title = title.trim(),
            artist = artist.trim(),
            listenedMs = listenedMsDelta,
            countedPlays = playDelta,
            completedCount = completionDelta,
            eventCount = eventDelta,
            playedAt = playedAt
        )
        val artistKey = ListenIdentity.artistKey(artist)
        if (artistKey.isNotEmpty()) {
            lifetimeDao.addArtistDelta(
                artistKey = artistKey,
                name = artist.trim(),
                listenedMs = listenedMsDelta,
                countedPlays = playDelta,
                completedCount = completionDelta,
                eventCount = eventDelta,
                playedAt = playedAt
            )
        }
    }

    suspend fun ensureLifetimeBackfill() {
        if (preferences.listeningLifetimeBackfillVersion() >= LIFETIME_BACKFILL_VERSION) return
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                if (preferences.listeningLifetimeBackfillVersion() >= LIFETIME_BACKFILL_VERSION) {
                    return@withLock
                }
                try {
                    database.withTransaction {
                        lifetimeDao.clearTracks()
                        lifetimeDao.clearArtists()
                        var afterId = 0L
                        var processed = 0
                        while (processed < BACKFILL_MAX_EVENTS) {
                            val page = dao.pageAfter(afterId, BACKFILL_PAGE_SIZE)
                            if (page.isEmpty()) break
                            page.forEach { entity -> backfillEvent(entity) }
                            afterId = page.last().id
                            processed += page.size
                            if (page.size < BACKFILL_PAGE_SIZE) break
                        }
                    }
                    preferences.setListeningLifetimeBackfillVersion(LIFETIME_BACKFILL_VERSION)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.w(error, "Lifetime listening backfill failed")
                }
            }
        }
    }

    private suspend fun backfillEvent(entity: ListenEventEntity) {
        if (!ListenPlayPolicy.isRecordableEvent(entity.listenedMs)) return
        val counted = ListenPlayPolicy.isCountedPlay(entity.listenedMs, entity.durationMs, entity.completed)
        addLifetimeDelta(
            trackId = entity.trackId,
            title = entity.title,
            artist = entity.artist,
            listenedMsDelta = entity.listenedMs,
            playDelta = if (counted) 1 else 0,
            completionDelta = if (entity.completed) 1 else 0,
            eventDelta = 1,
            playedAt = entity.startedAt
        )
    }

    suspend fun lifetime(): LifetimeListening = withContext(Dispatchers.IO) {
        try {
            val totals = lifetimeDao.trackTotals()
            LifetimeListening(
                totalListenMs = totals.listenedMs,
                countedPlays = totals.countedPlays,
                completedCount = totals.completedCount,
                eventCount = totals.eventCount,
                distinctTracks = totals.distinctTracks,
                distinctArtists = lifetimeDao.artistCount(),
                firstPlayedAt = totals.firstPlayedAt,
                lastPlayedAt = totals.lastPlayedAt,
                tracks = lifetimeDao.topTracks(LIFETIME_TOP_LIMIT).map { entity ->
                    PulseTrack(
                        trackId = entity.trackId,
                        title = entity.title,
                        artist = entity.artist,
                        plays = entity.countedPlays,
                        listenedMs = entity.listenedMs
                    )
                },
                artists = lifetimeDao.topArtists(LIFETIME_TOP_LIMIT).map { entity ->
                    LifetimeArtist(
                        name = entity.name,
                        countedPlays = entity.countedPlays,
                        listenedMs = entity.listenedMs
                    )
                }
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Lifetime listening load failed")
            LifetimeListening()
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
        runCatching { dao.since(since).map { it.toListenEvent() } }
            .onFailure { Timber.w(it, "Listen events load failed") }
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
        try {
            database.withTransaction {
                dao.clear()
                lifetimeDao.clearTracks()
                lifetimeDao.clearArtists()
            }
            preferences.setListeningLifetimeBackfillVersion(LIFETIME_BACKFILL_VERSION)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Listen events clear failed")
        }
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
        const val LIFETIME_BACKFILL_VERSION = 1
        const val LIFETIME_TOP_LIMIT = 8
        const val BACKFILL_PAGE_SIZE = 400
        const val BACKFILL_MAX_EVENTS = 50_000
        val RETENTION_MS = TimeUnit.DAYS.toMillis(RETENTION_DAYS.toLong())
        val PRUNE_INTERVAL_MS = TimeUnit.HOURS.toMillis(24L)
        val RECORD_SYNC_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2L)
    }
}
