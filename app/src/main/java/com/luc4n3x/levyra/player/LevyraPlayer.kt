package com.luc4n3x.levyra.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.*

@OptIn(UnstableApi::class)
class LevyraPlayer(context: Context) {
    var onCompletion: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    var controller: MediaController? = null
    private val controllerFuture = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    ).buildAsync()

    private var loadedTrackId: String? = null
    private var loadedStreamUrl: String? = null
    private var pendingPlay: Track? = null
    private var ignoreEndedFromManualStop = false
    private var audioSettings = LevyraAudioSettings()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sponsorJob: Job? = null

    init {
        controllerFuture.addListener({
            val connected = controllerFuture.get()
            controller = connected
            connected.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState != Player.STATE_ENDED) return
                    if (ignoreEndedFromManualStop || loadedTrackId == null || loadedStreamUrl == null || connected.mediaItemCount == 0) {
                        ignoreEndedFromManualStop = false
                        return
                    }
                    onCompletion?.invoke()
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (ignoreEndedFromManualStop || loadedTrackId == null || loadedStreamUrl == null || connected.mediaItemCount == 0) return
                    connected.pause()
                    loadedTrackId = null
                    loadedStreamUrl = null
                    onError?.invoke(cleanError(error))
                }
            })
            pendingPlay?.let { play(it) }
            pendingPlay = null
        }, ContextCompat.getMainExecutor(context))
    }

    val isPlaying: Boolean
        get() = controller?.let { it.isPlaying || (it.playWhenReady && it.playbackState == Player.STATE_BUFFERING) } ?: (pendingPlay != null)

    val positionMs: Long
        get() = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L

    val durationMs: Long
        get() {
            val duration = controller?.duration ?: return 0L
            return if (duration == C.TIME_UNSET) 0L else duration.coerceAtLeast(0L)
        }


    fun play(track: Track) {
        require(track.streamUrl.isNotBlank()) { "Stream URL assente per ${track.title}" }
        val active = controller
        if (active == null) {
            pendingPlay = track
            return
        }
        ignoreEndedFromManualStop = false
        active.playWhenReady = true
        applyPlaybackParameters(active)
        if (loadedTrackId != track.id || loadedStreamUrl != track.streamUrl) {
            loadedTrackId = track.id
            loadedStreamUrl = track.streamUrl
            PlaybackService.consumePreparedQueueNext(track.id)
            active.setMediaItem(LevyraMediaItemFactory.build(track))
            active.prepare()
        }
        active.play()
        startSponsorBlockMonitor(track)
    }

    private fun startSponsorBlockMonitor(track: Track) {
        sponsorJob?.cancel()
        if (track.sponsorSegments.isEmpty()) return
        sponsorJob = scope.launch {
            while (isActive) {
                if (isPlaying) {
                    val current = positionMs
                    for (segment in track.sponsorSegments) {
                        if (current >= segment.startMs && current < segment.endMs) {
                            seekTo(segment.endMs)
                            break
                        }
                    }
                }
                delay(500)
            }
        }
    }



    fun pause() {
        controller?.pause()
    }

    fun stop() {
        ignoreEndedFromManualStop = true
        loadedTrackId = null
        loadedStreamUrl = null
        pendingPlay = null
        sponsorJob?.cancel()
        controller?.let {
            it.pause()
            it.clearMediaItems()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun setSpeed(speed: Float) {
        setPlayback(speed, audioSettings.pitch)
    }

    fun setPlayback(speed: Float, pitch: Float) {
        audioSettings = audioSettings.copy(playbackSpeed = speed.coerceIn(0.5f, 2f), pitch = pitch.coerceIn(0.5f, 2f)).normalized()
        controller?.let { applyPlaybackParameters(it) }
    }

    fun setPremiumAudioSettings(settings: LevyraAudioSettings) {
        audioSettings = settings.normalized()
        controller?.let { applyPlaybackParameters(it) }
        PlaybackService.applyPremiumAudioSettings(audioSettings)
    }

    fun setVolume(volume: Float) {
        val safeVolume = volume.coerceIn(0f, 1f)
        controller?.let { active ->
            runCatching {
                active.javaClass.getMethod("setVolume", Float::class.javaPrimitiveType).invoke(active, safeVolume)
            }
        }
        PlaybackService.activePlayer?.let { active ->
            runCatching {
                active.javaClass.getMethod("setVolume", Float::class.javaPrimitiveType).invoke(active, safeVolume)
            }
        }
    }

    fun setSkipSilence(enabled: Boolean) {
        PlaybackService.activePlayer?.let { active ->
            runCatching {
                active.javaClass.getMethod("setSkipSilenceEnabled", Boolean::class.javaPrimitiveType).invoke(active, enabled)
            }
        }
    }

    private fun applyPlaybackParameters(active: Player) {
        active.setPlaybackParameters(PlaybackParameters(audioSettings.playbackSpeed, audioSettings.pitch))
    }

    fun setRepeatOne(one: Boolean) {
        controller?.repeatMode = if (one) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun release() {
        sponsorJob?.cancel()
        scope.cancel()
        controller?.release()
        controller = null
        MediaController.releaseFuture(controllerFuture)
    }

    private fun cleanError(error: PlaybackException): String {
        var cause: Throwable? = error
        while (cause?.cause != null && cause.cause != cause) cause = cause.cause
        return cause?.message?.takeIf { it.isNotBlank() } ?: error.message ?: "Riproduzione non riuscita"
    }
}
