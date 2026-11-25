package com.luc4n3x.levyra.ui.player

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.getValue
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.Nocturne78Palette
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.PlayArrow
import com.luc4n3x.levyra.ui.theme.*
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.StrokeCap
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

private val DeckFanCardWidth = 184.dp
private val DeckFanElevation = 26.dp
private val DeckPhoneShape = RoundedCornerShape(30.dp)
private val DeckPhoneBezel = 3.5.dp
private val DeckScreenShape = RoundedCornerShape(27.dp)
private const val DeckPreviewAspect = 0.54f
private val DeckPreviewDesignWidth = 124.dp
private const val DeckFanShrink = 0.14f
private const val DeckFanFade = 0.42f
private const val DeckFanTurn = 22f
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
    showBackgroundFor: (PlayerVisualMode) -> Boolean = { true },
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
            val pagerState = rememberPagerState(
                initialPage = PlayerDeckOrder.indexOf(selected).coerceAtLeast(0),
                pageCount = { PlayerDeckOrder.size }
            )
            PlayerDeckStageTitle(PlayerDeckOrder[pagerState.currentPage], chrome, surfaces, animated)
            PlayerDeckFan(pagerState, selected, track, artworkUrl, accent, chrome, surfaces, animated, onSelect)
            Spacer(Modifier.height(LevyraPlayerDesign.SpaceSm))
            AnimatedVisibility(
                visible = showBackgroundFor(PlayerDeckOrder[pagerState.currentPage]),
                enter = if (animated) fadeIn() + expandVertically() else EnterTransition.None,
                exit = if (animated) fadeOut() + shrinkVertically() else ExitTransition.None
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)) {
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
                }
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
    pagerState: PagerState,
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
    val halo by animateColorAsState(
        targetValue = accent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-deck-fan-halo"
    )
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val reach = size.height * 0.62f
                val center = Offset(size.width / 2f, size.height * 0.44f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(halo.copy(alpha = 0.42f), halo.copy(alpha = 0.12f), Color.Transparent),
                        center = center,
                        radius = reach
                    ),
                    radius = reach,
                    center = center
                )
                val floor = Offset(size.width / 2f, size.height * 0.93f)
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                        center = floor,
                        radius = size.width * 0.3f
                    ),
                    topLeft = Offset(floor.x - size.width * 0.3f, floor.y - 14.dp.toPx()),
                    size = Size(size.width * 0.6f, 28.dp.toPx())
                )
            }
    ) {
        val cardWidth = DeckFanCardWidth.coerceAtMost(maxWidth * 0.5f)
        val previewScale = (cardWidth - DeckPhoneBezel * 2) / DeckPreviewDesignWidth
        val previewHeight = DeckPreviewDesignWidth * ((cardWidth / DeckPreviewAspect - DeckPhoneBezel * 2) / (cardWidth - DeckPhoneBezel * 2))
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(cardWidth),
            contentPadding = PaddingValues(horizontal = (maxWidth - cardWidth) / 2),
            pageSpacing = LevyraPlayerDesign.SpaceMd,
            beyondViewportPageCount = 1,
            key = { PlayerDeckOrder[it].name },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LevyraPlayerDesign.SpaceLg, bottom = LevyraPlayerDesign.SpaceMd)
                .selectableGroup()
        ) { page ->
            val mode = PlayerDeckOrder[page]
            val label = playerDeckLabel(mode, strings)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(DeckPreviewAspect)
                    .graphicsLayer {
                        val offset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction)
                            .coerceIn(-1f, 1f)
                        val distance = abs(offset)
                        val scale = 1f - DeckFanShrink * distance
                        scaleX = scale
                        scaleY = scale
                        rotationY = if (animated) offset * DeckFanTurn else 0f
                        cameraDistance = 12f * density
                        alpha = 1f - DeckFanFade * distance
                        shadowElevation = (1f - distance) * DeckFanElevation.toPx()
                        ambientShadowColor = halo
                        spotShadowColor = halo
                        shape = DeckPhoneShape
                    }
                    .drawBehind { drawPhoneButtons() }
                    .clip(DeckPhoneShape)
                    .background(DeckPhoneFrame)
                    .border(
                        width = if (mode == selected) 2.dp else 1.dp,
                        brush = if (mode == selected) SolidColor(chrome) else DeckPhoneEdge,
                        shape = DeckPhoneShape
                    )
                    .selectable(
                        selected = mode == selected,
                        role = Role.RadioButton,
                        onClick = {
                            if (pagerState.currentPage == page) {
                                onSelect(mode)
                            } else {
                                scope.launch {
                                    if (animated) pagerState.animateScrollToPage(page) else pagerState.scrollToPage(page)
                                }
                            }
                        }
                    )
                    .semantics { contentDescription = label }
                    .padding(DeckPhoneBezel),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(DeckScreenShape)
                        .background(Color.Black)
                        .drawWithContent {
                            drawContent()
                            val drift = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                            val sweep = size.width * (0.2f + drift * 0.6f)
                            drawRect(
                                brush = Brush.linearGradient(
                                    0f to Color.Transparent,
                                    0.45f to Color.White.copy(alpha = 0.10f),
                                    0.55f to Color.White.copy(alpha = 0.03f),
                                    1f to Color.Transparent,
                                    start = Offset(sweep - size.width * 0.6f, 0f),
                                    end = Offset(sweep + size.width * 0.6f, size.height * 0.55f)
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .requiredSize(DeckPreviewDesignWidth, previewHeight)
                            .graphicsLayer {
                                scaleX = previewScale
                                scaleY = previewScale
                            }
                    ) {
                        PlayerDeckPreview(mode, track, artworkUrl, accent)
                        DeckScreenChrome()
                    }
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
                val focus = (1f - abs(pagerState.currentPage - index + pagerState.currentPageOffsetFraction))
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

@Composable
private fun PreviewDot(color: Color) {
    Box(modifier = Modifier.size(5.dp).background(color, CircleShape))
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

private val DeckPhoneFrame = Brush.horizontalGradient(listOf(Color(0xFF4A4B50), Color(0xFF1E1F23), Color(0xFF2B2C31), Color(0xFF55565C)))
private val DeckPhoneButton = Color(0xFF3C3D42)

private fun DrawScope.drawPhoneButtons() {
    val reach = 2.dp.toPx()
    val radius = CornerRadius(reach)
    listOf(0.2f to 0.06f, 0.3f to 0.13f).forEach { (top, length) ->
        drawRoundRect(
            color = DeckPhoneButton,
            topLeft = Offset(size.width - reach * 0.4f, size.height * top),
            size = Size(reach, size.height * length),
            cornerRadius = radius
        )
    }
}
private val DeckPhoneEdge = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.22f))
)
private val PreviewDockIcons = listOf(
    Icons.Rounded.Shuffle,
    Icons.Rounded.Description,
    Icons.AutoMirrored.Rounded.List,
    Icons.Rounded.Style,
    Icons.Rounded.Download,
    Icons.Rounded.Repeat
)

