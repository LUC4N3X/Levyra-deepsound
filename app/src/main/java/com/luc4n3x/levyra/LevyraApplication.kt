package com.luc4n3x.levyra

import android.app.Application
import android.content.ComponentCallbacks2
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.LevyraArtworkStartupMetrics
import com.luc4n3x.levyra.data.NewPipeRuntime
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.YoutubeLocalDecoder
import com.luc4n3x.levyra.data.ReleaseRadarWorker
import com.luc4n3x.levyra.data.AutomaticBackupScheduler
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.network.LevyraNetworkController
import com.luc4n3x.levyra.feature.cast.CastRuntimeInitializer
import com.luc4n3x.levyra.player.PlaybackNetworkStack
import com.luc4n3x.levyra.runtime.RuntimeHooks
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
        CastRuntimeInitializer.initialize(this)
        RuntimeHooks.start(this)
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        LevyraArtworkStartupMetrics.beginSession()
        runCatching { LevyraNetworkController.applyStoredConfiguration(this) }
            .onFailure { Timber.w(it, "Network configuration bootstrap failed") }
        LevyraArtworkCache.configure(this)
        YoutubeLocalDecoder.install(this)
        PlaybackNetworkStack.initialize(this)
        runCatching { NewPipeRuntime.ensure(this) }
            .onFailure { Timber.w(it, "Extractor initialization failed") }
        warmPlaybackPipeline()
        startupScope.launch {
            delay(1800L)
            runCatching {
                if (com.luc4n3x.levyra.data.FollowedArtistsStore(this@LevyraApplication).load().isEmpty()) {
                    ReleaseRadarWorker.cancel(this@LevyraApplication)
                } else {
                    ReleaseRadarWorker.schedule(this@LevyraApplication)
                }
            }
                .onFailure { Timber.w(it, "Release radar scheduling failed") }
            runCatching {
                AutomaticBackupScheduler.schedule(
                    this@LevyraApplication,
                    LevyraPreferences(this@LevyraApplication).backupSettings()
                )
            }.onFailure { Timber.w(it, "Automatic backup scheduling failed") }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (shouldReleaseMemory(level)) YoutubeLocalDecoder.trimMemory()
    }

    private fun shouldReleaseMemory(level: Int): Boolean =
        level == RUNNING_LOW_LEVEL ||
            level == RUNNING_CRITICAL_LEVEL ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND

    private companion object {
        const val RUNNING_LOW_LEVEL = 10
        const val RUNNING_CRITICAL_LEVEL = 15
    }

    private fun warmPlaybackPipeline() {
        startupScope.launch {
            runCatching { PlaybackResolver.getInstance(this@LevyraApplication).warmNetwork() }
                .onFailure { Timber.w(it, "Network warmup failed") }
        }
        startupScope.launch {
            delay(2500L)
            runCatching { YoutubeLocalDecoder.prewarm() }
                .onFailure { Timber.w(it, "Local decoder prewarm failed") }
        }
    }
}
