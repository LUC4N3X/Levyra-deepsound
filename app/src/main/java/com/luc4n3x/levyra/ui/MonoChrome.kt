package com.luc4n3x.levyra.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.semantics.selected
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.DockMorph
import com.luc4n3x.levyra.ui.components.DockMorphFrame
import com.luc4n3x.levyra.ui.components.DockMorphRow
import com.luc4n3x.levyra.ui.components.DockMorphSlots
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LocalDockMorph
import com.luc4n3x.levyra.ui.components.dockMorphFrame
import com.luc4n3x.levyra.ui.components.dockMorphReveal
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.MonoDesign
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

internal object MonoChromeState {
    var playing by mutableStateOf(false)
}

private val MonoNavPadding = 6.dp
private val MonoNavBottom = 8.dp
private val MonoEdgeFast: AnimationSpec<Float> = spring(dampingRatio = 0.62f, stiffness = 620f)
private val MonoEdgeSlow: AnimationSpec<Float> = spring(dampingRatio = 0.74f, stiffness = 180f)

private val MonoStillClock = object : State<Float> {
    override val value: Float = 0f
}

@Composable
private fun rememberMonoWaveClock(active: Boolean): State<Float> {
    if (!active || !LocalAnimationsEnabled.current) return MonoStillClock
    val transition = rememberInfiniteTransition(label = "mono-chrome-wave")
    return transition.animateFloat(
        0f, 2f * PI.toFloat(), infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "mono-chrome-wave-phase"
    )
}

private fun DrawScope.drawMonoChromeWave(
    path: Path, color: Color, phase: Float, amplitude: Float, cycles: Float, strokeWidth: Dp, to: Float = 1f
) {
    if (to <= 0f || size.width <= 0f) return
    val stroke = strokeWidth.toPx()
    val mid = size.height / 2f
    val reach = (size.height / 2f - stroke).coerceAtLeast(0f) * amplitude
    val steps = (size.width * to / 3f).toInt().coerceIn(6, 200)
    path.reset()
    for (i in 0..steps) {
        val t = to * i / steps
        val envelope = 0.3f + 0.7f * abs(sin(t * PI.toFloat() * 2f + phase * 0.35f))
        val y = mid + sin(t * PI.toFloat() * 2f * cycles + phase) * reach * envelope
        if (i == 0) path.moveTo(0f, y) else path.lineTo(t * size.width, y)
    }
    drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
}

@Composable
internal fun MonoBottomTabs(
    entries: List<LevyraTabEntry>, selected: LevyraTab, applySystemInset: Boolean, modifier: Modifier,
    onSelect: (LevyraTabEntry, Boolean) -> Unit
) {
    val morph = LocalDockMorph.current
    val frame = remember { DockMorphFrame() }
    val allEntries = rememberLevyraTabEntries()
    val tabs = remember(entries, allEntries) {
        if (entries.any { it.tab == LevyraTab.Search }) entries
        else entries + listOfNotNull(allEntries.firstOrNull { it.tab == LevyraTab.Search })
    }
    val selectedIndex = tabs.indexOfFirst { it.tab == selected }
    val shape = MonoDesign.pillShape
    Column(
        modifier.fillMaxWidth().padding(start = MonoDesign.miniMargin, end = MonoDesign.miniMargin, bottom = MonoNavBottom)
    ) {
        Box(
            Modifier.fillMaxWidth()
                .dockMorphFrame(morph, frame, MonoDesign.navHeight)
                .height(MonoDesign.navHeight)
                .clip(shape)
                .glacierSlab()
                .border(1.dp, MonoDesign.hairline, shape)
                .dockMorphReveal(
                    morph = morph,
                    slots = DockMorphSlots(tabs.size, selectedIndex.coerceAtLeast(0), MonoNavPadding, 0.dp, 1f),
                    row = DockMorphRow(
                        leading = MonoDesign.miniMargin, trailing = MonoDesign.miniMargin,
                        height = MonoDesign.navHeight, bottom = MonoNavBottom, gap = 0.dp
                    ),
                    frame = frame
                )
        ) {
            MonoTabRow(tabs, selectedIndex, morph, onSelect)
        }
        if (applySystemInset) Spacer(Modifier.navigationBarsPadding())
    }
}