@Composable
private fun BoxScope.DeckScreenChrome() {
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "9:41",
            color = Color.White,
            fontSize = 6.sp,
            lineHeight = 7.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFF26272B), CircleShape)
                .padding(1.2.dp)
                .background(Color.Black, CircleShape)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { bar ->
                Box(
                    modifier = Modifier
                        .size(width = 1.5.dp, height = (2 + bar * 1.2f).dp)
                        .background(Color.White, RoundedCornerShape(1.dp))
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 1.dp)
                    .size(width = 9.dp, height = 4.5.dp)
                    .background(Color.White, RoundedCornerShape(1.5.dp))
            )
        }
    }
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 4.dp)
            .size(width = 40.dp, height = 2.dp)
            .background(Color.White.copy(alpha = 0.75f), CircleShape)
    )
}

@Composable
private fun PreviewModeToggle(ink: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 17.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .background(ink.copy(alpha = 0.14f), CircleShape)
                .padding(1.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 15.dp, height = 8.dp)
                    .background(ink.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = ink, modifier = Modifier.size(5.dp))
            }
            Box(modifier = Modifier.size(width = 15.dp, height = 8.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Videocam, contentDescription = null, tint = ink.copy(alpha = 0.6f), modifier = Modifier.size(5.dp))
            }
        }
    }
}

