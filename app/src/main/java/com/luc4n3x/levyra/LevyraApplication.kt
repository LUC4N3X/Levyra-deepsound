package com.luc4n3x.levyra

import android.app.Application
import timber.log.Timber

class LevyraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
