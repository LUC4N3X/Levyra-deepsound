package com.luc4n3x.levyra.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.luc4n3x.levyra.data.locallibrary.LocalLibraryRepository
import com.luc4n3x.levyra.data.locallibrary.LocalMediaStoreScanner
import com.luc4n3x.levyra.data.locallibrary.PreferredLocalAudio
import com.luc4n3x.levyra.ui.SettingsToggle
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.preferLocalAudioCopy

@Composable
internal fun PreferLocalAudioSettingsPanel() {
    val context = LocalContext.current
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { preferLocalAudioCopy(strings.code) }
    val preferredLocalAudio = remember(context) { PreferredLocalAudio.get(context) }
    var enabled by remember { mutableStateOf(preferredLocalAudio.enabled()) }
    var permissionGranted by remember { mutableStateOf(preferredLocalAudio.hasPermission()) }
    LifecycleResumeEffect(preferredLocalAudio) {
        permissionGranted = preferredLocalAudio.hasPermission()
        onPauseOrDispose { }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (granted) {
            LocalLibraryRepository.get(context).refreshPermission()
            preferredLocalAudio.setEnabled(true)
            enabled = true
        }
    }
    SettingsToggle(
        icon = Icons.Rounded.LibraryMusic,
        title = copy.title,
        subtitle = if (enabled && !permissionGranted) copy.permissionNeeded else copy.subtitle,
        checked = enabled,
        onCheckedChange = { checked ->
            if (checked && !preferredLocalAudio.hasPermission()) {
                permissionLauncher.launch(LocalMediaStoreScanner.requiredPermission())
            } else {
                preferredLocalAudio.setEnabled(checked)
                enabled = checked
            }
        }
    )
}
