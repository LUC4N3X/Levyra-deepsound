package com.luc4n3x.levyra.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.luc4n3x.levyra.feature.systemintegration.LevyraDeviceFamily
import com.luc4n3x.levyra.feature.systemintegration.LevyraDeviceFeature
import com.luc4n3x.levyra.feature.systemintegration.LevyraDeviceProfile
import com.luc4n3x.levyra.feature.systemintegration.LevyraDeviceProfiles
import com.luc4n3x.levyra.feature.systemintegration.openLevyraBackgroundPlaybackSettings
import com.luc4n3x.levyra.ui.i18n.DeviceIntegrationCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.deviceIntegrationCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class DeviceIntegrationStatus {
    ACTIVE,
    AUTOMATIC,
    NOTIFICATIONS_OFF,
    UNAVAILABLE
}

internal data class DeviceIntegrationRow(
    val name: String,
    val detail: String,
    val status: DeviceIntegrationStatus
)

internal fun deviceIntegrationRows(
    profile: LevyraDeviceProfile,
    notificationsEnabled: Boolean,
    copy: DeviceIntegrationCopy
): List<DeviceIntegrationRow> = buildList {
    add(DeviceIntegrationRow("Android", copy.standardDetail, DeviceIntegrationStatus.ACTIVE))
    fun notificationFeature(feature: LevyraDeviceFeature): DeviceIntegrationStatus = when {
        !profile.supports(feature) -> DeviceIntegrationStatus.UNAVAILABLE
        notificationsEnabled -> DeviceIntegrationStatus.ACTIVE
        else -> DeviceIntegrationStatus.NOTIFICATIONS_OFF
    }
    when (profile.family) {
        LevyraDeviceFamily.XIAOMI -> add(
            DeviceIntegrationRow(
                "HyperOS Super Island",
                copy.superIslandDetail,
                notificationFeature(LevyraDeviceFeature.SUPER_ISLAND)
            )
        )
        LevyraDeviceFamily.VIVO -> add(
            DeviceIntegrationRow(
                "OriginOS Atomic Island",
                copy.atomicIslandDetail,
                notificationFeature(LevyraDeviceFeature.ATOMIC_ISLAND)
            )
        )
        LevyraDeviceFamily.OPLUS -> add(
            DeviceIntegrationRow(
                profile.systemName,
                copy.oplusLyricsDetail,
                DeviceIntegrationStatus.ACTIVE
            )
        )
        LevyraDeviceFamily.SAMSUNG -> add(
            DeviceIntegrationRow(
                "Now Bar",
                copy.nowBarDetail,
                if (profile.supports(LevyraDeviceFeature.NOW_BAR)) {
                    DeviceIntegrationStatus.AUTOMATIC
                } else {
                    DeviceIntegrationStatus.UNAVAILABLE
                }
            )
        )
        LevyraDeviceFamily.HONOR,
        LevyraDeviceFamily.GOOGLE,
        LevyraDeviceFamily.GENERIC -> Unit
    }
}

@Composable
internal fun DeviceIntegrationSettingsPanel() {
    val context = LocalContext.current
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { deviceIntegrationCopy(strings.code) }
    var resumeToken by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumeToken++
        onPauseOrDispose { }
    }
    val profile by produceState<LevyraDeviceProfile?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { LevyraDeviceProfiles.current(context) }
    }
    val notificationsEnabled = remember(resumeToken) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val detected = profile ?: return
    val rows = remember(detected, notificationsEnabled, copy) {
        deviceIntegrationRows(detected, notificationsEnabled, copy)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LevyraPanel)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            copy.title,
            color = LevyraText,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            "${detected.manufacturer.replaceFirstChar(Char::titlecase)} · ${detected.systemName}",
            color = LevyraCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(copy.subtitle, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        rows.forEach { row -> DeviceIntegrationStatusRow(row, copy) }
        if (rows.any { it.status == DeviceIntegrationStatus.NOTIFICATIONS_OFF }) {
            TextButton(onClick = { openNotificationSettings(context) }) {
                Text(copy.openNotifications, color = LevyraCyan, fontWeight = FontWeight.Bold)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(copy.backgroundTitle, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(copy.backgroundDetail, color = LevyraMuted, fontSize = 12.sp)
        }
        TextButton(onClick = { openLevyraBackgroundPlaybackSettings(context, detected.family) }) {
            Text(copy.backgroundTitle, color = LevyraCyan, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DeviceIntegrationStatusRow(row: DeviceIntegrationRow, copy: DeviceIntegrationCopy) {
    val (label, color) = when (row.status) {
        DeviceIntegrationStatus.ACTIVE -> copy.active to LevyraCyan
        DeviceIntegrationStatus.AUTOMATIC -> copy.automatic to LevyraCyan
        DeviceIntegrationStatus.NOTIFICATIONS_OFF -> copy.notificationsOff to LevyraMuted
        DeviceIntegrationStatus.UNAVAILABLE -> copy.unavailable to LevyraMuted
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.name, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(row.detail, color = LevyraMuted, fontSize = 12.sp)
        }
        DeviceIntegrationStatusLabel(label, color)
    }
}

@Composable
private fun DeviceIntegrationStatusLabel(label: String, color: Color) {
    Text(
        label,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

private fun openNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
