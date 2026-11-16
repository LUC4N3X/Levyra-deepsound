package com.luc4n3x.levyra.ui.discovery

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import androidx.compose.foundation.layout.BoxWithConstraints
import kotlin.math.absoluteValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.feature.discovery.DiscoveryCard
import com.luc4n3x.levyra.feature.discovery.DiscoveryDecision
import com.luc4n3x.levyra.ui.LevyraIsLight
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.i18n.DiscoveryCopy

internal val DefaultDiscoveryCardShape: Shape = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp, bottomEnd = 44.dp, bottomStart = 44.dp)

@Composable
internal fun rememberDiscoveryPalette(card: DiscoveryCard?): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val fallback = remember(scheme.primary, scheme.tertiary) { ArtworkPalette(scheme.primary.toArgb(), scheme.tertiary.toArgb()) }
    val url = card?.track?.thumbnailUrl.orEmpty()
    val palette by rememberArtworkPalette(
        paletteKey = if (url.isBlank()) "" else "discover-${card?.key}-$url",
        artworkUrl = url,
        fallback = fallback,
        retainPreviousUntilLoaded = true
    )
    val fade = if (LocalAnimationsEnabled.current) 700 else 0
    val start by animateColorAsState(Color(palette.start), tween(fade), label = "discover-palette-start")
    val end by animateColorAsState(Color(palette.end), tween(fade), label = "discover-palette-end")
    return start to end
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultDiscoveryBackdrop(card: DiscoveryCard?, animationsEnabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val (start, end) = rememberDiscoveryPalette(card)
    val spin = if (animationsEnabled) {
        rememberInfiniteTransition(label = "discover-aurora").animateFloat(
            0f, 360f, infiniteRepeatable(tween(90_000, easing = LinearEasing), RepeatMode.Restart), label = "discover-aurora-spin"
        )
    } else null
    val burst = MaterialShapes.SoftBurst.toShape()
    val cookie = MaterialShapes.Cookie9Sided.toShape()
    Box(Modifier.fillMaxSize()) {
        if (card != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Crossfade(card, animationSpec = tween(if (animationsEnabled) 600 else 0), label = "discover-aurora-art") { shown ->
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (LevyraIsLight) 0.38f else 0.55f; scaleX = 1.25f; scaleY = 1.25f }.blur(90.dp)) {
                    DiscoveryArtwork(shown, Modifier.fillMaxSize(), highRes = false)
                }
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(start.copy(alpha = 0.62f), Color.Transparent), center = Offset(0f, 0f), radius = 1500f)
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(end.copy(alpha = 0.5f), Color.Transparent), center = Offset(1500f, 2300f), radius = 1600f)
            )
        )
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 120.dp, y = (-90).dp).size(440.dp)
                .graphicsLayer { rotationZ = spin?.value ?: 0f }
                .clip(burst)
                .background(Brush.linearGradient(listOf(start.copy(alpha = 0.34f), end.copy(alpha = 0.12f))))
        )
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = (-110).dp, y = 70.dp).size(360.dp)
                .graphicsLayer { rotationZ = -(spin?.value ?: 0f) * 1.4f }
                .clip(cookie)
                .background(end.copy(alpha = 0.18f))
        )
        val veil = scheme.surface
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to veil.copy(alpha = if (LevyraIsLight) 0.42f else 0.18f),
                    0.45f to veil.copy(alpha = if (LevyraIsLight) 0.30f else 0.10f),
                    0.82f to veil.copy(alpha = if (LevyraIsLight) 0.78f else 0.72f),
                    1f to veil.copy(alpha = 0.94f)
                )
            )
        )
    }
}

