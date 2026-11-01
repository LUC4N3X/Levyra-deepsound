package com.luc4n3x.levyra.ui.discovery

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.feature.discovery.BubbleLevel
import com.luc4n3x.levyra.feature.discovery.DiscoveryCard
import com.luc4n3x.levyra.feature.discovery.DiscoveryLens
import com.luc4n3x.levyra.feature.discovery.DiscoveryRequest
import com.luc4n3x.levyra.feature.discovery.DiscoveryDecision
import com.luc4n3x.levyra.feature.discovery.DiscoveryPreviewStatus
import com.luc4n3x.levyra.ui.LevyraIsLight
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import com.luc4n3x.levyra.ui.homeCanvasColor
import com.luc4n3x.levyra.ui.i18n.DiscoveryCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.SmartMusicCopy
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import com.luc4n3x.levyra.ui.i18n.discoveryCopy
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.viewmodel.DiscoveryFeedStatus
import com.luc4n3x.levyra.viewmodel.DiscoveryMessageKind
import com.luc4n3x.levyra.viewmodel.DiscoveryUiState
import com.luc4n3x.levyra.viewmodel.DiscoveryViewModel
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class DiscoveryBackdrop { Wash, Burst, Flat, Paper, Bleed, Tonal }

private class DiscoveryLook(
    val cardShape: Shape,
    val artworkShape: Shape,
    val tilt: Float,
    val titleStyle: TextStyle,
    val headerStyle: TextStyle,
    val serif: Boolean,
    val backdrop: DiscoveryBackdrop,
    val artworkBleed: Boolean,
    val stackGap: Dp
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun rememberDiscoveryLook(style: HomeStyle): DiscoveryLook {
    val type = MaterialTheme.typography
    val cookie = MaterialShapes.Cookie12Sided.toShape()
    val square = MaterialShapes.Cookie4Sided.toShape()
    return remember(style, type, cookie, square) {
        when (style) {
            HomeStyle.Original -> DiscoveryLook(
                RoundedCornerShape(topStart = 36.dp, topEnd = 20.dp, bottomEnd = 36.dp, bottomStart = 20.dp),
                RoundedCornerShape(28.dp), 10f, type.titleLarge.copy(fontWeight = FontWeight.Bold), type.headlineSmall.copy(fontWeight = FontWeight.Bold),
                false, DiscoveryBackdrop.Wash, false, 14.dp
            )
            HomeStyle.ExpressiveMax -> DiscoveryLook(
                RoundedCornerShape(topStart = 48.dp, topEnd = 16.dp, bottomEnd = 48.dp, bottomStart = 16.dp),
                cookie, 14f, type.headlineSmallEmphasized.copy(fontWeight = FontWeight.Black), type.displaySmallEmphasized.copy(fontWeight = FontWeight.Black),
                false, DiscoveryBackdrop.Burst, false, 18.dp
            )
            HomeStyle.Flow -> DiscoveryLook(
                RoundedCornerShape(topStart = 28.dp, topEnd = 8.dp, bottomEnd = 28.dp, bottomStart = 8.dp),
                RoundedCornerShape(topStart = 22.dp, topEnd = 6.dp, bottomEnd = 22.dp, bottomStart = 6.dp), 8f,
                type.titleLarge.copy(fontWeight = FontWeight.SemiBold), type.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                false, DiscoveryBackdrop.Flat, false, 10.dp
            )
            HomeStyle.Editorial -> DiscoveryLook(
                RoundedCornerShape(4.dp), RoundedCornerShape(topStart = 4.dp, topEnd = 32.dp, bottomEnd = 4.dp, bottomStart = 4.dp), 4f,
                type.headlineSmall.copy(fontFamily = FontFamily.Serif), type.headlineMedium.copy(fontFamily = FontFamily.Serif),
                true, DiscoveryBackdrop.Paper, false, 10.dp
            )
            HomeStyle.Minimal -> DiscoveryLook(
                RoundedCornerShape(8.dp), RoundedCornerShape(8.dp), 0f, type.titleLarge.copy(fontWeight = FontWeight.Medium),
                type.titleLarge.copy(fontWeight = FontWeight.Medium), false, DiscoveryBackdrop.Flat, false, 8.dp
            )
            HomeStyle.Immersive -> DiscoveryLook(
                RoundedCornerShape(32.dp), RoundedCornerShape(32.dp), 10f, type.headlineSmall.copy(fontWeight = FontWeight.Black),
                type.headlineSmall.copy(fontWeight = FontWeight.Black), false, DiscoveryBackdrop.Bleed, true, 14.dp
            )
            HomeStyle.MaterialExpressive -> DiscoveryLook(
                RoundedCornerShape(28.dp), square, 9f, type.titleLargeEmphasized, type.headlineMediumEmphasized,
                false, DiscoveryBackdrop.Tonal, false, 12.dp
            )
        }
    }
}

@Composable
internal fun DiscoveryScreen(
    viewModel: DiscoveryViewModel,
    style: HomeStyle,
    animationsEnabled: Boolean,
    bottomInset: Dp,
    backEnabled: Boolean,
    onDismiss: () -> Unit,
    onOpenSaved: () -> Unit
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { discoveryCopy(strings.code) }
    val music = remember(strings.code) { smartMusicCopy(strings.code) }
    val look = rememberDiscoveryLook(style)
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    BackHandler(enabled = backEnabled, onBack = onDismiss)
    DisposableEffect(viewModel) {
        viewModel.openSession()
        onDispose { if (activity?.isChangingConfigurations != true) viewModel.closeSession() }
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) viewModel.onBackground() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val controller = remember { DiscoveryStackController() }
    val scope = rememberCoroutineScope()
    val decide: (DiscoveryDecision) -> Unit = { decision ->
        scope.launch {
            controller.flingOut(decision, animationsEnabled)
            if (decision == DiscoveryDecision.Saved) viewModel.save() else viewModel.dismiss()
            controller.reset()
        }
    }
    val undo: () -> Unit = {
        val last = ui.deck.history.lastOrNull()
        viewModel.undo()
        if (last != null) scope.launch { controller.enterFrom(last.decision, animationsEnabled) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(homeCanvasColor(LevyraIsLight))
            .pointerInput(Unit) {}
    ) {
        DiscoveryBackdropLayer(look, ui.deck.current, animationsEnabled)
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = bottomInset)) {
            DiscoveryTopBar(copy, look, ui, strings.back, onDismiss, viewModel::refresh, onOpenSaved)
            BubbleOrbitSelector(
                request = ui.request,
                scenes = ui.scenes,
                animated = animationsEnabled,
                copy = music,
                ink = if (look.backdrop == DiscoveryBackdrop.Bleed) Color.White else LevyraText,
                onLevel = viewModel::setLevel,
                onLens = viewModel::setLens,
                onLoadScenes = viewModel::loadScenes,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                when {
                    ui.deck.current != null -> DiscoveryCardStack(ui, look, copy, music, animationsEnabled, controller, decide, viewModel::togglePreview)
                    ui.status == DiscoveryFeedStatus.Loading || ui.status == DiscoveryFeedStatus.Idle -> DiscoveryStatePanel(copy.loading, null, null, loading = true)
                    ui.status == DiscoveryFeedStatus.Error -> DiscoveryStatePanel(copy.error, copy.retry, viewModel::retry, loading = false)
                    else -> DiscoveryStatePanel(copy.empty, copy.refresh, viewModel::refresh, loading = false)
                }
            }
            if (ui.deck.current != null) {
                DiscoveryControls(ui, look, copy, animationsEnabled, decide, undo, viewModel::togglePreview, viewModel::playFull)
            }
        }
        DiscoveryMessageHost(ui, copy, bottomInset, viewModel::consumeMessage)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiscoveryBackdropLayer(look: DiscoveryLook, card: DiscoveryCard?, animationsEnabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val accent = discoveryAccent(card)
    when (look.backdrop) {
        DiscoveryBackdrop.Wash -> Box(
            Modifier.fillMaxWidth().height(520.dp).background(
                Brush.verticalGradient(0f to accent.copy(alpha = if (LevyraIsLight) 0.28f else 0.42f), 0.6f to accent.copy(alpha = 0.08f), 1f to Color.Transparent)
            )
        )
        DiscoveryBackdrop.Burst -> {
            val spin = if (animationsEnabled) {
                rememberInfiniteTransition(label = "discovery-burst").animateFloat(
                    0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "discovery-burst-value"
                ).value
            } else 0f
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(accent.copy(alpha = 0.32f), Color.Transparent), radius = 1400f)))
            Box(
                Modifier.offset(x = 180.dp, y = (-60).dp).size(420.dp).graphicsLayer { rotationZ = spin }
                    .clip(MaterialShapes.SoftBurst.toShape()).background(scheme.tertiaryContainer.copy(alpha = 0.35f))
            )
        }
        DiscoveryBackdrop.Flat -> Unit
        DiscoveryBackdrop.Paper -> Box(Modifier.fillMaxSize().background(scheme.surface))
        DiscoveryBackdrop.Bleed -> if (card != null) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = 0.32f }) { DiscoveryArtwork(card, Modifier.fillMaxSize(), highRes = false) }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.88f)))))
        }
        DiscoveryBackdrop.Tonal -> Box(
            Modifier.fillMaxWidth().height(420.dp).background(
                Brush.verticalGradient(listOf(lerp(scheme.surfaceContainer, scheme.primaryContainer, 0.5f), Color.Transparent))
            )
        )
    }
}

