package com.luc4n3x.levyra.ui.player

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.cast.CastRouteButton
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.ui.MotionArtworkLayer
import com.luc4n3x.levyra.ui.MotionArtworkPresentation
import com.luc4n3x.levyra.ui.artwork.LivingArtworkColors
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.components.levyraMarquee
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities

private val AuraInk = Color.White
private val AuraInkSoft = Color.White.copy(alpha = 0.64f)
private val AuraInkFaint = Color.White.copy(alpha = 0.24f)
private val AuraBadge = Color.White.copy(alpha = 0.14f)
private val AuraGutter = 22.dp
private val AuraBarIdle = 4.dp
private val AuraBarActive = 9.dp
private val AuraScrim = Brush.verticalGradient(
    0.00f to Color.Black.copy(alpha = 0.32f),
    0.12f to Color.Transparent,
    0.46f to Color.Transparent,
    0.66f to Color.Black.copy(alpha = 0.62f),
    0.80f to Color.Black.copy(alpha = 0.88f),
    1.00f to Color.Black
)

@Composable
internal fun PlayerAuraDeck(
    track: Track,
    slots: PlayerDeckSlots,
    artworkUrl: String,
    motionArtwork: MotionArtwork?,
    livingArtwork: LivingArtworkColors?,
    motionEnabled: Boolean,
    canvasQuality: LevyraCanvasQuality,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    positionMs: Long,
    durationMs: Long,
    labels: PlayerControlLabels,
    animated: Boolean,
    onCollapse: () -> Unit,
    onMore: () -> Unit,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onSeek: (Float) -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color.Black)) {
        MotionArtworkLayer(
            artwork = motionArtwork,
            enabled = motionEnabled,
            isPlaying = isPlaying,
            cornerRadius = 0.dp,
            presentation = MotionArtworkPresentation.Immersive,
            quality = canvasQuality,
            livingArtwork = livingArtwork,
            modifier = Modifier.fillMaxSize()
        ) {
            AuraStaticArtwork(artworkUrl = artworkUrl)
        }
        Box(modifier = Modifier.fillMaxSize().background(AuraScrim))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = AuraGutter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuraGrabber(onCollapse = onCollapse)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                slots.gestures(track, Modifier.matchParentSize())
            }
            AuraHeadline(
                track = track,
                isFavorite = isFavorite,
                isDownloaded = isDownloaded,
                animated = animated,
                onArtistClick = onArtistClick,
                onToggleFavorite = onToggleFavorite,
                onDownload = onDownload,
                onMore = onMore,
                modifier = headlineModifier
            )
            Spacer(Modifier.height(14.dp))
            AuraLine(
                fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                enabled = durationMs > 0L,
                onCommit = onSeek
            )
            AuraTimes(positionMs = positionMs, durationMs = durationMs)
            Spacer(Modifier.height(10.dp))
            AuraTransport(
                isPlaying = isPlaying,
                isResolving = isResolving,
                labels = labels,
                onPrevious = onPrevious,
                onTogglePlay = onTogglePlay,
                onNext = onNext
            )
            Spacer(Modifier.height(10.dp))
            AuraVolume()
            Spacer(Modifier.height(8.dp))
            AuraUtilityRow(actions = slots.dockActions(track))
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun AuraStaticArtwork(artworkUrl: String) {
    val canBlur = LocalLevyraVisualCapabilities.current.heavyBlur &&
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (canBlur) {
            SeamlessArtworkImage(
                url = artworkUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.72f }
                    .blur(80.dp, BlurredEdgeTreatment.Rectangle)
            ) {}
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))
        }
        SeamlessArtworkImage(
            url = artworkUrl,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(AuraStaticArtworkHeight)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(brush = AuraStaticFade, blendMode = BlendMode.DstIn)
                }
        ) {}
    }
}

private const val AuraStaticArtworkHeight = 0.76f
private val AuraStaticFade = Brush.verticalGradient(
    0.00f to Color.Black,
    0.78f to Color.Black,
    0.92f to Color.Black.copy(alpha = 0.45f),
    1.00f to Color.Transparent
)

@Composable
private fun AuraGrabber(onCollapse: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .levyraPressable(onClick = onCollapse, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { contentDescription = strings.collapsePlayer },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.46f))
        )
    }
}

@Composable
private fun AuraHeadline(
    track: Track,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    animated: Boolean,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = track,
            modifier = Modifier.weight(1f),
            transitionSpec = { LevyraMotion.contentSwap(animated) },
            contentKey = { it.id },
            label = "player-aura-headline"
        ) { shown ->
            Column {
                Text(
                    text = shown.title,
                    color = AuraInk,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .levyraMarquee(enabled = animated, repeatDelayMillis = 3_200)
                        .semantics { heading() }
                )
                PlayerArtistText(
                    track = shown,
                    color = AuraInkSoft,
                    style = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
                    onClickLabel = strings.openArtist,
                    onArtistClick = onArtistClick,
                    enabled = shown.id == track.id
                )
            }
        }
        AuraIconButton(
            icon = if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
            label = if (isDownloaded) strings.downloaded else strings.download,
            size = 24.dp,
            onClick = onDownload
        )
        AuraIconButton(
            icon = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            label = strings.favoritesPlain,
            size = 26.dp,
            onClick = onToggleFavorite
        )
        AuraIconButton(icon = Icons.Rounded.MoreVert, label = strings.more, size = 22.dp, onClick = onMore)
    }
}

