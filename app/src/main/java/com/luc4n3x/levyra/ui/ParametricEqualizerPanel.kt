package com.luc4n3x.levyra.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.luc4n3x.levyra.domain.ParametricEqBand
import com.luc4n3x.levyra.domain.ParametricEqProfile
import com.luc4n3x.levyra.domain.ParametricEqualizer
import com.luc4n3x.levyra.domain.ParametricFilterType
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.ParametricEqCopy
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

internal enum class EqualizerEditorMode { GRAPHIC, PARAMETRIC }

@Composable
internal fun EqualizerModeSelector(
    selected: EqualizerEditorMode,
    copy: ParametricEqCopy,
    onSelect: (EqualizerEditorMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            EqualizerEditorMode.GRAPHIC to copy.graphicEq,
            EqualizerEditorMode.PARAMETRIC to copy.parametricEq
        ).forEach { (mode, label) ->
            val active = selected == mode
            Surface(
                color = if (active) LevyraCyan.copy(alpha = 0.16f) else LevyraAdaptiveChip,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (active) LevyraCyan.copy(alpha = 0.6f) else LevyraAdaptiveHairline),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(mode) })
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
                    Text(
                        text = label,
                        color = if (active) LevyraCyan else LevyraText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun ParametricEqualizerCard(
    enabled: Boolean,
    profile: ParametricEqProfile?,
    savedProfiles: List<ParametricEqProfile>,
    copy: ParametricEqCopy,
    onEnabled: (Boolean) -> Unit,
    onSelectProfile: (String) -> Unit,
    onPreamp: (Float) -> Unit,
    onBand: (Int, ParametricEqBand) -> Unit,
    onAddBand: () -> Unit,
    onRemoveBand: (Int) -> Unit,
    onReset: () -> Unit,
    onSave: (String, ParametricEqProfile) -> Unit
) {
    val activeProfile = profile ?: ParametricEqualizer.defaultProfile
    var showSaveDialog by remember { mutableStateOf(false) }
    Surface(
        color = LevyraAdaptiveCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(copy.parametricEq, color = LevyraText, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(copy.parametricSubtitle, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LevyraBlack,
                        checkedTrackColor = LevyraCyan,
                        uncheckedThumbColor = LevyraMuted,
                        uncheckedTrackColor = LevyraAdaptiveTrack
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(copy.activeProfile, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = activeProfile.name,
                    color = LevyraText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 10.dp)
                )
            }

            if (savedProfiles.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedProfiles, key = { it.id }) { saved ->
                        ParametricChip(
                            label = saved.name,
                            selected = saved.id == activeProfile.id,
                            onClick = { onSelectProfile(saved.id) }
                        )
                    }
                }
            }

            ParametricSliderRow(
                label = LocalLevyraStrings.current.preamp,
                valueLabel = dbLabel(activeProfile.preampDb),
                value = activeProfile.preampDb,
                valueRange = ParametricEqualizer.MIN_PREAMP_DB..ParametricEqualizer.MAX_PREAMP_DB,
                steps = 59,
                enabled = enabled,
                onValue = { onPreamp((it * 2f).roundToInt() / 2f) }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${copy.bands} · ${activeProfile.bands.size}/${ParametricEqualizer.MAX_BANDS}",
                    color = LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onAddBand,
                    enabled = enabled && activeProfile.bands.size < ParametricEqualizer.MAX_BANDS,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = copy.addBand, tint = LevyraCyan)
                }
            }

            activeProfile.bands.forEachIndexed { index, band ->
                ParametricBandEditor(
                    index = index,
                    band = band,
                    enabled = enabled,
                    canRemove = activeProfile.bands.size > 1,
                    copy = copy,
                    onBand = { onBand(index, it) },
                    onRemove = { onRemoveBand(index) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ParametricAction(
                    label = copy.reset,
                    icon = Icons.Rounded.RestartAlt,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = onReset
                )
                ParametricAction(
                    label = copy.saveProfile,
                    icon = Icons.Rounded.Save,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = { showSaveDialog = true }
                )
            }
        }
    }

    if (showSaveDialog) {
        ParametricSaveDialog(
            initialName = activeProfile.name,
            copy = copy,
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                onSave(name, activeProfile)
                showSaveDialog = false
            }
        )
    }
}