@Composable
private fun PreviewTitle(
    track: Track,
    ink: Color,
    modifier: Modifier = Modifier,
    size: TextUnit = 9.sp,
    lines: Int = 1,
    align: TextAlign = TextAlign.Start
) {
    Column(modifier = modifier) {
        Text(
            text = track.title,
            color = ink,
            fontSize = size,
            lineHeight = size * 1.15f,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            maxLines = lines,
            overflow = TextOverflow.Ellipsis,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = track.artist,
            color = ink.copy(alpha = 0.62f),
            fontSize = 5.5.sp,
            lineHeight = 7.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PreviewCircle(
    icon: ImageVector,
    fill: Color,
    tint: Color,
    size: Dp = 15.dp,
    shape: Shape = CircleShape,
    width: Dp = size
) {
    Box(
        modifier = Modifier
            .size(width = width, height = size)
            .background(fill, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
private fun PreviewSeek(ink: Color, fraction: Float = 0.06f, thumb: Boolean = true, height: Dp = 1.5.dp) {
    Canvas(modifier = Modifier.fillMaxWidth().height(5.dp)) {
        val y = size.height / 2f
        val stroke = height.toPx()
        drawLine(ink.copy(alpha = 0.25f), Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
        drawLine(ink.copy(alpha = 0.9f), Offset(0f, y), Offset(size.width * fraction, y), stroke, StrokeCap.Round)
        if (thumb) drawCircle(ink, radius = size.height / 2f, center = Offset(size.width * fraction, y))
    }
}

@Composable
private fun PreviewTimes(ink: Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("0:05", color = ink.copy(alpha = 0.55f), fontSize = 4.sp, lineHeight = 5.sp, modifier = Modifier.weight(1f))
        Text("3:31", color = ink.copy(alpha = 0.55f), fontSize = 4.sp, lineHeight = 5.sp)
    }
}

@Composable
private fun PreviewComments(ink: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().height(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(Color(0xFF2A2A30), Color(0xFF2F7DD1), Color(0xFF8A7464)).forEachIndexed { index, avatar ->
            Box(
                modifier = Modifier
                    .offset(x = (-2 * index).dp)
                    .size(6.dp)
                    .background(avatar, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 1.dp, end = 4.dp)
                .weight(1f)
                .height(2.5.dp)
                .background(ink.copy(alpha = 0.3f), CircleShape)
        )
        Row(
            modifier = Modifier
                .background(ink.copy(alpha = 0.14f), CircleShape)
                .padding(horizontal = 3.dp, vertical = 1.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.5.dp)
        ) {
            Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = ink, modifier = Modifier.size(4.dp))
            Box(Modifier.size(width = 6.dp, height = 2.dp).background(ink.copy(alpha = 0.8f), CircleShape))
        }
    }
}

@Composable
private fun PreviewIconRow(icons: List<ImageVector>, ink: Color, size: Dp = 7.dp, fill: Color? = null, active: Int = -1, activeFill: Color = ink) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icons.forEachIndexed { index, icon ->
            if (fill == null) {
                Icon(icon, contentDescription = null, tint = ink.copy(alpha = 0.85f), modifier = Modifier.size(size))
            } else {
                val on = index == active
                PreviewCircle(icon, if (on) activeFill else fill, if (on) Color.Black.copy(alpha = 0.8f) else ink, size = size * 2.1f)
            }
        }
    }
}

@Composable
private fun PreviewButtonGroup(icons: List<ImageVector>, ink: Color, fill: Color, active: Int = -1, activeFill: Color = ink, height: Dp = 15.dp) {
    Row(
        modifier = Modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
    ) {
        icons.forEachIndexed { index, icon ->
            val on = index == active
            val shape = when {
                on -> CircleShape
                index == 0 -> RoundedCornerShape(topStart = height / 2, bottomStart = height / 2, topEnd = 3.dp, bottomEnd = 3.dp)
                index == icons.lastIndex -> RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = height / 2, bottomEnd = height / 2)
                else -> RoundedCornerShape(3.dp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (on) activeFill else fill, shape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (on) Color.Black.copy(alpha = 0.8f) else ink, modifier = Modifier.size(height * 0.5f))
            }
        }
    }
}

@Composable
private fun PreviewFadedArtwork(track: Track, artworkUrl: String, fraction: Float, modifier: Modifier = Modifier) {
    DeckArtwork(
        track = track,
        artworkUrl = artworkUrl,
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(0.45f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn
                )
            }
    )
}

@Composable
private fun ImmersivePreview(track: Track, artworkUrl: String, accent: Color) {
    val ink = Color.White
    Box(modifier = Modifier.fillMaxSize().background(lerp(accent, Color.Black, 0.4f))) {
        DeckArtwork(
            track = track,
            artworkUrl = artworkUrl,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.6f
                    scaleY = 1.6f
                    alpha = 0.85f
                }
                .blur(22.dp)
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.32f))))
        PreviewFadedArtwork(track, artworkUrl, 0.58f)
        PreviewModeToggle(ink)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 9.dp, end = 9.dp, bottom = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PreviewTitle(track, ink, Modifier.weight(1f), size = 9.sp)
                PreviewCircle(Icons.Rounded.StarOutline, ink.copy(alpha = 0.16f), ink, size = 11.dp)
                Spacer(Modifier.width(3.dp))
                PreviewCircle(Icons.Rounded.MoreHoriz, ink.copy(alpha = 0.16f), ink, size = 11.dp)
            }
            Spacer(Modifier.height(4.dp))
            PreviewComments(ink)
            Spacer(Modifier.height(6.dp))
            PreviewSeek(ink, thumb = false, height = 3.dp)
            PreviewTimes(ink)
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = ink.copy(alpha = 0.6f), modifier = Modifier.size(6.dp))
                Icon(Icons.Rounded.FastRewind, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                Icon(Icons.Rounded.FastForward, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
                Icon(Icons.Rounded.Repeat, contentDescription = null, tint = ink.copy(alpha = 0.6f), modifier = Modifier.size(6.dp))
            }
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(Icons.AutoMirrored.Rounded.VolumeDown, contentDescription = null, tint = ink.copy(alpha = 0.6f), modifier = Modifier.size(5.dp))
                Box(Modifier.weight(1f)) { PreviewSeek(ink, fraction = 0f, thumb = false, height = 2.dp) }
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = ink.copy(alpha = 0.6f), modifier = Modifier.size(5.dp))
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(Icons.Rounded.FormatQuote, Icons.AutoMirrored.Rounded.List, Icons.Rounded.Style, Icons.Rounded.Download).forEach { icon ->
                    Icon(icon, contentDescription = null, tint = ink.copy(alpha = 0.75f), modifier = Modifier.size(7.dp))
                }
            }
        }
    }
}

