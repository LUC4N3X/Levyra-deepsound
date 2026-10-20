package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.ui.theme.*
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import com.luc4n3x.levyra.ui.playerMix
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import com.luc4n3x.levyra.ui.components.rememberLevyraAvatarPressShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.Nocturne78Palette
import com.luc4n3x.levyra.ui.i18n.LevyraSkinStrings
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.layout.PaddingValues
import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.ReactiveAuraPreset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll

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
    background: PlayerBackgroundMode,
    onSelect: (PlayerVisualMode) -> Unit,
    onSelectBackground: (PlayerBackgroundMode) -> Unit,
    onDismiss: () -> Unit,
    reactiveAuraPreset: ReactiveAuraPreset = ReactiveAuraPreset.Recommended,
    reactiveAuraIntensity: Int = LevyraInterfaceSettings.DEFAULT_REACTIVE_AURA_INTENSITY,
    onSelectReactiveAuraPreset: (ReactiveAuraPreset) -> Unit = {},
    onSelectReactiveAuraIntensity: (Int) -> Unit = {}
) {
    val strings = LocalLevyraStrings.current
    PlayerSheetFrame(
        surfaces = surfaces,
        animated = animated,
        onDismiss = onDismiss
    ) {
        if (LevyraSkinManagesLayout) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(LevyraPlayerDesign.Gutter),
                verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
            ) {
                Text(strings.playerDeck, color = surfaces.content, fontSize = 22.sp, modifier = Modifier.semantics { heading() })
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().heightIn(min = LevyraPlayerDesign.MinimumTouchTarget).semantics { this.selected = true },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = surfaces.control, contentColor = surfaces.activeContent)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(LevyraSkinLabel)
                }
                Text(LevyraSkinStrings.managedForCode(strings.code, LevyraSkinLabel), color = surfaces.contentMuted, fontSize = 14.sp)
            }
            return@PlayerSheetFrame
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
            PlayerDeckStylePreview(selected, track, artworkUrl, accent, surfaces, animated)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LevyraPlayerDesign.Gutter)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
            ) {
                PlayerDeckOrder.chunked(2).forEach { modes ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
                    ) {
                        modes.forEach { mode ->
                            PlayerDeckStyleButton(mode, mode == selected, accent, surfaces, animated,
                                onClick = { onSelect(mode) }, modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
            PlayerBackgroundPicker(
                selected = background,
                accent = accent,
                surfaces = surfaces,
                animated = animated,
                onSelect = onSelectBackground
            )
            if (background == PlayerBackgroundMode.ReactiveAura) {
                ReactiveAuraControls(
                    preset = reactiveAuraPreset,
                    intensity = reactiveAuraIntensity,
                    accent = accent,
                    surfaces = surfaces,
                    animated = animated,
                    onPreset = onSelectReactiveAuraPreset,
                    onIntensity = onSelectReactiveAuraIntensity
                )
            }
            Text(
                text = strings.playerDeckLandscapeNote,
                color = surfaces.contentMuted,
                fontSize = 11.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(11.sp),
                modifier = Modifier.padding(horizontal = LevyraPlayerDesign.Gutter)
            )
        }
    }
}

@Composable
private fun PlayerDeckStylePreview(
    mode: PlayerVisualMode,
    track: Track,
    artworkUrl: String,
    accent: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = Modifier
            .padding(horizontal = LevyraPlayerDesign.Gutter)
            .fillMaxWidth()
            .clip(LevyraPlayerDesign.ShapeXl)
            .background(surfaces.controlQuiet)
            .padding(LevyraPlayerDesign.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceLg)
    ) {
        Crossfade(
            targetState = mode,
            animationSpec = if (animated) LevyraPlayerDesign.standardTween<Float>() else snap(),
            modifier = Modifier.width(124.dp).aspectRatio(DeckPreviewAspect)
                .clip(LevyraPlayerDesign.ShapeMd).background(Color.Black),
            label = "player-deck-preview"
        ) { preview -> PlayerDeckPreview(preview, track, artworkUrl, accent) }
        Column(modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)) {
            Text(playerDeckLabel(mode, strings), color = surfaces.content, fontSize = 22.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(22.sp), fontWeight = FontWeight.Bold)
            Text(playerDeckHint(mode, strings), color = surfaces.contentMuted, fontSize = 13.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(13.sp))
            if (mode == DefaultPlayerVisualMode) {
                Text(strings.playerDeckDefaultBadge, color = accent, fontSize = 11.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(11.sp), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlayerDeckStyleButton(
    mode: PlayerVisualMode,
    selected: Boolean,
    accent: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val corner by animateDpAsState(
        targetValue = if (selected) LevyraPlayerDesign.CornerLg else LevyraPlayerDesign.CornerSm,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.expressiveSpring()),
        label = "player-deck-choice-shape"
    )
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(corner),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (selected) accent else surfaces.controlQuiet,
            contentColor = if (selected) surfaces.heroContent else surfaces.content
        ),
        contentPadding = PaddingValues(horizontal = LevyraPlayerDesign.SpaceMd, vertical = LevyraPlayerDesign.SpaceMd)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)) {
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(playerDeckLabel(mode, LocalLevyraStrings.current), fontSize = 13.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(13.sp), fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center)
        }
    }
}