internal val GlacierSlabShape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)

internal fun Modifier.glacierSlab(): Modifier = drawWithCache {
    val body = Brush.verticalGradient(listOf(MonoDesign.raised, MonoDesign.surface))
    val sheen = Brush.horizontalGradient(
        0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.10f), 1f to Color.Transparent
    )
    onDrawBehind {
        drawRect(body)
        drawRect(sheen, size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
    }
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
private fun MonoTabRow(
    entries: List<LevyraTabEntry>, selectedIndex: Int, morph: DockMorph?,
    onSelect: (LevyraTabEntry, Boolean) -> Unit
) {
    val animated = LocalAnimationsEnabled.current
    val collapse: () -> Float = { morph?.fraction ?: 0f }
    val startEdge = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    val endEdge = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    val bladeShown by animateFloatAsState(if (selectedIndex >= 0) 1f else 0f, if (animated) tween(220) else snap(), label = "glacier-blade-shown")
    LaunchedEffect(selectedIndex, animated) {
        if (selectedIndex < 0) return@LaunchedEffect
        val target = selectedIndex.toFloat()
        if (!animated) {
            startEdge.snapTo(target)
            endEdge.snapTo(target)
            return@LaunchedEffect
        }
        val forward = target >= (startEdge.value + endEdge.value) / 2f
        launch { startEdge.animateTo(target, if (forward) MonoEdgeSlow else MonoEdgeFast) }
        launch { endEdge.animateTo(target, if (forward) MonoEdgeFast else MonoEdgeSlow) }
    }
    val revealSpec: AnimationSpec<Float> = if (animated) spring(dampingRatio = 0.82f, stiffness = 380f) else snap()
    val reveals = entries.mapIndexed { index, entry ->
        key(entry.tab) { animateFloatAsState(if (index == selectedIndex) 1f else 0f, revealSpec, label = "glacier-tab-reveal") }
    }
    val breath = rememberGlacierBreath(MonoChromeState.playing && animated)
    val padding = MonoNavPadding
    Box(
        Modifier.fillMaxSize().selectableGroup().drawBehind {
            if (entries.isEmpty()) return@drawBehind
            val pad = padding.toPx()
            val slot = (size.width - pad * 2f) / entries.size
            val left = pad + slot * startEdge.value + slot * 0.5f
            val right = pad + slot * endEdge.value + slot * 0.5f
            val shown = bladeShown * (1f - collapse()).coerceIn(0f, 1f)
            if (shown <= 0.01f) return@drawBehind
            val centre = (left + right) / 2f
            val stretch = right - left
            val pool = 26.dp.toPx() + stretch * 0.5f
            drawOval(
                Brush.radialGradient(
                    listOf(MonoDesign.accent.copy(alpha = (0.20f + 0.06f * breath.value) * shown), Color.Transparent),
                    center = Offset(centre, size.height / 2f), radius = pool
                ),
                topLeft = Offset(centre - pool, size.height / 2f - pool),
                size = androidx.compose.ui.geometry.Size(pool * 2f, pool * 2f)
            )
            val bladeHeight = 3.dp.toPx()
            val half = 8.dp.toPx()
            drawRoundRect(
                MonoDesign.accent.copy(alpha = shown),
                topLeft = Offset(left - half, size.height - 9.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(stretch + half * 2f, bladeHeight),
                cornerRadius = CornerRadius(bladeHeight)
            )
        }.padding(horizontal = padding)
    ) {
        Row(Modifier.fillMaxSize()) {
            entries.forEachIndexed { index, entry ->
                key(entry.tab) { MonoTab(entry, index == selectedIndex, reveals[index], collapse, onSelect, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun rememberGlacierBreath(active: Boolean): State<Float> {
    if (!active) return MonoStillClock
    val transition = rememberInfiniteTransition(label = "glacier-breath")
    return transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glacier-breath-value")
}

@Composable
private fun MonoTab(
    entry: LevyraTabEntry, active: Boolean, reveal: State<Float>, collapse: () -> Float,
    onSelect: (LevyraTabEntry, Boolean) -> Unit, modifier: Modifier
) {
    val painter = rememberVectorPainter(if (active) entry.selectedIcon else entry.unselectedIcon)
    Column(
        modifier.fillMaxSize()
            .selectable(active, role = Role.Tab, onClick = { onSelect(entry, active) })
            .clearAndSetSemantics { contentDescription = entry.label; selected = active },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(26.dp)
                .graphicsLayer {
                    val lift = reveal.value * (1f - collapse())
                    translationY = -2.dp.toPx() * lift
                    scaleX = 1f + 0.08f * lift
                    scaleY = 1f + 0.08f * lift
                }
                .drawBehind {
                    val tint = lerp(MonoDesign.textSecondary, MonoDesign.accent, reveal.value.coerceIn(0f, 1f))
                    with(painter) { draw(size, colorFilter = ColorFilter.tint(tint)) }
                }
        )
    }
}

@Composable
internal fun GlacierMiniBackdrop(artworkColor: Color, progress: () -> Float, showProgress: Boolean, playing: Boolean) {
    val breath = rememberGlacierBreath(playing && LocalAnimationsEnabled.current)
    Spacer(
        Modifier.fillMaxSize().drawWithCache {
            val body = Brush.verticalGradient(listOf(MonoDesign.raised, MonoDesign.surface))
            val tint = Brush.radialGradient(
                0f to artworkColor.copy(alpha = 0.30f), 1f to Color.Transparent,
                center = Offset(size.height * 0.6f, size.height / 2f), radius = size.width * 0.55f
            )
            val track = 2.dp.toPx()
            onDrawBehind {
                drawRect(body)
                drawRect(tint)
                drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.08f), 1f to Color.Transparent), size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
                if (!showProgress) return@onDrawBehind
                val p = progress().coerceIn(0f, 1f)
                val inset = 18.dp.toPx()
                val span = size.width - inset * 2f
                val y = track / 2f + 1.dp.toPx()
                drawLine(MonoDesign.accent.copy(alpha = 0.14f), Offset(inset, y), Offset(inset + span, y), track, StrokeCap.Round)
                if (p <= 0f) return@onDrawBehind
                val head = inset + span * p
                drawLine(
                    Brush.horizontalGradient(listOf(MonoDesign.accent.copy(alpha = 0.25f), MonoDesign.accent), startX = inset, endX = head),
                    Offset(inset, y), Offset(head, y), track, StrokeCap.Round
                )
                drawCircle(MonoDesign.accent.copy(alpha = 0.22f + 0.12f * breath.value), 7.dp.toPx(), Offset(head, y))
                drawCircle(MonoDesign.accent, 2.5.dp.toPx(), Offset(head, y))
            }
        }
    )
}

@Composable
internal fun MonoMiniArtwork(track: Track, playing: Boolean, animated: Boolean, modifier: Modifier) {
    val angle = remember { Animatable(0f) }
    val spinning = playing && animated
    LaunchedEffect(spinning) {
        if (!spinning) {
            angle.stop()
            return@LaunchedEffect
        }
        while (true) {
            angle.animateTo(angle.value + 360f, tween(MonoMiniSpinMs, easing = LinearEasing))
            angle.snapTo(angle.value % 360f)
        }
    }
    Box(
        modifier.drawBehind {
            drawCircle(MonoDesign.accent.copy(alpha = if (playing) 0.55f else 0.2f), size.minDimension / 2f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        }.padding(2.dp)
    ) {
        Box(
            Modifier.matchParentSize()
                .graphicsLayer { rotationZ = angle.value }
                .clip(CircleShape)
                .background(MonoDesign.raised)
        ) {
            CoverImage(track, Modifier.fillMaxSize())
            Box(
                Modifier.align(Alignment.Center).fillMaxSize(0.22f).clip(CircleShape).background(MonoDesign.base)
                    .border(1.dp, MonoDesign.hairlineStrong, CircleShape)
            )
        }
    }
}

@Composable
internal fun MonoMiniControls(
    playing: Boolean, resolving: Boolean, favorite: Boolean, showFavorite: Boolean, animated: Boolean,
    onFavorite: () -> Unit, onToggle: () -> Unit, onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    SideEffect { MonoChromeState.playing = playing }
    DisposableEffect(Unit) { onDispose { MonoChromeState.playing = false } }
    Row(Modifier.padding(end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showFavorite) MonoMiniHeart(favorite, animated, onFavorite)
        val corner = animateDpAsState(
            if (playing) 12.dp else 20.dp,
            if (animated) spring(dampingRatio = 0.55f, stiffness = 420f) else snap(),
            label = "mono-mini-play-corner"
        )
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                .levyraPressable(onClick = onToggle, enabled = !resolving, onClickLabel = if (playing) strings.pause else strings.play, role = Role.Button),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(40.dp).drawBehind { drawRoundRect(MonoDesign.accent, cornerRadius = CornerRadius(corner.value.toPx())) },
                contentAlignment = Alignment.Center
            ) {
                if (resolving) {
                    LevyraLoadingIndicator(Modifier.size(20.dp), MonoDesign.onAccent)
                } else {
                    Icon(
                        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (playing) strings.pause else strings.play,
                        tint = MonoDesign.onAccent, modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                .levyraPressable(onClick = onClose, onClickLabel = strings.close, role = Role.Button)
                .semantics { contentDescription = strings.close },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Close, null, tint = MonoDesign.textSecondary, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
private fun MonoMiniHeart(favorite: Boolean, animated: Boolean, onFavorite: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val pop = remember { Animatable(1f) }
    val last = remember { mutableStateOf(favorite) }
    LaunchedEffect(favorite) {
        if (last.value == favorite) return@LaunchedEffect
        last.value = favorite
        if (animated && favorite) {
            pop.snapTo(0.72f)
            pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 520f))
        }
    }
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .levyraPressable(onClick = onFavorite, role = Role.Switch, haptic = LevyraHapticAction.Confirm)
            .semantics {
                contentDescription = if (favorite) strings.removeFromFavorites else strings.addToFavorites
                toggleableState = ToggleableState(favorite)
                role = Role.Switch
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null,
            tint = if (favorite) MonoDesign.accent else MonoDesign.text,
            modifier = Modifier.size(22.dp).graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
        )
    }
}

@Composable
internal fun MonoMiniWaveProgress(progress: () -> Float, waving: Boolean, modifier: Modifier) {
    val clock = rememberMonoWaveClock(waving)
    val amplitude = animateFloatAsState(
        if (waving) 1f else 0f, if (LocalAnimationsEnabled.current) tween(520) else snap(), label = "mono-mini-wave"
    )
    val path = remember { Path() }
    Canvas(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 1.dp).height(8.dp)) {
        val elapsed = progress().coerceIn(0f, 1f)
        val split = size.width * elapsed
        val mid = size.height / 2f
        val mirrored = layoutDirection == LayoutDirection.Rtl
        withTransform({ if (mirrored) scale(-1f, 1f) }) {
            if (split < size.width) {
                drawLine(MonoDesign.hairlineStrong, Offset(split, mid), Offset(size.width, mid), 1.dp.toPx(), StrokeCap.Round)
            }
            drawMonoChromeWave(path, MonoDesign.text, clock.value, amplitude.value, 14f, 1.6.dp, elapsed)
        }
    }
}

private const val MonoMiniSpinMs = 9000
