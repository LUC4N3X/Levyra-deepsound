package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.DiscoveryDeckState
import com.luc4n3x.levyra.desktop.app.state.DiscoveryNoticeKind
import com.luc4n3x.levyra.desktop.app.state.DiscoveryPick
import com.luc4n3x.levyra.desktop.app.state.DiscoveryPickReason
import com.luc4n3x.levyra.desktop.app.state.DiscoveryPreviewStatus
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.CookieShape
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.ui.i18n.DiscoveryCopy
import com.luc4n3x.levyra.ui.i18n.discoveryCopy
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun DiscoveryDeckScreen(
    deck: DiscoveryDeckState,
    savedThisWeek: Int,
    loading: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onUndo: () -> Unit,
    onTogglePreview: () -> Unit,
    onPlayFull: () -> Unit,
    onOpenSaved: () -> Unit,
    onRefresh: () -> Unit,
    onConsumeNotice: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val code = LocalStrings.current.languageCode
    val copy = remember(code) { discoveryCopy(code) }
    val current = deck.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Box(
        modifier.fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.Escape -> onBack()
                    event.key == Key.Z && event.isCtrlPressed || event.key == Key.Backspace -> if (deck.canUndo) onUndo()
                    current == null -> return@onPreviewKeyEvent false
                    event.key == Key.DirectionRight -> onSave()
                    event.key == Key.DirectionLeft -> onDismiss()
                    event.key == Key.Spacebar -> onTogglePreview()
                    event.key == Key.Enter -> onPlayFull()
                    else -> return@onPreviewKeyEvent false
                }
                true
            }
    ) {
        DiscoveryBackdrop(current)
        Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
            DiscoveryTopBar(copy, savedThisWeek, onBack, onOpenSaved, onRefresh)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    current != null -> DiscoveryStage(deck, current, copy, onSave, onDismiss, onTogglePreview)
                    loading -> DiscoveryMessage(copy.loading, null, null, loading = true)
                    else -> DiscoveryMessage(copy.empty, copy.refresh, onRefresh, loading = false)
                }
            }
            if (current != null) {
                DiscoveryControls(deck, current, copy, onSave, onDismiss, onUndo, onTogglePreview, onPlayFull)
                Text(
                    copy.swipeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                )
            }
        }
        DiscoveryNoticeHost(deck, copy, onOpenSaved, onConsumeNotice, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp))
    }
}

@Composable
private fun DiscoveryBackdrop(current: DiscoveryPick?) {
    val surface = MaterialTheme.colorScheme.surface
    Box(Modifier.fillMaxSize().background(surface)) {
        current?.track?.artworkUrl?.takeIf(String::isNotBlank)?.let { url ->
            Artwork(url, Modifier.fillMaxSize().blur(90.dp).alpha(0.45f), cornerRadius = 0.dp)
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(surface.copy(alpha = 0.35f), surface.copy(alpha = 0.78f), surface))
            )
        )
    }
}

