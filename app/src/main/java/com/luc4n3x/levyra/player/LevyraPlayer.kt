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
import com.luc4n3x.levyra.data.classifyPlaybackFailureReason
import com.luc4n3x.levyra.data.isTerminalPlaybackFailure
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.hasVideoPlaybackPayload
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val MODE_HANDOFF_BACKWARD_SEEK_TOLERANCE_MS = 1_500L
private const val VIDEO_FIRST_FRAME_TIMEOUT_MS = 5_500L

internal fun replacementStartPosition(
    sameTrack: Boolean,
    requestedPositionMs: Long,
    activePositionMs: Long,
    durationMs: Long,
    allowBackwardActivePosition: Boolean = false
): Long {
    val requested = requestedPositionMs.coerceAtLeast(0L)
    val active = activePositionMs.coerceAtLeast(0L)
    val position = when {
        !sameTrack -> requested
        allowBackwardActivePosition && active + MODE_HANDOFF_BACKWARD_SEEK_TOLERANCE_MS < requested -> active
        else -> maxOf(requested, active)
    }
    return if (durationMs > 0L) position.coerceAtMost((durationMs - 250L).coerceAtLeast(0L)) else position
}

internal fun isRecoverablePlaybackErrorCode(errorCode: Int): Boolean =
    errorCode in 2000..2008 ||
        errorCode in 3001..3004 ||
        errorCode in 4001..4005

internal fun shouldRunDelayedVideoRecovery(
    expectedGeneration: Long,
    currentGeneration: Long,
    recoveryInFlight: Boolean,
    recoveryAttempts: Int
): Boolean =
    expectedGeneration == currentGeneration && !recoveryInFlight && recoveryAttempts < 3

