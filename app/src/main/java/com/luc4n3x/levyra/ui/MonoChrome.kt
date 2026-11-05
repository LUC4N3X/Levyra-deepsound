package com.luc4n3x.levyra.ui

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
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

internal object MonoChromeState {
    var playing by mutableStateOf(false)
}

private val MonoNavPadding = 6.dp
private val MonoNavSpacing = 2.dp
private val MonoNavPillInset = 4.dp
private val MonoNavBottom = 8.dp
private val MonoNavKeyGap = 8.dp
private const val MonoNavSelectedWeight = 2.4f
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

private class MonoTabGeometry {
    var starts = FloatArray(0)
    var ends = FloatArray(0)

    fun ensure(count: Int) {
        if (starts.size != count) {
            starts = FloatArray(count)
            ends = FloatArray(count)
        }
    }

    fun at(edges: FloatArray, position: Float): Float {
        if (edges.isEmpty()) return 0f
        val clamped = position.coerceIn(0f, (edges.size - 1).toFloat())
        val low = floor(clamped).toInt()
        val high = (low + 1).coerceAtMost(edges.size - 1)
        val fraction = clamped - low
        return edges[low] + (edges[high] - edges[low]) * fraction
    }
}

@Composable
internal fun MonoBottomTabs(
    entries: List<LevyraTabEntry>, selected: LevyraTab, applySystemInset: Boolean, modifier: Modifier,
    onSelect: (LevyraTabEntry, Boolean) -> Unit
) {
    val morph = LocalDockMorph.current
    val frame = remember { DockMorphFrame() }
    val allEntries = rememberLevyraTabEntries()
    val searchEntry = remember(entries, allEntries) {
        entries.firstOrNull { it.tab == LevyraTab.Search } ?: allEntries.firstOrNull { it.tab == LevyraTab.Search }
    }
    val tabs = remember(entries) { entries.filterNot { it.tab == LevyraTab.Search } }
    val selectedIndex = tabs.indexOfFirst { it.tab == selected }.coerceAtLeast(0)
    val shape = MonoDesign.pillShape
    Column(
        modifier.fillMaxWidth().padding(start = MonoDesign.miniMargin, end = MonoDesign.miniMargin, bottom = MonoNavBottom)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.weight(1f)
                .dockMorphFrame(morph, frame, MonoDesign.navHeight)
                .height(MonoDesign.navHeight)
                .clip(shape)
                .background(MonoDesign.surface)
                .border(1.dp, MonoDesign.hairlineStrong, shape)
                .dockMorphReveal(
                    morph = morph,
                    slots = DockMorphSlots(tabs.size, selectedIndex, MonoNavPadding, MonoNavSpacing, MonoNavSelectedWeight),
                    row = DockMorphRow(
                        leading = MonoDesign.miniMargin, trailing = MonoDesign.miniMargin,
                        height = MonoDesign.navHeight, bottom = MonoNavBottom, gap = MonoNavKeyGap
                    ),
                    frame = frame
                )
        ) {
            MonoTabRow(tabs, selectedIndex, morph, onSelect)
        }
        if (searchEntry != null) MonoSearchKey(searchEntry, selected == LevyraTab.Search, { morph?.fraction ?: 0f }, onSelect)
        }
        if (applySystemInset) Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun MonoSearchKey(entry: LevyraTabEntry, active: Boolean, collapse: () -> Float, onSelect: (LevyraTabEntry, Boolean) -> Unit) {
    val lit by animateFloatAsState(if (active) 1f else 0f, if (LocalAnimationsEnabled.current) spring(stiffness = 420f) else snap(), label = "mono-search-key")
    val painter = rememberVectorPainter(entry.selectedIcon)
    val shape = CircleShape
    Box(
        Modifier
            .layout { measurable, constraints ->
                val shown = (1f - collapse()).coerceIn(0f, 1f)
                val gap = MonoNavKeyGap.roundToPx()
                val key = MonoDesign.navHeight.roundToPx()
                val placeable = measurable.measure(Constraints.fixed(key, key))
                layout(((key + gap) * shown).roundToInt(), key) {
                    placeable.placeRelativeWithLayer(gap, 0) { alpha = shown; scaleX = 0.6f + 0.4f * shown; scaleY = 0.6f + 0.4f * shown }
                }
            }
            .clip(shape)
            .drawBehind { drawRect(lerp(MonoDesign.surface, MonoDesign.accent, lit)) }
            .border(1.dp, MonoDesign.hairlineStrong, shape)
            .selectable(active, role = Role.Tab, onClick = { onSelect(entry, active) })
            .clearAndSetSemantics { contentDescription = entry.label; selected = active },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(24.dp).drawBehind {
            with(painter) { draw(size, colorFilter = ColorFilter.tint(lerp(MonoDesign.text, MonoDesign.onAccent, lit))) }
        })
    }
}

