package com.luc4n3x.levyra.feature.providers

import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.YoutubeMusicPlaylistDetail
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.AlbumDetail
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

interface LevyraCatalogProvider {
    val id: String
    val priority: Int
    suspend fun searchEverything(query: String, languageCode: String): SearchResults
    suspend fun albumDetail(album: AlbumHit, languageCode: String): AlbumDetail?
    suspend fun playlist(playlistId: String, languageCode: String, limit: Int): YoutubeMusicPlaylistDetail?
}

interface LevyraPlaybackProvider {
    val id: String
    val priority: Int
    suspend fun resolve(track: Track, videoMode: Boolean): Track
    suspend fun resolveForOffline(track: Track): Track
}

data class LevyraProviderHealth(
    val providerId: String,
    val consecutiveFailures: Int,
    val totalSuccesses: Long,
    val totalFailures: Long,
    val averageLatencyMs: Long,
    val circuitOpenUntilMs: Long
)

private class ProviderHealthState {
    val consecutiveFailures = AtomicInteger(0)
    val totalSuccesses = AtomicLong(0L)
    val totalFailures = AtomicLong(0L)
    val latencyTotalMs = AtomicLong(0L)
    val latencySamples = AtomicLong(0L)
    val circuitOpenUntilMs = AtomicLong(0L)
}

class LevyraProviderHealthTracker(
    private val failureThreshold: Int = 3,
    private val cooldownMs: Long = 60_000L
) {
    private val states = ConcurrentHashMap<String, ProviderHealthState>()

    fun available(providerId: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        return state(providerId).circuitOpenUntilMs.get() <= nowMs
    }

    fun recordSuccess(providerId: String, latencyMs: Long) {
        val state = state(providerId)
        state.consecutiveFailures.set(0)
        state.totalSuccesses.incrementAndGet()
        state.latencyTotalMs.addAndGet(latencyMs.coerceAtLeast(0L))
        state.latencySamples.incrementAndGet()
        state.circuitOpenUntilMs.set(0L)
    }

    fun recordFailure(providerId: String) {
        val state = state(providerId)
        state.totalFailures.incrementAndGet()
        val failures = state.consecutiveFailures.incrementAndGet()
        if (failures >= failureThreshold) {
            state.circuitOpenUntilMs.set(System.currentTimeMillis() + cooldownMs)
        }
    }

    fun score(providerId: String, priority: Int): Long {
        val state = state(providerId)
        val samples = state.latencySamples.get()
        val latency = if (samples > 0L) state.latencyTotalMs.get() / samples else 750L
        val failurePenalty = state.consecutiveFailures.get().toLong() * 5_000L
        return priority.toLong() * 100_000L + latency + failurePenalty
    }

    fun snapshot(): List<LevyraProviderHealth> {
        return states.entries.map { (id, state) ->
            val samples = state.latencySamples.get()
            LevyraProviderHealth(
                providerId = id,
                consecutiveFailures = state.consecutiveFailures.get(),
                totalSuccesses = state.totalSuccesses.get(),
                totalFailures = state.totalFailures.get(),
                averageLatencyMs = if (samples > 0L) state.latencyTotalMs.get() / samples else 0L,
                circuitOpenUntilMs = state.circuitOpenUntilMs.get()
            )
        }.sortedBy { it.providerId }
    }

    private fun state(providerId: String): ProviderHealthState = states.getOrPut(providerId) { ProviderHealthState() }
}

class YoutubeMusicCatalogProvider(
    private val repository: YoutubeMusicRepository
) : LevyraCatalogProvider {
    override val id: String = "youtube_music"
    override val priority: Int = 10

    override suspend fun searchEverything(query: String, languageCode: String): SearchResults {
        return repository.searchEverything(query, languageCode)
    }

    override suspend fun albumDetail(album: AlbumHit, languageCode: String): AlbumDetail? {
        return repository.albumDetail(album, languageCode)
    }

    override suspend fun playlist(playlistId: String, languageCode: String, limit: Int): YoutubeMusicPlaylistDetail? {
        return repository.playlist(playlistId, languageCode, limit)
    }
}

