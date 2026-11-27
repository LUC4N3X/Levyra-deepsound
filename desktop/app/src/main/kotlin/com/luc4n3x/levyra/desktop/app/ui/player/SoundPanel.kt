package com.luc4n3x.levyra.desktop.app.ui.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.luc4n3x.levyra.ui.i18n.SpectrumText
import com.luc4n3x.levyra.ui.i18n.spectrumCopy
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.luc4n3x.levyra.desktop.app.ui.components.EqualizerBars
import com.luc4n3x.levyra.desktop.app.ui.components.LevyraChip
import com.luc4n3x.levyra.desktop.app.ui.components.LevyraSlider
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.screens.equalizerPresetLabel
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.core.model.AudioQuality
import com.luc4n3x.levyra.desktop.core.model.DesktopSettings
import com.luc4n3x.levyra.desktop.core.model.EqualizerPreset
import com.luc4n3x.levyra.desktop.core.model.EqualizerSettings
import com.luc4n3x.levyra.domain.HighQualityAudioMode
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedStrings

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SoundPanel(
    settings: DesktopSettings,
    onUpdate: ((DesktopSettings) -> DesktopSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val shared = SharedStrings.forCode(settings.language.tag)
    val equalizer = settings.equalizer
    Popup(
        popupPositionProvider = AboveAnchor,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 16.dp,
            modifier = Modifier.width(440.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PanelLabel(strings.settingsAudioQuality)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AudioQuality.LOW to strings.settingsQualityLow,
                        AudioQuality.BALANCED to strings.settingsQualityBalanced,
                        AudioQuality.HIGH to strings.settingsQualityHigh
                    ).forEach { (value, label) ->
                        LevyraChip(label, settings.audioQuality == value, { onUpdate { it.copy(audioQuality = value) } })
                    }
                }
                PanelLabel(shared.alternativeAudioTitle)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        HighQualityAudioMode.PREFER_320 to shared.alternativeAudioPrefer320,
                        HighQualityAudioMode.PREFER_LOSSLESS to shared.alternativeAudioPreferLossless,
                        HighQualityAudioMode.AUTOMATIC to shared.alternativeAudioAutomatic,
                        HighQualityAudioMode.OFF to shared.alternativeAudioOff
                    ).forEach { (mode, label) ->
                        LevyraChip(label, settings.highQualityAudioMode == mode.storageValue, {
                            onUpdate { it.copy(highQualityAudioMode = mode.storageValue) }
                        })
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.settingsEqualizer, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(
                        checked = equalizer.enabled,
                        onCheckedChange = { value -> onUpdate { it.copy(equalizer = it.equalizer.copy(enabled = value)) } }
                    )
                }
                val activeId = EqualizerPreset.selectedId(equalizer)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EqualizerPreset.entries.forEach { preset ->
                        LevyraChip(equalizerPresetLabel(preset), activeId == preset.id, {
                            onUpdate { it.copy(equalizer = preset.applyTo(it.equalizer)) }
                        })
                    }
                    if (activeId == EqualizerPreset.CUSTOM_ID) {
                        LevyraChip(strings.equalizerPresetCustom, true, {})
                    }
                }
                EqualizerBars(
                    amps = equalizer.amps,
                    accent = LocalAccentColor.current,
                    onChange = { index, gain -> onUpdate { it.copy(equalizer = it.equalizer.withAmp(index, gain)) } },
                    modifier = Modifier.fillMaxWidth()
                )
                PanelLabel(strings.settingsEqualizerPreamp)
                val range = EqualizerSettings.MAX_GAIN - EqualizerSettings.MIN_GAIN
                LevyraSlider(
                    fraction = (equalizer.preamp - EqualizerSettings.MIN_GAIN) / range,
                    onFractionChange = { fraction ->
                        onUpdate { it.copy(equalizer = it.equalizer.copy(preamp = EqualizerSettings.MIN_GAIN + fraction * range)) }
                    },
                    activeColor = LocalAccentColor.current,
                    description = strings.settingsEqualizerPreamp,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { onUpdate { it.copy(equalizer = it.equalizer.flattened()) } },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(strings.settingsEqualizerReset)
                }
                EqualizerExportRow(settings)
                Spacer(Modifier.height(6.dp))
                OutputLatencyRow(settings, onUpdate)
            }
        }
    }
}

