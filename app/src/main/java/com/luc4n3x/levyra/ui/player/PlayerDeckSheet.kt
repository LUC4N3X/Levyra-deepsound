package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.ui.theme.*
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import com.luc4n3x.levyra.ui.components.LevyraUtilityBackdrop
import com.luc4n3x.levyra.ui.components.levyraUtilityColors
import com.luc4n3x.levyra.ui.components.LevyraUtilityStyleActive
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import com.luc4n3x.levyra.ui.playerMix
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.runtime.LaunchedEffect
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

private val DeckFanCardWidth = 172.dp
private val DeckFanDrop = 22.dp
private val DeckFanElevation = 18.dp
private val DeckFanCardShape = RoundedCornerShape(26.dp)
private const val DeckPreviewAspect = 0.66f
private val DeckPreviewDesignWidth = 124.dp
private const val DeckFanTilt = 7f
private const val DeckFanShrink = 0.14f
private const val DeckFanFade = 0.3f
private const val DeckWashStrength = 0.9f
private val SwatchWidth = 84.dp
private val SwatchInset = 3.dp
private const val SwatchAspect = 1.5f
private val SwatchShape = RoundedCornerShape(20.dp)
private val SwatchRingShape = RoundedCornerShape(23.dp)
private val DeckWashLift = 36.dp

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
        val washed = LevyraUtilityStyleActive
        val chrome = if (washed) levyraUtilityColors().accent else accent
        Box(modifier = Modifier.fillMaxWidth()) {
        if (washed) LevyraUtilityBackdrop(
            modifier = Modifier.matchParentSize().graphicsLayer { translationY = -DeckWashLift.toPx() },
            strength = DeckWashStrength
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = LevyraPlayerDesign.SpaceXl),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
        ) {
            PlayerDeckStageTitle(selected, chrome, surfaces, animated)
            PlayerDeckFan(selected, track, artworkUrl, accent, chrome, surfaces, animated, onSelect)
            Spacer(Modifier.height(LevyraPlayerDesign.SpaceSm))
            PlayerBackgroundPicker(
                selected = background,
                track = track,
                artworkUrl = artworkUrl,
                accent = accent,
                chrome = chrome,
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
}

@Composable
private fun PlayerDeckStageTitle(
    mode: PlayerVisualMode,
    accent: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean
) {
    val strings = LocalLevyraStrings.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = LevyraPlayerDesign.Gutter)) {
        PlayerDeckEyebrow(
            text = strings.playerDeck,
            highlight = if (mode == DefaultPlayerVisualMode) strings.playerDeckDefaultBadge else null,
            accent = accent,
            surfaces = surfaces
        )
        Crossfade(
            targetState = mode,
            animationSpec = if (animated) LevyraPlayerDesign.standardTween<Float>() else snap(),
            label = "player-deck-title"
        ) { shown ->
            Column {
                Text(
                    text = playerDeckLabel(shown, strings),
                    color = surfaces.content,
                    fontSize = 34.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(34.sp),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = LevyraPlayerDesign.SpaceXs).semantics { heading() }
                )
                Text(
                    text = playerDeckHint(shown, strings),
                    color = surfaces.contentMuted,
                    fontSize = 14.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(14.sp),
                    minLines = 2,
                    modifier = Modifier.padding(top = LevyraPlayerDesign.SpaceXs)
                )
            }
        }
    }
}

@Composable
private fun PlayerDeckEyebrow(
    text: String,
    highlight: String?,
    accent: Color,
    surfaces: PlayerSurfaceTokens,
    modifier: Modifier = Modifier
) {
    val label = remember(text, highlight, accent) {
        buildAnnotatedString {
            append(text.uppercase())
            if (highlight != null) {
                append("  ·  ")
                withStyle(SpanStyle(color = accent)) { append(highlight.uppercase()) }
            }
        }
    }
    Text(
        text = label,
        color = surfaces.contentMuted,
        fontSize = 12.sp,
        lineHeight = LevyraTypeRhythm.lineHeight(12.sp),
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.4.sp,
        maxLines = 1,
        modifier = modifier
    )
}