@Composable
private fun CardPreview(track: Track, artworkUrl: String, accent: Color) {
    val ink = Color.White
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(accent, Color.Black, 0.4f), lerp(accent, Color.Black, 0.72f))))
    ) {
        PreviewFadedArtwork(track, artworkUrl, 0.52f)
        PreviewModeToggle(ink)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PreviewTitle(track, ink, Modifier.weight(1f), size = 9.sp)
                PreviewCircle(Icons.Rounded.FavoriteBorder, ink.copy(alpha = 0.16f), ink, size = 13.dp)
            }
            Spacer(Modifier.height(4.dp))
            PreviewComments(ink)
            Spacer(Modifier.height(6.dp))
            PreviewSeek(ink)
            PreviewTimes(ink)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val soft = ink.copy(alpha = 0.14f)
                PreviewCircle(Icons.Rounded.Shuffle, soft, ink, size = 14.dp)
                PreviewCircle(Icons.Rounded.SkipPrevious, soft, ink, size = 18.dp)
                PreviewCircle(Icons.Rounded.PlayArrow, lerp(accent, Color.White, 0.88f), Color.Black.copy(alpha = 0.82f), size = 20.dp, width = 26.dp, shape = RoundedCornerShape(7.dp))
                PreviewCircle(Icons.Rounded.SkipNext, soft, ink, size = 18.dp)
                PreviewCircle(Icons.Rounded.Repeat, soft, ink, size = 14.dp)
            }
            Spacer(Modifier.height(5.dp))
            PreviewButtonGroup(
                icons = listOf(Icons.Rounded.Description, Icons.AutoMirrored.Rounded.List, Icons.Rounded.Style, Icons.Rounded.Download),
                ink = ink,
                fill = ink.copy(alpha = 0.14f)
            )
        }
    }
}

