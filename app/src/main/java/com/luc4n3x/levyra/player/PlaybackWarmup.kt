package com.luc4n3x.levyra.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class PlaybackWarmup(context: Context) {
    private val appContext = context.applicationContext
    private val okHttpClient by lazy { LevyraHttpClientFactory.media(appContext) }

    suspend fun prime(track: Track, bytes: Long = DEFAULT_PRIME_BYTES): Boolean = withContext(Dispatchers.IO) {
        if (track.streamUrl.isBlank()) return@withContext false
        runCatching {
            val cache = LevyraMediaCache.get(appContext)
            val upstream = OkHttpDataSource.Factory(okHttpClient)
                .setUserAgent("LevyraPlayer/1.13 Android Music")
                .setDefaultRequestProperties(
                    mapOf(
                        "Accept" to "*/*",
                        "Accept-Encoding" to "identity",
                        "Connection" to "keep-alive"
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
                .setUri(Uri.parse(track.streamUrl))
                .setKey(LevyraPlaybackCacheKey.stream(track))
                .setPosition(0L)
                .setLength(bytes.coerceIn(MIN_PRIME_BYTES, MAX_PRIME_BYTES))
                .build()
            CacheWriter(source, dataSpec, ByteArray(64 * 1024), null).cache()
            true
        }.getOrDefault(false)
    }

    companion object {
        private const val MIN_PRIME_BYTES = 96L * 1024L
        private const val DEFAULT_PRIME_BYTES = 512L * 1024L
        private const val MAX_PRIME_BYTES = 1L * 1024L * 1024L
        private const val PRIME_FRAGMENT_BYTES = 256L * 1024L
    }
}
