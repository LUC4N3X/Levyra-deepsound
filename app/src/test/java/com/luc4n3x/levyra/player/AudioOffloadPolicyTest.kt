package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.audio.AudioProcessor
import com.luc4n3x.levyra.data.backupAudioSettingsFromJson
import com.luc4n3x.levyra.data.backupAudioSettingsToJson
import com.luc4n3x.levyra.domain.AudioOffloadPreference
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.ReplayGainMode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioOffloadPolicyTest {

    private val plainSettings = LevyraAudioSettings(enhancedAudioEnabled = false)

    private fun decide(
        settings: LevyraAudioSettings = plainSettings,
        audioNormalization: Boolean = false,
        speed: Float = 1f,
        pitch: Float = 1f,
        skipSilence: Boolean = false,
        aaudioSupported: Boolean = true
    ): AudioOffloadDecision = AudioOffloadPolicy.decide(
        AudioOffloadInputs.from(settings, audioNormalization, speed, pitch, skipSilence, aaudioSupported)
    )

    @Test
    fun automaticWithoutProcessing_allowsOffload() {
        val decision = decide()
        assertTrue(decision.allowed)
        assertEquals("ALLOWED", decision.summary())
    }

    @Test
    fun defaultSettings_keepEnhancedAudioAndBlockOnDsp() {
        assertEquals(AudioOffloadPreference.AUTOMATIC, LevyraAudioSettings().audioOffloadPreference)
        assertEquals(setOf(AudioOffloadBlocker.DSP), decide(settings = LevyraAudioSettings()).blockers)
    }

    @Test
    fun preferenceOff_blocksOffload() {
        val decision = decide(settings = plainSettings.copy(audioOffloadPreference = AudioOffloadPreference.OFF))
        assertEquals(setOf(AudioOffloadBlocker.PREFERENCE_OFF), decision.blockers)
    }

    @Test
    fun supportedAaudioOutput_blocksAndReleasesWithoutChangingPreference() {
        val aaudio = plainSettings.copy(aaudioOutputEnabled = true)
        assertEquals(setOf(AudioOffloadBlocker.AAUDIO_OUTPUT), decide(settings = aaudio).blockers)
        assertEquals(AudioOffloadPreference.AUTOMATIC, aaudio.audioOffloadPreference)

        assertTrue(decide(settings = aaudio.copy(aaudioOutputEnabled = false)).allowed)
    }

    @Test
    fun unsupportedAaudioOutput_doesNotBlock() {
        assertTrue(decide(settings = plainSettings.copy(aaudioOutputEnabled = true), aaudioSupported = false).allowed)
    }

    @Test
    fun outputPath_isDerivedFromOffloadFlagAndEncoding() {
        assertEquals(AudioOffloadOutput.OFFLOADED, audioOffloadOutputOf(offload = true, encoding = C.ENCODING_AAC_LC))
        assertEquals(AudioOffloadOutput.PCM, audioOffloadOutputOf(offload = false, encoding = C.ENCODING_PCM_16BIT))
        assertEquals(AudioOffloadOutput.PCM, audioOffloadOutputOf(offload = false, encoding = C.ENCODING_PCM_FLOAT))
        assertEquals(AudioOffloadOutput.PASSTHROUGH, audioOffloadOutputOf(offload = false, encoding = C.ENCODING_E_AC3))
    }

    @Test
    fun clearWaveform_emptiesSharedVisualizerState() {
        val processor = VisualizerAudioProcessor()
        processor.configure(AudioProcessor.AudioFormat(44_100, 2, C.ENCODING_PCM_16BIT))
        val pcm = ByteBuffer.allocateDirect(4_096).order(ByteOrder.LITTLE_ENDIAN)
        while (pcm.remaining() >= 2) pcm.putShort(8_000)
        pcm.flip()
        processor.queueInput(pcm)
        assertTrue(VisualizerAudioProcessor.waveformState.value.isNotEmpty())

        VisualizerAudioProcessor.clearWaveform()
        assertTrue(VisualizerAudioProcessor.waveformState.value.isEmpty())
    }

    @Test
    fun crossfade_blocksAndReleasesWithoutChangingPreference() {
        val crossfade = plainSettings.copy(crossfadeSeconds = 6)
        assertEquals(setOf(AudioOffloadBlocker.CROSSFADE), decide(settings = crossfade).blockers)
        assertEquals(AudioOffloadPreference.AUTOMATIC, crossfade.audioOffloadPreference)

        assertTrue(decide(settings = crossfade.copy(crossfadeSeconds = 0)).allowed)
    }

    @Test
    fun crossfadeWithoutGapless_isInactiveAndDoesNotBlock() {
        assertTrue(decide(settings = plainSettings.copy(crossfadeSeconds = 6, gaplessEnabled = false)).allowed)
    }

    @Test
    fun equalizerParametricAndEnhancedAudio_blockAsDsp() {
        assertEquals(setOf(AudioOffloadBlocker.DSP), decide(settings = plainSettings.copy(equalizerEnabled = true)).blockers)
        assertEquals(setOf(AudioOffloadBlocker.DSP), decide(settings = plainSettings.withNeutralParametricEqualizer()).blockers)
        assertEquals(setOf(AudioOffloadBlocker.DSP), decide(settings = plainSettings.copy(enhancedAudioEnabled = true)).blockers)
        assertTrue(
            decide(settings = plainSettings.copy(parametricEqualizerEnabled = true, activeParametricProfile = null)).allowed
        )
    }

    @Test
    fun normalizationAndReplayGain_blockBecauseGainIsPcmProcessing() {
        val noLimiter = plainSettings.copy(limiterEnabled = false)
        assertEquals(
            setOf(AudioOffloadBlocker.NORMALIZATION),
            decide(settings = noLimiter, audioNormalization = true).blockers
        )
        assertEquals(
            setOf(AudioOffloadBlocker.NORMALIZATION),
            decide(settings = noLimiter.withReplayGainMode(ReplayGainMode.TRACK)).blockers
        )
        assertEquals(
            setOf(AudioOffloadBlocker.DSP, AudioOffloadBlocker.NORMALIZATION),
            decide(audioNormalization = true).blockers
        )
    }

    @Test
    fun requiredTruePeakLimiter_blocksAsDspEvenWithEqualizerOff() {
        val virtualizerOnly = plainSettings.copy(virtualizer = 40)
        assertEquals(setOf(AudioOffloadBlocker.DSP), decide(settings = virtualizerOnly).blockers)
        assertTrue(decide(settings = virtualizerOnly.copy(limiterEnabled = false)).allowed)
    }

    @Test
    fun playbackSpeedAndPitch_blockUntilNormalAgain() {
        assertTrue(decide(speed = 1f, pitch = 1f).allowed)
        assertEquals(setOf(AudioOffloadBlocker.PLAYBACK_SPEED), decide(speed = 1.25f).blockers)
        assertEquals(setOf(AudioOffloadBlocker.PLAYBACK_SPEED), decide(pitch = 0.9f).blockers)
        assertTrue(decide(speed = 1f).allowed)
    }

    @Test
    fun skipSilence_blocksBecauseMedia3AppliesItOnlyOnPcm() {
        assertEquals(setOf(AudioOffloadBlocker.SKIP_SILENCE), decide(skipSilence = true).blockers)
        assertTrue(decide(skipSilence = false).allowed)
    }

    @Test
    fun multipleBlockers_areAllReported() {
        val decision = decide(settings = plainSettings.copy(crossfadeSeconds = 4, equalizerEnabled = true), speed = 1.5f)
        assertEquals(
            setOf(AudioOffloadBlocker.CROSSFADE, AudioOffloadBlocker.DSP, AudioOffloadBlocker.PLAYBACK_SPEED),
            decision.blockers
        )
        assertEquals("BLOCKED(crossfade,dsp,playback_speed)", decision.summary())
    }

    @Test
    fun media3Preferences_matchDecision() {
        val allowed = AudioOffloadPolicy.preferences(decide(), gaplessRequired = true)
        assertEquals(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED, allowed.audioOffloadMode)
        assertTrue(allowed.isGaplessSupportRequired)
        assertFalse(allowed.isSpeedChangeSupportRequired)

        val blocked = AudioOffloadPolicy.preferences(decide(speed = 2f), gaplessRequired = true)
        assertEquals(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED, blocked.audioOffloadMode)
    }

    @Test
    fun offloadUpdate_onlyTouchesLocalAudioOffloadPreferences() {
        val local = TrackSelectionParameters.DEFAULT.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()

        val updated = requireNotNull(nextAudioOffloadTrackSelection(local, decide(), gaplessRequired = true))

        assertEquals(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED, updated.audioOffloadPreferences.audioOffloadMode)
        assertEquals(local.disabledTrackTypes, updated.disabledTrackTypes)
        assertEquals(local.buildUpon().setAudioOffloadPreferences(updated.audioOffloadPreferences).build(), updated)
    }

    @Test
    fun runtimeSettingChanges_updateTrackSelectionOnlyWhenDecisionChanges() {
        var parameters = TrackSelectionParameters.DEFAULT
        fun apply(decision: AudioOffloadDecision): Boolean {
            val next = nextAudioOffloadTrackSelection(parameters, decision, gaplessRequired = true) ?: return false
            parameters = next
            return true
        }

        assertTrue(apply(decide()))
        assertEquals(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED, parameters.audioOffloadPreferences.audioOffloadMode)
        assertFalse(apply(decide()))

        assertTrue(apply(decide(settings = plainSettings.copy(crossfadeSeconds = 6))))
        assertEquals(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED, parameters.audioOffloadPreferences.audioOffloadMode)
        assertFalse(apply(decide(speed = 1.25f)))

        assertTrue(apply(decide()))
        assertEquals(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED, parameters.audioOffloadPreferences.audioOffloadMode)
    }

    @Test
    fun backup_roundTripsPreferenceAndLegacyDefaultsToAutomatic() {
        val off = LevyraAudioSettings(audioOffloadPreference = AudioOffloadPreference.OFF)
        assertEquals(AudioOffloadPreference.OFF, backupAudioSettingsFromJson(backupAudioSettingsToJson(off)).audioOffloadPreference)

        assertEquals(AudioOffloadPreference.AUTOMATIC, backupAudioSettingsFromJson(JSONObject()).audioOffloadPreference)
        assertEquals(
            AudioOffloadPreference.AUTOMATIC,
            backupAudioSettingsFromJson(JSONObject().put("audioOffloadPreference", "unknown")).audioOffloadPreference
        )
    }

    @Test
    fun storageValue_parsesCaseInsensitivelyWithAutomaticFallback() {
        assertEquals(AudioOffloadPreference.OFF, AudioOffloadPreference.fromStorage(" OFF "))
        assertEquals(AudioOffloadPreference.AUTOMATIC, AudioOffloadPreference.fromStorage(null))
        assertEquals(AudioOffloadPreference.AUTOMATIC, AudioOffloadPreference.fromStorage(""))
    }
}
