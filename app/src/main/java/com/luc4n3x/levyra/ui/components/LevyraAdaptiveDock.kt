package com.luc4n3x.levyra.ui.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.GlassBackdropState
import com.luc4n3x.levyra.ui.glassFrost
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraIsPureBlack
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import kotlin.math.roundToInt

private val DockCollapseDistance = 56.dp
private val DockExpandDistance = 24.dp
private val DockBlurRadius = 28.dp
private val DockShadowDark = 16.dp
private val DockShadowLight = 10.dp
private const val DockFadeSpan = 0.6f
private val DockMorphGap = 8.dp
private val DockMorphTrayRow = 48.dp

@Stable
class LevyraDockState internal constructor(
    private val collapseDistancePx: Float,
    private val expandDistancePx: Float
) : NestedScrollConnection {
    var compact by mutableStateOf(false)
        private set
    internal var compactionAllowed = true
    private var travelPx = 0f

    fun expand() {
        travelPx = 0f
        if (compact) compact = false
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y > 0f) {
            expand()
            return Offset.Zero
        }
        val delta = consumed.y
        if (delta == 0f || !delta.isFinite()) return Offset.Zero
        if (travelPx != 0f && (travelPx > 0f != delta > 0f)) travelPx = 0f
        travelPx += delta
        if (!compact && travelPx <= -collapseDistancePx) {
            travelPx = 0f
            if (compactionAllowed) compact = true
        } else if (compact && travelPx >= expandDistancePx) {
            expand()
        }
        return Offset.Zero
    }
}

@Composable
fun rememberLevyraDockState(enabled: Boolean = true): LevyraDockState {
    val density = LocalDensity.current
    val state = remember(density) {
        with(density) { LevyraDockState(DockCollapseDistance.toPx(), DockExpandDistance.toPx()) }
    }
    val context = LocalContext.current
    var touchExploration by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
            touchExploration = enabled
        }
        touchExploration = manager?.isTouchExplorationEnabled == true
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    LaunchedEffect(state, touchExploration, enabled) {
        state.compactionAllowed = !touchExploration && enabled
        if (!state.compactionAllowed) state.expand()
    }
    return state
}

@Composable
fun LevyraDockState.animatedCompaction(animated: Boolean): State<Float> =
    animateFloatAsState(
        targetValue = if (compact) 1f else 0f,
        animationSpec = LevyraMotion.physics(animated, if (compact) LevyraMotion.settle else LevyraMotion.spatial),
        label = "dock-compaction"
    )

fun dockFade(compaction: Float): Float = (1f - compaction / DockFadeSpan).coerceIn(0f, 1f)

fun Modifier.dockLerpHeight(compaction: () -> Float, expanded: Dp, compact: Dp): Modifier =
    layout { measurable, constraints ->
        val height = lerpPx(expanded.toPx(), compact.toPx(), compaction()).roundToInt()
        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        layout(placeable.width, height) { placeable.place(0, 0) }
    }

fun Modifier.dockLerpSize(compaction: () -> Float, expanded: Dp, compact: Dp): Modifier =
    layout { measurable, _ ->
        val size = lerpPx(expanded.toPx(), compact.toPx(), compaction()).roundToInt()
        val placeable = measurable.measure(Constraints.fixed(size, size))
        layout(size, size) { placeable.place(0, 0) }
    }

fun Modifier.dockFoldHeight(compaction: () -> Float): Modifier =
    clipToBounds()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minHeight = 0))
            val height = (placeable.height * (1f - compaction().coerceIn(0f, 1f))).roundToInt()
            layout(placeable.width, height) { placeable.place(0, 0) }
        }
        .graphicsLayer { alpha = dockFade(compaction()) }

fun Modifier.dockFoldWidth(compaction: () -> Float): Modifier =
    clipToBounds()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0))
            val width = (placeable.width * (1f - compaction().coerceIn(0f, 1f))).roundToInt()
            layout(width, placeable.height) { placeable.placeRelative(0, 0) }
        }
        .graphicsLayer { alpha = dockFade(compaction()) }

fun Modifier.dockClipHeight(compaction: () -> Float, collapseBy: Dp): Modifier =
    clipToBounds()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val height = (placeable.height - collapseBy.toPx() * compaction().coerceIn(0f, 1f))
                .roundToInt()
                .coerceAtLeast(0)
            layout(placeable.width, height) { placeable.place(0, 0) }
        }

