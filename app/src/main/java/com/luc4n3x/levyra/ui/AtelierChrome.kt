package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.theme.atelierCarbon
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.ui.theme.AtelierDesign

private val AtelierTabOrder = listOf(LevyraTab.Home, LevyraTab.Explore, LevyraTab.Library, LevyraTab.Search)

private fun atelierTabIcon(tab: LevyraTab, entry: LevyraTabEntry): ImageVector = when (tab) {
    LevyraTab.Home -> Icons.Outlined.Home
    LevyraTab.Search -> Icons.Outlined.Search
    LevyraTab.Explore -> Icons.Outlined.Explore
    LevyraTab.Library -> Icons.Outlined.LibraryMusic
    else -> entry.unselectedIcon
}

@Composable
internal fun AtelierBottomTabs(
    entries: List<LevyraTabEntry>, selected: LevyraTab, applySystemInset: Boolean, modifier: Modifier,
    onSelect: (LevyraTabEntry, Boolean) -> Unit
) {
    val ordered = remember(entries) {
        AtelierTabOrder.mapNotNull { tab -> entries.firstOrNull { it.tab == tab } } + entries.filter { it.tab !in AtelierTabOrder }
    }
    Column(
        modifier.fillMaxWidth().background(Brush.verticalGradient(0f to Color.Transparent, 0.4f to AtelierDesign.room))
            .padding(start = AtelierDesign.miniMargin, end = AtelierDesign.miniMargin, bottom = 6.dp)
    ) {
        val joined = com.luc4n3x.levyra.ui.atelier.AtelierStage.miniAttached
        val plate = if (joined) RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp) else RoundedCornerShape(18.dp)
        Row(
            Modifier.fillMaxWidth().height(AtelierDesign.navHeight).clip(plate).atelierCarbon()
                .border(1.dp, AtelierDesign.hairline, plate).padding(4.dp).selectableGroup(),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ordered.forEach { entry ->
                val active = entry.tab == selected
                AtelierTabKey(entry, active, Modifier.weight(if (active) 1.7f else 1f)) { onSelect(entry, active) }
            }
        }
        if (applySystemInset) Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun AtelierTabKey(entry: LevyraTabEntry, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val animated = LocalAnimationsEnabled.current
    val lit by animateFloatAsState(if (active) 1f else 0f, if (animated) tween(240) else snap(), label = "atelier-tab-lit")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val travel by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 900f), label = "atelier-tab-press")
    val tint = lerp(AtelierDesign.textFaint, AtelierDesign.onAccent, lit)
    Row(
        modifier.fillMaxHeight().graphicsLayer { translationY = travel * 1.5.dp.toPx() }.clip(RoundedCornerShape(14.dp))
            .drawBehind {
                val top = lerp(Color(0xFF111114), lerp(AtelierDesign.accent, Color.Black, 0.02f), lit)
                val bottom = lerp(Color(0xFF0B0B0D), lerp(AtelierDesign.accent, Color.Black, 0.24f), lit)
                drawRect(Brush.verticalGradient(listOf(top, bottom)))
                drawRect(Color.White.copy(alpha = 0.05f + lit * 0.1f), size = Size(size.width, 1.dp.toPx()))
                if (lit > 0.01f) {
                    val width = 18.dp.toPx() * lit
                    drawRoundRect(
                        AtelierDesign.onAccent.copy(alpha = lit * 0.5f), Offset((size.width - width) / 2f, 5.dp.toPx()),
                        Size(width, 2.dp.toPx()), CornerRadius(1.dp.toPx())
                    )
                }
            }
            .selectable(active, interaction, indication = null, role = Role.Tab, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        Icon(atelierTabIcon(entry.tab, entry), entry.label, tint = tint, modifier = Modifier.size(22.dp))
        if (lit > 0.5f) {
            Spacer(Modifier.width(8.dp))
            Text(entry.label, color = tint, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

internal fun atelierMiniSurface(seed: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    hsl[1] = (hsl[1] * 0.55f).coerceAtMost(0.42f)
    hsl[2] = 0.135f
    return Color(ColorUtils.HSLToColor(hsl))
}

@Composable
internal fun AtelierMiniSleeve(track: Track, playing: Boolean, animated: Boolean, modifier: Modifier) {
    val rotation = remember { mutableFloatStateOf(0f) }
    val peek by animateFloatAsState(if (playing) 1f else 0.35f, if (animated) spring(dampingRatio = 0.72f, stiffness = 170f) else snap(), label = "atelier-mini-peek")
    val spinning = playing && animated
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) rotation.floatValue = (rotation.floatValue + (now - last) / 1_000_000_000f * 200f) % 360f
                last = now
            }
        }
    }
    Box(modifier) {
        Canvas(
            Modifier.fillMaxSize().graphicsLayer {
                translationX = size.width * 0.13f + peek * size.width * 0.20f
                rotationZ = rotation.floatValue
            }
        ) {
            val radius = size.minDimension * 0.46f
            drawCircle(Color(0xFF0C0B0D), radius)
            drawCircle(Color.White.copy(alpha = 0.07f), radius * 0.78f, style = Stroke(0.5.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.05f), radius * 0.62f, style = Stroke(0.5.dp.toPx()))
            drawCircle(AtelierDesign.accent, radius * 0.34f)
            drawCircle(AtelierDesign.onAccent, radius * 0.07f)
            drawCircle(Color.White.copy(alpha = 0.55f), radius * 0.05f, center + Offset(radius * 0.21f, 0f))
        }
        Box(Modifier.fillMaxHeight().aspectRatio(1f).clip(AtelierDesign.sleeveShape).background(AtelierDesign.panelRaised)) {
            CoverImage(track, Modifier.fillMaxSize())
        }
    }
}

@Composable
internal fun AtelierMiniControls(playing: Boolean, resolving: Boolean, onToggle: () -> Unit, onNext: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                .levyraPressable(onClick = onToggle, enabled = !resolving, onClickLabel = if (playing) strings.pause else strings.play, role = Role.Button),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(width = 44.dp, height = 40.dp).clip(RoundedCornerShape(11.dp))
                    .background(Brush.verticalGradient(listOf(AtelierDesign.accent, lerp(AtelierDesign.accent, Color.Black, 0.24f))))
                    .border(1.dp, AtelierDesign.hairline, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (resolving) LevyraLoadingIndicator(Modifier.size(18.dp), AtelierDesign.onAccent)
                else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) strings.pause else strings.play, tint = AtelierDesign.onAccent, modifier = Modifier.size(22.dp))
            }
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).levyraPressable(onClick = onNext, onClickLabel = strings.next, role = Role.Button),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.SkipNext, strings.next, tint = AtelierDesign.textSecondary, modifier = Modifier.size(22.dp))
        }
    }
}
