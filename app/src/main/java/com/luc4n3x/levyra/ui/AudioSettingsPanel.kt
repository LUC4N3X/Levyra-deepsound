package com.luc4n3x.levyra.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.domain.AutoEqImporter
import com.luc4n3x.levyra.domain.LevyraAudioPresets
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.localizedAudioPresetLabel
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val PanelShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
private val CardShape = RoundedCornerShape(20.dp)
private val ChipShape = RoundedCornerShape(14.dp)
private const val DISABLED_ALPHA = 0.42f
private val EqualizerHandleRadius = 5.dp

@Composable
internal fun AudioSettingsPanel(
    selected: String,
    volumePercent: Int,
    audioSettings: LevyraAudioSettings,
    onSelect: (String) -> Unit,
    onEqualizerEnabled: (Boolean) -> Unit,
    onPreset: (String) -> Unit,
    onBandLevel: (Int, Int) -> Unit,
    onBassBoost: (Int) -> Unit,
    onVirtualizer: (Int) -> Unit,
    onPreamp: (Float) -> Unit,
    onLimiter: (Boolean) -> Unit,
    onCrossfade: (Int) -> Unit,
    onDjSoft: (Boolean) -> Unit,
    onReplayGain: (Boolean) -> Unit,
    onTempo: (Float) -> Unit,
    onPitch: (Float) -> Unit,
    onGapless: (Boolean) -> Unit,
    onResetEqualizer: () -> Unit,
    onApplyAutoEq: (AutoEqImporter.ImportedProfile) -> Unit,
    onSaveAutoEqPreset: (String, AutoEqImporter.ImportedProfile) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val blocker = remember { MutableInteractionSource() }
    val equalizerEnabled = audioSettings.equalizerEnabled
    var showAutoEqImport by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraBlack.copy(alpha = 0.62f))
            .clickable(interactionSource = blocker, indication = null) { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = LevyraPanel,
            shape = PanelShape,
            border = BorderStroke(1.dp, LevyraAdaptiveHairline),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .navigationBarsPadding()
                .clickable(interactionSource = blocker, indication = null) {}
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { AudioPanelHandle() }
                item {
                    AudioPanelHeader(
                        title = strings.audioEngine,
                        subtitle = strings.audioEngineSubtitle,
                        volumeLabel = "$volumePercent%",
                        closeLabel = strings.close,
                        onClose = onClose
                    )
                }

                item { AudioSectionLabel(strings.audioSectionQuality) }
                item {
                    AudioQualityRow(
                        selected = selected,
                        labels = listOf(
                            strings.audioQualityAuto to "Auto",
                            strings.audioQualityHigh to "High",
                            strings.audioQualityLow to "Low"
                        ),
                        onSelect = onSelect
                    )
                }

                item { AudioSectionLabel(strings.audioSectionEqualizer) }
                item {
                    val selectedCustomPreset = audioSettings.customPresets
                        .firstOrNull { it.id == audioSettings.presetId }
                    val customCurve = selectedCustomPreset == null &&
                        audioSettings.presetId == LevyraAudioPresets.FLAT &&
                        audioSettings.bandLevels != LevyraAudioPresets.flatLevels
                    AudioCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            AudioEqualizerSwitchRow(
                                label = strings.equalizer,
                                subtitle = strings.equalizerSubtitle,
                                checked = equalizerEnabled,
                                onCheckedChange = onEqualizerEnabled
                            )
                            AudioCardHeader(
                                title = strings.preset,
                                trailing = when {
                                    selectedCustomPreset != null -> selectedCustomPreset.fallbackLabel
                                    customCurve -> strings.audioPresetCustom
                                    else -> strings.localizedAudioPresetLabel(
                                        audioSettings.presetId,
                                        LevyraAudioPresets.labelFor(audioSettings.presetId)
                                    )
                                }
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 4.dp)
                            ) {
                                items(audioSettings.customPresets, key = { "custom:${it.id}" }) { preset ->
                                    AudioPresetChip(
                                        label = preset.fallbackLabel,
                                        selected = audioSettings.presetId == preset.id,
                                        enabled = equalizerEnabled,
                                        onClick = { onPreset(preset.id) }
                                    )
                                }
                                items(LevyraAudioPresets.presets, key = { it.id }) { preset ->
                                    AudioPresetChip(
                                        label = strings.localizedAudioPresetLabel(preset.id, preset.fallbackLabel),
                                        selected = selectedCustomPreset == null &&
                                            !customCurve &&
                                            audioSettings.presetId == preset.id,
                                        enabled = equalizerEnabled,
                                        onClick = { onPreset(preset.id) }
                                    )
                                }
                            }
                            EqualizerCurve(
                                levels = audioSettings.bandLevels,
                                enabled = equalizerEnabled,
                                bandsLabel = strings.audioBands,
                                onBandLevel = onBandLevel
                            )
                            AudioTextAction(
                                label = strings.autoEqImport,
                                enabled = equalizerEnabled,
                                onClick = { showAutoEqImport = true }
                            )
                            AudioTextAction(
                                label = strings.audioResetEqualizer,
                                enabled = equalizerEnabled,
                                onClick = onResetEqualizer
                            )
                        }
                    }
                }
                item {
                    AudioSliderRow(
                        title = strings.preamp,
                        valueLabel = decibels(audioSettings.preampDb),
                        value = audioSettings.preampDb,
                        range = -12f..3f,
                        onValue = { onPreamp((it * 2f).roundToInt() / 2f) }
                    )
                }
                item {
                    AudioSliderRow(
                        title = strings.bassBoost,
                        valueLabel = "${audioSettings.bassBoost}%",
                        value = audioSettings.bassBoost.toFloat(),
                        range = 0f..100f,
                        onValue = { onBassBoost(it.roundToInt()) }
                    )
                }

                item { AudioSectionLabel(strings.audioSectionSpatial) }
                item {
                    AudioSliderRow(
                        title = strings.virtualizer,
                        valueLabel = "${audioSettings.virtualizer}%",
                        value = audioSettings.virtualizer.toFloat(),
                        range = 0f..100f,
                        icon = true,
                        onValue = { onVirtualizer(it.roundToInt()) }
                    )
                }

                item { AudioSectionLabel(strings.audioSectionDynamics) }
                item {
                    AudioToggleRow(
                        title = strings.truePeakLimiter,
                        subtitle = "−1.0 dBTP",
                        checked = audioSettings.limiterEnabled,
                        onCheckedChange = onLimiter
                    )
                }
                item {
                    AudioToggleRow(
                        title = strings.replayGain,
                        subtitle = "",
                        checked = audioSettings.replayGainEnabled,
                        onCheckedChange = onReplayGain
                    )
                }

                item { AudioSectionLabel(strings.audioSectionPlayback) }
                item {
                    AudioSliderRow(
                        title = strings.crossfade,
                        valueLabel = "${audioSettings.crossfadeSeconds}s",
                        value = audioSettings.crossfadeSeconds.toFloat(),
                        range = 0f..12f,
                        onValue = { onCrossfade(it.roundToInt()) }
                    )
                }
                item {
                    AudioToggleRow(
                        title = strings.djSoft,
                        subtitle = "${strings.crossfade} ${audioSettings.crossfadeSeconds}s",
                        checked = audioSettings.djSoftMode,
                        onCheckedChange = onDjSoft
                    )
                }
                item {
                    AudioToggleRow(
                        title = strings.gapless,
                        subtitle = "",
                        checked = audioSettings.gaplessEnabled,
                        onCheckedChange = onGapless
                    )
                }
                item {
                    AudioSliderRow(
                        title = strings.tempo,
                        valueLabel = "${trimAudioSpeed(audioSettings.playbackSpeed)}x",
                        value = audioSettings.playbackSpeed,
                        range = 0.5f..2.0f,
                        onValue = { onTempo((it * 100f).roundToInt() / 100f) }
                    )
                }
                item {
                    AudioSliderRow(
                        title = strings.pitch,
                        valueLabel = "${trimAudioSpeed(audioSettings.pitch)}x",
                        value = audioSettings.pitch,
                        range = 0.5f..2.0f,
                        onValue = { onPitch((it * 100f).roundToInt() / 100f) }
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(
                            color = LevyraCyan,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .width(136.dp)
                                .heightIn(min = 52.dp)
                                .clickable(onClick = onClose)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(strings.done, color = LevyraBlack, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAutoEqImport) {
        AutoEqImportDialog(
            onDismiss = { showAutoEqImport = false },
            onApply = { profile ->
                onApplyAutoEq(profile)
                showAutoEqImport = false
            },
            onSavePreset = { name, profile ->
                // Saved AutoEQ presets declare neutral bass/spatial values. Apply those values
                // before saving so the audible state matches the preset from the first frame.
                onBassBoost(0)
                onVirtualizer(0)
                onSaveAutoEqPreset(name, profile)
                showAutoEqImport = false
            }
        )
    }
}

@Composable
private fun AutoEqImportDialog(
    onDismiss: () -> Unit,
    onApply: (AutoEqImporter.ImportedProfile) -> Unit,
    onSavePreset: (String, AutoEqImporter.ImportedProfile) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    var rawText by remember { mutableStateOf("") }
    var presetName by remember { mutableStateOf("") }
    var presetNameDirty by remember { mutableStateOf(false) }
    var readError by remember { mutableStateOf<String?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        readError = null
        pendingUri = uri
    }

    LaunchedEffect(pendingUri) {
        val uri = pendingUri ?: return@LaunchedEffect
        val outcome = withContext(Dispatchers.IO) { readBoundedAutoEqText(context, uri) }
        pendingUri = null
        when (outcome) {
            is AutoEqFileRead.Success -> {
                rawText = outcome.text
                readError = null
            }
            AutoEqFileRead.TooLarge -> readError = strings.autoEqInputTooLarge
            AutoEqFileRead.Unreadable -> readError = strings.autoEqInvalidProfile
        }
    }

    val parsed = remember(rawText) {
        if (rawText.isBlank()) null else AutoEqImporter.parse(rawText)
    }
    val profile = if (readError == null) {
        (parsed as? AutoEqImporter.ParseResult.Success)?.profile
    } else {
        null
    }
    LaunchedEffect(profile?.name) {
        if (!presetNameDirty) presetName = profile?.name.orEmpty()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = LevyraPanel,
            shape = CardShape,
            border = BorderStroke(1.dp, LevyraAdaptiveHairline),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    strings.autoEqImport,
                    color = LevyraText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    strings.autoEqImportHint,
                    color = LevyraMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { candidate ->
                        if (candidate.length > AutoEqImporter.MAX_INPUT_CHARS) {
                            readError = strings.autoEqInputTooLarge
                        } else {
                            readError = null
                            rawText = candidate
                        }
                    },
                    placeholder = { Text("GraphicEQ: 20 4.5; 25 4.4; ...", color = LevyraMuted, fontSize = 12.sp) },
                    minLines = 3,
                    maxLines = 6,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LevyraText,
                        unfocusedTextColor = LevyraText,
                        focusedBorderColor = LevyraCyan.copy(alpha = 0.7f),
                        unfocusedBorderColor = LevyraAdaptiveHairline,
                        cursorColor = LevyraCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                AudioTextAction(
                    label = strings.autoEqPickFile,
                    enabled = true,
                    onClick = { picker.launch(AutoEqDocumentMimeTypes) }
                )

                val errorMessage = readError ?: if (parsed is AutoEqImporter.ParseResult.Error) {
                    if (parsed.error == AutoEqImporter.ParseError.TOO_LARGE) {
                        strings.autoEqInputTooLarge
                    } else {
                        strings.autoEqInvalidProfile
                    }
                } else {
                    null
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = LevyraOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                if (profile != null) {
                    AutoEqProfilePreview(profile = profile)
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = {
                            presetNameDirty = true
                            presetName = it.take(48)
                        },
                        singleLine = true,
                        label = { Text(strings.autoEqPresetName, color = LevyraMuted, fontSize = 12.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LevyraText,
                            unfocusedTextColor = LevyraText,
                            focusedBorderColor = LevyraCyan.copy(alpha = 0.7f),
                            unfocusedBorderColor = LevyraAdaptiveHairline,
                            cursorColor = LevyraCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AutoEqDialogButton(
                        label = strings.cancel,
                        primary = false,
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )
                    AutoEqDialogButton(
                        label = strings.autoEqSavePreset,
                        primary = false,
                        enabled = profile != null && presetName.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        onClick = { profile?.let { onSavePreset(presetName.trim(), it) } }
                    )
                    AutoEqDialogButton(
                        label = strings.autoEqApply,
                        primary = true,
                        enabled = profile != null,
                        modifier = Modifier.weight(1f),
                        onClick = { profile?.let(onApply) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoEqProfilePreview(profile: AutoEqImporter.ImportedProfile) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = LevyraAdaptiveCard,
        shape = CardShape,
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioCardHeader(
                title = profile.name ?: strings.audioPresetCustom,
                trailing = "${strings.preamp} ${decibels(profile.preampDb)}"
            )
            LevyraAudioPresets.bandFrequencyLabels.forEachIndexed { index, label ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("$label Hz", color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(
                        decibels(profile.bandGainDb.getOrElse(index) { 0f }),
                        color = LevyraText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (profile.clamped || profile.interpolated || profile.skippedPoints > 0) {
                Text(
                    strings.autoEqAdjustedNotice,
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AutoEqDialogButton(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (primary) LevyraCyan else LevyraAdaptiveChip,
        shape = ChipShape,
        border = if (primary) null else BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
            Text(
                label,
                color = if (primary) LevyraBlack else LevyraText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

private sealed interface AutoEqFileRead {
    data class Success(val text: String) : AutoEqFileRead
    data object TooLarge : AutoEqFileRead
    data object Unreadable : AutoEqFileRead
}

private val AutoEqDocumentMimeTypes = arrayOf("text/plain", "application/octet-stream", "*/*")

private fun readBoundedAutoEqText(context: Context, uri: Uri): AutoEqFileRead {
    val limit = AutoEqImporter.MAX_INPUT_CHARS
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val reader = stream.reader(Charsets.UTF_8)
            val buffer = CharArray(limit + 1)
            var read = 0
            while (read <= limit) {
                val count = reader.read(buffer, read, buffer.size - read)
                if (count <= 0) break
                read += count
            }
            if (read > limit) AutoEqFileRead.TooLarge else AutoEqFileRead.Success(String(buffer, 0, read))
        } ?: AutoEqFileRead.Unreadable
    }.getOrElse { AutoEqFileRead.Unreadable }
}

@Composable
private fun AudioPanelHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(LevyraMuted.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun AudioPanelHeader(
    title: String,
    subtitle: String,
    volumeLabel: String,
    closeLabel: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(LevyraCyan.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = LevyraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(
                subtitle,
                color = LevyraMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Rounded.GraphicEq, null, tint = LevyraMuted, modifier = Modifier.size(15.dp))
            Text(volumeLabel, color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = closeLabel, tint = LevyraText)
        }
    }
}

@Composable
private fun AudioSectionLabel(text: String) {
    Text(
        text,
        color = LevyraMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(top = 10.dp, start = 4.dp)
    )
}

@Composable
private fun AudioCard(content: @Composable () -> Unit) {
    Surface(
        color = LevyraAdaptiveCard,
        shape = CardShape,
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            content()
        }
    }
}

@Composable
private fun AudioEqualizerSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text(
                subtitle,
                color = LevyraMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LevyraBlack,
                checkedTrackColor = LevyraCyan,
                uncheckedThumbColor = LevyraMuted,
                uncheckedTrackColor = LevyraAdaptiveTrack
            ),
            modifier = Modifier.semantics { contentDescription = label }
        )
    }
}

@Composable
private fun AudioCardHeader(title: String, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = LevyraText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            trailing,
            color = LevyraMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f, fill = false)
        )
    }
}

@Composable
private fun AudioToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = LevyraAdaptiveCard,
        shape = CardShape,
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LevyraBlack,
                    checkedTrackColor = LevyraCyan,
                    uncheckedThumbColor = LevyraMuted,
                    uncheckedTrackColor = LevyraAdaptiveTrack
                )
            )
        }
    }
}

@Composable
private fun AudioSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    icon: Boolean = false,
    onValue: (Float) -> Unit
) {
    Surface(
        color = LevyraAdaptiveCard,
        shape = CardShape,
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (icon) {
                        Icon(Icons.Rounded.SurroundSound, null, tint = LevyraMuted, modifier = Modifier.size(16.dp))
                    }
                    Text(title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
                Text(valueLabel, color = LevyraCyan, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Slider(
                value = value.coerceIn(range.start, range.endInclusive),
                onValueChange = onValue,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = LevyraCyan,
                    activeTrackColor = LevyraCyan,
                    inactiveTrackColor = LevyraAdaptiveTrack
                ),
                modifier = Modifier.semantics { contentDescription = title }
            )
        }
    }
}

@Composable
private fun AudioPresetChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.18f) else LevyraAdaptiveChip,
        shape = ChipShape,
        border = BorderStroke(1.dp, if (selected) LevyraCyan.copy(alpha = 0.7f) else LevyraAdaptiveHairline),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                label,
                color = if (selected) LevyraCyan else LevyraText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AudioQualityRow(
    selected: String,
    labels: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEach { (label, quality) ->
            val isSelected = selected.equals(quality, ignoreCase = true)
            Surface(
                color = if (isSelected) LevyraCyan.copy(alpha = 0.18f) else LevyraAdaptiveChip,
                shape = ChipShape,
                border = BorderStroke(1.dp, if (isSelected) LevyraCyan.copy(alpha = 0.7f) else LevyraAdaptiveHairline),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable { onSelect(quality) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        color = if (isSelected) LevyraCyan else LevyraText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioTextAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = Color.Transparent,
            shape = ChipShape,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(label, color = LevyraCyan, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun EqualizerCurve(
    levels: List<Int>,
    enabled: Boolean,
    bandsLabel: String,
    onBandLevel: (Int, Int) -> Unit
) {
    val bandCount = LevyraAudioPresets.bandCount
    val safeLevels = remember(levels) {
        if (levels.size == bandCount) levels else LevyraAudioPresets.flatLevels
    }
    var activeBand by remember { mutableIntStateOf(-1) }
    val handleRadiusPx = with(LocalDensity.current) { EqualizerHandleRadius.toPx() }
    val curveColor = LevyraCyan
    val gridColor = LevyraMuted.copy(alpha = 0.28f)
    val handleFill = LevyraPanel
    val density = LocalDensity.current

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AudioCardHeader(
            title = bandsLabel,
            trailing = if (activeBand in 0 until bandCount) {
                "${LevyraAudioPresets.bandFrequencyLabels[activeBand]} Hz · " +
                    decibels(LevyraAudioPresets.bandDb(safeLevels[activeBand]))
            } else {
                "±${LevyraAudioPresets.maxBandDb.roundToInt()} dB"
            }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (LevyraIsLight) LevyraBlack.copy(alpha = 0.05f) else LevyraBlack.copy(alpha = 0.35f))
                .border(1.dp, LevyraAdaptiveHairline, RoundedCornerShape(16.dp))
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .pointerInput(enabled, bandCount) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = { offset ->
                            val band = bandAt(offset.x, size.width, bandCount)
                            activeBand = band
                            onBandLevel(band, bandLevelAt(offset.y, size.height.toFloat(), handleRadiusPx))
                            tryAwaitRelease()
                            activeBand = -1
                        }
                    )
                }
                .pointerInput(enabled, bandCount) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset -> activeBand = bandAt(offset.x, size.width, bandCount) },
                        onDragEnd = { activeBand = -1 },
                        onDragCancel = { activeBand = -1 }
                    ) { change, _ ->
                        val band = bandAt(change.position.x, size.width, bandCount)
                        activeBand = band
                        onBandLevel(band, bandLevelAt(change.position.y, size.height.toFloat(), handleRadiusPx))
                        change.consume()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawEqualizerCurve(
                    levels = safeLevels,
                    curveColor = curveColor,
                    gridColor = gridColor,
                    handleFill = handleFill,
                    activeBand = activeBand,
                    handleRadius = handleRadiusPx,
                    strokeWidth = with(density) { 2.dp.toPx() }
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(modifier = Modifier.fillMaxSize()) {
                    safeLevels.forEachIndexed { index, level ->
                    val frequency = LevyraAudioPresets.bandFrequencyLabels[index]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .progressSemantics(level.toFloat(), -100f..100f, 0)
                                .semantics {
                                    contentDescription = "$frequency Hz"
                                    stateDescription = decibels(LevyraAudioPresets.bandDb(level))
                                    if (enabled) {
                                        setProgress { target ->
                                            onBandLevel(index, target.roundToInt())
                                            true
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(modifier = Modifier.fillMaxWidth()) {
                LevyraAudioPresets.bandFrequencyLabels.forEach { frequency ->
                    Text(
                        frequency,
                        color = LevyraMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun bandLevelAt(y: Float, height: Float, handleRadius: Float): Int {
    val half = height / 2f
    val usable = (half - handleRadius * 2f).coerceAtLeast(1f)
    val fraction = 0.5f - ((half - y) / usable) / 2f
    return LevyraAudioPresets.bandLevelFromVerticalFraction(fraction)
}

private fun bandAt(x: Float, width: Int, bandCount: Int): Int {
    if (width <= 0) return 0
    val slot = width.toFloat() / bandCount
    return (x / slot).toInt().coerceIn(0, bandCount - 1)
}

private fun DrawScope.drawEqualizerCurve(
    levels: List<Int>,
    curveColor: Color,
    gridColor: Color,
    handleFill: Color,
    activeBand: Int,
    handleRadius: Float,
    strokeWidth: Float
) {
    val slot = size.width / levels.size
    val zeroY = size.height / 2f
    drawLine(gridColor, Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = strokeWidth / 2f)

    val points = levels.mapIndexed { index, level ->
        val x = slot * index + slot / 2f
        val y = zeroY - (level / 100f) * (size.height / 2f - handleRadius * 2f)
        Offset(x, y)
    }
    val curve = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (index in 0 until points.size - 1) {
            val current = points[index]
            val next = points[index + 1]
            val midX = (current.x + next.x) / 2f
            cubicTo(midX, current.y, midX, next.y, next.x, next.y)
        }
    }
    val fill = Path().apply {
        addPath(curve)
        lineTo(points.last().x, zeroY)
        lineTo(points.first().x, zeroY)
        close()
    }
    drawPath(
        path = fill,
        brush = Brush.verticalGradient(
            listOf(curveColor.copy(alpha = 0.22f), curveColor.copy(alpha = 0.04f))
        )
    )
    drawPath(path = curve, color = curveColor, style = Stroke(width = strokeWidth))
    points.forEachIndexed { index, point ->
        val radius = if (index == activeBand) handleRadius * 1.4f else handleRadius
        drawCircle(handleFill, radius, point)
        drawCircle(curveColor, radius, point, style = Stroke(width = strokeWidth))
    }
}

private fun decibels(value: Float): String = String.format(Locale.US, "%+.1f dB", value)

private fun trimAudioSpeed(value: Float): String {
    val rounded = (value * 100f).roundToInt() / 100f
    return if (abs(rounded - rounded.roundToInt()) < 0.005f) {
        rounded.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
    }
}