@Composable
private fun ParametricBandEditor(
    index: Int,
    band: ParametricEqBand,
    enabled: Boolean,
    canRemove: Boolean,
    copy: ParametricEqCopy,
    onBand: (ParametricEqBand) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = LevyraAdaptiveChip,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${index + 1} · ${filterLabel(band.filterType, copy)} · ${frequencyLabel(band.frequencyHz)}",
                    color = LevyraText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = band.enabled,
                    onCheckedChange = { onBand(band.copy(enabled = it)) },
                    enabled = enabled,
                    colors = SwitchDefaults.colors(checkedTrackColor = LevyraCyan)
                )
                IconButton(onClick = onRemove, enabled = enabled && canRemove, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = copy.removeBand, tint = LevyraMuted)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ParametricFilterType.entries.forEach { type ->
                    ParametricChip(
                        label = filterLabel(type, copy),
                        selected = band.filterType == type,
                        enabled = enabled && band.enabled,
                        modifier = Modifier.weight(1f),
                        onClick = { onBand(band.copy(filterType = type)) }
                    )
                }
            }

            val frequencyPosition = frequencyPosition(band.frequencyHz)
            ParametricSliderRow(
                label = copy.frequency,
                valueLabel = frequencyLabel(band.frequencyHz),
                value = frequencyPosition,
                valueRange = 0f..1f,
                steps = 0,
                enabled = enabled && band.enabled,
                onValue = { onBand(band.copy(frequencyHz = frequencyAt(it))) }
            )
            ParametricSliderRow(
                label = copy.gain,
                valueLabel = dbLabel(band.gainDb),
                value = band.gainDb,
                valueRange = -ParametricEqualizer.MAX_GAIN_DB..ParametricEqualizer.MAX_GAIN_DB,
                steps = 95,
                enabled = enabled && band.enabled,
                onValue = { onBand(band.copy(gainDb = (it * 2f).roundToInt() / 2f)) }
            )
            ParametricSliderRow(
                label = copy.qFactor,
                valueLabel = String.format(Locale.US, "%.2f", band.q),
                value = band.q.coerceIn(ParametricEqualizer.MIN_Q, ParametricEqualizer.MAX_Q),
                valueRange = ParametricEqualizer.MIN_Q..ParametricEqualizer.MAX_Q,
                steps = 198,
                enabled = enabled && band.enabled,
                onValue = { onBand(band.copy(q = (it * 10f).roundToInt() / 10f)) }
            )
        }
    }
}

@Composable
private fun ParametricSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean,
    onValue: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row {
            Text(label, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(valueLabel, color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValue,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = LevyraCyan,
                activeTrackColor = LevyraCyan,
                inactiveTrackColor = LevyraAdaptiveTrack
            )
        )
    }
}

@Composable
private fun ParametricChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.16f) else LevyraAdaptiveChip,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) LevyraCyan.copy(alpha = 0.55f) else LevyraAdaptiveHairline),
        modifier = modifier
            .heightIn(min = 44.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                label,
                color = if (selected) LevyraCyan else LevyraText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ParametricAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = LevyraAdaptiveChip,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
            Text(label, color = LevyraText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 7.dp))
        }
    }
}

@Composable
private fun ParametricSaveDialog(
    initialName: String,
    copy: ParametricEqCopy,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName.take(ParametricEqualizer.MAX_NAME_CHARS)) }
    val strings = LocalLevyraStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = LevyraPanel,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, LevyraAdaptiveHairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(copy.saveProfile, color = LevyraText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(ParametricEqualizer.MAX_NAME_CHARS) },
                    singleLine = true,
                    label = { Text(strings.autoEqPresetName) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LevyraText,
                        unfocusedTextColor = LevyraText,
                        focusedBorderColor = LevyraCyan,
                        unfocusedBorderColor = LevyraAdaptiveHairline,
                        cursorColor = LevyraCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ParametricDialogButton(strings.cancel, primary = false, enabled = true, Modifier.weight(1f), onDismiss)
                    ParametricDialogButton(
                        copy.saveProfile,
                        primary = true,
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        onClick = { onSave(name.trim()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ParametricDialogButton(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (primary) LevyraCyan else LevyraAdaptiveChip,
        shape = RoundedCornerShape(14.dp),
        border = if (primary) null else BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            Text(label, color = if (primary) LevyraBlack else LevyraText, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun filterLabel(type: ParametricFilterType, copy: ParametricEqCopy): String = when (type) {
    ParametricFilterType.PEAK -> copy.peak
    ParametricFilterType.LOW_SHELF -> copy.lowShelf
    ParametricFilterType.HIGH_SHELF -> copy.highShelf
}

private fun frequencyPosition(frequencyHz: Float): Float {
    val bounded = frequencyHz.coerceIn(MANUAL_MIN_FREQUENCY_HZ, MANUAL_MAX_FREQUENCY_HZ)
    return ((log10(bounded) - log10(MANUAL_MIN_FREQUENCY_HZ)) /
        (log10(MANUAL_MAX_FREQUENCY_HZ) - log10(MANUAL_MIN_FREQUENCY_HZ))).coerceIn(0f, 1f)
}

private fun frequencyAt(position: Float): Float {
    val lower = log10(MANUAL_MIN_FREQUENCY_HZ)
    val upper = log10(MANUAL_MAX_FREQUENCY_HZ)
    val raw = 10f.pow(lower + (upper - lower) * position.coerceIn(0f, 1f))
    return when {
        raw < 100f -> raw.roundToInt().toFloat()
        raw < 1_000f -> (raw / 5f).roundToInt() * 5f
        else -> (raw / 10f).roundToInt() * 10f
    }
}

private fun frequencyLabel(value: Float): String = if (value >= 1_000f) {
    val kilohertz = value / 1_000f
    val pattern = if (kilohertz >= 10f || kilohertz % 1f == 0f) "%.0f kHz" else "%.1f kHz"
    String.format(Locale.US, pattern, kilohertz)
} else {
    "${value.roundToInt()} Hz"
}

private fun dbLabel(value: Float): String = String.format(Locale.US, "%+.1f dB", value)

private const val MANUAL_MIN_FREQUENCY_HZ = 20f
private const val MANUAL_MAX_FREQUENCY_HZ = 24_000f