internal fun playerDeckLabel(mode: PlayerVisualMode, strings: LevyraStrings): String {
    val labels = playerDeckShortLabels(strings.code)
    return when (mode) {
        PlayerVisualMode.CanvasImmersive -> labels.immersive
        PlayerVisualMode.CanvasCard -> labels.canvasCard
        PlayerVisualMode.Artwork -> labels.artwork
        PlayerVisualMode.Expressive -> labels.expressive
        PlayerVisualMode.Pulse -> labels.pulse
        PlayerVisualMode.Halo -> labels.halo
        PlayerVisualMode.Folio -> labels.folio
        PlayerVisualMode.Flow -> labels.flow
    }
}

internal fun playerDeckHint(mode: PlayerVisualMode, strings: LevyraStrings): String = when (mode) {
    PlayerVisualMode.CanvasImmersive -> strings.playerDeckImmersiveHint
    PlayerVisualMode.CanvasCard -> strings.playerDeckCardHint
    PlayerVisualMode.Artwork -> strings.playerDeckArtworkHint
    PlayerVisualMode.Expressive -> strings.playerDeckExpressiveHint
    PlayerVisualMode.Pulse -> strings.playerDeckPulseHint
    PlayerVisualMode.Halo -> strings.playerDeckHaloHint
    PlayerVisualMode.Folio -> strings.playerDeckFolioHint
    PlayerVisualMode.Flow -> strings.playerDeckFlowHint
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
        PlayerVisualMode.CanvasCard -> SleevePreview(track, artworkUrl, accent)
        PlayerVisualMode.Artwork -> CardPreview(track, artworkUrl, accent)
        PlayerVisualMode.Expressive -> ExpressivePreview(track, artworkUrl, accent)
        PlayerVisualMode.Pulse -> PulsePreview(track, artworkUrl, accent)
        PlayerVisualMode.Halo -> ExpressivePreview(track, artworkUrl, accent)
        PlayerVisualMode.Folio -> ExpressivePreview(track, artworkUrl, accent)
        PlayerVisualMode.Flow -> FlowPreview(track, artworkUrl, accent)
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
private fun CardPreview(track: Track, artworkUrl: String, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.18f).compositeOnBlack(), Color.Black)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressivePreview(track: Track, artworkUrl: String, accent: Color) {
    val ink = lerp(accent, Color.White, 0.72f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lerp(accent, Color.Black, 0.62f))
    ) {
        DeckArtwork(
            track = track,
            artworkUrl = artworkUrl,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
                .fillMaxWidth(0.7f)
                .aspectRatio(1f)
                .clip(MaterialShapes.Puffy.toShape())
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PreviewBar(width = 92.dp, height = 12.dp, color = ink)
            PreviewBar(width = 48.dp, height = 4.dp, color = Color.White.copy(alpha = 0.55f))
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(ink.copy(alpha = 0.32f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .background(ink, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(ink.copy(alpha = 0.32f), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun SleevePreview(track: Track, artworkUrl: String, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.22f).compositeOnBlack(), Color.Black)))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .fillMaxWidth(0.78f)
                .aspectRatio(1.26f)
        ) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(0.94f)
                    .aspectRatio(1f)
            ) {
                drawCircle(Color(0xFF0B0B0D))
                drawCircle(Color.White.copy(alpha = 0.08f), radius = size.minDimension * 0.34f, style = Stroke(width = 1.dp.toPx()))
                drawCircle(accent, radius = size.minDimension * 0.18f)
            }
            DeckArtwork(
                track = track,
                artworkUrl = artworkUrl,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp))
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PreviewBar(width = 70.dp, height = 7.dp, color = Color.White)
            PreviewBar(width = 44.dp, height = 4.dp, color = Color.White.copy(alpha = 0.55f))
            Spacer(modifier = Modifier.height(2.dp))
            PreviewTransport(accent = accent)
        }
    }
}