@Composable
private fun FlowPreview(track: Track, artworkUrl: String, accent: Color) {
    val ink = Color.White
    val soft = ink.copy(alpha = 0.14f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(accent, Color.Black, 0.42f), lerp(accent, Color.Black, 0.7f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = 10.dp)
        ) {
            PreviewModeToggle(ink)
            Spacer(Modifier.height(5.dp))
            DeckArtwork(
                track = track,
                artworkUrl = artworkUrl,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.84f)
                    .aspectRatio(1f)
                    .clip(flowCoverShape(1f))
            )
            Spacer(Modifier.weight(1f))
            PreviewBar(width = 22.dp, height = 2.dp, color = ink.copy(alpha = 0.4f))
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                PreviewTitle(track, ink, Modifier.weight(1f), size = 9.sp, lines = 2)
                PreviewCircle(Icons.AutoMirrored.Rounded.PlaylistAdd, soft, ink, size = 13.dp)
                Spacer(Modifier.width(3.dp))
                PreviewCircle(Icons.Rounded.FavoriteBorder, soft, ink, size = 13.dp)
            }
            Spacer(Modifier.height(4.dp))
            PreviewComments(ink)
            Spacer(Modifier.height(5.dp))
            PreviewSeek(ink)
            PreviewTimes(ink)
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = ink.copy(alpha = 0.7f), modifier = Modifier.size(7.dp))
                PreviewCircle(Icons.Rounded.SkipPrevious, soft, ink, size = 16.dp)
                PreviewCircle(Icons.Rounded.PlayArrow, lerp(accent, Color.White, 0.86f), Color.Black.copy(alpha = 0.82f), size = 23.dp)
                PreviewCircle(Icons.Rounded.SkipNext, soft, ink, size = 16.dp)
                Icon(Icons.Rounded.Repeat, contentDescription = null, tint = ink.copy(alpha = 0.7f), modifier = Modifier.size(7.dp))
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(Icons.Rounded.Description, Icons.AutoMirrored.Rounded.List, Icons.Rounded.Style, Icons.Rounded.Download).forEach { icon ->
                    PreviewCircle(icon, soft, ink, size = 15.dp)
                }
            }
        }
    }
}

