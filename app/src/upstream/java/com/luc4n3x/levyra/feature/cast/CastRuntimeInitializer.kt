@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.luc4n3x.levyra.feature.cast

import android.content.Context
import androidx.media3.cast.Cast
import timber.log.Timber

object CastRuntimeInitializer {
    @Volatile
    var available: Boolean = false
        private set

    fun initialize(context: Context) {
        available = runCatching {
            Cast.getSingletonInstance(context.applicationContext).apply {
                if (needsInitialization()) initialize()
            }
            true
        }.onFailure {
            Timber.w(it, "Media3 Cast initialization failed; Cast will stay unavailable")
        }.getOrDefault(false)
    }
}
