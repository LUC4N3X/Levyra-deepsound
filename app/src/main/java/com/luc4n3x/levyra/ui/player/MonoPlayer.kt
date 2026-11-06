package com.luc4n3x.levyra.ui.player

import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.MonoOverline
import com.luc4n3x.levyra.ui.StableRemoteArtwork
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.i18n.LevyraMonoPlayerStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.MonoPlayerCopy
import com.luc4n3x.levyra.ui.i18n.systemPlayerCopy
import com.luc4n3x.levyra.ui.i18n.trackCreditsCopy
import com.luc4n3x.levyra.ui.lyrics.activeLyricIndex
import com.luc4n3x.levyra.ui.theme.MonoDesign
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal class MonoPlayerActions(
    val sleepActive: Boolean,
    val onOutput: () -> Unit,
    val onSleepTimer: () -> Unit,
    val onShare: () -> Unit,
    val onQueue: () -> Unit,
    val onLyrics: () -> Unit,
    val similar: (@Composable (Track) -> Unit)?
)

private val MonoHeroCorner = 10.dp
private val MonoHeroShape = RoundedCornerShape(MonoHeroCorner)
private val MonoSheetShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
private const val MonoPausedScale = com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign.ArtworkPausedScale
private const val MonoPreviewWindow = 5

@Composable
internal fun MonoPlayerScene(
    track: Track, slots: PlayerDeckSlots, artworkCap: Dp, playing: Boolean,
    metadata: @Composable () -> Unit, transport: @Composable () -> Unit, actions: MonoPlayerActions,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoPlayerStrings.forCode(strings.code) }
    var showSimilar by rememberSaveable(track.id) { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        MonoPlayerBackdrop(track, playing)
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            val gutter = MonoDesign.gutter
            val artSize = minOf(maxWidth - gutter * 2, maxHeight * 0.42f, artworkCap)
            val viewport = maxHeight
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(Modifier.fillMaxWidth().heightIn(min = viewport), verticalArrangement = Arrangement.SpaceBetween) {
                    Box(Modifier.padding(horizontal = 4.dp)) { slots.header() }
                    Box(Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 8.dp), contentAlignment = Alignment.Center) {
                        MonoHero(track, slots, artSize, playing)
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        metadata()
                        slots.progress(PlayerProgressStyle.Classic)
                    }
                    Box(Modifier.padding(horizontal = 12.dp)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { transport() }
                    }
                    MonoSecondaryRow(actions, Modifier.padding(horizontal = gutter))
                    MonoTabBar(copy, actions, onSimilar = { showSimilar = true }, Modifier.padding(top = 12.dp))
                }
                Column(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    slots.lyricsPreview?.takeIf { it.trackId == track.id }?.let { MonoLyricsPreviewCard(it, copy) }
                    slots.artistPanel?.takeIf { it.trackId == track.id }?.let { panel ->
                        MonoAboutArtistCard(panel, copy)
                        MonoCreditsCard(panel, copy)
                    }
                }
            }
        }
    }
    val similar = actions.similar
    if (showSimilar && similar != null) MonoSimilarSheet(track, copy, similar) { showSimilar = false }
}

@Composable
private fun MonoHero(track: Track, slots: PlayerDeckSlots, size: Dp, playing: Boolean) {
    val animated = LocalAnimationsEnabled.current
    val lift by animateFloatAsState(
        if (playing) 1f else 0f,
        if (animated) spring(dampingRatio = 0.78f, stiffness = 220f) else snap(),
        label = "mono-hero-lift"
    )
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val scale = MonoPausedScale + (1f - MonoPausedScale) * lift
                scaleX = scale
                scaleY = scale
                translationY = (1f - lift) * 4.dp.toPx()
                shadowElevation = (6f + 26f * lift).dp.toPx()
                shape = MonoHeroShape
                ambientShadowColor = Color.Black
                spotShadowColor = Color.Black
            }
        )
        Box(
            Modifier.fillMaxSize().drawBehind {
                val halo = 0.10f + 0.16f * lift
                drawOval(
                    Brush.radialGradient(
                        listOf(MonoDesign.accent.copy(alpha = halo), Color.Transparent),
                        center = Offset(this.size.width / 2f, this.size.height * 1.02f), radius = this.size.width * 0.62f
                    ),
                    topLeft = Offset(-this.size.width * 0.12f, this.size.height * 0.72f),
                    size = androidx.compose.ui.geometry.Size(this.size.width * 1.24f, this.size.height * 0.6f)
                )
            }
        )
        slots.stage(track, Modifier.fillMaxSize(), MonoHeroCorner)
    }
}

