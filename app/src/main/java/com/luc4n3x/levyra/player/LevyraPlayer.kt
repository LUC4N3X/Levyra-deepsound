package com.luc4n3x.levyra.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import androidx.media3.common.util.UnstableApi

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
            active.setMediaItem(buildItem(track))
            active.prepare()
        }
        active.play()
    }

    private fun buildItem(track: Track): MediaItem {
        val art = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
        val extras = android.os.Bundle().apply {
            putString("levyra.title", track.title)
            putString("levyra.artist", track.artist)
            putString("levyra.album", track.album)
            putLong("levyra.durationMs", track.durationMs.coerceAtLeast(0L))
            putString("levyra.source", track.source)
            if (track.videoStreamUrl.isNotBlank()) {
                putString(PlaybackService.EXTRA_VIDEO_URL, track.videoStreamUrl)
                putString(PlaybackService.EXTRA_VIDEO_CACHE_KEY, LevyraPlaybackCacheKey.video(track))
            }
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setDisplayTitle(track.title)
            .setArtist(track.artist)
            .setSubtitle(track.artist)
            .setAlbumTitle(track.album.ifBlank { "Levyra" })
            .apply { if (art.isNotBlank()) setArtworkUri(Uri.parse(art)) }
            .setExtras(extras)
            .build()
        return MediaItem.Builder()
            .setUri(track.streamUrl)
            .setMimeType(mimeTypeFor(track.streamUrl))
            .setCustomCacheKey(LevyraPlaybackCacheKey.stream(track))
            .setMediaId(track.id.ifBlank { track.videoUrl.ifBlank { "${track.artist}-${track.title}" } })
            .setMediaMetadata(metadata)
            .build()
    }


    private fun mimeTypeFor(url: String): String {
        val clean = url.substringBefore('?').lowercase()
        return when {
            clean.endsWith(".m3u8") -> "application/x-mpegURL"
            clean.endsWith(".mpd") -> "application/dash+xml"
            clean.endsWith(".webm") -> "audio/webm"
            clean.endsWith(".mp3") -> "audio/mpeg"
            clean.endsWith(".m4a") || clean.endsWith(".mp4") -> "audio/mp4"
            else -> "audio/mp4"
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