@Composable
private fun DiscoveryTopBar(
    copy: DiscoveryCopy,
    look: DiscoveryLook,
    ui: DiscoveryUiState,
    backLabel: String,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSaved: () -> Unit
) {
    val ink = if (look.backdrop == DiscoveryBackdrop.Bleed) Color.White else LevyraText
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LevyraBackButton(onClick = onDismiss, contentDescription = backLabel)
            Spacer(Modifier.weight(1f))
            if (ui.savedThisWeek > 0) {
                TextButton(onClick = onOpenSaved) {
                    Icon(Icons.Rounded.LibraryMusic, null, Modifier.size(18.dp), tint = ink)
                    Spacer(Modifier.width(6.dp))
                    Text("${copy.savedThisWeek} · ${ui.savedThisWeek}", color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onRefresh, enabled = !ui.refreshing) {
                if (ui.refreshing) LevyraLoadingIndicator(Modifier.size(22.dp))
                else Icon(Icons.Rounded.Refresh, copy.refresh, tint = ink)
            }
        }
        Text(
            copy.title,
            style = look.headerStyle,
            color = ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp).semantics { heading() }
        )
        Text(copy.swipeHint, style = MaterialTheme.typography.bodySmall, color = if (look.backdrop == DiscoveryBackdrop.Bleed) Color.White.copy(alpha = 0.72f) else LevyraMuted, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
    }
}

