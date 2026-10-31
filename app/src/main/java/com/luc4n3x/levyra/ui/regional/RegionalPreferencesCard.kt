package com.luc4n3x.levyra.ui.regional

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.AudioLanguageIntelligence
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraLanguageOption
import com.luc4n3x.levyra.ui.LocalSettingsAccent
import com.luc4n3x.levyra.ui.SettingsTile
import com.luc4n3x.levyra.ui.SettingsTileIcon
import com.luc4n3x.levyra.ui.SettingsTileText
import com.luc4n3x.levyra.ui.i18n.LevyraLanguagePickerStrings
import com.luc4n3x.levyra.ui.i18n.LevyraRegionalStrings
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText

private const val REGIONAL_ROW_COUNT = 3

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
        RegionalPreferenceRow(
            icon = Icons.Rounded.Translate,
            title = copy.appLanguageTitle,
            explanation = copy.appLanguageBody,
            value = appLanguageValue,
            position = 0,
            onClick = { appLanguageSheetOpen = true }
        )
        RegionalPreferenceRow(
            icon = Icons.Rounded.Public,
            title = copy.contentCountryTitle,
            explanation = copy.contentCountryBody,
            value = contentCountryValue,
            position = 1,
            onClick = onContentCountryClick
        )
        RegionalPreferenceRow(
            icon = Icons.Rounded.RecordVoiceOver,
            title = copy.audioLanguageTitle,
            explanation = copy.audioLanguageBody,
            value = audioValue,
            position = 2,
            onClick = { audioSheetOpen = true }
        )
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

@Composable
private fun RegionalPreferenceRow(
    icon: ImageVector,
    title: String,
    explanation: String,
    value: String,
    position: Int,
    onClick: () -> Unit
) {
    SettingsTile(onClick = onClick, groupIndex = position, groupCount = REGIONAL_ROW_COUNT) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsTileIcon(icon = icon, accent = LocalSettingsAccent.current ?: LevyraPink)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsTileText(title = title, subtitle = explanation, maxTitleLines = 2, maxSubtitleLines = 2)
                Text(
                    text = value,
                    color = LevyraText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = LevyraMuted.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
