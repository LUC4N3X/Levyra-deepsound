package com.luc4n3x.levyra.ui.library

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.monoPanel
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.MonoDesign
import kotlin.math.exp
import kotlinx.coroutines.launch

internal val MonoDeckPeek = 76.dp
internal val MonoDeckCardHeight = 172.dp
private const val MonoDeckSpreadFactor = 0.14f
internal const val MonoScrubberLetters = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ"

internal class MonoLibraryMenuItem(val label: String, val action: () -> Unit)

@Composable
internal fun MonoDeckCard(
    index: Int,
    title: String,
    meta: String,
    ordinal: String,
    covers: List<String>,
    tint: Color,
    last: Boolean,
    listState: LazyListState,
    spread: () -> Float,
    onOpen: () -> Unit,
    menu: () -> List<MonoLibraryMenuItem>,
    modifier: Modifier = Modifier
) {
    val animations = LocalAnimationsEnabled.current
    val scope = rememberCoroutineScope()
    val lift = remember { Animatable(0f) }
    var lifted by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val liftPx = with(density) { 18.dp.toPx() }
    val wash = remember(tint) { Brush.verticalGradient(0f to tint.copy(alpha = 0.38f), 0.62f to Color.Transparent) }
    val open = {
        if (!lifted) {
            if (!animations) {
                onOpen()
            } else {
                lifted = true
                scope.launch {
                    lift.animateTo(1f, tween(170, easing = FastOutSlowInEasing))
                    onOpen()
                    lift.snapTo(0f)
                    lifted = false
                }
            }
        }
    }
    Box(
        modifier.fillMaxWidth()
            .height(if (last) MonoDeckCardHeight else MonoDeckPeek)
            .zIndex(if (lifted) 1f else 0f)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .height(MonoDeckCardHeight)
                .graphicsLayer {
                    val info = listState.layoutInfo
                    val visible = info.visibleItemsInfo
                    var shift = 0f
                    var scale = 1f
                    var fade = 1f
                    var tilt = 0f
                    if (visible.isNotEmpty()) {
                        val slot = index - visible[0].index
                        val item = if (slot in visible.indices) visible[slot] else null
                        if (item != null && item.index == index) {
                            val top = (item.offset - info.viewportStartOffset).toFloat()
                            val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
                            if (top < 0f) {
                                val depth = (-top / item.size.coerceAtLeast(1)).coerceIn(0f, 1f)
                                shift = -top * 0.72f
                                scale = 1f - 0.08f * depth
                                fade = 1f - depth
                                tilt = 14f * depth
                            }
                            shift += spread() * (top - viewport * 0.35f) * MonoDeckSpreadFactor
                        }
                    }
                    val raise = lift.value
                    translationY = shift - raise * liftPx
                    scaleX = scale * (1f + 0.03f * raise)
                    scaleY = scaleX
                    alpha = fade
                    rotationX = tilt
                    cameraDistance = 14f * this.density
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .monoPanel(MonoDesign.panelShape, MonoDesign.raised)
                .background(wash)
                .clickable(role = Role.Button, onClickLabel = LocalLevyraStrings.current.open, onClick = open)
        ) {
            Row(
                Modifier.fillMaxWidth().height(MonoDeckPeek).padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(meta, style = MaterialTheme.typography.labelSmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(ordinal, style = MaterialTheme.typography.labelMedium, color = MonoDesign.textFaint, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                MonoLibraryMoreKey(menu)
            }
            Row(
                Modifier.fillMaxWidth().weight(1f).padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (covers.size <= 1) {
                    LibraryArtwork(covers.firstOrNull().orEmpty(), title, Modifier.weight(1f).fillMaxHeight(), MonoDesign.artShape, false)
                } else {
                    covers.forEach { url ->
                        LibraryArtwork(url, title, Modifier.weight(1f).fillMaxHeight(), MonoDesign.artShape, false)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MonoLibraryMoreKey(items: () -> List<MonoLibraryMenuItem>) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { open = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MoreVert, LocalLevyraStrings.current.more, tint = MonoDesign.textSecondary, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(open, { open = false }) {
            items().forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        open = false
                        item.action()
                    }
                )
            }
        }
    }
}

@Composable
internal fun MonoAlphaScrubber(
    available: BooleanArray,
    label: String,
    onLetter: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val animations = LocalAnimationsEnabled.current
    val haptics = LocalLevyraHaptics.current
    val scope = rememberCoroutineScope()
    val currentOnLetter by rememberUpdatedState(onLetter)
    val bulge = remember { Animatable(0f) }
    val touchY = remember { mutableFloatStateOf(0f) }
    val active = remember { mutableIntStateOf(-1) }
    val path = remember { Path() }
    val measurer = rememberTextMeasurer()
    val letterStyle = MaterialTheme.typography.labelSmall
    val bubbleStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = letterStyle.fontFamily)
    val letters = remember(measurer, letterStyle) { MonoScrubberLetters.map { measurer.measure(it.toString(), letterStyle) } }
    val bubbles = remember(measurer, bubbleStyle) { MonoScrubberLetters.map { measurer.measure(it.toString(), bubbleStyle) } }
    val tallest = remember(letters) { letters.maxOf { it.size.height } }
    Canvas(
        modifier
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                fun touch(y: Float) {
                    touchY.floatValue = y.coerceIn(0f, size.height.toFloat())
                    val slot = (y / size.height.coerceAtLeast(1) * MonoScrubberLetters.length).toInt().coerceIn(0, MonoScrubberLetters.length - 1)
                    if (slot != active.intValue) {
                        active.intValue = slot
                        haptics.perform(LevyraHapticAction.SeekSnap)
                        currentOnLetter(slot)
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    touch(down.position.y)
                    scope.launch { if (animations) bulge.animateTo(1f, spring(0.55f, Spring.StiffnessMedium)) else bulge.snapTo(1f) }
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        touch(change.position.y)
                    }
                    active.intValue = -1
                    scope.launch { if (animations) bulge.animateTo(0f, tween(280)) else bulge.snapTo(0f) }
                }
            }
    ) {
        val count = MonoScrubberLetters.length
        val slot = size.height / count
        val lineX = size.width - 4.dp.toPx()
        val raise = bulge.value
        val reach = 22.dp.toPx() * raise
        val sigma = 30.dp.toPx()
        val center = touchY.floatValue
        path.reset()
        val step = 3.dp.toPx()
        var y = 0f
        path.moveTo(lineX - monoBump(0f, center, sigma, reach), 0f)
        while (y < size.height) {
            y = (y + step).coerceAtMost(size.height)
            path.lineTo(lineX - monoBump(y, center, sigma, reach), y)
        }
        val lineColor = if (raise > 0.01f) MonoDesign.accent else MonoDesign.hairlineStrong
        drawPath(path, lineColor, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        val stride = if (slot < tallest) 2 else 1
        for (i in 0 until count) {
            if (i % stride != 0 && i != active.intValue) continue
            val layout = letters[i]
            val cy = slot * (i + 0.5f)
            val color = when {
                i == active.intValue -> MonoDesign.accent
                available[i] -> MonoDesign.textSecondary
                else -> MonoDesign.textFaint
            }
            drawText(layout, color, Offset(lineX - 8.dp.toPx() - monoBump(cy, center, sigma, reach) - layout.size.width, cy - layout.size.height / 2f))
        }
        val selected = active.intValue
        if (raise > 0.01f && selected >= 0) {
            val radius = 20.dp.toPx()
            val bubbleCenter = Offset(lineX - reach - 34.dp.toPx(), center.coerceIn(radius, size.height - radius))
            drawCircle(MonoDesign.accent.copy(alpha = raise.coerceIn(0f, 1f)), radius, bubbleCenter)
            val big = bubbles[selected]
            drawText(
                big,
                MonoDesign.onAccent.copy(alpha = raise.coerceIn(0f, 1f)),
                Offset(bubbleCenter.x - big.size.width / 2f, bubbleCenter.y - big.size.height / 2f)
            )
        }
    }
}

internal fun monoScrubberSlot(title: String): Int {
    val first = java.text.Normalizer.normalize(title.trim().take(1), java.text.Normalizer.Form.NFD).firstOrNull()?.uppercaseChar() ?: return 0
    val slot = MonoScrubberLetters.indexOf(first)
    return if (slot > 0) slot else 0
}

internal fun monoScrubberTarget(slot: Int, firstIndexBySlot: IntArray): Int {
    for (i in slot until firstIndexBySlot.size) if (firstIndexBySlot[i] >= 0) return firstIndexBySlot[i]
    for (i in slot downTo 0) if (firstIndexBySlot[i] >= 0) return firstIndexBySlot[i]
    return -1
}

private fun monoBump(y: Float, center: Float, sigma: Float, reach: Float): Float {
    val d = (y - center) / sigma
    return reach * exp(-d * d)
}
