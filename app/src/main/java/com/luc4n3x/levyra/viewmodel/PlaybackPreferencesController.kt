package com.luc4n3x.levyra.viewmodel

import android.app.Application
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.domain.GentleWakeSettings
import com.luc4n3x.levyra.domain.HighQualityAudioMode
import com.luc4n3x.levyra.domain.LevyraAudioQuality
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.MorningResumeSnapshot
import com.luc4n3x.levyra.domain.MotionControlSettings
import com.luc4n3x.levyra.domain.NightListeningPreset
import com.luc4n3x.levyra.domain.PlaybackBufferSettings
import com.luc4n3x.levyra.domain.ReplayGainMode
import com.luc4n3x.levyra.domain.SleepWithMusicSettings
import com.luc4n3x.levyra.domain.SmartAudioProfile
import com.luc4n3x.levyra.domain.SmartAudioResolver
import com.luc4n3x.levyra.player.GentleWakeScheduler
import com.luc4n3x.levyra.player.LevyraPlayer
import com.luc4n3x.levyra.player.MotionGestureController
import com.luc4n3x.levyra.player.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PlaybackPreferencesController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val application: Application,
    private val preferences: LevyraPreferences,
    private val resolver: PlaybackResolver,
    private val player: LevyraPlayer,
    private val audioSettingsPersistence: AudioSettingsPersistenceCoordinator,
    private val offlineDownloads: OfflineDownloadsController,
    private val qobuzEnabled: MutableStateFlow<Boolean>,
    private val preferredAudioLanguage: MutableStateFlow<String>,
    private val smartListening: MutableStateFlow<SmartListeningUiState>
) {
    private var audioSettingsPersistJob: Job? = null

    fun flushPendingAudioSettings() {
        audioSettingsPersistJob?.cancel()
        audioSettingsPersistence.flush()
    }

    fun setPreferredAudioLanguage(language: String) {
        val normalized = com.luc4n3x.levyra.data.AudioLanguageIntelligence.normalizeLanguage(language)
        if (normalized == preferredAudioLanguage.value) return
        preferredAudioLanguage.value = normalized
        preferences.setPreferredAudioLanguage(normalized)
        resolver.setPreferredAudioLanguage(normalized)
    }

    fun toggleAudioNormalization() {
        val enabled = !uiState.value.audioNormalization
        preferences.setAudioNormalization(enabled)
        updateAudioSettings(uiState.value.audioSettings, audioNormalization = enabled)
    }

    fun setBassBoost(value: Int) {
        updateAudioSettings(
            uiState.value.audioSettings.copy(
                equalizerEnabled = true,
                parametricEqualizerEnabled = false,
                bassBoost = value
            )
        )
    }

    fun setPreampDb(value: Float) {
        updateAudioSettings(uiState.value.audioSettings.copy(preampDb = value))
    }

    fun setLimiterEnabled(value: Boolean) {
        updateAudioSettings(uiState.value.audioSettings.copy(limiterEnabled = value))
    }

    fun setEnhancedAudioEnabled(value: Boolean) {
        updateAudioSettings(uiState.value.audioSettings.copy(enhancedAudioEnabled = value))
    }

    fun setAudioOffloadPreference(value: com.luc4n3x.levyra.domain.AudioOffloadPreference) {
        updateAudioSettings(uiState.value.audioSettings.copy(audioOffloadPreference = value))
    }

    fun setSpectrumVisualizerStyle(value: com.luc4n3x.levyra.domain.SpectrumVisualizerStyle) {
        updateAudioSettings(uiState.value.audioSettings.copy(spectrumVisualizerStyle = value))
    }

    fun setPlaybackBuffer(value: PlaybackBufferSettings) {
        updateAudioSettings(uiState.value.audioSettings.copy(playbackBuffer = value))
    }

    fun setVirtualizer(value: Int) {
        val settings = uiState.value.audioSettings
        updateAudioSettings(settings.copy(equalizerEnabled = !settings.parametricEqualizerEnabled, virtualizer = value))
    }

    fun setCrossfadeSeconds(seconds: Int) {
        updateAudioSettings(uiState.value.audioSettings.copy(crossfadeSeconds = seconds))
    }

    fun setDjSoftMode(value: Boolean) {
        updateAudioSettings(uiState.value.audioSettings.copy(djSoftMode = value, crossfadeSeconds = if (value && uiState.value.audioSettings.crossfadeSeconds == 0) 6 else uiState.value.audioSettings.crossfadeSeconds))
    }

    fun setReplayGainEnabled(value: Boolean) {
        setReplayGainMode(if (value) ReplayGainMode.SMART else ReplayGainMode.OFF)
    }

    fun setReplayGainMode(mode: ReplayGainMode) {
        updateAudioSettings(uiState.value.audioSettings.withReplayGainMode(mode))
    }

    fun setReplayGainPreampDb(value: Float) {
        updateAudioSettings(uiState.value.audioSettings.copy(replayGainPreampDb = value))
    }

    fun setReplayGainPreventClipping(value: Boolean) {
        updateAudioSettings(uiState.value.audioSettings.copy(replayGainPreventClipping = value))
    }

    fun setPlaybackSpeed(value: Float) {
        updateAudioSettings(uiState.value.audioSettings.copy(playbackSpeed = value))
    }

    fun setTemporaryPlaybackSpeed(value: Float) {
        val speed = value.coerceIn(0.25f, 3f)
        val pitch = uiState.value.audioSettings.pitch
        player.setPlayback(speed, pitch)
        uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setPitch(value: Float) {
        updateAudioSettings(uiState.value.audioSettings.copy(pitch = value))
    }

    fun setGaplessEnabled(value: Boolean) {
        updateAudioSettings(uiState.value.audioSettings.copy(gaplessEnabled = value))
    }

    fun setAaudioOutputEnabled(value: Boolean) {
        updateAudioSettings(uiState.value.audioSettings.copy(aaudioOutputEnabled = value))
    }

    fun updateAudioSettings(next: LevyraAudioSettings, audioNormalization: Boolean = uiState.value.audioNormalization) {
        val normalized = next.normalized()
        audioSettingsPersistJob?.cancel()
        audioSettingsPersistence.schedule(normalized)
        audioSettingsPersistJob = scope.launch(Dispatchers.IO) {
            delay(120L)
            audioSettingsPersistence.persist(normalized)
        }
        player.setPremiumAudioSettings(normalized, audioNormalization)
        player.setPlayback(normalized.playbackSpeed, normalized.pitch)
        uiState.update { it.copy(audioSettings = normalized, playbackSpeed = normalized.playbackSpeed, audioNormalization = audioNormalization) }
    }

    fun setNightListeningEnabled(value: Boolean) {
        updateAudioSettings(uiState.value.audioSettings.copy(nightListeningEnabled = value))
    }

    fun setNightListeningPreset(value: NightListeningPreset) {
        updateAudioSettings(uiState.value.audioSettings.copy(nightListeningPreset = value))
    }

    fun setNightListeningTrim(value: Float) {
        updateAudioSettings(uiState.value.audioSettings.copy(nightListeningTrimDb = value))
    }

    fun setSmartResumeEnabled(value: Boolean) {
        updateAudioSettings(uiState.value.audioSettings.copy(smartResumeEnabled = value))
    }

    fun setSmartResumeProfile(value: com.luc4n3x.levyra.domain.SmartResumeProfile) {
        updateAudioSettings(uiState.value.audioSettings.copy(smartResumeProfile = value))
    }

    fun setSmartResumeMinPause(seconds: Int) {
        updateAudioSettings(uiState.value.audioSettings.copy(smartResumeMinPauseSeconds = seconds))
    }

    fun setAdaptiveListeningEnabled(value: Boolean) {
        updateAudioSettings(uiState.value.audioSettings.copy(adaptiveListeningEnabled = value))
        if (!value) {
            com.luc4n3x.levyra.player.AdaptiveListeningBus.configure(com.luc4n3x.levyra.player.AdaptiveListeningConfig(enabled = false))
            com.luc4n3x.levyra.feature.adaptive.AdaptiveListeningService.stop(application)
        }
    }

    fun setAdaptiveListeningSensitivity(value: com.luc4n3x.levyra.domain.AdaptiveSensitivity) {
        updateAudioSettings(uiState.value.audioSettings.copy(adaptiveListeningSensitivity = value))
    }

    fun setAdaptiveListeningMaxBoost(value: Float) {
        updateAudioSettings(uiState.value.audioSettings.copy(adaptiveListeningMaxBoostDb = value))
    }

    fun setSmartAudioProfilesEnabled(value: Boolean) {
        updateAudioSettings(uiState.value.audioSettings.copy(smartAudioProfilesEnabled = value))
    }

    fun saveSmartAudioProfile(profile: SmartAudioProfile) {
        val settings = uiState.value.audioSettings
        updateAudioSettings(settings.copy(smartAudioProfiles = SmartAudioResolver.upsert(settings.smartAudioProfiles, profile)))
    }

    fun removeSmartAudioProfile(deviceKey: String) {
        val settings = uiState.value.audioSettings
        updateAudioSettings(settings.copy(smartAudioProfiles = settings.smartAudioProfiles.filterNot { it.deviceKey == deviceKey }))
    }

    fun setAudioQuality(value: String) {
        val normalized = LevyraAudioQuality.normalize(value)
        preferences.setAudioQuality(normalized)
        resolver.setAudioQuality(normalized)
        offlineDownloads.lastDownloadQualityProbe = null
        uiState.update { it.copy(audioQuality = normalized) }
    }

    fun setHighQualityAudioMode(mode: HighQualityAudioMode) {
        preferences.setHighQualityAudioMode(mode)
        resolver.setHighQualityAudioMode(mode)
        offlineDownloads.lastDownloadQualityProbe = null
        uiState.update { it.copy(highQualityAudioMode = mode) }
    }

    fun setQobuzEnabled(enabled: Boolean) {
        preferences.setQobuzEnabled(enabled)
        resolver.setQobuzEnabled(enabled)
        qobuzEnabled.value = enabled
    }

    fun setSleepTimerMinutes(minutes: Int) {
        PlaybackService.startSleepTimer(minutes)
    }

    fun setSleepTimerEndOfTrack() {
        PlaybackService.startSleepTimerEndOfTrack()
    }

    fun setSleepWithMusicTimerMinutes(minutes: Int, timbreSoftening: Boolean) {
        if (!PlaybackService.startSleepWithMusicTimer(minutes, timbreSoftening)) {
            PlaybackService.startSleepTimer(minutes)
        }
    }

    fun setMotionControlSettings(value: MotionControlSettings) {
        val normalized = value.normalized()
        if (normalized == smartListening.value.motionControlSettings) return
        smartListening.update {
            it.copy(
                motionControlSettings = normalized,
                motionSensorCapability = MotionGestureController.inspectCapability(application)
            )
        }
        scope.launch(Dispatchers.IO) {
            preferences.setMotionControlSettings(normalized)
        }
    }

    fun setGentleWakeSettings(value: GentleWakeSettings) {
        val normalized = value.normalized()
        val app = application
        val reliability = GentleWakeScheduler.reliabilityStatus(app)
        smartListening.update {
            it.copy(
                gentleWakeSettings = normalized,
                gentleWakeReliability = reliability
            )
        }
        scope.launch(Dispatchers.IO) {
            preferences.setGentleWakeSettings(normalized)
            GentleWakeScheduler.apply(app, normalized)
            if (smartListening.value.sleepWithMusicSettings.linkMorningPromptToGentleWake) {
                val snapshot = preferences.morningResumeSnapshot()
                if (snapshot != null && !snapshot.dismissed) {
                    val nextPrompt = MorningResumeSnapshot.computeMorningPromptTime(
                        now = java.time.ZonedDateTime.now(),
                        sleepSettings = smartListening.value.sleepWithMusicSettings,
                        gentleWakeSettings = normalized
                    )
                    val updatedSnapshot = snapshot.copy(promptAtEpochMs = nextPrompt.toInstant().toEpochMilli())
                    preferences.setMorningResumeSnapshot(updatedSnapshot)
                    GentleWakeScheduler.scheduleMorningResumePrompt(app, updatedSnapshot)
                }
            }
        }
    }

    fun setSleepWithMusicSettings(value: SleepWithMusicSettings) {
        val normalized = value.normalized()
        if (normalized == smartListening.value.sleepWithMusicSettings) return
        smartListening.update { it.copy(sleepWithMusicSettings = normalized) }
        scope.launch(Dispatchers.IO) {
            preferences.setSleepWithMusicSettings(normalized)
        }
    }

    fun setSkipSilence(value: Boolean) {
        preferences.setSkipSilence(value)
        player.setSkipSilence(value)
        uiState.update { it.copy(skipSilence = value) }
    }
}
