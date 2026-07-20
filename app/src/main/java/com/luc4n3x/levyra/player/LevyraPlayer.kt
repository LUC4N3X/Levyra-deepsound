package com.luc4n3x.levyra.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@UnstableApi
class LevyraPlayer(context: Context) {
    var onCompletion: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onRecoverableStreamError: ((Track, Long, Boolean, Boolean, String) -> Unit)? = null

    var controller: MediaController? = null
    private val controllerFuture = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    ).buildAsync()

    private var loadedTrack: Track? = null
    private var loadedStreamIdentity: String? = null
    private var loadedVideoMode = false
    private var pendingPlayback: PendingPlayback? = null
    private var ignoreEndedFromManualStop = false
    private var recoveryInFlight = false
    private var recoveryAttempts = 0
    private var recoveryResetJob: Job? = null
    private var audioSettings = LevyraAudioSettings()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sponsorJob: Job? = null

    init {
        controllerFuture.addListener({
            val connected = runCatching { controllerFuture.get() }.getOrElse { error ->
                onError?.invoke(error.message?.takeIf { it.isNotBlank() } ?: "Servizio di riproduzione non disponibile")
                return@addListener
            }
            controller = connected
            PlaybackService.setUiRecoveryAvailable(true)
            connected.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        recoveryResetJob?.cancel()
                        recoveryResetJob = scope.launch {
                            delay(5_000L)
                            if (connected.playbackState == Player.STATE_READY) recoveryAttempts = 0
                        }
                    }
                    if (playbackState != Player.STATE_ENDED) return
                    sponsorJob?.cancel()
                    sponsorJob = null
                    if (ignoreEndedFromManualStop || loadedTrack == null || loadedStreamIdentity == null || connected.mediaItemCount == 0) {
                        ignoreEndedFromManualStop = false
                        return
                    }
                    onCompletion?.invoke()
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (ignoreEndedFromManualStop || connected.mediaItemCount == 0) return
                    val track = loadedTrack ?: return
                    val message = cleanError(error)
                    if (isRecoverable(error) && !recoveryInFlight && recoveryAttempts < 3 && onRecoverableStreamError != null) {
                        recoveryInFlight = true
                        recoveryAttempts++
                        val playWhenReadyBeforeError = connected.playWhenReady
                        sponsorJob?.cancel()
                        sponsorJob = null
                        connected.pause()
                        onRecoverableStreamError?.invoke(
                            track,
                            connected.currentPosition.coerceAtLeast(0L),
                            loadedVideoMode,
                            playWhenReadyBeforeError,
                            message
                        )
                        return
                    }
                    sponsorJob?.cancel()
                    sponsorJob = null
                    clearLoadedState()
                    connected.pause()
                    onError?.invoke(message)
                }
            })
            pendingPlayback?.let { pending ->
                replaceSource(
                    track = pending.track,
                    positionMs = pending.positionMs,
                    videoMode = pending.videoMode,
                    playWhenReady = pending.playWhenReady
                )
            }
            pendingPlayback = null
        }, ContextCompat.getMainExecutor(context))
    }

    val isPlaying: Boolean
        get() = controller?.let { it.isPlaying || it.playWhenReady && it.playbackState == Player.STATE_BUFFERING }
            ?: pendingPlayback?.playWhenReady
            ?: false

    val positionMs: Long
        get() = controller?.currentPosition?.coerceAtLeast(0L) ?: pendingPlayback?.positionMs ?: 0L

    val durationMs: Long
        get() {
            val duration = controller?.duration ?: return 0L
            return if (duration == C.TIME_UNSET) 0L else duration.coerceAtLeast(0L)
        }

    fun play(track: Track, videoMode: Boolean = false) {
        require(track.streamUrl.isNotBlank()) { "Stream URL assente per ${track.title}" }
        val identity = streamIdentity(track, videoMode)
        val active = controller
        if (active == null) {
            pendingPlayback = PendingPlayback(track, 0L, videoMode, true)
            return
        }
        ignoreEndedFromManualStop = false
        recoveryInFlight = false
        recoveryAttempts = 0
        applyPlaybackParameters(active)
        if (loadedTrack?.id != track.id || loadedStreamIdentity != identity || loadedVideoMode != videoMode) {
            replaceSource(track, 0L, videoMode, true)
            return
        }
        active.playWhenReady = true
        active.play()
        startSponsorBlockMonitor(track)
    }

    fun replaceSource(
        track: Track,
        positionMs: Long,
        videoMode: Boolean,
        playWhenReady: Boolean
    ) {
        require(track.streamUrl.isNotBlank()) { "Stream URL assente per ${track.title}" }
        val active = controller
        if (active == null) {
            pendingPlayback = PendingPlayback(track, positionMs.coerceAtLeast(0L), videoMode, playWhenReady)
            return
        }
        ignoreEndedFromManualStop = false
        val recoveryReplacement = recoveryInFlight
        recoveryInFlight = false
        if (!recoveryReplacement) recoveryAttempts = 0
        loadedTrack = track
        loadedStreamIdentity = streamIdentity(track, videoMode)
        loadedVideoMode = videoMode
        PlaybackService.consumePreparedQueueNext(track.id)
        applyPlaybackParameters(active)
        active.playWhenReady = playWhenReady
        active.setMediaItem(LevyraMediaItemFactory.build(track, videoMode), positionMs.coerceAtLeast(0L))
        active.prepare()
        if (playWhenReady) active.play() else active.pause()
        startSponsorBlockMonitor(track)
    }

    fun failRecovery(message: String) {
        recoveryInFlight = false
        recoveryAttempts = 0
        recoveryResetJob?.cancel()
        sponsorJob?.cancel()
        sponsorJob = null
        clearLoadedState()
        controller?.pause()
        onError?.invoke(message)
    }

    fun pause() {
        controller?.pause()
    }

    fun stop() {
        ignoreEndedFromManualStop = true
        recoveryInFlight = false
        recoveryAttempts = 0
        recoveryResetJob?.cancel()
        sponsorJob?.cancel()
        sponsorJob = null
        clearLoadedState()
        pendingPlayback = null
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
        audioSettings = audioSettings.copy(
            playbackSpeed = speed.coerceIn(0.5f, 2f),
            pitch = pitch.coerceIn(0.5f, 2f)
        ).normalized()
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

    fun setRepeatOne(one: Boolean) {
        controller?.repeatMode = if (one) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun release() {
        PlaybackService.setUiRecoveryAvailable(false)
        sponsorJob?.cancel()
        sponsorJob = null
        recoveryResetJob?.cancel()
        scope.cancel()
        controller?.release()
        controller = null
        MediaController.releaseFuture(controllerFuture)
    }

    private fun startSponsorBlockMonitor(track: Track) {
        sponsorJob?.cancel()
        if (track.sponsorSegments.isEmpty()) return
        sponsorJob = scope.launch {
            while (isActive) {
                if (isPlaying) {
                    val current = positionMs
                    track.sponsorSegments.firstOrNull { current >= it.startMs && current < it.endMs }?.let {
                        seekTo(it.endMs)
                    }
                }
                delay(500L)
            }
        }
    }

    private fun applyPlaybackParameters(active: Player) {
        active.setPlaybackParameters(PlaybackParameters(audioSettings.playbackSpeed, audioSettings.pitch))
    }

    private fun clearLoadedState() {
        loadedTrack = null
        loadedStreamIdentity = null
        loadedVideoMode = false
    }

    private fun streamIdentity(track: Track, videoMode: Boolean): String {
        return buildString {
            append(track.streamUrl)
            append('|')
            append(track.videoStreamUrl)
            append('|')
            append(videoMode)
        }
    }

    private fun isRecoverable(error: PlaybackException): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is HttpDataSource.InvalidResponseCodeException && current.responseCode in setOf(403, 404, 410, 416, 429, 500, 502, 503, 504)) {
                return true
            }
            val next = current.cause
            if (next === current) break
            current = next
        }
        return error.errorCode in 2000..2008 || error.errorCode in 4001..4005
    }

    private fun cleanError(error: PlaybackException): String {
        var current: Throwable? = error
        var root: Throwable = error
        while (current != null) {
            root = current
            val next = current.cause
            if (next === current) break
            current = next
        }
        return root.message?.takeIf { it.isNotBlank() } ?: error.message ?: "Riproduzione non riuscita"
    }

    private data class PendingPlayback(
        val track: Track,
        val positionMs: Long,
        val videoMode: Boolean,
        val playWhenReady: Boolean
    )
}
