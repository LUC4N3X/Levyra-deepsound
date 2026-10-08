package com.luc4n3x.levyra.ui.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.GlassBackdropState
import com.luc4n3x.levyra.ui.glassFrost
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraIsPureBlack
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import kotlin.math.roundToInt

private val DockCollapseDistance = 56.dp
private val DockExpandDistance = 24.dp
private val DockBlurRadius = 20.dp
private val DockShadowDark = 10.dp
private val DockShadowLight = 6.dp
private const val DockFadeSpan = 0.6f

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
fun rememberLevyraDockState(): LevyraDockState {
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
    LaunchedEffect(state, touchExploration) {
        state.compactionAllowed = !touchExploration
        if (touchExploration) state.expand()
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
    val colors = MaterialTheme.colorScheme
    val black = LevyraBlack
    return remember(isLight, pureBlack, black, colors) {
        when {
            isLight -> DockMaterial(
                ground = colors.surface,
                frostTop = colors.surfaceContainerHigh.copy(alpha = 0.82f),
                frostBottom = colors.surfaceContainer.copy(alpha = 0.94f),
                solidTop = colors.surfaceContainerHigh,
                solidBottom = colors.surfaceContainer,
                rim = colors.outlineVariant.copy(alpha = 0.42f),
                shadow = DockShadowLight
            )
            pureBlack -> DockMaterial(
                ground = Color.Black,
                frostTop = Color.Black.copy(alpha = 0.82f),
                frostBottom = Color.Black.copy(alpha = 0.94f),
                solidTop = Color.Black,
                solidBottom = Color.Black,
                rim = Color.White.copy(alpha = 0.08f),
                shadow = DockShadowDark
            )
            else -> DockMaterial(
                ground = black,
                frostTop = colors.surfaceContainerHigh.copy(alpha = 0.76f),
                frostBottom = colors.surfaceContainer.copy(alpha = 0.92f),
                solidTop = colors.surfaceContainerHigh,
                solidBottom = colors.surfaceContainer,
                rim = colors.outlineVariant.copy(alpha = 0.38f),
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
