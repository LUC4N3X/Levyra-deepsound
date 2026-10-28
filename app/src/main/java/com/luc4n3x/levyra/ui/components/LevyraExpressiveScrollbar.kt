package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.ceil
import kotlin.math.roundToInt

private val ScrollbarHitWidth = 32.dp
private val ScrollbarEndInset = 4.dp
private val ScrollbarRestWidth = 6.dp
private val ScrollbarGrabWidth = 24.dp
private val ScrollbarThumbHeight = 52.dp
private val ScrollbarTrackWidth = 3.dp
private val ScrollbarLabelSize = 60.dp
private val ScrollbarLabelGap = 14.dp
private val ScrollbarSlide = 14.dp
private const val ScrollbarIdleHideMs = 1_400L
private const val ScrollbarMinViewports = 2.5f
private const val ScrollbarTrackAlpha = 0.12f

internal fun levyraScrollbarProgress(
    firstIndex: Int,
    firstOffsetFraction: Float,
    totalItems: Int,
    visibleItems: Float
): Float {
    val scrollable = totalItems - visibleItems
    if (totalItems <= 0 || scrollable <= 0f) return 0f
    return ((firstIndex + firstOffsetFraction.coerceIn(0f, 1f)) / scrollable).coerceIn(0f, 1f)
}

internal fun levyraScrollbarTargetIndex(progress: Float, totalItems: Int, visibleItems: Float): Int {
    if (totalItems <= 1) return 0
    val scrollable = (totalItems - visibleItems).coerceAtLeast(0f)
    return (progress.coerceIn(0f, 1f) * scrollable).roundToInt().coerceIn(0, totalItems - 1)
}

internal fun levyraScrollbarWorthShowing(totalItems: Int, visibleItems: Float): Boolean =
    totalItems > 0 && visibleItems > 0f && totalItems >= visibleItems * ScrollbarMinViewports

internal fun levyraScrollbarGlyph(value: String?): String? {
    val lead = value.orEmpty().trim().firstOrNull { it.isLetterOrDigit() } ?: return null
    return if (lead.isDigit()) "#" else lead.uppercaseChar().toString()
}

@Stable
private interface ScrollbarSource {
    val totalItems: Int
    val visibleItems: Float
    val firstIndex: Int
    val firstOffsetFraction: Float
    val atStart: Boolean
    val atEnd: Boolean
    val scrolling: Boolean
    val firstVisibleKey: Any?
    suspend fun scrollToIndex(index: Int)
}

private class ListScrollbarSource(private val state: LazyListState) : ScrollbarSource {
    override val totalItems: Int get() = state.layoutInfo.totalItemsCount
    override val visibleItems: Float
        get() {
            val info = state.layoutInfo
            val items = info.visibleItemsInfo
            if (items.isEmpty()) return 0f
            val average = items.sumOf { it.size + info.mainAxisItemSpacing }.toFloat() / items.size
            val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
            return if (average > 0f) viewport / average else items.size.toFloat()
        }
    override val firstIndex: Int get() = state.firstVisibleItemIndex
    override val firstOffsetFraction: Float
        get() {
            val size = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: return 0f
            return if (size > 0) state.firstVisibleItemScrollOffset / size.toFloat() else 0f
        }
    override val atStart: Boolean get() = !state.canScrollBackward
    override val atEnd: Boolean get() = !state.canScrollForward
    override val scrolling: Boolean get() = state.isScrollInProgress
    override val firstVisibleKey: Any? get() = state.layoutInfo.visibleItemsInfo.firstOrNull()?.key
    override suspend fun scrollToIndex(index: Int) = state.scrollToItem(index)
}