class MemoryCatalogProvider(
    private val repository: YoutubeMusicRepository
) : LevyraCatalogProvider {
    override val id: String = "memory_catalog"
    override val priority: Int = 90

    override suspend fun searchEverything(query: String, languageCode: String): SearchResults {
        val tokens = normalize(query).split(' ').filter { it.length >= 2 }
        val tracks = repository.cachedTracks()
            .asSequence()
            .map { track -> track to relevance(track, tokens) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { it.id.ifBlank { "${it.title.lowercase()}|${it.artist.lowercase()}" } }
            .take(36)
            .toList()
        return SearchResults(topTrack = tracks.firstOrNull(), songs = tracks)
    }

    override suspend fun albumDetail(album: AlbumHit, languageCode: String): AlbumDetail? = null

    override suspend fun playlist(playlistId: String, languageCode: String, limit: Int): YoutubeMusicPlaylistDetail? = null

    private fun relevance(track: Track, tokens: List<String>): Int {
        if (tokens.isEmpty()) return 0
        val text = normalize("${track.title} ${track.artist} ${track.album}")
        return tokens.count { token -> text.contains(token) } * 10
    }

    private fun normalize(value: String): String {
        return value.lowercase()
            .replace(Regex("[^a-z0-9àèéìòóùçñäöüß\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

class CachedPlaybackProvider(
    private val resolver: PlaybackResolver
) : LevyraPlaybackProvider {
    override val id: String = "playback_cache"
    override val priority: Int = 0

    override suspend fun resolve(track: Track, videoMode: Boolean): Track {
        return resolver.cached(track, videoMode) ?: throw IOException("Stream non presente in cache")
    }

    override suspend fun resolveForOffline(track: Track): Track {
        return resolver.cached(track, false) ?: throw IOException("Stream offline non presente in cache")
    }
}

class LevyraNativePlaybackProvider(
    private val resolver: PlaybackResolver
) : LevyraPlaybackProvider {
    override val id: String = "levyra_native"
    override val priority: Int = 10

    override suspend fun resolve(track: Track, videoMode: Boolean): Track {
        return resolver.resolve(track, videoMode)
    }

    override suspend fun resolveForOffline(track: Track): Track {
        return resolver.resolveForOffline(track)
    }
}

class LevyraProviderRouter(
    catalogProviders: List<LevyraCatalogProvider>,
    playbackProviders: List<LevyraPlaybackProvider>,
    private val healthTracker: LevyraProviderHealthTracker = LevyraProviderHealthTracker(),
    private val catalogTimeoutMs: Long = 18_000L,
    private val playbackTimeoutMs: Long = 25_000L
) {
    private val catalogProviders = catalogProviders.distinctBy { it.id }
    private val playbackProviders = playbackProviders.distinctBy { it.id }

    suspend fun searchEverything(query: String, languageCode: String): SearchResults {
        return executeCatalog("search") { it.searchEverything(query, languageCode) }
    }

    suspend fun albumDetail(album: AlbumHit, languageCode: String): AlbumDetail? {
        var lastError: Throwable? = null
        for (provider in orderedCatalogProviders()) {
            val startedAt = System.nanoTime()
            try {
                val result = withTimeout(catalogTimeoutMs) { provider.albumDetail(album, languageCode) }
                if (result != null && result.tracks.isNotEmpty()) {
                    healthTracker.recordSuccess(provider.id, elapsedMs(startedAt))
                    return result
                }
                healthTracker.recordFailure(provider.id)
            } catch (error: Throwable) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                lastError = error
                healthTracker.recordFailure(provider.id)
            }
        }
        if (lastError is TimeoutCancellationException) throw IOException("Provider album lento", lastError)
        return null
    }

    suspend fun playlist(playlistId: String, languageCode: String, limit: Int = 150): YoutubeMusicPlaylistDetail? {
        var lastError: Throwable? = null
        for (provider in orderedCatalogProviders()) {
            val startedAt = System.nanoTime()
            try {
                val result = withTimeout(catalogTimeoutMs) { provider.playlist(playlistId, languageCode, limit) }
                if (result != null && result.tracks.isNotEmpty()) {
                    healthTracker.recordSuccess(provider.id, elapsedMs(startedAt))
                    return result
                }
                healthTracker.recordFailure(provider.id)
            } catch (error: Throwable) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                lastError = error
                healthTracker.recordFailure(provider.id)
            }
        }
        if (lastError is TimeoutCancellationException) throw IOException("Provider playlist lento", lastError)
        return null
    }

    suspend fun resolve(track: Track, videoMode: Boolean = false): Track {
        return executePlayback("playback") { it.resolve(track, videoMode) }
    }

    suspend fun resolveForOffline(track: Track): Track {
        return executePlayback("offline") { it.resolveForOffline(track) }
    }

    fun health(): List<LevyraProviderHealth> = healthTracker.snapshot()

    fun diagnostics(): String {
        return health().joinToString(" | ") { item ->
            val circuit = if (item.circuitOpenUntilMs > System.currentTimeMillis()) "open" else "closed"
            "${item.providerId}:ok=${item.totalSuccesses},fail=${item.totalFailures},lat=${item.averageLatencyMs}ms,circuit=$circuit"
        }
    }

    private suspend fun executeCatalog(operation: String, block: suspend (LevyraCatalogProvider) -> SearchResults): SearchResults {
        var lastError: Throwable? = null
        var emptyResult: SearchResults? = null
        for (provider in orderedCatalogProviders()) {
            val startedAt = System.nanoTime()
            try {
                val result = withTimeout(catalogTimeoutMs) { block(provider) }
                healthTracker.recordSuccess(provider.id, elapsedMs(startedAt))
                if (!result.isEmpty) return result
                if (emptyResult == null) emptyResult = result
            } catch (error: Throwable) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                lastError = error
                healthTracker.recordFailure(provider.id)
            }
        }
        emptyResult?.let { return it }
        throw IOException("Nessun provider disponibile per $operation", lastError)
    }

    private suspend fun executePlayback(operation: String, block: suspend (LevyraPlaybackProvider) -> Track): Track {
        var lastError: Throwable? = null
        for (provider in orderedPlaybackProviders()) {
            val startedAt = System.nanoTime()
            try {
                val result = withTimeout(playbackTimeoutMs) { block(provider) }
                if (result.streamUrl.isNotBlank()) {
                    healthTracker.recordSuccess(provider.id, elapsedMs(startedAt))
                    return result
                }
                healthTracker.recordFailure(provider.id)
            } catch (error: Throwable) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                lastError = error
                healthTracker.recordFailure(provider.id)
            }
        }
        throw IOException("Nessun provider disponibile per $operation", lastError)
    }

    private fun orderedCatalogProviders(): List<LevyraCatalogProvider> {
        val available = catalogProviders.filter { healthTracker.available(it.id) }
        val source = available.ifEmpty { catalogProviders }
        return source.sortedBy { healthTracker.score(it.id, it.priority) }
    }

    private fun orderedPlaybackProviders(): List<LevyraPlaybackProvider> {
        val available = playbackProviders.filter { healthTracker.available(it.id) }
        val source = available.ifEmpty { playbackProviders }
        return source.sortedBy { healthTracker.score(it.id, it.priority) }
    }

    private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt).coerceAtLeast(0L) / 1_000_000L
}
