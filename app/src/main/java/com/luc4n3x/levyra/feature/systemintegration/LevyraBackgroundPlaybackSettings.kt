package com.luc4n3x.levyra.feature.systemintegration

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

internal fun levyraBackgroundSettingsComponents(family: LevyraDeviceFamily): List<ComponentName> = when (family) {
    LevyraDeviceFamily.XIAOMI -> listOf(
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
    )
    LevyraDeviceFamily.OPLUS -> listOf(
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
    )
    LevyraDeviceFamily.VIVO -> listOf(
        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
        ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")
    )
    LevyraDeviceFamily.SAMSUNG -> listOf(
        ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")
    )
    LevyraDeviceFamily.HONOR -> listOf(
        ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
    )
    LevyraDeviceFamily.GOOGLE,
    LevyraDeviceFamily.GENERIC -> emptyList()
}

internal fun openLevyraBackgroundPlaybackSettings(context: Context, family: LevyraDeviceFamily): Boolean {
    val candidates = levyraBackgroundSettingsComponents(family).map { component ->
        Intent().setComponent(component)
    } + Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    return candidates.any { intent ->
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
