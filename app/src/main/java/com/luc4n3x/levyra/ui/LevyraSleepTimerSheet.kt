package com.luc4n3x.levyra.ui

import android.app.TimePickerDialog
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.domain.LevyraAutomationSettings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.automationCopy
import com.luc4n3x.levyra.ui.i18n.systemPlayerCopy
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val SleepSheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)

@Composable
internal fun LevyraSleepTimerSheet(
    state: LevyraUiState,
    onAutomationSettings: (LevyraAutomationSettings) -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.systemPlayerCopy()
    val blocker = remember { MutableInteractionSource() }
    var remainingSeconds by remember(state.sleepTimerDeadlineElapsedRealtimeMs) {
        mutableLongStateOf(timerRemainingSeconds(state.sleepTimerDeadlineElapsedRealtimeMs))
    }

    LaunchedEffect(state.sleepTimerDeadlineElapsedRealtimeMs) {
        while (state.sleepTimerDeadlineElapsedRealtimeMs > 0L) {
            remainingSeconds = timerRemainingSeconds(state.sleepTimerDeadlineElapsedRealtimeMs)
            if (remainingSeconds <= 0L) break
            delay(1_000L)
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LevyraBlack.copy(alpha = 0.72f))
                .clickable(interactionSource = blocker, indication = null, onClick = onClose),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = LevyraPanel,
                shape = SleepSheetShape,
                border = BorderStroke(1.dp, LevyraAdaptiveHairline),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .clickable(interactionSource = blocker, indication = null) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(38.dp)
                            .height(4.dp)
                            .background(LevyraGlassBorder, CircleShape)
                    )
                    SleepTimerHeader(
                        title = strings.sleepTimer,
                        subtitle = copy.sleepChooseDuration,
                        closeLabel = strings.close,
                        onClose = onClose
                    )
                    if (state.sleepTimerMinutes > 0 || state.sleepTimerEndOfTrack) {
                        SleepTimerActiveCard(
                            state = state,
                            remainingSeconds = remainingSeconds,
                            onAddFifteen = {
                                val remainingMinutes = ((remainingSeconds + 59L) / 60L).toInt()
                                onSelectMinutes((remainingMinutes + 15).coerceIn(15, 720))
                            },
                            onCancel = onCancel
                        )
                    }
                    SleepTimerPresets(
                        selectedMinutes = state.sleepTimerMinutes,
                        endOfTrack = state.sleepTimerEndOfTrack,
                        onSelectMinutes = onSelectMinutes,
                        onSelectEndOfTrack = onSelectEndOfTrack
                    )
                    SleepTimerAutomationCard(
                        automation = state.automationSettings,
                        onChange = onAutomationSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepTimerHeader(
    title: String,
    subtitle: String,
    closeLabel: String,
    onClose: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = LevyraCyan.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = LevyraCyan)
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LevyraText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = LevyraMuted, fontSize = 12.sp)
        }
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = closeLabel, tint = LevyraMuted)
        }
    }
}