private val DiscoveryCardChrome = 132.dp
private val DiscoveryDistanceChrome = 36.dp

internal class DiscoveryStackController {
    val offset = Animatable(0f)
    var width = 1f
    var nudged = false

    suspend fun flingOut(decision: DiscoveryDecision, animated: Boolean) {
        val target = if (decision == DiscoveryDecision.Saved) width * 1.4f else -width * 1.4f
        if (animated) offset.animateTo(target, tween(220)) else offset.snapTo(target)
    }

    suspend fun reset() = offset.snapTo(0f)

    suspend fun nudge() {
        val reach = width * 0.24f
        offset.animateTo(reach, tween(420))
        offset.animateTo(-reach, tween(640))
        offset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    suspend fun enterFrom(decision: DiscoveryDecision, animated: Boolean) {
        if (!animated) {
            offset.snapTo(0f)
            return
        }
        offset.snapTo(if (decision == DiscoveryDecision.Saved) width * 1.2f else -width * 1.2f)
        offset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
    }
}

@Composable
private fun DiscoveryCardStack(
    ui: DiscoveryUiState,
    look: DiscoveryLook,
    copy: DiscoveryCopy,
    music: SmartMusicCopy,
    animationsEnabled: Boolean,
    controller: DiscoveryStackController,
    onDecide: (DiscoveryDecision) -> Unit,
    onTogglePreview: () -> Unit
) {
    val current = ui.deck.current ?: return
    val upcoming = ui.deck.upcoming
    val decide by rememberUpdatedState(onDecide)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val widthPx = with(density) { maxWidth.toPx() }
        val chrome = DiscoveryCardChrome + if (showsDistance(ui.request)) DiscoveryDistanceChrome else 0.dp
        val artSide = minOf(maxWidth - 32.dp, maxHeight - chrome - look.stackGap * 2).coerceAtLeast(96.dp)
        controller.width = widthPx
        val threshold = widthPx * 0.3f
        LaunchedEffect(controller, animationsEnabled) {
            if (animationsEnabled && !controller.nudged) {
                controller.nudged = true
                delay(650L)
                controller.nudge()
            }
        }
        val progress = (controller.offset.value.absoluteValue / threshold).coerceIn(0f, 1f)
        upcoming.take(2).asReversed().forEachIndexed { reversedIndex, card ->
            val depth = upcoming.take(2).size - reversedIndex
            val baseScale = 1f - 0.06f * depth
            val scale = baseScale + 0.06f * progress * if (depth == 1) 1f else 0.5f
            DiscoveryCardFace(
                card, look, copy, music, ui.request, artSide, preview = null,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = with(density) { (look.stackGap * depth * 1.6f).toPx() } * (1f - progress * 0.6f)
                        rotationZ = if (animationsEnabled) (if (depth == 1) 3f else -3f) * (1f - progress) else 0f
                        alpha = 0.6f + 0.4f * (1f - depth * 0.25f) + 0.2f * progress
                    }
            )
        }
        val label = copy.cardDescription(current.track.title, current.track.artist)
        DiscoveryCardFace(
            current, look, copy, music, ui.request, artSide, preview = ui.preview.takeIf { it.isFor(current.key) }?.status,
            onTogglePreview = onTogglePreview,
            modifier = Modifier
                .graphicsLayer {
                    translationX = controller.offset.value
                    rotationZ = if (animationsEnabled) look.tilt * (controller.offset.value / widthPx) else 0f
                }
                .semantics {
                    contentDescription = label + ". " + discoveryReasonLabel(current, copy, music)
                    customActions = listOf(
                        CustomAccessibilityAction(copy.save) { decide(DiscoveryDecision.Saved); true },
                        CustomAccessibilityAction(copy.skip) { decide(DiscoveryDecision.Dismissed); true }
                    )
                }
                .pointerInput(current.key) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val value = controller.offset.value
                            when {
                                value > threshold -> decide(DiscoveryDecision.Saved)
                                value < -threshold -> decide(DiscoveryDecision.Dismissed)
                                else -> scope.launch { controller.offset.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                            }
                        },
                        onDragCancel = { scope.launch { controller.offset.animateTo(0f) } }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch { controller.offset.snapTo(controller.offset.value + dragAmount) }
                    }
                }
        )
        DiscoverySwipeStamp(controller.offset.value, threshold, copy)
    }
}