private class GridScrollbarSource(private val state: LazyGridState) : ScrollbarSource {
    private val span: Int get() = state.layoutInfo.maxSpan.coerceAtLeast(1)
    override val totalItems: Int get() = ceil(state.layoutInfo.totalItemsCount / span.toFloat()).toInt()
    override val visibleItems: Float
        get() {
            val info = state.layoutInfo
            val rows = info.visibleItemsInfo.groupBy { it.row }
            if (rows.isEmpty()) return 0f
            val average = rows.values.sumOf { line -> line.maxOf { it.size.height } + info.mainAxisItemSpacing }
                .toFloat() / rows.size
            val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
            return if (average > 0f) viewport / average else rows.size.toFloat()
        }
    override val firstIndex: Int get() = state.firstVisibleItemIndex / span
    override val firstOffsetFraction: Float
        get() {
            val size = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: return 0f
            return if (size > 0) state.firstVisibleItemScrollOffset / size.toFloat() else 0f
        }
    override val atStart: Boolean get() = !state.canScrollBackward
    override val atEnd: Boolean get() = !state.canScrollForward
    override val scrolling: Boolean get() = state.isScrollInProgress
    override val firstVisibleKey: Any? get() = state.layoutInfo.visibleItemsInfo.firstOrNull()?.key
    override suspend fun scrollToIndex(index: Int) = state.scrollToItem(index * span)
}

@Composable
fun LevyraExpressiveScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    label: ((Any) -> String?)? = null
) {
    val source = remember(state) { ListScrollbarSource(state) }
    ScrollbarHost(source, modifier, label)
}

@Composable
fun LevyraExpressiveScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier,
    label: ((Any) -> String?)? = null
) {
    val source = remember(state) { GridScrollbarSource(state) }
    ScrollbarHost(source, modifier, label)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScrollbarHost(
    source: ScrollbarSource,
    modifier: Modifier,
    label: ((Any) -> String?)?
) {
    val animated = LocalAnimationsEnabled.current
    val haptics = LocalLevyraHaptics.current
    val colors = MaterialTheme.colorScheme
    val worthShowing by remember(source) {
        derivedStateOf { levyraScrollbarWorthShowing(source.totalItems, source.visibleItems) }
    }
    val listProgress by remember(source) {
        derivedStateOf {
            when {
                source.atStart -> 0f
                source.atEnd -> 1f
                else -> levyraScrollbarProgress(
                    source.firstIndex,
                    source.firstOffsetFraction,
                    source.totalItems,
                    source.visibleItems
                )
            }
        }
    }
    var dragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var pendingIndex by remember { mutableIntStateOf(-1) }
    var shown by remember { mutableStateOf(false) }
    val active = worthShowing && (source.scrolling || dragging)

    LaunchedEffect(active) {
        if (active) {
            shown = true
        } else {
            delay(ScrollbarIdleHideMs)
            shown = false
        }
    }
    LaunchedEffect(source) {
        snapshotFlow { pendingIndex }
            .distinctUntilChanged()
            .collectLatest { index -> if (index >= 0) source.scrollToIndex(index) }
    }

    val reveal by animateFloatAsState(
        targetValue = if (shown && worthShowing) 1f else 0f,
        animationSpec = if (animated) LevyraMotion.settle.spec() else snap(),
        label = "scrollbar-reveal"
    )
    val thumbWidth by animateDpAsState(
        targetValue = if (dragging) ScrollbarGrabWidth else ScrollbarRestWidth,
        animationSpec = if (animated) LevyraMotion.expressive.spec() else snap(),
        label = "scrollbar-width"
    )
    val activeLabel by remember(source, label) {
        derivedStateOf { source.firstVisibleKey?.let { key -> label?.invoke(key) } }
    }
    val labelVisible = dragging && !activeLabel.isNullOrBlank()
    val labelReveal by animateFloatAsState(
        targetValue = if (labelVisible) 1f else 0f,
        animationSpec = if (animated) LevyraMotion.expressive.spec() else snap(),
        label = "scrollbar-label"
    )
    var shownLabel by remember { mutableStateOf("") }
    LaunchedEffect(activeLabel, dragging) {
        val next = activeLabel
        if (!next.isNullOrBlank() && next != shownLabel) {
            if (dragging && shownLabel.isNotEmpty()) haptics.perform(LevyraHapticAction.SeekSnap)
            shownLabel = next
        }
    }
    if (reveal <= 0f && !worthShowing) return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(ScrollbarHitWidth + ScrollbarLabelSize + ScrollbarLabelGap)
            .clearAndSetSemantics {}
    ) {
        val density = LocalDensity.current
        val trackHeightPx = with(density) { maxHeight.toPx() }
        val thumbHeightPx = with(density) { ScrollbarThumbHeight.toPx() }
        val travelPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)
        val slidePx = with(density) { ScrollbarSlide.toPx() }
        val displayed = { if (dragging) dragProgress else listProgress }

        fun progressAt(y: Float): Float = ((y - thumbHeightPx / 2f) / travelPx).coerceIn(0f, 1f)
        fun jumpTo(progress: Float) {
            dragProgress = progress
            pendingIndex = levyraScrollbarTargetIndex(progress, source.totalItems, source.visibleItems)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .width(ScrollbarHitWidth)
                .graphicsLayer {
                    alpha = reveal
                    translationX = (1f - reveal) * slidePx
                }
                .scrollbarDrag(
                    enabled = shown && worthShowing,
                    key = source,
                    onStart = { y ->
                        dragging = true
                        haptics.perform(LevyraHapticAction.SeekSnap)
                        jumpTo(progressAt(y))
                    },
                    onMove = { y -> jumpTo(progressAt(y)) },
                    onEnd = {
                        dragging = false
                        pendingIndex = -1
                    }
                )
        ) {
            ScrollbarTrack()
            ScrollbarThumb(width = thumbWidth, offsetY = { (displayed() * travelPx).roundToInt() })
        }

        if (labelReveal > 0f && shownLabel.isNotEmpty()) {
            ScrollbarLabel(
                text = shownLabel,
                reveal = { labelReveal },
                slidePx = slidePx,
                animated = animated,
                offsetY = { displayed() * travelPx + thumbHeightPx / 2f },
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

@Composable
private fun BoxScope.ScrollbarTrack() {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = -(ScrollbarEndInset + (ScrollbarRestWidth - ScrollbarTrackWidth) / 2))
            .width(ScrollbarTrackWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = ScrollbarTrackAlpha))
    )
}

