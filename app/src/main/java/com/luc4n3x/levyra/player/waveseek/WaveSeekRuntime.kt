package com.luc4n3x.levyra.player.waveseek

import android.content.Context
import androidx.media3.common.C
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.player.VisualizerAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal object WaveSeekRuntime {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var started = false
    private var activeConfiguration = ""
    private var activeMediaId = ""
    private var activeDurationMs = 0L
    private var capture: WaveSeekCapture? = null

    fun start(context: Context) {
        if (started) return
        synchronized(this) {
            if (started) return
            started = true
        }
        val appContext = context.applicationContext
        scope.launch {
            while (isActive) {
                sample(appContext)
                val player = PlaybackService.activePlayerFlow.value
                delay(
                    waveSeekPollDelayMs(
                        hasPlayer = player != null,
                        hasCapture = capture != null,
                        isPlaying = player?.isPlaying == true
                    )
                )
            }
        }
    }

    private fun sample(context: Context) {
        val player = PlaybackService.activePlayerFlow.value ?: run {
            reset()
            return
        }
        val mediaItem = player.currentMediaItem ?: run {
            reset()
            return
        }
        val extras = mediaItem.mediaMetadata.extras
        val metadataDurationMs = extras
            ?.getLong("levyra.durationMs", C.TIME_UNSET)
            ?: C.TIME_UNSET
        val playerDurationMs = player.duration
        val durationMs = metadataDurationMs
            .takeIf { it > 0L && it != C.TIME_UNSET }
            ?: playerDurationMs.takeIf { it > 0L && it != C.TIME_UNSET }
            ?: 0L
        val mediaId = mediaItem.mediaId.trim()
        val source = mediaItem.localConfiguration?.uri?.toString().orEmpty()
        val videoMode = extras?.getBoolean(PlaybackService.EXTRA_VIDEO_MODE, false) == true
        val liveRadio = extras?.getBoolean(PlaybackService.EXTRA_LIVE_RADIO, false) == true
        val configuration = buildString {
            append(mediaId)
            append('|')
            append(durationMs)
            append('|')
            append(source)
            append('|')
            append(videoMode)
            append('|')
            append(liveRadio)
        }

        if (configuration != activeConfiguration) {
            activeConfiguration = configuration
            activeMediaId = mediaId
            activeDurationMs = durationMs
            val alreadyStored = WaveSeekStore.load(context, mediaId, durationMs) != null
            val spec = waveSeekCaptureSpec(
                mediaId = mediaId,
                source = source,
                durationMs = durationMs,
                videoMode = videoMode,
                liveRadio = liveRadio,
                alreadyStored = alreadyStored
            )
            capture = spec?.let { WaveSeekCapture(it.durationMs) }
        }

        val currentCapture = capture ?: return
        if (!player.isPlaying) return
        val waveform = VisualizerAudioProcessor.waveformState.value
        if (waveform.isEmpty()) return
        currentCapture.record(player.currentPosition, waveform)
        val envelope = currentCapture.snapshotIfReady() ?: return
        if (WaveSeekStore.save(context, activeMediaId, activeDurationMs, envelope)) {
            capture = null
        }
    }

    private fun reset() {
        activeConfiguration = ""
        activeMediaId = ""
        activeDurationMs = 0L
        capture = null
    }
}
