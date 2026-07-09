package com.luc4n3x.levyra

import android.app.Application
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.NewPipeRuntime
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.ReleaseRadarWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class LevyraApplication : Application() {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        LevyraArtworkCache.configure(this)
        warmPlaybackPipeline()
        startupScope.launch {
            delay(1800L)
            runCatching { ReleaseRadarWorker.schedule(this@LevyraApplication) }
                .onFailure { Timber.w(it, "Release radar scheduling failed") }
        }
    }

    private fun warmPlaybackPipeline() {
        startupScope.launch {
            runCatching { NewPipeRuntime.ensure() }
                .onFailure { Timber.w(it, "Extractor warmup failed") }
        }
        startupScope.launch {
            runCatching { PlaybackResolver.getInstance(this@LevyraApplication).warmNetwork() }
                .onFailure { Timber.w(it, "Network warmup failed") }
        }
    }
}
