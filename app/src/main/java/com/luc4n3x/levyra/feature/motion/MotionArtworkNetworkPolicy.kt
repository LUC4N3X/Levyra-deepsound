package com.luc4n3x.levyra.feature.motion

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerManager

class MotionArtworkNetworkPolicy(context: Context) {
    private val appContext = context.applicationContext

    fun canResolveCurrent(): Boolean = currentNetworkState(appContext)?.let(::canResolve) == true

    fun canPrefetchNext(): Boolean = currentNetworkState(appContext)?.let(::canPrefetch) == true

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

        fun canUseMotionArtwork(context: Context): Boolean =
            currentNetworkState(context)?.let(::canResolve) == true

        internal fun canResolve(network: MotionArtworkNetworkState): Boolean =
            canResolve(wifiOnly = wifiOnly, network = network)

        internal fun canPrefetch(network: MotionArtworkNetworkState): Boolean = canResolve(network)

        private fun currentNetworkState(context: Context): MotionArtworkNetworkState? {
            val appContext = context.applicationContext
            val connectivity = appContext.getSystemService(ConnectivityManager::class.java) ?: return null
            val network = connectivity.activeNetwork ?: return null
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return null
            return MotionArtworkNetworkState(
                localAllowed = canAnimateLocally(appContext),
                dataSaverActive = connectivity.restrictBackgroundStatus ==
                    ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED,
                internet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                unmetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                wifiOrEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            )
        }

        internal fun canResolve(
            wifiOnly: Boolean,
            network: MotionArtworkNetworkState
        ): Boolean = network.localAllowed &&
            !network.dataSaverActive &&
            network.internet &&
            network.validated &&
            (!wifiOnly || (network.unmetered && network.wifiOrEthernet))
    }
}

internal data class MotionArtworkNetworkState(
    val localAllowed: Boolean,
    val dataSaverActive: Boolean,
    val internet: Boolean,
    val validated: Boolean,
    val unmetered: Boolean,
    val wifiOrEthernet: Boolean
)
