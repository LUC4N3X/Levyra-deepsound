package com.luc4n3x.levyra.player

import android.app.ActivityManager
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object LevyraMediaCache {
    private const val LOW_RAM_MAX_BYTES = 160L * 1024L * 1024L
    private const val DEFAULT_MAX_BYTES = 384L * 1024L * 1024L
    private const val MIN_MAX_BYTES = 64L * 1024L * 1024L

    @Volatile
    private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache {
        val appContext = context.applicationContext
        return cache ?: synchronized(this) {
            cache ?: create(appContext).also { cache = it }
        }
    }

    private fun create(context: Context): SimpleCache {
        val directory = File(context.cacheDir, "levyra_media_cache")
        val maxBytes = maxCacheBytes(context, directory)
        return runCatching {
            createInstance(context, directory, maxBytes)
        }.getOrElse {
            directory.deleteRecursively()
            directory.mkdirs()
            createInstance(context, directory, maxBytes)
        }
    }

    private fun createInstance(context: Context, directory: File, maxBytes: Long): SimpleCache {
        return SimpleCache(
            directory,
            LeastRecentlyUsedCacheEvictor(maxBytes),
            StandaloneDatabaseProvider(context)
        )
    }

    private fun maxCacheBytes(context: Context, directory: File): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val base = if (activityManager.isLowRamDevice) LOW_RAM_MAX_BYTES else DEFAULT_MAX_BYTES
        val availableBudget = (directory.parentFile?.usableSpace ?: context.cacheDir.usableSpace) / 8L
        return availableBudget.coerceIn(MIN_MAX_BYTES, base)
    }
}
