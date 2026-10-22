package com.luc4n3x.levyra.feature.discovery

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.luc4n3x.levyra.domain.SponsorSegment
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.LevyraMediaCache
import com.luc4n3x.levyra.player.LevyraYoutubeDataSource
import com.luc4n3x.levyra.player.PlaybackNetworkStack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DiscoveryPreviewStatus { Idle, Loading, Playing, Paused, Ended, Failed }

@Immutable
data class DiscoveryPreviewState(val key: String = "", val status: DiscoveryPreviewStatus = DiscoveryPreviewStatus.Idle) {
    fun isFor(cardKey: String): Boolean = key == cardKey
    val audible: Boolean get() = status == DiscoveryPreviewStatus.Playing || status == DiscoveryPreviewStatus.Loading
}

data class DiscoveryPreviewWindow(val startMs: Long, val endMs: Long)

internal object DiscoveryPreviewWindows {
    const val PREVIEW_MS = 30_000L
    private const val SHORT_TRACK_MS = 40_000L
    private const val INTRO_TOLERANCE_MS = 1_000L

    fun of(track: Track): DiscoveryPreviewWindow {
        val duration = track.durationMs
        val start = highlightStart(track.sponsorSegments, duration) ?: introEnd(track.sponsorSegments, duration) ?: 0L
        val clampedStart = if (duration > 0L) start.coerceIn(0L, (duration - PREVIEW_MS).coerceAtLeast(0L)) else start
        val end = if (duration in 1..SHORT_TRACK_MS) duration else clampedStart + PREVIEW_MS
        return DiscoveryPreviewWindow(clampedStart, end)
    }

    private fun highlightStart(segments: List<SponsorSegment>, duration: Long): Long? =
        segments.firstOrNull { it.category == "poi_highlight" && it.startMs >= 0L && (duration <= 0L || it.startMs < duration) }?.startMs

    private fun introEnd(segments: List<SponsorSegment>, duration: Long): Long? =
        segments.firstOrNull {
            it.category == "music_offtopic" && it.startMs <= INTRO_TOLERANCE_MS && it.endMs > it.startMs && (duration <= 0L || it.endMs < duration / 2)
        }?.endMs
}

@OptIn(UnstableApi::class)
internal class DiscoveryPreviewPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(DiscoveryPreviewState())
    val state: StateFlow<DiscoveryPreviewState> = _state.asStateFlow()
    private var player: ExoPlayer? = null
    private var activeKey = ""

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = publish()
        override fun onPlaybackStateChanged(playbackState: Int) = publish()
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = publish()
        override fun onPlayerError(error: PlaybackException) {
            if (activeKey.isNotBlank()) _state.value = DiscoveryPreviewState(activeKey, DiscoveryPreviewStatus.Failed)
        }
    }

    fun markLoading(key: String) {
        activeKey = key
        player?.let { engine ->
            engine.playWhenReady = false
            engine.stop()
        }
        _state.value = DiscoveryPreviewState(key, DiscoveryPreviewStatus.Loading)
    }

    fun play(key: String, track: Track) {
        if (activeKey != key) return
        if (track.streamUrl.isBlank()) {
            markFailed(key)
            return
        }
        val window = DiscoveryPreviewWindows.of(track)
        val engine = player ?: build().also { player = it }
        engine.setMediaItem(
            MediaItem.Builder()
                .setUri(track.streamUrl)
                .setMediaId("discovery-preview:$key")
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(window.startMs)
                        .setEndPositionMs(window.endMs)
                        .build()
                )
                .build()
        )
        engine.prepare()
        engine.playWhenReady = true
        publish()
    }

    fun markFailed(key: String) {
        if (activeKey != key) return
        player?.stop()
        _state.value = DiscoveryPreviewState(key, DiscoveryPreviewStatus.Failed)
    }

    fun pause() {
        val engine = player
        if (engine == null || engine.mediaItemCount == 0) {
            if (_state.value.status == DiscoveryPreviewStatus.Loading) {
                activeKey = ""
                _state.value = _state.value.copy(status = DiscoveryPreviewStatus.Paused)
            }
            return
        }
        engine.playWhenReady = false
        publish()
    }

    fun resume(key: String): Boolean {
        val engine = player ?: return false
        if (activeKey != key || engine.mediaItemCount == 0 || engine.playbackState == Player.STATE_IDLE) return false
        if (engine.playbackState == Player.STATE_ENDED) engine.seekToDefaultPosition()
        engine.playWhenReady = true
        publish()
        return true
    }

    fun stop() {
        activeKey = ""
        player?.let { engine ->
            engine.playWhenReady = false
            engine.stop()
            engine.clearMediaItems()
        }
        _state.value = DiscoveryPreviewState()
    }

    fun release() {
        stop()
        player?.let { engine ->
            engine.removeListener(listener)
            engine.release()
        }
        player = null
    }

    private fun publish() {
        val engine = player ?: return
        if (activeKey.isBlank()) return
        val current = _state.value
        if (current.status == DiscoveryPreviewStatus.Failed && current.key == activeKey) return
        val status = when {
            engine.playbackState == Player.STATE_ENDED -> DiscoveryPreviewStatus.Ended
            engine.isPlaying -> DiscoveryPreviewStatus.Playing
            engine.playWhenReady -> DiscoveryPreviewStatus.Loading
            else -> DiscoveryPreviewStatus.Paused
        }
        _state.value = DiscoveryPreviewState(activeKey, status)
    }

    private fun build(): ExoPlayer {
        val upstream = LevyraYoutubeDataSource.Factory(
            PlaybackNetworkStack.playbackFactory(appContext)
                .setDefaultRequestProperties(mapOf("Accept" to "*/*", "Accept-Encoding" to "identity"))
        )
        val dataSource = CacheDataSource.Factory()
            .setCache(LevyraMediaCache.get(appContext))
            .setUpstreamDataSourceFactory(upstream)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { it.addListener(listener) }
    }
}
