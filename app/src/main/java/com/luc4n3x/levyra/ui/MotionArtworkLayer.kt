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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.feature.motion.MotionArtworkNetworkPolicy
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal enum class MotionArtworkPresentation {
    Card,
    Immersive
}

@Composable
internal fun MotionArtworkLayer(
    artwork: MotionArtwork?,
    enabled: Boolean,
    isPlaying: Boolean,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    presentation: MotionArtworkPresentation = MotionArtworkPresentation.Card,
    staticArtwork: @Composable () -> Unit
) {
    val lifecycleActive = rememberMotionArtworkLifecycleActive()
    val environment = rememberMotionArtworkEnvironment(enabled && lifecycleActive)
    var videoUnavailable by remember(artwork?.identityKey, artwork?.url, artwork?.mimeType) {
        mutableStateOf(false)
    }
    var videoReady by remember(artwork?.identityKey, artwork?.url, artwork?.mimeType) {
        mutableStateOf(false)
    }
    var videoRetryCount by remember(artwork?.identityKey, artwork?.url, artwork?.mimeType) {
        mutableStateOf(0)
    }
    val videoArtwork = artwork?.takeIf {
        enabled &&
            lifecycleActive &&
            environment.remoteAllowed &&
            !videoUnavailable
    }
    LaunchedEffect(videoArtwork) {
        if (videoArtwork == null) videoReady = false
    }
    LaunchedEffect(
        videoUnavailable,
        enabled,
        lifecycleActive,
        environment.remoteAllowed,
        isPlaying,
    ) {
        if (
            !videoUnavailable ||
            !enabled ||
            !lifecycleActive ||
            !environment.remoteAllowed ||
            !isPlaying ||
            videoRetryCount >= MAX_VIDEO_RETRIES
        ) {
            return@LaunchedEffect
        }
        delay(VIDEO_RETRY_DELAY_MS)
        if (
            enabled &&
            lifecycleActive &&
            environment.remoteAllowed &&
            isPlaying &&
            videoRetryCount < MAX_VIDEO_RETRIES
        ) {
            videoRetryCount += 1
            videoUnavailable = false
        }
    }
    val animateStatic = enabled &&
        lifecycleActive &&
        environment.localAllowed &&
        isPlaying &&
        !videoReady
    val staticBedAlpha by animateFloatAsState(
        targetValue = if (videoReady) 0f else 1f,
        animationSpec = tween(
            durationMillis = STATIC_ARTWORK_BED_FADE_MS,
            delayMillis = if (videoReady) VIDEO_FADE_IN_MS else 0,
            easing = FastOutSlowInEasing
        ),
        label = "motion-artwork-bed-alpha"
    )

    Box(modifier = modifier) {
        MotionArtworkStaticFallback(
            animated = animateStatic,
            cornerRadius = cornerRadius,
            alpha = { staticBedAlpha },
            modifier = Modifier.fillMaxSize(),
            content = staticArtwork
        )
        if (videoArtwork != null) {
            MotionArtworkVideo(
                artwork = videoArtwork,
                isPlaying = isPlaying,
                cornerRadius = cornerRadius,
                presentation = presentation,
                onFirstFrame = {
                    videoReady = true
                    videoRetryCount = 0
                },
                onUnavailable = {
                    videoReady = false
                    videoUnavailable = true
                },
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
    alpha: () -> Float,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    var artworkSize by remember { mutableStateOf(IntSize.Zero) }
    val zoomPhase = remember { Animatable(0f) }
    val horizontalDrift = remember { Animatable(0f) }
    val verticalDrift = remember { Animatable(0f) }
    val motionAmount by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (animated) STATIC_ARTWORK_MOTION_ENTER_MS else STATIC_ARTWORK_MOTION_EXIT_MS,
            easing = FastOutSlowInEasing
        ),
        label = "static-artwork-motion-amount"
    )

    LaunchedEffect(animated) {
        if (!animated) {
            coroutineScope {
                launch {
                    zoomPhase.animateTo(
                        0f,
                        tween(STATIC_ARTWORK_MOTION_EXIT_MS, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    horizontalDrift.animateTo(
                        0f,
                        tween(STATIC_ARTWORK_MOTION_EXIT_MS, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    verticalDrift.animateTo(
                        0f,
                        tween(STATIC_ARTWORK_MOTION_EXIT_MS, easing = FastOutSlowInEasing)
                    )
                }
            }
            return@LaunchedEffect
        }
        coroutineScope {
            launch {
                while (isActive) {
                    zoomPhase.animateTo(
                        1f,
                        tween(STATIC_ARTWORK_ZOOM_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                    zoomPhase.animateTo(
                        0f,
                        tween(STATIC_ARTWORK_ZOOM_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                }
            }
            launch {
                while (isActive) {
                    horizontalDrift.animateTo(
                        1f,
                        tween(STATIC_ARTWORK_HORIZONTAL_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                    horizontalDrift.animateTo(
                        -1f,
                        tween(STATIC_ARTWORK_HORIZONTAL_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                }
            }
            launch {
                while (isActive) {
                    verticalDrift.animateTo(
                        -1f,
                        tween(STATIC_ARTWORK_VERTICAL_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                    verticalDrift.animateTo(
                        1f,
                        tween(STATIC_ARTWORK_VERTICAL_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    Box(modifier = modifier.clip(shape)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { artworkSize = it }
                .graphicsLayer {
                    this.alpha = alpha()
                    val amount = motionAmount
                    val scale = 1f + amount * (0.042f + zoomPhase.value * 0.022f)
                    scaleX = scale
                    scaleY = scale
                    translationX = artworkSize.width * 0.016f * horizontalDrift.value * amount
                    translationY = artworkSize.height * 0.012f * verticalDrift.value * amount
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
    presentation: MotionArtworkPresentation,
    onFirstFrame: () -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val currentOnFirstFrame by rememberUpdatedState(onFirstFrame)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    var firstFrameRendered by remember(artwork.identityKey, artwork.url, artwork.mimeType) {
        mutableStateOf(false)
    }
    var failed by remember(artwork.identityKey, artwork.url, artwork.mimeType) { mutableStateOf(false) }
    var videoSize by remember(artwork.identityKey, artwork.url, artwork.mimeType) {
        mutableStateOf(VideoSize.UNKNOWN)
    }
    var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
    val maxZoom = when (presentation) {
        MotionArtworkPresentation.Card -> MotionArtworkCardMaxZoom
        MotionArtworkPresentation.Immersive -> MotionArtworkImmersiveMaxZoom
    }
    val player = remember(artwork.identityKey, artwork.url, artwork.mimeType, presentation) {
        val decodeLimit = when (presentation) {
            MotionArtworkPresentation.Card -> CARD_MAX_VIDEO_DIMENSION
            MotionArtworkPresentation.Immersive -> IMMERSIVE_MAX_VIDEO_DIMENSION
        }
        val bitrateLimit = when (presentation) {
            MotionArtworkPresentation.Card -> CARD_MAX_VIDEO_BITRATE
            MotionArtworkPresentation.Immersive -> IMMERSIVE_MAX_VIDEO_BITRATE
        }
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .setViewportSize(decodeLimit, decodeLimit, false)
                .setMaxVideoSize(decodeLimit, decodeLimit)
                .setMaxVideoBitrate(bitrateLimit)
                .build()
        }
    }
    val textureView = remember(player) { TextureView(context) }
    val videoAlpha by animateFloatAsState(
        targetValue = if (firstFrameRendered && !failed) 1f else 0f,
        animationSpec = tween(durationMillis = VIDEO_FADE_IN_MS, easing = FastOutSlowInEasing),
        label = "motion-artwork-alpha"
    )

    DisposableEffect(player, textureView, artwork.url, artwork.mimeType) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
                currentOnFirstFrame()
            }

            override fun onVideoSizeChanged(size: VideoSize) {
                videoSize = size
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
            .onSizeChanged { surfaceSize = it }
            .graphicsLayer {
                alpha = videoAlpha
                val fit = motionArtworkFit(
                    videoWidth = videoSize.width,
                    videoHeight = videoSize.height,
                    pixelWidthHeightRatio = videoSize.pixelWidthHeightRatio,
                    containerWidth = surfaceSize.width,
                    containerHeight = surfaceSize.height,
                    maxZoom = maxZoom
                )
                scaleX = fit.scaleX
                scaleY = fit.scaleY
            }
    )
}

private data class MotionArtworkEnvironment(
    val remoteAllowed: Boolean,
    val localAllowed: Boolean
)

private const val STATIC_ARTWORK_ZOOM_DURATION_MS = 11_000
private const val STATIC_ARTWORK_HORIZONTAL_DURATION_MS = 14_000
private const val STATIC_ARTWORK_VERTICAL_DURATION_MS = 17_000
private const val STATIC_ARTWORK_MOTION_ENTER_MS = 360
private const val STATIC_ARTWORK_MOTION_EXIT_MS = 220
private const val STATIC_ARTWORK_BED_FADE_MS = 420
private const val VIDEO_FADE_IN_MS = 620
private const val VIDEO_FIRST_FRAME_TIMEOUT_MS = 9_000L
private const val VIDEO_RETRY_DELAY_MS = 4_000L
private const val MAX_VIDEO_RETRIES = 1
private const val CARD_MAX_VIDEO_DIMENSION = 1_280
private const val IMMERSIVE_MAX_VIDEO_DIMENSION = 1_920
private const val CARD_MAX_VIDEO_BITRATE = 4_000_000
private const val IMMERSIVE_MAX_VIDEO_BITRATE = 8_000_000
