package com.luc4n3x.levyra

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.view.Display

internal fun Activity.preferHighestRefreshRate() {
    val display = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else null)
        ?: @Suppress("DEPRECATION") windowManager.defaultDisplay
        ?: return
    val powerSave = (getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
    val target = if (powerSave) 0 else highestRefreshModeId(display) ?: return
    val attributes = window.attributes
    if (attributes.preferredDisplayModeId == target) return
    attributes.preferredDisplayModeId = target
    window.attributes = attributes
}

private fun highestRefreshModeId(display: Display): Int? {
    val current = display.mode
    return display.supportedModes
        .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
        .maxByOrNull { it.refreshRate }
        ?.modeId
}
