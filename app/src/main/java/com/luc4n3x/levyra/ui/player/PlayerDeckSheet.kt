package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm

private val DeckCardWidth = 148.dp
private val DeckCardCorner = 22.dp
private val DeckPreviewCorner = 16.dp
private const val DeckPreviewAspect = 0.62f

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
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        val index = PlayerDeckOrder.indexOf(selected)
        if (index > 0) listState.scrollToItem(index)
    }
    PlayerSheetFrame(
        surfaces = surfaces,
        animated = animated,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = LevyraPlayerDesign.SpaceXl),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceLg)
        ) {
            Column(modifier = Modifier.padding(horizontal = LevyraPlayerDesign.Gutter)) {
                Text(
                    text = strings.playerDeck,
                    color = surfaces.content,
                    fontSize = 20.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(20.sp),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = strings.playerDeckSubtitle,
                    color = surfaces.contentMuted,
                    fontSize = 13.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(13.sp),
                    fontWeight = FontWeight.Medium
                )
            }
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = LevyraPlayerDesign.Gutter),
                horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd),
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                items(PlayerDeckOrder, key = { it.name }) { mode ->
                    PlayerDeckCard(
                        mode = mode,
                        track = track,
                        artworkUrl = artworkUrl,
                        selected = mode == selected,
                        surfaces = surfaces,
                        accent = accent,
                        animated = animated,
                        strings = strings,
                        onClick = { onSelect(mode) }
                    )
                }
            }
            Text(
                text = strings.playerDeckLandscapeNote,
                color = surfaces.contentFaint,
                fontSize = 12.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(12.sp),
                modifier = Modifier.padding(horizontal = LevyraPlayerDesign.Gutter)
            )
        }
    }
}

@Composable
private fun PlayerDeckCard(
    mode: PlayerVisualMode,
    track: Track,
    artworkUrl: String,
    selected: Boolean,
    surfaces: PlayerSurfaceTokens,
    accent: Color,
    animated: Boolean,
    strings: LevyraStrings,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(DeckCardCorner)
    val outline by animateColorAsState(
        targetValue = if (selected) accent else surfaces.contentFaint.copy(alpha = 0.18f),
        animationSpec = LevyraMotion.spec(animated, LevyraMotion.fade()),
        label = "player-deck-outline"
    )
    val lift by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = LevyraMotion.physics(animated, LevyraMotion.settle),
        label = "player-deck-lift"
    )
    Column(
        modifier = Modifier
            .width(DeckCardWidth)
            .graphicsLayer { translationY = -lift * 4.dp.toPx() }
            .clip(shape)
            .background(if (selected) surfaces.active else surfaces.controlQuiet)
            .border(if (selected) 1.5.dp else LevyraPlayerDesign.Hairline, outline, shape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(LevyraPlayerDesign.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(DeckPreviewAspect)
                .clip(RoundedCornerShape(DeckPreviewCorner))
                .background(Color.Black)
        ) {
            PlayerDeckPreview(
                mode = mode,
                track = track,
                artworkUrl = artworkUrl,
                accent = accent
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(LevyraPlayerDesign.SpaceSm)
                        .size(22.dp)
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = surfaces.heroContent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(horizontal = LevyraPlayerDesign.SpaceXs)) {
            Text(
                text = visualModeStateDescription(mode, strings),
                color = if (selected) surfaces.activeContent else surfaces.content,
                fontSize = 14.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(14.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = playerDeckHint(mode, strings),
                color = surfaces.contentMuted,
                fontSize = 11.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(11.sp),
                maxLines = 3,
                minLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
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
        PreviewBar(width = 110.dp, height = 2.dp, color = Color.White.copy(alpha = 0.3f))
        PreviewTransport(accent = accent)
    }
}

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
                .size(width = 34.dp, height = 14.dp)
                .background(accent, RoundedCornerShape(5.dp))
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
