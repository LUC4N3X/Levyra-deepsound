package com.luc4n3x.levyra.ui.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.MAX_LYRICS_OFFSET_MS
import com.luc4n3x.levyra.data.MAX_LYRICS_TRACK_OFFSET_MS
import com.luc4n3x.levyra.ui.components.LevyraExpressiveIconButton
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.LyricsSyncCopy
import com.luc4n3x.levyra.ui.i18n.lyricsSyncCopy
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import kotlin.math.roundToLong

private const val LYRICS_LATENCY_STEP_MS = 100L
private val LYRICS_SYNC_JUMPS_MS = longArrayOf(-10_000L, -LYRICS_OFFSET_JUMP_MS, LYRICS_OFFSET_JUMP_MS, 10_000L)
private const val LYRICS_SYNC_SLIDER_STEPS = (2 * MAX_LYRICS_TRACK_OFFSET_MS / LYRICS_OFFSET_STEP_MS - 1).toInt()

private val LyricsSyncNumberStyle = TextStyle(textDirection = TextDirection.Ltr)

fun lyricsOffsetIsolated(offsetMs: Long): String = "\u2066${formatLyricsOffsetSeconds(offsetMs)}\u2069"

internal fun lyricsSyncStateLabel(copy: LyricsSyncCopy, offsetMs: Long): String {
    val magnitude = lyricsOffsetIsolated(kotlin.math.abs(offsetMs)).replace("+", "")
    return when {
        offsetMs > 0L -> copy.laterState(magnitude)
        offsetMs < 0L -> copy.earlierState(magnitude)
        else -> copy.inSync
    }
}

@Composable
fun LyricsSyncQuickBar(
    offsetMs: Long,
    accent: Color,
    onAdjust: (Long) -> Unit,
    onReset: () -> Unit,
    onOpenAdvanced: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { lyricsSyncCopy(strings.code) }
    val haptics = LocalLevyraHaptics.current
    val adjusted = offsetMs != 0L
    val valueColor by animateColorAsState(
        targetValue = if (adjusted) accent else Color.White,
        label = "lyricsSyncValueColor"
    )
    val stateLabel = lyricsSyncStateLabel(copy, offsetMs)
    Surface(
        color = Color.Black.copy(alpha = 0.82f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (adjusted) 0.16f else 0.10f)),
        shape = CircleShape,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LyricsSyncIconButton(
                icon = Icons.Rounded.Remove,
                contentDescription = copy.earlierAction,
                enabled = offsetMs > -MAX_LYRICS_TRACK_OFFSET_MS,
                onClick = {
                    haptics.perform(LevyraHapticAction.SeekSnap)
                    onAdjust(-LYRICS_OFFSET_STEP_MS)
                }
            )
            Surface(
                onClick = onOpenAdvanced,
                color = Color.Transparent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .sizeIn(minWidth = 88.dp, minHeight = 48.dp)
                    .semantics {
                        contentDescription = copy.advanced
                        stateDescription = stateLabel
                        liveRegion = LiveRegionMode.Polite
                    }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formatLyricsOffsetSeconds(offsetMs),
                        style = LyricsSyncNumberStyle,
                        color = valueColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = copy.title,
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 132.dp)
                    )
                }
            }
            LyricsSyncIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = copy.laterAction,
                enabled = offsetMs < MAX_LYRICS_TRACK_OFFSET_MS,
                onClick = {
                    haptics.perform(LevyraHapticAction.SeekSnap)
                    onAdjust(LYRICS_OFFSET_STEP_MS)
                }
            )
            AnimatedVisibility(
                visible = adjusted,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                LyricsSyncIconButton(
                    icon = Icons.Rounded.RestartAlt,
                    contentDescription = strings.lyricsOffsetReset,
                    onClick = {
                        haptics.perform(LevyraHapticAction.Confirm)
                        onReset()
                    }
                )
            }
            LyricsSyncIconButton(
                icon = Icons.Rounded.Tune,
                contentDescription = copy.advanced,
                onClick = onOpenAdvanced
            )
        }
    }
}

