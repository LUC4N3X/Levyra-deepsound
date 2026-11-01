package com.luc4n3x.levyra.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as ComposeRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.GENTLE_WAKE_CRESCENDO_MINUTES_OPTIONS
import com.luc4n3x.levyra.domain.GentleWakeReliabilityStatus
import com.luc4n3x.levyra.domain.GentleWakeRuntimeState
import com.luc4n3x.levyra.domain.GentleWakeSettings
import com.luc4n3x.levyra.domain.GentleWakeSourceType
import com.luc4n3x.levyra.domain.MorningResumeSnapshot
import com.luc4n3x.levyra.domain.MotionControlSettings
import com.luc4n3x.levyra.domain.MotionSensorCapability
import com.luc4n3x.levyra.domain.MotionShakeSensitivity
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.SLEEP_WITH_MUSIC_DURATION_PRESETS
import com.luc4n3x.levyra.domain.SleepWithMusicSettings
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.player.GentleWakePlaybackCoordinator
import com.luc4n3x.levyra.player.GentleWakeScheduler
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.luc4n3x.levyra.ui.i18n.LevyraSmartListeningCopy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.smartListeningCopy
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import com.luc4n3x.levyra.viewmodel.completedScanSongs
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet

import java.time.DayOfWeek
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val LevyraAdaptiveBorder: Color get() = LevyraGlassBorder
private val LevyraAdaptiveSubtle: Color get() = LevyraPanelSoft
private val LevyraSurface: Color get() = LevyraBlack
private val LevyraGreen = Color(0xFF34D399)

@Composable
internal fun SmartListeningSettingsContent(
    strings: LevyraStrings,
    animationsEnabled: Boolean,
    motionSettings: MotionControlSettings,
    motionCapability: MotionSensorCapability,
    gentleWakeSettings: GentleWakeSettings,
    gentleWakeReliability: GentleWakeReliabilityStatus,
    gentleWakeRuntime: GentleWakeRuntimeState,
    sleepWithMusicSettings: SleepWithMusicSettings,
    morningResumeSnapshot: MorningResumeSnapshot?,
    playlists: List<Playlist>,
    albums: List<AlbumHit>,
    radioStations: List<RadioStation>,
    downloads: List<DownloadedTrack>,
    localTracks: List<Track>,
    onMotionSettingsChange: (MotionControlSettings) -> Unit,
    onGentleWakeSettingsChange: (GentleWakeSettings) -> Unit,
    onToggleGentleWakePreview: () -> Unit,
    onSnoozeGentleWake: () -> Unit,
    onStopGentleWakeForToday: () -> Unit,
    onSleepWithMusicSettingsChange: (SleepWithMusicSettings) -> Unit,
    onStartSleepWithMusicTimer: (Int, Boolean) -> Unit,
    onOpenSleepTimerSheet: () -> Unit,
    onResumeMorningPlayback: () -> Unit,
    onDismissMorningResume: () -> Unit
) {
    val copy = remember(strings.code) { strings.smartListeningCopy() }
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code.replace('_', '-')) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (morningResumeSnapshot != null && morningResumeSnapshot.isReadyToPrompt(System.currentTimeMillis())) {
            MorningResumePromptBanner(
                strings = strings,
                snapshot = morningResumeSnapshot,
                onResume = onResumeMorningPlayback,
                onDismiss = onDismissMorningResume
            )
        }

        if (gentleWakeRuntime.active && !gentleWakeRuntime.previewActive) {
            GentleWakeActiveAlarmCard(
                strings = strings,
                runtime = gentleWakeRuntime,
                animationsEnabled = animationsEnabled,
                onSnooze = onSnoozeGentleWake,
                onStop = onStopGentleWakeForToday
            )
        }

        SmartListeningMotionSection(
            copy = copy,
            motionSettings = motionSettings,
            motionCapability = motionCapability,
            animationsEnabled = animationsEnabled,
            onMotionSettingsChange = onMotionSettingsChange
        )

        SmartListeningGentleWakeSection(
            strings = strings,
            copy = copy,
            locale = locale,
            animationsEnabled = animationsEnabled,
            gentleWakeSettings = gentleWakeSettings,
            gentleWakeReliability = gentleWakeReliability,
            gentleWakeRuntime = gentleWakeRuntime,
            playlists = playlists,
            albums = albums,
            radioStations = radioStations,
            downloads = downloads,
            localTracks = localTracks,
            onGentleWakeSettingsChange = onGentleWakeSettingsChange,
            onToggleGentleWakePreview = onToggleGentleWakePreview
        )

        SmartListeningSleepSection(
            strings = strings,
            copy = copy,
            locale = locale,
            gentleWakeSettings = gentleWakeSettings,
            sleepWithMusicSettings = sleepWithMusicSettings,
            onSleepWithMusicSettingsChange = onSleepWithMusicSettingsChange,
            onStartSleepWithMusicTimer = onStartSleepWithMusicTimer,
            onOpenSleepTimerSheet = onOpenSleepTimerSheet
        )
    }
}

