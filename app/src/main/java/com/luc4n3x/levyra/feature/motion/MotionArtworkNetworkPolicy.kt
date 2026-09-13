package com.luc4n3x.levyra.feature.motion

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerManager

class MotionArtworkNetworkPolicy(context: Context) {
    private val appContext = context.applicationContext

    fun canResolveCurrent(): Boolean = canUseMotionArtwork(appContext)

    fun canPrefetchNext(): Boolean {
        if (!canUseMotionArtwork(appContext)) return false
        val connectivity = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    companion object {
        @Volatile
        private var wifiOnly: Boolean = false

        fun updateWifiOnly(value: Boolean) {
            wifiOnly = value
        }

        fun canAnimateLocally(context: Context): Boolean {
            val appContext = context.applicationContext
            val powerManager = appContext.getSystemService(PowerManager::class.java)
            val activityManager = appContext.getSystemService(ActivityManager::class.java)
            return powerManager?.isPowerSaveMode != true && activityManager?.isLowRamDevice != true
        }

        fun conditions(context: Context): MotionCanvasConditions {
            val appContext = context.applicationContext
            val powerManager = appContext.getSystemService(PowerManager::class.java)
            val activityManager = appContext.getSystemService(ActivityManager::class.java)
            val connectivity = appContext.getSystemService(ConnectivityManager::class.java)
            val capabilities = connectivity?.activeNetwork?.let(connectivity::getNetworkCapabilities)
            return MotionCanvasConditions(
                unmetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true,
                dataSaverActive = connectivity?.restrictBackgroundStatus ==
                    ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED,
                batterySaverActive = powerManager?.isPowerSaveMode == true,
                lowRamDevice = activityManager?.isLowRamDevice == true
            )
        }

        fun canUseMotionArtwork(context: Context): Boolean {
            val appContext = context.applicationContext
            val connectivity = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
            val network = connectivity.activeNetwork ?: return false
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
            return canResolve(
                wifiOnly = wifiOnly,
                network = MotionArtworkNetworkState(
                    localAllowed = canAnimateLocally(appContext),
                    dataSaverActive = connectivity.restrictBackgroundStatus ==
                        ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED,
                    internet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                    validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                    unmetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                )
            )
        }

        internal fun canResolve(
            wifiOnly: Boolean,
            network: MotionArtworkNetworkState
        ): Boolean = network.localAllowed &&
            !network.dataSaverActive &&
            network.internet &&
            network.validated &&
            (!wifiOnly || network.unmetered)
    }
}

internal data class MotionArtworkNetworkState(
    val localAllowed: Boolean,
    val dataSaverActive: Boolean,
    val internet: Boolean,
    val validated: Boolean,
    val unmetered: Boolean
)