@Composable
internal fun rememberMonoClock(running: Boolean): FloatState {
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                clock.floatValue += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }
    return clock
}

@Composable
private fun MonoPlayerBackdrop(track: Track, playing: Boolean) {
    val animated = LocalAnimationsEnabled.current
    val mesh = rememberArtworkMesh(track)
    val clock = rememberMonoClock(playing && animated)
    Box(Modifier.fillMaxSize().background(MonoDesign.base).clipToBounds()) {
        Crossfade(mesh, animationSpec = if (animated) tween(900) else snap(), label = "mono-backdrop") { shown ->
            if (shown != null) Canvas(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        val t = clock.floatValue
                        val zoom = 1.45f + 0.1f * sin(t * 0.11f)
                        scaleX = zoom
                        scaleY = zoom
                        rotationZ = 7f * sin(t * 0.045f)
                        translationX = size.width * 0.07f * sin(t * 0.07f)
                        translationY = size.height * 0.05f * cos(t * 0.053f)
                        alpha = 0.62f
                    }
                    .blur(72.dp)
            ) {
                drawImage(
                    shown.image,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.Low
                )
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to MonoDesign.void.copy(alpha = 0.42f),
                    0.45f to MonoDesign.base.copy(alpha = 0.58f),
                    0.82f to MonoDesign.base.copy(alpha = 0.9f),
                    1f to MonoDesign.base
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.55f to Color.Transparent, 1f to MonoDesign.accent.copy(alpha = 0.07f))
            )
        )
    }
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
internal fun MonoSeekbar(positionMs: Long, durationMs: Long, onSeek: (Float) -> Unit, modifier: Modifier = Modifier, playing: Boolean = true) {
    var dragging by remember(durationMs) { mutableStateOf<Float?>(null) }
    val fraction = dragging ?: if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val seekLabel = LocalLevyraStrings.current.gestureActionSeek
    val animated = LocalAnimationsEnabled.current
    val clock = rememberMonoClock(playing && animated)
    val amplitude = animateFloatAsState(if (playing) 1f else 0f, if (animated) tween(520) else snap(), label = "mono-seek-amp")
    val grab = animateFloatAsState(if (dragging != null) 1f else 0f, if (animated) tween(160) else snap(), label = "mono-seek-grab")
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(with(density) { 2.dp.toPx() }, cap = StrokeCap.Round) }
    val wave = remember { Path() }
    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier.fillMaxWidth().height(48.dp)
                .semantics {
                    contentDescription = seekLabel
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    if (durationMs > 0L) setProgress { target -> onSeek(target.coerceIn(0f, 1f)); true }
                }
                .pointerInput(durationMs) {
                    if (durationMs <= 0L) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = (it.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = { dragging?.let(onSeek); dragging = null },
                        onDragCancel = { dragging = null }
                    ) { change, _ -> dragging = (change.position.x / size.width).coerceIn(0f, 1f) }
                }
                .pointerInput(durationMs) {
                    if (durationMs <= 0L) return@pointerInput
                    detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) }
                }
        ) {
            val middle = size.height / 2f
            val head = size.width * fraction
            val reach = 7.dp.toPx() * amplitude.value * (1f - 0.45f * grab.value)
            val wavelength = 24.dp.toPx()
            val edge = 14.dp.toPx()
            val step = 2.dp.toPx()
            val phase = clock.floatValue * 2f * PI.toFloat() * 1.3f
            wave.reset()
            wave.moveTo(0f, middle)
            var x = 0f
            while (x < head) {
                x = min(x + step, head)
                val taper = min(1f, min(x / edge, (head - x) / edge))
                wave.lineTo(x, middle + sin(x / wavelength * 2f * PI.toFloat() - phase) * reach * taper)
            }
            drawPath(wave, MonoDesign.text, style = stroke)
            if (head < size.width) drawLine(MonoDesign.hairlineStrong, Offset(head, middle), Offset(size.width, middle), 1.dp.toPx(), StrokeCap.Round)
            drawCircle(MonoDesign.text, (5f + 3f * grab.value).dp.toPx(), Offset(head, middle))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shown = dragging?.let { (durationMs * it).toLong() } ?: positionMs
            Text(formatSeekbarMillis(shown), style = MaterialTheme.typography.labelSmall, color = MonoDesign.textSecondary, maxLines = 1)
            Text(formatSeekbarMillis(durationMs), style = MaterialTheme.typography.labelSmall, color = MonoDesign.textSecondary, maxLines = 1)
        }
    }
}