@Composable
private fun EmberPreview(track: Track, artworkUrl: String, accent: Color) {
    val ink = Color.White
    val light = lerp(accent, Color.White, 0.86f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(accent, Color(0xFF3A3840), 0.78f), Color(0xFF1C1B20))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = 9.dp)
        ) {
            PreviewModeToggle(ink)
            Spacer(Modifier.height(5.dp))
            DeckArtwork(
                track = track,
                artworkUrl = artworkUrl,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.76f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.title,
                    color = ink,
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = ink.copy(alpha = 0.8f), modifier = Modifier.size(7.dp))
            }
            Text(
                text = track.artist,
                color = ink.copy(alpha = 0.6f),
                fontSize = 6.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(Modifier.height(3.dp))
            PreviewComments(ink)
            Spacer(Modifier.height(3.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                val bars = 34
                val step = size.width / bars
                val stroke = step * 0.55f
                repeat(bars) { index ->
                    val level = 0.35f + 0.65f * PreviewSignalLevels[index * 7 % PreviewSignalLevels.size]
                    val x = step * index + step / 2f
                    drawLine(
                        color = if (index == 0) ink else ink.copy(alpha = 0.34f),
                        start = Offset(x, size.height),
                        end = Offset(x, size.height * (1f - level)),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
            PreviewTimes(ink)
            Spacer(Modifier.height(4.dp))
            PreviewIconRow(
                icons = listOf(Icons.AutoMirrored.Rounded.List, Icons.Rounded.Description, Icons.Rounded.Shuffle, Icons.Rounded.FavoriteBorder, Icons.Rounded.Download, Icons.Rounded.Style),
                ink = ink
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Rounded.Repeat, contentDescription = null, tint = ink.copy(alpha = 0.8f), modifier = Modifier.size(7.dp))
                Spacer(Modifier.width(2.dp))
                PreviewCircle(Icons.Rounded.SkipPrevious, light, Color.Black.copy(alpha = 0.8f), size = 18.dp)
                PreviewCircle(Icons.Rounded.SkipNext, light, Color.Black.copy(alpha = 0.8f), size = 18.dp)
                Spacer(Modifier.weight(1f))
                PreviewCircle(Icons.Rounded.PlayArrow, light, Color.Black.copy(alpha = 0.8f), size = 26.dp)
            }
        }
    }
}

@Composable
private fun CinemaPreview(track: Track, artworkUrl: String, accent: Color) {
    val ink = Color.White
    Box(modifier = Modifier.fillMaxSize()) {
        DeckArtwork(
            track = track,
            artworkUrl = artworkUrl,
            modifier = Modifier
                .fillMaxSize()
                .blur(14.dp)
                .graphicsLayer {
                    scaleX = 1.3f
                    scaleY = 1.3f
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.5f to lerp(accent, Color.Black, 0.7f).copy(alpha = 0.55f),
                        1f to Color.Black.copy(alpha = 0.7f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 9.dp, end = 9.dp, bottom = 10.dp)
        ) {
            PreviewModeToggle(ink)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DeckArtwork(track = track, artworkUrl = artworkUrl, modifier = Modifier.size(13.dp).clip(RoundedCornerShape(3.dp)))
                PreviewTitle(track, ink, Modifier.weight(1f), size = 6.sp)
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PreviewBar(width = 58.dp, height = 7.dp, color = ink.copy(alpha = 0.32f))
                PreviewBar(width = 82.dp, height = 8.dp, color = ink)
                PreviewBar(width = 66.dp, height = 8.dp, color = ink.copy(alpha = 0.55f))
                PreviewBar(width = 74.dp, height = 7.dp, color = ink.copy(alpha = 0.26f))
                PreviewBar(width = 50.dp, height = 7.dp, color = ink.copy(alpha = 0.16f))
            }
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 7.dp, vertical = 6.dp)
            ) {
                PreviewSeek(ink)
                PreviewTimes(ink)
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = ink, modifier = Modifier.size(7.dp))
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = ink, modifier = Modifier.size(10.dp))
                    PreviewCircle(Icons.Rounded.PlayArrow, ink.copy(alpha = 0.92f), Color.Black.copy(alpha = 0.85f), size = 17.dp, shape = RoundedCornerShape(5.dp))
                    Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = ink, modifier = Modifier.size(10.dp))
                    Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null, tint = ink, modifier = Modifier.size(7.dp))
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(Icons.Rounded.Shuffle, Icons.Rounded.Repeat, Icons.Rounded.Description, Icons.Rounded.Style, Icons.Rounded.Download).forEach { icon ->
                        Icon(icon, contentDescription = null, tint = ink.copy(alpha = 0.8f), modifier = Modifier.size(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StackPreview(track: Track, artworkUrl: String, accent: Color) {
    val ink = Color.White
    val face = lerp(accent, Color.White, 0.86f)
    val ring = lerp(accent, Color.Black, 0.45f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(accent, Color.Black, 0.4f), lerp(accent, Color.Black, 0.7f), Color.Black)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PreviewModeToggle(ink)
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, tint = ink.copy(alpha = 0.7f), modifier = Modifier.size(7.dp))
            Box(modifier = Modifier.fillMaxWidth().height(124.dp)) {
                listOf(0, 1).forEach { depth ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = (depth * 15).dp)
                            .fillMaxWidth(0.74f + 0.06f * depth)
                            .height(44.dp)
                            .graphicsLayer {
                                rotationX = 18f
                                rotationZ = if (depth == 0) -2f else 1.5f
                                cameraDistance = 8f * density
                            }
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        DeckArtwork(track = track, artworkUrl = artworkUrl, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f - depth * 0.12f)))
                        Column(modifier = Modifier.padding(start = 6.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            PreviewBar(width = 34.dp, height = 3.dp, color = ink)
                            PreviewBar(width = 20.dp, height = 2.dp, color = ink.copy(alpha = 0.6f))
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .fillMaxWidth(0.88f)
                        .aspectRatio(1.08f)
                ) {
                    DeckArtwork(
                        track = track,
                        artworkUrl = artworkUrl,
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(8.dp, RoundedCornerShape(9.dp))
                            .clip(RoundedCornerShape(9.dp))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .size(11.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = ink, modifier = Modifier.size(6.dp))
                    }
                }
                listOf(Alignment.CenterStart, Alignment.CenterEnd).forEach { side ->
                    Box(
                        modifier = Modifier
                            .align(side)
                            .size(width = 7.dp, height = 22.dp)
                            .background(ring.copy(alpha = 0.7f), RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (side == Alignment.CenterStart) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = ink,
                            modifier = Modifier.size(6.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(17.dp)
                        .background(ring, RoundedCornerShape(6.dp))
                        .padding(1.5.dp)
                        .background(face, RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black.copy(alpha = 0.85f), modifier = Modifier.size(9.dp))
                }
            }
            Text("12  /  27", color = ink.copy(alpha = 0.7f), fontSize = 4.5.sp, lineHeight = 6.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.Bold)
            PreviewTitle(track, ink, size = 9.sp, align = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .background(Color.Black.copy(alpha = 0.38f), CircleShape)
                    .padding(horizontal = 1.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(Modifier.size(7.dp).background(face, CircleShape))
                Text("3:31", color = ink.copy(alpha = 0.7f), fontSize = 4.sp, lineHeight = 5.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
            }
            Spacer(Modifier.height(6.dp))
            PreviewIconRow(PreviewDockIcons, ink)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun FolioPreview(track: Track, artworkUrl: String, accent: Color) {
    val paper = lerp(accent, Color.Black, 0.5f)
    val ink = lerp(accent, Color.White, 0.86f)
    val onInk = lerp(accent, Color.Black, 0.6f)
    Box(modifier = Modifier.fillMaxSize().background(paper)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = 10.dp)
        ) {
            PreviewModeToggle(ink)
            Spacer(Modifier.height(6.dp))
            DeckArtwork(
                track = track,
                artworkUrl = artworkUrl,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
            )
            Spacer(Modifier.weight(1f))
            PreviewTitle(track, ink, size = 10.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .background(ink, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(LocalLevyraStrings.current.play.uppercase(), color = onInk, maxLines = 1, fontSize = 5.5.sp, lineHeight = 6.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)
                }
                PreviewCircle(Icons.Rounded.SkipNext, ink, onInk, size = 18.dp)
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PreviewCircle(Icons.Rounded.SkipPrevious, ink, onInk, size = 18.dp)
                Column(modifier = Modifier.weight(1f)) {
                    PreviewSeek(ink, thumb = false, height = 2.dp)
                    PreviewTimes(ink)
                }
            }
            Spacer(Modifier.height(4.dp))
            PreviewIconRow(
                icons = listOf(Icons.Rounded.FavoriteBorder, Icons.Rounded.Shuffle, Icons.Rounded.Description, Icons.AutoMirrored.Rounded.List, Icons.Rounded.Style),
                ink = ink,
                size = 6.5.dp,
                fill = ink.copy(alpha = 0.18f),
                active = 4
            )
        }
    }
}

@Composable
private fun ExpressivePreview(track: Track, artworkUrl: String, accent: Color) {
    val ink = Color.White
    val light = lerp(accent, Color.White, 0.88f)
    val soft = ink.copy(alpha = 0.14f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(accent, Color.Black, 0.32f), lerp(accent, Color.Black, 0.66f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = 10.dp)
        ) {
            PreviewModeToggle(ink)
            Spacer(Modifier.height(6.dp))
            DeckArtwork(
                track = track,
                artworkUrl = artworkUrl,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.84f)
                    .aspectRatio(1f)
                    .clip(CutCornerShape(8))
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PreviewTitle(track, ink, Modifier.weight(1f), size = 10.sp, lines = 2)
                PreviewCircle(Icons.Rounded.FavoriteBorder, soft, ink, size = 14.dp)
            }
            Spacer(Modifier.height(4.dp))
            PreviewComments(ink)
            Spacer(Modifier.height(4.dp))
            PreviewSeek(ink, thumb = false, height = 2.dp)
            PreviewTimes(ink)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                PreviewCircle(Icons.Rounded.SkipPrevious, soft, ink, size = 20.dp)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .background(light, CircleShape),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black.copy(alpha = 0.85f), modifier = Modifier.size(8.dp))
                    Box(Modifier.padding(start = 2.dp).size(width = 22.dp, height = 3.dp).background(Color.Black.copy(alpha = 0.75f), CircleShape))
                }
                PreviewCircle(Icons.Rounded.SkipNext, soft, ink, size = 20.dp)
            }
            Spacer(Modifier.height(4.dp))
            PreviewButtonGroup(PreviewDockIcons, ink, soft, active = 3, activeFill = light, height = 13.dp)
        }
    }
}

