package com.luc4n3x.levyra.ui.lyrics

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRouter
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.security.MessageDigest
import java.util.Locale

data class LyricsAudioOutputRoute(
    val stableKey: String?,
    val displayName: String,
    val type: Int,
    val bluetooth: Boolean
)

internal fun selectLyricsAudioOutputRoute(
    routes: List<LyricsAudioOutputRoute>,
    systemOrdered: Boolean
): LyricsAudioOutputRoute? {
    if (systemOrdered) return routes.firstOrNull()
    return routes.maxByOrNull { route ->
        when {
            route.bluetooth -> 4
            route.type in wiredOutputTypes -> 3
            route.type in externalOutputTypes -> 2
            route.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 1
            else -> 0
        }
    }
}

internal fun stableLyricsAudioRouteKey(type: Int, address: String): String? {
    val identity = address.trim().takeIf(String::isNotBlank) ?: return null
    val digest = MessageDigest.getInstance("SHA-256")
        .digest("$type:$identity".toByteArray(Charsets.UTF_8))
        .take(12)
        .joinToString("") { byte -> "%02x".format(Locale.ROOT, byte.toInt() and 0xff) }
    return "audio-$type-$digest"
}

@Composable
fun rememberLyricsAudioOutputRoute(): LyricsAudioOutputRoute? {
    val context = LocalContext.current.applicationContext
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val mediaRouter = remember(context) { context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter }
    var route by remember(audioManager, mediaRouter) {
        mutableStateOf(queryLyricsAudioOutputRoute(audioManager, mediaRouter))
    }

    DisposableEffect(audioManager, mediaRouter) {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                route = queryLyricsAudioOutputRoute(audioManager, mediaRouter)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                route = queryLyricsAudioOutputRoute(audioManager, mediaRouter)
            }
        }
        val routeCallback = object : MediaRouter.SimpleCallback() {
            override fun onRouteSelected(router: MediaRouter, type: Int, info: MediaRouter.RouteInfo) {
                route = queryLyricsAudioOutputRoute(audioManager, mediaRouter)
            }

            override fun onRouteUnselected(router: MediaRouter, type: Int, info: MediaRouter.RouteInfo) {
                route = queryLyricsAudioOutputRoute(audioManager, mediaRouter)
            }

            override fun onRouteChanged(router: MediaRouter, info: MediaRouter.RouteInfo) {
                route = queryLyricsAudioOutputRoute(audioManager, mediaRouter)
            }
        }
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        mediaRouter.addCallback(
            MediaRouter.ROUTE_TYPE_LIVE_AUDIO,
            routeCallback,
            MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS
        )
        onDispose {
            audioManager.unregisterAudioDeviceCallback(callback)
            mediaRouter.removeCallback(routeCallback)
        }
    }
    return route
}

private fun queryLyricsAudioOutputRoute(
    audioManager: AudioManager,
    mediaRouter: MediaRouter
): LyricsAudioOutputRoute? {
    val systemOrdered = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val devices = runCatching {
        if (systemOrdered) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioManager.getAudioDevicesForAttributes(attributes)
        } else {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        }
    }.getOrDefault(emptyList())
    val routes = devices.filter(AudioDeviceInfo::isSink).map(::toLyricsAudioOutputRoute)
    if (systemOrdered) return selectLyricsAudioOutputRoute(routes, systemOrdered = true)
    val selected = mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
    val selectedByName = routes.firstOrNull { route ->
        route.displayName.equals(selected.name.toString().trim(), ignoreCase = true)
    }
    if (selectedByName != null) return selectedByName
    return when (selected.deviceType) {
        MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH -> routes.firstOrNull { it.bluetooth }
        MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER -> routes.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }
        else -> selectLyricsAudioOutputRoute(routes, systemOrdered = false)
    }
}

private fun toLyricsAudioOutputRoute(device: AudioDeviceInfo): LyricsAudioOutputRoute {
    val name = device.productName.toString().trim()
    val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) device.address else ""
    return LyricsAudioOutputRoute(
        stableKey = stableLyricsAudioRouteKey(device.type, address),
        displayName = name,
        type = device.type,
        bluetooth = device.type in bluetoothOutputTypes
    )
}

private val bluetoothOutputTypes = setOf(
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_BLE_SPEAKER,
    AudioDeviceInfo.TYPE_BLE_BROADCAST
)

private val wiredOutputTypes = setOf(
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_LINE_ANALOG,
    AudioDeviceInfo.TYPE_LINE_DIGITAL
)

private val externalOutputTypes = setOf(
    AudioDeviceInfo.TYPE_USB_DEVICE,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_HDMI,
    AudioDeviceInfo.TYPE_HDMI_ARC,
    AudioDeviceInfo.TYPE_HDMI_EARC
)