@Composable
private fun MonoTabRow(
    entries: List<LevyraTabEntry>, selectedIndex: Int, morph: DockMorph?,
    onSelect: (LevyraTabEntry, Boolean) -> Unit
) {
    val animated = LocalAnimationsEnabled.current
    val collapse: () -> Float = { morph?.fraction ?: 0f }
    val startEdge = remember { Animatable(selectedIndex.toFloat()) }
    val endEdge = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex, animated) {
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
        key(entry.tab) {
            animateFloatAsState(if (index == selectedIndex) 1f else 0f, revealSpec, label = "mono-tab-reveal")
        }
    }
    val geometry = remember { MonoTabGeometry() }
    Layout(
        content = {
            Box(Modifier.graphicsLayer { alpha = 1f - collapse() }.clip(MonoDesign.pillShape).background(MonoDesign.accent))
            entries.forEachIndexed { index, entry ->
                key(entry.tab) { MonoTab(entry, index == selectedIndex, reveals[index], collapse, onSelect) }
            }
        },
        modifier = Modifier.fillMaxSize().selectableGroup()
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val count = measurables.size - 1
        val padding = MonoNavPadding.toPx()
        val spacing = MonoNavSpacing.toPx()
        var total = 0f
        for (i in 0 until count) total += 1f + (MonoNavSelectedWeight - 1f) * reveals[i].value
        val unit = (width - padding * 2f - spacing * (count - 1).coerceAtLeast(0)).coerceAtLeast(0f) / total.coerceAtLeast(1f)
        geometry.ensure(count)
        var x = padding
        for (i in 0 until count) {
            val slot = unit * (1f + (MonoNavSelectedWeight - 1f) * reveals[i].value)
            geometry.starts[i] = x
            geometry.ends[i] = x + slot
            x += slot + spacing
        }
        val inner = (height - padding * 2f).roundToInt().coerceAtLeast(0)
        val tabs = List(count) { i ->
            val slot = (geometry.ends[i] - geometry.starts[i]).roundToInt().coerceAtLeast(0)
            measurables[i + 1].measure(Constraints.fixed(slot, inner))
        }
        val inset = MonoNavPillInset.roundToPx()
        val left = geometry.at(geometry.starts, startEdge.value)
        val right = geometry.at(geometry.ends, endEdge.value).coerceAtLeast(left)
        val pill = measurables[0].measure(Constraints.fixed((right - left).roundToInt(), (inner - inset * 2).coerceAtLeast(0)))
        val top = padding.roundToInt()
        layout(width, height) {
            if (count > 0) pill.placeRelative(left.roundToInt(), top + inset)
            tabs.forEachIndexed { i, tab -> tab.placeRelative(geometry.starts[i].roundToInt(), top) }
        }
    }
}

@Composable
private fun MonoTab(
    entry: LevyraTabEntry, active: Boolean, reveal: State<Float>, collapse: () -> Float,
    onSelect: (LevyraTabEntry, Boolean) -> Unit
) {
    val painter = rememberVectorPainter(if (active) entry.selectedIcon else entry.unselectedIcon)
    Box(
        Modifier.clip(MonoDesign.pillShape)
            .selectable(active, role = Role.Tab, onClick = { onSelect(entry, active) })
            .clearAndSetSemantics { contentDescription = entry.label },
        contentAlignment = Alignment.Center
    ) {
        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(24.dp).graphicsLayer { translationY = -2.dp.toPx() * reveal.value * (1f - collapse()) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(22.dp).drawBehind {
                        val lit = reveal.value.coerceIn(0f, 1f)
                        val tint = lerp(lerp(MonoDesign.textSecondary, MonoDesign.onAccent, lit), MonoDesign.accent, collapse() * lit)
                        with(painter) { draw(size, colorFilter = ColorFilter.tint(tint)) }
                    }
                )
                if (active) {
                    MonoNavWave(
                        MonoChromeState.playing, reveal, collapse,
                        Modifier.align(Alignment.BottomCenter).offset(y = 8.dp).size(width = 18.dp, height = 5.dp)
                    )
                }
            }
            Text(
                entry.label, color = MonoDesign.onAccent, style = MaterialTheme.typography.labelLarge,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clipToBounds().layout { measurable, constraints ->
                    val shown = (reveal.value * (1f - collapse())).coerceIn(0f, 1f)
                    if (shown <= 0.001f) return@layout layout(0, 0) {}
                    val gap = 7.dp.roundToPx()
                    val placeable = measurable.measure(Constraints(maxWidth = (constraints.maxWidth - gap).coerceAtLeast(0)))
                    layout(((placeable.width + gap) * shown).roundToInt(), placeable.height) {
                        placeable.placeRelativeWithLayer(gap, 0) { alpha = shown * shown }
                    }
                }
            )
        }
    }
}

@Composable
private fun MonoNavWave(playing: Boolean, reveal: State<Float>, collapse: () -> Float, modifier: Modifier) {
    val clock = rememberMonoWaveClock(playing)
    val presence = animateFloatAsState(if (playing) 1f else 0f, tween(320), label = "mono-nav-wave")
    val path = remember { Path() }
    Canvas(modifier) {
        val shown = presence.value * reveal.value.coerceIn(0f, 1f)
        if (shown <= 0.01f) return@Canvas
        val color = lerp(MonoDesign.onAccent, MonoDesign.accent, collapse())
        drawMonoChromeWave(path, color.copy(alpha = shown), clock.value, shown, 2.5f, 1.5.dp)
    }
}

@Composable
internal fun MonoMiniArtwork(track: Track, playing: Boolean, animated: Boolean, modifier: Modifier) {
    val breath = remember { Animatable(1f) }
    val breathing = playing && animated
    LaunchedEffect(breathing) {
        if (!breathing) {
            if (animated) breath.animateTo(1f, spring(stiffness = 300f)) else breath.snapTo(1f)
            return@LaunchedEffect
        }
        while (true) {
            breath.animateTo(0.955f, tween(1700, easing = FastOutSlowInEasing))
            breath.animateTo(1f, tween(1700, easing = FastOutSlowInEasing))
        }
    }
    Box(modifier) {
        Box(
            Modifier.matchParentSize()
                .graphicsLayer {
                    scaleX = breath.value
                    scaleY = breath.value
                }
                .clip(MonoDesign.artShape)
                .background(MonoDesign.raised)
        ) {
            CoverImage(track, Modifier.fillMaxSize())
        }
    }
}

@Composable
internal fun MonoMiniControls(
    playing: Boolean, resolving: Boolean, favorite: Boolean, showFavorite: Boolean, animated: Boolean,
    onFavorite: () -> Unit, onToggle: () -> Unit
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