@Composable
private fun PlayerDeckFan(
    selected: PlayerVisualMode,
    track: Track,
    artworkUrl: String,
    accent: Color,
    chrome: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onSelect: (PlayerVisualMode) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = PlayerDeckOrder.indexOf(selected).coerceAtLeast(0),
        pageCount = { PlayerDeckOrder.size }
    )
    val latestSelect by rememberUpdatedState(onSelect)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .drop(1)
            .collect { page -> latestSelect(PlayerDeckOrder[page]) }
    }
    val tilt = if (animated) DeckFanTilt else 0f
    val halo by animateColorAsState(
        targetValue = chrome,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-deck-fan-halo"
    )
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val reach = size.height * 0.62f
                val center = Offset(size.width / 2f, size.height * 0.46f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(halo.copy(alpha = 0.2f), halo.copy(alpha = 0.06f), Color.Transparent),
                        center = center,
                        radius = reach
                    ),
                    radius = reach,
                    center = center
                )
            }
    ) {
        val cardWidth = DeckFanCardWidth.coerceAtMost(maxWidth * 0.5f)
        val previewScale = cardWidth / DeckPreviewDesignWidth
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(cardWidth),
            contentPadding = PaddingValues(horizontal = (maxWidth - cardWidth) / 2),
            pageSpacing = LevyraPlayerDesign.SpaceMd,
            beyondViewportPageCount = 1,
            key = { PlayerDeckOrder[it].name },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LevyraPlayerDesign.SpaceLg, bottom = DeckFanDrop + LevyraPlayerDesign.SpaceSm)
                .selectableGroup()
        ) { page ->
            val mode = PlayerDeckOrder[page]
            val label = playerDeckLabel(mode, strings)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(DeckPreviewAspect)
                    .graphicsLayer {
                        val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            .coerceIn(-1f, 1f)
                        val distance = abs(offset)
                        val scale = 1f - DeckFanShrink * distance
                        scaleX = scale
                        scaleY = scale
                        rotationZ = -offset * tilt
                        translationY = DeckFanDrop.toPx() * distance
                        alpha = 1f - DeckFanFade * distance
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        shadowElevation = (1f - distance) * DeckFanElevation.toPx()
                        shape = DeckFanCardShape
                        clip = true
                    }
                    .background(Color.Black)
                    .border(1.dp, surfaces.outline, DeckFanCardShape)
                    .selectable(
                        selected = mode == selected,
                        role = Role.RadioButton,
                        onClick = {
                            if (pagerState.currentPage != page) {
                                latestSelect(mode)
                                scope.launch {
                                    if (animated) pagerState.animateScrollToPage(page) else pagerState.scrollToPage(page)
                                }
                            }
                        }
                    )
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(DeckPreviewDesignWidth, DeckPreviewDesignWidth / DeckPreviewAspect)
                        .graphicsLayer {
                            scaleX = previewScale
                            scaleY = previewScale
                        }
                ) {
                    PlayerDeckPreview(mode, track, artworkUrl, accent)
                }
            }
        }
    }
    PlayerDeckDots(
        count = PlayerDeckOrder.size,
        pagerState = pagerState,
        accent = chrome,
        surfaces = surfaces
    )
}

@Composable
private fun PlayerDeckDots(
    count: Int,
    pagerState: PagerState,
    accent: Color,
    surfaces: PlayerSurfaceTokens
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            Canvas(modifier = Modifier.size(width = 20.dp, height = 6.dp)) {
                val focus = (1f - abs((pagerState.currentPage - index) + pagerState.currentPageOffsetFraction))
                    .coerceIn(0f, 1f)
                val width = size.height + (size.width - size.height) * focus
                drawRoundRect(
                    color = lerp(surfaces.contentFaint, accent, focus),
                    topLeft = Offset((size.width - width) / 2f, 0f),
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
            }
        }
    }
}

