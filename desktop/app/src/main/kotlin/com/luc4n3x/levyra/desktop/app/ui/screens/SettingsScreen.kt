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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.components.EqualizerBars
import com.luc4n3x.levyra.desktop.app.ui.components.LevyraChip
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.core.model.AppLanguage
import com.luc4n3x.levyra.desktop.core.model.AudioQuality
import com.luc4n3x.levyra.desktop.core.model.DesktopSettings
import com.luc4n3x.levyra.desktop.core.model.EqualizerSettings
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

    ScrollableColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(text = strings.navSettings, style = MaterialTheme.typography.displaySmall)
        }

        item {
            SettingsSection(title = strings.settingsAudio) {
                ChoiceRow(
                    title = strings.settingsAudioQuality,
                    selected = settings.audioQuality,
                    options = listOf(
                        AudioQuality.LOW to strings.settingsQualityLow,
                        AudioQuality.BALANCED to strings.settingsQualityBalanced,
                        AudioQuality.HIGH to strings.settingsQualityHigh
                    ),
                    onSelect = { value -> onUpdate { it.copy(audioQuality = value) } }
                )
                ChoiceRow(
                    title = strings.settingsCodec,
                    selected = settings.preferredCodec,
                    options = listOf(
                        PreferredCodec.AUTO to strings.settingsCodecAuto,
                        PreferredCodec.OPUS to strings.settingsCodecOpus,
                        PreferredCodec.AAC to strings.settingsCodecAac
                    ),
                    onSelect = { value -> onUpdate { it.copy(preferredCodec = value) } }
                )
                SettingsToggle(
                    title = strings.settingsAutoplayRadio,
                    body = strings.settingsAutoplayRadioBody,
                    checked = settings.autoplayRadio,
                    onCheckedChange = { value -> onUpdate { it.copy(autoplayRadio = value) } }
                )
            }
        }

        item {
            SettingsSection(title = strings.settingsEqualizer) {
                Text(
                    text = strings.settingsEqualizerBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SettingsToggle(
                    title = strings.settingsEqualizerEnable,
                    body = "",
                    checked = settings.equalizer.enabled,
                    onCheckedChange = { value ->
                        onUpdate { it.copy(equalizer = it.equalizer.copy(enabled = value)) }
                    }
                )
                EqualizerBars(
                    amps = settings.equalizer.amps,
                    accent = LocalAccentColor.current,
                    onChange = { index, gain ->
                        onUpdate { it.copy(equalizer = it.equalizer.withAmp(index, gain)) }
                    }
                )
                SettingsRow(title = strings.settingsEqualizerPreamp) {
                    Slider(
                        value = settings.equalizer.preamp,
                        onValueChange = { value ->
                            onUpdate { it.copy(equalizer = it.equalizer.copy(preamp = value)) }
                        },
                        valueRange = EqualizerSettings.MIN_GAIN..EqualizerSettings.MAX_GAIN,
                        modifier = Modifier.widthIn(max = 320.dp)
                    )
                }
                OutlinedButton(
                    onClick = { onUpdate { it.copy(equalizer = it.equalizer.flattened()) } }
                ) {
                    Text(strings.settingsEqualizerReset)
                }
            }
        }

        item {
            SettingsSection(title = strings.settingsInterface) {
                ChoiceRow(
                    title = strings.settingsTheme,
                    selected = settings.themeMode,
                    options = listOf(
                        ThemeMode.SYSTEM to strings.settingsThemeSystem,
                        ThemeMode.LIGHT to strings.settingsThemeLight,
                        ThemeMode.DARK to strings.settingsThemeDark
                    ),
                    onSelect = { value -> onUpdate { it.copy(themeMode = value) } }
                )
                ChoiceRow(
                    title = strings.settingsLanguage,
                    selected = settings.language,
                    options = listOf(
                        AppLanguage.ITALIAN to "Italiano",
                        AppLanguage.ENGLISH to "English"
                    ),
                    onSelect = { value -> onUpdate { it.copy(language = value) } }
                )
                SettingsRow(title = strings.settingsCountry, body = strings.settingsCountryBody) {
                    CountryField(
                        value = settings.contentCountry,
                        onCommit = { value -> onUpdate { it.copy(contentCountry = value) } }
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
                    var vlcDraft by remember(settings.vlcDirectory) {
                        mutableStateOf(settings.vlcDirectory)
                    }
                    fun commitVlcDirectory() {
                        val trimmed = vlcDraft.trim()
                        if (trimmed != settings.vlcDirectory) {
                            onUpdate { it.copy(vlcDirectory = trimmed) }
                        }
                    }
                    OutlinedTextField(
                        value = vlcDraft,
                        onValueChange = { value -> vlcDraft = value },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focus -> if (!focus.isFocused) commitVlcDirectory() }
                    )
                    OutlinedButton(onClick = onBrowseVlc) {
                        Text(strings.settingsVlcBrowse)
                    }
                    Button(
                        onClick = {
                            commitVlcDirectory()
                            onVerifyVlc()
                        }
                    ) {
                        Text(strings.settingsVlcVerify)
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
private fun CountryField(value: String, onCommit: (String) -> Unit) {
    var draft by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = draft,
        onValueChange = { input ->
            draft = input.filter { it.isLetter() }.take(COUNTRY_CODE_LENGTH).uppercase()
            if (draft.length == COUNTRY_CODE_LENGTH && draft != value) {
                onCommit(draft)
            }
        },
        singleLine = true,
        modifier = Modifier.widthIn(max = 120.dp)
    )
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
private fun <T> ChoiceRow(
    title: String,
    selected: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    body: String = ""
) {
    SettingsRow(title = title, body = body) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                LevyraChip(
                    label = label,
                    selected = value == selected,
                    onClick = { onSelect(value) }
                )
            }
        }
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

private const val COUNTRY_CODE_LENGTH = 2
