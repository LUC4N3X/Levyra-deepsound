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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.components.CountryPicker
import com.luc4n3x.levyra.desktop.app.ui.components.EqualizerBars
import com.luc4n3x.levyra.desktop.app.ui.components.LanguagePicker
import com.luc4n3x.levyra.desktop.app.ui.components.LevyraChip
import com.luc4n3x.levyra.desktop.app.ui.components.LevyraOption
import com.luc4n3x.levyra.desktop.app.ui.components.LevyraOptionPicker
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.tracksTextInputFocus
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.app.util.Format
import com.luc4n3x.levyra.desktop.core.model.AudioQuality
import com.luc4n3x.levyra.desktop.core.model.DesktopSettings
import com.luc4n3x.levyra.desktop.core.model.EqualizerPreset
import com.luc4n3x.levyra.desktop.core.model.EqualizerSettings
import com.luc4n3x.levyra.desktop.core.model.PreferredCodec
import com.luc4n3x.levyra.desktop.core.model.ThemeMode
import com.luc4n3x.levyra.desktop.player.AudioOutputDevice

@Composable
fun SettingsScreen(
    settings: DesktopSettings,
    dataDirectory: String,
    vlcStatus: String,
    appVersion: String,
    audioOutputDevices: List<AudioOutputDevice>,
    audioOutputDeviceMissing: Boolean,
    onUpdate: ((DesktopSettings) -> DesktopSettings) -> Unit,
    onBrowseVlc: () -> Unit,
    onVerifyVlc: () -> Unit,
    onOpenDataFolder: () -> Unit,
    onRefreshAudioOutputDevices: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    LaunchedEffect(Unit) {
        onRefreshAudioOutputDevices(false)
    }

    ScrollableColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(text = strings.navSettings, style = MaterialTheme.typography.displaySmall)
        }

        item {
            SettingsSection(title = strings.settingsProfile) {
                var nameDraft by remember(settings.displayName) {
                    mutableStateOf(settings.displayName)
                }
                fun commitName() {
                    val clean = nameDraft.trim().take(DesktopSettings.MAX_DISPLAY_NAME_LENGTH)
                    if (clean != settings.displayName) {
                        onUpdate { it.copy(displayName = clean) }
                    }
                }
                SettingsRow(
                    title = strings.settingsProfileName,
                    body = strings.onboardingNameQuestion
                ) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = {
                            nameDraft = it.take(DesktopSettings.MAX_DISPLAY_NAME_LENGTH)
                        },
                        placeholder = { Text(strings.onboardingNamePlaceholder) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                            .tracksTextInputFocus()
                            .onFocusChanged { state ->
                                if (!state.isFocused) commitName()
                            }
                    )
                }
                OutlinedButton(
                    onClick = {
                        commitName()
                        onUpdate { it.copy(onboardingCompleted = false) }
                    }
                ) {
                    Text(strings.settingsRedoOnboarding)
                }
                Text(
                    text = strings.settingsRedoOnboardingBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                SettingsRow(
                    title = strings.settingsLanguage,
                    body = strings.onboardingLanguageQuestion
                ) {
                    LanguagePicker(
                        selected = settings.language,
                        label = strings.settingsLanguage,
                        contentDescription = strings.onboardingLanguageQuestion,
                        onSelected = { language ->
                            onUpdate {
                                it.copy(
                                    language = language,
                                    contentCountry = if (
                                        it.contentCountry.equals(it.language.defaultCountry, ignoreCase = true)
                                    ) {
                                        language.defaultCountry
                                    } else {
                                        it.contentCountry
                                    }
                                )
                            }
                        }
                    )
                }
                SettingsRow(
                    title = strings.settingsCountry,
                    body = strings.settingsCountryBody
                ) {
                    CountryPicker(
                        selectedCode = settings.contentCountry,
                        label = strings.chartsCountry,
                        contentDescription = strings.chartsSelectCountry,
                        onSelected = { country ->
                            onUpdate { it.copy(contentCountry = country) }
                        }
                    )
                }
            }
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
                SettingsRow(
                    title = strings.settingsAudioOutput,
                    body = strings.settingsAudioOutputBody
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val options = buildList {
                            add(
                                LevyraOption(
                                    id = AudioOutputDevice.SYSTEM_DEFAULT_ID,
                                    label = strings.audioOutputSystemDefault
                                )
                            )
                            audioOutputDevices.forEach { device ->
                                add(LevyraOption(id = device.id, label = device.label))
                            }
                            if (
                                settings.audioOutputDeviceId.isNotEmpty() &&
                                audioOutputDevices.none { it.id == settings.audioOutputDeviceId }
                            ) {
                                add(
                                    LevyraOption(
                                        id = settings.audioOutputDeviceId,
                                        label = settings.audioOutputDeviceId,
                                        supporting = strings.audioOutputEmpty
                                    )
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LevyraOptionPicker(
                                label = strings.settingsAudioOutput,
                                options = options,
                                selectedId = settings.audioOutputDeviceId,
                                contentDescription = strings.settingsAudioOutputBody,
                                onSelect = { value ->
                                    onUpdate { it.copy(audioOutputDeviceId = value) }
                                }
                            )
                            OutlinedButton(onClick = { onRefreshAudioOutputDevices(true) }) {
                                Text(strings.audioOutputRefresh)
                            }
                        }
                        if (audioOutputDeviceMissing) {
                            Text(
                                text = strings.audioOutputUnavailable,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (audioOutputDevices.isEmpty()) {
                            Text(
                                text = strings.audioOutputEmpty,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
                SettingsRow(title = strings.equalizerPreset) {
                    val activeId = EqualizerPreset.selectedId(settings.equalizer)
                    val options = buildList {
                        EqualizerPreset.entries.forEach { preset ->
                            add(
                                LevyraOption(
                                    id = preset.id,
                                    label = equalizerPresetLabel(preset)
                                )
                            )
                        }
                        if (activeId == EqualizerPreset.CUSTOM_ID) {
                            add(
                                LevyraOption(
                                    id = EqualizerPreset.CUSTOM_ID,
                                    label = strings.equalizerPresetCustom
                                )
                            )
                        }
                    }
                    LevyraOptionPicker(
                        label = strings.equalizerPreset,
                        options = options,
                        selectedId = activeId,
                        contentDescription = strings.settingsEqualizerBody,
                        onSelect = { value ->
                            val preset = EqualizerPreset.fromId(value) ?: return@LevyraOptionPicker
                            onUpdate { it.copy(equalizer = preset.applyTo(it.equalizer)) }
                        }
                    )
                }
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
            SettingsSection(title = strings.settingsPlayback) {
                ChoiceRow(
                    title = strings.settingsSpeed,
                    selected = DesktopSettings.normalizeSpeed(settings.playbackSpeed),
                    options = DesktopSettings.SPEED_STEPS.map { step -> step to Format.speed(step) },
                    onSelect = { value -> onUpdate { it.copy(playbackSpeed = value) } }
                )
                ChoiceRow(
                    title = strings.crossfade,
                    body = strings.crossfadeBody,
                    selected = DesktopSettings.normalizeCrossfade(settings.crossfadeMs),
                    options = DesktopSettings.CROSSFADE_STEPS_MS.map { step ->
                        step to if (step == 0) strings.sleepTimerOff else "${step / 1_000}s"
                    },
                    onSelect = { value -> onUpdate { it.copy(crossfadeMs = value) } }
                )
                SettingsToggle(
                    title = strings.smartCrossfade,
                    body = strings.smartCrossfadeBody,
                    checked = settings.smartCrossfade,
                    onCheckedChange = { value -> onUpdate { it.copy(smartCrossfade = value) } }
                )
                SettingsToggle(
                    title = strings.settingsPreloadNext,
                    body = strings.settingsPreloadNextBody,
                    checked = settings.preloadNextTrack,
                    onCheckedChange = { value -> onUpdate { it.copy(preloadNextTrack = value) } }
                )
                SettingsToggle(
                    title = strings.settingsMediaKeys,
                    body = strings.settingsMediaKeysBody,
                    checked = settings.globalMediaKeys,
                    onCheckedChange = { value -> onUpdate { it.copy(globalMediaKeys = value) } }
                )
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
            SettingsSection(title = strings.settingsShortcuts) {
                val shortcuts = listOf(
                    "Space" to "${strings.playbackPlay} / ${strings.playbackPause}",
                    "Ctrl + ← / →" to "${strings.playbackPrevious} / ${strings.playbackNext}",
                    "← / →" to strings.shortcutSeek,
                    "Ctrl + ↑ / ↓" to strings.shortcutVolume,
                    "Ctrl + M" to strings.playbackMute,
                    "Ctrl + Shift + M" to strings.miniPlayer,
                    "Ctrl + S" to strings.playbackShuffle,
                    "Ctrl + R" to strings.playbackRepeat,
                    "Ctrl + Q" to strings.queueTitle,
                    "Ctrl + P" to strings.navNowPlaying,
                    "Ctrl + F / K" to strings.navSearch
                )
                shortcuts.forEach { (keys, label) -> ShortcutRow(keys = keys, label = label) }
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
                            .tracksTextInputFocus()
                            .onFocusChanged { focus ->
                                if (!focus.isFocused) commitVlcDirectory()
                            }
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
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(22.dp),
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
private fun ShortcutRow(keys: String, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = keys,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (body.isNotBlank()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun equalizerPresetLabel(preset: EqualizerPreset): String {
    val strings = LocalStrings.current
    return when (preset) {
        EqualizerPreset.FLAT -> strings.equalizerPresetFlat
        EqualizerPreset.BASS_BOOST -> strings.equalizerPresetBassBoost
        EqualizerPreset.VOCAL -> strings.equalizerPresetVocal
        EqualizerPreset.ROCK -> strings.equalizerPresetRock
        EqualizerPreset.POP -> strings.equalizerPresetPop
        EqualizerPreset.ELECTRONIC -> strings.equalizerPresetElectronic
        EqualizerPreset.HIP_HOP -> strings.equalizerPresetHipHop
        EqualizerPreset.CLASSICAL -> strings.equalizerPresetClassical
        EqualizerPreset.ACOUSTIC -> strings.equalizerPresetAcoustic
        EqualizerPreset.NIGHT -> strings.equalizerPresetNight
    }
}
