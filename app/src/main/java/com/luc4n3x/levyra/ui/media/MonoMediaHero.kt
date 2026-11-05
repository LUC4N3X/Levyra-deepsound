package com.luc4n3x.levyra.ui.media

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.MonoDesign

private const val MonoCoverWidthFraction = 0.64f
private val MonoCoverMax = 300.dp
private val MonoCoverTop = 16.dp
private val MonoPlaySize = 60.dp
private const val MonoBloomMillis = 900

internal fun monoMediaColors(cover: Color): ImmersiveMediaColors = ImmersiveMediaColors(
    fieldTop = MonoDesign.base,
    fieldMid = monoGlowTone(cover),
    base = MonoDesign.base,
    content = MonoDesign.text,
    contentMuted = MonoDesign.textSecondary,
    accent = MonoDesign.accent,
    actionStart = MonoDesign.accent,
    actionEnd = MonoDesign.accent,
    actionContent = MonoDesign.onAccent,
    secondaryFill = MonoDesign.surface,
    hairline = MonoDesign.hairline
)

private fun monoGlowTone(cover: Color): Color {
    if (!cover.isSpecified) return MonoDesign.accentDim
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(cover.toArgb(), hsl)
    if (hsl[1] < 0.08f) return MonoDesign.accentDim
    hsl[1] = hsl[1].coerceIn(0.28f, 0.66f)
    hsl[2] = hsl[2].coerceIn(0.38f, 0.56f)
    return Color(ColorUtils.HSLToColor(hsl))
}

@Composable
internal fun MonoMediaHero(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    viewportWidth: Dp, topBarHeight: Dp, onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit, modifier: Modifier
) {
    val cover = min(viewportWidth * MonoCoverWidthFraction, MonoCoverMax)
    val bloom = rememberMonoBloom(LocalAnimationsEnabled.current)
    val glow = colors.fieldMid
    Column(
        modifier.fillMaxWidth().drawWithCache {
            val coverPx = cover.toPx()
            val center = Offset(size.width / 2f, (topBarHeight + MonoCoverTop).toPx() + coverPx / 2f)
            val radius = coverPx * 1.05f
            val halo = Brush.radialGradient(
                0f to glow.copy(alpha = 0.55f), 0.45f to glow.copy(alpha = 0.22f),
                0.75f to glow.copy(alpha = 0.06f), 1f to Color.Transparent,
                center = center, radius = radius
            )
            onDrawBehind {
                val progress = bloom.value
                if (progress > 0.01f) {
                    val grow = 0.72f + 0.28f * progress
                    scale(grow, grow, center) { drawCircle(halo, radius, center, alpha = progress) }
                }
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.padding(top = topBarHeight + MonoCoverTop).size(cover)
                .clip(MonoDesign.artShape).background(MonoDesign.surface)
                .border(1.dp, MonoDesign.hairlineStrong, MonoDesign.artShape),
            content = artwork
        )
        MonoHeroText(title, overline, subtitle, metadata, onSubtitleClick)
        Box(Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter).padding(bottom = 20.dp), contentAlignment = Alignment.Center) {
            actions()
        }
    }
}

@Composable
private fun rememberMonoBloom(animated: Boolean): Animatable<Float, AnimationVector1D> {
    var bloomed by rememberSaveable { mutableStateOf(false) }
    val bloom = remember { Animatable(if (bloomed || !animated) 1f else 0f) }
    LaunchedEffect(animated) {
        if (animated && bloom.value < 1f) {
            bloom.animateTo(1f, tween(MonoBloomMillis, easing = FastOutSlowInEasing))
        } else {
            bloom.snapTo(1f)
        }
        bloomed = true
    }
    return bloom
}

@Composable
private fun MonoHeroText(title: String, overline: String, subtitle: String, metadata: String, onSubtitleClick: (() -> Unit)?) {
    val meta = remember(overline, metadata) {
        listOf(overline, metadata).filter(String::isNotBlank).joinToString(" · ").uppercase()
    }
    Column(
        Modifier.fillMaxWidth().padding(start = MonoDesign.gutter, end = MonoDesign.gutter, top = 24.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title, color = MonoDesign.text, style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
        if (subtitle.isNotBlank()) {
            Box(
                if (onSubtitleClick != null) {
                    Modifier.heightIn(min = 48.dp).clip(MonoDesign.pillShape)
                        .levyraPressable(onClick = onSubtitleClick, role = Role.Button, pressedScale = LevyraPressScale.Control)
                        .padding(horizontal = 10.dp)
                } else {
                    Modifier.padding(vertical = 6.dp)
                },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    subtitle, color = if (onSubtitleClick != null) MonoDesign.accent else MonoDesign.textSecondary,
                    style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Spacer(Modifier.height(10.dp))
        }
        if (meta.isNotBlank()) {
            Text(
                meta, color = MonoDesign.textSecondary,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp),
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun MonoMediaActionRow(
    primary: ImmersiveMediaPrimaryAction,
    shuffleLabel: String,
    downloadLabel: String,
    shuffleEnabled: Boolean,
    downloadEnabled: Boolean,
    onShuffle: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier,
    secondary: @Composable RowScope.() -> Unit
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonoPlayKey(primary)
        MonoQuietAction(Icons.Rounded.Shuffle, shuffleLabel, shuffleEnabled, onShuffle)
        MonoQuietAction(Icons.Rounded.Download, downloadLabel, downloadEnabled, onDownload)
        secondary()
    }
}

@Composable
private fun MonoPlayKey(primary: ImmersiveMediaPrimaryAction) {
    val enabled = primary.enabled && !primary.loading
    Box(
        Modifier.size(MonoPlaySize).clip(CircleShape)
            .background(if (primary.enabled) MonoDesign.accent else MonoDesign.raised)
            .levyraPressable(onClick = primary.onClick, enabled = enabled, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { contentDescription = primary.contentDescription },
        contentAlignment = Alignment.Center
    ) {
        val tint = if (primary.enabled) MonoDesign.onAccent else MonoDesign.textFaint
        if (primary.loading) {
            LevyraLoadingIndicator(color = tint, modifier = Modifier.size(26.dp))
        } else {
            Icon(primary.icon, null, tint = tint, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
internal fun MonoQuietAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    active: Boolean = false,
    toggleable: Boolean = false
) {
    val ring = when {
        !enabled -> MonoDesign.hairline
        active -> MonoDesign.accent.copy(alpha = 0.6f)
        else -> MonoDesign.hairlineStrong
    }
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .levyraPressable(onClick = onClick, enabled = enabled, pressedScale = LevyraPressScale.Control, role = Role.Button)
            .border(1.dp, ring, CircleShape)
            .semantics { if (toggleable) toggleableState = ToggleableState(active) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, label,
            tint = when {
                !enabled -> MonoDesign.textFaint
                active -> MonoDesign.accent
                else -> MonoDesign.text
            },
            modifier = Modifier.size(22.dp)
        )
    }
}