@Composable
internal fun MonoTransport(
    playing: Boolean, resolving: Boolean, shuffle: Boolean, repeat: RepeatMode,
    labels: PlayerControlLabels, onShuffle: () -> Unit, onPrevious: () -> Unit,
    onToggle: () -> Unit, onNext: () -> Unit, onRepeat: () -> Unit, modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().heightIn(min = 92.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        MonoModeToggle(Icons.Rounded.Shuffle, labels.shuffle, shuffle, onShuffle)
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, labels.previous, tint = MonoDesign.text, modifier = Modifier.size(34.dp))
        }
        MonoPlayDisc(playing, resolving, if (playing) labels.pause else labels.play, onToggle)
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipNext, labels.next, tint = MonoDesign.text, modifier = Modifier.size(34.dp))
        }
        MonoModeToggle(
            if (repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, labels.repeat, repeat != RepeatMode.Off, onRepeat,
            stateLabel = when (repeat) {
                RepeatMode.All -> androidx.compose.ui.res.stringResource(com.luc4n3x.levyra.R.string.notification_repeat_all)
                RepeatMode.One -> androidx.compose.ui.res.stringResource(com.luc4n3x.levyra.R.string.notification_repeat_one)
                else -> null
            }
        )
    }
}

@Composable
private fun MonoPlayDisc(playing: Boolean, resolving: Boolean, label: String, onToggle: () -> Unit) {
    val animated = LocalAnimationsEnabled.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.92f else 1f, if (animated) spring(dampingRatio = 0.55f, stiffness = 700f) else snap(), label = "mono-play-press")
    val morph = animateFloatAsState(if (playing) 1f else 0f, if (animated) spring(dampingRatio = 0.8f, stiffness = 420f) else snap(), label = "mono-play-morph")
    val left = remember { Path() }
    val right = remember { Path() }
    Box(
        Modifier.size(MonoDesign.playerPlay)
            .graphicsLayer { scaleX = press; scaleY = press }
            .clip(CircleShape)
            .background(MonoDesign.accent)
            .clickable(interaction, indication = null, enabled = !resolving, role = Role.Button, onClick = onToggle)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (resolving) {
            LevyraLoadingIndicator(Modifier.size(30.dp), MonoDesign.onAccent)
        } else {
            Canvas(Modifier.size(26.dp)) {
                val m = morph.value
                val w = size.width
                val h = size.height
                val bar = w * 0.27f
                val leftEnd = mix(w * 0.56f, w * 0.12f + bar, m)
                val rightStart = mix(w * 0.56f, w * 0.88f - bar, m)
                left.reset()
                left.moveTo(mix(w * 0.18f, w * 0.12f, m), h * 0.06f)
                left.lineTo(leftEnd, mix(h * 0.28f, h * 0.06f, m))
                left.lineTo(leftEnd, mix(h * 0.72f, h * 0.94f, m))
                left.lineTo(mix(w * 0.18f, w * 0.12f, m), h * 0.94f)
                left.close()
                right.reset()
                right.moveTo(rightStart, mix(h * 0.28f, h * 0.06f, m))
                right.lineTo(mix(w * 0.94f, w * 0.88f, m), mix(h * 0.5f, h * 0.06f, m))
                right.lineTo(mix(w * 0.94f, w * 0.88f, m), mix(h * 0.5f, h * 0.94f, m))
                right.lineTo(rightStart, mix(h * 0.72f, h * 0.94f, m))
                right.close()
                drawPath(left, MonoDesign.onAccent)
                drawPath(right, MonoDesign.onAccent)
            }
        }
    }
}

