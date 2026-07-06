package com.luc4n3x.levyra

import android.app.Application
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.ReleaseRadarWorker
import timber.log.Timber

class LevyraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        LevyraArtworkCache.configure(this)
        runCatching { ReleaseRadarWorker.schedule(this) }
            .onFailure { Timber.w(it, "Release radar scheduling failed") }
    }
}
