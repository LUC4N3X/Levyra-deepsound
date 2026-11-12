package com.luc4n3x.levyra.ui.regional

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.data.AudioLanguageIntelligence
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraLanguageOption
import com.luc4n3x.levyra.ui.SettingsValueRow
import com.luc4n3x.levyra.ui.i18n.LevyraLanguagePickerStrings
import com.luc4n3x.levyra.ui.i18n.LevyraRegionalStrings

internal enum class RegionalPreferencesSection { App, Content }

internal fun preferredAudioLanguageOption(preferred: String): LevyraLanguageOption? {
    val normalized = AudioLanguageIntelligence.normalizeLanguage(preferred)
    if (normalized.isEmpty()) return null
    val options = LevyraLanguageCatalog.selectableLanguages
    return options.firstOrNull { AudioLanguageIntelligence.normalizeLanguage(it.code) == normalized }
        ?: options.firstOrNull {
            AudioLanguageIntelligence.normalizeLanguage(it.code) == normalized.substringBefore('-')
        }
}

@Composable
internal fun RegionalPreferencesCard(
    languageCode: String,
    appLanguageValue: String,
    contentCountryValue: String,
    preferredAudioLanguage: String,
    selectedAppLanguage: String,
    onAppLanguage: (String) -> Unit,
    onContentCountryClick: () -> Unit,
    onPreferredAudioLanguage: (String) -> Unit,
    section: RegionalPreferencesSection,
    modifier: Modifier = Modifier
) {
    val copy = LevyraRegionalStrings.forCode(languageCode)
    val pickerCopy = LevyraLanguagePickerStrings.forCode(languageCode)
    var audioSheetOpen by rememberSaveable { mutableStateOf(false) }
    var appLanguageSheetOpen by rememberSaveable { mutableStateOf(false) }
    val audioValue = preferredAudioLanguageOption(preferredAudioLanguage)
        ?.let { option -> "${option.flag} ${option.nativeName}" }
        ?: AudioLanguageIntelligence.normalizeLanguage(preferredAudioLanguage).ifEmpty { copy.automatic }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        when (section) {
            RegionalPreferencesSection.App -> SettingsValueRow(
                icon = Icons.Rounded.Translate,
                title = copy.appLanguageTitle,
                subtitle = copy.appLanguageBody,
                value = appLanguageValue,
                onClick = { appLanguageSheetOpen = true },
                groupIndex = 0,
                groupCount = 1
            )
            RegionalPreferencesSection.Content -> {
                SettingsValueRow(
                    icon = Icons.Rounded.Public,
                    title = copy.contentCountryTitle,
                    subtitle = copy.contentCountryBody,
                    value = contentCountryValue,
                    onClick = onContentCountryClick,
                    groupIndex = 0,
                    groupCount = 2
                )
                SettingsValueRow(
                    icon = Icons.Rounded.RecordVoiceOver,
                    title = copy.audioLanguageTitle,
                    subtitle = copy.audioLanguageBody,
                    value = audioValue,
                    onClick = { audioSheetOpen = true },
                    groupIndex = 1,
                    groupCount = 2
                )
            }
        }
    }
    if (audioSheetOpen) {
        LanguagePickerSheet(
            title = copy.audioLanguageTitle,
            note = copy.fallbackNote,
            copy = pickerCopy,
            selectedCode = preferredAudioLanguageOption(preferredAudioLanguage)?.code,
            automatic = LanguagePickerAutomatic(copy.automatic, copy.automaticBody),
            onSelect = onPreferredAudioLanguage,
            onDismiss = { audioSheetOpen = false }
        )
    }
    if (appLanguageSheetOpen) {
        LanguagePickerSheet(
            title = copy.appLanguageTitle,
            note = null,
            copy = pickerCopy,
            selectedCode = LevyraLanguageCatalog.normalize(selectedAppLanguage),
            automatic = null,
            onSelect = onAppLanguage,
            onDismiss = { appLanguageSheetOpen = false }
        )
    }
}
