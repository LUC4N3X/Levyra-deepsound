@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.luc4n3x.levyra.ui

import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.luc4n3x.levyra.data.SamplePreviewLoader
import com.luc4n3x.levyra.data.SpotifyClipCanvasRegistry
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.motion.MotionArtworkNetworkPolicy
import com.luc4n3x.levyra.player.LevyraMediaItemFactory
import com.luc4n3x.levyra.player.LevyraYoutubeDataSource
import com.luc4n3x.levyra.player.PlaybackService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import kotlinx.coroutines.awaitCancellation

@Composable
internal fun SpotifySamplePreview(track: Track) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val enabled = LocalAnimationsEnabled.current && LocalLevyraVisualCapabilities.current.decorativeMotion
    val loader = remember(context) { SamplePreviewLoader(context) }
    var visible by remember(track.id) { mutableStateOf(false) }
    var preview by remember(track.id) { mutableStateOf<Track?>(null) }
    LaunchedEffect(track.id, enabled, visible, lifecycle) {
        if (!enabled || !visible) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                if (MotionArtworkNetworkPolicy.canUseMotionArtwork(context)) {
                    val canvas = SpotifyClipCanvasRegistry.canvasUrl(track)
                    preview = if (canvas.isNotBlank()) track.copy(streamUrl = canvas, videoStreamUrl = canvas) else loader.load(track)
                }
                awaitCancellation()
            } finally {
                preview = null
            }
        }
    }
    Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        visible = bounds.width > 0f && bounds.height > 0f && bounds.bottom > 0f && bounds.top < view.height && bounds.right > 0f && bounds.left < view.width
    }) {
        preview?.let { SpotifySamplePreviewVideo(it, canvas = SpotifyClipCanvasRegistry.canvasUrl(track)) }
    }
}

@Composable
internal fun SpotifyCanvasLoop(track: Track?, modifier: Modifier = Modifier, syncToAudio: Boolean = false) {
    val canvas = SpotifyClipCanvasRegistry.canvasUrl(track)
    if (track == null || canvas.isBlank()) return
    val playWhenReady = if (syncToAudio) sampleAudioIsPlaying(track.id) else true
    Box(modifier) { SpotifySamplePreviewVideo(track, canvas, playWhenReady) }
}

@Composable
private fun sampleAudioIsPlaying(trackId: String): Boolean {
    val player = PlaybackService.activePlayerFlow.collectAsStateWithLifecycle().value
    var playing by remember(player, trackId) {
        mutableStateOf(player?.isPlaying == true && player.currentMediaItem?.mediaId == trackId)
    }
    DisposableEffect(player, trackId) {
        val listener = object : Player.Listener {
            override fun onEvents(active: Player, events: Player.Events) {
                playing = active.isPlaying && active.currentMediaItem?.mediaId == trackId
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    return playing
}

@Composable
private fun SpotifySamplePreviewVideo(track: Track, canvas: String = "", playWhenReady: Boolean = true) {
    val context = LocalContext.current
    var ready by remember(track.id) { mutableStateOf(false) }
    var attempt by remember(track.id, canvas) { mutableStateOf(0) }
    val player = remember(track.id, track.streamUrl, canvas, attempt) {
        val http = OkHttpDataSource.Factory(LevyraHttpClientFactory.media(context))
        val factory = if (canvas.isNotBlank()) http else LevyraYoutubeDataSource.Factory(http)
        val builder = if (canvas.isNotBlank()) ExoPlayer.Builder(context, canvasRenderersFactory(context, canvas)) else ExoPlayer.Builder(context)
        builder.setMediaSourceFactory(DefaultMediaSourceFactory(factory))
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(1_000, 5_000, 500, 500).setTargetBufferBytes(6 * 1024 * 1024).build()).build().apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ONE
                trackSelectionParameters = trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true).build()
            }
    }
    val texture = remember(player) { TextureView(context) }
    var videoSize by remember(player) { mutableStateOf(androidx.media3.common.VideoSize.UNKNOWN) }
    var viewSize by remember(player) { mutableStateOf(IntSize.Zero) }
    DisposableEffect(player, texture) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { ready = true }
            override fun onVideoSizeChanged(size: androidx.media3.common.VideoSize) { videoSize = size }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                ready = false
                player.stop()
                if (canvas.isNotBlank() && attempt < 2 && CanvasDecoderBlocklist.recordFailure(canvas, error)) attempt++
            }
        }
        player.addListener(listener)
        player.setVideoTextureView(texture)
        player.setMediaItem(if (canvas.isNotBlank()) MediaItem.fromUri(canvas) else LevyraMediaItemFactory.build(track, videoMode = true))
        player.prepare()
        player.playWhenReady = playWhenReady
        onDispose {
            player.removeListener(listener)
            player.clearVideoTextureView(texture)
            player.release()
        }
    }
    LaunchedEffect(player, playWhenReady) {
        player.playWhenReady = playWhenReady
    }
    AndroidView(factory = { texture }, update = { surface ->
        if (viewSize.width > 0 && viewSize.height > 0 && videoSize.width > 0 && videoSize.height > 0) {
            val sourceAspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
            val targetAspect = viewSize.width.toFloat() / viewSize.height
            surface.setTransform(android.graphics.Matrix().apply {
                setScale(maxOf(1f, sourceAspect / targetAspect), maxOf(1f, targetAspect / sourceAspect), viewSize.width / 2f, viewSize.height / 2f)
            })
        }
    }, modifier = Modifier.fillMaxSize().onSizeChanged { viewSize = it }.graphicsLayer { alpha = if (ready) 1f else 0f })
}
