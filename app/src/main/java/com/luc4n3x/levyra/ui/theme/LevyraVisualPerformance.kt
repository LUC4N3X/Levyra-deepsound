package com.luc4n3x.levyra.ui.theme

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.luc4n3x.levyra.domain.LevyraVisualPerformance

@Immutable
data class LevyraVisualCapabilities(
    val decorativeMotion: Boolean,
    val depthTransitions: Boolean,
    val microMotion: Boolean,
    val heavyBlur: Boolean
) {
    companion object {
        val Full = LevyraVisualCapabilities(
            decorativeMotion = true,
            depthTransitions = true,
            microMotion = true,
            heavyBlur = true
        )
    }
}

val LocalLevyraVisualCapabilities = staticCompositionLocalOf { LevyraVisualCapabilities.Full }

fun resolveVisualPerformance(
    selected: LevyraVisualPerformance,
    lowRamDevice: Boolean,
    powerSaveMode: Boolean
): LevyraVisualPerformance = when (selected) {
    LevyraVisualPerformance.Full -> LevyraVisualPerformance.Full
    LevyraVisualPerformance.Smooth -> LevyraVisualPerformance.Smooth
    LevyraVisualPerformance.Auto -> if (lowRamDevice || powerSaveMode) {
        LevyraVisualPerformance.Smooth
    } else {
        LevyraVisualPerformance.Full
    }
}

fun resolveVisualCapabilities(
    selected: LevyraVisualPerformance,
    animationsEnabled: Boolean,
    lowRamDevice: Boolean,
    powerSaveMode: Boolean
): LevyraVisualCapabilities {
    val full = resolveVisualPerformance(selected, lowRamDevice, powerSaveMode) == LevyraVisualPerformance.Full
    val motion = animationsEnabled && full
    return LevyraVisualCapabilities(
        decorativeMotion = motion,
        depthTransitions = motion,
        microMotion = motion,
        heavyBlur = full
    )
}

@Composable
fun rememberLevyraVisualCapabilities(
    selected: LevyraVisualPerformance,
    animationsEnabled: Boolean
): LevyraVisualCapabilities {
    val context = LocalContext.current.applicationContext
    val lowRamDevice = remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val powerSaveMode = rememberPowerSaveMode()
    return remember(selected, animationsEnabled, lowRamDevice, powerSaveMode) {
        resolveVisualCapabilities(selected, animationsEnabled, lowRamDevice, powerSaveMode)
    }
}

@Composable
fun rememberPowerSaveMode(): Boolean {
    val context = LocalContext.current.applicationContext
    val powerManager = remember(context) { context.getSystemService(PowerManager::class.java) }
    var powerSave by remember(powerManager) { mutableStateOf(powerManager?.isPowerSaveMode == true) }
    DisposableEffect(context, powerManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                powerSave = powerManager?.isPowerSaveMode == true
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        powerSave = powerManager?.isPowerSaveMode == true
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return powerSave
}
