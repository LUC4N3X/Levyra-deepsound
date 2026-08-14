package com.luc4n3x.levyra.player.offline

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.LevyraMediaCache
import com.luc4n3x.levyra.player.LevyraPlaybackCacheKey
import com.luc4n3x.levyra.player.LevyraYoutubeDataSource
import com.luc4n3x.levyra.player.PlaybackNetworkStack
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

internal fun offlineContinuousDownloadProgress(bytesCached: Long, contentLength: Long): Int {
    if (contentLength <= 0L) return 12
    val ratio = bytesCached.coerceIn(0L, contentLength).toDouble() / contentLength.toDouble()
    return (12 + ratio * 68.0).toInt().coerceIn(12, 80)
}

@UnstableApi
internal class OfflineStreamPrefetcher(context: Context) {
    private val appContext = context.applicationContext

    suspend fun cache(
        track: Track,
        progress: suspend (Int) -> Unit
    ) = coroutineScope {
        val url = track.streamUrl.trim()
        if (url.isBlank()) throw IOException("Stream audio non disponibile")

        val requestLength = AtomicLong(-1L)
        val cachedBytes = AtomicLong(0L)
        val finished = AtomicBoolean(false)
        val reporter = launch {
            var lastProgress = 11
            while (isActive && !finished.get()) {
                val nextProgress = offlineContinuousDownloadProgress(
                    bytesCached = cachedBytes.get(),
                    contentLength = requestLength.get()
                )
                if (nextProgress > lastProgress) {
                    lastProgress = nextProgress
                    progress(nextProgress)
                }
                delay(PROGRESS_POLL_MS)
            }
        }

        try {
            withContext(Dispatchers.IO) {
                val cache = LevyraMediaCache.get(appContext)
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
                    .setFragmentSize(CACHE_FRAGMENT_BYTES)
                val source = CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(upstream)
                    .setCacheWriteDataSinkFactory(sink)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    .createDataSource()
                val dataSpec = DataSpec.Builder()
                    .setUri(Uri.parse(url))
                    .setKey(LevyraPlaybackCacheKey.stream(track))
                    .setPosition(0L)
                    .build()
                val listener = CacheWriter.ProgressListener { length, bytes, _ ->
                    if (length > 0L) requestLength.set(length)
                    cachedBytes.set(bytes.coerceAtLeast(0L))
                }

                CacheWriter(
                    source,
                    dataSpec,
                    ByteArray(CACHE_BUFFER_BYTES),
                    listener
                ).cache()
            }
            finished.set(true)
            progress(80)
        } finally {
            finished.set(true)
            reporter.cancelAndJoin()
        }
    }

    private companion object {
        const val CACHE_FRAGMENT_BYTES = 2L * 1024L * 1024L
        const val CACHE_BUFFER_BYTES = 256 * 1024
        const val PROGRESS_POLL_MS = 150L
    }
}

@UnstableApi
internal class OfflineExportPipeline(
    context: Context,
    private val progress: suspend (Int) -> Unit,
    private val taskKey: String,
    private val settings: LevyraDownloadSettings,
    private val downloadQualityKey: String
) {
    private val appContext = context.applicationContext
    private val resolver = PlaybackResolver.getInstance(appContext)
    private val prefetcher = OfflineStreamPrefetcher(appContext)

    suspend fun export(track: Track): OfflineExportResult {
        var resolved = resolve(track)
        if (isMp4AudioExportUrl(resolved.streamUrl)) {
            try {
                prefetcher.cache(resolved, progress)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w(error, "Continuous offline prefetch failed; refreshing source")
                resolver.reportPlaybackFailure(
                    track = resolved,
                    isVideoMode = false,
                    reason = error.message.orEmpty().ifBlank { "continuous offline prefetch failed" }
                )
                resolved = resolve(track)
                if (isMp4AudioExportUrl(resolved.streamUrl)) {
                    prefetcher.cache(resolved, progress)
                }
            }
        }

        return OfflineAudioExporter(
            context = appContext,
            resolver = resolver,
            progress = progress,
            taskKey = taskKey,
            settings = settings,
            downloadQualityKey = downloadQualityKey
        ).export(resolved)
    }

    private suspend fun resolve(track: Track): Track {
        progress(4)
        val resolved = resolver.resolveForOffline(
            track = track.copy(streamUrl = ""),
            preferredAudioQuality = settings.resolverAudioQuality
        )
        if (resolved.streamUrl.isBlank()) throw IOException("Stream audio non disponibile")
        return resolved
    }
}
