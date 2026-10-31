package com.luc4n3x.levyra.ui.homestudio

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.background
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.HomeBlock
import com.luc4n3x.levyra.domain.HomeSectionId
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.ui.HomeStylePreviewCanvas
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPalette
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.appleMusicPreview
import com.luc4n3x.levyra.ui.theme.atelierPreview
import com.luc4n3x.levyra.ui.theme.qobuzPreview
import com.luc4n3x.levyra.ui.theme.spotifyPreview
import com.luc4n3x.levyra.ui.theme.youTubeMusicPreview
import kotlin.math.min

@Immutable
internal class HomePreviewTones(
    val canvas: Color,
    val panel: Color,
    val soft: Color,
    val accent: Color,
    val second: Color,
    val text: Color,
    val muted: Color,
    val line: Color
)

@Composable
internal fun homePreviewTones(tokens: HomeStudioSkinTokens): HomePreviewTones {
    val scheme = MaterialTheme.colorScheme
    val palette = LevyraActivePalette
    return if (tokens.expressive) {
        HomePreviewTones(
            canvas = scheme.background,
            panel = scheme.surfaceContainerHigh,
            soft = scheme.surfaceContainerHighest,
            accent = scheme.primary,
            second = scheme.tertiary,
            text = LevyraText,
            muted = LevyraMuted,
            line = scheme.outlineVariant
        )
    } else {
        HomePreviewTones(
            canvas = palette.black,
            panel = palette.panel,
            soft = palette.panelSoft,
            accent = tokens.accent,
            second = palette.violet,
            text = palette.text,
            muted = palette.muted,
            line = palette.outline
        )
    }
}

internal enum class HomeSectionGlyph { Rows, Cards, Hero, Chips, Ranking, Quotes, Radio, Video, Dial, Circles, Orbit, Feed }

internal fun homeSectionGlyph(block: HomeBlock): HomeSectionGlyph = when (block) {
    HomeBlock.SkinFeed -> HomeSectionGlyph.Feed
    is HomeBlock.Section -> when (block.id) {
        HomeSectionId.GenreChips -> HomeSectionGlyph.Chips
        HomeSectionId.SpeedDial -> HomeSectionGlyph.Dial
        HomeSectionId.Spotlight, HomeSectionId.Discovery -> HomeSectionGlyph.Hero
        HomeSectionId.PersonalOrbit -> HomeSectionGlyph.Orbit
        HomeSectionId.FavoriteRadio -> HomeSectionGlyph.Radio
        HomeSectionId.EditorialCollections, HomeSectionId.AlbumsForYou, HomeSectionId.MoreSections -> HomeSectionGlyph.Cards
        HomeSectionId.QuickPicks, HomeSectionId.ForgottenFavorites, HomeSectionId.NewReleases, HomeSectionId.ReleaseRadar -> HomeSectionGlyph.Rows
        HomeSectionId.TrendingArtists, HomeSectionId.SimilarArtists -> HomeSectionGlyph.Circles
        HomeSectionId.MusicVideos -> HomeSectionGlyph.Video
        HomeSectionId.Resonance -> HomeSectionGlyph.Quotes
        HomeSectionId.Charts -> HomeSectionGlyph.Ranking
    }
}

@Composable
internal fun HomeSectionThumbnail(
    block: HomeBlock,
    tokens: HomeStudioSkinTokens,
    tones: HomePreviewTones,
    modifier: Modifier = Modifier
) {
    val glyph = homeSectionGlyph(block)
    val radius = tokens.previewRadius
    Canvas(modifier.clip(tokens.thumbShape).background(tones.canvas)) {
        mirrorForRtl {
            val inset = size.minDimension * 0.14f
            drawHomeSectionGlyph(glyph, inset, inset, size.width - inset * 2, size.height - inset * 2, tones, min(radius.toPx(), inset))
        }
    }
}

@Composable
internal fun HomeOriginalPhone(idiom: HomePreviewIdiom, style: HomeStyle, tones: HomePreviewTones, modifier: Modifier = Modifier) {
    if (idiom == HomePreviewIdiom.Expressive) {
        HomeStylePreviewCanvas(style = style, modifier = modifier)
        return
    }
    val palette = LevyraActivePalette
    Canvas(modifier) {
        mirrorForRtl {
            drawRect(tones.canvas)
            drawOriginalHome(idiom, palette, tones)
        }
    }
}

