package com.luc4n3x.levyra.ui.ambient

import android.app.ActivityManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.SystemClock
import android.text.format.DateFormat
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.domain.LevyraAmbientMode
import com.luc4n3x.levyra.domain.LevyraAmbientSettings
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.domain.ambientPixelShiftOffset
import com.luc4n3x.levyra.ui.MotionArtworkLayer
import com.luc4n3x.levyra.ui.MotionArtworkPresentation
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import java.util.Calendar
import java.util.Locale
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
    val lowResources = rememberAmbientLowResources()
    val motionAllowed = state.animationsEnabled && !lowResources
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
    val shiftX by animateDpAsState(shift.first.dp, tween(SHIFT_DURATION_MS), label = "ambient-shift-x")
    val shiftY by animateDpAsState(shift.second.dp, tween(SHIFT_DURATION_MS), label = "ambient-shift-y")
    val contentAlpha by animateFloatAsState(
        targetValue = if (dimmed) LevyraAmbientSettings.DIMMED_CONTENT_ALPHA else 1f,
        animationSpec = tween(if (motionAllowed) 1_200 else 0),
        label = "ambient-alpha"
    )
    val resting = dimmed && !controlsVisible
    val palette = rememberAmbientPalette(state.accentStart, state.accentEnd, settings.amoledBlack)
    val rootInteractionSource = remember { MutableInteractionSource() }
    val onInteract = { lastInteractionAt = SystemClock.elapsedRealtime() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable(
                interactionSource = rootInteractionSource,
                indication = null,
                onClick = onInteract
            )
    ) {
        if (covered) return@Box

        if (settings.mode == LevyraAmbientMode.Spotlight && !resting && state.hasTrack) {
            AmbientSpotlightGlow(color = palette.glow, alpha = contentAlpha)
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val landscape = maxWidth > maxHeight
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(
                        horizontal = if (landscape) 48.dp else 32.dp,
                        vertical = 24.dp
                    )
                    .offset { IntOffset(shiftX.roundToPx(), shiftY.roundToPx()) }
                    .alpha(contentAlpha),
                contentAlignment = Alignment.Center
            ) {
                if (!state.hasTrack) {
                    Text(
                        text = strings.ambientNothingPlaying,
                        color = palette.muted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else if (landscape && settings.mode != LevyraAmbientMode.Minimal) {
                    AmbientLandscapeContent(
                        state = state,
                        palette = palette,
                        resting = resting,
                        controlsVisible = controlsVisible,
                        motionAllowed = motionAllowed,
                        onInteract = onInteract,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onPrevious = onPrevious
                    )
                } else {
                    AmbientPortraitContent(
                        state = state,
                        palette = palette,
                        resting = resting,
                        controlsVisible = controlsVisible,
                        motionAllowed = motionAllowed,
                        onInteract = onInteract,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onPrevious = onPrevious
                    )
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
                        tint = palette.muted,
                        onClick = onExit
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientPortraitContent(
    state: AmbientUiState,
    palette: AmbientPalette,
    resting: Boolean,
    controlsVisible: Boolean,
    motionAllowed: Boolean,
    onInteract: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val settings = state.settings
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (settings.showClock) {
            AmbientClock(color = palette.text, compact = settings.mode != LevyraAmbientMode.Minimal)
            Spacer(modifier = Modifier.height(if (settings.mode == LevyraAmbientMode.Minimal) 20.dp else 32.dp))
        }

        if (settings.usesArtwork) {
            AmbientArtwork(
                artworkUrl = state.artworkUrl,
                motionArtwork = state.motionArtwork,
                isPlaying = state.isPlaying,
                showCanvas = settings.showCanvas,
                motionAllowed = motionAllowed,
                quality = state.canvasQuality,
                palette = palette,
                widthFraction = if (resting) 0.46f else 0.66f,
                elevated = settings.mode == LevyraAmbientMode.Spotlight
            )
            Spacer(modifier = Modifier.height(if (resting) 20.dp else 30.dp))
        }

        if (settings.mode == LevyraAmbientMode.Lyrics) {
            AmbientLyricHero(
                line = state.lyricLine,
                fallback = state.title,
                palette = palette,
                resting = resting
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (settings.showTitle) {
            AmbientTrackTitle(
                title = state.title,
                artist = state.artist,
                palette = palette,
                resting = resting,
                compact = settings.mode == LevyraAmbientMode.Lyrics
            )
        }

        if (settings.usesLyricLine &&
            settings.mode != LevyraAmbientMode.Lyrics &&
            state.lyricLine.isNotBlank()
        ) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = state.lyricLine,
                color = palette.lyric,
                fontSize = if (resting) 15.sp else 17.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(if (resting) 15f else 17f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (settings.showProgress) {
            Spacer(modifier = Modifier.height(24.dp))
            AmbientProgress(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                palette = palette,
                widthFraction = 0.62f
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(600))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(28.dp))
                AmbientControls(
                    isPlaying = state.isPlaying,
                    palette = palette,
                    onInteract = onInteract,
                    onTogglePlay = onTogglePlay,
                    onNext = onNext,
                    onPrevious = onPrevious
                )
            }
        }
    }
}

@Composable
private fun AmbientLandscapeContent(
    state: AmbientUiState,
    palette: AmbientPalette,
    resting: Boolean,
    controlsVisible: Boolean,
    motionAllowed: Boolean,
    onInteract: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val settings = state.settings
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        if (settings.usesArtwork) {
            Box(modifier = Modifier.fillMaxHeight(if (resting) 0.58f else 0.78f)) {
                AmbientArtwork(
                    artworkUrl = state.artworkUrl,
                    motionArtwork = state.motionArtwork,
                    isPlaying = state.isPlaying,
                    showCanvas = settings.showCanvas,
                    motionAllowed = motionAllowed,
                    quality = state.canvasQuality,
                    palette = palette,
                    widthFraction = 1f,
                    elevated = settings.mode == LevyraAmbientMode.Spotlight,
                    fillHeight = true
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = LANDSCAPE_CONTENT_MAX_WIDTH),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            if (settings.showClock) {
                AmbientClock(color = palette.text, compact = true, centered = false)
                Spacer(modifier = Modifier.height(20.dp))
            }
            if (settings.mode == LevyraAmbientMode.Lyrics) {
                AmbientLyricHero(
                    line = state.lyricLine,
                    fallback = state.title,
                    palette = palette,
                    resting = resting,
                    centered = false
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (settings.showTitle) {
                AmbientTrackTitle(
                    title = state.title,
                    artist = state.artist,
                    palette = palette,
                    resting = resting,
                    compact = settings.mode == LevyraAmbientMode.Lyrics,
                    centered = false
                )
            }
            if (settings.usesLyricLine &&
                settings.mode != LevyraAmbientMode.Lyrics &&
                state.lyricLine.isNotBlank()
            ) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = state.lyricLine,
                    color = palette.lyric,
                    fontSize = 16.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(16f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (settings.showProgress) {
                Spacer(modifier = Modifier.height(20.dp))
                AmbientProgress(
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    palette = palette,
                    widthFraction = 1f
                )
            }
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(600))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    AmbientControls(
                        isPlaying = state.isPlaying,
                        palette = palette,
                        onInteract = onInteract,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onPrevious = onPrevious
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientTrackTitle(
    title: String,
    artist: String,
    palette: AmbientPalette,
    resting: Boolean,
    compact: Boolean,
    centered: Boolean = true
) {
    val titleSize = when {
        compact -> if (resting) 15f else 17f
        resting -> 18f
        else -> 23f
    }
    Column(
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            color = palette.text,
            fontSize = titleSize.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(titleSize),
            fontWeight = FontWeight.Bold,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (artist.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = artist,
                color = palette.muted,
                fontSize = if (resting) 13.sp else 15.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(if (resting) 13f else 15f),
                fontWeight = FontWeight.Medium,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AmbientLyricHero(
    line: String,
    fallback: String,
    palette: AmbientPalette,
    resting: Boolean,
    centered: Boolean = true
) {
    val text = line.ifBlank { fallback }
    if (text.isBlank()) return
    val size = if (resting) 22f else 28f
    Text(
        text = text,
        color = if (line.isBlank()) palette.muted else palette.text,
        fontSize = size.sp,
        lineHeight = LevyraTypeRhythm.lineHeight(size),
        fontWeight = FontWeight.Bold,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AmbientClock(color: Color, compact: Boolean, centered: Boolean = true) {
    val context = LocalContext.current
    val use24Hours = remember(context) { DateFormat.is24HourFormat(context) }
    var minuteToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(millisUntilNextMinute())
            minuteToken++
        }
    }
    val time = remember(minuteToken, use24Hours) { ambientClockLabel(use24Hours) }
    val size = if (compact) 34f else 64f
    Text(
        text = time,
        color = color,
        fontSize = size.sp,
        lineHeight = LevyraTypeRhythm.lineHeight(size),
        fontWeight = FontWeight.Light,
        letterSpacing = 1.sp,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        modifier = if (centered) Modifier.fillMaxWidth() else Modifier
    )
}

@Composable
private fun AmbientArtwork(
    artworkUrl: String,
    motionArtwork: MotionArtwork?,
    isPlaying: Boolean,
    showCanvas: Boolean,
    motionAllowed: Boolean,
    quality: LevyraCanvasQuality,
    palette: AmbientPalette,
    widthFraction: Float,
    elevated: Boolean,
    fillHeight: Boolean = false
) {
    val context = LocalContext.current
    val base = if (fillHeight) {
        Modifier.fillMaxHeight().aspectRatio(1f)
    } else {
        Modifier.fillMaxWidth(widthFraction).aspectRatio(1f)
    }
    Box(modifier = base.clip(RoundedCornerShape(if (elevated) 24.dp else 18.dp))) {
        MotionArtworkLayer(
            artwork = motionArtwork.takeIf { showCanvas && motionAllowed },
            enabled = motionAllowed && showCanvas,
            isPlaying = isPlaying,
            cornerRadius = if (elevated) 24.dp else 18.dp,
            presentation = MotionArtworkPresentation.Card,
            quality = quality,
            modifier = Modifier.fillMaxSize()
        ) {
            if (artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(artworkUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(palette.artworkFallback))
            }
        }
    }
}

@Composable
private fun AmbientSpotlightGlow(color: Color, alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to color.copy(alpha = GLOW_CORE_ALPHA),
                        0.5f to color.copy(alpha = GLOW_EDGE_ALPHA),
                        1f to Color.Transparent
                    )
                )
            )
            .clearAndSetSemantics { }
    )
}

@Composable
private fun AmbientProgress(
    positionMs: Long,
    durationMs: Long,
    palette: AmbientPalette,
    widthFraction: Float
) {
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(palette.track)
            .clearAndSetSemantics { }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(2.dp)
                .background(palette.accent)
        )
    }
}

@Composable
private fun AmbientControls(
    isPlaying: Boolean,
    palette: AmbientPalette,
    onInteract: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        AmbientIconButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = strings.previous,
            size = 30.dp,
            tint = palette.text,
            onClick = {
                onInteract()
                onPrevious()
            }
        )
        AmbientIconButton(
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) strings.pause else strings.play,
            size = 42.dp,
            tint = palette.text,
            onClick = {
                onInteract()
                onTogglePlay()
            }
        )
        AmbientIconButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = strings.next,
            size = 30.dp,
            tint = palette.text,
            onClick = {
                onInteract()
                onNext()
            }
        )
    }
}

@Composable
private fun AmbientIconButton(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    tint: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(maxOf(size + 22.dp, MINIMUM_TOUCH_TARGET))
            .clip(RoundedCornerShape(50))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
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

@Composable
private fun rememberAmbientLowResources(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var token by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) token++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return remember(context, token) {
        val power = context.getSystemService(PowerManager::class.java)
        val activity = context.getSystemService(ActivityManager::class.java)
        power?.isPowerSaveMode == true || activity?.isLowRamDevice == true
    }
}

@Composable
private fun rememberAmbientPalette(
    accentStart: Int,
    accentEnd: Int,
    amoledBlack: Boolean
): AmbientPalette = remember(accentStart, accentEnd, amoledBlack) {
    ambientPaletteFor(accentStart, accentEnd, amoledBlack)
}

internal data class AmbientPalette(
    val background: Color,
    val text: Color,
    val muted: Color,
    val lyric: Color,
    val track: Color,
    val accent: Color,
    val glow: Color,
    val artworkFallback: Color
)

internal fun ambientPaletteFor(
    accentStart: Int,
    accentEnd: Int,
    amoledBlack: Boolean
): AmbientPalette {
    val hasAccent = accentStart != 0 && accentEnd != 0
    val accent = if (hasAccent) {
        lerp(Color(accentStart), Color(accentEnd), 0.5f)
    } else {
        AmbientNeutralAccent
    }
    val readableAccent = lerp(accent, Color.White, 0.25f)
    val background = if (amoledBlack || !hasAccent) {
        Color.Black
    } else {
        lerp(Color.Black, accent, 0.06f)
    }
    return AmbientPalette(
        background = background,
        text = AmbientText,
        muted = AmbientMuted,
        lyric = AmbientLyric,
        track = AmbientTrack,
        accent = if (hasAccent) readableAccent else AmbientNeutralAccent,
        glow = readableAccent.copy(alpha = 1f),
        artworkFallback = if (amoledBlack) Color(0xFF0A0A0C) else Color(0xFF101014)
    )
}

private fun millisUntilNextMinute(): Long {
    val calendar = Calendar.getInstance()
    val elapsed = calendar.get(Calendar.SECOND) * 1_000L + calendar.get(Calendar.MILLISECOND)
    return (60_000L - elapsed).coerceIn(1_000L, 60_000L)
}

private fun ambientClockLabel(use24Hours: Boolean): String {
    val calendar = Calendar.getInstance()
    val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    return if (use24Hours) {
        String.format(Locale.getDefault(), "%02d:%02d", hour24, minute)
    } else {
        val hour12 = when (val raw = hour24 % 12) {
            0 -> 12
            else -> raw
        }
        String.format(Locale.getDefault(), "%d:%02d", hour12, minute)
    }
}

private val LANDSCAPE_CONTENT_MAX_WIDTH = 520.dp
private const val PROXIMITY_NEAR_CM = 5f
private const val SHIFT_DURATION_MS = 2_000
private const val GLOW_CORE_ALPHA = 0.30f
private const val GLOW_EDGE_ALPHA = 0.10f
private val MINIMUM_TOUCH_TARGET = 48.dp
private val AmbientText = Color(0xFFE8E8EC)
private val AmbientMuted = Color(0xFF8A8A93)
private val AmbientLyric = Color(0xFFB9B9C4)
private val AmbientTrack = Color(0xFF1E1E22)
private val AmbientNeutralAccent = Color(0xFF6C6C78)
