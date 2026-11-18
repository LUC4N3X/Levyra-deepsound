package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.ADVANCED_QUEUE_PREFETCH_MAX
import com.luc4n3x.levyra.domain.ADVANCED_QUEUE_PREFETCH_OFF
import com.luc4n3x.levyra.domain.ADVANCED_QUEUE_PREFETCH_OPTIONS
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.advancedPrefetchCopy
import com.luc4n3x.levyra.ui.i18n.offlineQueueCopy
import com.luc4n3x.levyra.ui.i18n.smartOfflineCopy

@Composable
internal fun AdvancedPrefetchChoiceSetting(tracks: Int, onSelect: (Int) -> Unit) {
    val strings = LocalLevyraStrings.current
    val prefetchCopy = advancedPrefetchCopy(strings.code)
    val queueCopy = offlineQueueCopy(strings.code)
    var customSelected by rememberSaveable(tracks) { mutableStateOf(tracks !in ADVANCED_QUEUE_PREFETCH_OPTIONS) }
    var customDraft by rememberSaveable(tracks) { mutableStateOf(tracks.takeIf { it > ADVANCED_QUEUE_PREFETCH_OFF }?.toString().orEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SettingsChoiceRow(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            title = prefetchCopy.title,
            subtitle = prefetchCopy.description,
            options = ADVANCED_QUEUE_PREFETCH_OPTIONS.map { option ->
                option.toString() to if (option == ADVANCED_QUEUE_PREFETCH_OFF) prefetchCopy.off else prefetchCopy.tracks(option)
            } + (ADVANCED_PREFETCH_CUSTOM_KEY to if (customSelected && tracks > ADVANCED_QUEUE_PREFETCH_OFF) {
                "${strings.smartOfflineCopy().custom} · ${prefetchCopy.tracks(tracks)}"
            } else {
                strings.smartOfflineCopy().custom
            }),
            selected = if (customSelected) ADVANCED_PREFETCH_CUSTOM_KEY else tracks.toString(),
            onSelect = { value ->
                if (value == ADVANCED_PREFETCH_CUSTOM_KEY) {
                    customSelected = true
                } else {
                    customSelected = false
                    value.toIntOrNull()?.let(onSelect)
                }
            }
        )
        if (customSelected) {
            val saveCustom = {
                customDraft.toIntOrNull()
                    ?.coerceIn(1, ADVANCED_QUEUE_PREFETCH_MAX)
                    ?.let { value ->
                        customDraft = value.toString()
                        onSelect(value)
                    }
                Unit
            }
            SmartOfflineTextSetting(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                title = queueCopy.prefetchCustomTitle,
                subtitle = queueCopy.prefetchCustomSubtitle(ADVANCED_QUEUE_PREFETCH_MAX),
                value = customDraft,
                hint = "1–$ADVANCED_QUEUE_PREFETCH_MAX",
                saveLabel = strings.save,
                numeric = true,
                onValueChange = { customDraft = it.filter(Char::isDigit).take(2) },
                onSave = saveCustom
            )
        }
    }
}

private const val ADVANCED_PREFETCH_CUSTOM_KEY = "custom"