@Composable
internal fun HomeCustomPhone(
    blocks: List<HomeBlock>,
    highlighted: HomeBlock?,
    tokens: HomeStudioSkinTokens,
    tones: HomePreviewTones,
    insertProgress: () -> Float,
    modifier: Modifier = Modifier
) {
    val window = remember(blocks, highlighted) { customPhoneWindow(blocks, highlighted) }
    val idiom = tokens.idiom
    val radius = tokens.previewRadius
    Canvas(modifier) {
        mirrorForRtl {
            drawIdiomBackdrop(idiom, tones)
            drawCustomStack(window, idiom, tones, radius.toPx(), insertProgress())
            drawIdiomNav(idiom, tones)
        }
    }
}

@Composable
internal fun HomeLevyraPhone(
    blocks: List<HomeBlock>,
    tokens: HomeStudioSkinTokens,
    tones: HomePreviewTones,
    modifier: Modifier = Modifier
) {
    val glyphs = remember(blocks) { blocks.map(::homeSectionGlyph) }
    val idiom = tokens.idiom
    val radius = tokens.previewRadius
    Canvas(modifier) {
        mirrorForRtl {
            drawIdiomBackdrop(idiom, tones)
            drawLevyraRhythm(glyphs, idiom, tones, radius.toPx())
            drawIdiomNav(idiom, tones)
        }
    }
}

private class CustomPhoneWindow(val glyphs: List<HomeSectionGlyph>, val highlight: Int)

private const val CustomPhoneSlots = 5

private fun customPhoneWindow(blocks: List<HomeBlock>, highlighted: HomeBlock?): CustomPhoneWindow {
    val requested = highlighted?.let { blocks.indexOf(it) }?.takeIf { it >= 0 }
    val start = if (requested != null && requested >= CustomPhoneSlots) requested - CustomPhoneSlots + 1 else 0
    val shown = blocks.drop(start).take(CustomPhoneSlots)
    val highlight = requested?.minus(start) ?: if (shown.size > 1) 1 else 0
    return CustomPhoneWindow(shown.map(::homeSectionGlyph), highlight)
}

private fun DrawScope.mirrorForRtl(block: DrawScope.() -> Unit) {
    if (layoutDirection == LayoutDirection.Rtl) {
        scale(-1f, 1f, center) { block() }
    } else {
        block()
    }
}

private fun DrawScope.pill(color: Color, x: Float, y: Float, width: Float, height: Float) =
    drawRoundRect(color, Offset(x, y), Size(width, height), CornerRadius(height / 2f))

private fun DrawScope.tile(color: Color, x: Float, y: Float, width: Float, height: Float, radius: Float) =
    drawRoundRect(color, Offset(x, y), Size(width, height), CornerRadius(radius))

internal fun DrawScope.drawHomeSectionGlyph(
    glyph: HomeSectionGlyph,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    tones: HomePreviewTones,
    radius: Float
) {
    clipRect(x, y, x + width, y + height) {
        when (glyph) {
            HomeSectionGlyph.Rows -> glyphRows(x, y, width, height, tones, radius)
            HomeSectionGlyph.Cards -> glyphCards(x, y, width, height, tones, radius)
            HomeSectionGlyph.Hero -> glyphHero(x, y, width, height, tones, radius)
            HomeSectionGlyph.Chips -> glyphChips(x, y, width, height, tones)
            HomeSectionGlyph.Ranking -> glyphRanking(x, y, width, height, tones)
            HomeSectionGlyph.Quotes -> glyphQuotes(x, y, width, height, tones, radius)
            HomeSectionGlyph.Radio -> glyphRadio(x, y, width, height, tones)
            HomeSectionGlyph.Video -> glyphVideo(x, y, width, height, tones, radius)
            HomeSectionGlyph.Dial -> glyphDial(x, y, width, height, tones, radius)
            HomeSectionGlyph.Circles -> glyphCircles(x, y, width, height, tones)
            HomeSectionGlyph.Orbit -> glyphOrbit(x, y, width, height, tones)
            HomeSectionGlyph.Feed -> glyphFeed(x, y, width, height, tones, radius)
        }
    }
}