internal fun playerDeckLabel(mode: PlayerVisualMode, strings: LevyraStrings): String {
    val labels = playerDeckShortLabels(strings.code)
    return when (mode) {
        PlayerVisualMode.CanvasImmersive -> labels.immersive
        PlayerVisualMode.Cinema -> labels.cinema
        PlayerVisualMode.Artwork -> labels.artwork
        PlayerVisualMode.Expressive -> labels.expressive
        PlayerVisualMode.Pulse -> labels.pulse
        PlayerVisualMode.Stack -> labels.stack
        PlayerVisualMode.Folio -> labels.folio
        PlayerVisualMode.Flow -> labels.flow
        PlayerVisualMode.Ember -> labels.ember
    }
}

internal fun playerDeckHint(mode: PlayerVisualMode, strings: LevyraStrings): String = when (mode) {
    PlayerVisualMode.CanvasImmersive -> strings.playerDeckImmersiveHint
    PlayerVisualMode.Cinema -> strings.playerDeckCardHint
    PlayerVisualMode.Artwork -> strings.playerDeckArtworkHint
    PlayerVisualMode.Expressive -> strings.playerDeckExpressiveHint
    PlayerVisualMode.Pulse -> strings.playerDeckPulseHint
    PlayerVisualMode.Stack -> strings.playerDeckStackHint
    PlayerVisualMode.Folio -> strings.playerDeckFolioHint
    PlayerVisualMode.Flow -> strings.playerDeckFlowHint
    PlayerVisualMode.Ember -> strings.playerDeckEmberHint
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
        PlayerVisualMode.Cinema -> CinemaPreview(track, artworkUrl, accent)
        PlayerVisualMode.Artwork -> CardPreview(track, artworkUrl, accent)
        PlayerVisualMode.Expressive -> ExpressivePreview(track, artworkUrl, accent)
        PlayerVisualMode.Pulse -> PulsePreview(track, artworkUrl, accent)
        PlayerVisualMode.Stack -> StackPreview(track, artworkUrl, accent)
        PlayerVisualMode.Folio -> FolioPreview(track, artworkUrl, accent)
        PlayerVisualMode.Flow -> FlowPreview(track, artworkUrl, accent)
        PlayerVisualMode.Ember -> EmberPreview(track, artworkUrl, accent)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StackPreview(track: Track, artworkUrl: String, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(accent, Color.Black, 0.55f), Color.Black)))
    ) {
        listOf(2, 1).forEach { depth ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 22.dp)
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        val scale = 1f - 0.1f * depth
                        scaleX = scale
                        scaleY = scale
                        translationY = -depth * size.height * 0.11f
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                        alpha = 1f - 0.28f * depth
                    }
                    .clip(RoundedCornerShape(9.dp))
                    .background(lerp(accent, Color.Black, 0.35f + 0.15f * depth))
            )
        }
        DeckArtwork(
            track = track,
            artworkUrl = artworkUrl,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 22.dp)
                .fillMaxWidth(0.7f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(9.dp))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PreviewBar(width = 74.dp, height = 8.dp, color = Color.White)
            PreviewBar(width = 44.dp, height = 4.dp, color = Color.White.copy(alpha = 0.55f))
            PreviewWave(accent = lerp(accent, Color.White, 0.5f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PreviewDot(Color.White.copy(alpha = 0.5f))
                Box(Modifier.size(width = 16.dp, height = 13.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp, topEnd = 3.dp, bottomEnd = 3.dp)))
                Box(Modifier.size(20.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(lerp(accent, Color.White, 0.6f)))
                Box(Modifier.size(width = 16.dp, height = 13.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 7.dp, bottomEnd = 7.dp)))
                PreviewDot(Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun FolioPreview(track: Track, artworkUrl: String, accent: Color) {
    val paper = lerp(accent, Color.Black, 0.72f)
    val ink = lerp(accent, Color.White, 0.86f)
    Box(modifier = Modifier.fillMaxSize().background(paper)) {
        DeckArtwork(
            track = track,
            artworkUrl = artworkUrl,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .fillMaxWidth(0.72f)
                .aspectRatio(1f)
                .clip(MaterialShapes.Cookie9Sided.toShape())
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PreviewBar(width = 80.dp, height = 10.dp, color = ink)
            PreviewBar(width = 46.dp, height = 4.dp, color = ink.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(1.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(18.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(18.dp).background(ink.copy(alpha = 0.22f), CircleShape))
                Box(Modifier.weight(1f).fillMaxHeight().background(ink, CircleShape))
                Box(Modifier.size(18.dp).background(ink.copy(alpha = 0.22f), CircleShape))
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { Box(Modifier.weight(1f).fillMaxHeight().background(ink.copy(alpha = 0.16f), CircleShape)) }
            }
        }
    }
}

@Composable
private fun EmberPreview(track: Track, artworkUrl: String, accent: Color) {
    val top = lerp(accent, Color(0xFF101014), 0.55f)
    val bottom = lerp(accent, Color(0xFF101014), 0.9f)
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom)))) {
        DeckArtwork(
            track = track,
            artworkUrl = artworkUrl,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .fillMaxWidth(0.74f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 9.dp, end = 9.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PreviewBar(width = 56.dp, height = 3.dp, color = Color.White.copy(alpha = 0.3f))
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 4.dp, vertical = 3.dp)
            ) {
                PreviewBar(width = 64.dp, height = 7.dp, color = Color.White)
            }
            PreviewBar(width = 44.dp, height = 4.dp, color = Color.White.copy(alpha = 0.55f))
            Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) { drawPreviewSignal(lerp(accent, Color.White, 0.35f)) }
            Row(
                modifier = Modifier.fillMaxWidth().height(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PreviewDot(Color.White.copy(alpha = 0.6f))
                Box(Modifier.size(width = 34.dp, height = 16.dp).background(accent, RoundedCornerShape(6.dp)))
                PreviewDot(Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun PreviewDot(color: Color) {
    Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
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
private fun CinemaPreview(track: Track, artworkUrl: String, accent: Color) {
    Box(modifier = Modifier.fillMaxSize()) {
        DeckArtwork(
            track = track,
            artworkUrl = artworkUrl,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.3f
                    scaleY = 1.3f
                    alpha = 0.7f
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.5f to lerp(accent, Color.Black, 0.8f).copy(alpha = 0.6f),
                        1f to Color.Black
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                DeckArtwork(track = track, artworkUrl = artworkUrl, modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)))
                PreviewBar(width = 40.dp, height = 4.dp, color = Color.White.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            PreviewBar(width = 70.dp, height = 6.dp, color = Color.White.copy(alpha = 0.3f))
            PreviewBar(width = 96.dp, height = 9.dp, color = Color.White)
            PreviewBar(width = 80.dp, height = 9.dp, color = Color.White)
            PreviewBar(width = 62.dp, height = 6.dp, color = Color.White.copy(alpha = 0.3f))
            PreviewBar(width = 50.dp, height = 6.dp, color = Color.White.copy(alpha = 0.18f))
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 7.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                PreviewBar(width = 88.dp, height = 3.dp, color = Color.White.copy(alpha = 0.45f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreviewDot(Color.White.copy(alpha = 0.6f))
                    PreviewDot(Color.White.copy(alpha = 0.8f))
                    Box(Modifier.size(16.dp).background(lerp(accent, Color.White, 0.62f), CircleShape))
                    PreviewDot(Color.White.copy(alpha = 0.8f))
                    PreviewDot(Color.White.copy(alpha = 0.6f))
                }
            }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(topStart = 9.dp, bottomStart = 9.dp, topEnd = 3.dp, bottomEnd = 3.dp))
                )
                Box(
                    modifier = Modifier
                        .weight(1.55f)
                        .fillMaxHeight()
                        .background(hero, RoundedCornerShape(6.dp))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 9.dp, bottomEnd = 9.dp))
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
    track: Track,
    artworkUrl: String,
    accent: Color,
    chrome: Color,
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
    val scroll = rememberScrollState()
    Column(verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)) {
        PlayerDeckEyebrow(
            text = strings.playerBackground,
            highlight = if (selected == DefaultPlayerBackgroundMode) strings.playerDeckDefaultBadge else null,
            accent = chrome,
            surfaces = surfaces,
            modifier = Modifier
                .padding(horizontal = LevyraPlayerDesign.Gutter)
                .semantics { heading() }
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXxs),
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = LevyraPlayerDesign.Gutter - SwatchInset)
                .selectableGroup()
        ) {
            options.forEach { (mode, label) ->
                PlayerBackgroundSwatch(
                    mode = mode,
                    label = label,
                    selected = mode == selected,
                    track = track,
                    artworkUrl = artworkUrl,
                    accent = accent,
                    chrome = chrome,
                    surfaces = surfaces,
                    animated = animated,
                    onClick = { onSelect(mode) }
                )
            }
        }
    }
}

