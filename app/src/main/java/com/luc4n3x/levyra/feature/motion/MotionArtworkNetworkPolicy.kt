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
        fun canUseMotionArtwork(context: Context): Boolean {
            val appContext = context.applicationContext
            val connectivity = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
            val powerManager = appContext.getSystemService(PowerManager::class.java)
            val activityManager = appContext.getSystemService(ActivityManager::class.java)
            if (powerManager?.isPowerSaveMode == true || activityManager?.isLowRamDevice == true) return false
            if (connectivity.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED) return false
            val network = connectivity.activeNetwork ?: return false
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }
}