@Composable
private fun DiscoveryTopBar(copy: DiscoveryCopy, savedThisWeek: Int, onBack: () -> Unit, onOpenSaved: () -> Unit, onRefresh: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RoundIcon(LevyraIcons.ChevronLeft, copy.close, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface, 44.dp, onBack)
        Column(Modifier.weight(1f)) {
            Text(
                copy.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Text(copy.lead, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (savedThisWeek > 0) {
            Row(
                Modifier.heightIn(min = 40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClickLabel = copy.openSaved, role = Role.Button, onClick = onOpenSaved)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(LevyraIcons.HeartFilled, contentDescription = null, tint = LevyraBrand.pink, modifier = Modifier.size(16.dp))
                Text("${copy.savedThisWeek} · $savedThisWeek", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        RoundIcon(LevyraIcons.Refresh, copy.refresh, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface, 44.dp, onRefresh)
    }
}

@Composable
private fun DiscoveryStage(
    deck: DiscoveryDeckState,
    current: DiscoveryPick,
    copy: DiscoveryCopy,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onTogglePreview: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val cardWidth = (maxHeight * 0.66f).coerceAtMost(maxWidth * 0.8f).coerceIn(240.dp, 420.dp)
        val threshold = with(LocalDensity.current) { (cardWidth * 0.42f).toPx() }
        deck.upcoming.asReversed().forEachIndexed { reversedIndex, pick ->
            val depth = deck.upcoming.size - reversedIndex
            DiscoveryCardFace(
                pick, copy, DiscoveryPreviewStatus.Idle, cardWidth,
                Modifier.graphicsLayer {
                    val scale = 1f - depth * 0.06f
                    scaleX = scale
                    scaleY = scale
                    translationY = depth * 22.dp.toPx()
                    alpha = 1f - depth * 0.28f
                },
                onTogglePreview = null
            )
        }
        val offset = remember(current.key) { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val fling: (Boolean) -> Unit = { save ->
            scope.launch {
                offset.animateTo(if (save) threshold * 3f else -threshold * 3f, tween(220))
                if (save) onSave() else onDismiss()
            }
        }
        Box(
            Modifier.graphicsLayer {
                translationX = offset.value
                rotationZ = (offset.value / threshold) * 9f
            }.pointerInput(current.key) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            offset.value > threshold -> fling(true)
                            offset.value < -threshold -> fling(false)
                            else -> scope.launch { offset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } }
                ) { change, amount ->
                    change.consume()
                    scope.launch { offset.snapTo(offset.value + amount) }
                }
            }.semantics {
                contentDescription = copy.cardDescription(current.track.title, current.track.artist)
                customActions = listOf(
                    CustomAccessibilityAction(copy.save) { onSave(); true },
                    CustomAccessibilityAction(copy.skip) { onDismiss(); true }
                )
            }
        ) {
            DiscoveryCardFace(current, copy, deck.previewStatusFor(current), cardWidth, Modifier, onTogglePreview)
            DiscoveryStamp(offset.value, threshold, copy, Modifier.align(Alignment.TopCenter).padding(top = 28.dp))
        }
    }
}

@Composable
private fun DiscoveryCardFace(
    pick: DiscoveryPick,
    copy: DiscoveryCopy,
    preview: DiscoveryPreviewStatus,
    width: Dp,
    modifier: Modifier,
    onTogglePreview: (() -> Unit)?
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.widthIn(max = width).clip(RoundedCornerShape(32.dp)).background(colors.surfaceContainerHigh).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Artwork(pick.track.artworkUrl, Modifier.fillMaxSize(), cornerRadius = 22.dp, iconSize = 56.dp)
            if (onTogglePreview != null) {
                Box(Modifier.align(Alignment.BottomEnd).padding(14.dp)) {
                    PreviewBadge(preview, copy, onTogglePreview)
                }
            }
        }
        Column(Modifier.padding(horizontal = 6.dp).padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(pick.track.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(pick.track.artist, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                discoveryReason(pick, copy),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp).clip(CircleShape).background(colors.secondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun PreviewBadge(status: DiscoveryPreviewStatus, copy: DiscoveryCopy, onToggle: () -> Unit) {
    val label = when (status) {
        DiscoveryPreviewStatus.Idle -> copy.playPreview
        DiscoveryPreviewStatus.Loading -> copy.previewLoading
        DiscoveryPreviewStatus.Playing -> copy.pausePreview
    }
    Box(
        Modifier.size(52.dp).clip(CookieShape).background(LevyraBrand.soft)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onToggle)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            DiscoveryPreviewStatus.Loading -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = LevyraBrand.onSoft)
            DiscoveryPreviewStatus.Playing -> Icon(LevyraIcons.Pause, contentDescription = null, tint = LevyraBrand.onSoft, modifier = Modifier.size(24.dp))
            DiscoveryPreviewStatus.Idle -> Icon(LevyraIcons.Play, contentDescription = null, tint = LevyraBrand.onSoft, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun DiscoveryStamp(offset: Float, threshold: Float, copy: DiscoveryCopy, modifier: Modifier) {
    val progress = (abs(offset) / threshold).coerceIn(0f, 1f)
    if (progress <= 0.05f) return
    val save = offset > 0
    val tint = if (save) LevyraBrand.soft else MaterialTheme.colorScheme.error
    Text(
        (if (save) copy.save else copy.skip).uppercase(),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black,
        color = tint,
        modifier = modifier.alpha(progress).graphicsLayer { rotationZ = if (save) -10f else 10f }
            .clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun DiscoveryControls(
    deck: DiscoveryDeckState,
    current: DiscoveryPick,
    copy: DiscoveryCopy,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onUndo: () -> Unit,
    onTogglePreview: () -> Unit,
    onPlayFull: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val preview = deck.previewStatusFor(current)
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.alpha(if (deck.canUndo) 1f else 0.38f)) {
            TextPill(copy.undo, colors.surfaceContainerHigh, colors.onSurface, enabled = deck.canUndo, onClick = onUndo)
        }
        RoundIcon(LevyraIcons.Close, copy.skip, colors.surfaceContainerHighest, colors.onSurface, 64.dp, onDismiss)
        RoundIcon(
            if (preview == DiscoveryPreviewStatus.Idle) LevyraIcons.Play else LevyraIcons.Pause,
            if (preview == DiscoveryPreviewStatus.Idle) copy.playPreview else copy.pausePreview,
            colors.secondaryContainer, colors.onSecondaryContainer, 52.dp, onTogglePreview
        )
        RoundIcon(LevyraIcons.HeartFilled, copy.save, LevyraBrand.soft, LevyraBrand.onSoft, 72.dp, onSave)
        TextPill(copy.playFull, colors.primaryContainer, colors.onPrimaryContainer, enabled = true, onClick = onPlayFull)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, label: String, container: Color, content: Color, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(container)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.38f))
    }
}

@Composable
private fun TextPill(label: String, container: Color, content: Color, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 220.dp).heightIn(min = 44.dp).clip(CircleShape).background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    )
}

@Composable
private fun DiscoveryMessage(message: String, action: String?, onAction: (() -> Unit)?, loading: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.widthIn(max = 420.dp)) {
        if (loading) CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp, color = LevyraBrand.soft)
        Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null && onAction != null) {
            TextPill(action, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, enabled = true, onClick = onAction)
        }
    }
}

