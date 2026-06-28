package com.luc4n3x.levyra.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
object LevyraMediaCache {
    private const val MAX_BYTES = 384L * 1024L * 1024L

    @Volatile
    private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache {
        return cache ?: synchronized(this) {
            cache ?: SimpleCache(
                File(context.cacheDir, "levyra_media_cache"),
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                StandaloneDatabaseProvider(context)
            ).also { cache = it }
        }
    }

    fun release() {
        synchronized(this) {
            cache?.release()
            cache = null
        }
    }
}
