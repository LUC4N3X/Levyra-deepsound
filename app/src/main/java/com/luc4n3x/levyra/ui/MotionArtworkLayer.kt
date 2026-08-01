@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.luc4n3x.levyra.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.TextureView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.feature.motion.MotionArtworkNetworkPolicy
import kotlinx.coroutines.delay

@Composable
internal fun MotionArtworkLayer(
    artwork: MotionArtwork?,
    enabled: Boolean,
    isPlaying: Boolean,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    staticArtwork: @Composable () -> Unit
) {
    val lifecycleActive = rememberMotionArtworkLifecycleActive()
    val environment = rememberMotionArtworkEnvironment(enabled && lifecycleActive)
    var videoUnavailable by remember(artwork?.identityKey, artwork?.url, artwork?.mimeType) {
        mutableStateOf(false)
    }
    val showVideo = artwork != null &&
        enabled &&
        lifecycleActive &&
        environment.remoteAllowed &&
        !videoUnavailable
    val animateStatic = enabled &&
        lifecycleActive &&
        environment.localAllowed &&
        isPlaying &&
        !showVideo

    Box(modifier = modifier) {
        MotionArtworkStaticFallback(
            animated = animateStatic,
            cornerRadius = cornerRadius,
            modifier = Modifier.fillMaxSize(),
            content = staticArtwork
        )
        if (showVideo && artwork != null) {
            MotionArtworkVideo(
                artwork = artwork,
                isPlaying = isPlaying,
                cornerRadius = cornerRadius,
                onUnavailable = { videoUnavailable = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun rememberMotionArtworkLifecycleActive(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var active by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            active = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return active
}

@Composable
private fun rememberMotionArtworkEnvironment(observe: Boolean): MotionArtworkEnvironment {
    val context = LocalContext.current.applicationContext
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(context, observe) {
        if (!observe) return@DisposableEffect onDispose { }
        val mainHandler = Handler(Looper.getMainLooper())
        val refresh: () -> Unit = {
            mainHandler.post { revision++ }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                refresh()
            }
        }
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED)
        }
        val receiverRegistered = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
        }.isSuccess
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                refresh()
            }

            override fun onLost(network: Network) {
                refresh()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                refresh()
            }
        }
        val callbackRegistered = runCatching {
            connectivity?.registerDefaultNetworkCallback(callback)
        }.isSuccess
        onDispose {
            if (receiverRegistered) runCatching { context.unregisterReceiver(receiver) }
            if (callbackRegistered) runCatching { connectivity?.unregisterNetworkCallback(callback) }
            mainHandler.removeCallbacksAndMessages(null)
        }
    }
    return remember(context, observe, revision) {
        if (!observe) {
            MotionArtworkEnvironment(remoteAllowed = false, localAllowed = false)
        } else {
            MotionArtworkEnvironment(
                remoteAllowed = MotionArtworkNetworkPolicy.canUseMotionArtwork(context),
                localAllowed = MotionArtworkNetworkPolicy.canAnimateLocally(context)
            )
        }
    }
}

@Composable
private fun MotionArtworkStaticFallback(
    animated: Boolean,
    cornerRadius: Dp,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(modifier = modifier.clip(shape)) {
        if (!animated) {
            Box(modifier = Modifier.fillMaxSize()) { content() }
            return@Box
        }

        var artworkSize by remember { mutableStateOf(IntSize.Zero) }
        val transition = rememberInfiniteTransition(label = "static-artwork-motion")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = STATIC_ARTWORK_MOTION_DURATION_MS,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "static-artwork-motion-progress"
        )
        val direction = progress * 2f - 1f
        val scale = 1.04f + progress * 0.025f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { artworkSize = it }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = artworkSize.width * 0.014f * direction
                    translationY = artworkSize.height * -0.010f * direction
                    rotationZ = direction * 0.16f
                }
        ) {
            content()
        }
    }
}

@Composable
private fun MotionArtworkVideo(
    artwork: MotionArtwork,
    isPlaying: Boolean,
    cornerRadius: Dp,
    onUnavailable: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    var firstFrameRendered by remember(artwork.identityKey, artwork.url, artwork.mimeType) {
        mutableStateOf(false)
    }
    var failed by remember(artwork.identityKey, artwork.url, artwork.mimeType) { mutableStateOf(false) }
    val player = remember(artwork.identityKey, artwork.url, artwork.mimeType) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .setMaxVideoSize(1280, 1280)
                .setMaxVideoBitrate(4_000_000)
                .build()
        }
    }
    val textureView = remember(player) { TextureView(context) }
    val videoAlpha by animateFloatAsState(
        targetValue = if (firstFrameRendered && !failed) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "motion-artwork-alpha"
    )

    DisposableEffect(player, textureView, artwork.url, artwork.mimeType) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
            }

            override fun onPlayerError(error: PlaybackException) {
                failed = true
                currentOnUnavailable()
            }
        }
        player.addListener(listener)
        player.setVideoTextureView(textureView)
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(artwork.url)
                .setMimeType(artwork.mimeType.takeIf { it.isNotBlank() })
                .build()
        )
        player.prepare()
        onDispose {
            player.removeListener(listener)
            player.clearVideoTextureView(textureView)
            player.release()
        }
    }

    LaunchedEffect(player, isPlaying, failed) {
        if (failed) {
            player.playWhenReady = false
            player.stop()
            player.clearMediaItems()
        } else {
            player.playWhenReady = isPlaying
        }
    }

    LaunchedEffect(player, isPlaying, firstFrameRendered, failed) {
        if (!isPlaying || firstFrameRendered || failed) return@LaunchedEffect
        delay(VIDEO_FIRST_FRAME_TIMEOUT_MS)
        if (!firstFrameRendered && !failed) currentOnUnavailable()
    }

    AndroidView(
        factory = { textureView },
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .graphicsLayer { alpha = videoAlpha }
    )
}

private data class MotionArtworkEnvironment(
    val remoteAllowed: Boolean,
    val localAllowed: Boolean
)

private const val STATIC_ARTWORK_MOTION_DURATION_MS = 12_000
private const val VIDEO_FIRST_FRAME_TIMEOUT_MS = 6_000L