@Immutable
private data class DockMaterial(
    val ground: Color,
    val frostTop: Color,
    val frostBottom: Color,
    val solidTop: Color,
    val solidBottom: Color,
    val rim: Color,
    val shadow: Dp
)

@Composable
private fun rememberDockMaterial(): DockMaterial {
    val isLight = LevyraActivePalette.isLight
    val pureBlack = LevyraIsPureBlack
    val ink = LevyraInk
    val black = LevyraBlack
    val panel = LevyraPanel
    return remember(isLight, pureBlack, ink, black, panel) {
        when {
            isLight -> DockMaterial(
                ground = black,
                frostTop = Color.White.copy(alpha = 0.74f),
                frostBottom = panel.copy(alpha = 0.88f),
                solidTop = Color.White,
                solidBottom = panel,
                rim = Color(0x1A11131F),
                shadow = DockShadowLight
            )
            pureBlack -> DockMaterial(
                ground = Color.Black,
                frostTop = Color.Black.copy(alpha = 0.74f),
                frostBottom = Color.Black.copy(alpha = 0.90f),
                solidTop = Color.Black,
                solidBottom = Color.Black,
                rim = Color.White.copy(alpha = 0.10f),
                shadow = DockShadowDark
            )
            else -> DockMaterial(
                ground = black,
                frostTop = ink.copy(alpha = 0.72f),
                frostBottom = black.copy(alpha = 0.88f),
                solidTop = ink,
                solidBottom = black,
                rim = Color.White.copy(alpha = 0.12f),
                shadow = DockShadowDark
            )
        }
    }
}

@Composable
fun LevyraAdaptiveDockSurface(
    glass: GlassBackdropState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val material = rememberDockMaterial()
    val shape = remember {
        RoundedCornerShape(topStart = LevyraPlayerDesign.DockTrayCorner, topEnd = LevyraPlayerDesign.DockTrayCorner)
    }
    val frostBrush = remember(material) { Brush.verticalGradient(listOf(material.frostTop, material.frostBottom)) }
    val solidBrush = remember(material) { Brush.verticalGradient(listOf(material.solidTop, material.solidBottom)) }
    val rimBrush = remember(material) { Brush.verticalGradient(0f to material.rim, 0.35f to Color.Transparent) }
    Column(
        modifier = modifier
            .shadow(material.shadow, shape, clip = true)
            .glassFrost(
                state = glass,
                blurRadius = DockBlurRadius,
                groundColor = material.ground,
                onDrawFrosted = { drawRect(frostBrush) },
                onDrawFallback = { drawRect(solidBrush) }
            )
            .border(LevyraPlayerDesign.Hairline, rimBrush, shape),
        content = content
    )
}

