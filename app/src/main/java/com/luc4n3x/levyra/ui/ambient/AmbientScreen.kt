package com.luc4n3x.levyra.ui.ambient

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.domain.LevyraAmbientSettings
import com.luc4n3x.levyra.domain.ambientPixelShiftOffset
import com.luc4n3x.levyra.ui.MotionArtworkLayer
import com.luc4n3x.levyra.ui.MotionArtworkPresentation
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
internal fun AmbientScreen(
    state: AmbientUiState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExit: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val settings = state.settings
    var lastInteractionAt by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var controlsVisible by remember { mutableStateOf(true) }
    var dimmed by remember { mutableStateOf(false) }
    var shiftStep by remember { mutableIntStateOf(0) }
    val covered = rememberProximityCovered(settings.proximityBlackout)

    LaunchedEffect(lastInteractionAt, settings.autoDim, settings.autoDimAfterSeconds) {
        controlsVisible = true
        dimmed = false
        delay(LevyraAmbientSettings.CONTROLS_VISIBLE_MS)
        controlsVisible = false
        if (settings.autoDim) {
            delay(settings.autoDimAfterMs)
            dimmed = true
        }
    }

    LaunchedEffect(settings.pixelShift) {
        if (!settings.pixelShift) {
            shiftStep = 0
            return@LaunchedEffect
        }
        while (isActive) {
            delay(LevyraAmbientSettings.PIXEL_SHIFT_INTERVAL_MS)
            shiftStep++
        }
    }

    val shift = remember(shiftStep, settings.pixelShift) {
        if (settings.pixelShift) ambientPixelShiftOffset(shiftStep) else 0f to 0f
    }
    val shiftX by animateDpAsState(shift.first.dp, tween(2_000), label = "ambient-shift-x")
    val shiftY by animateDpAsState(shift.second.dp, tween(2_000), label = "ambient-shift-y")
    val contentAlpha by animateFloatAsState(
        targetValue = if (dimmed) LevyraAmbientSettings.DIMMED_CONTENT_ALPHA else 1f,
        animationSpec = tween(1_200),
        label = "ambient-alpha"
    )
    val minimal = dimmed && !controlsVisible
    val rootInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = rootInteractionSource,
                indication = null
            ) { lastInteractionAt = SystemClock.elapsedRealtime() }
    ) {
        if (covered) return@Box

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .offset(x = shiftX, y = shiftY)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!state.hasTrack) {
                Text(
                    text = strings.ambientNothingPlaying,
                    color = AmbientMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                AmbientArtwork(state = state, minimal = minimal)
                Spacer(modifier = Modifier.height(if (minimal) 18.dp else 28.dp))
                Text(
                    text = state.title,
                    color = AmbientText,
                    fontSize = if (minimal) 18.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.artist.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.artist,
                        color = AmbientMuted,
                        fontSize = if (minimal) 13.sp else 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (settings.showLyrics && state.lyricLine.isNotBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = state.lyricLine,
                        color = AmbientLyric,
                        fontSize = if (minimal) 15.sp else 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AnimatedVisibility(
                    visible = !minimal,
                    enter = fadeIn(tween(400)),
                    exit = fadeOut(tween(600))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(24.dp))
                        AmbientProgress(positionMs = state.positionMs, durationMs = state.durationMs)
                    }
                }
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(220)),
                    exit = fadeOut(tween(600))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(28.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            AmbientIconButton(
                                icon = Icons.Rounded.SkipPrevious,
                                contentDescription = strings.previous,
                                size = 30.dp,
                                onClick = {
                                    lastInteractionAt = SystemClock.elapsedRealtime()
                                    onPrevious()
                                }
                            )
                            AmbientIconButton(
                                icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (state.isPlaying) strings.pause else strings.play,
                                size = 42.dp,
                                onClick = {
                                    lastInteractionAt = SystemClock.elapsedRealtime()
                                    onTogglePlay()
                                }
                            )
                            AmbientIconButton(
                                icon = Icons.Rounded.SkipNext,
                                contentDescription = strings.next,
                                size = 30.dp,
                                onClick = {
                                    lastInteractionAt = SystemClock.elapsedRealtime()
                                    onNext()
                                }
                            )
                        }
                    }
                }
            }
        }

        if (onExit != null) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(600)),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Box(modifier = Modifier.systemBarsPadding().padding(12.dp)) {
                    AmbientIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = strings.ambientExit,
                        size = 24.dp,
                        onClick = onExit
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientArtwork(state: AmbientUiState, minimal: Boolean) {
    val context = LocalContext.current
    val fraction = if (minimal) 0.42f else 0.62f
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
    ) {
        MotionArtworkLayer(
            artwork = state.motionArtwork.takeIf { state.settings.showCanvas },
            enabled = state.animationsEnabled && state.settings.showCanvas,
            isPlaying = state.isPlaying,
            cornerRadius = 18.dp,
            presentation = MotionArtworkPresentation.Card,
            quality = state.canvasQuality,
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(state.artworkUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(AmbientArtworkFallback))
            }
        }
    }
}

@Composable
private fun AmbientProgress(positionMs: Long, durationMs: Long) {
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Box(
        modifier = Modifier
            .fillMaxWidth(0.62f)
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(AmbientTrack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(2.dp)
                .background(AmbientProgressColor)
        )
    }
}

@Composable
private fun AmbientIconButton(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(size + 22.dp)
            .clip(RoundedCornerShape(50))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = AmbientText,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun rememberProximityCovered(enabled: Boolean): Boolean {
    val context = LocalContext.current
    var covered by remember(enabled) { mutableStateOf(false) }
    DisposableEffect(enabled, context) {
        if (!enabled) {
            covered = false
            return@DisposableEffect onDispose { }
        }
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        if (manager == null || sensor == null) {
            covered = false
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val value = event.values.firstOrNull() ?: return
                covered = value < sensor.maximumRange && value < PROXIMITY_NEAR_CM
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose {
            manager.unregisterListener(listener)
            covered = false
        }
    }
    return covered
}

private const val PROXIMITY_NEAR_CM = 5f
private val AmbientText = Color(0xFFE8E8EC)
private val AmbientMuted = Color(0xFF8A8A93)
private val AmbientLyric = Color(0xFFB9B9C4)
private val AmbientTrack = Color(0xFF1E1E22)
private val AmbientProgressColor = Color(0xFF6C6C78)
private val AmbientArtworkFallback = Color(0xFF101014)