private fun mix(from: Float, to: Float, t: Float): Float = from + (to - from) * t

@Composable
private fun MonoModeToggle(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit, stateLabel: String? = null) {
    Column(
        Modifier.size(48.dp).clip(CircleShape).toggleable(value = active, role = Role.Switch, onValueChange = { onClick() })
            .semantics {
                contentDescription = label
                if (stateLabel != null) stateDescription = stateLabel
            },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (active) MonoDesign.accent else MonoDesign.textSecondary, modifier = Modifier.size(22.dp))
        Box(Modifier.padding(top = 4.dp).size(4.dp).clip(CircleShape).background(if (active) MonoDesign.accent else Color.Transparent))
    }
}

@Composable
private fun MonoSecondaryRow(actions: MonoPlayerActions, modifier: Modifier = Modifier) {
    val strings = LocalLevyraStrings.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        MonoQuietKey(Icons.Rounded.Speaker, strings.systemPlayerCopy().outputTitle, false, actions.onOutput)
        MonoQuietKey(Icons.Rounded.Bedtime, strings.sleepTimer, actions.sleepActive, actions.onSleepTimer)
        MonoQuietKey(Icons.Rounded.Share, strings.share, false, actions.onShare)
        MonoQuietKey(Icons.AutoMirrored.Rounded.QueueMusic, strings.queue, false, actions.onQueue)
    }
}

