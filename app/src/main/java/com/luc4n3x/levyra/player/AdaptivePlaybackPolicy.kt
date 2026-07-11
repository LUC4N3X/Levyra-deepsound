package com.luc4n3x.levyra.player

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import java.util.Locale

class AdaptivePlaybackPolicy(context: Context) {
    private val appContext = context.applicationContext
    private val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun current(videoMode: Boolean): PlaybackPrefetchPlan {
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val lowRam = activityManager.isLowRamDevice || memoryInfo.lowMemory
        val manufacturer = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase(Locale.ROOT)
        val aggressiveOem = isAggressiveOem(manufacturer)
        val battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 } ?: 100
        val charging = batteryManager.isCharging
        val powerSave = powerManager.isPowerSaveMode
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
        val unmetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
        val fastTransport = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true || capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        val constrained = lowRam || powerSave || (!charging && battery <= 20)
        val resolveCount = when {
            videoMode -> 1
            constrained -> 1
            !unmetered -> 2
            else -> 3
        }
        val primeCount = when {
            videoMode -> 1
            constrained -> 1
            else -> 1
        }
        val primeBytes = when {
            videoMode && constrained -> 192L * 1024L
            videoMode -> 384L * 1024L
            constrained -> 160L * 1024L
            fastTransport && charging -> 640L * 1024L
            unmetered -> 448L * 1024L
            else -> 256L * 1024L
        }
        val concurrency = when {
            constrained -> 1
            aggressiveOem && !charging -> 1
            unmetered && fastTransport -> 2
            else -> 1
        }
        return PlaybackPrefetchPlan(
            resolveCount = resolveCount,
            primeCount = primeCount,
            concurrency = concurrency,
            primeBytes = primeBytes,
            staggerMs = if (constrained) 120L else if (unmetered) 30L else 70L,
            lowRam = lowRam,
            powerConstrained = constrained,
            manufacturer = manufacturer
        )
    }

    fun serviceBuffers(): PlaybackBufferProfile {
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val lowRam = activityManager.isLowRamDevice || memoryInfo.lowMemory
        val manufacturer = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase(Locale.ROOT)
        val aggressiveOem = isAggressiveOem(manufacturer)
        return when {
            lowRam -> PlaybackBufferProfile(600, 12_000, 100, 250, 0)
            aggressiveOem -> PlaybackBufferProfile(800, 20_000, 100, 300, 2_000)
            else -> PlaybackBufferProfile(900, 24_000, 100, 300, 4_000)
        }
    }

    private fun isAggressiveOem(value: String): Boolean {
        return listOf("xiaomi", "redmi", "poco", "honor", "huawei", "samsung").any(value::contains)
    }
}

data class PlaybackPrefetchPlan(
    val resolveCount: Int,
    val primeCount: Int,
    val concurrency: Int,
    val primeBytes: Long,
    val staggerMs: Long,
    val lowRam: Boolean,
    val powerConstrained: Boolean,
    val manufacturer: String
)

data class PlaybackBufferProfile(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val playbackBufferMs: Int,
    val rebufferMs: Int,
    val backBufferMs: Int
)
