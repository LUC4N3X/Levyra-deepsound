package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.components.LevyraChip
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.core.model.AppLanguage
import com.luc4n3x.levyra.desktop.core.model.AudioQuality
import com.luc4n3x.levyra.desktop.core.model.DesktopSettings
import com.luc4n3x.levyra.desktop.core.model.PreferredCodec
import com.luc4n3x.levyra.desktop.core.model.ThemeMode

@Composable
fun SettingsScreen(
    settings: DesktopSettings,
    dataDirectory: String,
    vlcStatus: String,
    appVersion: String,
    onUpdate: ((DesktopSettings) -> DesktopSettings) -> Unit,
    onBrowseVlc: () -> Unit,
    onVerifyVlc: () -> Unit,
    onOpenDataFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(text = strings.navSettings, style = MaterialTheme.typography.displaySmall)
        }

        item {
            SettingsSection(title = strings.settingsAudio) {
                SettingsRow(title = strings.settingsAudioQuality) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LevyraChip(
                            label = strings.settingsQualityLow,
                            selected = settings.audioQuality == AudioQuality.LOW,
                            onClick = { onUpdate { it.copy(audioQuality = AudioQuality.LOW) } }
                        )
                        LevyraChip(
                            label = strings.settingsQualityBalanced,
                            selected = settings.audioQuality == AudioQuality.BALANCED,
                            onClick = { onUpdate { it.copy(audioQuality = AudioQuality.BALANCED) } }
                        )
                        LevyraChip(
                            label = strings.settingsQualityHigh,
                            selected = settings.audioQuality == AudioQuality.HIGH,
                            onClick = { onUpdate { it.copy(audioQuality = AudioQuality.HIGH) } }
                        )
                    }
                }
                SettingsRow(title = strings.settingsCodec) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LevyraChip(
                            label = strings.settingsCodecAuto,
                            selected = settings.preferredCodec == PreferredCodec.AUTO,
                            onClick = { onUpdate { it.copy(preferredCodec = PreferredCodec.AUTO) } }
                        )
                        LevyraChip(
                            label = strings.settingsCodecOpus,
                            selected = settings.preferredCodec == PreferredCodec.OPUS,
                            onClick = { onUpdate { it.copy(preferredCodec = PreferredCodec.OPUS) } }
                        )
                        LevyraChip(
                            label = strings.settingsCodecAac,
                            selected = settings.preferredCodec == PreferredCodec.AAC,
                            onClick = { onUpdate { it.copy(preferredCodec = PreferredCodec.AAC) } }
                        )
                    }
                }
                SettingsToggle(
                    title = strings.settingsAutoplayRadio,
                    body = strings.settingsAutoplayRadioBody,
                    checked = settings.autoplayRadio,
                    onCheckedChange = { value -> onUpdate { it.copy(autoplayRadio = value) } }
                )
            }
        }

        item {
            SettingsSection(title = strings.settingsInterface) {
                SettingsRow(title = strings.settingsTheme) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LevyraChip(
                            label = strings.settingsThemeSystem,
                            selected = settings.themeMode == ThemeMode.SYSTEM,
                            onClick = { onUpdate { it.copy(themeMode = ThemeMode.SYSTEM) } }
                        )
                        LevyraChip(
                            label = strings.settingsThemeLight,
                            selected = settings.themeMode == ThemeMode.LIGHT,
                            onClick = { onUpdate { it.copy(themeMode = ThemeMode.LIGHT) } }
                        )
                        LevyraChip(
                            label = strings.settingsThemeDark,
                            selected = settings.themeMode == ThemeMode.DARK,
                            onClick = { onUpdate { it.copy(themeMode = ThemeMode.DARK) } }
                        )
                    }
                }
                SettingsRow(title = strings.settingsLanguage) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LevyraChip(
                            label = "Italiano",
                            selected = settings.language == AppLanguage.ITALIAN,
                            onClick = { onUpdate { it.copy(language = AppLanguage.ITALIAN) } }
                        )
                        LevyraChip(
                            label = "English",
                            selected = settings.language == AppLanguage.ENGLISH,
                            onClick = { onUpdate { it.copy(language = AppLanguage.ENGLISH) } }
                        )
                    }
                }
                SettingsRow(title = strings.settingsCountry, body = strings.settingsCountryBody) {
                    OutlinedTextField(
                        value = settings.contentCountry,
                        onValueChange = { value -> onUpdate { it.copy(contentCountry = value) } },
                        singleLine = true,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                }
            }
        }

        item {
            SettingsSection(title = strings.settingsPlayback) {
                SettingsToggle(
                    title = strings.settingsResume,
                    body = strings.settingsResumeBody,
                    checked = settings.resumeOnStartup,
                    onCheckedChange = { value -> onUpdate { it.copy(resumeOnStartup = value) } }
                )
                SettingsToggle(
                    title = strings.settingsTray,
                    body = strings.settingsTrayBody,
                    checked = settings.minimizeToTray,
                    onCheckedChange = { value -> onUpdate { it.copy(minimizeToTray = value) } }
                )
            }
        }

        item {
            SettingsSection(title = strings.settingsVlc) {
                Text(
                    text = strings.settingsVlcBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = settings.vlcDirectory,
                        onValueChange = { value -> onUpdate { it.copy(vlcDirectory = value) } },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = onBrowseVlc) {
                        Text(strings.settingsVlcBrowse)
                    }
                    Button(onClick = onVerifyVlc) {
                        Text(strings.retry)
                    }
                }
                if (vlcStatus.isNotBlank()) {
                    Text(
                        text = vlcStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        item {
            SettingsSection(title = strings.settingsStorage) {
                Text(
                    text = strings.settingsStorageBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dataDirectory,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                OutlinedButton(onClick = onOpenDataFolder) {
                    Text(strings.settingsOpenFolder)
                }
            }
        }

        item {
            SettingsSection(title = strings.settingsAbout) {
                Text(
                    text = strings.settingsAboutBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = appVersion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun SettingsRow(
    title: String,
    body: String = "",
    control: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        if (body.isNotBlank()) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        control()
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
