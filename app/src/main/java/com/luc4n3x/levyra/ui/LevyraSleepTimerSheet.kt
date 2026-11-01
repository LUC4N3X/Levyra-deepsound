package com.luc4n3x.levyra.ui

import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.domain.LevyraAutomationSettings
import com.luc4n3x.levyra.domain.PlaybackSleepTimerMode
import com.luc4n3x.levyra.domain.SLEEP_WITH_MUSIC_DURATION_PRESETS
import com.luc4n3x.levyra.domain.SleepWithMusicSettings
import com.luc4n3x.levyra.ui.i18n.LevyraSmartListeningCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.automationCopy
import com.luc4n3x.levyra.ui.i18n.smartListeningCopy
import com.luc4n3x.levyra.ui.i18n.systemPlayerCopy
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import com.luc4n3x.levyra.viewmodel.SmartListeningUiState
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

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
    val owner = LocalViewModelStoreOwner.current
    val vm = remember(owner) { owner?.let { ViewModelProvider(it)[LevyraViewModel::class.java] } }
    val smartFlow = remember(vm) { vm?.smartListening ?: MutableStateFlow(SmartListeningUiState()) }
    val smart by smartFlow.collectAsStateWithLifecycle()
    val onSleepWithMusicSettings: (SleepWithMusicSettings) -> Unit = { vm?.setSleepWithMusicSettings(it) }
    val onSelectSleepWithMusicMinutes: (Int, Boolean) -> Unit = { mins, timbre ->
        if (vm != null) vm.setSleepWithMusicTimerMinutes(mins, timbre) else onSelectMinutes(mins)
    }
    val onResumeMorningSnapshot: () -> Unit = { vm?.resumeMorningPlayback() }
    val onDismissMorningSnapshot: () -> Unit = { vm?.dismissMorningResumePrompt() }
    val strings = LocalLevyraStrings.current
    val copy = strings.systemPlayerCopy()
    val smartCopy = strings.smartListeningCopy()
    val blocker = remember { MutableInteractionSource() }
    val timerRunning = state.sleepTimerMinutes > 0 || state.sleepTimerEndOfTrack
    var selectedMode by remember(timerRunning, smart.sleepTimerMode, smart.sleepWithMusicSettings.preferSmartSleepMode) {
        mutableStateOf(initialSleepTimerMode(timerRunning, smart))
    }
    val isSleepWithMusicMode = selectedMode == PlaybackSleepTimerMode.SLEEP_WITH_MUSIC

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
                        title = if (isSleepWithMusicMode) smartCopy.sleepWithMusicTitle else strings.sleepTimer,
                        subtitle = if (isSleepWithMusicMode) smartCopy.sleepWithMusicSubtitle else copy.sleepChooseDuration,
                        isSleepWithMusicMode = isSleepWithMusicMode,
                        closeLabel = strings.close,
                        onClose = onClose
                    )
                    SleepTimerModeSwitcher(
                        selectedMode = selectedMode,
                        classicLabel = smartCopy.classicTimerMode,
                        sleepWithMusicLabel = smartCopy.smartSleepMode,
                        onSelectMode = { mode ->
                            selectedMode = mode
                            onSleepWithMusicSettings(
                                smart.sleepWithMusicSettings.copy(
                                    preferSmartSleepMode = mode == PlaybackSleepTimerMode.SLEEP_WITH_MUSIC
                                )
                            )
                        }
                    )
                    val morningSnapshot = smart.morningResumeSnapshot
                    if (morningSnapshot != null && morningSnapshot.isReadyToPrompt(System.currentTimeMillis())) {
                        MorningResumePromptBanner(
                            strings = strings,
                            snapshot = morningSnapshot,
                            onResume = onResumeMorningSnapshot,
                            onDismiss = onDismissMorningSnapshot
                        )
                    }
                    if (timerRunning) {
                        SleepTimerActiveCard(
                            state = state,
                            sleepTimerMode = smart.sleepTimerMode,
                            smartCopy = smartCopy,
                            remainingSeconds = remainingSeconds,
                            onAddFifteen = {
                                extendSleepTimerByFifteen(
                                    remainingSeconds = remainingSeconds,
                                    smart = smart,
                                    onSelectMinutes = onSelectMinutes,
                                    onSelectSleepWithMusicMinutes = onSelectSleepWithMusicMinutes
                                )
                            },
                            onCancel = onCancel
                        )
                    }
                    SleepTimerPresetSection(
                        state = state,
                        smart = smart,
                        timerRunning = timerRunning,
                        isSleepWithMusicMode = isSleepWithMusicMode,
                        smartCopy = smartCopy,
                        onAutomationSettings = onAutomationSettings,
                        onSelectMinutes = onSelectMinutes,
                        onSelectEndOfTrack = onSelectEndOfTrack,
                        onSleepWithMusicSettings = onSleepWithMusicSettings,
                        onSelectSleepWithMusicMinutes = onSelectSleepWithMusicMinutes
                    )
                }
            }
        }
    }
}