@Composable
private fun FlowPreview(track: Track, artworkUrl: String, accent: Color) {
    val hero = lerp(accent, Color.White, 0.8f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.46f to Color.Black,
                    1f to lerp(accent, Color.Black, 0.86f)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
                .fillMaxWidth(0.6f)
                .aspectRatio(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.45f; scaleY = 1.45f }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to accent.copy(alpha = 0.46f),
                        0.55f to accent.copy(alpha = 0.16f),
                        1f to Color.Transparent
                    )
                )
            }
            DeckArtwork(
                track = track,
                artworkUrl = artworkUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(flowCoverShape(1f))
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PreviewBar(width = 30.dp, height = 3.dp, color = Color.White.copy(alpha = 0.4f))
            PreviewBar(width = 84.dp, height = 9.dp, color = Color.White)
            PreviewBar(width = 50.dp, height = 4.dp, color = Color.White.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(1.dp))
            PreviewWave(accent = hero)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color.White.copy(alpha = 0.16f), FlowLeafShape(0.42f, 0.16f, mirrored = true))
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(hero, FlowLeafShape(FlowLeafMajor, FlowLeafMinor * 1.4f))
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color.White.copy(alpha = 0.16f), FlowLeafShape(0.42f, 0.16f))
                )
            }
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
                .fillMaxWidth(0.62f)
                .aspectRatio(1f)
                .clip(
                    RoundedCornerShape(
                        topStartPercent = 50,
                        topEndPercent = 50,
                        bottomEndPercent = 7,
                        bottomStartPercent = 7
                    )
                )
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

@Composable
private fun PlayerBackgroundPicker(
    selected: PlayerBackgroundMode,
    accent: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onSelect: (PlayerBackgroundMode) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val options = listOf(
        PlayerBackgroundMode.Dynamic to strings.playerBackgroundDynamic,
        PlayerBackgroundMode.ReactiveAura to strings.playerBackgroundReactiveAura,
        PlayerBackgroundMode.Aurora to strings.playerBackgroundAurora,
        PlayerBackgroundMode.Gradient to strings.playerBackgroundGradient,
        PlayerBackgroundMode.Blur to strings.playerBackgroundBlur,
        PlayerBackgroundMode.Dark to strings.playerBackgroundDark,
        PlayerBackgroundMode.PureBlack to strings.playerBackgroundPureBlack
    )
    Column(verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)) {
        Text(
            text = strings.playerBackground,
            color = surfaces.content,
            fontSize = 18.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(18.sp),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = LevyraPlayerDesign.Gutter)
                .semantics { heading() }
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = LevyraPlayerDesign.Gutter)
                .selectableGroup()
        ) {
            options.forEach { (mode, label) ->
                PlayerBackgroundTile(
                    mode = mode,
                    label = label,
                    selected = mode == selected,
                    accent = accent,
                    surfaces = surfaces,
                    animated = animated,
                    onClick = { onSelect(mode) }
                )
            }
        }
    }
}