@Composable
internal fun Modifier.defaultDiscoveryGlow(card: DiscoveryCard, shape: Shape): Modifier {
    val (start, _) = rememberDiscoveryPalette(card)
    return this
        .shadow(36.dp, shape, ambientColor = start, spotColor = start)
        .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultDiscoveryControls(
    card: DiscoveryCard,
    canUndo: Boolean,
    playing: Boolean,
    loading: Boolean,
    morph: Shape,
    copy: DiscoveryCopy,
    onDecide: (DiscoveryDecision) -> Unit,
    onUndo: () -> Unit,
    onTogglePreview: () -> Unit,
    onPlayFull: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val (start, end) = rememberDiscoveryPalette(card)
    val heroStart = lerp(start, scheme.primary, 0.5f)
    val heroEnd = lerp(end, scheme.tertiary, 0.5f)
    val heroContent = if (lerp(heroStart, heroEnd, 0.5f).luminance() > 0.45f) Color.Black else Color.White
    BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp)) {
    val roomy = maxWidth >= DefaultDiscoveryControlsWidth
    val sideSize = if (roomy) 62.dp else 56.dp
    val heroSize = if (roomy) 80.dp else 68.dp
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (roomy) 10.dp else 2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DefaultDiscoveryGlassButton(Icons.AutoMirrored.Rounded.Undo, copy.undo, enabled = canUndo, onClick = onUndo)
        FilledTonalIconButton(
            onClick = { onDecide(DiscoveryDecision.Dismissed) },
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = scheme.errorContainer,
                contentColor = scheme.onErrorContainer
            ),
            modifier = Modifier.size(sideSize)
        ) { Icon(Icons.Rounded.Close, copy.skip, Modifier.size(30.dp)) }
        Box(
            Modifier.size(heroSize)
                .shadow(22.dp, morph, ambientColor = heroStart, spotColor = heroStart)
                .clip(morph)
                .background(Brush.linearGradient(listOf(heroStart, heroEnd))),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onTogglePreview, modifier = Modifier.fillMaxSize()) {
                when {
                    loading -> LevyraLoadingIndicator(Modifier.size(36.dp).semantics { contentDescription = copy.previewLoading }, heroContent)
                    playing -> Icon(Icons.Rounded.Pause, copy.pausePreview, tint = heroContent, modifier = Modifier.size(40.dp))
                    else -> Icon(Icons.Rounded.PlayArrow, copy.playPreview, tint = heroContent, modifier = Modifier.size(40.dp))
                }
            }
        }
        FilledIconButton(
            onClick = { onDecide(DiscoveryDecision.Saved) },
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(sideSize)
        ) { Icon(Icons.Rounded.Favorite, copy.save, Modifier.size(30.dp)) }
        DefaultDiscoveryGlassButton(Icons.Rounded.PlayCircle, copy.playFull, onClick = onPlayFull)
    }
    }
}

private val DefaultDiscoveryControlsWidth = 344.dp

@Composable
internal fun DefaultDiscoveryGlassButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = scheme.surfaceContainerHighest.copy(alpha = 0.55f),
            contentColor = scheme.onSurface
        ),
        modifier = Modifier.size(48.dp).border(1.dp, scheme.onSurface.copy(alpha = 0.10f), CircleShape)
    ) {
        if (content != null) content() else Icon(icon, label)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultDiscoveryTopBar(
    copy: DiscoveryCopy,
    savedThisWeek: Int,
    refreshing: Boolean,
    backLabel: String,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSaved: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DefaultDiscoveryGlassButton(Icons.AutoMirrored.Rounded.ArrowBack, backLabel, onClick = onDismiss)
        Column(Modifier.weight(1f)) {
            Text(
                copy.title,
                style = MaterialTheme.typography.titleLargeEmphasized.copy(fontWeight = FontWeight.Black),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (savedThisWeek > 0) {
                Text(
                    "${copy.savedThisWeek} · $savedThisWeek",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onOpenSaved).padding(vertical = 2.dp)
                )
            }
        }
        DefaultDiscoveryGlassButton(Icons.Rounded.Refresh, copy.refresh, enabled = !refreshing, onClick = onRefresh) {
            if (refreshing) LevyraLoadingIndicator(Modifier.size(22.dp)) else Icon(Icons.Rounded.Refresh, copy.refresh)
        }
    }
}

@Composable
internal fun DefaultDiscoverySwipeStamp(offset: Float, threshold: Float, copy: DiscoveryCopy) {
    val strength = (offset.absoluteValue / threshold).coerceIn(0f, 1f)
    if (strength < 0.08f) return
    val saving = offset > 0f
    val scheme = MaterialTheme.colorScheme
    val tint = if (saving) scheme.primary else scheme.error
    Box(
        Modifier.fillMaxWidth().padding(top = 40.dp, start = 28.dp, end = 28.dp),
        contentAlignment = if (saving) Alignment.TopStart else Alignment.TopEnd
    ) {
        Row(
            Modifier
                .graphicsLayer {
                    alpha = strength
                    rotationZ = if (saving) -16f else 16f
                    val grow = 0.8f + 0.25f * strength
                    scaleX = grow
                    scaleY = grow
                }
                .border(4.dp, tint, RoundedCornerShape(18.dp))
                .background(tint.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(if (saving) Icons.Rounded.Favorite else Icons.Rounded.Close, null, Modifier.size(30.dp), tint = tint)
            Text(
                (if (saving) copy.save else copy.skip).uppercase(),
                style = MaterialTheme.typography.headlineMediumEmphasized.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp),
                color = tint,
                maxLines = 1
            )
        }
    }
}