private fun extendSleepTimerByFifteen(
    remainingSeconds: Long,
    smart: SmartListeningUiState,
    onSelectMinutes: (Int) -> Unit,
    onSelectSleepWithMusicMinutes: (Int, Boolean) -> Unit
) {
    val remainingMinutes = ((remainingSeconds + 59L) / 60L).toInt()
    val targetMinutes = (remainingMinutes + 15).coerceIn(5, 720)
    if (smart.sleepTimerMode == PlaybackSleepTimerMode.SLEEP_WITH_MUSIC) {
        onSelectSleepWithMusicMinutes(targetMinutes, smart.sleepWithMusicSettings.timbreSofteningEnabled)
    } else {
        onSelectMinutes(targetMinutes)
    }
}

private fun initialSleepTimerMode(timerRunning: Boolean, smart: SmartListeningUiState): PlaybackSleepTimerMode =
    when {
        timerRunning -> smart.sleepTimerMode
        smart.sleepWithMusicSettings.preferSmartSleepMode -> PlaybackSleepTimerMode.SLEEP_WITH_MUSIC
        else -> PlaybackSleepTimerMode.CLASSIC
    }

@Composable
private fun SleepTimerPresetSection(
    state: LevyraUiState,
    smart: SmartListeningUiState,
    timerRunning: Boolean,
    isSleepWithMusicMode: Boolean,
    smartCopy: LevyraSmartListeningCopy,
    onAutomationSettings: (LevyraAutomationSettings) -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onSleepWithMusicSettings: (SleepWithMusicSettings) -> Unit,
    onSelectSleepWithMusicMinutes: (Int, Boolean) -> Unit
) {
    val activeMinutesForMode: (PlaybackSleepTimerMode) -> Int = { mode ->
        if (timerRunning && smart.sleepTimerMode == mode) state.sleepTimerMinutes else 0
    }
    if (isSleepWithMusicMode) {
        SleepWithMusicPresets(
            selectedMinutes = activeMinutesForMode(PlaybackSleepTimerMode.SLEEP_WITH_MUSIC),
            endOfTrack = state.sleepTimerEndOfTrack,
            onSelectMinutes = { minutes ->
                onSleepWithMusicSettings(
                    smart.sleepWithMusicSettings.copy(
                        preferSmartSleepMode = true,
                        defaultDurationMinutes = minutes
                    )
                )
                onSelectSleepWithMusicMinutes(minutes, smart.sleepWithMusicSettings.timbreSofteningEnabled)
            },
            onSelectEndOfTrack = onSelectEndOfTrack
        )
        SleepWithMusicOptionsCard(
            settings = smart.sleepWithMusicSettings,
            smartCopy = smartCopy,
            onChange = onSleepWithMusicSettings
        )
    } else {
        SleepTimerPresets(
            selectedMinutes = activeMinutesForMode(PlaybackSleepTimerMode.CLASSIC),
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

@Composable
private fun SleepTimerModeSwitcher(
    selectedMode: PlaybackSleepTimerMode,
    classicLabel: String,
    sleepWithMusicLabel: String,
    onSelectMode: (PlaybackSleepTimerMode) -> Unit
) {
    Surface(
        color = LevyraBlack.copy(alpha = 0.32f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LevyraGlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SleepTimerModeTab(
                selected = selectedMode == PlaybackSleepTimerMode.CLASSIC,
                label = classicLabel,
                icon = Icons.Rounded.Timer,
                accent = LevyraCyan,
                selectedLabelColor = LevyraCyan,
                containerAlpha = 0.16f,
                borderAlpha = 0.42f,
                onClick = { onSelectMode(PlaybackSleepTimerMode.CLASSIC) }
            )
            SleepTimerModeTab(
                selected = selectedMode == PlaybackSleepTimerMode.SLEEP_WITH_MUSIC,
                label = sleepWithMusicLabel,
                icon = Icons.Rounded.AutoAwesome,
                accent = LevyraViolet,
                selectedLabelColor = LevyraText,
                containerAlpha = 0.20f,
                borderAlpha = 0.48f,
                onClick = { onSelectMode(PlaybackSleepTimerMode.SLEEP_WITH_MUSIC) }
            )
        }
    }
}

@Composable
private fun RowScope.SleepTimerModeTab(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    accent: Color,
    selectedLabelColor: Color,
    containerAlpha: Float,
    borderAlpha: Float,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .sizeIn(minHeight = 44.dp)
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = containerAlpha) else LevyraBlack.copy(alpha = 0.01f),
        border = BorderStroke(
            1.dp,
            if (selected) accent.copy(alpha = borderAlpha) else LevyraGlassBorder.copy(alpha = 0f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else LevyraMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) selectedLabelColor else LevyraMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SleepTimerHeader(
    title: String,
    subtitle: String,
    isSleepWithMusicMode: Boolean,
    closeLabel: String,
    onClose: () -> Unit
) {
    val accent = if (isSleepWithMusicMode) LevyraViolet else LevyraCyan
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = accent.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isSleepWithMusicMode) Icons.Rounded.AutoAwesome else Icons.Rounded.Bedtime,
                    contentDescription = null,
                    tint = accent
                )
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
private fun SleepTimerProgressRing(
    fraction: Float,
    icon: ImageVector,
    iconTint: Color
) {
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
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun SleepTimerActiveCard(
    state: LevyraUiState,
    sleepTimerMode: PlaybackSleepTimerMode,
    smartCopy: LevyraSmartListeningCopy,
    remainingSeconds: Long,
    onAddFifteen: () -> Unit,
    onCancel: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.systemPlayerCopy()
    val isSmartSleep = sleepTimerMode == PlaybackSleepTimerMode.SLEEP_WITH_MUSIC
    val totalSeconds = (state.sleepTimerTotalMs / 1_000L).coerceAtLeast(1L)
    val fraction = if (state.sleepTimerEndOfTrack) 1f else {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    }
    val endAt = if (state.sleepTimerEndOfTrack) {
        strings.sleepTimerEndOfTrack
    } else {
        "${copy.sleepEndsAt} ${formatSleepEndClock(state.sleepTimerDeadlineElapsedRealtimeMs, strings.code)}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        LevyraViolet.copy(alpha = if (isSmartSleep) 0.18f else 0.12f),
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
            SleepTimerProgressRing(
                fraction = fraction,
                icon = when {
                    isSmartSleep -> Icons.Rounded.AutoAwesome
                    state.sleepTimerEndOfTrack -> Icons.Rounded.Bedtime
                    else -> Icons.Rounded.Timer
                },
                iconTint = if (isSmartSleep) LevyraViolet else LevyraCyan
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = if (isSmartSleep) smartCopy.smartSleepMode else copy.sleepActive,
                    color = if (isSmartSleep) LevyraViolet else LevyraCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (state.sleepTimerEndOfTrack) strings.sleepTimerEndOfTrack else formatTimerClock(remainingSeconds),
                    color = LevyraText,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(endAt, color = LevyraMuted, fontSize = 11.sp)
                val detail = when {
                    isSmartSleep -> smartCopy.sleepWithMusicSubtitle
                    state.sleepTimerFadeMs > 0L -> copy.sleepFadeActive
                    else -> null
                }
                if (detail != null) {
                    Text(detail, color = LevyraMuted, fontSize = 11.sp, maxLines = 2)
                }
            }
        }
        Row(
            modifier = Modifier.align(Alignment.End),
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
private fun SleepWithMusicPresets(
    selectedMinutes: Int,
    endOfTrack: Boolean,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.systemPlayerCopy()
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (minutes in SLEEP_WITH_MUSIC_DURATION_PRESETS) {
                SleepPreset(
                    label = minutes.toString(),
                    suffix = copy.sleepMinuteUnit,
                    selected = selectedMinutes == minutes && !endOfTrack,
                    accent = LevyraViolet,
                    onClick = { onSelectMinutes(minutes) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        SleepTimerCustomDuration(
            selectedMinutes = selectedMinutes,
            endOfTrack = endOfTrack,
            unit = copy.sleepMinuteUnit,
            presetMinutes = SLEEP_WITH_MUSIC_DURATION_PRESETS,
            onSelectMinutes = onSelectMinutes
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelectEndOfTrack)
                .sizeIn(minHeight = 54.dp),
            shape = RoundedCornerShape(18.dp),
            color = if (endOfTrack) LevyraViolet.copy(alpha = 0.14f) else LevyraBlack.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, if (endOfTrack) LevyraViolet.copy(alpha = 0.42f) else LevyraGlassBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = if (endOfTrack) LevyraViolet else LevyraMuted)
                Spacer(Modifier.width(12.dp))
                Text(strings.sleepTimerEndOfTrack, color = LevyraText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SleepWithMusicOptionsCard(
    settings: SleepWithMusicSettings,
    smartCopy: LevyraSmartListeningCopy,
    onChange: (SleepWithMusicSettings) -> Unit
) {
    val context = LocalContext.current
    Surface(
        color = LevyraBlack.copy(alpha = 0.24f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, LevyraGlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = smartCopy.sleepWithMusicTitle.uppercase(Locale.ROOT),
                color = LevyraViolet,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            SleepAutomationToggle(
                title = smartCopy.timbreSofteningTitle,
                subtitle = smartCopy.timbreSofteningSubtitle,
                checked = settings.timbreSofteningEnabled,
                onChecked = { onChange(settings.copy(timbreSofteningEnabled = it)) }
            )
            SleepAutomationToggle(
                title = smartCopy.morningResumeTitle,
                subtitle = smartCopy.morningResumeSubtitle,
                checked = settings.morningResumeEnabled,
                onChecked = { onChange(settings.copy(morningResumeEnabled = it)) }
            )
            if (settings.morningResumeEnabled) {
                SleepAutomationToggle(
                    title = smartCopy.linkMorningToWakeTitle,
                    subtitle = "",
                    checked = settings.linkMorningPromptToGentleWake,
                    onChecked = { onChange(settings.copy(linkMorningPromptToGentleWake = it)) }
                )
                if (!settings.linkMorningPromptToGentleWake) {
                    val promptMinuteOfDay = settings.morningPromptHour * 60 + settings.morningPromptMinute
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        onChange(
                                            settings.copy(
                                                morningPromptHour = hour,
                                                morningPromptMinute = minute
                                            )
                                        )
                                    },
                                    settings.morningPromptHour,
                                    settings.morningPromptMinute,
                                    android.text.format.DateFormat.is24HourFormat(context)
                                ).show()
                            }
                            .sizeIn(minHeight = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(smartCopy.gentleWakeTimeLabel, color = LevyraMuted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatBedtimeClock(context, promptMinuteOfDay),
                            color = LevyraText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
    val copy = strings.systemPlayerCopy()
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SleepTimerPresetMinutes.forEach { minutes ->
                SleepPreset(
                    label = minutes.toString(),
                    suffix = copy.sleepMinuteUnit,
                    selected = selectedMinutes == minutes && !endOfTrack,
                    onClick = { onSelectMinutes(minutes) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        SleepTimerCustomDuration(
            selectedMinutes = selectedMinutes,
            endOfTrack = endOfTrack,
            unit = copy.sleepMinuteUnit,
            presetMinutes = SleepTimerPresetMinutes,
            onSelectMinutes = onSelectMinutes
        )
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
private fun SleepTimerCustomDuration(
    selectedMinutes: Int,
    endOfTrack: Boolean,
    unit: String,
    presetMinutes: List<Int>,
    onSelectMinutes: (Int) -> Unit
) {
    val customActive = !endOfTrack && isCustomSleepTimerMinutes(selectedMinutes, presetMinutes)
    var minutes by rememberSaveable(customActive, selectedMinutes) {
        mutableIntStateOf(if (customActive) selectedMinutes else SLEEP_TIMER_CUSTOM_DEFAULT_MINUTES)
    }
    val label = "$minutes $unit"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (customActive) LevyraCyan.copy(alpha = 0.11f) else LevyraBlack.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (customActive) LevyraCyan.copy(alpha = 0.35f) else LevyraGlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = if (customActive) LevyraCyan else LevyraMuted)
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                color = if (customActive) LevyraCyan else LevyraText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(72.dp)
            )
            Slider(
                value = minutes.toFloat(),
                onValueChange = { minutes = snapSleepTimerMinutes(it) },
                onValueChangeFinished = { onSelectMinutes(minutes) },
                valueRange = SLEEP_TIMER_CUSTOM_MIN_MINUTES.toFloat()..SLEEP_TIMER_CUSTOM_MAX_MINUTES.toFloat(),
                steps = SLEEP_TIMER_CUSTOM_SLIDER_STEPS,
                colors = SliderDefaults.colors(
                    thumbColor = LevyraCyan,
                    activeTrackColor = LevyraCyan,
                    inactiveTrackColor = LevyraGlassBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { stateDescription = label }
            )
        }
    }
}

@Composable
private fun SleepPreset(
    label: String,
    suffix: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color = LevyraCyan
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .sizeIn(minHeight = 64.dp),
        color = if (selected) accent.copy(alpha = 0.14f) else LevyraBlack.copy(alpha = 0.28f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.42f) else LevyraGlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = if (selected) accent else LevyraText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                                android.text.format.DateFormat.is24HourFormat(context)
                            ).show()
                        }
                        .sizeIn(minHeight = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(copy.startTime, color = LevyraMuted, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatBedtimeClock(context, bedtime.startMinuteOfDay),
                        color = LevyraText,
                        fontWeight = FontWeight.Bold
                    )
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
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = LevyraMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

private val SleepTimerPresetMinutes = listOf(15, 30, 45, 60)
internal const val SLEEP_TIMER_CUSTOM_MIN_MINUTES = 5
internal const val SLEEP_TIMER_CUSTOM_MAX_MINUTES = 240
internal const val SLEEP_TIMER_CUSTOM_STEP_MINUTES = 5
private const val SLEEP_TIMER_CUSTOM_DEFAULT_MINUTES = 90
internal const val SLEEP_TIMER_CUSTOM_SLIDER_STEPS =
    (SLEEP_TIMER_CUSTOM_MAX_MINUTES - SLEEP_TIMER_CUSTOM_MIN_MINUTES) / SLEEP_TIMER_CUSTOM_STEP_MINUTES - 1

internal fun snapSleepTimerMinutes(value: Float): Int {
    val stepped = Math.round(value / SLEEP_TIMER_CUSTOM_STEP_MINUTES) * SLEEP_TIMER_CUSTOM_STEP_MINUTES
    return stepped.coerceIn(SLEEP_TIMER_CUSTOM_MIN_MINUTES, SLEEP_TIMER_CUSTOM_MAX_MINUTES)
}

internal fun isCustomSleepTimerMinutes(
    minutes: Int,
    presetMinutes: List<Int> = SleepTimerPresetMinutes
): Boolean =
    minutes in SLEEP_TIMER_CUSTOM_MIN_MINUTES..SLEEP_TIMER_CUSTOM_MAX_MINUTES && minutes !in presetMinutes

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

private fun formatBedtimeClock(context: Context, startMinuteOfDay: Int): String {
    val hour = (startMinuteOfDay / 60).coerceIn(0, 23)
    val minute = (startMinuteOfDay % 60).coerceIn(0, 59)
    val time = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
    return android.text.format.DateFormat.getTimeFormat(context).format(time)
}