@Composable
private fun BoxScope.ScrollbarThumb(width: Dp, offsetY: () -> Int) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset { IntOffset(-ScrollbarEndInset.roundToPx(), offsetY()) }
            .size(width = width, height = ScrollbarThumbHeight)
            .clip(
                RoundedCornerShape(
                    topStartPercent = 50,
                    bottomStartPercent = 50,
                    topEndPercent = 30,
                    bottomEndPercent = 30
                )
            )
            .background(colors.primary),
        contentAlignment = Alignment.Center
    ) {
        if (width > ScrollbarRestWidth * 2) {
            val grow = ((width - ScrollbarRestWidth) / (ScrollbarGrabWidth - ScrollbarRestWidth)).coerceIn(0f, 1f)
            Icon(
                imageVector = Icons.Rounded.UnfoldMore,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        alpha = grow
                        scaleX = grow
                        scaleY = grow
                    }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScrollbarLabel(
    text: String,
    reveal: () -> Float,
    slidePx: Float,
    animated: Boolean,
    offsetY: () -> Float,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialShapes.Cookie9Sided.toShape()
    Box(
        modifier = modifier
            .offset { IntOffset(0, (offsetY() - ScrollbarLabelSize.toPx() / 2f).roundToInt()) }
            .size(ScrollbarLabelSize)
            .graphicsLayer {
                val value = reveal()
                alpha = value
                val scale = 0.6f + 0.4f * value
                scaleX = scale
                scaleY = scale
                translationX = (1f - value) * slidePx
                transformOrigin = TransformOrigin(1f, 0.5f)
            }
            .shadow(6.dp, shape)
            .background(colors.primaryContainer, shape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = { LevyraMotion.trackChange(animated, 0) },
            label = "scrollbar-glyph"
        ) { glyph ->
            Text(
                text = glyph,
                color = colors.onPrimaryContainer,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

private fun Modifier.scrollbarDrag(
    enabled: Boolean,
    key: Any,
    onStart: (Float) -> Unit,
    onMove: (Float) -> Unit,
    onEnd: () -> Unit
): Modifier {
    if (!enabled) return this
    return pointerInput(key) {
        detectVerticalDragGestures(
            onDragStart = { offset -> onStart(offset.y) },
            onDragEnd = onEnd,
            onDragCancel = onEnd,
            onVerticalDrag = { change, _ ->
                change.consume()
                onMove(change.position.y)
            }
        )
    }
}
