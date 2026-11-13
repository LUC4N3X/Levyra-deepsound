package com.luc4n3x.levyra.ui.theme

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import androidx.graphics.shapes.RoundedPolygon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import com.luc4n3x.levyra.ui.i18n.homeStudioCopy
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
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

internal fun LevyraSkin.title(): String = when (this) {
    LevyraSkin.Default -> "Levyra Default"
    LevyraSkin.Nocturne78 -> Nocturne78Palette.label
    LevyraSkin.Prism -> "Prism"
    LevyraSkin.Torque -> "Torque"
    LevyraSkin.Ivory -> "Ivory"
    LevyraSkin.SpotifyStyle -> "Spotify"
    LevyraSkin.AppleMusicStyle -> "Apple Music"
    LevyraSkin.YouTubeMusicStyle -> "YouTube Music"
    LevyraSkin.QobuzStyle -> "Qobuz"
    LevyraSkin.Atelier -> "Atelier"
    LevyraSkin.Mono -> "Glacier"
}

@Composable
internal fun SkinSelector(selected: LevyraSkin, onSelect: (LevyraSkin) -> Unit) {
    val code = LocalLevyraStrings.current.code
    val studio = LocalLevyraStrings.current.homeStudioCopy
    val style = skinSelectorStyle(selected)
    val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = SkinOrder.indexOf(selected).coerceAtLeast(0)) { SkinOrder.size }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val focused = SkinOrder[pager.currentPage]
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SkinDeck(pager, selected, style) { skin -> if (skin == SkinOrder[pager.currentPage]) onSelect(skin) else scope.launch { pager.animateScrollToPage(SkinOrder.indexOf(skin)) } }
        SkinGems(focused, selected, style) { skin -> scope.launch { pager.animateScrollToPage(SkinOrder.indexOf(skin)) } }
        AnimatedContent(targetState = focused, label = "skin-copy") { shown -> SkinCopyBlock(shown, code, style) }
        SkinApplyButton(focused == selected, style, if (focused == selected) studio.inUse else studio.apply) { onSelect(focused) }
        if (selected != LevyraSkin.Default) {
            SkinManagedNotice { onSelect(LevyraSkin.Default) }
        }
    }
}

private fun SkinSelectorStyle.font(base: androidx.compose.ui.text.TextStyle) = labelFont?.let { base.copy(fontFamily = it) } ?: base

@Composable
private fun SkinCopyBlock(shown: LevyraSkin, code: String, style: SkinSelectorStyle) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            shown.title(), style = style.font(MaterialTheme.typography.headlineMedium),
            fontWeight = style.titleWeight, color = style.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
        Text(skinBody(shown, code), style = MaterialTheme.typography.bodyLarge, color = style.muted)
    }
}

@Composable
private fun SkinApplyButton(inUse: Boolean, style: SkinSelectorStyle, label: String, onApply: () -> Unit) {
    val shape = if (style.cutCorners) CutCornerShape(topEnd = 18.dp, bottomStart = 6.dp) else RoundedCornerShape(28.dp)
    Box(
        Modifier.fillMaxWidth().height(56.dp).clip(shape)
            .background(if (inUse) style.stageColor else style.accent)
            .border(1.dp, if (inUse) style.accent.copy(alpha = 0.5f) else Color.Transparent, shape)
            .selectable(selected = inUse, enabled = !inUse, role = Role.Button, onClick = onApply),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (inUse) Icon(Icons.Rounded.Check, null, tint = style.accent, modifier = Modifier.size(20.dp))
            Text(label, style = style.font(MaterialTheme.typography.titleMedium), fontWeight = FontWeight.Bold, color = if (inUse) style.accent else style.onAccent)
        }
    }
}