@Composable
private fun SmartListeningMotionSection(
    copy: LevyraSmartListeningCopy,
    motionSettings: MotionControlSettings,
    motionCapability: MotionSensorCapability,
    animationsEnabled: Boolean,
    onMotionSettingsChange: (MotionControlSettings) -> Unit
) {
    SmartListeningSurfaceCard(
        accent = LevyraCyan,
        icon = Icons.Rounded.Sensors,
        title = copy.motionSectionTitle,
        subtitle = copy.motionSectionSubtitle
    ) {
        SmartListeningSwitchRow(
            title = copy.motionMasterTitle,
            subtitle = copy.motionMasterSubtitle,
            checked = motionSettings.enabled,
            accent = LevyraCyan,
            onCheckedChange = { enabled ->
                onMotionSettingsChange(motionSettings.copy(enabled = enabled))
            }
        )

        if (!motionCapability.isAvailable) {
            SmartListeningInfoBanner(
                icon = Icons.Rounded.WarningAmber,
                text = copy.sensorsUnavailableHint,
                tint = LevyraOrange
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MotionGestureIllustrationCard(
                modifier = Modifier.weight(1f),
                title = copy.flipToPauseTitle,
                active = motionSettings.enabled && motionSettings.flipToPauseEnabled,
                accent = LevyraCyan,
                isFlip = true,
                animationsEnabled = animationsEnabled
            )
            MotionGestureIllustrationCard(
                modifier = Modifier.weight(1f),
                title = copy.shakeToSkipTitle,
                active = motionSettings.enabled && motionSettings.shakeToSkipEnabled,
                accent = LevyraViolet,
                isFlip = false,
                animationsEnabled = animationsEnabled
            )
        }

        SmartListeningSwitchRow(
            title = copy.flipToPauseTitle,
            subtitle = copy.flipToPauseSubtitle,
            checked = motionSettings.flipToPauseEnabled,
            enabled = motionSettings.enabled && motionCapability.canDetectFlip,
            accent = LevyraCyan,
            onCheckedChange = { flip ->
                onMotionSettingsChange(motionSettings.copy(flipToPauseEnabled = flip))
            }
        )

        SmartListeningSwitchRow(
            title = copy.shakeToSkipTitle,
            subtitle = copy.shakeToSkipSubtitle,
            checked = motionSettings.shakeToSkipEnabled,
            enabled = motionSettings.enabled && motionCapability.canDetectShake,
            accent = LevyraViolet,
            onCheckedChange = { shake ->
                onMotionSettingsChange(motionSettings.copy(shakeToSkipEnabled = shake))
            }
        )

        AnimatedVisibility(
            visible = motionSettings.enabled && motionSettings.shakeToSkipEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = copy.sensitivityLabel,
                    color = LevyraText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sensitivities = listOf(
                        MotionShakeSensitivity.LOW to copy.sensitivityLow,
                        MotionShakeSensitivity.NORMAL to copy.sensitivityNormal,
                        MotionShakeSensitivity.HIGH to copy.sensitivityHigh
                    )
                    sensitivities.forEach { (sensitivity, label) ->
                        ExpressiveChoicePill(
                            label = label,
                            selected = motionSettings.shakeSensitivity == sensitivity,
                            accent = LevyraViolet,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onMotionSettingsChange(motionSettings.copy(shakeSensitivity = sensitivity))
                            }
                        )
                    }
                }
            }
        }

        SmartListeningSwitchRow(
            title = copy.screenOffTitle,
            subtitle = if (motionCapability.wakeUpSensorSupported) {
                copy.screenOffSupportedHint
            } else {
                copy.screenOffUnsupportedHint
            },
            checked = motionSettings.allowScreenOffWhenSupported && motionCapability.wakeUpSensorSupported,
            enabled = motionSettings.enabled && motionCapability.wakeUpSensorSupported,
            accent = LevyraCyan,
            onCheckedChange = { allow ->
                onMotionSettingsChange(motionSettings.copy(allowScreenOffWhenSupported = allow))
            }
        )
    }
}

