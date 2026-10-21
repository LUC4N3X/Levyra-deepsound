package com.luc4n3x.levyra.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import com.luc4n3x.levyra.ui.SettingsStudioGutter
import com.luc4n3x.levyra.ui.recentSearchesBleed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(copy.title, style = MaterialTheme.typography.titleLarge, color = LevyraText, modifier = Modifier.semantics { heading() })
        Row(
            Modifier
                .recentSearchesBleed(SettingsStudioGutter)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = SettingsStudioGutter)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LevyraSkin.entries.forEach { skin ->
                SkinOption(skin, selected == skin, { onSelect(skin) }, Modifier.width(112.dp))
            }
        }
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(selected.title(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (selected != LevyraSkin.Default) {
            SkinManagedNotice { onSelect(LevyraSkin.Default) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SkinOption(skin: LevyraSkin, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val corner by animateDpAsState(if (active) 28.dp else 18.dp, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "skin-option-corner")
    val container by animateColorAsState(if (active) scheme.primaryContainer else scheme.surfaceContainerHigh, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "skin-option-container")
    val content = if (active) scheme.onPrimaryContainer else scheme.onSurface
    Column(
        modifier.clip(RoundedCornerShape(corner)).background(container)
            .selectable(selected = active, role = Role.RadioButton, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.62f)) {
            SkinPhonePreview(skin, (corner - 8.dp).coerceAtLeast(10.dp), Modifier.fillMaxSize())
            if (active) Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp).clip(CircleShape).background(scheme.primary),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Check, null, tint = scheme.onPrimary, modifier = Modifier.size(16.dp)) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            skin.title(), style = MaterialTheme.typography.labelLarge, color = content, textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(2.dp)
        )
    }
}

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
    val nocturne = skin == LevyraSkin.Nocturne78
    val prism = skin == LevyraSkin.Prism
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val frame = CornerRadius(corner.toPx())
        drawRoundRect(palette.black, cornerRadius = frame)
        drawSkinPreviewWash(skin, palette, frame)
        val pad = w * 0.12f
        val art = w - pad * 2
        val artTop = h * 0.12f
        val torque = skin == LevyraSkin.Torque
        val artRadius = CornerRadius(if (nocturne || torque) 0f else if (prism) 6.dp.toPx() else 10.dp.toPx())
        val artBrush = skinPreviewArtBrush(skin, palette)
        if (torque) {
            torqueBand(palette, w, h)
            return@Canvas
        }
        drawRoundRect(artBrush, Offset(pad, artTop), Size(art, art), artRadius)
        if (nocturne) drawCircle(palette.cyan, art * 0.18f, Offset(w / 2, artTop + art / 2), style = Stroke(1.5.dp.toPx()))
        val textTop = artTop + art + h * 0.06f
        previewLine(palette.text, pad, textTop, art * 0.7f, 3.dp.toPx())
        previewLine(palette.muted, pad, textTop + h * 0.045f, art * 0.45f, 2.dp.toPx())
        val seekY = textTop + h * 0.11f
        previewLine(palette.panelSoft, pad, seekY, art, 2.5.dp.toPx())
        if (prism) drawLine(
            Brush.horizontalGradient(listOf(palette.blue, palette.violet), pad, pad + art * 0.55f),
            Offset(pad, seekY), Offset(pad + art * 0.55f, seekY), 2.5.dp.toPx(), cap = StrokeCap.Round
        ) else previewLine(palette.cyan, pad, seekY, art * 0.55f, 2.5.dp.toPx())
        previewPlayButton(skin, palette, Offset(w / 2, seekY + h * 0.1f), w * 0.11f)
        val navTop = h * 0.9f
        val navWidth = w - pad * 1.2f
        drawRoundRect(palette.panel, Offset(pad * 0.6f, navTop), Size(navWidth, h * 0.065f), CornerRadius(if (nocturne) 2.dp.toPx() else h * 0.033f))
        repeat(4) { index ->
            val x = pad * 0.6f + navWidth * (index + 0.5f) / 4f
            drawCircle(if (index == 0) palette.cyan else palette.muted.copy(alpha = 0.6f), 1.8.dp.toPx(), Offset(x, navTop + h * 0.0325f))
        }
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

private fun skinPreviewPalette(skin: LevyraSkin): LevyraPalette = when (skin) {
    LevyraSkin.Nocturne78 -> Nocturne78Palette
    LevyraSkin.Prism -> prismPalette()
    LevyraSkin.Torque -> torquePalette()
    LevyraSkin.Default -> LevyraThemes.levyraAura
}

private fun skinPreviewArtBrush(skin: LevyraSkin, palette: LevyraPalette): Brush = when (skin) {
    LevyraSkin.Nocturne78 -> Brush.linearGradient(listOf(palette.panelSoft, palette.panel))
    LevyraSkin.Prism -> Brush.linearGradient(listOf(palette.blue, palette.violet))
    LevyraSkin.Default -> Brush.linearGradient(listOf(palette.cyan, palette.violet))
    LevyraSkin.Torque -> Brush.linearGradient(listOf(palette.panelSoft, palette.panel))
}

private fun DrawScope.drawSkinPreviewWash(skin: LevyraSkin, palette: LevyraPalette, frame: CornerRadius) {
    when (skin) {
        LevyraSkin.Prism -> drawRoundRect(Brush.verticalGradient(listOf(palette.blue.copy(alpha = 0.22f), palette.black), endY = size.height * 0.6f), cornerRadius = frame)
        LevyraSkin.Default -> drawRoundRect(Brush.verticalGradient(listOf(palette.violet.copy(alpha = 0.28f), palette.black), endY = size.height * 0.7f), cornerRadius = frame)
        LevyraSkin.Nocturne78, LevyraSkin.Torque -> Unit
    }
}
