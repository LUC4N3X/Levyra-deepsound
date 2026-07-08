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
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ListeningPulseStore(context: Context) {
    private val dao = LevyraDatabase.get(context.applicationContext).listenEventsDao()
    private val writesSincePrune = AtomicInteger(0)

    suspend fun record(track: Track, listenedMs: Long, completed: Boolean, startedAt: Long) {
        if (listenedMs < ListeningPulseEngine.MIN_LISTEN_MS || track.title.isBlank()) return
        val cappedMs = if (track.durationMs > 0L) listenedMs.coerceAtMost(track.durationMs * MAX_LOOPS) else listenedMs
        withContext(Dispatchers.IO) {
            runCatching {
                dao.insert(
                    track.copy(streamUrl = "", videoStreamUrl = "")
                        .toListenEventEntity(cappedMs, completed, startedAt)
                )
                if (writesSincePrune.incrementAndGet() >= PRUNE_EVERY) {
                    writesSincePrune.set(0)
                    dao.prune(System.currentTimeMillis() - RETENTION_MS)
                }
            }.onFailure { Timber.w(it, "Listen event write failed") }
        }
    }

    fun recordSync(track: Track, listenedMs: Long, completed: Boolean, startedAt: Long) {
        runBlocking { record(track, listenedMs, completed, startedAt) }
    }

    suspend fun eventsWindow(days: Int = WINDOW_DAYS): List<ListenEvent> = withContext(Dispatchers.IO) {
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
        const val WINDOW_DAYS = 90
        const val RECENT_LIMIT = 20
        const val OVERSCAN = 4
        const val MAX_LOOPS = 6L
        const val PRUNE_EVERY = 40
        val RETENTION_MS = TimeUnit.DAYS.toMillis(365L)
    }
}
