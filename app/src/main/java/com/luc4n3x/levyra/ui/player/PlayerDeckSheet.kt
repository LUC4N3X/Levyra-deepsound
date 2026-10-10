package com.luc4n3x.levyra.ui.player

import androidx.compose.runtime.getValue
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import com.luc4n3x.levyra.ui.playerMix
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.playerDeckShortLabels
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm

private val DeckThumbWidth = 64.dp
private val DeckPreviewRenderWidth = 148.dp
private val DeckRowGap = 3.dp
private val DeckIdleSurface = Color(0xFF1A1B1F)
private val DeckPreviewSelectedCorner = 48.dp
private val DeckPreviewCorner = 28.dp
private const val DeckPreviewAspect = 0.78f

@Composable
internal fun PlayerDeckSheet(
    track: Track,
    artworkUrl: String,
    selected: PlayerVisualMode,
    surfaces: PlayerSurfaceTokens,
    accent: Color,
    animated: Boolean,
    onSelect: (PlayerVisualMode) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    PlayerSheetFrame(
        surfaces = surfaces,
        animated = animated,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = LevyraPlayerDesign.SpaceXl),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
        ) {
            Column(modifier = Modifier.padding(horizontal = LevyraPlayerDesign.Gutter)) {
                Text(
                    text = strings.playerDeck,
                    color = surfaces.content,
                    fontSize = 22.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(22.sp),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = strings.playerDeckSubtitle,
                    color = surfaces.contentMuted,
                    fontSize = 12.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(12.sp),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LevyraPlayerDesign.Gutter)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(DeckRowGap)
            ) {
                PlayerDeckOrder.forEachIndexed { index, mode ->
                    PlayerDeckRow(
                        mode = mode,
                        selected = mode == selected,
                        shape = levyraGroupedListShape(index, PlayerDeckOrder.size),
                        track = track,
                        artworkUrl = artworkUrl,
                        accent = accent,
                        surfaces = surfaces,
                        animated = animated,
                        onClick = { onSelect(mode) }
                    )
                }
            }
            Text(
                text = strings.playerDeckLandscapeNote,
                color = surfaces.contentFaint,
                fontSize = 11.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(11.sp),
                maxLines = 2,
                modifier = Modifier.padding(horizontal = LevyraPlayerDesign.Gutter)
            )
        }
    }
}

@Composable
private fun PlayerDeckRow(
    mode: PlayerVisualMode,
    selected: Boolean,
    shape: Shape,
    track: Track,
    artworkUrl: String,
    accent: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val fill by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.22f) else DeckIdleSurface,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(220)),
        label = "player-deck-row-fill"
    )
    val thumbCorner = levyraExpressiveToggleCorner(
        checked = selected,
        unchecked = 10.dp,
        checkedCorner = 20.dp,
        label = "player-deck-thumb-corner"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = DeckThumbWidth, height = DeckThumbWidth / DeckPreviewAspect)
                .clip(RoundedCornerShape(thumbCorner))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(width = DeckPreviewRenderWidth, height = DeckPreviewRenderWidth / DeckPreviewAspect)
                    .graphicsLayer {
                        val scale = DeckThumbWidth / DeckPreviewRenderWidth
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                PlayerDeckPreview(
                    mode = mode,
                    track = track,
                    artworkUrl = artworkUrl,
                    accent = accent
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = playerDeckLabel(mode, strings),
                color = if (selected) accent.playerMix(Color.White, 0.55f) else surfaces.content,
                fontSize = 17.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(17.sp),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = playerDeckHint(mode, strings),
                color = surfaces.contentMuted,
                fontSize = 13.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(13.sp),
                fontWeight = FontWeight.Medium
            )
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (selected) accent else Color.Transparent)
                .border(2.dp, if (selected) accent else surfaces.contentFaint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = surfaces.heroContent,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

internal fun playerDeckLabel(mode: PlayerVisualMode, strings: LevyraStrings): String {
    val labels = playerDeckShortLabels(strings.code)
    return when (mode) {
        PlayerVisualMode.CanvasImmersive -> labels.immersive
        PlayerVisualMode.CanvasCard -> labels.canvasCard
        PlayerVisualMode.Artwork -> labels.artwork
        PlayerVisualMode.Editorial -> labels.editorial
        PlayerVisualMode.Pulse -> labels.pulse
    }
}

internal fun playerDeckHint(mode: PlayerVisualMode, strings: LevyraStrings): String = when (mode) {
    PlayerVisualMode.CanvasImmersive -> strings.playerDeckImmersiveHint
    PlayerVisualMode.CanvasCard -> strings.playerDeckCardHint
    PlayerVisualMode.Artwork -> strings.playerDeckArtworkHint
    PlayerVisualMode.Editorial -> strings.playerDeckEditorialHint
    PlayerVisualMode.Pulse -> strings.playerDeckPulseHint
}

@Composable
private fun PlayerDeckPreview(
    mode: PlayerVisualMode,
    track: Track,
    artworkUrl: String,
    accent: Color
) {
    when (mode) {
        PlayerVisualMode.CanvasImmersive -> ImmersivePreview(track, artworkUrl, accent)
        PlayerVisualMode.CanvasCard -> CardPreview(track, artworkUrl, accent, glow = true)
        PlayerVisualMode.Artwork -> CardPreview(track, artworkUrl, accent, glow = false)
        PlayerVisualMode.Editorial -> EditorialPreview(track, artworkUrl, accent)
        PlayerVisualMode.Pulse -> PulsePreview(track, artworkUrl, accent)
    }
}

@Composable
private fun DeckArtwork(
    track: Track,
    artworkUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val url = artworkUrl.ifBlank { track.thumbnailUrl }
    val request = remember(context, url) {
        ImageRequest.Builder(context)
            .data(LevyraArtworkCache.small(url))
            .build()
    }
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(Color(track.accentStart), Color(track.accentEnd)))
        )
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ImmersivePreview(track: Track, artworkUrl: String, accent: Color) {
    Box(modifier = Modifier.fillMaxSize()) {
        DeckArtwork(
            track = track,
            artworkUrl = artworkUrl,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.30f to Color.Transparent,
                        0.62f to accent.copy(alpha = 0.18f).compositeOnBlack(),
                        1f to Color.Black
                    )
                )
        )
        PreviewControls(accent = accent, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun CardPreview(track: Track, artworkUrl: String, accent: Color, glow: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = if (glow) 0.34f else 0.18f).compositeOnBlack(), Color.Black)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
                .fillMaxWidth(0.72f)
                .aspectRatio(1f)
        ) {
            if (glow) {
                Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.3f; scaleY = 1.3f }) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.45f), Color.Transparent)
                        )
                    )
                }
            }
            DeckArtwork(
                track = track,
                artworkUrl = artworkUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        PreviewControls(accent = accent, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun EditorialPreview(track: Track, artworkUrl: String, accent: Color) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0D))) {
        Canvas(
            modifier = Modifier
                .padding(start = 8.dp, top = 22.dp)
                .width(10.dp)
                .height(84.dp)
        ) {
            drawRoundRect(accent, size = Size(size.width, 5.dp.toPx()), cornerRadius = CornerRadius(2f))
            drawRect(
                Color.White.copy(alpha = 0.28f),
                topLeft = Offset(1.dp.toPx(), 12.dp.toPx()),
                size = Size(1f, size.height - 12.dp.toPx())
            )
        }
        DeckArtwork(
            track = track,
            artworkUrl = artworkUrl,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 8.dp)
                .fillMaxWidth(0.78f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(2.dp))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, end = 10.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PreviewBar(width = 26.dp, height = 3.dp, color = accent)
            PreviewBar(width = 86.dp, height = 9.dp, color = Color.White)
            PreviewBar(width = 60.dp, height = 9.dp, color = Color.White)
            PreviewBar(width = 44.dp, height = 4.dp, color = Color.White.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(2.dp))
            PreviewBar(width = 100.dp, height = 1.dp, color = Color.White.copy(alpha = 0.3f))
            PreviewTransport(accent = accent)
        }
    }
}

