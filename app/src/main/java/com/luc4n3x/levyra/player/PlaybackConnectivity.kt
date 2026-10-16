package com.luc4n3x.levyra.player

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

internal fun hasUsableInternet(capabilities: NetworkCapabilities?): Boolean =
    capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

internal suspend fun ConnectivityManager.awaitInternetNetwork(timeoutMs: Long): Boolean? {
    val available = CompletableDeferred<Unit>()
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            if (hasUsableInternet(networkCapabilities)) available.complete(Unit)
        }
    }
    try {
        registerDefaultNetworkCallback(callback)
    } catch (error: RuntimeException) {
        Timber.w(error, "Playback connectivity callback unavailable")
        return null
    }
    return try {
        withTimeoutOrNull(timeoutMs) { available.await() } != null
    } finally {
        runCatching { unregisterNetworkCallback(callback) }
    }
}
