package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.toListenEvent
import com.luc4n3x.levyra.data.local.toListenEventEntity
import com.luc4n3x.levyra.data.local.toTrack
import com.luc4n3x.levyra.domain.ListenEvent
import com.luc4n3x.levyra.domain.ListeningPulseEngine
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ListeningPulseStore(context: Context) {
    private val dao = LevyraDatabase.get(context.applicationContext).listenEventsDao()
    private val preferences = LevyraPreferences(context.applicationContext)
    private val writeLock = Mutex()

    suspend fun record(track: Track, listenedMs: Long, completed: Boolean, startedAt: Long) {
        if (listenedMs < ListeningPulseEngine.MIN_LISTEN_MS || track.title.isBlank()) return
        val cappedMs = if (track.durationMs > 0L) listenedMs.coerceAtMost(track.durationMs * MAX_LOOPS) else listenedMs
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                runCatching {
                    val cleanTrack = track.copy(streamUrl = "", videoStreamUrl = "")
                    val updated = dao.updateSession(cleanTrack.id, startedAt, cappedMs, if (completed) 1 else 0)
                    if (updated == 0) {
                        dao.insert(cleanTrack.toListenEventEntity(cappedMs, completed, startedAt))
                    }
                    pruneIfDue(System.currentTimeMillis())
                }.onFailure { Timber.w(it, "Listen event write failed") }
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
        runCatching { dao.since(since).map { it.toListenEvent() } }
            .onFailure { Timber.w(it, "Listen events load failed") }
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

    suspend fun clear() = withContext(Dispatchers.IO) {
        runCatching { dao.clear() }.onFailure { Timber.w(it, "Listen events clear failed") }
        Unit
    }

    private companion object {
        const val RETENTION_DAYS = 365
        const val RECENT_LIMIT = 40
        const val OVERSCAN = 4
        const val MAX_LOOPS = 6L
        val RETENTION_MS = TimeUnit.DAYS.toMillis(RETENTION_DAYS.toLong())
        val PRUNE_INTERVAL_MS = TimeUnit.HOURS.toMillis(24L)
        val RECORD_SYNC_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2L)
    }
}