private fun DrawScope.glyphRows(x: Float, y: Float, width: Float, height: Float, tones: HomePreviewTones, radius: Float) {
    val rowHeight = height / 3f
    val art = rowHeight * 0.74f
    repeat(3) { index ->
        val top = y + index * rowHeight + (rowHeight - art) / 2f
        tile(if (index == 0) tones.accent else tones.soft, x, top, art, art, min(radius, art / 3f))
        pill(tones.text.copy(alpha = 0.82f), x + art * 1.3f, top + art * 0.14f, width * (0.58f - index * 0.06f), art * 0.24f)
        pill(tones.muted.copy(alpha = 0.7f), x + art * 1.3f, top + art * 0.58f, width * 0.36f, art * 0.18f)
    }
}

private fun DrawScope.glyphCards(x: Float, y: Float, width: Float, height: Float, tones: HomePreviewTones, radius: Float) {
    val gap = width * 0.05f
    val side = min((width - gap * 2) / 3f, height * 0.72f)
    val top = y + (height - side * 1.3f).coerceAtLeast(0f) / 2f
    repeat(3) { index ->
        val left = x + index * (side + gap)
        tile(if (index == 0) tones.accent.copy(alpha = 0.85f) else tones.soft, left, top, side, side, radius)
        pill(tones.text.copy(alpha = 0.72f), left, top + side * 1.1f, side * 0.8f, side * 0.13f)
    }
}

private fun DrawScope.glyphHero(x: Float, y: Float, width: Float, height: Float, tones: HomePreviewTones, radius: Float) {
    drawRoundRect(
        Brush.linearGradient(listOf(tones.accent, tones.second), Offset(x, y), Offset(x + width, y + height)),
        Offset(x, y),
        Size(width, height),
        CornerRadius(radius)
    )
    pill(Color.White.copy(alpha = 0.92f), x + width * 0.08f, y + height * 0.56f, width * 0.56f, height * 0.11f)
    pill(Color.White.copy(alpha = 0.62f), x + width * 0.08f, y + height * 0.74f, width * 0.36f, height * 0.08f)
    drawCircle(Color.White, min(width, height) * 0.13f, Offset(x + width * 0.84f, y + height * 0.72f))
}

private fun DrawScope.glyphChips(x: Float, y: Float, width: Float, height: Float, tones: HomePreviewTones) {
    val chipHeight = height * 0.3f
    val gap = width * 0.04f
    val rows = listOf(listOf(0.36f, 0.26f, 0.3f), listOf(0.24f, 0.42f, 0.26f))
    rows.forEachIndexed { rowIndex, widths ->
        var left = x
        val top = y + height * 0.12f + rowIndex * (chipHeight + height * 0.12f)
        widths.forEachIndexed { index, fraction ->
            val chip = width * fraction
            pill(if (rowIndex == 0 && index == 0) tones.accent else tones.soft, left, top, chip, chipHeight)
            left += chip + gap
        }
    }
}

private fun DrawScope.glyphRanking(x: Float, y: Float, width: Float, height: Float, tones: HomePreviewTones) {
    val rowHeight = height / 4f
    repeat(4) { index ->
        val center = y + rowHeight * (index + 0.5f)
        pill(tones.accent.copy(alpha = 1f - index * 0.2f), x, center - rowHeight * 0.17f, width * 0.08f, rowHeight * 0.34f)
        pill(tones.text.copy(alpha = 0.78f), x + width * 0.16f, center - rowHeight * 0.13f, width * (0.8f - index * 0.15f), rowHeight * 0.26f)
    }
}

private fun DrawScope.glyphQuotes(x: Float, y: Float, width: Float, height: Float, tones: HomePreviewTones, radius: Float) {
    val tail = height * 0.14f
    val bubble = height - tail
    tile(tones.soft, x, y, width, bubble, radius.coerceAtLeast(height * 0.1f))
    val point = Path().apply {
        moveTo(x + width * 0.16f, y + bubble - 1f)
        lineTo(x + width * 0.16f, y + height)
        lineTo(x + width * 0.34f, y + bubble - 1f)
        close()
    }
    drawPath(point, tones.soft)
    val dot = min(width, height) * 0.075f
    val quoteY = y + bubble * 0.26f
    drawCircle(tones.accent, dot, Offset(x + width * 0.13f, quoteY))
    drawCircle(tones.accent, dot, Offset(x + width * 0.13f + dot * 2.5f, quoteY))
    pill(tones.text.copy(alpha = 0.8f), x + width * 0.08f, y + bubble * 0.5f, width * 0.8f, bubble * 0.1f)
    pill(tones.muted.copy(alpha = 0.8f), x + width * 0.08f, y + bubble * 0.7f, width * 0.54f, bubble * 0.09f)
}