private fun skinBody(skin: LevyraSkin, code: String): String = when (skin) {
    LevyraSkin.Default -> LevyraSkinStrings.forCode(code).defaultBody
    LevyraSkin.Nocturne78 -> LevyraSkinStrings.forCode(code).nocturneBody
    LevyraSkin.Prism -> LevyraPrismStrings.forCode(code).body
    LevyraSkin.Torque -> LevyraPrismStrings.forCode(code).torqueBody
    LevyraSkin.Ivory -> LevyraPrismStrings.forCode(code).ivoryBody
    LevyraSkin.SpotifyStyle -> LevyraPrismStrings.forCode(code).spotifyBody
    LevyraSkin.AppleMusicStyle -> LevyraPrismStrings.forCode(code).appleMusicBody
    LevyraSkin.YouTubeMusicStyle -> LevyraPrismStrings.forCode(code).youTubeMusicBody
    LevyraSkin.QobuzStyle -> com.luc4n3x.levyra.ui.i18n.LevyraQobuzSkinStrings.forCode(code).skinBody
    LevyraSkin.Atelier -> com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings.forCode(code).skinBody
    LevyraSkin.Mono -> com.luc4n3x.levyra.ui.i18n.LevyraMonoStrings.forCode(code).skinBody
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SkinDeck(
    pager: androidx.compose.foundation.pager.PagerState,
    selected: LevyraSkin,
    style: SkinSelectorStyle,
    onTap: (LevyraSkin) -> Unit
) {
    val animations = LocalAnimationsEnabled.current
    val spin = if (animations) {
        rememberInfiniteTransition(label = "skin-deck-spin").animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(SKIN_STAGE_SPIN_MS, easing = LinearEasing)),
            label = "skin-deck-rotation"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val focused = SkinOrder[pager.currentPage]
    val glow by animateColorAsState(skinAccent(focused, style.accent), label = "skin-deck-glow")
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.fillMaxWidth().height(380.dp).clip(style.stageShape).background(style.stageColor)
            .border(1.dp, style.stageBorder, style.stageShape)
    ) {
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(Brush.radialGradient(listOf(glow.copy(alpha = 0.55f), glow.copy(alpha = 0.12f), Color.Transparent), center = Offset(size.width / 2f, size.height * 0.46f), radius = size.width * 0.75f))
                if (style.cutCorners) {
                    val step = 18.dp.toPx()
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(Color.White.copy(alpha = 0.035f), Offset(x, size.height), Offset(x + size.height, 0f), 1.dp.toPx())
                        x += step
                    }
                }
            }
        )
        if (!style.cutCorners) {
            AnimatedContent(targetState = focused, label = "skin-deck-shape", modifier = Modifier.align(Alignment.Center)) { shown ->
                Box(
                    Modifier.size(270.dp).graphicsLayer { rotationZ = spin.value; alpha = 0.55f }
                        .background(skinAccent(shown, style.accent), skinStagePolygon(shown).toShape())
                )
            }
        }
        val phoneWidth = 150.dp
        val side = (maxWidth - phoneWidth) / 2
        androidx.compose.foundation.pager.HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = side),
            pageSpacing = (-22).dp,
            beyondViewportPageCount = 2,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val skin = SkinOrder[page]
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.width(phoneWidth).aspectRatio(0.56f)
                        .graphicsLayer {
                            val offset = (pager.currentPage - page + pager.currentPageOffsetFraction).coerceIn(-2f, 2f)
                            val distance = kotlin.math.abs(offset)
                            rotationZ = offset * 9f
                            translationY = distance * distance * 26.dp.toPx()
                            translationX = offset * 18.dp.toPx()
                            scaleX = 1f - 0.16f * distance.coerceAtMost(1.5f)
                            scaleY = scaleX
                            alpha = 1f - 0.38f * distance.coerceAtMost(1.6f)
                            shadowElevation = (1f - distance.coerceAtMost(1f)) * 30.dp.toPx()
                            shape = RoundedCornerShape(20.dp)
                            clip = true
                        }
                        .selectable(selected = skin == selected, role = Role.RadioButton) { onTap(skin) }
                        .semantics { contentDescription = skin.title() }
                ) {
                    SkinPhonePreview(skin, 20.dp, Modifier.fillMaxSize())
                    if (skin == selected) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).background(style.accent, style.badgeShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Check, null, tint = style.onAccent, modifier = Modifier.size(17.dp)) }
                    }
                }
            }
        }
        if (style.numbered) {
            Text(
                "${(pager.currentPage + 1).toString().padStart(2, '0')} / ${SkinOrder.size.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoCodeFont), color = style.accent,
                modifier = Modifier.align(Alignment.TopStart).padding(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SkinGems(focused: LevyraSkin, selected: LevyraSkin, style: SkinSelectorStyle, onJump: (LevyraSkin) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(SkinOrder.size) { index ->
            val skin = SkinOrder[index]
            val active = skin == focused
            val size by animateDpAsState(if (active) 44.dp else 34.dp, label = "skin-gem-size")
            val gemShape = when {
                style.cutCorners -> CutCornerShape(topEnd = size / 3, bottomStart = size / 8)
                style.expressive -> skinStagePolygon(skin).toShape()
                else -> style.badgeShape
            }
            Box(
                Modifier.size(48.dp).selectable(selected = active, role = Role.Tab, onClick = { onJump(skin) })
                    .semantics { contentDescription = skin.title() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(size).clip(gemShape)
                        .background(Brush.linearGradient(listOf(skinAccent(skin, style.accent), skinPreviewPalette(skin).black)))
                        .border(if (active) 2.dp else 1.dp, if (active) style.text else style.muted.copy(alpha = 0.3f), gemShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (skin == selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

private val SkinOrder = listOf(
    LevyraSkin.Default, LevyraSkin.Mono, LevyraSkin.Atelier, LevyraSkin.Prism, LevyraSkin.Nocturne78, LevyraSkin.Torque,
    LevyraSkin.Ivory, LevyraSkin.SpotifyStyle, LevyraSkin.AppleMusicStyle, LevyraSkin.YouTubeMusicStyle, LevyraSkin.QobuzStyle
)
private const val SKIN_GRID_COLUMNS = 3

private class SkinSelectorStyle(
    val stageColor: Color,
    val stageShape: Shape,
    val stageBorder: Color,
    val accent: Color,
    val onAccent: Color,
    val text: Color,
    val muted: Color,
    val cardCorner: Dp,
    val selectedCorner: Dp,
    val badgeShape: Shape,
    val cutCorners: Boolean,
    val labelFont: FontFamily?,
    val numbered: Boolean,
    val titleWeight: FontWeight,
    val expressive: Boolean = false
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun skinSelectorStyle(active: LevyraSkin): SkinSelectorStyle {
    val scheme = MaterialTheme.colorScheme
    return when (active) {
        LevyraSkin.Default -> SkinSelectorStyle(
            stageColor = scheme.surfaceContainerHigh, stageShape = RoundedCornerShape(40.dp), stageBorder = Color.Transparent,
            accent = scheme.primary, onAccent = scheme.onPrimary, text = scheme.onSurface, muted = scheme.onSurfaceVariant,
            cardCorner = 22.dp, selectedCorner = 34.dp, badgeShape = MaterialShapes.Cookie9Sided.toShape(),
            cutCorners = false, labelFont = null, numbered = false, titleWeight = FontWeight.Black, expressive = true
        )
        LevyraSkin.Mono -> SkinSelectorStyle(
            stageColor = MonoDesign.surface, stageShape = CutCornerShape(topEnd = 36.dp, bottomStart = 10.dp), stageBorder = MonoDesign.hairlineStrong,
            accent = MonoDesign.accent, onAccent = MonoDesign.onAccent, text = MonoDesign.text, muted = MonoDesign.textSecondary,
            cardCorner = 18.dp, selectedCorner = 18.dp, badgeShape = CircleShape,
            cutCorners = true, labelFont = MonoCodeFont, numbered = true, titleWeight = FontWeight.Medium
        )
        else -> {
            val palette = skinPreviewPalette(active)
            val corner = when (active) {
                LevyraSkin.Nocturne78 -> 2.dp
                LevyraSkin.QobuzStyle -> 4.dp
                LevyraSkin.Atelier -> 6.dp
                LevyraSkin.Torque -> 10.dp
                else -> 18.dp
            }
            SkinSelectorStyle(
                stageColor = palette.panel, stageShape = RoundedCornerShape(corner * 2), stageBorder = palette.outline,
                accent = palette.cyan, onAccent = palette.black, text = scheme.onSurface, muted = scheme.onSurfaceVariant,
                cardCorner = corner, selectedCorner = corner, badgeShape = if (corner < 8.dp) RoundedCornerShape(corner) else CircleShape,
                cutCorners = false, labelFont = null, numbered = false,
                titleWeight = if (active == LevyraSkin.Atelier || active == LevyraSkin.Nocturne78) FontWeight.Medium else FontWeight.Bold
            )
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
    LevyraSkin.AppleMusicStyle -> MaterialShapes.Pill
    LevyraSkin.YouTubeMusicStyle -> MaterialShapes.Circle
    LevyraSkin.QobuzStyle -> MaterialShapes.Square
    LevyraSkin.Atelier -> MaterialShapes.Clover4Leaf
    LevyraSkin.Mono -> MaterialShapes.Slanted
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
            LevyraSkin.AppleMusicStyle -> appleMusicPreview(palette, w, h)
            LevyraSkin.YouTubeMusicStyle -> youTubeMusicPreview(palette, w, h)
            LevyraSkin.QobuzStyle -> qobuzPreview(palette, w, h)
            LevyraSkin.Atelier -> atelierPreview(palette, w, h)
            LevyraSkin.Mono -> monoPreview(palette, w, h)
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
        LevyraSkin.AppleMusicStyle -> drawCircle(palette.panel, radius, center)
        LevyraSkin.YouTubeMusicStyle -> drawCircle(YouTubeMusicDesign.red, radius, center)
        LevyraSkin.QobuzStyle -> drawCircle(palette.text, radius, center)
        LevyraSkin.Atelier -> drawCircle(palette.cyan, radius, center)
        LevyraSkin.Mono -> drawCircle(palette.cyan, radius, center)
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
        LevyraSkin.AppleMusicStyle -> palette.text
        LevyraSkin.YouTubeMusicStyle -> Color.White
        LevyraSkin.QobuzStyle -> palette.black
        LevyraSkin.Atelier -> palette.black
        LevyraSkin.Mono -> MonoDesign.onAccent
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

internal fun DrawScope.spotifyPreview(palette: LevyraPalette, w: Float, h: Float) {
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

internal fun DrawScope.youTubeMusicPreview(palette: LevyraPalette, w: Float, h: Float) {
    val pad = w * 0.06f
    drawRect(Brush.verticalGradient(listOf(Color(0xFF3A2A4A), palette.black), endY = h * 0.32f), size = Size(w, h * 0.32f))
    drawCircle(YouTubeMusicDesign.red, 4.dp.toPx(), Offset(pad + 4.dp.toPx(), h * 0.07f))
    previewLine(palette.text, pad + 11.dp.toPx(), h * 0.07f - 1.5.dp.toPx(), w * 0.2f, 3.dp.toPx())
    repeat(3) { index -> drawRoundRect(YouTubeMusicDesign.chipFill, Offset(pad + index * w * 0.27f, h * 0.11f), Size(w * 0.24f, h * 0.035f), CornerRadius(2.dp.toPx())) }
    previewLine(palette.text, pad, h * 0.18f, w * 0.38f, 3.5.dp.toPx())
    repeat(4) { index ->
        val y = h * 0.21f + index * h * 0.065f
        drawRect(palette.panelSoft, Offset(pad, y), Size(h * 0.05f, h * 0.05f))
        previewLine(palette.text, pad + h * 0.065f, y + h * 0.014f, w * 0.4f, 1.8.dp.toPx())
        previewLine(palette.muted, pad + h * 0.065f, y + h * 0.034f, w * 0.3f, 1.2.dp.toPx())
    }
    previewLine(palette.text, pad, h * 0.5f, w * 0.34f, 3.dp.toPx())
    repeat(3) { index -> drawRoundRect(palette.panelSoft, Offset(pad + index * w * 0.36f, h * 0.53f), Size(w * 0.32f, w * 0.32f), CornerRadius(1.dp.toPx())) }
    val miniTop = h * 0.8f
    drawRect(YouTubeMusicDesign.miniPlayer, Offset(0f, miniTop), Size(w, h * 0.06f))
    drawRect(palette.panelSoft, Offset(pad, miniTop + h * 0.01f), Size(h * 0.04f, h * 0.04f))
    drawRect(Color.White, Offset(0f, miniTop + h * 0.06f - 1.dp.toPx()), Size(w * 0.4f, 1.dp.toPx()))
    repeat(3) { index ->
        val x = w * (index + 0.5f) / 3f
        drawCircle(if (index == 0) Color.White else palette.muted, 2.6.dp.toPx(), Offset(x, h * 0.9f))
        previewLine(if (index == 0) Color.White else palette.muted, x - w * 0.05f, h * 0.925f, w * 0.1f, 1.dp.toPx())
    }
}

internal fun DrawScope.atelierPreview(palette: LevyraPalette, w: Float, h: Float) {
    val side = w * 0.46f
    val stack = listOf(0.62f, 0.54f, 0.46f)
    stack.forEachIndexed { index, x ->
        drawRect(Color(0xFF2A2556).copy(alpha = 0.5f + index * 0.2f), Offset(w * x, h * (0.12f + index * 0.02f)), Size(side * 0.7f, side * 0.8f))
    }
    drawCircle(Color(0xFF0D0C0E), side * 0.46f, Offset(w * 0.56f, h * 0.31f))
    drawCircle(palette.cyan, side * 0.13f, Offset(w * 0.56f, h * 0.31f))
    drawRect(Brush.linearGradient(listOf(Color(0xFF8A5A33), Color(0xFF2A1C12))), Offset(w * 0.14f, h * 0.14f), Size(side, side))
    drawRoundRect(Color(0xFF4A2C18), Offset(w * 0.04f, h * 0.56f), Size(w * 0.92f, h * 0.32f), CornerRadius(3.dp.toPx()))
    drawRoundRect(Color(0xFF2F2A24), Offset(w * 0.07f, h * 0.58f), Size(w * 0.86f, h * 0.28f), CornerRadius(2.dp.toPx()))
    drawCircle(Color(0xFF0D0C0E), h * 0.115f, Offset(w * 0.38f, h * 0.72f))
    drawCircle(palette.cyan, h * 0.03f, Offset(w * 0.38f, h * 0.72f))
    drawLine(Color(0xFFCFC9BD), Offset(w * 0.82f, h * 0.60f), Offset(w * 0.56f, h * 0.78f), 2.dp.toPx())
}

internal fun DrawScope.monoPreview(palette: LevyraPalette, w: Float, h: Float) {
    val pad = w * 0.07f
    val mid = h * 0.3f
    val wave = Path()
    val steps = 64
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val envelope = 0.35f + 0.65f * kotlin.math.abs(kotlin.math.sin(t * Math.PI.toFloat() * 2f))
        val y = mid + kotlin.math.sin(t * Math.PI.toFloat() * 18f) * h * 0.11f * envelope
        if (i == 0) wave.moveTo(pad + t * (w - pad * 2), y) else wave.lineTo(pad + t * (w - pad * 2), y)
    }
    drawPath(wave, palette.text, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    previewLine(palette.text, pad, h * 0.55f, w * 0.42f, 5.dp.toPx())
    previewLine(palette.muted, pad, h * 0.61f, w * 0.26f, 3.dp.toPx())
    drawLine(palette.outline, Offset(pad, h * 0.69f), Offset(w - pad, h * 0.69f), 2.dp.toPx(), StrokeCap.Round)
    drawLine(palette.text, Offset(pad, h * 0.69f), Offset(w * 0.38f, h * 0.69f), 2.dp.toPx(), StrokeCap.Round)
    drawRoundRect(palette.panel, Offset(pad, h * 0.78f), Size(w - pad * 2, h * 0.13f), CornerRadius(h * 0.03f))
    drawRoundRect(palette.panelSoft, Offset(pad * 1.6f, h * 0.805f), Size(h * 0.08f, h * 0.08f), CornerRadius(3.dp.toPx()))
    drawCircle(palette.cyan, h * 0.04f, Offset(w - pad * 2.2f, h * 0.845f))
}

internal fun DrawScope.qobuzPreview(palette: LevyraPalette, w: Float, h: Float) {
    val pad = w * 0.06f
    val gap = w * 0.035f
    previewLine(palette.text, pad, h * 0.075f, w * 0.3f, 5.dp.toPx())
    drawRoundRect(palette.text, Offset(pad, h * 0.12f), Size(w * 0.13f, h * 0.04f), CornerRadius(h * 0.02f))
    drawRoundRect(palette.panel, Offset(pad + w * 0.15f, h * 0.12f), Size(w * 0.4f, h * 0.04f), CornerRadius(h * 0.02f))
    drawRoundRect(palette.panel, Offset(pad + w * 0.57f, h * 0.12f), Size(w * 0.22f, h * 0.04f), CornerRadius(h * 0.02f))
    val cardTop = h * 0.2f
    drawRoundRect(palette.panel, Offset(pad * 0.6f, cardTop), Size(w - pad * 1.2f, h * 0.15f), CornerRadius(3.dp.toPx()))
    drawRoundRect(Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFFFF4FD8), Color(0xFFFFE680))), Offset(w * 0.56f, cardTop + h * 0.015f), Size(w * 0.36f, h * 0.12f), CornerRadius(2.dp.toPx()))
    previewLine(palette.text, pad, cardTop + h * 0.03f, w * 0.28f, 2.5.dp.toPx())
    previewLine(palette.muted, pad, cardTop + h * 0.06f, w * 0.36f, 1.4.dp.toPx())
    val gridTop = h * 0.4f
    previewLine(palette.text, pad, gridTop, w * 0.22f, 3.dp.toPx())
    val card = (w - pad - gap * 2) / 2.3f
    repeat(3) { index ->
        drawRoundRect(palette.panelSoft, Offset(pad + index * (card + gap), gridTop + h * 0.03f), Size(card, card), CornerRadius(2.dp.toPx()))
        previewLine(palette.text, pad + index * (card + gap), gridTop + h * 0.04f + card, card * 0.8f, 1.6.dp.toPx())
        previewLine(palette.cyan, pad + index * (card + gap), gridTop + h * 0.06f + card, card * 0.4f, 1.2.dp.toPx())
    }
    val tagTop = gridTop + h * 0.1f + card
    drawRoundRect(Brush.linearGradient(listOf(Color(0xFFE0A33A), Color(0xFFE2702B))), Offset(pad, tagTop), Size(w * 0.42f, h * 0.06f), CornerRadius(3.dp.toPx()))
    drawRoundRect(Brush.linearGradient(listOf(Color(0xFF7B2FF2), Color(0xFF4A12D6))), Offset(pad + w * 0.42f + gap, tagTop), Size(w * 0.42f, h * 0.06f), CornerRadius(3.dp.toPx()))
    val miniTop = h * 0.8f
    drawRoundRect(palette.panelSoft, Offset(pad * 0.6f, miniTop), Size(w - pad * 1.2f, h * 0.05f), CornerRadius(4.dp.toPx()))
    previewLine(palette.cyan, pad, miniTop + h * 0.047f, w * 0.18f, 1.dp.toPx())
    val navTop = h * 0.87f
    repeat(4) { index ->
        val x = pad + (w - pad * 2) * (index + 0.5f) / 4f
        drawCircle(if (index == 0) palette.text else palette.muted, 2.4.dp.toPx(), Offset(x, navTop + h * 0.03f))
    }
}

internal fun DrawScope.appleMusicPreview(palette: LevyraPalette, w: Float, h: Float) {
    val pad = w * 0.07f
    val gap = w * 0.04f
    previewLine(palette.text, pad, h * 0.075f, w * 0.34f, 5.dp.toPx())
    previewLine(palette.outline, pad, h * 0.11f, w - pad * 2, 0.5.dp.toPx())
    val heroW = w - pad - gap - w * 0.1f
    previewLine(palette.muted, pad, h * 0.14f, w * 0.2f, 1.5.dp.toPx())
    previewLine(palette.text, pad, h * 0.165f, w * 0.5f, 2.5.dp.toPx())
    drawRoundRect(Brush.linearGradient(listOf(palette.cyan, Color(0xFF8E2DE2))), Offset(pad, h * 0.2f), Size(heroW, heroW / 1.5f), CornerRadius(4.dp.toPx()))
    drawRoundRect(palette.panelSoft, Offset(pad + heroW + gap, h * 0.2f), Size(w, heroW / 1.5f), CornerRadius(4.dp.toPx()))
    val rowsTop = h * 0.2f + heroW / 1.5f + h * 0.05f
    previewLine(palette.text, pad, rowsTop, w * 0.42f, 3.dp.toPx())
    repeat(3) { index ->
        val y = rowsTop + h * 0.03f + index * h * 0.06f
        drawRoundRect(palette.panelSoft, Offset(pad, y), Size(h * 0.045f, h * 0.045f), CornerRadius(1.5.dp.toPx()))
        previewLine(palette.text, pad + h * 0.06f, y + h * 0.014f, w * 0.38f, 1.8.dp.toPx())
        previewLine(palette.muted, pad + h * 0.06f, y + h * 0.032f, w * 0.26f, 1.2.dp.toPx())
    }
    val miniTop = h * 0.79f
    drawRoundRect(palette.panel, Offset(pad, miniTop), Size(w - pad * 2, h * 0.045f), CornerRadius(h * 0.0225f))
    drawRoundRect(palette.panelSoft, Offset(pad * 1.6f, miniTop + h * 0.0085f), Size(h * 0.028f, h * 0.028f), CornerRadius(1.dp.toPx()))
    val navTop = h * 0.85f
    drawRoundRect(palette.panel, Offset(pad, navTop), Size(w - pad * 2, h * 0.075f), CornerRadius(h * 0.0375f))
    repeat(5) { index ->
        val x = pad + (w - pad * 2) * (index + 0.5f) / 5f
        drawCircle(if (index == 1) palette.cyan else palette.muted, 2.2.dp.toPx(), Offset(x, navTop + h * 0.03f))
    }
}

private fun skinPreviewPalette(skin: LevyraSkin): LevyraPalette = when (skin) {
    LevyraSkin.Nocturne78 -> Nocturne78Palette
    LevyraSkin.Prism -> prismPalette()
    LevyraSkin.Torque -> torquePalette()
    LevyraSkin.Ivory -> ivoryPalette()
    LevyraSkin.SpotifyStyle -> spotifyPalette()
    LevyraSkin.AppleMusicStyle -> appleMusicPalette()
    LevyraSkin.YouTubeMusicStyle -> youTubeMusicPalette()
    LevyraSkin.QobuzStyle -> qobuzPalette()
    LevyraSkin.Atelier -> atelierPalette()
    LevyraSkin.Mono -> monoPalette()
    LevyraSkin.Default -> LevyraThemes.levyraAura
}

private fun skinPreviewArtBrush(skin: LevyraSkin, palette: LevyraPalette): Brush = when (skin) {
    LevyraSkin.Nocturne78 -> Brush.linearGradient(listOf(palette.panelSoft, palette.panel))
    LevyraSkin.Prism -> Brush.linearGradient(listOf(palette.blue, palette.violet))
    LevyraSkin.Default -> Brush.linearGradient(listOf(palette.cyan, palette.violet))
    LevyraSkin.Torque -> Brush.linearGradient(listOf(palette.panelSoft, palette.panel))
    LevyraSkin.Ivory -> Brush.linearGradient(listOf(palette.panelSoft, palette.panel))
    LevyraSkin.SpotifyStyle -> Brush.linearGradient(listOf(palette.panelSoft, palette.panel))
    LevyraSkin.AppleMusicStyle -> Brush.linearGradient(listOf(palette.cyan, palette.panelSoft))
    LevyraSkin.YouTubeMusicStyle -> Brush.linearGradient(listOf(Color(0xFF7B1F3A), palette.panelSoft))
    LevyraSkin.QobuzStyle -> Brush.linearGradient(listOf(Color(0xFFE0A33A), Color(0xFFD9533B)))
    LevyraSkin.Atelier -> Brush.linearGradient(listOf(Color(0xFF9C94F0), Color(0xFF070708)))
    LevyraSkin.Mono -> Brush.linearGradient(listOf(MonoDesign.moodFocus, MonoDesign.base))
}

private fun DrawScope.drawSkinPreviewWash(skin: LevyraSkin, palette: LevyraPalette, frame: CornerRadius) {
    when (skin) {
        LevyraSkin.Prism -> drawRoundRect(Brush.verticalGradient(listOf(palette.blue.copy(alpha = 0.22f), palette.black), endY = size.height * 0.6f), cornerRadius = frame)
        LevyraSkin.Default -> drawRoundRect(Brush.verticalGradient(listOf(palette.violet.copy(alpha = 0.28f), palette.black), endY = size.height * 0.7f), cornerRadius = frame)
        LevyraSkin.Nocturne78, LevyraSkin.Torque, LevyraSkin.Ivory, LevyraSkin.SpotifyStyle, LevyraSkin.AppleMusicStyle, LevyraSkin.YouTubeMusicStyle, LevyraSkin.QobuzStyle, LevyraSkin.Atelier, LevyraSkin.Mono -> Unit
    }
}