@Composable
private fun DiscoverySwipeStamp(offset: Float, threshold: Float, copy: DiscoveryCopy) {
    val strength = (offset.absoluteValue / threshold).coerceIn(0f, 1f)
    if (strength < 0.15f) return
    val saving = offset > 0f
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = if (saving) Alignment.TopStart else Alignment.TopEnd) {
        Surface(
            color = if (saving) scheme.primary else scheme.errorContainer,
            contentColor = if (saving) scheme.onPrimary else scheme.onErrorContainer,
            shape = CircleShape,
            modifier = Modifier.padding(horizontal = 18.dp).graphicsLayer { alpha = strength; rotationZ = if (saving) -12f else 12f }
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (saving) Icons.Rounded.Favorite else Icons.Rounded.Close, null, Modifier.size(18.dp))
                Text(if (saving) copy.save else copy.skip, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun DiscoveryCardFace(
    card: DiscoveryCard,
    look: DiscoveryLook,
    copy: DiscoveryCopy,
    music: SmartMusicCopy,
    request: DiscoveryRequest,
    artSide: Dp,
    preview: DiscoveryPreviewStatus?,
    modifier: Modifier = Modifier,
    onTogglePreview: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val accent = discoveryAccent(card)
    if (look.artworkBleed) {
        Box(modifier.fillMaxWidth().height(artSide + DiscoveryCardChrome).clip(look.cardShape).background(scheme.surfaceContainerHigh)) {
            DiscoveryArtwork(card, Modifier.fillMaxSize(), highRes = true)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.86f))))
            DiscoveryReasonChip(card, copy, music, Color.Black.copy(alpha = 0.45f), Color.White, Modifier.align(Alignment.TopStart).padding(16.dp))
            if (showsDistance(request)) BubbleDistanceBadge(card.distance, music, Color.Black.copy(alpha = 0.45f), Color.White, Modifier.align(Alignment.TopEnd).padding(16.dp))
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(card.track.title, style = look.titleStyle, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(card.track.artist, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.82f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                DiscoveryPreviewBadge(preview, copy, Color.White, onTogglePreview)
            }
        }
        return
    }
    val container = when (look.backdrop) {
        DiscoveryBackdrop.Paper -> scheme.surfaceContainerLow
        DiscoveryBackdrop.Tonal -> lerp(scheme.surfaceContainerHigh, scheme.primaryContainer, 0.35f)
        DiscoveryBackdrop.Flat -> scheme.surfaceContainer
        else -> lerp(scheme.surfaceContainer, accent, 0.14f)
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(look.cardShape)
            .background(container)
            .border(1.dp, scheme.outlineVariant.copy(alpha = if (look.backdrop == DiscoveryBackdrop.Paper) 0.7f else 0.35f), look.cardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(artSide)) {
            DiscoveryArtwork(card, Modifier.fillMaxSize().clip(look.artworkShape), highRes = true)
            DiscoveryReasonChip(card, copy, music, Color.Black.copy(alpha = 0.5f), Color.White, Modifier.align(Alignment.TopStart).padding(12.dp))
        }
        Text(card.track.title, style = look.titleStyle, color = LevyraText, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(card.track.artist, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = if (look.serif) FontFamily.Serif else null), color = LevyraMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val album = card.track.album.takeUnless { discoveryTitleKey(it) == discoveryTitleKey(card.track.title) }.orEmpty()
                val meta = listOf(album, card.track.year).filter(String::isNotBlank).joinToString(" · ")
                if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = LevyraMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (showsDistance(request)) {
                    BubbleDistanceBadge(
                        card.distance,
                        music,
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer,
                        Modifier.padding(top = 4.dp)
                    )
                }
            }
            DiscoveryPreviewBadge(preview, copy, MaterialTheme.colorScheme.primary, onTogglePreview)
        }
    }
}

