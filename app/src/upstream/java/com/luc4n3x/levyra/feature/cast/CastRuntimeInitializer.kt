@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.luc4n3x.levyra.feature.cast

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.media3.cast.Cast
import com.google.android.gms.cast.framework.CastContext
import timber.log.Timber

object CastRuntimeInitializer {
    private const val PLAY_SERVICES_PACKAGE = "com.google.android.gms"
    private const val BACKGROUND_INIT_MIN_PLAY_SERVICES = 222_000_000L

    @Volatile
    var available: Boolean = false
        private set

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        available = runCatching {
            val cast = Cast.getSingletonInstance(appContext)
            if (cast.needsInitialization()) {
                if (requiresMainThreadCastContext(playServicesVersion(appContext))) {
                    val main = ContextCompat.getMainExecutor(appContext)
                    CastContext.getSharedInstance(appContext, main)
                        .addOnSuccessListener(main) { initializeMedia3(cast) }
                        .addOnFailureListener(main) { Timber.w(it, "Cast context unavailable on this Play services build") }
                } else {
                    cast.initialize()
                }
            }
            true
        }.onFailure {
            Timber.w(it, "Media3 Cast initialization failed; Cast will stay unavailable")
        }.getOrDefault(false)
    }

    fun castOrNull(context: Context): Cast? =
        if (available) runCatching { Cast.getSingletonInstance(context.applicationContext) }.getOrNull() else null

    internal fun requiresMainThreadCastContext(playServicesVersion: Long?): Boolean =
        playServicesVersion == null || playServicesVersion < BACKGROUND_INIT_MIN_PLAY_SERVICES

    private fun initializeMedia3(cast: Cast) {
        runCatching { if (cast.needsInitialization()) cast.initialize() }
            .onFailure { Timber.w(it, "Media3 Cast initialization failed after context creation") }
    }

    private fun playServicesVersion(context: Context): Long? = runCatching {
        val info = context.packageManager.getPackageInfo(PLAY_SERVICES_PACKAGE, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }.getOrNull()
}