@Composable
private fun PanelLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private object AboveAnchor : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val gap = anchorBounds.height / 2
        val x = (anchorBounds.right - popupContentSize.width).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val y = (anchorBounds.top - popupContentSize.height - gap).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

@Composable
private fun OutputLatencyRow(settings: DesktopSettings, onUpdate: ((DesktopSettings) -> DesktopSettings) -> Unit) {
    val copy = SharedStrings.forCode(settings.language.tag).spectrumCopy
    val offset = settings.outputLatencyOffsetMs
    val label = when {
        offset > 0 -> "+$offset ms"
        offset < 0 -> "−${-offset} ms"
        else -> "0 ms"
    }
    fun shift(delta: Int) = onUpdate { current ->
        current.copy(
            outputLatencyOffsetMs = (current.outputLatencyOffsetMs + delta)
                .coerceIn(-DesktopSettings.MAX_OUTPUT_LATENCY_OFFSET_MS, DesktopSettings.MAX_OUTPUT_LATENCY_OFFSET_MS)
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(copy[SpectrumText.Latency], style = MaterialTheme.typography.titleSmall)
            Text(label, style = MaterialTheme.typography.labelLarge, color = LocalAccentColor.current)
            Text(
                copy[SpectrumText.LatencyDesc],
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = { shift(-OUTPUT_LATENCY_STEP_MS) }, enabled = offset > -DesktopSettings.MAX_OUTPUT_LATENCY_OFFSET_MS) {
            Text("−", modifier = Modifier.semantics { contentDescription = copy[SpectrumText.Earlier] })
        }
        TextButton(onClick = { shift(OUTPUT_LATENCY_STEP_MS) }, enabled = offset < DesktopSettings.MAX_OUTPUT_LATENCY_OFFSET_MS) {
            Text("+", modifier = Modifier.semantics { contentDescription = copy[SpectrumText.Later] })
        }
        if (offset != 0) {
            TextButton(onClick = { onUpdate { it.copy(outputLatencyOffsetMs = 0) } }) { Text(copy[SpectrumText.Reset]) }
        }
    }
}

@Composable
private fun EqualizerExportRow(settings: DesktopSettings) {
    val copy = SharedStrings.forCode(settings.language.tag).spectrumCopy
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = {
                val text = settings.equalizer.toGraphicEq()
                scope.launch {
                    val target = chooseEqualizerExportFile() ?: return@launch
                    val written = withContext(Dispatchers.IO) {
                        runCatching { target.writeText(text, Charsets.UTF_8) }.isSuccess
                    }
                    status = if (written) copy[SpectrumText.Exported] else copy[SpectrumText.ExportFailed]
                }
            },
            enabled = settings.equalizer.enabled
        ) {
            Text("${copy[SpectrumText.ExportTitle]} · ${copy[SpectrumText.ExportGraphic]}")
        }
        status?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private suspend fun chooseEqualizerExportFile(): File? = withContext(Dispatchers.Swing) {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.FILES_ONLY
    chooser.isMultiSelectionEnabled = false
    chooser.fileFilter = FileNameExtensionFilter("GraphicEQ", "txt")
    chooser.selectedFile = File(EQUALIZER_EXPORT_FILE_NAME)
    if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return@withContext null
    val selected = chooser.selectedFile ?: return@withContext null
    if (selected.extension.isBlank()) File(selected.parentFile, "${selected.name}.txt") else selected
}

private const val OUTPUT_LATENCY_STEP_MS = 10
private const val EQUALIZER_EXPORT_FILE_NAME = "Levyra EQ GraphicEQ.txt"