internal fun shouldRunVideoFrameWatchdog(
    videoMode: Boolean,
    hasVideoPayload: Boolean,
    renderedVideoFrame: Boolean,
    surfaceAttached: Boolean,
    playbackReady: Boolean,
    playWhenReady: Boolean
): Boolean =
    videoMode &&
        hasVideoPayload &&
        !renderedVideoFrame &&
        surfaceAttached &&
        playbackReady &&
        playWhenReady

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
    private val queueEngine = PersistentQueueEngine.get(context.applicationContext)

    private var loadedTrack: Track? = null
    private var loadedStreamIdentity: String? = null
    private var loadedVideoMode = false
    private var pendingPlayback: PendingPlayback? = null
    private var ignoreEndedFromManualStop = false
    private var recoveryInFlight = false
    private var recoveryAttempts = 0
    private var invalidVideoRecoveryJob: Job? = null
    private var videoFrameWatchdogJob: Job? = null
    private var playbackGeneration = 0L
    private var observedServicePlayer: Player? = null
    private var videoSurfaceAttached = false
    private var renderedVideoFrame = false
    private var audioSettings = LevyraAudioSettings()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sponsorJob: Job? = null

    private val videoRenderListener = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            renderedVideoFrame = true
            videoFrameWatchdogJob?.cancel()
            videoFrameWatchdogJob = null
        }

        override fun onSurfaceSizeChanged(width: Int, height: Int) {
            videoSurfaceAttached = width > 0 && height > 0
            if (videoSurfaceAttached) {
                scheduleVideoFrameWatchdog()
            } else {
                videoFrameWatchdogJob?.cancel()
                videoFrameWatchdogJob = null
            }
        }
    }

    init {
        scope.launch {
            PlaybackService.activePlayerFlow.collect { servicePlayer ->
                if (observedServicePlayer === servicePlayer) return@collect
                observedServicePlayer?.removeListener(videoRenderListener)
                observedServicePlayer = servicePlayer
                renderedVideoFrame = false
                videoSurfaceAttached = servicePlayer?.surfaceSize?.let { size ->
                    size.width > 0 && size.height > 0
                } == true
                servicePlayer?.addListener(videoRenderListener)
                if (videoSurfaceAttached) scheduleVideoFrameWatchdog()
            }
        }

        controllerFuture.addListener({
            val connected = runCatching { controllerFuture.get() }.getOrElse { error ->
                onError?.invoke(error.message?.takeIf { it.isNotBlank() } ?: "Servizio di riproduzione non disponibile")
                return@addListener
            }
            controller = connected
            PlaybackService.setUiRecoveryAvailable(true)
            connected.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    val queueTrack = queueEngine.state.value.currentTrack ?: return
                    if (mediaItem?.mediaId != LevyraMediaItemFactory.metadataOnly(queueTrack).mediaId) return
                    loadedVideoMode = mediaItem.mediaMetadata.extras
                        ?.getBoolean(PlaybackService.EXTRA_VIDEO_MODE, false) == true
                    val playingUri = mediaItem.localConfiguration?.uri?.toString().orEmpty()
                    loadedTrack = when {
                        playingUri.isBlank() -> queueTrack
                        loadedVideoMode -> queueTrack.copy(videoStreamUrl = playingUri)
                        else -> queueTrack.copy(streamUrl = playingUri)
                    }
                    loadedStreamIdentity = streamIdentity(mediaItem, loadedVideoMode)
                    renderedVideoFrame = false
                    refreshVideoSurfaceState()
                    videoFrameWatchdogJob?.cancel()
                    videoFrameWatchdogJob = null
                    startSponsorBlockMonitor(queueTrack)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        scheduleVideoFrameWatchdog()
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

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    if (!playWhenReady) {
                        videoFrameWatchdogJob?.cancel()
                        videoFrameWatchdogJob = null
                        return
                    }
                    if (connected.playbackState == Player.STATE_READY) scheduleVideoFrameWatchdog()
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (ignoreEndedFromManualStop || connected.mediaItemCount == 0) return
                    val track = loadedTrack ?: return
                    val message = cleanError(error)
                    if (isLocalPlayback(track)) {
                        recoveryInFlight = false
                        recoveryAttempts = 0
                        sponsorJob?.cancel()
                        sponsorJob = null
                        clearLoadedState()
                        connected.pause()
                        onError?.invoke(message)
                        return
                    }
                    val terminalFailure = isTerminalPlaybackFailure(classifyPlaybackFailureReason(message))
                    if (!terminalFailure && isRecoverable(error) && !recoveryInFlight && recoveryAttempts < 3 && onRecoverableStreamError != null) {
                        recoveryInFlight = true
                        recoveryAttempts++
                        val playWhenReadyBeforeError = connected.playWhenReady
                        sponsorJob?.cancel()
                        sponsorJob = null
                        videoFrameWatchdogJob?.cancel()
                        videoFrameWatchdogJob = null
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

    val bufferedPositionMs: Long
        get() = controller?.bufferedPosition?.coerceAtLeast(0L) ?: 0L

    val durationMs: Long
        get() {
            val duration = controller?.duration ?: return 0L
            return if (duration == C.TIME_UNSET) 0L else duration.coerceAtLeast(0L)
        }

    fun play(track: Track, videoMode: Boolean = false) {
        require(track.streamUrl.isNotBlank()) { "Stream URL assente per ${track.title}" }
        invalidateDelayedVideoRecovery()
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
        val sameTrack = loadedTrack?.id == track.id
        if (!sameTrack || loadedStreamIdentity != identity || loadedVideoMode != videoMode) {
            replaceSource(
                track = track,
                positionMs = if (sameTrack) active.currentPosition.coerceAtLeast(0L) else 0L,
                videoMode = videoMode,
                playWhenReady = true
            )
            return
        }
        active.playWhenReady = true
        active.play()
        startSponsorBlockMonitor(track)
        scheduleVideoFrameWatchdog()
    }

    fun replaceSource(
        track: Track,
        positionMs: Long,
        videoMode: Boolean,
        playWhenReady: Boolean
    ) {
        require(track.streamUrl.isNotBlank()) { "Stream URL assente per ${track.title}" }
        invalidateDelayedVideoRecovery()
        val active = controller
        if (active == null) {
            pendingPlayback = PendingPlayback(track, positionMs.coerceAtLeast(0L), videoMode, playWhenReady)
            return
        }
        if (videoMode && !track.hasVideoPlaybackPayload()) {
            scheduleInvalidVideoRecovery(track, positionMs, playWhenReady)
            return
        }

        ignoreEndedFromManualStop = false
        val recoveryReplacement = recoveryInFlight
        recoveryInFlight = false
        if (!recoveryReplacement) recoveryAttempts = 0
        val sameTrack = loadedTrack?.id == track.id
        val startPositionMs = replacementStartPosition(
            sameTrack = sameTrack,
            requestedPositionMs = positionMs,
            activePositionMs = if (active.mediaItemCount > 0) active.currentPosition else 0L,
            durationMs = track.durationMs,
            allowBackwardActivePosition = sameTrack && !recoveryReplacement
        )
        val effectivePlayWhenReady = if (sameTrack && !recoveryReplacement && active.mediaItemCount > 0) {
            active.playWhenReady
        } else {
            playWhenReady
        }
        loadedTrack = track
        loadedStreamIdentity = streamIdentity(track, videoMode)
        loadedVideoMode = videoMode
        renderedVideoFrame = false
        refreshVideoSurfaceState()
        videoFrameWatchdogJob?.cancel()
        videoFrameWatchdogJob = null
        PlaybackService.consumePreparedQueueNext(track.id)
        applyPlaybackParameters(active)
        active.playWhenReady = effectivePlayWhenReady
        active.setMediaItem(LevyraMediaItemFactory.build(track, videoMode), startPositionMs)
        active.prepare()
        if (effectivePlayWhenReady) active.play() else active.pause()
        startSponsorBlockMonitor(track)
        scheduleVideoFrameWatchdog()
    }

    fun failRecovery(message: String) {
        invalidateDelayedVideoRecovery()
        recoveryInFlight = false
        recoveryAttempts = 0
        sponsorJob?.cancel()
        sponsorJob = null
        clearLoadedState()
        controller?.pause()
        onError?.invoke(message)
    }

    fun pause() {
        videoFrameWatchdogJob?.cancel()
        videoFrameWatchdogJob = null
        controller?.pause()
    }

    fun stop() {
        invalidateDelayedVideoRecovery()
        ignoreEndedFromManualStop = true
        recoveryInFlight = false
        recoveryAttempts = 0
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

    fun setPremiumAudioSettings(
        settings: LevyraAudioSettings,
        audioNormalization: Boolean
    ) {
        audioSettings = settings.normalized()
        controller?.let { applyPlaybackParameters(it) }
        PlaybackService.applyPremiumAudioSettings(audioSettings, audioNormalization)
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
        invalidateDelayedVideoRecovery()
        observedServicePlayer?.removeListener(videoRenderListener)
        observedServicePlayer = null
        sponsorJob?.cancel()
        sponsorJob = null
        scope.cancel()
        controller?.release()
        controller = null
        MediaController.releaseFuture(controllerFuture)
    }

    private fun scheduleInvalidVideoRecovery(track: Track, positionMs: Long, playWhenReady: Boolean) {
        if (recoveryInFlight || recoveryAttempts >= 3) {
            onError?.invoke("Sorgente video non valida")
            return
        }
        val callback = onRecoverableStreamError ?: run {
            onError?.invoke("Sorgente video non valida")
            return
        }
        val generation = playbackGeneration
        invalidVideoRecoveryJob?.cancel()
        invalidVideoRecoveryJob = scope.launch {
            delay(1L)
            if (!shouldRunDelayedVideoRecovery(generation, playbackGeneration, recoveryInFlight, recoveryAttempts)) {
                return@launch
            }
            recoveryInFlight = true
            recoveryAttempts++
            callback(
                track,
                positionMs.coerceAtLeast(0L),
                true,
                playWhenReady,
                "Sorgente video priva di una traccia video"
            )
        }
    }

    private fun scheduleVideoFrameWatchdog() {
        videoFrameWatchdogJob?.cancel()
        val active = controller ?: return
        val track = loadedTrack ?: return
        val identity = loadedStreamIdentity ?: return
        val generation = playbackGeneration
        val surfaceAttached = refreshVideoSurfaceState()
        if (
            !shouldRunVideoFrameWatchdog(
                videoMode = loadedVideoMode,
                hasVideoPayload = track.hasVideoPlaybackPayload(),
                renderedVideoFrame = renderedVideoFrame,
                surfaceAttached = surfaceAttached,
                playbackReady = active.playbackState == Player.STATE_READY,
                playWhenReady = active.playWhenReady
            )
        ) {
            return
        }

        videoFrameWatchdogJob = scope.launch {
            delay(VIDEO_FIRST_FRAME_TIMEOUT_MS)
            val current = controller ?: return@launch
            if (generation != playbackGeneration) return@launch
            if (!loadedVideoMode || loadedStreamIdentity != identity || loadedTrack?.id != track.id) return@launch
            if (
                !shouldRunVideoFrameWatchdog(
                    videoMode = loadedVideoMode,
                    hasVideoPayload = track.hasVideoPlaybackPayload(),
                    renderedVideoFrame = renderedVideoFrame,
                    surfaceAttached = refreshVideoSurfaceState(),
                    playbackReady = current.playbackState == Player.STATE_READY,
                    playWhenReady = current.playWhenReady
                )
            ) {
                return@launch
            }
            if (recoveryInFlight || recoveryAttempts >= 3) return@launch
            val callback = onRecoverableStreamError ?: return@launch

            recoveryInFlight = true
            recoveryAttempts++
            val playWhenReadyBeforeRecovery = current.playWhenReady
            sponsorJob?.cancel()
            sponsorJob = null
            current.pause()
            callback(
                track,
                current.currentPosition.coerceAtLeast(0L),
                true,
                playWhenReadyBeforeRecovery,
                "Decoder video senza fotogrammi renderizzati"
            )
        }
    }

    private fun invalidateDelayedVideoRecovery() {
        playbackGeneration++
        invalidVideoRecoveryJob?.cancel()
        invalidVideoRecoveryJob = null
        videoFrameWatchdogJob?.cancel()
        videoFrameWatchdogJob = null
    }

    private fun refreshVideoSurfaceState(): Boolean {
        val size = observedServicePlayer?.surfaceSize
        if (size != null && size.width > 0 && size.height > 0) {
            videoSurfaceAttached = true
        }
        return videoSurfaceAttached
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
        renderedVideoFrame = false
        videoSurfaceAttached = false
        invalidVideoRecoveryJob?.cancel()
        invalidVideoRecoveryJob = null
        videoFrameWatchdogJob?.cancel()
        videoFrameWatchdogJob = null
    }

    private fun streamIdentity(track: Track, videoMode: Boolean): String {
        return buildString {
            append(track.streamUrl)
            append('|')
            append(if (videoMode) track.videoStreamUrl else "")
            append('|')
            append(videoMode)
        }
    }

    private fun streamIdentity(mediaItem: androidx.media3.common.MediaItem, videoMode: Boolean): String {
        return buildString {
            append(mediaItem.localConfiguration?.uri?.toString().orEmpty())
            append('|')
            append(
                if (videoMode) {
                    mediaItem.mediaMetadata.extras?.getString(PlaybackService.EXTRA_VIDEO_URL).orEmpty()
                } else {
                    ""
                }
            )
            append('|')
            append(videoMode)
        }
    }

    private fun isLocalPlayback(track: Track): Boolean {
        val stream = track.streamUrl.trim()
        return track.source.equals("Offline", ignoreCase = true) ||
            stream.startsWith("content://", ignoreCase = true) ||
            stream.startsWith("file://", ignoreCase = true)
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
        return isRecoverablePlaybackErrorCode(error.errorCode)
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