@Composable
private fun SmartListeningGentleWakeSection(
    strings: LevyraStrings,
    copy: LevyraSmartListeningCopy,
    locale: Locale,
    animationsEnabled: Boolean,
    gentleWakeSettings: GentleWakeSettings,
    gentleWakeReliability: GentleWakeReliabilityStatus,
    gentleWakeRuntime: GentleWakeRuntimeState,
    playlists: List<Playlist>,
    albums: List<AlbumHit>,
    radioStations: List<RadioStation>,
    downloads: List<DownloadedTrack>,
    localTracks: List<Track>,
    onGentleWakeSettingsChange: (GentleWakeSettings) -> Unit,
    onToggleGentleWakePreview: () -> Unit
) {
    val context = LocalContext.current
    SmartListeningSurfaceCard(
        accent = LevyraOrange,
        icon = Icons.Rounded.WbTwilight,
        title = copy.gentleWakeTitle,
        subtitle = copy.gentleWakeSubtitle
    ) {
        GentleWakeSunriseBanner(
            settings = gentleWakeSettings,
            runtime = gentleWakeRuntime,
            animationsEnabled = animationsEnabled
        )

        SmartListeningSwitchRow(
            title = copy.gentleWakeEnable,
            subtitle = String.format(locale, "%02d:%02d · %d min", gentleWakeSettings.hour, gentleWakeSettings.minute, gentleWakeSettings.crescendoMinutes),
            checked = gentleWakeSettings.enabled,
            accent = LevyraOrange,
            onCheckedChange = { enabled ->
                onGentleWakeSettingsChange(
                    gentleWakeSettings.copy(
                        enabled = enabled,
                        dismissedDateIso = ""
                    )
                )
            }
        )

        if (gentleWakeSettings.enabled && !gentleWakeReliability.isReliable) {
            GentleWakePermissionWarningCard(
                copy = copy,
                reliability = gentleWakeReliability,
                onGrantExactAlarm = {
                    runCatching {
                        context.startActivity(GentleWakeScheduler.buildExactAlarmSettingsIntent(context))
                    }
                },
                onGrantNotifications = {
                    runCatching {
                        context.startActivity(GentleWakeScheduler.buildNotificationSettingsIntent(context))
                    }
                }
            )
        } else if (gentleWakeSettings.enabled && gentleWakeReliability.isReliable) {
            SmartListeningInfoBanner(
                icon = Icons.Rounded.CheckCircle,
                text = copy.alarmReliableBadge,
                tint = LevyraGreen
            )
        }

        GentleWakeTimeSelector(
            label = copy.gentleWakeTimeLabel,
            hour = gentleWakeSettings.hour,
            minute = gentleWakeSettings.minute,
            locale = locale,
            accent = LevyraOrange,
            onTimeChange = { newHour, newMinute ->
                onGentleWakeSettingsChange(
                    gentleWakeSettings.copy(
                        hour = newHour,
                        minute = newMinute,
                        dismissedDateIso = ""
                    )
                )
            }
        )

        GentleWakeDaysSelector(
            label = copy.gentleWakeDaysLabel,
            settings = gentleWakeSettings,
            locale = locale,
            onGentleWakeSettingsChange = onGentleWakeSettingsChange
        )

        GentleWakeSourceSelector(
            copy = copy,
            settings = gentleWakeSettings,
            playlists = playlists,
            albums = albums,
            radioStations = radioStations,
            downloads = downloads,
            localTracks = localTracks,
            onSelectSettings = onGentleWakeSettingsChange
        )

        GentleWakeIntensityControls(
            strings = strings,
            copy = copy,
            gentleWakeSettings = gentleWakeSettings,
            onGentleWakeSettingsChange = onGentleWakeSettingsChange
        )

        GentleWakePreviewButton(
            copy = copy,
            gentleWakeRuntime = gentleWakeRuntime,
            onToggleGentleWakePreview = onToggleGentleWakePreview
        )
    }
}

