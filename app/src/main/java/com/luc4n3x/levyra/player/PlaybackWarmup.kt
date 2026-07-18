package com.luc4n3x.levyra.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.luc4n3x.levyra.domain.Track
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

@UnstableApi
class PlaybackWarmup(context: Context) {
    private val appContext = context.applicationContext
    private val primeLocks = ConcurrentHashMap<String, Mutex>()

    suspend fun prime(track: Track, bytes: Long = DEFAULT_PRIME_BYTES): Boolean = primeUrl(
        url = track.streamUrl,
        cacheKey = LevyraPlaybackCacheKey.stream(track),
        bytes = bytes
    )

    suspend fun primeVideo(track: Track): Boolean = coroutineScope {
        val jobs = buildList {
            if (track.streamUrl.isNotBlank()) {
                add(async { prime(track, VIDEO_AUDIO_PRIME_BYTES) })
            }
            if (track.videoStreamUrl.isNotBlank()) {
                add(async { primeUrl(track.videoStreamUrl, LevyraPlaybackCacheKey.video(track), VIDEO_PRIME_BYTES) })
            }
        }
        if (jobs.isEmpty()) false else jobs.awaitAll().any { it }
    }

    private suspend fun primeUrl(url: String, cacheKey: String, bytes: Long): Boolean = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext false
        val lock = primeLocks.computeIfAbsent(cacheKey) { Mutex() }
        try {
            lock.withLock {
                runCatching {
                    val requestedBytes = bytes.coerceIn(MIN_PRIME_BYTES, MAX_PRIME_BYTES)
                    val cache = LevyraMediaCache.get(appContext)
                    if (cache.isCached(cacheKey, 0L, requestedBytes)) {
                        return@runCatching true
                    }
                    val upstream = LevyraYoutubeDataSource.Factory(
                        PlaybackNetworkStack.warmupFactory(appContext)
                            .setDefaultRequestProperties(
                                mapOf(
                                    "Accept" to "*/*",
                                    "Accept-Encoding" to "identity"
                                )
                            )
                    )
                    val sink = CacheDataSink.Factory()
                        .setCache(cache)
                        .setFragmentSize(PRIME_FRAGMENT_BYTES)
                    val source = CacheDataSource.Factory()
                        .setCache(cache)
                        .setUpstreamDataSourceFactory(upstream)
                        .setCacheWriteDataSinkFactory(sink)
                        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                        .createDataSource()
                    val dataSpec = DataSpec.Builder()
                        .setUri(Uri.parse(url))
                        .setKey(cacheKey)
                        .setPosition(0L)
                        .setLength(requestedBytes)
                        .build()
                    CacheWriter(source, dataSpec, ByteArray(PRIME_BUFFER_BYTES), null).cache()
                    Timber.d("warmup cached bytes=%d key=%s", requestedBytes, cacheKey)
                    true
                }.onFailure { Timber.d(it, "warmup skipped key=%s", cacheKey) }.getOrDefault(false)
            }
        } finally {
            primeLocks.remove(cacheKey, lock)
        }
    }

    companion object {
        private const val MIN_PRIME_BYTES = 64L * 1024L
        private const val DEFAULT_PRIME_BYTES = 384L * 1024L
        private const val VIDEO_AUDIO_PRIME_BYTES = 256L * 1024L
        private const val VIDEO_PRIME_BYTES = 1024L * 1024L
        private const val MAX_PRIME_BYTES = 1536L * 1024L
        private const val PRIME_FRAGMENT_BYTES = 256L * 1024L
        private const val PRIME_BUFFER_BYTES = 128 * 1024
    }
}