@Composable
private fun PulsePreview(track: Track, artworkUrl: String, accent: Color) {
    val ink = Color.White
    val light = lerp(accent, Color.White, 0.86f)
    val soft = ink.copy(alpha = 0.16f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(accent, Color.Black, 0.36f), lerp(accent, Color.Black, 0.74f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = 10.dp)
        ) {
            PreviewModeToggle(ink)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.9f)
                    .aspectRatio(0.98f)
                    .clip(RoundedCornerShape(topStartPercent = 40, topEndPercent = 40, bottomEndPercent = 8, bottomStartPercent = 8))
            ) {
                DeckArtwork(track = track, artworkUrl = artworkUrl, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))))
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PreviewTitle(track, ink, size = 9.sp, align = TextAlign.Center)
                    Spacer(Modifier.height(3.dp))
                    Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = ink, modifier = Modifier.size(7.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            PreviewComments(ink)
            Spacer(Modifier.height(4.dp))
            PreviewSeek(ink, thumb = false, height = 2.dp)
            PreviewTimes(ink)
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.18f), CircleShape)
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PreviewCircle(Icons.Rounded.SkipPrevious, soft, ink, size = 17.dp)
                Icon(Icons.Rounded.Replay10, contentDescription = null, tint = ink, modifier = Modifier.size(7.dp))
                PreviewCircle(Icons.Rounded.PlayArrow, light, Color.Black.copy(alpha = 0.85f), size = 17.dp, width = 24.dp, shape = RoundedCornerShape(6.dp))
                Icon(Icons.Rounded.Forward10, contentDescription = null, tint = ink, modifier = Modifier.size(7.dp))
                PreviewCircle(Icons.Rounded.SkipNext, soft, ink, size = 17.dp)
            }
            Spacer(Modifier.height(4.dp))
            PreviewIconRow(
                icons = listOf(Icons.Rounded.Shuffle, Icons.Rounded.Description, Icons.Rounded.Repeat, Icons.AutoMirrored.Rounded.List, Icons.Rounded.Style, Icons.Rounded.Download),
                ink = ink,
                size = 6.dp,
                fill = soft,
                active = 4,
                activeFill = light
            )
        }
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