private fun DrawScope.glyphRadio(x: Float, y: Float, width: Float, height: Float, tones: HomePreviewTones) {
    val center = Offset(x + width / 2f, y + height / 2f)
    val base = min(width, height) * 0.5f
    val stroke = (base * 0.09f).coerceAtLeast(1f)
    drawCircle(tones.accent, base * 0.2f, center)
    drawCircle(tones.accent.copy(alpha = 0.7f), base * 0.52f, center, style = Stroke(stroke))
    drawCircle(tones.accent.copy(alpha = 0.32f), base * 0.86f, center, style = Stroke(stroke))
}

private fun DrawScope.glyphVideo(x: Float, y: Float, width: Float, height: Float, tones: HomePreviewTones, radius: Float) {
    val frameWidth = min(width * 0.8f, height * 0.92f * 16f / 9f)
    val frameHeight = frameWidth * 9f / 16f
    val top = y + (height - frameHeight) / 2f
    tile(tones.soft, x + frameWidth + width * 0.05f, top, frameWidth, frameHeight, radius)
    drawRoundRect(
        Brush.verticalGradient(listOf(tones.accent.copy(alpha = 0.72f), tones.second.copy(alpha = 0.92f)), top, top + frameHeight),
        Offset(x, top),
        Size(frameWidth, frameHeight),
        CornerRadius(radius)
    )
    val side = frameHeight * 0.17f
    val centerX = x + frameWidth / 2f
    val centerY = top + frameHeight / 2f
    val play = Path().apply {
        moveTo(centerX - side * 0.8f, centerY - side)
        lineTo(centerX + side, centerY)
        lineTo(centerX - side * 0.8f, centerY + side)
        close()
    }
    drawPath(play, Color.White)
}

private fun DrawScope.glyphDial(x: Float, y: Float, width: Float, height: Float, tones: HomePreviewTones, radius: Float) {
    val gap = width * 0.05f
    val tileWidth = (width - gap * 2) / 3f
    val tileHeight = (height - gap) / 2f
    repeat(6) { index ->
        val column = index % 3
        val row = index / 3
        val tint = when (index) {
            0 -> tones.accent
            4 -> tones.second.copy(alpha = 0.8f)
            else -> tones.soft
        }
        tile(tint, x + column * (tileWidth + gap), y + row * (tileHeight + gap), tileWidth, tileHeight, radius)
    }
}

private fun DrawScope.glyphCircles(x: Float, y: Float, width: Float, height: Float, tones: HomePreviewTones) {
    val circleRadius = min(width / 4.6f, height / 2.6f)
    val centerY = y + height / 2f
    listOf(tones.soft, tones.second.copy(alpha = 0.9f), tones.accent).forEachIndexed { index, tint ->
        drawCircle(tint, circleRadius, Offset(x + circleRadius + index * circleRadius * 1.55f, centerY))
    }
}

private fun DrawScope.glyphOrbit(x: Float, y: Float, width: Float, height: Float, tones: HomePreviewTones) {
    val center = Offset(x + width / 2f, y + height / 2f)
    val ring = min(width, height) * 0.4f
    drawCircle(tones.muted.copy(alpha = 0.55f), ring, center, style = Stroke((ring * 0.08f).coerceAtLeast(1f)))
    drawCircle(tones.accent, ring * 0.42f, center)
    drawCircle(tones.second, ring * 0.22f, center + Offset(ring * 0.72f, -ring * 0.7f))
    drawCircle(tones.text.copy(alpha = 0.8f), ring * 0.16f, center + Offset(-ring * 0.9f, ring * 0.44f))
}

private fun DrawScope.glyphFeed(x: Float, y: Float, width: Float, height: Float, tones: HomePreviewTones, radius: Float) {
    pill(tones.text.copy(alpha = 0.85f), x, y, width * 0.46f, height * 0.09f)
    drawRoundRect(
        Brush.linearGradient(listOf(tones.accent, tones.second), Offset(x, y), Offset(x + width, y + height)),
        Offset(x, y + height * 0.16f),
        Size(width, height * 0.42f),
        CornerRadius(radius)
    )
    repeat(2) { index ->
        val top = y + height * (0.66f + index * 0.18f)
        tile(tones.soft, x, top, height * 0.13f, height * 0.13f, radius / 2f)
        pill(tones.muted.copy(alpha = 0.8f), x + height * 0.2f, top + height * 0.03f, width * 0.6f, height * 0.07f)
    }
}