@Composable
private fun DiscoveryNoticeHost(
    deck: DiscoveryDeckState,
    copy: DiscoveryCopy,
    onOpenSaved: () -> Unit,
    onConsume: (Long) -> Unit,
    modifier: Modifier
) {
    val notice = deck.notice ?: return
    LaunchedEffect(notice.id) {
        delay(NOTICE_MS)
        onConsume(notice.id)
    }
    val message = when (notice.kind) {
        DiscoveryNoticeKind.Saved -> copy.savedMessage
        DiscoveryNoticeKind.SaveFailed -> copy.saveFailed
        DiscoveryNoticeKind.PreviewUnavailable -> copy.previewUnavailable
    }
    Row(
        modifier.widthIn(max = 560.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.inverseSurface)
            .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(message, color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false))
        if (notice.kind == DiscoveryNoticeKind.Saved) {
            Text(
                copy.openSaved,
                color = MaterialTheme.colorScheme.inversePrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onOpenSaved).padding(horizontal = 12.dp, vertical = 10.dp)
            )
        } else {
            Spacer(Modifier.height(36.dp))
        }
    }
}

private fun discoveryReason(pick: DiscoveryPick, copy: DiscoveryCopy): String = when (pick.reason) {
    DiscoveryPickReason.NewRelease -> copy.reasonNewRelease
    DiscoveryPickReason.Editorial -> copy.reasonEditorial
    DiscoveryPickReason.SimilarTo -> pick.seed?.let { seed -> copy.reasonSimilarTo(seed.title, seed.artist) } ?: copy.reasonSimilarArtist
}

private const val NOTICE_MS = 3_500L