private fun lerpPx(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

private fun lerpPx(start: Int, stop: Int, fraction: Float): Int =
    lerpPx(start.toFloat(), stop.toFloat(), fraction).roundToInt()

@Immutable
data class DockMorphSlots(
    val count: Int,
    val index: Int,
    val padding: Dp = 0.dp,
    val spacing: Dp = 0.dp,
    val selectedWeight: Float = 1f
)

@Immutable
data class DockMorphRow(
    val leading: Dp,
    val trailing: Dp,
    val height: Dp,
    val bottom: Dp = 0.dp,
    val gap: Dp = DockMorphGap
)

@Stable
class DockMorph(private val progress: () -> Float) {
    internal var anchorEndPx = -1
    internal var trailingPx = 0
    internal var rowHeightPx = 0
    internal var rowBottomPx = 0
    internal var gapPx = 0
    internal var revealExtentPx = Float.MAX_VALUE
    internal var revealRtl = false

    val fraction: Float get() = progress().coerceIn(0f, 1f)

    internal fun revealContains(x: Float, width: Int): Boolean {
        val leading = if (revealRtl) width - x else x
        return leading <= revealExtentPx
    }
    internal val ready: Boolean get() = anchorEndPx >= 0
}

class DockMorphFrame {
    internal var shrinkPx = 0
    internal var anchorPx = 0
}

val LocalDockMorph = staticCompositionLocalOf<DockMorph?> { null }

fun Modifier.dockMorphFrame(morph: DockMorph?, frame: DockMorphFrame, anchor: Dp): Modifier =
    if (morph == null) {
        this
    } else {
        layout { measurable, constraints ->
            val full = constraints.maxWidth
            val anchorPx = anchor.roundToPx().coerceAtMost(full)
            frame.anchorPx = anchorPx
            val fraction = morph.fraction
            if (fraction <= 0f) {
                frame.shrinkPx = 0
                val placeable = measurable.measure(constraints)
                return@layout layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
            }
            val width = lerpPx(full, anchorPx, fraction)
            frame.shrinkPx = full - width
            val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
            layout(full, placeable.height) { placeable.placeRelative(0, 0) }
        }
    }

fun Modifier.dockMorphReveal(
    morph: DockMorph?,
    slots: DockMorphSlots,
    row: DockMorphRow,
    frame: DockMorphFrame? = null
): Modifier =
    if (morph == null) {
        this
    } else {
        pointerInput(morph) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (morph.fraction <= 0f) continue
                    val outside = event.changes.any { change -> !morph.revealContains(change.position.x, size.width) }
                    if (outside) event.changes.forEach { it.consume() }
                }
            }
        }
            .drawWithContent {
                if (morph.fraction <= 0f) {
                    drawContent()
                } else {
                    val start = if (layoutDirection == LayoutDirection.Rtl) size.width - morph.revealExtentPx else 0f
                    clipRect(left = start, right = start + morph.revealExtentPx) { this@drawWithContent.drawContent() }
                }
            }
            .graphicsLayer { clip = morph.fraction > 0f }
            .layout { measurable, constraints ->
                val fraction = morph.fraction
                val visible = constraints.maxWidth
                morph.revealRtl = layoutDirection == LayoutDirection.Rtl
                val content = visible + (frame?.shrinkPx ?: 0)
                val count = slots.count.coerceAtLeast(1)
                val index = slots.index.coerceIn(0, count - 1)
                val padding = slots.padding.toPx()
                val spacing = slots.spacing.toPx()
                val available = (content - padding * 2f - spacing * (count - 1)).coerceAtLeast(0f)
                val unit = available / (count - 1 + slots.selectedWeight).coerceAtLeast(1f)
                val selectedWidth = unit * slots.selectedWeight
                val selectedCenter = padding + (unit + spacing) * index + selectedWidth / 2f
                val anchorExtent = frame?.anchorPx ?: (padding + selectedWidth).roundToInt()
                morph.anchorEndPx = row.leading.roundToPx() + anchorExtent
                morph.trailingPx = row.trailing.roundToPx()
                morph.rowHeightPx = row.height.roundToPx()
                morph.rowBottomPx = row.bottom.roundToPx()
                morph.gapPx = row.gap.roundToPx()
                if (fraction <= 0f) {
                    morph.revealExtentPx = visible.toFloat()
                    val placeable = measurable.measure(constraints)
                    return@layout layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                }
                val placeable = measurable.measure(constraints.copy(minWidth = content, maxWidth = content))
                val target = if (frame != null) visible / 2f else padding + selectedWidth / 2f
                morph.revealExtentPx = if (frame != null) {
                    visible.toFloat()
                } else {
                    lerpPx(content.toFloat(), anchorExtent.coerceAtMost(content).toFloat(), fraction)
                }
                val shift = ((target - selectedCenter) * fraction).roundToInt()
                layout(visible, placeable.height) { placeable.placeRelative(shift, 0) }
            }
    }

fun Modifier.dockMorphTray(morph: DockMorph?, count: Int, index: Int, leading: Dp = 0.dp): Modifier =
    dockMorphReveal(
        morph = morph,
        slots = DockMorphSlots(count = count, index = index),
        row = DockMorphRow(leading = leading, trailing = DockMorphGap, height = DockMorphTrayRow)
    )

fun Modifier.dockMorphPadding(morph: DockMorph?, start: Dp, end: Dp, top: Dp, bottom: Dp): Modifier =
    if (morph == null) {
        padding(start = start, end = end, top = top, bottom = bottom)
    } else {
        layout { measurable, constraints ->
            val keep = 1f - morph.fraction
            val startPx = (start.toPx() * keep).roundToInt()
            val endPx = (end.toPx() * keep).roundToInt()
            val topPx = (top.toPx() * keep).roundToInt()
            val bottomPx = (bottom.toPx() * keep).roundToInt()
            val placeable = measurable.measure(constraints.offset(-(startPx + endPx), -(topPx + bottomPx)))
            layout(
                constraints.constrainWidth(placeable.width + startPx + endPx),
                constraints.constrainHeight(placeable.height + topPx + bottomPx)
            ) { placeable.placeRelative(startPx, topPx) }
        }
    }