private fun DrawScope.drawIdiomBackdrop(idiom: HomePreviewIdiom, tones: HomePreviewTones) {
    val w = size.width
    val h = size.height
    drawRect(tones.canvas)
    when (idiom) {
        HomePreviewIdiom.Spotify, HomePreviewIdiom.YouTubeMusic ->
            drawRect(Brush.verticalGradient(listOf(tones.second.copy(alpha = 0.34f), Color.Transparent), endY = h * 0.34f), size = Size(w, h * 0.34f))
        HomePreviewIdiom.Prism ->
            drawRect(Brush.verticalGradient(listOf(tones.accent.copy(alpha = 0.24f), Color.Transparent), endY = h * 0.5f), size = Size(w, h * 0.5f))
        HomePreviewIdiom.Expressive ->
            drawCircle(tones.second.copy(alpha = 0.14f), w * 0.48f, Offset(w * 0.9f, h * 0.05f))
        HomePreviewIdiom.Torque -> {
            val band = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.06f)
                lineTo(0f, h * 0.12f)
                close()
            }
            drawPath(band, tones.accent)
        }
        HomePreviewIdiom.Nocturne, HomePreviewIdiom.Ivory, HomePreviewIdiom.AppleMusic, HomePreviewIdiom.Qobuz, HomePreviewIdiom.Atelier -> Unit
    }
}