@Composable
private fun AuraIconButton(icon: ImageVector, label: String, size: Dp, tint: Color = AuraInk, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
private fun AuraLine(fraction: Float, enabled: Boolean, onCommit: (Float) -> Unit, onLive: ((Float) -> Unit)? = null) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val latestCommit by rememberUpdatedState(onCommit)
    val latestLive by rememberUpdatedState(onLive)
    val shown = dragFraction ?: fraction
    val barHeight by animateDpAsState(
        targetValue = if (dragFraction != null) AuraBarActive else AuraBarIdle,
        label = "aura-line-height"
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
                setProgress { target ->
                    latestCommit(target.coerceIn(0f, 1f))
                    true
                }
            }
            .pointerInput(enabled) {
                detectTapGestures { offset ->
                    if (enabled) latestCommit((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(enabled) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (enabled) dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragFraction?.let(latestCommit)
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                    onHorizontalDrag = { change, _ ->
                        if (enabled) {
                            val next = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragFraction = next
                            latestLive?.invoke(next)
                        }
                    }
                )
            }
    ) {
        val height = barHeight.toPx()
        val top = (size.height - height) / 2f
        val radius = CornerRadius(height / 2f)
        drawRoundRect(color = AuraInkFaint, topLeft = Offset(0f, top), size = Size(size.width, height), cornerRadius = radius)
        drawRoundRect(
            color = AuraInk.copy(alpha = if (dragFraction != null) 1f else 0.86f),
            topLeft = Offset(0f, top),
            size = Size((size.width * shown).coerceAtLeast(height), height),
            cornerRadius = radius
        )
    }
}

@Composable
private fun AuraTimes(positionMs: Long, durationMs: Long) {
    val spec by rememberPlayerAudioSpec()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuraTime(formatSeekbarMillis(positionMs), Modifier.weight(1f))
        if (spec.codec.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AuraBadge)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = AuraInkSoft, modifier = Modifier.size(12.dp))
                Text(spec.codec, color = AuraInkSoft, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
            }
        }
        val remaining = if (durationMs > 0L) "-" + formatSeekbarMillis((durationMs - positionMs).coerceAtLeast(0L)) else "--:--"
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            AuraTime(remaining)
        }
    }
}

@Composable
private fun AuraTime(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = AuraInkSoft,
        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
        modifier = modifier
    )
}

@Composable
private fun AuraTransport(
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuraTransportButton(Icons.Rounded.FastRewind, labels.previous, 38.dp) {
            haptics.perform(LevyraHapticAction.Transport)
            onPrevious()
        }
        PlayerAuraPlayPauseButton(
            isPlaying = isPlaying,
            isResolving = isResolving,
            labels = labels,
            onTogglePlay = onTogglePlay
        )
        AuraTransportButton(Icons.Rounded.FastForward, labels.next, 38.dp) {
            haptics.perform(LevyraHapticAction.Transport)
            onNext()
        }
    }
}

@Composable
internal fun PlayerAuraPlayPauseButton(
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    onTogglePlay: () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    Box(
        modifier = Modifier
            .size(72.dp)
            .levyraPressable(
                onClick = {
                    haptics.perform(LevyraHapticAction.Transport)
                    onTogglePlay()
                },
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            )
            .semantics { contentDescription = if (isPlaying) labels.pause else labels.play },
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) {
            LevyraLoadingIndicator(color = AuraInk, modifier = Modifier.size(42.dp))
        } else {
            LevyraPlayPauseGlyph(
                playing = isPlaying,
                color = AuraInk,
                modifier = Modifier.size(42.dp),
                animateMorph = false
            )
        }
    }
}

@Composable
private fun AuraTransportButton(icon: ImageVector, label: String, glyph: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = AuraInk, modifier = Modifier.size(glyph))
    }
}

@Composable
private fun AuraVolume() {
    val context = LocalContext.current
    val audio = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val max = remember(audio) { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var level by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max) }
    DisposableEffect(audio) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                level = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    val apply: (Float) -> Unit = { fraction ->
        level = fraction
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (fraction * max).toInt().coerceIn(0, max), 0)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.AutoMirrored.Rounded.VolumeDown, contentDescription = null, tint = AuraInkSoft, modifier = Modifier.size(18.dp))
        Box(modifier = Modifier.weight(1f)) {
            AuraLine(fraction = level, enabled = true, onCommit = apply, onLive = apply)
        }
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = AuraInkSoft, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AuraUtilityRow(actions: List<PlayerDockAction>) {
    val byKey = actions.associateBy { it.key }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        byKey["lyrics"]?.let { action ->
            AuraIconButton(action.icon, action.label, 22.dp, if (action.active) AuraInk else AuraInkSoft, action.onClick)
        }
        CastRouteButton(modifier = Modifier.size(44.dp))
        byKey["visual-mode"]?.let { action ->
            AuraIconButton(action.icon, action.label, 22.dp, AuraInkSoft, action.onClick)
        }
        byKey["queue"]?.let { action ->
            AuraIconButton(action.icon, action.label, 22.dp, AuraInkSoft, action.onClick)
        }
    }
}