@Composable
private fun SleepTimerActiveCard(
    state: LevyraUiState,
    remainingSeconds: Long,
    onAddFifteen: () -> Unit,
    onCancel: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.systemPlayerCopy()
    val totalSeconds = (state.sleepTimerTotalMs / 1_000L).coerceAtLeast(1L)
    val fraction = if (state.sleepTimerEndOfTrack) 1f else {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    }
    val endAt = if (state.sleepTimerEndOfTrack) {
        strings.sleepTimerEndOfTrack
    } else {
        "${copy.sleepEndsAt} ${formatSleepEndClock(state.sleepTimerDeadlineElapsedRealtimeMs, strings.code)}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        LevyraViolet.copy(alpha = 0.12f),
                        LevyraCyan.copy(alpha = 0.1f),
                        LevyraCyan.copy(alpha = 0.025f)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .border(1.dp, LevyraGlassBorder, RoundedCornerShape(24.dp))
            .padding(17.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(86.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 5.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    drawArc(
                        color = LevyraGlassBorder,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(LevyraCyan, LevyraViolet, LevyraCyan)),
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Icon(
                    imageVector = if (state.sleepTimerEndOfTrack) Icons.Rounded.Bedtime else Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = LevyraCyan,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(copy.sleepActive, color = LevyraCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    text = if (state.sleepTimerEndOfTrack) strings.sleepTimerEndOfTrack else formatTimerClock(remainingSeconds),
                    color = LevyraText,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(endAt, color = LevyraMuted, fontSize = 11.sp)
                if (state.sleepTimerFadeMs > 0L) {
                    Text(copy.sleepFadeActive, color = LevyraMuted, fontSize = 11.sp)
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(top = 76.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!state.sleepTimerEndOfTrack) {
                TextButton(onClick = onAddFifteen, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
                    Text(copy.sleepAddFifteen, color = LevyraCyan, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onCancel, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
                Text(strings.sleepTimerCancel, color = LevyraMuted, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SleepTimerPresets(
    selectedMinutes: Int,
    endOfTrack: Boolean,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(15, 30, 45, 60).forEach { minutes ->
                SleepPreset(
                    label = minutes.toString(),
                    suffix = "min",
                    selected = selectedMinutes == minutes && !endOfTrack,
                    onClick = { onSelectMinutes(minutes) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelectEndOfTrack)
                .sizeIn(minHeight = 54.dp),
            shape = RoundedCornerShape(18.dp),
            color = if (endOfTrack) LevyraCyan.copy(alpha = 0.11f) else LevyraBlack.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, if (endOfTrack) LevyraCyan.copy(alpha = 0.35f) else LevyraGlassBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = if (endOfTrack) LevyraCyan else LevyraMuted)
                Spacer(Modifier.width(12.dp))
                Text(strings.sleepTimerEndOfTrack, color = LevyraText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SleepPreset(
    label: String,
    suffix: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .sizeIn(minHeight = 64.dp),
        color = if (selected) LevyraCyan.copy(alpha = 0.12f) else LevyraBlack.copy(alpha = 0.28f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (selected) LevyraCyan.copy(alpha = 0.38f) else LevyraGlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = if (selected) LevyraCyan else LevyraText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(suffix, color = LevyraMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun SleepTimerAutomationCard(
    automation: LevyraAutomationSettings,
    onChange: (LevyraAutomationSettings) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalLevyraStrings.current
    val copy = strings.automationCopy()
    val bedtime = automation.bedtime
    Surface(
        color = LevyraBlack.copy(alpha = 0.24f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, LevyraGlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(copy.automation.uppercase(Locale.ROOT), color = LevyraMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            SleepAutomationToggle(
                title = copy.fadeOut,
                subtitle = copy.fadeOutSubtitle,
                checked = automation.sleepFadeOutEnabled,
                onChecked = { onChange(automation.copy(sleepFadeOutEnabled = it)) }
            )
            SleepAutomationToggle(
                title = copy.bedtime,
                subtitle = copy.bedtimeSubtitle,
                checked = bedtime.enabled,
                onChecked = { onChange(automation.copy(bedtime = bedtime.copy(enabled = it))) }
            )
            if (bedtime.enabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    onChange(
                                        automation.copy(
                                            bedtime = bedtime.copy(startMinuteOfDay = hour * 60 + minute)
                                        )
                                    )
                                },
                                bedtime.startMinuteOfDay / 60,
                                bedtime.startMinuteOfDay % 60,
                                true
                            ).show()
                        }
                        .sizeIn(minHeight = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(copy.startTime, color = LevyraMuted, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text(formatBedtimeClock(bedtime.startMinuteOfDay), color = LevyraText, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15, 30, 45, 60).forEach { minutes ->
                        TextButton(
                            onClick = {
                                onChange(automation.copy(bedtime = bedtime.copy(durationMinutes = minutes)))
                            },
                            modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp)
                        ) {
                            Text(
                                "$minutes",
                                color = if (bedtime.durationMinutes == minutes) LevyraCyan else LevyraMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepAutomationToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = LevyraMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

private fun timerRemainingSeconds(deadlineElapsedRealtimeMs: Long): Long =
    if (deadlineElapsedRealtimeMs > 0L) {
        ((deadlineElapsedRealtimeMs - SystemClock.elapsedRealtime()) / 1_000L).coerceAtLeast(0L)
    } else {
        0L
    }

private fun formatTimerClock(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600L
    val minutes = totalSeconds % 3_600L / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

private fun formatSleepEndClock(deadlineElapsedRealtimeMs: Long, languageCode: String): String {
    if (deadlineElapsedRealtimeMs <= 0L) return "—"
    val remaining = (deadlineElapsedRealtimeMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
    val locale = Locale.forLanguageTag(languageCode.replace('_', '-'))
    return DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(System.currentTimeMillis() + remaining))
}

private fun formatBedtimeClock(startMinuteOfDay: Int): String {
    val hour = (startMinuteOfDay / 60).coerceIn(0, 23)
    val minute = (startMinuteOfDay % 60).coerceIn(0, 59)
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