private fun DrawScope.drawIdiomTitle(idiom: HomePreviewIdiom, tones: HomePreviewTones, x: Float, y: Float, width: Float) {
    val h = size.height
    when (idiom) {
        HomePreviewIdiom.Nocturne -> {
            pill(tones.text.copy(alpha = 0.86f), x, y, width * 0.46f, h * 0.008f)
            drawLine(tones.line, Offset(x, y + h * 0.018f), Offset(x + width, y + h * 0.018f), 1f)
        }
        HomePreviewIdiom.Torque -> {
            drawRect(tones.accent, Offset(x, y - h * 0.002f), Size(width * 0.05f, h * 0.016f))
            drawRect(tones.text, Offset(x + width * 0.09f, y), Size(width * 0.5f, h * 0.012f))
        }
        HomePreviewIdiom.AppleMusic -> {
            pill(tones.text, x, y, width * 0.52f, h * 0.013f)
            drawLine(
                tones.accent,
                Offset(x + width * 0.56f, y),
                Offset(x + width * 0.59f, y + h * 0.0065f),
                1.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                tones.accent,
                Offset(x + width * 0.59f, y + h * 0.0065f),
                Offset(x + width * 0.56f, y + h * 0.013f),
                1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        else -> pill(tones.text.copy(alpha = 0.88f), x, y, width * 0.48f, h * 0.012f)
    }
}

private fun DrawScope.drawIdiomNav(idiom: HomePreviewIdiom, tones: HomePreviewTones) {
    val w = size.width
    val h = size.height
    val top = h * 0.92f
    when (idiom) {
        HomePreviewIdiom.AppleMusic, HomePreviewIdiom.Ivory, HomePreviewIdiom.Expressive ->
            pill(tones.panel, w * 0.08f, top, w * 0.84f, h * 0.06f)
        HomePreviewIdiom.Nocturne, HomePreviewIdiom.Torque ->
            drawLine(tones.line, Offset(0f, top), Offset(w, top), 1f)
        else -> drawRect(tones.panel, Offset(0f, top), Size(w, h - top))
    }
    repeat(4) { index ->
        val x = w * (0.2f + index * 0.2f)
        drawCircle(if (index == 0) tones.accent else tones.muted.copy(alpha = 0.6f), w * 0.016f, Offset(x, top + h * 0.03f))
    }
}

private fun DrawScope.drawCustomStack(
    window: CustomPhoneWindow,
    idiom: HomePreviewIdiom,
    tones: HomePreviewTones,
    radius: Float,
    insert: Float
) {
    val w = size.width
    val h = size.height
    val pad = w * 0.08f
    val inner = w - pad * 2
    drawIdiomTitle(idiom, tones, pad, h * 0.07f, inner)
    val listTop = h * 0.115f
    val listBottom = h * 0.895f
    val count = window.glyphs.size.coerceAtLeast(1)
    val gap = h * 0.012f
    val slot = (listBottom - listTop - gap * (count - 1)) / count
    window.glyphs.forEachIndexed { index, glyph ->
        val highlighted = index == window.highlight
        val hover = if (highlighted) inner * 0.07f * (1f - insert) else 0f
        val top = listTop + index * (slot + gap)
        val left = pad + hover
        val cardRadius = radius.coerceAtMost(slot / 3f)
        tile(if (highlighted) tones.accent.copy(alpha = 0.2f) else tones.panel, left, top, inner, slot, cardRadius)
        if (highlighted) {
            drawRoundRect(tones.accent, Offset(left, top), Size(inner, slot), CornerRadius(cardRadius), style = Stroke(1.5.dp.toPx()))
        }
        val grip = inner * 0.07f
        repeat(2) { line ->
            val y = top + slot * (0.42f + line * 0.16f)
            drawLine(tones.muted.copy(alpha = 0.8f), Offset(left + grip * 0.5f, y), Offset(left + grip * 1.3f, y), 1.dp.toPx(), cap = StrokeCap.Round)
        }
        val inset = slot * 0.14f
        val glyphLeft = left + grip * 1.9f
        val glyphWidth = inner - grip * 1.9f - inset - if (highlighted) inner * 0.18f else 0f
        drawHomeSectionGlyph(glyph, glyphLeft, top + inset, glyphWidth, slot - inset * 2, tones, cardRadius / 2f)
        if (highlighted) {
            val center = Offset(left + inner - inner * 0.1f, top + slot / 2f)
            val mark = min(inner * 0.07f, slot * 0.2f)
            drawCircle(tones.accent, mark, center)
            val arm = mark * 0.55f
            val markStroke = 1.dp.toPx()
            drawLine(tones.canvas, center - Offset(arm, 0f), center + Offset(arm, 0f), markStroke, cap = StrokeCap.Round)
            drawLine(tones.canvas, center - Offset(0f, arm), center + Offset(0f, arm), markStroke, cap = StrokeCap.Round)
        }
    }
}

private fun rhythmFor(glyph: HomeSectionGlyph): Float = when (glyph) {
    HomeSectionGlyph.Chips -> 0.05f
    HomeSectionGlyph.Hero, HomeSectionGlyph.Feed -> 0.17f
    HomeSectionGlyph.Dial -> 0.11f
    HomeSectionGlyph.Rows, HomeSectionGlyph.Ranking -> 0.14f
    HomeSectionGlyph.Cards, HomeSectionGlyph.Video, HomeSectionGlyph.Quotes -> 0.11f
    HomeSectionGlyph.Radio, HomeSectionGlyph.Circles, HomeSectionGlyph.Orbit -> 0.08f
}

private fun DrawScope.drawLevyraRhythm(glyphs: List<HomeSectionGlyph>, idiom: HomePreviewIdiom, tones: HomePreviewTones, radius: Float) {
    val w = size.width
    val h = size.height
    val pad = w * 0.08f
    val inner = w - pad * 2
    val limit = h * 0.91f
    var y = h * 0.07f
    clipRect(0f, 0f, w, limit) {
        for (glyph in glyphs) {
            if (y >= limit) break
            if (glyph != HomeSectionGlyph.Chips) {
                drawIdiomTitle(idiom, tones, pad, y, inner * if (glyph == HomeSectionGlyph.Hero) 0.8f else 1f)
                y += h * 0.024f
            }
            val blockHeight = h * rhythmFor(glyph)
            drawHomeSectionGlyph(glyph, pad, y, inner, blockHeight, tones, radius)
            y += blockHeight + h * 0.024f
        }
    }
}

private fun DrawScope.drawOriginalHome(idiom: HomePreviewIdiom, palette: LevyraPalette, tones: HomePreviewTones) {
    val w = size.width
    val h = size.height
    when (idiom) {
        HomePreviewIdiom.Spotify -> spotifyPreview(palette, w, h)
        HomePreviewIdiom.AppleMusic -> appleMusicPreview(palette, w, h)
        HomePreviewIdiom.YouTubeMusic -> youTubeMusicPreview(palette, w, h)
        HomePreviewIdiom.Qobuz -> qobuzPreview(palette, w, h)
        HomePreviewIdiom.Atelier -> atelierPreview(palette, w, h)
        HomePreviewIdiom.Nocturne -> nocturneHome(tones)
        HomePreviewIdiom.Prism -> prismHome(tones)
        HomePreviewIdiom.Torque -> torqueHome(tones)
        HomePreviewIdiom.Ivory -> ivoryHome(tones)
        HomePreviewIdiom.Expressive -> Unit
    }
}

private fun DrawScope.nocturneHome(tones: HomePreviewTones) {
    val w = size.width
    val h = size.height
    val pad = w * 0.09f
    val inner = w - pad * 2
    pill(tones.muted, pad, h * 0.07f, inner * 0.34f, h * 0.007f)
    pill(tones.text, pad, h * 0.09f, inner * 0.72f, h * 0.018f)
    drawLine(tones.line, Offset(pad, h * 0.13f), Offset(pad + inner, h * 0.13f), 1f)
    tile(tones.panel, pad, h * 0.15f, inner, h * 0.25f, 3.dp.toPx())
    drawCircle(tones.accent, inner * 0.2f, Offset(pad + inner * 0.72f, h * 0.275f), style = Stroke(1.5.dp.toPx()))
    drawCircle(tones.accent, inner * 0.03f, Offset(pad + inner * 0.72f, h * 0.275f))
    pill(tones.text.copy(alpha = 0.86f), pad + inner * 0.08f, h * 0.25f, inner * 0.34f, h * 0.012f)
    pill(tones.muted, pad + inner * 0.08f, h * 0.28f, inner * 0.24f, h * 0.008f)
    repeat(4) { index ->
        val y = h * (0.45f + index * 0.1f)
        drawLine(tones.line, Offset(pad, y), Offset(pad + inner, y), 1f)
        tile(tones.soft, pad, y + h * 0.018f, h * 0.06f, h * 0.06f, 2.dp.toPx())
        pill(tones.text.copy(alpha = 0.82f), pad + h * 0.08f, y + h * 0.03f, inner * 0.46f, h * 0.01f)
        pill(if (index == 0) tones.accent else tones.muted, pad + h * 0.08f, y + h * 0.054f, inner * 0.28f, h * 0.007f)
    }
    drawLine(tones.line, Offset(0f, h * 0.92f), Offset(w, h * 0.92f), 1f)
    repeat(4) { index ->
        drawRect(if (index == 0) tones.accent else tones.muted.copy(alpha = 0.6f), Offset(w * (0.18f + index * 0.2f), h * 0.955f), Size(w * 0.04f, 1.5.dp.toPx()))
    }
}

private fun DrawScope.prismHome(tones: HomePreviewTones) {
    val w = size.width
    val h = size.height
    val pad = w * 0.08f
    val inner = w - pad * 2
    val radius = 6.dp.toPx()
    drawRect(Brush.verticalGradient(listOf(tones.accent.copy(alpha = 0.3f), Color.Transparent), endY = h * 0.55f), size = Size(w, h * 0.55f))
    pill(tones.text, pad, h * 0.07f, inner * 0.56f, h * 0.02f)
    drawRoundRect(
        Brush.linearGradient(listOf(tones.accent, tones.second), Offset(pad, h * 0.12f), Offset(pad + inner, h * 0.36f)),
        Offset(pad, h * 0.12f),
        Size(inner, h * 0.24f),
        CornerRadius(radius * 2f)
    )
    drawCircle(Color.White, inner * 0.09f, Offset(pad + inner * 0.82f, h * 0.3f))
    pill(Color.White.copy(alpha = 0.9f), pad + inner * 0.08f, h * 0.285f, inner * 0.44f, h * 0.013f)
    val half = (inner - pad * 0.6f) / 2f
    repeat(4) { index ->
        val x = pad + index % 2 * (half + pad * 0.6f)
        val y = h * 0.39f + index / 2 * h * 0.08f
        tile(tones.panel, x, y, half, h * 0.068f, radius)
        tile(tones.soft, x, y, h * 0.068f, h * 0.068f, radius)
    }
    pill(tones.text.copy(alpha = 0.82f), pad, h * 0.575f, inner * 0.42f, h * 0.014f)
    val side = (inner - pad) / 2.4f
    repeat(3) { index ->
        tile(if (index == 0) tones.second.copy(alpha = 0.7f) else tones.panel, pad + index * (side + pad * 0.5f), h * 0.61f, side, side, radius)
    }
    pill(tones.panel, w * 0.08f, h * 0.91f, w * 0.84f, h * 0.06f)
    repeat(4) { index ->
        drawCircle(if (index == 0) tones.accent else tones.muted.copy(alpha = 0.6f), w * 0.016f, Offset(w * (0.2f + index * 0.2f), h * 0.94f))
    }
}

private fun DrawScope.torqueHome(tones: HomePreviewTones) {
    val w = size.width
    val h = size.height
    val pad = w * 0.07f
    val inner = w - pad * 2
    val rise = Path().apply {
        moveTo(0f, 0f)
        lineTo(w, 0f)
        lineTo(w, h * 0.24f)
        lineTo(0f, h * 0.38f)
        close()
    }
    drawPath(rise, tones.accent)
    drawRect(tones.text, Offset(pad, h * 0.08f), Size(inner * 0.72f, h * 0.022f))
    drawRect(tones.text.copy(alpha = 0.78f), Offset(pad, h * 0.12f), Size(inner * 0.48f, h * 0.014f))
    drawCircle(tones.canvas, w * 0.08f, Offset(pad + w * 0.08f, h * 0.24f))
    drawCircle(tones.text, w * 0.08f, Offset(pad + w * 0.08f, h * 0.24f), style = Stroke(1.5.dp.toPx()))
    drawRect(tones.text, Offset(pad, h * 0.42f), Size(inner * 0.44f, h * 0.014f))
    val side = (inner - pad) / 3f
    repeat(3) { index ->
        drawRect(if (index == 0) tones.accent else tones.soft, Offset(pad + index * (side + pad * 0.5f), h * 0.45f), Size(side, side))
    }
    repeat(3) { index ->
        val y = h * 0.45f + side + h * (0.04f + index * 0.07f)
        drawRect(tones.accent, Offset(pad, y), Size(inner * 0.05f, h * 0.04f))
        drawRect(tones.text.copy(alpha = 0.84f), Offset(pad + inner * 0.1f, y + h * 0.004f), Size(inner * 0.6f, h * 0.012f))
        drawRect(tones.muted, Offset(pad + inner * 0.1f, y + h * 0.026f), Size(inner * 0.36f, h * 0.008f))
    }
    drawLine(tones.line, Offset(0f, h * 0.92f), Offset(w, h * 0.92f), 1f)
    drawRect(tones.accent, Offset(w * 0.08f, h * 0.92f), Size(w * 0.2f, 2.dp.toPx()))
}

private fun DrawScope.ivoryHome(tones: HomePreviewTones) {
    val w = size.width
    val h = size.height
    val pad = w * 0.09f
    val inner = w - pad * 2
    val radius = 10.dp.toPx()
    pill(tones.text, pad, h * 0.07f, inner * 0.62f, h * 0.022f)
    pill(tones.muted, pad, h * 0.105f, inner * 0.38f, h * 0.01f)
    tile(tones.panel, pad, h * 0.14f, inner, h * 0.27f, radius * 1.6f)
    val quarter = inner * 0.34f
    drawArc(tones.accent, 180f, 90f, true, Offset(pad + inner - quarter, h * 0.41f - quarter), Size(quarter * 2f, quarter * 2f))
    pill(tones.text.copy(alpha = 0.86f), pad + inner * 0.1f, h * 0.2f, inner * 0.44f, h * 0.014f)
    val half = (inner - pad * 0.6f) / 2f
    repeat(4) { index ->
        val x = pad + index % 2 * (half + pad * 0.6f)
        val y = h * 0.44f + index / 2 * h * 0.2f
        tile(if (index == 1) tones.soft else tones.panel, x, y, half, h * 0.18f, radius)
        pill(tones.text.copy(alpha = 0.72f), x + half * 0.14f, y + h * 0.13f, half * 0.6f, h * 0.01f)
    }
    drawRect(tones.panel, Offset(0f, h * 0.91f), Size(w, h * 0.09f))
    val bump = Path().apply {
        val left = w * 0.34f
        val right = w * 0.66f
        val base = h
        val crest = h * 0.885f
        moveTo(left - w * 0.06f, base)
        cubicTo(left, base, left, crest, left + w * 0.04f, crest)
        lineTo(right - w * 0.04f, crest)
        cubicTo(right, crest, right, base, right + w * 0.06f, base)
        close()
    }
    drawPath(bump, tones.accent)
}
