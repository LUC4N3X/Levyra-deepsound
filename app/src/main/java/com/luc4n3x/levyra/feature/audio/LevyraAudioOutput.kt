package com.luc4n3x.levyra.feature.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRouter
import android.media.MediaRouter2
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.security.MessageDigest
import java.util.Locale

data class LevyraAudioOutputRoute(
    val stableKey: String?,
    val displayName: String,
    val type: Int,
    val bluetooth: Boolean
) {
    val wired: Boolean get() = type in wiredOutputTypes
    val external: Boolean get() = type in externalOutputTypes
    val speaker: Boolean get() = type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
}

data class LevyraAudioOutputState(
    val active: LevyraAudioOutputRoute?,
    val connected: List<LevyraAudioOutputRoute>,
    val volumePercent: Int,
    val systemSwitcherAvailable: Boolean
)

internal fun selectLevyraAudioOutputRoute(
    routes: List<LevyraAudioOutputRoute>,
    systemOrdered: Boolean
): LevyraAudioOutputRoute? {
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

internal fun stableLevyraAudioRouteKey(type: Int, address: String?): String? {
    val cleanAddress = address?.trim()?.takeIf(String::isNotBlank) ?: return null
    val digest = MessageDigest.getInstance("SHA-256")
        .digest("$type:$cleanAddress".toByteArray(Charsets.UTF_8))
        .take(12)
        .joinToString("") { byte -> "%02x".format(Locale.ROOT, byte.toInt() and 0xff) }
    return "audio-$type-$digest"
}

@Composable
fun rememberLevyraAudioOutputState(): LevyraAudioOutputState {
    val context = LocalContext.current.applicationContext
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val mediaRouter = remember(context) { context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter }
    var state by remember(audioManager, mediaRouter) {
        mutableStateOf(queryLevyraAudioOutputState(audioManager, mediaRouter))
    }

    DisposableEffect(context, audioManager, mediaRouter) {
        val refresh = { state = queryLevyraAudioOutputState(audioManager, mediaRouter) }
        val deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()
        }
        val routeCallback = object : MediaRouter.SimpleCallback() {
            override fun onRouteSelected(router: MediaRouter, type: Int, info: MediaRouter.RouteInfo) = refresh()
            override fun onRouteUnselected(router: MediaRouter, type: Int, info: MediaRouter.RouteInfo) = refresh()
            override fun onRouteChanged(router: MediaRouter, info: MediaRouter.RouteInfo) = refresh()
        }
        val volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                val streamType = intent?.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, AudioManager.STREAM_MUSIC)
                    ?: AudioManager.STREAM_MUSIC
                if (streamType == AudioManager.STREAM_MUSIC) refresh()
            }
        }

        audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        mediaRouter.addCallback(
            MediaRouter.ROUTE_TYPE_LIVE_AUDIO,
            routeCallback,
            MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS
        )
        context.registerReceiver(volumeReceiver, IntentFilter(VOLUME_CHANGED_ACTION))

        onDispose {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
            mediaRouter.removeCallback(routeCallback)
            runCatching { context.unregisterReceiver(volumeReceiver) }
        }
    }
    return state
}

fun openLevyraSystemOutputSwitcher(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val shown = runCatching {
            MediaRouter2.getInstance(context).showSystemOutputSwitcher()
        }.getOrDefault(false)
        if (shown) return true
    }
    return runCatching {
        context.startActivity(
            Intent(Settings.ACTION_SOUND_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    }.getOrDefault(false)
}

private fun queryLevyraAudioOutputState(
    audioManager: AudioManager,
    mediaRouter: MediaRouter
): LevyraAudioOutputState {
    val systemOrdered = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val devices = queryOutputDevices(audioManager, systemOrdered)
    val routes = devices
        .filter(AudioDeviceInfo::isSink)
        .map(::toLevyraAudioOutputRoute)
        .distinctBy { it.stableKey ?: "${it.type}:${it.displayName.lowercase(Locale.ROOT)}" }
    val active = if (systemOrdered) {
        selectLevyraAudioOutputRoute(routes, systemOrdered = true)
    } else {
        val selected = mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
        resolveLevyraPreTiramisuAudioOutputRoute(routes, selected.name?.toString(), selected.deviceType)
    }
    val maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, maximum)
    return LevyraAudioOutputState(
        active = active,
        connected = routes,
        volumePercent = (current.toFloat() / maximum.toFloat() * 100f).toInt().coerceIn(0, 100),
        systemSwitcherAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    )
}

private fun queryOutputDevices(audioManager: AudioManager, systemOrdered: Boolean): List<AudioDeviceInfo> =
    runCatching {
        if (systemOrdered && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioManager.getAudioDevicesForAttributes(attributes)
        } else {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        }
    }.getOrDefault(emptyList())

internal fun resolveLevyraPreTiramisuAudioOutputRoute(
    routes: List<LevyraAudioOutputRoute>,
    selectedRouteName: String?,
    selectedDeviceType: Int
): LevyraAudioOutputRoute? {
    val cleanSelectedName = selectedRouteName?.trim()
    if (!cleanSelectedName.isNullOrBlank()) {
        routes.firstOrNull { route ->
            route.displayName.equals(cleanSelectedName, ignoreCase = true)
        }?.let { return it }
    }
    return when (selectedDeviceType) {
        MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH -> {
            val bluetoothRoutes = routes.filter { it.bluetooth }
            when (bluetoothRoutes.size) {
                1 -> bluetoothRoutes.single()
                else -> bluetoothRoutes.takeIf { it.isNotEmpty() }?.let {
                    LevyraAudioOutputRoute(
                        stableKey = null,
                        displayName = cleanSelectedName?.ifBlank { "Bluetooth" } ?: "Bluetooth",
                        type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        bluetooth = true
                    )
                }
            }
        }
        MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER ->
            routes.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        else -> {
            val bluetoothRoutes = routes.filter { it.bluetooth }
            if (bluetoothRoutes.size > 1) {
                selectLevyraAudioOutputRoute(routes.filter { !it.bluetooth }, systemOrdered = false)
            } else {
                selectLevyraAudioOutputRoute(routes, systemOrdered = false)
            }
        }
    }
}

private fun toLevyraAudioOutputRoute(device: AudioDeviceInfo): LevyraAudioOutputRoute {
    val name = device.productName.toString().trim()
    val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        runCatching { device.address }.getOrNull().orEmpty()
    } else {
        ""
    }
    return LevyraAudioOutputRoute(
        stableKey = stableLevyraAudioRouteKey(device.type, address),
        displayName = name.ifBlank { "Audio" },
        type = device.type,
        bluetooth = device.type in bluetoothOutputTypes
    )
}

private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"

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
