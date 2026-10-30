package com.luc4n3x.levyra.ui.atelier

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.library.toLibraryTrack
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

private const val ShelfDragSpan = 0.40f
private const val ShelfSceneAnchor = 0.43f
private const val ShelfEnterMillis = 980
private const val CoverWindow = 5

private class AtelierShelfRenderer {
    val painter = AtelierSleevePainter()
    val camera = AtelierCamera()
    val slot = FloatArray(AtelierSlot.SIZE)
    val quad = FloatArray(8)
    val frontQuad = FloatArray(8)
    val rect = RectF()
    val glowPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    val shadowMask: Bitmap = atelierBakeSoftMask(128, 24f, 16f)
    val glow: Bitmap = atelierBakeGlow(128)
}

private fun blend(from: Int, to: Int, fraction: Float): Int {
    val f = fraction.coerceIn(0f, 1f)
    val r = atelierLerp((from shr 16 and 0xFF).toFloat(), (to shr 16 and 0xFF).toFloat(), f).toInt()
    val g = atelierLerp((from shr 8 and 0xFF).toFloat(), (to shr 8 and 0xFF).toFloat(), f).toInt()
    val b = atelierLerp((from and 0xFF).toFloat(), (to and 0xFF).toFloat(), f).toInt()
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

@Composable
internal fun AtelierShelfEntry(viewModel: LevyraViewModel, state: LevyraUiState, onOpenCollection: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    val items by produceState(
        initialValue = emptyList<AtelierShelfItem>(),
        state.favorites, state.recentListens, state.downloads, state.playlists, state.homeAlbums
    ) {
        value = withContext(Dispatchers.Default) {
            buildAtelierShelf(
                state.favorites + state.recentListens + state.downloads.map { with(it) { toLibraryTrack() } },
                state.playlists,
                state.homeAlbums
            )
        }
    }
    if (items.isEmpty()) {
        AtelierShelfEmpty(copy.shelfEmptyTitle, copy.shelfEmptyBody, copy.openCollection, onOpenCollection)
        return
    }
    AtelierShelfStage(items, viewModel, state, onOpenCollection)
}

@Composable
private fun AtelierShelfEmpty(title: String, body: String, collection: String, onOpenCollection: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(AtelierDesign.room).padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = AtelierDesign.text, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(body, color = AtelierDesign.textSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        AtelierCollectionPill(collection, onOpenCollection)
    }
}

@Composable
private fun AtelierShelfStage(items: List<AtelierShelfItem>, viewModel: LevyraViewModel, state: LevyraUiState, onOpenCollection: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    val animated = LocalAnimationsEnabled.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val quality = remember { atelierQualityOf(context) }
    val rich = quality == AtelierQuality.High
    val renderer = remember { AtelierShelfRenderer() }
    val disc by produceState<Bitmap?>(null) {
        value = withContext(Dispatchers.Default) { atelierBakeDisc(if (rich) 640 else 420) }
    }
    var savedKey by rememberSaveable { mutableStateOf(items.firstOrNull { it.contains(state.currentTrack) }?.key ?: "") }
    val startIndex = remember { items.indexOfFirst { it.key == savedKey }.coerceAtLeast(0) }
    val position = remember { Animatable(startIndex.toFloat()) }
    val enter = remember { Animatable(if (animated) 0f else 1f) }
    val covers = remember { mutableStateMapOf<String, Bitmap>() }
    val ambients = remember { mutableStateMapOf<String, Pair<Int, Int>>() }
    var selectedIndex by remember { mutableStateOf(startIndex) }

    LaunchedEffect(Unit) {
        if (enter.value < 1f) enter.animateTo(1f, tween(ShelfEnterMillis, easing = androidx.compose.animation.core.LinearEasing))
    }
    LaunchedEffect(items) {
        position.updateBounds(0f, (items.size - 1).coerceAtLeast(0).toFloat())
        val keep = items.indexOfFirst { it.key == savedKey }
        if (keep >= 0 && keep != position.value.roundToInt() && !position.isRunning) position.snapTo(keep.toFloat())
    }
    LaunchedEffect(items) {
        snapshotFlow { position.value.roundToInt().coerceIn(0, items.lastIndex) }.collect { index ->
            if (index != selectedIndex) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            selectedIndex = index
            savedKey = items[index].key
        }
    }
    LaunchedEffect(items, selectedIndex) {
        loadAtelierShelfCovers(context, density, rich, items, selectedIndex, covers, ambients)
    }
    DisposableEffect(Unit) { onDispose { AtelierStage.shelfQuadValid = false } }

    val settle: (Float) -> Unit = { target ->
        scope.launch {
            val clamped = target.coerceIn(0f, items.lastIndex.toFloat())
            if (animated) position.animateTo(clamped, spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessLow)) else position.snapTo(clamped)
        }
    }
    LaunchedEffect(state.selectedTab) {
        if (state.selectedTab == LevyraTab.Library) AtelierStage.shelfHandoff = false
    }
    val open: (AtelierShelfItem) -> Unit = { item -> openAtelierShelfItem(item, state, viewModel) }
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val current = items[safeIndex]
    val discProvider = { disc }

    BoxWithConstraints(Modifier.fillMaxSize().background(AtelierDesign.room)) {
        val widthPx = with(density) { maxWidth.toPx() }
        val currentForTap by androidx.compose.runtime.rememberUpdatedState(current)
        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(items) {
                    val tracker = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = { tracker.resetTracking(); scope.launch { position.stop() } },
                        onDragEnd = {
                            val velocity = tracker.calculateVelocity().x
                            val projected = position.value - velocity / (widthPx * ShelfDragSpan) * 0.28f
                            settle(projected.roundToInt().toFloat())
                        },
                        onDragCancel = { settle(position.value.roundToInt().toFloat()) }
                    ) { change, amount ->
                        tracker.addPosition(change.uptimeMillis, change.position)
                        change.consume()
                        scope.launch { position.snapTo(position.value - amount / (widthPx * ShelfDragSpan)) }
                    }
                }
                .pointerInput(items) {
                    detectAtelierShelfTaps(renderer, position, settle) { open(currentForTap) }
                }
                .semantics(mergeDescendants = false) {
                    contentDescription = "${current.title}, ${current.subtitle}"
                    role = Role.Button
                    onClick { open(current); true }
                    customActions = listOf(
                        CustomAccessibilityAction(copy.previousSleeve) { settle((safeIndex - 1).toFloat()); true },
                        CustomAccessibilityAction(copy.nextSleeve) { settle((safeIndex + 1).toFloat()); true }
                    )
                }
        ) {
            drawAtelierShelf(renderer, items, state, current, position.value, enter.value, ambients, covers, discProvider, rich)
        }

        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AtelierCollectionPill(copy.openCollection, onOpenCollection)
            Spacer(Modifier.height(10.dp))
            AnimatedContent(
                targetState = current,
                transitionSpec = { fadeIn(tween(170, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(110)) },
                contentKey = { it.key },
                label = "atelier-shelf-title"
            ) { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(item.title, color = AtelierDesign.text, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (item.tracks.size > 1) "${item.subtitle} · ${copy.tracksCount.format(item.tracks.size)}" else item.subtitle,
                        color = AtelierDesign.textSecondary, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                    )
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = atelierDockReserve(state.currentTrack != null)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AtelierShelfDial(items, safeIndex, copy.shelfPosition, { position.value }) { target -> settle(target.toFloat()) }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.heightIn(min = 48.dp).clip(CircleShape).background(Color(0x1FFFFFFF)).border(1.dp, AtelierDesign.hairline, CircleShape)
                        .levyraPressable(onClick = { open(current) }, role = Role.Button).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = AtelierDesign.accentBright, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(copy.playCollection, color = AtelierDesign.text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Color(0x14FFFFFF)).border(1.dp, AtelierDesign.hairline, CircleShape)
                        .levyraPressable(onClick = { openAtelierShelfDetails(current, viewModel) }, role = Role.Button).semantics { contentDescription = copy.openDetails },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = AtelierDesign.textSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private fun CoroutineScope.loadAtelierShelfCovers(
    context: Context, density: Density, rich: Boolean, items: List<AtelierShelfItem>, selectedIndex: Int,
    covers: SnapshotStateMap<String, Bitmap>, ambients: SnapshotStateMap<String, Pair<Int, Int>>
) {
    val sizePx = with(density) { if (rich) 380.dp.roundToPx() else 280.dp.roundToPx() }.coerceAtMost(720)
    val low = (selectedIndex - CoverWindow).coerceAtLeast(0)
    val high = (selectedIndex + CoverWindow).coerceAtMost(items.lastIndex)
    val wanted = HashSet<String>()
    for (index in low..high) wanted += items[index].coverUrl
    for (stale in covers.keys.filter { it !in wanted }) covers.remove(stale)
    val order = LinkedHashSet<Int>()
    for (step in 0..CoverWindow) {
        order += selectedIndex + step
        order += selectedIndex - step
    }
    for (index in order) {
        if (index !in low..high) continue
        val url = items[index].coverUrl
        launchAtelierShelfCoverLoad(context, url, sizePx, covers, ambients)
    }
}

private fun CoroutineScope.launchAtelierShelfCoverLoad(
    context: Context, url: String, sizePx: Int,
    covers: SnapshotStateMap<String, Bitmap>, ambients: SnapshotStateMap<String, Pair<Int, Int>>
) {
    launch {
        if (url !in covers) atelierLoadCover(context, url, sizePx, hardware = true)?.let { covers[url] = it }
        if (url !in ambients) atelierLoadAmbient(context, url)?.let { ambients[url] = it }
    }
}

private fun openAtelierShelfItem(item: AtelierShelfItem, state: LevyraUiState, viewModel: LevyraViewModel) {
    if (item.tracks.isNotEmpty()) AtelierStage.shelfHandoff = true
    when {
        item.contains(state.currentTrack) -> viewModel.selectTab(LevyraTab.Player)
        item.tracks.isNotEmpty() -> {
            viewModel.playFrom(item.tracks, item.tracks.first())
            viewModel.selectTab(LevyraTab.Player)
        }
        else -> viewModel.openAlbum(item.albumDetail())
    }
}

private fun openAtelierShelfDetails(current: AtelierShelfItem, viewModel: LevyraViewModel) {
    val playlistId = current.playlistId
    if (playlistId != null) viewModel.openPlaylist(playlistId) else viewModel.openAlbum(current.albumDetail())
}

private suspend fun PointerInputScope.detectAtelierShelfTaps(
    renderer: AtelierShelfRenderer, position: Animatable<Float, AnimationVector1D>, settle: (Float) -> Unit, onOpenFront: () -> Unit
) {
    detectTapGestures { tap: Offset ->
        when {
            atelierQuadContains(renderer.frontQuad, tap.x, tap.y) -> onOpenFront()
            tap.y < size.height * 0.62f && tap.x > renderer.frontQuad[2] -> settle((position.value.roundToInt() + 1).toFloat())
            tap.y < size.height * 0.62f && tap.x < renderer.frontQuad[0] -> settle((position.value.roundToInt() - 1).toFloat())
        }
    }
}

private fun DrawScope.drawAtelierShelf(
    r: AtelierShelfRenderer, items: List<AtelierShelfItem>, state: LevyraUiState, current: AtelierShelfItem, pos: Float, entered: Float,
    ambients: SnapshotStateMap<String, Pair<Int, Int>>, covers: SnapshotStateMap<String, Bitmap>, disc: () -> Bitmap?, rich: Boolean
) {
    val w = size.width
    val h = size.height
    val base = floor(pos).toInt().coerceIn(0, items.lastIndex)
    val nextIndex = (base + 1).coerceAtMost(items.lastIndex)
    val mix = pos - base
    val a = ambients[items[base].coverUrl]
    val b = ambients[items[nextIndex].coverUrl]
    val fallback = 0xFF2A2556.toInt()
    val primary = blend(a?.first ?: fallback, b?.first ?: fallback, mix)
    val secondary = blend(a?.second ?: fallback, b?.second ?: fallback, mix)
    drawIntoCanvas { composeCanvas ->
        val canvas = composeCanvas.nativeCanvas
        val glowIn = atelierSmooth(0f, 0.32f, entered)
        r.glowPaint.color = primary
        r.glowPaint.alpha = (150f * glowIn).toInt()
        r.rect.set(-w * 0.75f, -h * 0.16f, w * 1.1f, h * 0.62f)
        canvas.drawBitmap(r.glow, null, r.rect, r.glowPaint)
        r.glowPaint.color = secondary
        r.glowPaint.alpha = (130f * glowIn).toInt()
        r.rect.set(-w * 0.05f, h * 0.36f, w * 1.75f, h * 1.12f)
        canvas.drawBitmap(r.glow, null, r.rect, r.glowPaint)
        r.camera.frame(w, h, ShelfSceneAnchor)

        val stageClock = AtelierStage.expansion().coerceIn(0f, 1f)
        val handedOff = stageClock > 0.001f && (AtelierStage.shelfHandoff || current.contains(state.currentTrack))
        drawAtelierShelfSleeves(canvas, r, items, state, base, pos, entered, handedOff, covers, disc, rich)
    }
}

private fun drawAtelierShelfSleeves(
    canvas: android.graphics.Canvas, r: AtelierShelfRenderer, items: List<AtelierShelfItem>, state: LevyraUiState,
    base: Int, pos: Float, entered: Float, handedOff: Boolean,
    covers: SnapshotStateMap<String, Bitmap>, disc: () -> Bitmap?, rich: Boolean
) {
    val reach = 6
    val low = (base - reach).coerceAtLeast(0)
    val high = (base + reach).coerceAtMost(items.lastIndex)
    var outward = reach + 1
    while (outward >= 0) {
        for (side in 0..1) {
            val index = if (side == 0) base + 1 + outward else base - outward
            if (index !in low..high) continue
            val depth = index - pos
            AtelierSlot.fill(depth, entered, r.slot)
            val alpha = r.slot[AtelierSlot.ALPHA]
            if (alpha <= 0.004f) continue
            r.camera.project(r.slot, r.quad)
            val focus = 1f - atelierSmooth(0f, 1f, abs(depth).coerceAtMost(1f))
            if (focus > 0.5f) publishAtelierShelfFront(r, items, index, state, focus, entered, covers)
            if (handedOff && focus > 0.5f) continue
            val discOut = focus * atelierSmooth(0.74f, 1f, entered)
            r.painter.draw(
                canvas, r.quad, covers[items[index].coverUrl], disc(), discOut,
                r.shadowMask, 0.62f, depth * 0.34f - 0.1f,
                atelierSmooth(0f, 6f, abs(depth)) * 0.62f + (1f - focus) * 0.08f, alpha, rich
            )
        }
        outward--
    }
}

private fun publishAtelierShelfFront(
    r: AtelierShelfRenderer, items: List<AtelierShelfItem>, index: Int, state: LevyraUiState,
    focus: Float, entered: Float, covers: SnapshotStateMap<String, Bitmap>
) {
    r.quad.copyInto(r.frontQuad)
    r.quad.copyInto(AtelierStage.shelfQuad)
    AtelierStage.shelfCover = covers[items[index].coverUrl]
    AtelierStage.shelfQuadValid = focus > 0.92f && entered >= 1f && (AtelierStage.shelfHandoff || items[index].contains(state.currentTrack))
}

private val DialHeight = 56.dp

@Composable
private fun AtelierShelfDial(
    items: List<AtelierShelfItem>, selected: Int, label: String, position: () -> Float, onSeek: (Int) -> Unit
) {
    val marks = remember(items) { atelierShelfLetterMarks(items) }
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val density = LocalDensity.current
    var trackWidth by remember { mutableStateOf(1f) }
    val lastIndex = items.lastIndex.coerceAtLeast(1)
    Canvas(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp).height(DialHeight)
            .pointerInput(items) {
                trackWidth = size.width.toFloat()
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onSeek((change.position.x / size.width * lastIndex).roundToInt().coerceIn(0, items.lastIndex))
                }
            }
            .pointerInput(items) {
                detectTapGestures { tap -> onSeek((tap.x / size.width * lastIndex).roundToInt().coerceIn(0, items.lastIndex)) }
            }
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(selected.toFloat(), 0f..lastIndex.toFloat(), (lastIndex - 1).coerceAtLeast(0))
                setProgress { target -> onSeek(target.roundToInt().coerceIn(0, items.lastIndex)); true }
            }
    ) {
        val w = size.width
        val tickBottom = with(density) { 34.dp.toPx() }
        val stroke = with(density) { 1.dp.toPx() }
        val spots = 42
        val thumb = position() / lastIndex
        for (spot in 0..spots) {
            val fraction = spot / spots.toFloat()
            val near = (1f - abs(fraction - thumb) * 9f).coerceIn(0f, 1f)
            val tall = with(density) { (9.dp + (15.dp * near)).toPx() }
            drawLine(
                Color.White.copy(alpha = 0.16f + 0.42f * near),
                Offset(fraction * w, tickBottom - tall), Offset(fraction * w, tickBottom), stroke
            )
        }
        val thumbX = thumb * w
        drawLine(AtelierDesign.accentBright, Offset(thumbX, with(density) { 2.dp.toPx() }), Offset(thumbX, tickBottom), stroke * 1.6f)
        drawCircle(AtelierDesign.accentBright, with(density) { 2.5.dp.toPx() }, Offset(thumbX, with(density) { 2.dp.toPx() }))
        val minimumGap = with(density) { 22.dp.toPx() }
        var lastX = -minimumGap
        marks.forEach { (index, letter) ->
            val x = index / lastIndex.toFloat() * w
            if (x - lastX < minimumGap) return@forEach
            lastX = x
            val active = letter == items[selected].letter
            val layout = measurer.measure(letter.toString(), labelStyle.copy(color = if (active) AtelierDesign.text else AtelierDesign.textFaint))
            drawText(layout, topLeft = Offset(x - layout.size.width / 2f, tickBottom + with(density) { 5.dp.toPx() }))
        }
    }
}

@Composable
internal fun atelierDockReserve(hasMini: Boolean) = WindowInsets.navigationBars
    .let { insets -> with(LocalDensity.current) { insets.getBottom(this).toDp() } } + if (hasMini) 158.dp else 92.dp

@Composable
private fun AtelierCollectionPill(label: String, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp).levyraPressable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label.uppercase(), color = AtelierDesign.textSecondary, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clip(CircleShape).border(1.dp, AtelierDesign.hairline, CircleShape).padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