@Composable
private fun showsDistance(request: DiscoveryRequest): Boolean = request.level != BubbleLevel.Near || request.lens != DiscoveryLens.Open

@Composable
private fun DiscoveryReasonChip(card: DiscoveryCard, copy: DiscoveryCopy, music: SmartMusicCopy, container: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(14.dp))
            Text(discoveryReasonLabel(card, copy, music), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DiscoveryPreviewBadge(status: DiscoveryPreviewStatus?, copy: DiscoveryCopy, tint: Color, onToggle: (() -> Unit)?) {
    if (onToggle == null) return
    val playing = status == DiscoveryPreviewStatus.Playing
    val loading = status == DiscoveryPreviewStatus.Loading
    IconButton(onClick = onToggle, modifier = Modifier.size(48.dp)) {
        when {
            loading -> LevyraLoadingIndicator(Modifier.size(24.dp).semantics { contentDescription = copy.previewLoading }, tint)
            playing -> Icon(Icons.Rounded.GraphicEq, copy.pausePreview, tint = tint)
            else -> Icon(Icons.Rounded.PlayCircle, copy.playPreview, tint = tint)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiscoveryControls(
    ui: DiscoveryUiState,
    look: DiscoveryLook,
    copy: DiscoveryCopy,
    animationsEnabled: Boolean,
    onDecide: (DiscoveryDecision) -> Unit,
    onUndo: () -> Unit,
    onTogglePreview: () -> Unit,
    onPlayFull: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val current = ui.deck.current ?: return
    val previewStatus = ui.preview.takeIf { it.isFor(current.key) }?.status
    val playing = previewStatus == DiscoveryPreviewStatus.Playing
    val loading = previewStatus == DiscoveryPreviewStatus.Loading
    val morph = rememberLevyraPlayMorphShape(playing = playing, animated = animationsEnabled)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DiscoverySwipeGuide(copy, look)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            val minimal = look.backdrop == DiscoveryBackdrop.Flat && look.tilt == 0f
            if (minimal || look.serif) {
                OutlinedIconButton(onClick = { onDecide(DiscoveryDecision.Dismissed) }, modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.Close, copy.skip) }
                OutlinedIconButton(onClick = onUndo, enabled = ui.deck.canUndo, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Rounded.Undo, copy.undo) }
            } else {
                FilledTonalIconButton(onClick = { onDecide(DiscoveryDecision.Dismissed) }, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(60.dp)) {
                    Icon(Icons.Rounded.Close, copy.skip, Modifier.size(28.dp))
                }
                FilledTonalIconButton(onClick = onUndo, enabled = ui.deck.canUndo, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, copy.undo)
                }
            }
            Box(
                Modifier.size(76.dp).clip(if (minimal || look.serif) CircleShape else morph).background(scheme.primary),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onTogglePreview, modifier = Modifier.fillMaxSize()) {
                    when {
                        loading -> LevyraLoadingIndicator(Modifier.size(32.dp).semantics { contentDescription = copy.previewLoading }, scheme.onPrimary)
                        playing -> Icon(Icons.Rounded.Pause, copy.pausePreview, tint = scheme.onPrimary, modifier = Modifier.size(36.dp))
                        else -> Icon(Icons.Rounded.PlayArrow, copy.playPreview, tint = scheme.onPrimary, modifier = Modifier.size(36.dp))
                    }
                }
            }
            if (minimal || look.serif) {
                OutlinedIconButton(onClick = { onDecide(DiscoveryDecision.Saved) }, modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.Favorite, copy.save) }
            } else {
                FilledIconButton(onClick = { onDecide(DiscoveryDecision.Saved) }, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(60.dp)) {
                    Icon(Icons.Rounded.Favorite, copy.save, Modifier.size(28.dp))
                }
            }
        }
        if (look.serif) {
            OutlinedButton(onClick = onPlayFull, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(copy.playFull, fontFamily = FontFamily.Serif)
            }
        } else {
            FilledTonalButton(onClick = onPlayFull, shapes = ButtonDefaults.shapes(), modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.PlayCircle, null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(copy.playFull)
            }
        }
    }
}

