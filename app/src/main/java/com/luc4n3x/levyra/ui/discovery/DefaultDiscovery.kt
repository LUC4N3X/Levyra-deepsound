package com.luc4n3x.levyra.ui.discovery

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.feature.discovery.DiscoveryCard
import com.luc4n3x.levyra.feature.discovery.DiscoveryDecision
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.i18n.DiscoveryCopy
import kotlin.math.absoluteValue

internal val DefaultDiscoveryCardShape: Shape = RoundedCornerShape(24.dp)
internal val DefaultDiscoveryPanel = 100.dp
internal val DefaultDiscoveryPeek = 9.dp

@Composable
internal fun rememberDiscoveryPalette(card: DiscoveryCard?, animate: Boolean = true): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val fallback = remember(scheme.primary, scheme.tertiary) { ArtworkPalette(scheme.primary.toArgb(), scheme.tertiary.toArgb()) }
    val url = card?.track?.thumbnailUrl.orEmpty()
    val palette by rememberArtworkPalette(
        paletteKey = if (url.isBlank()) "" else "discover-${card?.key}-$url",
        artworkUrl = url,
        fallback = fallback,
        retainPreviousUntilLoaded = true
    )
    val fade = if (animate && LocalAnimationsEnabled.current) 700 else 0
    val start by animateColorAsState(Color(palette.start), tween(fade), label = "discover-palette-start")
    val end by animateColorAsState(Color(palette.end), tween(fade), label = "discover-palette-end")
    return start to end
}

@Composable
internal fun DefaultDiscoveryBackdrop(card: DiscoveryCard?, next: DiscoveryCard?, blend: () -> Float) {
    val surface = MaterialTheme.colorScheme.surface
    val (tone, _) = rememberDiscoveryPalette(card, animate = false)
    val (nextTone, _) = rememberDiscoveryPalette(next, animate = false)
    val target = if (next == null) tone else nextTone
    Box(
        Modifier.fillMaxSize().drawWithCache {
            val current = defaultDiscoveryWash(tone, surface)
            val upcoming = defaultDiscoveryWash(target, surface)
            onDrawBehind {
                drawRect(current)
                val mix = blend()
                if (mix > 0f) drawRect(upcoming, alpha = mix)
            }
        }
    )
}

private fun defaultDiscoveryWash(tone: Color, surface: Color): Brush = Brush.verticalGradient(
    0f to lerp(tone, surface, 0.42f),
    0.55f to lerp(tone, surface, 0.58f),
    1f to lerp(tone, surface, 0.9f)
)

@Composable
internal fun defaultDiscoveryPanelHeight(): Dp = DefaultDiscoveryPanel * LocalDensity.current.fontScale.coerceAtLeast(1f)

@Composable
internal fun Modifier.defaultDiscoveryCardShadow(shape: Shape): Modifier =
    shadow(14.dp, shape, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.6f))

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultDiscoveryControls(
    playing: Boolean,
    loading: Boolean,
    morph: Shape,
    copy: DiscoveryCopy,
    onDecide: (DiscoveryDecision) -> Unit,
    onTogglePreview: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val previewLabel = if (playing) copy.pausePreview else copy.playPreview
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 18.dp),
        verticalAlignment = Alignment.Top
    ) {
        DefaultDiscoveryAction(copy.skip, Modifier.weight(1f)) {
            FilledTonalIconButton(
                onClick = { onDecide(DiscoveryDecision.Dismissed) },
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = scheme.surfaceContainerHigh,
                    contentColor = scheme.error
                ),
                modifier = Modifier.size(DefaultDiscoverySideAction)
            ) { Icon(Icons.Rounded.Close, copy.skip, Modifier.size(30.dp)) }
        }
        DefaultDiscoveryAction(previewLabel, Modifier.weight(1f)) {
            Box(
                Modifier.size(DefaultDiscoveryHeroAction).clip(morph).background(scheme.inverseSurface),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onTogglePreview, modifier = Modifier.fillMaxSize()) {
                    when {
                        loading -> LevyraLoadingIndicator(Modifier.size(34.dp).semantics { contentDescription = copy.previewLoading }, scheme.inverseOnSurface)
                        playing -> Icon(Icons.Rounded.Pause, copy.pausePreview, tint = scheme.inverseOnSurface, modifier = Modifier.size(38.dp))
                        else -> Icon(Icons.Rounded.PlayArrow, copy.playPreview, tint = scheme.inverseOnSurface, modifier = Modifier.size(38.dp))
                    }
                }
            }
        }
        DefaultDiscoveryAction(copy.save, Modifier.weight(1f)) {
            FilledTonalIconButton(
                onClick = { onDecide(DiscoveryDecision.Saved) },
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = scheme.surfaceContainerHigh,
                    contentColor = scheme.primary
                ),
                modifier = Modifier.size(DefaultDiscoverySideAction)
            ) { Icon(Icons.Rounded.Favorite, copy.save, Modifier.size(30.dp)) }
        }
    }
}

private val DefaultDiscoverySideAction = 58.dp
private val DefaultDiscoveryHeroAction = 66.dp

@Composable
private fun DefaultDiscoveryAction(label: String, modifier: Modifier, button: @Composable () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.height(DefaultDiscoveryHeroAction), contentAlignment = Alignment.Center) { button() }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics {}
        )
    }
}

@Composable
internal fun DefaultDiscoveryMixLine(savedThisWeek: Int, copy: DiscoveryCopy, onOpenSaved: () -> Unit) {
    if (savedThisWeek <= 0) return
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        TextButton(onClick = onOpenSaved) {
            Text(
                "${copy.savedThisWeek} · $savedThisWeek",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp), tint = scheme.onSurfaceVariant)
        }
    }
}

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
            containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.7f),
            contentColor = scheme.onSurface
        ),
        modifier = Modifier.size(48.dp)
    ) {
        if (content != null) content() else Icon(icon, label)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultDiscoveryTopBar(
    copy: DiscoveryCopy,
    canUndo: Boolean,
    backLabel: String,
    filtersLabel: String,
    filtersActive: Boolean,
    onDismiss: () -> Unit,
    onUndo: () -> Unit,
    onFilters: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val titleText = copy.title.trim()
    val firstLine = titleText.substringBefore(' ')
    val restLine = titleText.substringAfter(' ', "").trim()
    val titleStyle = MaterialTheme.typography.displaySmall.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = (-1).sp,
        lineHeight = 40.sp
    )
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
            Text(
                firstLine,
                style = titleStyle,
                color = scheme.onSurface,
                maxLines = 2,
                autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 36.sp),
                overflow = TextOverflow.Ellipsis
            )
            if (restLine.isNotEmpty()) {
                Text(
                    restLine,
                    style = titleStyle,
                    color = scheme.primary,
                    maxLines = 2,
                    autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 36.sp),
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DefaultDiscoveryGlassButton(Icons.AutoMirrored.Rounded.ArrowBack, backLabel, onClick = onDismiss)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DefaultDiscoveryGlassButton(Icons.AutoMirrored.Rounded.Undo, copy.undo, enabled = canUndo, onClick = onUndo)
                FilledTonalIconButton(
                    onClick = onFilters,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (filtersActive) scheme.primary else scheme.surfaceContainerHigh.copy(alpha = 0.7f),
                        contentColor = if (filtersActive) scheme.onPrimary else scheme.onSurface
                    ),
                    modifier = Modifier.size(48.dp)
                ) { Icon(Icons.Rounded.Tune, filtersLabel) }
            }
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
        Modifier.fillMaxWidth().padding(top = 56.dp, start = 40.dp, end = 40.dp),
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
