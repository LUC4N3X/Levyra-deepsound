package com.luc4n3x.levyra.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object AudioCache {
    @Volatile
    private var instance: SimpleCache? = null

    fun getInstance(context: Context): SimpleCache {
        return instance ?: synchronized(this) {
            instance ?: run {
                val audioCacheDir = File(context.applicationContext.cacheDir, "levyra_audio_cache")
                val evictor = LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024)
                SimpleCache(audioCacheDir, evictor, StandaloneDatabaseProvider(context.applicationContext)).also {
                    instance = it
                }
            }
        }
    }
}
