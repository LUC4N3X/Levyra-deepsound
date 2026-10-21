package com.luc4n3x.levyra.ui.theme

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import androidx.graphics.shapes.RoundedPolygon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraSkin
import com.luc4n3x.levyra.ui.i18n.LevyraPrismStrings
import com.luc4n3x.levyra.ui.i18n.LevyraSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings

private fun LevyraSkin.title(): String = when (this) {
    LevyraSkin.Default -> "Levyra Default"
    LevyraSkin.Nocturne78 -> Nocturne78Palette.label
    LevyraSkin.Prism -> "Prism"
    LevyraSkin.Torque -> "Torque"
    LevyraSkin.Ivory -> "Ivory"
    LevyraSkin.SpotifyStyle -> "Spotify Style"
}

@Composable
internal fun SkinSelector(selected: LevyraSkin, onSelect: (LevyraSkin) -> Unit) {
    val code = LocalLevyraStrings.current.code
    val copy = LevyraSkinStrings.forCode(code)
    val body = when (selected) {
        LevyraSkin.Default -> copy.defaultBody
        LevyraSkin.Nocturne78 -> copy.nocturneBody
        LevyraSkin.Prism -> LevyraPrismStrings.forCode(code).body
        LevyraSkin.Torque -> LevyraPrismStrings.forCode(code).torqueBody
        LevyraSkin.Ivory -> LevyraPrismStrings.forCode(code).ivoryBody
        LevyraSkin.SpotifyStyle -> LevyraPrismStrings.forCode(code).spotifyBody
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SkinStage(selected)
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                selected.title(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SkinCarousel(selected, onSelect)
        if (selected != LevyraSkin.Default) {
            SkinManagedNotice { onSelect(LevyraSkin.Default) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SkinStage(skin: LevyraSkin) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val animations = LocalAnimationsEnabled.current
    val spin = if (animations) {
        rememberInfiniteTransition(label = "skin-stage-spin").animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(SKIN_STAGE_SPIN_MS, easing = LinearEasing)),
            label = "skin-stage-rotation"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Box(
        Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(40.dp)).background(scheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = skin,
            transitionSpec = {
                fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.72f) togetherWith
                    fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec(), targetScale = 1.12f)
            },
            label = "skin-stage"
        ) { shown ->
            val accent = skinAccent(shown, scheme.primary)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(300.dp)
                        .graphicsLayer { rotationZ = spin.value }
                        .background(accent.copy(alpha = 0.92f), skinStagePolygon(shown).toShape())
                )
                Box(
                    Modifier.align(Alignment.TopStart).padding(22.dp).size(44.dp)
                        .graphicsLayer { rotationZ = -spin.value * 2f }
                        .background(scheme.tertiaryContainer, MaterialShapes.Clover4Leaf.toShape())
                )
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(26.dp).size(30.dp)
                        .background(scheme.secondaryContainer, MaterialShapes.Cookie4Sided.toShape())
                )
                SkinPhonePreview(
                    shown, 20.dp,
                    Modifier.width(150.dp).aspectRatio(0.56f)
                        .graphicsLayer { rotationZ = -6f }
                        .shadow(28.dp, RoundedCornerShape(20.dp), clip = false)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SkinCarousel(selected: LevyraSkin, onSelect: (LevyraSkin) -> Unit) {
    val skins = LevyraSkin.entries
    val scheme = MaterialTheme.colorScheme
    val state = rememberCarouselState(initialItem = skins.indexOf(selected).coerceAtLeast(0)) { skins.size }
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = 136.dp,
        itemSpacing = 8.dp,
        modifier = Modifier.fillMaxWidth().height(210.dp).selectableGroup()
    ) { index ->
        val skin = skins[index]
        val active = skin == selected
        Box(
            Modifier.fillMaxSize().maskClip(MaterialTheme.shapes.extraLarge)
                .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(skin) })
        ) {
            SkinPhonePreview(skin, 0.dp, Modifier.fillMaxSize())
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                    .padding(start = 14.dp, end = 14.dp, top = 28.dp, bottom = 14.dp)
            ) {
                Text(skin.title(), style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (active) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(10.dp).size(32.dp).background(scheme.primary, MaterialShapes.Cookie9Sided.toShape()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, null, tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private const val SKIN_STAGE_SPIN_MS = 36_000

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun skinStagePolygon(skin: LevyraSkin): RoundedPolygon = when (skin) {
    LevyraSkin.Default -> MaterialShapes.Cookie9Sided
    LevyraSkin.Nocturne78 -> MaterialShapes.Sunny
    LevyraSkin.Prism -> MaterialShapes.Gem
    LevyraSkin.Torque -> MaterialShapes.SoftBurst
    LevyraSkin.Ivory -> MaterialShapes.Flower
    LevyraSkin.SpotifyStyle -> MaterialShapes.Cookie12Sided
}

private fun skinAccent(skin: LevyraSkin, primary: Color): Color =
    if (skin == LevyraSkin.Default) primary else skinPreviewPalette(skin).cyan

@Composable
internal fun SkinManagedNotice(onUseDefault: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            LevyraSkinStrings.managedForCode(LocalLevyraStrings.current.code, LevyraSkinLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = LevyraMuted
        )
        if (onUseDefault != null) {
            TextButton(onClick = onUseDefault) { Text("Levyra Default") }
        }
    }
}

@Composable
private fun SkinPhonePreview(skin: LevyraSkin, corner: Dp, modifier: Modifier) {
    val palette = skinPreviewPalette(skin)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val frame = CornerRadius(corner.toPx())
        drawRoundRect(palette.black, cornerRadius = frame)
        drawSkinPreviewWash(skin, palette, frame)
        when (skin) {
            LevyraSkin.Torque -> torqueBand(palette, w, h)
            LevyraSkin.Ivory -> ivoryPreview(palette, w, h)
            LevyraSkin.SpotifyStyle -> spotifyPreview(palette, w, h)
            else -> playerPreview(skin, palette, w, h)
        }
    }
}

private fun DrawScope.playerPreview(skin: LevyraSkin, palette: LevyraPalette, w: Float, h: Float) {
    val nocturne = skin == LevyraSkin.Nocturne78
    val prism = skin == LevyraSkin.Prism
    val pad = w * 0.12f
    val line = w - pad * 2
    val art = minOf(line, h * 0.44f)
    val artTop = h * 0.08f
    val artRadius = CornerRadius(if (nocturne) 0f else if (prism) 6.dp.toPx() else 10.dp.toPx())
    val artBrush = skinPreviewArtBrush(skin, palette)
    drawRoundRect(artBrush, Offset((w - art) / 2, artTop), Size(art, art), artRadius)
    if (nocturne) drawCircle(palette.cyan, art * 0.18f, Offset(w / 2, artTop + art / 2), style = Stroke(1.5.dp.toPx()))
    val textTop = artTop + art + h * 0.05f
    previewLine(palette.text, pad, textTop, line * 0.7f, 3.dp.toPx())
    previewLine(palette.muted, pad, textTop + h * 0.04f, line * 0.45f, 2.dp.toPx())
    val seekY = textTop + h * 0.095f
    previewLine(palette.panelSoft, pad, seekY, line, 2.5.dp.toPx())
    if (prism) drawLine(
        Brush.horizontalGradient(listOf(palette.blue, palette.violet), pad, pad + line * 0.55f),
        Offset(pad, seekY), Offset(pad + line * 0.55f, seekY), 2.5.dp.toPx(), cap = StrokeCap.Round
    ) else previewLine(palette.cyan, pad, seekY, line * 0.55f, 2.5.dp.toPx())
    val playRadius = minOf(w * 0.11f, h * 0.065f)
    previewPlayButton(skin, palette, Offset(w / 2, seekY + h * 0.035f + playRadius), playRadius)
    val navTop = h * 0.9f
    val navWidth = w - pad * 1.2f
    drawRoundRect(palette.panel, Offset(pad * 0.6f, navTop), Size(navWidth, h * 0.065f), CornerRadius(if (nocturne) 2.dp.toPx() else h * 0.033f))
    repeat(4) { index ->
        val x = pad * 0.6f + navWidth * (index + 0.5f) / 4f
        drawCircle(if (index == 0) palette.cyan else palette.muted.copy(alpha = 0.6f), 1.8.dp.toPx(), Offset(x, navTop + h * 0.0325f))
    }
}

private fun DrawScope.previewLine(color: Color, x: Float, y: Float, length: Float, width: Float) =
    drawLine(color, Offset(x, y), Offset(x + length, y), width, cap = StrokeCap.Round)

private fun DrawScope.previewPlayButton(skin: LevyraSkin, palette: LevyraPalette, center: Offset, radius: Float) {
    when (skin) {
        LevyraSkin.Nocturne78 -> drawRoundRect(palette.panelSoft, center - Offset(radius, radius), Size(radius * 2, radius * 2), CornerRadius(2.dp.toPx()))
        LevyraSkin.Prism -> drawCircle(Brush.linearGradient(listOf(palette.blue, palette.violet), center - Offset(radius, radius), center + Offset(radius, radius)), radius, center)
        LevyraSkin.Default -> drawRoundRect(palette.cyan, center - Offset(radius, radius), Size(radius * 2, radius * 2), CornerRadius(radius * 0.6f))
        LevyraSkin.Torque -> {
            drawCircle(palette.black, radius, center)
            drawCircle(palette.blue, radius, center, style = Stroke(radius * 0.22f))
        }
        LevyraSkin.Ivory -> drawRoundRect(palette.blue, center - Offset(radius * 2.4f, radius), Size(radius * 4.8f, radius * 2), CornerRadius(radius))
        LevyraSkin.SpotifyStyle -> drawCircle(palette.cyan, radius, center)
    }
    val glyph = Path().apply {
        moveTo(center.x - radius * 0.32f, center.y - radius * 0.45f)
        lineTo(center.x + radius * 0.48f, center.y)
        lineTo(center.x - radius * 0.32f, center.y + radius * 0.45f)
        close()
    }
    val glyphColor = when (skin) {
        LevyraSkin.Nocturne78 -> palette.cyan
        LevyraSkin.Prism -> Color.White
        LevyraSkin.Default -> palette.black
        LevyraSkin.Torque -> palette.text
        LevyraSkin.Ivory -> palette.black
        LevyraSkin.SpotifyStyle -> Color.Black
    }
    drawPath(glyph, glyphColor, style = if (skin == LevyraSkin.Nocturne78) Stroke(1.2.dp.toPx()) else Fill)
}

private fun DrawScope.torqueBand(palette: LevyraPalette, w: Float, h: Float) {
    val coverBottom = h * 0.5f
    drawRect(Brush.linearGradient(listOf(palette.panelSoft, palette.panel)), Offset.Zero, Size(w, coverBottom))
    drawCircle(palette.muted.copy(alpha = 0.5f), w * 0.22f, Offset(w / 2, coverBottom / 2), style = Stroke(1.dp.toPx()))
    val band = Path().apply {
        moveTo(0f, coverBottom)
        lineTo(w, coverBottom)
        lineTo(w, h * 0.66f)
        lineTo(0f, h * 0.86f)
        close()
    }
    drawPath(band, palette.blue)
    previewLine(palette.text, w * 0.3f, h * 0.56f, w * 0.4f, 3.dp.toPx())
    previewLine(palette.text.copy(alpha = 0.8f), w * 0.36f, h * 0.61f, w * 0.28f, 2.dp.toPx())
    previewPlayButton(LevyraSkin.Torque, palette, Offset(w / 2, h * 0.74f), w * 0.11f)
    previewLine(palette.text, w * 0.12f, h * 0.92f, w * 0.76f, 1.5.dp.toPx())
}

private fun DrawScope.ivoryPreview(palette: LevyraPalette, w: Float, h: Float) {
    val pad = w * 0.14f
    val art = w - pad * 2
    val top = h * 0.1f
    drawRoundRect(Brush.linearGradient(listOf(palette.panelSoft, palette.panel)), Offset(pad, top), Size(art, art), CornerRadius(art * 0.24f))
    val corner = art * 0.3f
    drawArc(palette.blue, 180f, 90f, true, Offset(pad + art - corner, top + art - corner), Size(corner * 2, corner * 2))
    previewLine(palette.text, w * 0.22f, top + art + h * 0.07f, w * 0.56f, 3.dp.toPx())
    previewLine(palette.muted, w * 0.3f, top + art + h * 0.115f, w * 0.4f, 2.dp.toPx())
    val bar = top + art + h * 0.17f
    previewLine(palette.panelSoft, pad, bar, art, 2.dp.toPx())
    previewLine(palette.blue, pad, bar, art * 0.55f, 2.dp.toPx())
    previewPlayButton(LevyraSkin.Ivory, palette, Offset(w / 2, bar + h * 0.09f), w * 0.09f)
    val tab = Path().apply {
        moveTo(0f, h * 0.7f); lineTo(w * 0.1f, h * 0.7f); lineTo(w * 0.1f, h * 0.76f); lineTo(0f, h * 0.76f); close()
    }
    drawPath(tab, palette.blue)
    drawRect(palette.ink, Offset(0f, h * 0.91f), Size(w, h * 0.09f))
    val bump = Path().apply {
        val l = w * 0.36f; val r = w * 0.64f; val b = h; val t = h * 0.885f
        moveTo(l - w * 0.06f, b); cubicTo(l, b, l, t, l + w * 0.04f, t); lineTo(r - w * 0.04f, t); cubicTo(r, t, r, b, r + w * 0.06f, b); close()
    }
    drawPath(bump, palette.blue)
}

private fun DrawScope.spotifyPreview(palette: LevyraPalette, w: Float, h: Float) {
    val pad = w * 0.08f
    val gap = w * 0.04f
    drawRect(Brush.verticalGradient(listOf(Color(0xFF3A2A58), palette.black), endY = h * 0.42f), Offset.Zero, Size(w, h * 0.42f))
    repeat(3) { index ->
        val x = pad + index * (w * 0.22f + gap * 0.5f)
        drawRoundRect(if (index == 0) palette.cyan else palette.panelSoft, Offset(x, h * 0.07f), Size(w * 0.22f, h * 0.035f), CornerRadius(h * 0.0175f))
    }
    val tileW = (w - pad * 2 - gap) / 2
    val tileH = h * 0.06f
    repeat(6) { index ->
        val x = pad + index % 2 * (tileW + gap)
        val y = h * 0.14f + index / 2 * (tileH + gap * 0.6f)
        drawRoundRect(Color.White.copy(alpha = 0.1f), Offset(x, y), Size(tileW, tileH), CornerRadius(2.dp.toPx()))
        drawRect(Brush.linearGradient(listOf(palette.panelSoft, Color(0xFF535353))), Offset(x, y), Size(tileH, tileH))
    }
    previewLine(palette.text, pad, h * 0.44f, w * 0.5f, 3.dp.toPx())
    val card = (w - pad * 2 - gap) / 2.2f
    repeat(3) { index ->
        drawRoundRect(Brush.linearGradient(listOf(palette.panelSoft, palette.panel)), Offset(pad + index * (card + gap), h * 0.48f), Size(card, card), CornerRadius(2.dp.toPx()))
    }
    previewLine(palette.muted, pad, h * 0.48f + card + h * 0.035f, card * 0.8f, 2.dp.toPx())
    drawRoundRect(Color(0xFF3B2D4F), Offset(pad * 0.5f, h * 0.8f), Size(w - pad, h * 0.065f), CornerRadius(3.dp.toPx()))
    drawRoundRect(palette.panelSoft, Offset(pad, h * 0.81f), Size(h * 0.045f, h * 0.045f), CornerRadius(1.dp.toPx()))
    previewLine(palette.text, pad * 1.4f + h * 0.045f, h * 0.825f, w * 0.34f, 2.dp.toPx())
    previewLine(palette.text, pad * 0.9f, h * 0.862f, w * 0.42f, 1.dp.toPx())
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = h * 0.86f, endY = h), Offset(0f, h * 0.86f), Size(w, h * 0.14f))
    repeat(4) { index ->
        drawCircle(if (index == 0) palette.text else palette.muted.copy(alpha = 0.7f), 2.dp.toPx(), Offset(w * (index + 0.5f) / 4f, h * 0.935f))
    }
}

private fun skinPreviewPalette(skin: LevyraSkin): LevyraPalette = when (skin) {
    LevyraSkin.Nocturne78 -> Nocturne78Palette
    LevyraSkin.Prism -> prismPalette()
    LevyraSkin.Torque -> torquePalette()
    LevyraSkin.Ivory -> ivoryPalette()
    LevyraSkin.SpotifyStyle -> spotifyPalette()
    LevyraSkin.Default -> LevyraThemes.levyraAura
}

private fun skinPreviewArtBrush(skin: LevyraSkin, palette: LevyraPalette): Brush = when (skin) {
    LevyraSkin.Nocturne78 -> Brush.linearGradient(listOf(palette.panelSoft, palette.panel))
    LevyraSkin.Prism -> Brush.linearGradient(listOf(palette.blue, palette.violet))
    LevyraSkin.Default -> Brush.linearGradient(listOf(palette.cyan, palette.violet))
    LevyraSkin.Torque -> Brush.linearGradient(listOf(palette.panelSoft, palette.panel))
    LevyraSkin.Ivory -> Brush.linearGradient(listOf(palette.panelSoft, palette.panel))
    LevyraSkin.SpotifyStyle -> Brush.linearGradient(listOf(palette.panelSoft, palette.panel))
}

private fun DrawScope.drawSkinPreviewWash(skin: LevyraSkin, palette: LevyraPalette, frame: CornerRadius) {
    when (skin) {
        LevyraSkin.Prism -> drawRoundRect(Brush.verticalGradient(listOf(palette.blue.copy(alpha = 0.22f), palette.black), endY = size.height * 0.6f), cornerRadius = frame)
        LevyraSkin.Default -> drawRoundRect(Brush.verticalGradient(listOf(palette.violet.copy(alpha = 0.28f), palette.black), endY = size.height * 0.7f), cornerRadius = frame)
        LevyraSkin.Nocturne78, LevyraSkin.Torque, LevyraSkin.Ivory, LevyraSkin.SpotifyStyle -> Unit
    }
}