fun Modifier.dockMorphHeight(morph: DockMorph?, compaction: () -> Float, expanded: Dp, compact: Dp): Modifier =
    if (morph == null) {
        dockLerpHeight(compaction, expanded, compact)
    } else {
        layout { measurable, constraints ->
            val legacy = lerpPx(expanded.toPx(), compact.toPx(), compaction())
            val target = if (morph.ready) morph.rowHeightPx.toFloat() else legacy
            val height = lerpPx(legacy, target, morph.fraction).roundToInt()
            val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
            layout(placeable.width, height) { placeable.place(0, 0) }
        }
    }

fun DockMorph?.morphedCompaction(compaction: () -> Float): () -> Float =
    if (this == null) compaction else { { maxOf(compaction(), fraction) } }

@Composable
fun LevyraDockMorphLayout(
    morph: DockMorph,
    mini: @Composable () -> Unit,
    dock: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    val systemBars = WindowInsets.navigationBars
    Layout(contents = listOf(dock, mini, trailing), modifier = modifier) { (dockMeasurables, miniMeasurables, trailingMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val dockPlaceables = dockMeasurables.map { it.measure(loose) }
        val dockWidth = dockPlaceables.maxOfOrNull { it.width } ?: 0
        val dockHeight = dockPlaceables.sumOf { it.height }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else dockWidth
        val frameX = (width - dockWidth) / 2
        val fraction = if (morph.ready && miniMeasurables.isNotEmpty()) morph.fraction else 0f
        if (fraction <= 0f) {
            val miniPlaceables = miniMeasurables.map { it.measure(loose) }
            val miniHeight = miniPlaceables.sumOf { it.height }
            return@Layout layout(width, constraints.constrainHeight(miniHeight + dockHeight)) {
                var y = 0
                miniPlaceables.forEach { placeable ->
                    placeable.placeRelative((width - placeable.width) / 2, y)
                    y += placeable.height
                }
                dockPlaceables.forEach { placeable ->
                    placeable.placeRelative((width - placeable.width) / 2, y)
                    y += placeable.height
                }
            }
        }
        val slotStart = morph.anchorEndPx + morph.gapPx
        val bloomSize = morph.rowHeightPx
        val bloomPlaceables = trailingMeasurables.map { it.measure(Constraints.fixed(bloomSize, bloomSize)) }
        val bloomReserve = if (bloomPlaceables.isEmpty()) 0 else bloomSize + morph.gapPx
        val slotWidth = (dockWidth - morph.trailingPx - bloomReserve - slotStart).coerceAtLeast(0)
        val miniWidth = lerpPx(dockWidth, slotWidth, fraction)
        val miniPlaceables = miniMeasurables.map {
            it.measure(loose.copy(minWidth = miniWidth, maxWidth = miniWidth))
        }
        val miniHeight = miniPlaceables.sumOf { it.height }
        val rowCenter = dockHeight - systemBars.getBottom(this) - morph.rowBottomPx - morph.rowHeightPx / 2
        val mergedHeight = dockHeight + (miniHeight / 2 - rowCenter).coerceAtLeast(0)
        val height = lerpPx(miniHeight + dockHeight, mergedHeight, fraction)
        val dockY = height - dockHeight
        val miniX = frameX + lerpPx(0, slotStart, fraction)
        val miniY = lerpPx(0, dockY + rowCenter - miniHeight / 2, fraction)
        layout(width, constraints.constrainHeight(height)) {
            var y = dockY
            dockPlaceables.forEach { placeable ->
                placeable.placeRelative(frameX, y)
                y += placeable.height
            }
            var top = miniY
            miniPlaceables.forEach { placeable ->
                placeable.placeRelative(miniX, top)
                top += placeable.height
            }
            bloomPlaceables.forEach { placeable ->
                placeable.placeRelative(
                    frameX + dockWidth - morph.trailingPx - bloomSize,
                    dockY + rowCenter - bloomSize / 2
                )
            }
        }
    }
}