@Composable
private fun PulsePreview(track: Track, artworkUrl: String, accent: Color) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        DeckArtwork(
            track = track,
            artworkUrl = artworkUrl,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 22.dp)
                .fillMaxWidth(0.56f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) { drawPreviewSignal(accent) }
            PreviewBar(width = 52.dp, height = 3.dp, color = Color.White.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(2.dp))
            PreviewBar(width = 76.dp, height = 7.dp, color = Color.White)
            PreviewTransport(accent = accent)
        }
    }
}

@Composable
private fun PreviewControls(accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        PreviewBar(width = 78.dp, height = 7.dp, color = Color.White)
        PreviewBar(width = 48.dp, height = 4.dp, color = Color.White.copy(alpha = 0.55f))
        Spacer(modifier = Modifier.height(2.dp))
        PreviewWave(accent = accent)
        PreviewTransport(accent = accent)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PreviewTransport(accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 30.dp, height = 18.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(accent)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-30).dp)
                .size(width = 22.dp, height = 14.dp)
                .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(5.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 30.dp)
                .size(width = 22.dp, height = 14.dp)
                .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(5.dp))
        )
    }
}

@Composable
private fun PreviewWave(accent: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val centerY = size.height / 2f
        val split = size.width * 0.58f
        val stroke = 2.dp.toPx()
        val amplitude = 2.dp.toPx()
        val waveLength = 12.dp.toPx()
        val path = Path().apply {
            moveTo(0f, centerY)
            var x = 0f
            while (x < split) {
                x = (x + 1.dp.toPx()).coerceAtMost(split)
                lineTo(x, centerY - amplitude * sin(x / waveLength * 2f * PI.toFloat()))
            }
        }
        drawPath(path, accent, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(split + 3.dp.toPx(), centerY),
            end = Offset(size.width, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun PreviewBar(width: Dp, height: Dp, color: Color) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .background(color, RoundedCornerShape(height / 2))
    )
}

private val PreviewSignalLevels = floatArrayOf(
    0.20f, 0.45f, 0.30f, 0.70f, 0.55f, 0.90f, 0.40f, 0.65f, 0.35f, 0.80f,
    0.50f, 0.28f, 0.60f, 0.42f, 0.75f, 0.33f, 0.52f, 0.24f, 0.46f, 0.18f
)

private fun DrawScope.drawPreviewSignal(accent: Color) {
    val step = size.width / PreviewSignalLevels.size
    val stroke = 1.5.dp.toPx()
    val center = size.height / 2f
    PreviewSignalLevels.forEachIndexed { index, level ->
        val x = step * index + step / 2f
        val half = level * size.height / 2f
        drawLine(
            color = accent,
            start = Offset(x, center - half),
            end = Offset(x, center + half),
            strokeWidth = stroke
        )
    }
}

private fun Color.compositeOnBlack(): Color =
    Color(red = red * alpha, green = green * alpha, blue = blue * alpha, alpha = 1f)