@Composable
private fun PlayerBackgroundTile(
    mode: PlayerBackgroundMode,
    label: String,
    selected: Boolean,
    accent: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onClick: () -> Unit
) {
    val shape = rememberLevyraAvatarPressShape(pressed = selected, animated = animated)
    val tileWidth = 88.dp * (LocalDensity.current.fontScale / 1.3f).coerceAtLeast(1f)
    Column(
        modifier = Modifier
            .width(tileWidth)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(shape)
                .border(1.dp, if (selected) accent else surfaces.outline, shape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) { drawBackgroundPreview(mode, accent) }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = surfaces.heroContent,
                    modifier = Modifier.background(accent, CircleShape)
                        .padding(LevyraPlayerDesign.SpaceSm).size(20.dp)
                )
            }
        }
        Text(
            text = label,
            color = if (selected) surfaces.content else surfaces.contentMuted,
            fontSize = 11.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(11.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.heightIn(min = 32.dp)
        )
        Text(
                text = if (mode == DefaultPlayerBackgroundMode) LocalLevyraStrings.current.playerDeckDefaultBadge else "",
                color = surfaces.content,
                fontSize = 10.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(10.sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = LevyraPlayerDesign.SpaceXs)
        )
    }
}

private fun DrawScope.drawBackgroundPreview(mode: PlayerBackgroundMode, accent: Color) {
    val deep = lerp(accent, Color.Black, 0.78f)
    when (mode) {
        PlayerBackgroundMode.PureBlack -> drawRect(Color.Black)
        PlayerBackgroundMode.Dark -> drawRect(Brush.verticalGradient(listOf(Color(0xFF1C1D24), Color(0xFF0B0B0F))))
        PlayerBackgroundMode.Blur -> {
            drawRect(deep)
            drawCircle(accent.copy(alpha = 0.55f), radius = size.width * 0.7f, center = Offset(size.width * 0.5f, size.height * 0.3f))
            drawRect(Color.Black.copy(alpha = 0.35f))
        }
        PlayerBackgroundMode.Gradient -> {
            drawRect(
                Brush.linearGradient(
                    listOf(accent, lerp(accent, Color(0xFF7C4DFF), 0.55f)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, deep.copy(alpha = 0.9f))))
        }
        PlayerBackgroundMode.Aurora -> {
            drawRect(deep)
            listOf(
                accent to Offset(0.2f, 0.25f),
                lerp(accent, Color(0xFF7C4DFF), 0.6f) to Offset(0.85f, 0.5f),
                lerp(accent, Color(0xFF00BFA5), 0.6f) to Offset(0.35f, 0.85f)
            ).forEach { (color, anchor) ->
                val center = Offset(size.width * anchor.x, size.height * anchor.y)
                drawCircle(
                    Brush.radialGradient(listOf(color.copy(alpha = 0.75f), Color.Transparent), center, size.width * 0.75f),
                    radius = size.width * 0.75f,
                    center = center
                )
            }
        }
        PlayerBackgroundMode.ReactiveAura -> {
            drawRect(deep)
            listOf(
                accent to Offset(0.22f, 0.26f),
                lerp(accent, Color(0xFF7C4DFF), 0.45f) to Offset(0.84f, 0.56f),
                lerp(accent, Color(0xFF7C4DFF), 0.2f) to Offset(0.36f, 0.86f)
            ).forEach { (color, anchor) ->
                val center = Offset(size.width * anchor.x, size.height * anchor.y)
                val reach = size.width * 0.62f
                drawCircle(
                    Brush.radialGradient(
                        listOf(color.copy(alpha = 0.5f), color.copy(alpha = 0f)),
                        center,
                        reach
                    ),
                    radius = reach,
                    center = center
                )
            }
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
        }
        PlayerBackgroundMode.Dynamic -> {
            drawRect(deep)
            drawCircle(
                Brush.radialGradient(listOf(accent.copy(alpha = 0.6f), Color.Transparent), Offset(size.width / 2f, size.height * 0.35f), size.width),
                radius = size.width,
                center = Offset(size.width / 2f, size.height * 0.35f)
            )
        }
    }
}