@Composable
private fun MonoQuietKey(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics {
                contentDescription = label
                if (active) toggleableState = ToggleableState.On
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (active) MonoDesign.accent else MonoDesign.textSecondary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MonoTabBar(copy: MonoPlayerCopy, actions: MonoPlayerActions, onSimilar: () -> Unit, modifier: Modifier = Modifier) {
    val pull = with(LocalDensity.current) { 36.dp.toPx() }
    val openQueue by rememberUpdatedState(actions.onQueue)
    Column(
        modifier.fillMaxWidth().clip(MonoSheetShape).background(MonoDesign.surface.copy(alpha = 0.92f))
            .border(1.dp, MonoDesign.hairline, MonoSheetShape)
            .pointerInput(pull) {
                var travel = 0f
                detectVerticalDragGestures(
                    onDragStart = { travel = 0f },
                    onDragEnd = { if (travel < -pull) openQueue() }
                ) { change, amount ->
                    change.consume()
                    travel += amount
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.padding(top = 8.dp).size(width = 32.dp, height = 3.dp).clip(CircleShape).background(MonoDesign.hairlineStrong))
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            MonoTab(copy.upNext, actions.onQueue, Modifier.weight(1f))
            MonoTab(copy.lyricsTab, actions.onLyrics, Modifier.weight(1f))
            if (actions.similar != null) MonoTab(copy.similar, onSimilar, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MonoTab(label: String, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier.heightIn(min = 52.dp).clip(MonoDesign.pillShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonoSimilarSheet(track: Track, copy: MonoPlayerCopy, content: @Composable (Track) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MonoDesign.surface,
        contentColor = MonoDesign.text,
        shape = MonoSheetShape,
        dragHandle = { Box(Modifier.padding(vertical = 12.dp).size(width = 32.dp, height = 3.dp).clip(CircleShape).background(MonoDesign.hairlineStrong)) }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Column(Modifier.padding(horizontal = MonoDesign.gutter), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MonoOverline(copy.similar)
                Text(
                    track.title, style = MaterialTheme.typography.titleLarge, color = MonoDesign.text, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                )
            }
            Spacer(Modifier.height(12.dp))
            content(track)
        }
    }
}

@Composable
private fun MonoCardTitle(overline: String, title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MonoOverline(overline)
            Text(title, style = MaterialTheme.typography.titleLarge, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
        }
        trailing?.invoke()
    }
}

@Composable
private fun MonoCardAction(label: String, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp).clip(MonoDesign.pillShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick).padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("$label ›", style = MaterialTheme.typography.labelLarge, color = MonoDesign.textSecondary, maxLines = 1)
    }
}

@Composable
private fun MonoLyricsPreviewCard(preview: PlayerLyricsPreview, copy: MonoPlayerCopy) {
    val strings = LocalLevyraStrings.current
    val animated = LocalAnimationsEnabled.current
    val active = remember(preview.lines, preview.synced, preview.positionMs / 100) {
        if (preview.synced) activeLyricIndex(preview.positionMs, preview.lines) else -1
    }
    val start = (active - 1).coerceAtLeast(0)
    val window = remember(preview.lines, start) {
        preview.lines.withIndex().drop(start).filter { it.value.text.isNotBlank() }.take(MonoPreviewWindow)
    }
    Column(
        Modifier.fillMaxWidth().monoCard()
            .clickable(role = Role.Button, onClickLabel = copy.showLyrics, onClick = preview.onOpen)
            .padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 8.dp)
    ) {
        MonoCardTitle(if (preview.synced) copy.syncedLyrics else strings.lyrics, copy.lyricsPreview)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            window.forEach { (index, line) -> key(index) {
                val lit = !preview.synced || index == active
                val tone by androidx.compose.animation.animateColorAsState(
                    if (lit) MonoDesign.text else MonoDesign.textFaint,
                    if (animated) tween(260) else snap(),
                    label = "mono-preview-line"
                )
                Text(line.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = tone, maxLines = 2, overflow = TextOverflow.Ellipsis)
            } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { MonoCardAction(copy.showLyrics, preview.onOpen) }
    }
}

@Composable
private fun MonoAboutArtistCard(panel: PlayerArtistPanel, copy: MonoPlayerCopy) {
    val strings = LocalLevyraStrings.current
    val profile = panel.profile
    val name = profile?.name?.ifBlank { null } ?: panel.artistName
    val image = profile?.thumbnailUrl.orEmpty()
    val listeners = profile?.let { it.monthlyListeners.ifBlank { it.subscribers } }.orEmpty()
    val bio = profile?.biography?.text?.trim().orEmpty()
    Column(
        Modifier.fillMaxWidth().monoCard()
            .clickable(role = Role.Button, onClickLabel = strings.openArtist, onClick = panel.onOpenArtist)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MonoOverline(copy.aboutArtist)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(MonoDesign.raised).border(1.dp, MonoDesign.hairline, CircleShape)) {
                if (image.isNotBlank()) StableRemoteArtwork(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
                if (listeners.isNotBlank()) Text(listeners, style = MaterialTheme.typography.labelSmall, color = MonoDesign.textSecondary, maxLines = 1)
            }
        }
        if (bio.isNotBlank()) {
            Text(bio, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MonoCreditsCard(panel: PlayerArtistPanel, copy: MonoPlayerCopy) {
    val strings = LocalLevyraStrings.current
    val name = panel.profile?.name?.ifBlank { null } ?: panel.artistName
    val writerRole = strings.trackCreditsCopy().composition
    Column(Modifier.fillMaxWidth().monoCard().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)) {
        MonoCardTitle(copy.credits, panel.trackTitle) { MonoCardAction(strings.showAll, panel.onCredits) }
        Spacer(Modifier.height(4.dp))
        MonoCreditRow(name, copy.mainArtist)
        panel.writers.take(4).forEach { MonoCreditRow(it, writerRole) }
    }
}

@Composable
private fun MonoCreditRow(name: String, role: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(name, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(role.uppercase(), style = MaterialTheme.typography.labelSmall, color = MonoDesign.textFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun Modifier.monoCard(): Modifier =
    clip(MonoDesign.panelShape).background(MonoDesign.surface.copy(alpha = 0.86f)).border(1.dp, MonoDesign.hairline, MonoDesign.panelShape)

@Composable
internal fun MonoPlayerFavoriteButton(isFavorite: Boolean, label: String, scale: Float, onToggle: () -> Unit) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { toggleableState = ToggleableState(isFavorite) }
    ) {
        Icon(
            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, label,
            tint = if (isFavorite) MonoDesign.accent else MonoDesign.text, modifier = Modifier.size(26.dp)
        )
    }
}