@Composable
private fun GentleWakeDaysSelector(
    label: String,
    settings: GentleWakeSettings,
    locale: Locale,
    onGentleWakeSettingsChange: (GentleWakeSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            color = LevyraText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DayOfWeek.entries.forEach { day ->
                val active = day in settings.days
                val shortName = day.getDisplayName(JavaTextStyle.NARROW_STANDALONE, locale)
                val fullName = day.getDisplayName(JavaTextStyle.FULL, locale)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (active) LevyraOrange.copy(alpha = 0.22f) else LevyraAdaptiveSubtle
                        )
                        .border(
                            width = 1.dp,
                            color = if (active) LevyraOrange else LevyraAdaptiveBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .semantics {
                            role = Role.Checkbox
                            selected = active
                        }
                        .clickable {
                            val updated = if (active && settings.days.size > 1) {
                                settings.days - day
                            } else {
                                settings.days + day
                            }
                            onGentleWakeSettingsChange(settings.copy(days = updated))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shortName.ifBlank { fullName.take(1) },
                        color = if (active) LevyraText else LevyraMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GentleWakeIntensityControls(
    strings: LevyraStrings,
    copy: LevyraSmartListeningCopy,
    gentleWakeSettings: GentleWakeSettings,
    onGentleWakeSettingsChange: (GentleWakeSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = copy.crescendoLabel,
                color = LevyraText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GENTLE_WAKE_CRESCENDO_MINUTES_OPTIONS.forEach { mins ->
                    ExpressiveChoicePill(
                        label = strings.formatSleepTimerMinutes(mins),
                        selected = gentleWakeSettings.crescendoMinutes == mins,
                        accent = LevyraOrange,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onGentleWakeSettingsChange(gentleWakeSettings.copy(crescendoMinutes = mins))
                        }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            var draftMaxVolume by remember(gentleWakeSettings.maxVolumeFraction) {
                mutableFloatStateOf(gentleWakeSettings.maxVolumeFraction)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = copy.maxVolumeLabel,
                    color = LevyraText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(draftMaxVolume * 100f).roundToInt()}%",
                    color = LevyraOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = draftMaxVolume,
                onValueChange = { fraction -> draftMaxVolume = fraction },
                onValueChangeFinished = {
                    onGentleWakeSettingsChange(gentleWakeSettings.copy(maxVolumeFraction = draftMaxVolume))
                },
                valueRange = 0.20f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = LevyraOrange,
                    activeTrackColor = LevyraOrange,
                    inactiveTrackColor = LevyraAdaptiveBorder
                )
            )
        }
    }
}

@Composable
private fun GentleWakePreviewButton(
    copy: LevyraSmartListeningCopy,
    gentleWakeRuntime: GentleWakeRuntimeState,
    onToggleGentleWakePreview: () -> Unit
) {
    val previewing = gentleWakeRuntime.active && gentleWakeRuntime.previewActive
    OutlinedButton(
        onClick = onToggleGentleWakePreview,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (previewing) LevyraOrange.copy(alpha = 0.16f) else Color.Transparent,
            contentColor = LevyraText
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Icon(
            imageVector = if (previewing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = LevyraOrange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (previewing) {
                "${copy.stopPreview} (${(gentleWakeRuntime.currentVolumeFraction * 100f).roundToInt()}%)"
            } else {
                copy.previewCrescendo
            },
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartListeningSleepSection(
    strings: LevyraStrings,
    copy: LevyraSmartListeningCopy,
    locale: Locale,
    gentleWakeSettings: GentleWakeSettings,
    sleepWithMusicSettings: SleepWithMusicSettings,
    onSleepWithMusicSettingsChange: (SleepWithMusicSettings) -> Unit,
    onStartSleepWithMusicTimer: (Int, Boolean) -> Unit,
    onOpenSleepTimerSheet: () -> Unit
) {
    SmartListeningSurfaceCard(
        accent = LevyraPink,
        icon = Icons.Rounded.Bedtime,
        title = copy.sleepWithMusicTitle,
        subtitle = copy.sleepWithMusicSubtitle
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SLEEP_WITH_MUSIC_DURATION_PRESETS.forEach { minutes ->
                ExpressiveChoicePill(
                    label = strings.formatSleepTimerMinutes(minutes),
                    selected = sleepWithMusicSettings.defaultDurationMinutes == minutes,
                    accent = LevyraPink,
                    onClick = {
                        onSleepWithMusicSettingsChange(
                            sleepWithMusicSettings.copy(
                                preferSmartSleepMode = true,
                                defaultDurationMinutes = minutes
                            )
                        )
                        onStartSleepWithMusicTimer(minutes, sleepWithMusicSettings.timbreSofteningEnabled)
                    }
                )
            }
        }

        SmartListeningSwitchRow(
            title = copy.timbreSofteningTitle,
            subtitle = copy.timbreSofteningSubtitle,
            checked = sleepWithMusicSettings.timbreSofteningEnabled,
            accent = LevyraPink,
            onCheckedChange = { enabled ->
                onSleepWithMusicSettingsChange(
                    sleepWithMusicSettings.copy(timbreSofteningEnabled = enabled)
                )
            }
        )

        SmartListeningSwitchRow(
            title = copy.morningResumeTitle,
            subtitle = copy.morningResumeSubtitle,
            checked = sleepWithMusicSettings.morningResumeEnabled,
            accent = LevyraPink,
            onCheckedChange = { enabled ->
                onSleepWithMusicSettingsChange(
                    sleepWithMusicSettings.copy(morningResumeEnabled = enabled)
                )
            }
        )

        AnimatedVisibility(
            visible = sleepWithMusicSettings.morningResumeEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmartListeningSwitchRow(
                    title = copy.linkMorningToWakeTitle,
                    subtitle = String.format(locale, "%02d:%02d", gentleWakeSettings.hour, gentleWakeSettings.minute),
                    checked = sleepWithMusicSettings.linkMorningPromptToGentleWake,
                    accent = LevyraPink,
                    onCheckedChange = { link ->
                        onSleepWithMusicSettingsChange(
                            sleepWithMusicSettings.copy(linkMorningPromptToGentleWake = link)
                        )
                    }
                )
                if (!sleepWithMusicSettings.linkMorningPromptToGentleWake) {
                    GentleWakeTimeSelector(
                        label = copy.morningResumeTitle,
                        hour = sleepWithMusicSettings.morningPromptHour,
                        minute = sleepWithMusicSettings.morningPromptMinute,
                        locale = locale,
                        accent = LevyraPink,
                        onTimeChange = { newHour, newMinute ->
                            onSleepWithMusicSettingsChange(
                                sleepWithMusicSettings.copy(
                                    morningPromptHour = newHour,
                                    morningPromptMinute = newMinute
                                )
                            )
                        }
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onOpenSleepTimerSheet,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LevyraText),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = LevyraPink)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.sleepTimer, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun MorningResumePromptBanner(
    strings: LevyraStrings,
    snapshot: MorningResumeSnapshot,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = remember(strings.code) { strings.smartListeningCopy() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, LevyraOrange.copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = LevyraAdaptiveCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            LevyraOrange.copy(alpha = 0.14f),
                            LevyraPink.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LevyraOrange.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (snapshot.track.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = snapshot.track.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = LevyraOrange)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = copy.morningPromptTitle,
                        color = LevyraText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${snapshot.track.title} · ${snapshot.track.artist}",
                        color = LevyraMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LevyraOrange,
                        contentColor = LevyraInk
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(copy.morningPromptActionResume, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LevyraText),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text(copy.morningPromptActionLater, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun GentleWakeActiveAlarmCard(
    strings: LevyraStrings,
    runtime: GentleWakeRuntimeState,
    animationsEnabled: Boolean,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = remember(strings.code) { strings.smartListeningCopy() }
    val progress = if (runtime.crescendoDurationMs > 0L) {
        (runtime.elapsedMs.toFloat() / runtime.crescendoDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, LevyraOrange.copy(alpha = 0.65f), RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = LevyraAdaptiveCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            LevyraOrange.copy(alpha = 0.22f),
                            LevyraPink.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(LevyraOrange.copy(alpha = 0.24f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Alarm, contentDescription = null, tint = LevyraOrange)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = copy.gentleWakeTitle,
                        color = LevyraText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (runtime.usingEmergencyFallback) {
                            copy.emergencyFallbackNotice
                        } else {
                            runtime.sourceTitle.ifBlank { copy.builtInToneName }
                        },
                        color = LevyraMuted,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "${(runtime.currentVolumeFraction * 100f).roundToInt()}%",
                    color = LevyraOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                color = LevyraOrange,
                trackColor = LevyraAdaptiveBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSnooze,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LevyraOrange,
                        contentColor = LevyraInk
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                ) {
                    Icon(Icons.Rounded.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(copy.snoozeAction, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onStop,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LevyraText),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(copy.stopAlarmForToday, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SmartListeningSurfaceCard(
    accent: Color,
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LevyraAdaptiveBorder, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = LevyraAdaptiveCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = LevyraText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = subtitle,
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun MotionGestureIllustrationCard(
    modifier: Modifier = Modifier,
    title: String,
    active: Boolean,
    accent: Color,
    isFlip: Boolean,
    animationsEnabled: Boolean
) {
    val phase = if (animationsEnabled && active) {
        val transition = rememberInfiniteTransition(label = if (isFlip) "flipAnim" else "shakeAnim")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = LinearEasing),
                repeatMode = ComposeRepeatMode.Reverse
            ),
            label = "motionPhase"
        )
        value
    } else {
        0.35f
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(LevyraAdaptiveSubtle)
            .border(
                width = 1.dp,
                color = if (active) accent.copy(alpha = 0.45f) else LevyraAdaptiveBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeColor = if (active) accent else LevyraMuted.copy(alpha = 0.6f)
            if (isFlip) {
                drawLine(
                    color = strokeColor.copy(alpha = 0.35f),
                    start = Offset(size.width * 0.18f, size.height * 0.78f),
                    end = Offset(size.width * 0.82f, size.height * 0.78f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                val angle = -12f * (1f - phase)
                rotate(degrees = angle, pivot = center) {
                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(center.x - 28f, center.y - 10f),
                        size = Size(56f, 22f),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 3.5f)
                    )
                }
            } else {
                val shift = sin(phase * PI.toFloat() * 2f) * 8f
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(center.x - 16f + shift, center.y - 24f),
                    size = Size(32f, 48f),
                    cornerRadius = CornerRadius(9f, 9f),
                    style = Stroke(width = 3.5f)
                )
                drawArc(
                    color = strokeColor.copy(alpha = 0.55f),
                    startAngle = 150f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - 34f, center.y - 18f),
                    size = Size(24f, 36f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = strokeColor.copy(alpha = 0.55f),
                    startAngle = -30f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x + 10f, center.y - 18f),
                    size = Size(24f, 36f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = title,
            color = if (active) LevyraText else LevyraMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GentleWakeSunriseBanner(
    settings: GentleWakeSettings,
    runtime: GentleWakeRuntimeState,
    animationsEnabled: Boolean
) {
    val breath = if (animationsEnabled && (settings.enabled || runtime.active)) {
        val transition = rememberInfiniteTransition(label = "gentleWakeSunrise")
        val value by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = ComposeRepeatMode.Reverse
            ),
            label = "sunriseBreath"
        )
        value
    } else {
        if (settings.enabled) 0.65f else 0.25f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        LevyraOrange.copy(alpha = 0.22f * breath + 0.06f),
                        LevyraPink.copy(alpha = 0.12f * breath + 0.04f),
                        LevyraAdaptiveSubtle
                    )
                )
            )
            .border(1.dp, LevyraOrange.copy(alpha = 0.28f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val horizonY = size.height * 0.78f
            val sunRadius = size.height * (0.28f + 0.08f * breath)
            val sunCenter = Offset(size.width * 0.5f, horizonY + sunRadius * (0.35f - 0.25f * breath))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LevyraOrange.copy(alpha = 0.55f),
                        LevyraPink.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = sunCenter,
                    radius = sunRadius * 2.2f
                ),
                radius = sunRadius * 2.2f,
                center = sunCenter
            )
            drawCircle(
                color = LevyraOrange.copy(alpha = 0.75f),
                radius = sunRadius * 0.68f,
                center = sunCenter
            )
            drawLine(
                color = LevyraOrange.copy(alpha = 0.45f),
                start = Offset(size.width * 0.12f, horizonY),
                end = Offset(size.width * 0.88f, horizonY),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun GentleWakeTimeSelector(
    label: String,
    hour: Int,
    minute: Int,
    locale: Locale,
    accent: Color,
    onTimeChange: (Int, Int) -> Unit
) {
    val totalMinutes = hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59)
    fun adjustBy(deltaMinutes: Int) {
        val wrapped = ((totalMinutes + deltaMinutes) % 1440 + 1440) % 1440
        onTimeChange(wrapped / 60, wrapped % 60)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LevyraAdaptiveSubtle)
            .border(1.dp, LevyraAdaptiveBorder, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            color = LevyraMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TimeStepChip(label = "-1h", accent = accent) { adjustBy(-60) }
                TimeStepChip(label = "-5m", accent = accent) { adjustBy(-5) }
            }
            Text(
                text = String.format(locale, "%02d:%02d", hour, minute),
                color = LevyraText,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TimeStepChip(label = "+5m", accent = accent) { adjustBy(5) }
                TimeStepChip(label = "+1h", accent = accent) { adjustBy(60) }
            }
        }
    }
}

@Composable
private fun TimeStepChip(
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = LevyraText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GentleWakeSourceSelector(
    copy: com.luc4n3x.levyra.ui.i18n.LevyraSmartListeningCopy,
    settings: GentleWakeSettings,
    playlists: List<Playlist>,
    albums: List<AlbumHit>,
    radioStations: List<RadioStation>,
    downloads: List<DownloadedTrack>,
    localTracks: List<Track>,
    onSelectSettings: (GentleWakeSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = copy.gentleWakeSourceLabel,
            color = LevyraText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val types = listOf(
                Triple(GentleWakeSourceType.BUILT_IN_TONE, copy.sourceBuiltInTone, Icons.Rounded.AutoAwesome),
                Triple(GentleWakeSourceType.PLAYLIST, copy.sourcePlaylist, Icons.AutoMirrored.Rounded.QueueMusic),
                Triple(GentleWakeSourceType.ALBUM, copy.sourceAlbum, Icons.Rounded.Album),
                Triple(GentleWakeSourceType.RADIO, copy.sourceRadio, Icons.Rounded.Radio),
                Triple(GentleWakeSourceType.LOCAL_AUDIO, copy.sourceLocalAudio, Icons.Rounded.LibraryMusic)
            )
            types.forEach { (type, label, _) ->
                ExpressiveChoicePill(
                    label = label,
                    selected = settings.sourceType == type,
                    accent = LevyraOrange,
                    onClick = {
                        if (type == GentleWakeSourceType.BUILT_IN_TONE) {
                            onSelectSettings(
                                settings.copy(
                                    sourceType = GentleWakeSourceType.BUILT_IN_TONE,
                                    sourceId = "built_in",
                                    sourceTitle = copy.builtInToneName,
                                    sourceSubtitle = copy.builtInToneSubtitle,
                                    sourceArtworkUrl = "",
                                    sourcePayloadJson = ""
                                )
                            )
                        } else {
                            onSelectSettings(settings.copy(sourceType = type))
                        }
                    }
                )
            }
        }

        when (settings.sourceType) {
            GentleWakeSourceType.BUILT_IN_TONE -> {
                SourceOptionCard(
                    title = copy.builtInToneName,
                    subtitle = copy.builtInToneSubtitle,
                    artworkUrl = "",
                    selected = true,
                    fallbackIcon = Icons.Rounded.AutoAwesome,
                    onClick = {}
                )
            }
            GentleWakeSourceType.PLAYLIST -> {
                if (playlists.isEmpty()) {
                    SmartListeningInfoBanner(
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        text = copy.builtInToneSubtitle,
                        tint = LevyraMuted
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(playlists.take(20), key = { it.id }) { playlist ->
                            val artwork = playlist.tracks.firstOrNull()?.thumbnailUrl.orEmpty()
                            SourceOptionCard(
                                title = playlist.name,
                                subtitle = "${playlist.tracks.size}",
                                artworkUrl = artwork,
                                selected = settings.sourceId == playlist.id,
                                fallbackIcon = Icons.AutoMirrored.Rounded.QueueMusic,
                                onClick = {
                                    onSelectSettings(
                                        settings.copy(
                                            sourceType = GentleWakeSourceType.PLAYLIST,
                                            sourceId = playlist.id,
                                            sourceTitle = playlist.name,
                                            sourceSubtitle = "${playlist.tracks.size}",
                                            sourceArtworkUrl = artwork,
                                            sourcePayloadJson = GentleWakePlaybackCoordinator.encodePayloadTracks(playlist.tracks)
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
            GentleWakeSourceType.ALBUM -> {
                if (albums.isEmpty()) {
                    SmartListeningInfoBanner(
                        icon = Icons.Rounded.Album,
                        text = copy.builtInToneSubtitle,
                        tint = LevyraMuted
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(albums.take(20), key = { it.browseId.ifBlank { it.title } }) { album ->
                            val id = album.browseId.ifBlank { album.title }
                            val seedTrack = Track(
                                id = id,
                                title = album.title,
                                artist = album.artist,
                                album = album.title,
                                durationMs = 0L,
                                streamUrl = "",
                                videoUrl = "",
                                thumbnailUrl = album.thumbnailUrl,
                                largeThumbnailUrl = album.thumbnailUrl,
                                source = "AlbumWake",
                                moodTags = emptySet(),
                                energy = 50,
                                vocal = 50,
                                replayScore = 80,
                                cacheScore = 80,
                                accentStart = 0xFFFFB77C.toInt(),
                                accentEnd = 0xFF7B61FF.toInt()
                            )
                            SourceOptionCard(
                                title = album.title,
                                subtitle = album.artist,
                                artworkUrl = album.thumbnailUrl,
                                selected = settings.sourceId == id,
                                fallbackIcon = Icons.Rounded.Album,
                                onClick = {
                                    onSelectSettings(
                                        settings.copy(
                                            sourceType = GentleWakeSourceType.ALBUM,
                                            sourceId = id,
                                            sourceTitle = album.title,
                                            sourceSubtitle = album.artist,
                                            sourceArtworkUrl = album.thumbnailUrl,
                                            sourcePayloadJson = GentleWakePlaybackCoordinator.encodePayloadTracks(listOf(seedTrack))
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
            GentleWakeSourceType.RADIO -> {
                if (radioStations.isEmpty()) {
                    SmartListeningInfoBanner(
                        icon = Icons.Rounded.Radio,
                        text = copy.builtInToneSubtitle,
                        tint = LevyraMuted
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(radioStations.take(20), key = { it.uuid }) { station ->
                            val track = station.toTrack(artwork = station.safeFaviconUrl)
                            SourceOptionCard(
                                title = station.name,
                                subtitle = station.country.ifBlank { station.language },
                                artworkUrl = station.safeFaviconUrl,
                                selected = settings.sourceId == station.uuid,
                                fallbackIcon = Icons.Rounded.Radio,
                                onClick = {
                                    onSelectSettings(
                                        settings.copy(
                                            sourceType = GentleWakeSourceType.RADIO,
                                            sourceId = station.uuid,
                                            sourceTitle = station.name,
                                            sourceSubtitle = station.country,
                                            sourceArtworkUrl = station.safeFaviconUrl,
                                            sourcePayloadJson = GentleWakePlaybackCoordinator.encodePayloadTracks(listOf(track))
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
            GentleWakeSourceType.LOCAL_AUDIO -> {
                val localCandidates = remember(downloads, localTracks) {
                    val fromDownloads = downloads.map { dl ->
                        Track(
                            id = dl.trackId,
                            title = dl.title,
                            artist = dl.artist,
                            album = dl.album,
                            durationMs = dl.durationMs,
                            streamUrl = dl.uri,
                            videoUrl = "",
                            thumbnailUrl = "",
                            largeThumbnailUrl = "",
                            source = "LocalDownload",
                            moodTags = emptySet(),
                            energy = 50,
                            vocal = 50,
                            replayScore = 80,
                            cacheScore = 100,
                            accentStart = 0xFFFFB77C.toInt(),
                            accentEnd = 0xFF7B61FF.toInt()
                        )
                    }
                    (fromDownloads + localTracks).distinctBy { it.id.ifBlank { it.streamUrl } }.take(25)
                }
                if (localCandidates.isEmpty()) {
                    SmartListeningInfoBanner(
                        icon = Icons.Rounded.MusicNote,
                        text = copy.builtInToneSubtitle,
                        tint = LevyraMuted
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(localCandidates, key = { it.id.ifBlank { it.streamUrl } }) { track ->
                            val id = track.id.ifBlank { track.streamUrl }
                            SourceOptionCard(
                                title = track.title,
                                subtitle = track.artist,
                                artworkUrl = track.thumbnailUrl,
                                selected = settings.sourceId == id,
                                fallbackIcon = Icons.Rounded.MusicNote,
                                onClick = {
                                    onSelectSettings(
                                        settings.copy(
                                            sourceType = GentleWakeSourceType.LOCAL_AUDIO,
                                            sourceId = id,
                                            sourceTitle = track.title,
                                            sourceSubtitle = track.artist,
                                            sourceArtworkUrl = track.thumbnailUrl,
                                            sourcePayloadJson = GentleWakePlaybackCoordinator.encodePayloadTracks(listOf(track))
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceOptionCard(
    title: String,
    subtitle: String,
    artworkUrl: String,
    selected: Boolean,
    fallbackIcon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) LevyraOrange.copy(alpha = 0.16f) else LevyraAdaptiveSubtle)
            .border(
                width = 1.dp,
                color = if (selected) LevyraOrange else LevyraAdaptiveBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LevyraOrange.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            if (artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(fallbackIcon, contentDescription = null, tint = LevyraOrange)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = LevyraText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GentleWakePermissionWarningCard(
    copy: com.luc4n3x.levyra.ui.i18n.LevyraSmartListeningCopy,
    reliability: GentleWakeReliabilityStatus,
    onGrantExactAlarm: () -> Unit,
    onGrantNotifications: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LevyraOrange.copy(alpha = 0.14f))
            .border(1.dp, LevyraOrange.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = LevyraOrange)
            Text(
                text = copy.permissionRequiredBanner,
                color = LevyraText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        if (!reliability.exactAlarmPermitted) {
            Text(
                text = copy.exactAlarmPermissionHint,
                color = LevyraMuted,
                fontSize = 12.sp
            )
            Button(
                onClick = onGrantExactAlarm,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LevyraOrange,
                    contentColor = LevyraInk
                ),
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text(copy.grantPermissionAction, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        if (!reliability.notificationsPermitted) {
            Text(
                text = copy.notificationPermissionHint,
                color = LevyraMuted,
                fontSize = 12.sp
            )
            OutlinedButton(
                onClick = onGrantNotifications,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LevyraText),
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text(copy.grantPermissionAction, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SmartListeningInfoBanner(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(
            text = text,
            color = LevyraText,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun SmartListeningSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LevyraAdaptiveSubtle)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) LevyraText else LevyraMuted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LevyraInk,
                checkedTrackColor = accent,
                uncheckedThumbColor = LevyraMuted,
                uncheckedTrackColor = LevyraSurface
            )
        )
    }
}

@Composable
private fun ExpressiveChoicePill(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else LevyraAdaptiveSubtle)
            .border(
                width = 1.dp,
                color = if (selected) accent else LevyraAdaptiveBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) LevyraText else LevyraMuted,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun SmartListeningSettingsHost(onClose: () -> Unit) {
    val owner = LocalViewModelStoreOwner.current ?: return
    val vm = remember(owner) { ViewModelProvider(owner)[LevyraViewModel::class.java] }
    val state by vm.state.collectAsStateWithLifecycle()
    val smart by vm.smartListening.collectAsStateWithLifecycle()
    val strings = LocalLevyraStrings.current
    SmartListeningSettingsContent(
        strings = strings,
        animationsEnabled = state.animationsEnabled,
        motionSettings = smart.motionControlSettings,
        motionCapability = smart.motionSensorCapability,
        gentleWakeSettings = smart.gentleWakeSettings,
        gentleWakeReliability = smart.gentleWakeReliability,
        gentleWakeRuntime = smart.gentleWakeRuntimeState,
        sleepWithMusicSettings = smart.sleepWithMusicSettings,
        morningResumeSnapshot = smart.morningResumeSnapshot,
        playlists = state.playlists,
        albums = state.homeAlbums + state.exploreNewReleases,
        radioStations = smart.availableRadioStations,
        downloads = state.downloads,
        localTracks = state.localLibrary.completedScanSongs().orEmpty(),
        onMotionSettingsChange = vm::setMotionControlSettings,
        onGentleWakeSettingsChange = vm::setGentleWakeSettings,
        onToggleGentleWakePreview = vm::toggleGentleWakePreview,
        onSnoozeGentleWake = { vm.snoozeGentleWake() },
        onStopGentleWakeForToday = vm::stopGentleWakeForToday,
        onSleepWithMusicSettingsChange = vm::setSleepWithMusicSettings,
        onStartSleepWithMusicTimer = { minutes, timbreSoftening ->
            onClose()
            vm.setSleepWithMusicTimerMinutes(minutes, timbreSoftening)
        },
        onOpenSleepTimerSheet = {
            onClose()
            vm.openSleepTimer()
        },
        onResumeMorningPlayback = {
            onClose()
            vm.resumeMorningPlayback()
        },
        onDismissMorningResume = vm::dismissMorningResumePrompt
    )
}

@Composable
internal fun SmartListeningFloatingHost(
    state: LevyraUiState,
    viewModel: LevyraViewModel
) {
    val smart by viewModel.smartListening.collectAsStateWithLifecycle()
    val showFloatingGentleWakeCard =
        smart.gentleWakeRuntimeState.active &&
            !smart.gentleWakeRuntimeState.previewActive &&
            !state.showSettings
    val activeMorningSnapshot = smart.morningResumeSnapshot?.takeIf { snapshot ->
        !snapshot.dismissed &&
            snapshot.isReadyToPrompt(System.currentTimeMillis()) &&
            !state.showSettings &&
            !state.showSleepTimer
    }
    if (!showFloatingGentleWakeCard && activeMorningSnapshot == null) return
    val strings = LocalLevyraStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(65f)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showFloatingGentleWakeCard) {
                GentleWakeActiveAlarmCard(
                    strings = strings,
                    runtime = smart.gentleWakeRuntimeState,
                    animationsEnabled = state.animationsEnabled,
                    onSnooze = { viewModel.snoozeGentleWake() },
                    onStop = viewModel::stopGentleWakeForToday,
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(LevyraPanel)
                )
            }
            if (activeMorningSnapshot != null) {
                MorningResumePromptBanner(
                    strings = strings,
                    snapshot = activeMorningSnapshot,
                    onResume = viewModel::resumeMorningPlayback,
                    onDismiss = viewModel::dismissMorningResumePrompt,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(LevyraPanel)
                )
            }
        }
    }
}