@Composable
private fun LyricsSyncIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = Color.White
) {
    LevyraExpressiveIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = tint,
            disabledContentColor = tint.copy(alpha = 0.32f)
        ),
        modifier = Modifier.size(48.dp)
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LyricsSyncSheet(
    trackOffsetMs: Long,
    latencyOffsetMs: Long,
    latencyDeviceName: String?,
    latencyResettable: Boolean,
    onAdjustTrack: (Long) -> Unit,
    onSetTrack: (Long) -> Unit,
    onAdjustLatency: (Long) -> Unit,
    onResetLatency: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { lyricsSyncCopy(strings.code) }
    val haptics = LocalLevyraHaptics.current
    val focusManager = LocalFocusManager.current
    val primary = MaterialTheme.colorScheme.primary
    val sliderState = rememberSliderState(
        value = trackOffsetMs / 1_000f,
        steps = LYRICS_SYNC_SLIDER_STEPS,
        trackRange = -(MAX_LYRICS_TRACK_OFFSET_MS / 1_000f)..(MAX_LYRICS_TRACK_OFFSET_MS / 1_000f)
    )
    LaunchedEffect(trackOffsetMs) {
        val target = trackOffsetMs / 1_000f
        if (sliderState.value != target) sliderState.value = target
    }
    var manualInput by rememberSaveable { mutableStateOf("") }
    var manualInvalid by rememberSaveable { mutableStateOf(false) }
    fun applyManualInput() {
        val parsed = parseLyricsOffsetSeconds(manualInput)
        if (parsed == null) {
            manualInvalid = true
            return
        }
        manualInvalid = false
        manualInput = ""
        focusManager.clearFocus()
        haptics.perform(LevyraHapticAction.Confirm)
        onSetTrack(parsed)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(copy.title, color = LevyraText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(copy.hint, color = LevyraMuted, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            }
            LyricsSyncSectionCard {
                Text(
                    text = "${copy.trackScope} · ${copy.trackScopeHint}",
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val stateLabel = lyricsSyncStateLabel(copy, trackOffsetMs)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics {
                            contentDescription = stateLabel
                            liveRegion = LiveRegionMode.Polite
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatLyricsOffsetSeconds(trackOffsetMs),
                        style = LyricsSyncNumberStyle,
                        color = if (trackOffsetMs != 0L) primary else LevyraText,
                        fontSize = 40.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stateLabel,
                        color = LevyraMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
                Slider(
                    state = sliderState,
                    onValueChange = { seconds ->
                        sliderState.value = seconds
                        val snapped = snapLyricsTrackOffset((seconds * 1_000f).roundToLong())
                        if (snapped != trackOffsetMs) {
                            if (snapped == 0L || trackOffsetMs == 0L) haptics.perform(LevyraHapticAction.SeekSnap)
                            onSetTrack(snapped)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { stateDescription = stateLabel }
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("−30 s", color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, style = LyricsSyncNumberStyle)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("0", color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, style = LyricsSyncNumberStyle)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("+30 s", color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, style = LyricsSyncNumberStyle)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LyricsSyncStepButton(
                        label = copy.earlierAction,
                        icon = Icons.Rounded.Remove,
                        caption = "−0.5 s",
                        enabled = trackOffsetMs > -MAX_LYRICS_TRACK_OFFSET_MS,
                        onClick = {
                            haptics.perform(LevyraHapticAction.SeekSnap)
                            onAdjustTrack(-LYRICS_OFFSET_STEP_MS)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    LyricsSyncStepButton(
                        label = copy.laterAction,
                        icon = Icons.Rounded.Add,
                        caption = "+0.5 s",
                        enabled = trackOffsetMs < MAX_LYRICS_TRACK_OFFSET_MS,
                        onClick = {
                            haptics.perform(LevyraHapticAction.SeekSnap)
                            onAdjustTrack(LYRICS_OFFSET_STEP_MS)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LYRICS_SYNC_JUMPS_MS.forEach { jumpMs ->
                        val label = formatLyricsOffsetSeconds(jumpMs).replace(".0 s", " s")
                        val description = if (jumpMs < 0L) {
                            "${copy.earlierAction} $label"
                        } else {
                            "${copy.laterAction} $label"
                        }
                        val jumpEnabled = if (jumpMs < 0L) {
                            trackOffsetMs > -MAX_LYRICS_TRACK_OFFSET_MS
                        } else {
                            trackOffsetMs < MAX_LYRICS_TRACK_OFFSET_MS
                        }
                        Surface(
                            onClick = {
                                haptics.perform(LevyraHapticAction.SeekSnap)
                                onAdjustTrack(jumpMs)
                            },
                            enabled = jumpEnabled,
                            color = MaterialTheme.colorScheme.secondaryContainer
                                .copy(alpha = if (jumpEnabled) 1f else 0.38f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                .copy(alpha = if (jumpEnabled) 1f else 0.38f),
                            shape = CircleShape,
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .semantics { contentDescription = description }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, style = LyricsSyncNumberStyle)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { value ->
                            manualInput = value.take(8)
                            manualInvalid = false
                        },
                        label = { Text(copy.manualLabel) },
                        placeholder = { Text(formatLyricsOffsetInput(trackOffsetMs)) },
                        isError = manualInvalid,
                        supportingText = if (manualInvalid) {
                            { Text(copy.invalidInput) }
                        } else {
                            null
                        },
                        singleLine = true,
                        textStyle = LyricsSyncNumberStyle,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { applyManualInput() }),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = { applyManualInput() },
                        enabled = manualInput.isNotBlank(),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(copy.apply, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(
                    onClick = {
                        haptics.perform(LevyraHapticAction.Confirm)
                        onSetTrack(0L)
                    },
                    enabled = trackOffsetMs != 0L,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.lyricsOffsetReset, fontWeight = FontWeight.Bold)
                }
            }
            LyricsSyncSectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(copy.deviceScope, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text(
                            text = listOfNotNull(latencyDeviceName?.takeIf(String::isNotBlank), copy.deviceScopeHint)
                                .joinToString(" · "),
                            color = LevyraMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    LyricsSyncIconButton(
                        icon = Icons.Rounded.Remove,
                        contentDescription = copy.earlierAction,
                        enabled = latencyOffsetMs > -MAX_LYRICS_OFFSET_MS,
                        tint = LevyraText,
                        onClick = {
                            haptics.perform(LevyraHapticAction.SeekSnap)
                            onAdjustLatency(-LYRICS_LATENCY_STEP_MS)
                        }
                    )
                    Text(
                        text = formatLyricsOffsetSeconds(latencyOffsetMs),
                        style = LyricsSyncNumberStyle,
                        color = if (latencyOffsetMs != 0L) primary else LevyraText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .widthIn(min = 88.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    LyricsSyncIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = copy.laterAction,
                        enabled = latencyOffsetMs < MAX_LYRICS_OFFSET_MS,
                        tint = LevyraText,
                        onClick = {
                            haptics.perform(LevyraHapticAction.SeekSnap)
                            onAdjustLatency(LYRICS_LATENCY_STEP_MS)
                        }
                    )
                    LyricsSyncIconButton(
                        icon = Icons.Rounded.RestartAlt,
                        contentDescription = strings.lyricsOffsetReset,
                        enabled = latencyResettable,
                        tint = LevyraText,
                        onClick = {
                            haptics.perform(LevyraHapticAction.Confirm)
                            onResetLatency()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsSyncSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun LyricsSyncStepButton(
    label: String,
    icon: ImageVector,
    caption: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, fontSize = 11.sp, fontWeight = FontWeight.Medium, style = LyricsSyncNumberStyle)
        }
    }
}