@Composable
private fun DiscoverySwipeGuide(copy: DiscoveryCopy, look: DiscoveryLook) {
    val tint = if (look.backdrop == DiscoveryBackdrop.Bleed) Color.White.copy(alpha = 0.8f) else LevyraMuted
    val style = MaterialTheme.typography.labelLarge.copy(fontFamily = if (look.serif) FontFamily.Serif else null)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp).semantics { contentDescription = copy.swipeHint },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(18.dp), tint = tint)
        Spacer(Modifier.width(6.dp))
        Text(copy.skip, style = style, color = tint, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Rounded.Swipe, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.weight(1f))
        Text(copy.save, style = style, color = tint, maxLines = 1)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp), tint = tint)
    }
}

@Composable
private fun DiscoveryStatePanel(message: String, action: String?, onAction: (() -> Unit)?, loading: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(24.dp)) {
        if (loading) LevyraLoadingIndicator(Modifier.size(56.dp))
        else Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = LevyraText, textAlign = TextAlign.Center, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (action != null && onAction != null) {
            FilledTonalButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun DiscoveryMessageHost(ui: DiscoveryUiState, copy: DiscoveryCopy, bottomInset: Dp, onConsume: (Long) -> Unit) {
    val message = ui.message ?: return
    LaunchedEffect(message.id) {
        delay(2_400L)
        onConsume(message.id)
    }
    val text = when (message.kind) {
        DiscoveryMessageKind.Saved -> copy.savedMessage
        DiscoveryMessageKind.SaveFailed -> copy.saveFailed
        DiscoveryMessageKind.PreviewUnavailable -> copy.previewUnavailable
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 64.dp), contentAlignment = Alignment.TopCenter) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(horizontal = 24.dp).semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        }
    }
}

private fun discoveryTitleKey(value: String): String = com.luc4n3x.levyra.viewmodel.playbackTitleKey(value)