@Composable
private fun PlayerBackgroundSwatch(
    mode: PlayerBackgroundMode,
    label: String,
    selected: Boolean,
    track: Track,
    artworkUrl: String,
    accent: Color,
    chrome: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onClick: () -> Unit
) {
    val lift by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.expressiveSpring()),
        label = "player-background-swatch-lift"
    )
    val ring by animateColorAsState(
        targetValue = if (selected) chrome else Color.Transparent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween()),
        label = "player-background-swatch-ring"
    )
    val width = SwatchWidth * LocalDensity.current.fontScale.coerceIn(1f, 1.4f)
    Column(
        modifier = Modifier
            .width(width + SwatchInset * 2)
            .clip(SwatchRingShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)
    ) {
        Box(
            modifier = Modifier
                .border(2.dp, ring, SwatchRingShape)
                .padding(SwatchInset)
        ) {
            Column(
                modifier = Modifier
                    .size(width = width, height = width * SwatchAspect)
                    .clip(SwatchShape)
                    .drawBehind { drawBackgroundPreview(mode, accent) }
                    .border(1.dp, surfaces.outline, SwatchShape)
                    .padding(LevyraPlayerDesign.SpaceMd),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DeckArtwork(
                    track = track,
                    artworkUrl = artworkUrl,
                    modifier = Modifier
                        .padding(top = LevyraPlayerDesign.SpaceXs)
                        .graphicsLayer {
                            val scale = 0.92f + 0.08f * lift
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = 6.dp.toPx()
                            shape = RoundedCornerShape(LevyraPlayerDesign.CornerXs)
                            clip = true
                        }
                        .fillMaxWidth(0.72f)
                        .aspectRatio(1f)
                )
                Spacer(Modifier.weight(1f))
                PreviewBar(width = 46.dp, height = 5.dp, color = Color.White.copy(alpha = 0.92f))
                Spacer(Modifier.height(4.dp))
                PreviewBar(width = 28.dp, height = 3.dp, color = Color.White.copy(alpha = 0.5f))
                Spacer(Modifier.height(LevyraPlayerDesign.SpaceSm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreviewDot(Color.White.copy(alpha = 0.55f))
                    Box(Modifier.size(14.dp).background(Color.White, CircleShape))
                    PreviewDot(Color.White.copy(alpha = 0.55f))
                }
            }
            if (lift > 0f) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(LevyraPlayerDesign.SpaceXs)
                        .graphicsLayer {
                            alpha = lift
                            scaleX = lift
                            scaleY = lift
                        }
                        .background(chrome, CircleShape)
                        .padding(3.dp)
                        .size(12.dp)
                )
            }
        }
        Text(
            text = label,
            color = if (selected) surfaces.content else surfaces.contentMuted,
            fontSize = 12.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(12.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier
                .padding(horizontal = LevyraPlayerDesign.SpaceXxs)
                .padding(bottom = LevyraPlayerDesign.SpaceXs)
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
