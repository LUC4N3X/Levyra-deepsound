package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.theme.*
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.domain.MiniArtworkShape
import com.luc4n3x.levyra.domain.MiniBackground
import com.luc4n3x.levyra.domain.MiniButtonStyle
import com.luc4n3x.levyra.domain.MiniColorMode
import com.luc4n3x.levyra.domain.MiniControlSet
import com.luc4n3x.levyra.domain.MiniMotion
import com.luc4n3x.levyra.domain.MiniPlayerStyle
import com.luc4n3x.levyra.domain.MiniPlayerTuning
import com.luc4n3x.levyra.domain.MiniProgressStyle
import com.luc4n3x.levyra.domain.MiniTypography
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.dockFoldHeight
import com.luc4n3x.levyra.ui.components.dockFoldWidth
import com.luc4n3x.levyra.ui.components.DockMorph
import com.luc4n3x.levyra.ui.components.LocalDockMorph
import com.luc4n3x.levyra.ui.components.levyraCookieLandingShape
import com.luc4n3x.levyra.ui.components.dockLerpHeight
import com.luc4n3x.levyra.ui.components.dockMorphHeight
import com.luc4n3x.levyra.ui.components.dockMorphPadding
import com.luc4n3x.levyra.ui.components.morphedCompaction
import com.luc4n3x.levyra.ui.components.dockLerpSize
import com.luc4n3x.levyra.ui.components.levyraMarquee
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.player.PlayerMorphAnchors
import com.luc4n3x.levyra.ui.player.PlayerMorphSlot
import com.luc4n3x.levyra.ui.player.PlayerSwipeMotion
import com.luc4n3x.levyra.ui.player.miniPlayerHorizontalGesturesEnabled
import com.luc4n3x.levyra.ui.player.playerAxisDragGestures
import com.luc4n3x.levyra.ui.player.playerMorphAnchor
import com.luc4n3x.levyra.ui.player.playerSwipeContentAlpha
import com.luc4n3x.levyra.ui.player.rememberPlayerSwipeMotion
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities

private const val StyledMiniCarryFraction = 0.5f
private val FullRingProgress: () -> Float = { 1f }
private val StyledMiniTrayTop = 4.dp
private val StyledMiniTrayBottom = 10.dp
private val StyledMiniFloatingBottom = 12.dp
private val LiquidCapsuleMiniBottom = 10.dp
private const val MiniEclipseSpin = -40f
private val MiniEclipseShape: Shape by lazy { levyraCookieLandingShape(1f) }
private val LiquidCapsuleElevation = 10.dp

internal fun liquidCapsuleRim(onDark: Boolean): Brush = Brush.linearGradient(
    0f to Color.White.copy(alpha = if (onDark) 0.42f else 0.85f),
    0.45f to Color.White.copy(alpha = if (onDark) 0.10f else 0.35f),
    1f to Color.White.copy(alpha = if (onDark) 0.22f else 0.60f)
)
private val StyledMiniGutter = 12.dp
private val StyledMiniCompactShrink = 14.dp
private val StyledMiniMinHeight = 48.dp
private val StyledMiniLineHeight = 3.dp
private val StyledMiniRingStroke = 3.dp
private val StyledMiniDarkBase = PlayerDarkSurface
private val StyledMiniYtMusicSurface = Color.Black
private val StyledMiniYtMusicContentStart = 16.dp
private val StyledMiniEdgeProgressHeight = 2.dp
private val StyledMiniFixedDock: () -> Float = { 0f }
private val StyledMiniFullAttach: () -> Float = { 1f }
private val ConnectedMiniDefaultCorner = 28.dp
private val ConnectedMiniElevation = 6.dp
private val ConnectedSeamPlaying = 4.dp
private val ConnectedSeamPaused = 8.dp
private val ConnectedSeamFused = 2.dp
private val ConnectedBridgeMax = 3.dp
private val ConnectedBridgeMin = 1.5.dp
private val ConnectedBridgeInset = 18.dp

internal class MiniPlayerPalette(
    val accent: Color,
    val accentAlt: Color,
    val surface: Color,
    val content: Color,
    val contentMuted: Color,
    val onDark: Boolean
)

@Composable
private fun rememberMiniArtworkPalette(track: Track): ArtworkPalette {
    val context = LocalContext.current
    val fallback = remember(track.accentStart, track.accentEnd) {
        ArtworkPalette(track.accentStart, track.accentEnd)
    }
    val key = remember(track.id, track.thumbnailUrl, track.largeThumbnailUrl) {
        ArtworkPaletteCache.key(
            trackId = track.id,
            thumbnailUrl = track.thumbnailUrl,
            largeThumbnailUrl = track.largeThumbnailUrl
        )
    }
    val memory = remember(key) { ArtworkPaletteCache.peek(key) }
    val state = remember(key) { mutableStateOf(memory ?: fallback) }
    LaunchedEffect(key) {
        if (memory == null) {
            ArtworkPaletteCache.load(context, key)?.let { state.value = it }
        }
    }
    return state.value
}

@Composable
internal fun rememberMiniPlayerPalette(
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    track: Track,
    animated: Boolean
): MiniPlayerPalette {
    val skinPalette = rememberSkinMiniPlayerPalette(track, animated)
    if (skinPalette != null) return skinPalette
    return rememberTunedMiniPlayerPalette(style, tuning, track, animated)
}

@Composable
private fun rememberSkinMiniPlayerPalette(track: Track, animated: Boolean): MiniPlayerPalette? {
    val scheme = MaterialTheme.colorScheme
    if (LevyraIsNocturne) {
        return MiniPlayerPalette(scheme.primary, scheme.primary, scheme.surfaceContainerHigh, scheme.onSurface, scheme.onSurfaceVariant, true)
    }
    if (LevyraIsSpotify) {
        val artwork = rememberMiniArtworkPalette(track)
        val target = remember(artwork) { SpotifyDesign.miniSurface(Color(artwork.start)) }
        val surface by animateColorAsState(target, if (animated) LevyraPlayerDesign.paletteTween() else snap(), label = "spotify-mini-surface")
        return MiniPlayerPalette(Color.White, Color.White, surface, SpotifyDesign.text, SpotifyDesign.subdued, true)
    }
    if (LevyraIsAppleMusic) {
        return MiniPlayerPalette(AppleMusicDesign.label, AppleMusicDesign.label, AppleMusicDesign.chrome, AppleMusicDesign.label, AppleMusicDesign.secondaryLabel, !LevyraActivePalette.isLight)
    }
    if (LevyraIsYouTubeMusic) {
        return MiniPlayerPalette(Color.White, Color.White, YouTubeMusicDesign.miniPlayer, YouTubeMusicDesign.text, YouTubeMusicDesign.textSecondary, true)
    }
    if (LevyraIsQobuz) {
        return MiniPlayerPalette(QobuzDesign.textAccent, QobuzDesign.textAccent, QobuzDesign.miniPlayer, QobuzDesign.textPrimary, QobuzDesign.textSecondary, !LevyraActivePalette.isLight)
    }
    if (LevyraIsIvory) {
        return MiniPlayerPalette(IvoryDesign.rose, IvoryDesign.ink, LevyraActivePalette.ink, scheme.onSurface, scheme.onSurfaceVariant, !LevyraActivePalette.isLight)
    }
    if (LevyraIsPrism) {
        return MiniPlayerPalette(scheme.primary, scheme.tertiary, prismGlassColor(), scheme.onSurface, scheme.onSurfaceVariant, !LevyraActivePalette.isLight)
    }
    return null
}

@Composable
private fun rememberTunedMiniPlayerPalette(
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    track: Track,
    animated: Boolean
): MiniPlayerPalette {
    val scheme = MaterialTheme.colorScheme
    val artwork = rememberMiniArtworkPalette(track)
    val targets = when (tuning.colorMode) {
        MiniColorMode.Artwork -> remember(artwork) {
            harmonizePlayerAccents(Color(artwork.start), Color(artwork.end))
        }
        MiniColorMode.Theme -> PlayerAccentPair(scheme.primary, scheme.tertiary)
        MiniColorMode.Custom -> remember(tuning.customColor) {
            val custom = Color(tuning.customColor)
            PlayerAccentPair(custom, custom.playerMix(Color.White, 0.32f))
        }
    }
    val spec = if (animated) LevyraPlayerDesign.paletteTween<Color>() else snap()
    val accent by animateColorAsState(targets.primary, spec, label = "styled-mini-accent")
    val accentAlt by animateColorAsState(targets.secondary, spec, label = "styled-mini-accent-alt")
    val surface = when (style) {
        MiniPlayerStyle.Floating -> scheme.surfaceContainerHigh.playerMix(accent, 0.08f)
        MiniPlayerStyle.UltraCompact -> scheme.surfaceContainerHighest.playerMix(accent, 0.06f)
        MiniPlayerStyle.LiquidGlass, MiniPlayerStyle.LiquidCapsule -> scheme.surfaceContainerHighest
        MiniPlayerStyle.YtMusic -> StyledMiniYtMusicSurface
        MiniPlayerStyle.Connected -> scheme.surfaceContainerHigh.playerMix(accent, 0.12f)
        else -> StyledMiniDarkBase
    }
    val onDark = surface.luminance() < 0.45f
    val content = if (onDark) LevyraPlayerDesign.TextPrimary else scheme.onSurface
    val muted = if (onDark) LevyraPlayerDesign.TextSecondary else scheme.onSurfaceVariant
    return MiniPlayerPalette(accent, accentAlt, surface, content, muted, onDark)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun miniArtworkShape(shape: MiniArtworkShape): Shape = when (shape) {
    MiniArtworkShape.Cookie -> MaterialShapes.Cookie9Sided.toShape()
    MiniArtworkShape.Circle -> CircleShape
    MiniArtworkShape.Rounded -> RoundedCornerShape(28)
    MiniArtworkShape.Clover -> MaterialShapes.Clover4Leaf.toShape()
    MiniArtworkShape.Square -> RoundedCornerShape(12)
}

private fun miniTitleStyle(typography: MiniTypography, compact: Boolean): TextStyle {
    val base = if (compact) 14f else 15f
    return when (typography) {
        MiniTypography.Standard -> TextStyle(
            fontSize = base.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(base.sp),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp
        )
        MiniTypography.Bold -> TextStyle(
            fontSize = (base + 1f).sp,
            lineHeight = LevyraTypeRhythm.lineHeight((base + 1f).sp),
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.4).sp
        )
        MiniTypography.Editorial -> TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize = (base + 2f).sp,
            lineHeight = LevyraTypeRhythm.lineHeight((base + 2f).sp),
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        )
    }
}

private fun miniArtistStyle(typography: MiniTypography, compact: Boolean): TextStyle {
    val base = if (compact) 12f else 13f
    return when (typography) {
        MiniTypography.Editorial -> TextStyle(
            fontSize = (base - 2f).sp,
            lineHeight = LevyraTypeRhythm.lineHeight(base.sp),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
        else -> TextStyle(
            fontSize = base.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(base.sp),
            fontWeight = FontWeight.Medium
        )
    }
}

private class StyledMiniLayout(style: MiniPlayerStyle, tuning: MiniPlayerTuning) {
    val height = tuning.heightDp.dp
    val compactHeight = (height - StyledMiniCompactShrink).coerceAtLeast(StyledMiniMinHeight)
    val artworkSize = tuning.artworkDp.dp
    val artworkCompact = (artworkSize * 0.8f).coerceAtMost(compactHeight - 8.dp)
    val gutter = when (style) {
        MiniPlayerStyle.Floating, MiniPlayerStyle.LiquidGlass, MiniPlayerStyle.SuperExpressive, MiniPlayerStyle.YtMusic,
        MiniPlayerStyle.Connected, MiniPlayerStyle.LiquidCapsule -> tuning.insetDp.dp
        else -> StyledMiniGutter
    }
    val topGap = if (style == MiniPlayerStyle.YtMusic) 0.dp else StyledMiniTrayTop
    val bottomGap = when {
        LevyraIsQobuz -> 0.dp
        else -> when (style) {
        MiniPlayerStyle.Floating, MiniPlayerStyle.LiquidGlass -> StyledMiniFloatingBottom
        MiniPlayerStyle.LiquidCapsule -> LiquidCapsuleMiniBottom
        MiniPlayerStyle.YtMusic, MiniPlayerStyle.Connected -> 0.dp
        else -> StyledMiniTrayBottom
        }
    }
}

@Composable
private fun rememberMiniContainerShape(
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    height: Dp,
    playing: Boolean,
    animated: Boolean
): Shape {
    val expressiveCorner by animateDpAsState(
        targetValue = if (playing) height / 2 else height * 0.34f,
        animationSpec = if (animated) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        } else {
            snap()
        },
        label = "styled-mini-corner"
    )
    return if (style == MiniPlayerStyle.SuperExpressive) {
        RoundedCornerShape(expressiveCorner)
    } else {
        RoundedCornerShape(skinMiniPlayerCorner(tuning))
    }
}

private fun Modifier.morphAnchorWhen(
    enabled: Boolean,
    anchors: PlayerMorphAnchors?,
    slot: PlayerMorphSlot
): Modifier = if (enabled && anchors != null) playerMorphAnchor(anchors, slot) else this

@Composable
private fun MiniFillProgress(progress: () -> Float, palette: MiniPlayerPalette) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val width = size.width * progress().coerceIn(0f, 1f)
                val start = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                drawRect(
                    color = palette.accent.copy(alpha = if (palette.onDark) 0.24f else 0.16f),
                    topLeft = Offset(start, 0f),
                    size = Size(width, size.height)
                )
            }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun StyledMiniPlayer(
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    model: MiniPlayerModel,
    morphAnchors: PlayerMorphAnchors?,
    compaction: () -> Float,
    artworkHidden: () -> Boolean,
    playbackActions: MiniPlayerPlaybackActions,
    expansionActions: MiniPlayerExpansionActions,
    glass: GlassBackdropState?,
    interactive: Boolean = true,
    link: DockLink? = null
) {
    val track = model.track
    val liveRadio = track.isLiveRadio()
    val animated = model.animated && tuning.motion != MiniMotion.Reduced
    val haptics = LocalLevyraHaptics.current
    val palette = rememberMiniPlayerPalette(style, tuning, track, model.animated)
    val progressState = animateFloatAsState(
        targetValue = model.progress.coerceIn(0f, 1f),
        animationSpec = if (model.animated) tween(420, easing = LinearOutSlowInEasing) else snap(),
        label = "styled-mini-progress"
    )
    val progress: () -> Float = { progressState.value }
    val swipe = rememberPlayerSwipeMotion(animated)
    val layout = remember(style, tuning) { StyledMiniLayout(style, tuning) }
    val morph = LocalDockMorph.current
    val dockCompaction = morph.morphedCompaction(miniDockCompaction(style, compaction))
    val containerShape = rememberMiniContainerShape(style, tuning, layout.height, model.isPlaying, animated)
    val interaction = rememberMiniInteraction(interactive, model, liveRadio, playbackActions, expansionActions, swipe)
    val connected = style == MiniPlayerStyle.Connected
    val attach = link?.attach ?: StyledMiniFullAttach
    val fuse = link?.fuse ?: dockCompaction
    val seam = if (connected) rememberConnectedSeam(model.isPlaying, animated, attach, fuse) else null
    val connectedOuter = minOf(link?.outerCorner ?: ConnectedMiniDefaultCorner, layout.height / 2)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (seam != null && tuning.progress == MiniProgressStyle.Line && !liveRadio) {
                    Modifier.connectedSeamBridge(seam, attach, progress, palette, layout.gutter)
                } else {
                    Modifier
                }
            )
            .dockMorphPadding(morph, layout.gutter, layout.gutter, layout.topGap, layout.bottomGap)
            .then(if (seam != null) Modifier.dockBottomGap(morphedSeam(morph, seam)) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .dockMorphHeight(morph, dockCompaction, layout.height, layout.compactHeight)
                .morphAnchorWhen(interactive, morphAnchors, PlayerMorphSlot.MiniCard)
                .then(
                    if (connected) {
                        Modifier.dockLinkedClip(ConnectedMiniElevation, palette.content.copy(alpha = 0.08f)) {
                            connectedMiniShape(connectedOuter, attach(), fuse())
                        }
                    } else {
                        Modifier
                            .miniDepth(style, tuning, palette, containerShape)
                            .clip(containerShape)
                    }
                )
                .miniBackground(style, tuning, palette, glass, containerShape)
                .miniOutline(style, palette, containerShape)
                .then(interaction)
        ) {
            MiniBackdropLayers(style, tuning, track, palette, progress, liveRadio)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = miniContentStart(style, tuning),
                        end = 6.dp,
                        bottom = miniContentBottom(tuning)
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniTrackContent(
                    style = style,
                    tuning = tuning,
                    model = model,
                    palette = palette,
                    layout = layout,
                    swipe = swipe,
                    animated = animated,
                    liveRadio = liveRadio,
                    anchors = morphAnchors.takeIf { interactive },
                    compaction = dockCompaction,
                    artworkHidden = artworkHidden
                )
                MiniStyledControls(
                    style = style,
                    tuning = tuning,
                    palette = palette,
                    isPlaying = model.isPlaying,
                    isResolving = model.isResolving,
                    liveRadio = liveRadio,
                    progress = progress,
                    animated = animated,
                    compaction = dockCompaction,
                    playbackActions = playbackActions,
                    isFavorite = model.isFavorite,
                    onToggle = {
                        if (tuning.motion == MiniMotion.Expressive) haptics.perform(LevyraHapticAction.Transport)
                        playbackActions.toggle()
                    }
                )
            }
            if (!liveRadio) {
                MiniBottomProgress(
                    style = style,
                    tuning = tuning,
                    palette = palette,
                    progress = progress,
                    attach = attach,
                    height = layout.height,
                    waving = model.isPlaying && animated
                )
            }
        }
    }
}

@Composable
private fun rememberConnectedSeam(
    playing: Boolean,
    animated: Boolean,
    attach: () -> Float,
    fuse: () -> Float
): () -> Dp {
    val rest = animateDpAsState(
        targetValue = if (playing) ConnectedSeamPlaying else ConnectedSeamPaused,
        animationSpec = if (animated) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        } else {
            snap()
        },
        label = "connected-mini-seam"
    )
    return remember(rest, attach, fuse) {
        {
            val joined = lerp(rest.value, ConnectedSeamFused, fuse().coerceIn(0f, 1f))
            lerp(StyledMiniFloatingBottom, joined, attach().coerceIn(0f, 1f))
        }
    }
}

private fun connectedMiniShape(outer: Dp, attach: Float, fuse: Float): Shape {
    val inner = dockLinkInnerCorner(outer, attach, fuse)
    return RoundedCornerShape(topStart = outer, topEnd = outer, bottomEnd = inner, bottomStart = inner)
}

private fun Modifier.dockBottomGap(gap: () -> Dp): Modifier = layout { measurable, constraints ->
    val extra = gap().roundToPx().coerceAtLeast(0)
    val placeable = measurable.measure(constraints.offset(vertical = -extra))
    layout(placeable.width, placeable.height + extra) { placeable.place(0, 0) }
}

private fun Modifier.connectedSeamBridge(
    seam: () -> Dp,
    attach: () -> Float,
    progress: () -> Float,
    palette: MiniPlayerPalette,
    gutter: Dp
): Modifier = drawBehind {
    val presence = attach().coerceIn(0f, 1f)
    val gap = seam().toPx()
    val start = (gutter + ConnectedBridgeInset).toPx()
    val length = size.width - start * 2f
    if (presence <= 0f || gap <= 0f || length <= 0f) return@drawBehind
    val thickness = (gap - 1.dp.toPx()).coerceIn(ConnectedBridgeMin.toPx(), ConnectedBridgeMax.toPx())
    val top = size.height - gap / 2f - thickness / 2f
    val corner = CornerRadius(thickness / 2f)
    drawRoundRect(
        color = palette.surface,
        topLeft = Offset(start, top),
        size = Size(length, thickness),
        cornerRadius = corner,
        alpha = presence
    )
    val filled = length * progress().coerceIn(0f, 1f)
    if (filled > 0f) {
        val left = if (layoutDirection == LayoutDirection.Rtl) start + length - filled else start
        drawRoundRect(
            color = palette.accent,
            topLeft = Offset(left, top),
            size = Size(filled, thickness),
            cornerRadius = corner,
            alpha = presence
        )
    }
}

private fun morphedSeam(morph: DockMorph?, seam: () -> Dp): () -> Dp =
    if (morph == null) seam else { { seam() * (1f - morph.fraction) } }

private fun miniDockCompaction(style: MiniPlayerStyle, compaction: () -> Float): () -> Float =
    if (style == MiniPlayerStyle.YtMusic || LevyraIsAppleMusic) StyledMiniFixedDock else compaction

private fun miniContentBottom(tuning: MiniPlayerTuning): Dp =
    if (tuning.progress == MiniProgressStyle.Wavy) 4.dp else 0.dp

@Composable
private fun rememberMiniInteraction(
    interactive: Boolean,
    model: MiniPlayerModel,
    liveRadio: Boolean,
    playbackActions: MiniPlayerPlaybackActions,
    expansionActions: MiniPlayerExpansionActions,
    swipe: PlayerSwipeMotion
): Modifier {
    if (!interactive) return Modifier
    return Modifier.miniInteraction(
        trackId = model.track.id,
        model = model,
        horizontalGesturesEnabled = miniPlayerHorizontalGesturesEnabled(
            gesturesEnabled = model.gesturesEnabled,
            swipeTrackChangeEnabled = model.swipeTrackChangeEnabled,
            liveRadio = liveRadio
        ),
        rightToLeft = LocalLayoutDirection.current == LayoutDirection.Rtl,
        playbackActions = playbackActions,
        expansionActions = expansionActions,
        swipe = swipe,
        expandLabel = LocalLevyraStrings.current.expandPlayer
    )
}

@Composable
private fun MiniBackdropLayers(
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    track: Track,
    palette: MiniPlayerPalette,
    progress: () -> Float,
    liveRadio: Boolean
) {
    if (style == MiniPlayerStyle.ArtworkFocus && tuning.background == MiniBackground.Artwork) {
        MiniArtworkBackdrop(track = track, palette = palette)
    }
    if (tuning.progress == MiniProgressStyle.Fill && !liveRadio) {
        MiniFillProgress(progress = progress, palette = palette)
    }
}

@Composable
private fun RowScope.MiniTrackContent(
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    model: MiniPlayerModel,
    palette: MiniPlayerPalette,
    layout: StyledMiniLayout,
    swipe: PlayerSwipeMotion,
    animated: Boolean,
    liveRadio: Boolean,
    anchors: PlayerMorphAnchors?,
    compaction: () -> Float,
    artworkHidden: () -> Boolean
) {
    val track = model.track
    val motion = LocalLevyraVisualCapabilities.current.microMotion && animated
    val radioStrings = LevyraLiveRadioCatalog.forCode(LocalLevyraStrings.current.code)
    AnimatedContent(
        targetState = track,
        modifier = Modifier
            .weight(1f)
            .clipToBounds()
            .onSizeChanged { size -> swipe.carryLimitPx = size.width * StyledMiniCarryFraction },
        transitionSpec = { LevyraMotion.miniTrackSwap(animated) },
        contentKey = { it.id },
        label = "styled-mini-track"
    ) { shownTrack ->
        val current = shownTrack.id == track.id
        Row(
            modifier = Modifier.graphicsLayer {
                val offset = swipe.offsetFor(shownTrack.id)
                translationX = offset
                alpha = playerSwipeContentAlpha(offset, size.width)
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (style == MiniPlayerStyle.UltraCompact) 10.dp else 12.dp)
        ) {
            MiniStyledArtwork(
                track = shownTrack,
                style = style,
                tuning = tuning,
                palette = palette,
                playing = current && model.isPlaying,
                animated = animated,
                modifier = Modifier
                    .morphAnchorWhen(current, anchors, PlayerMorphSlot.Mini)
                    .dockLerpSize(compaction, layout.artworkSize, layout.artworkCompact)
                    .animateEnterExit(enter = LevyraMotion.miniArtworkEnter(motion), exit = ExitTransition.None)
                    .graphicsLayer { alpha = if (current && artworkHidden()) 0f else 1f }
            )
            MiniStyledMetadata(
                track = shownTrack,
                style = style,
                tuning = tuning,
                palette = palette,
                subtitle = miniSubtitle(shownTrack, liveRadio, model.liveNowPlaying, radioStrings.live),
                playing = current && model.isPlaying,
                animated = animated,
                compaction = compaction,
                modifier = Modifier
                    .weight(1f)
                    .animateEnterExit(
                        enter = LevyraMotion.miniMetadataEnter(motion, model.stepDirection),
                        exit = ExitTransition.None
                    )
            )
        }
    }
}

private fun miniSubtitle(track: Track, liveRadio: Boolean, nowPlaying: String, liveLabel: String): String =
    if (liveRadio) nowPlaying.ifBlank { "$liveLabel / ${track.artist}" } else track.artist

@Composable
private fun Modifier.miniInteraction(
    trackId: String,
    model: MiniPlayerModel,
    horizontalGesturesEnabled: Boolean,
    rightToLeft: Boolean,
    playbackActions: MiniPlayerPlaybackActions,
    expansionActions: MiniPlayerExpansionActions,
    swipe: PlayerSwipeMotion,
    expandLabel: String
): Modifier {
    val haptics = LocalLevyraHaptics.current
    return playerAxisDragGestures(
        key = "$trackId:$horizontalGesturesEnabled",
        enabled = model.gesturesEnabled,
        rightToLeft = rightToLeft,
        edgeZonesEnabled = false
    ) { event ->
        handleMiniPlayerDragEvent(
            event = event,
            trackId = trackId,
            playbackActions = playbackActions,
            expansionActions = expansionActions,
            haptics = haptics,
            horizontalGesturesEnabled = horizontalGesturesEnabled,
            swipe = swipe
        )
    }
        .semantics { onClick(label = expandLabel, action = null) }
        .levyraPressable(onClick = playbackActions.open, pressedScale = LevyraPressScale.Row)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BoxScope.MiniBottomProgress(
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    palette: MiniPlayerPalette,
    progress: () -> Float,
    attach: () -> Float,
    height: Dp,
    waving: Boolean
) {
    when (tuning.progress) {
        MiniProgressStyle.Line -> when (style) {
            MiniPlayerStyle.YtMusic -> MiniEdgeProgress(
                progress = progress,
                palette = palette,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            MiniPlayerStyle.Connected -> MiniLineProgress(
                progress = progress,
                palette = palette,
                inset = (tuning.cornerDp.dp * 0.7f).coerceAtLeast(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = 1f - attach().coerceIn(0f, 1f) }
            )
            else -> MiniLineProgress(
                progress = progress,
                palette = palette,
                inset = if (LevyraIsSpotify) 8.dp else (tuning.cornerDp.dp * 0.7f).coerceAtLeast(12.dp),
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        MiniProgressStyle.Wavy -> LinearWavyProgressIndicator(
            progress = progress,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = height * 0.42f, end = height * 0.42f, bottom = 4.dp)
                .height(8.dp),
            color = palette.accent.playerMix(Color.White, 0.42f),
            trackColor = palette.content.copy(alpha = 0.14f),
            amplitude = { if (waving) 1f else 0f }
        )
        else -> Unit
    }
}

private fun miniContentStart(style: MiniPlayerStyle, tuning: MiniPlayerTuning): Dp {
    val vertical = (tuning.heightDp - tuning.artworkDp).dp / 2
    return when (style) {
        MiniPlayerStyle.SuperExpressive -> vertical.coerceIn(8.dp, 14.dp)
        MiniPlayerStyle.ArtworkFocus -> vertical.coerceIn(6.dp, 10.dp)
        MiniPlayerStyle.YtMusic -> StyledMiniYtMusicContentStart
        else -> vertical.coerceIn(6.dp, 12.dp)
    }
}

private fun Modifier.miniDepth(
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    palette: MiniPlayerPalette,
    shape: Shape
): Modifier {
    val accentElevation = (tuning.glow * 0.26f).dp
    val shadowElevation = (tuning.shadow * 0.22f).dp
    return when (style) {
        MiniPlayerStyle.SuperExpressive -> if (tuning.glow == 0) this else shadow(
            elevation = accentElevation,
            shape = shape,
            ambientColor = palette.accent.copy(alpha = 0.42f),
            spotColor = palette.accent.copy(alpha = 0.60f)
        )
        MiniPlayerStyle.LiquidGlass -> if (tuning.glow == 0) this else shadow(
            elevation = accentElevation * 0.7f,
            shape = shape,
            ambientColor = palette.accent.copy(alpha = 0.28f),
            spotColor = palette.accent.copy(alpha = 0.36f)
        )
        MiniPlayerStyle.LiquidCapsule -> shadow(
            elevation = LiquidCapsuleElevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.16f),
            spotColor = Color.Black.copy(alpha = 0.30f)
        )
        MiniPlayerStyle.ArtworkFocus -> if (tuning.shadow == 0) this else shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = palette.accent.copy(alpha = 0.30f),
            spotColor = Color.Black.copy(alpha = 0.50f)
        )
        else -> if (tuning.shadow == 0) this else shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.18f),
            spotColor = Color.Black.copy(alpha = 0.32f)
        )
    }
}

private fun Modifier.miniBackground(
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    palette: MiniPlayerPalette,
    glass: GlassBackdropState?,
    shape: Shape
): Modifier = when (style) {
    MiniPlayerStyle.LiquidGlass, MiniPlayerStyle.LiquidCapsule -> {
        val capsule = style == MiniPlayerStyle.LiquidCapsule
        val tint = palette.surface.playerMix(palette.accent, if (capsule) 0.04f else 0.10f)
        val alpha = tuning.opacity / 100f
        val solidAlpha = (alpha + 0.32f).coerceAtMost(0.96f)
        val sheen = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = if (capsule && !palette.onDark) 0.30f else 0.10f), Color.Transparent)
        )
        if (glass != null) {
            glassFrost(
                state = glass,
                blurRadius = if (capsule) 30.dp else 24.dp,
                refractionShape = shape,
                onDrawFrosted = {
                    drawRect(tint.copy(alpha = alpha))
                    drawRect(sheen)
                },
                onDrawFallback = {
                    drawRect(tint.copy(alpha = solidAlpha))
                    drawRect(sheen)
                }
            )
        } else {
            drawBehind {
                drawRect(tint.copy(alpha = (alpha + 0.12f).coerceAtMost(0.96f)))
                drawRect(sheen)
            }
        }
    }
    MiniPlayerStyle.Floating, MiniPlayerStyle.UltraCompact, MiniPlayerStyle.YtMusic,
    MiniPlayerStyle.Connected -> background(palette.surface)
    MiniPlayerStyle.ArtworkFocus -> if (tuning.background == MiniBackground.Artwork) {
        background(StyledMiniDarkBase)
    } else {
        miniExpressiveFill(palette, MiniBackground.Gradient)
    }
    else -> miniExpressiveFill(palette, tuning.background)
}

private fun Modifier.miniExpressiveFill(palette: MiniPlayerPalette, background: MiniBackground): Modifier =
    drawWithCache {
        val fill = if (background == MiniBackground.Solid) {
            Brush.horizontalGradient(
                listOf(
                    StyledMiniDarkBase.playerAmbienceMix(palette.accent, 0.46f),
                    StyledMiniDarkBase.playerAmbienceMix(palette.accent, 0.46f)
                )
            )
        } else {
            Brush.linearGradient(
                colorStops = arrayOf(
                    0f to StyledMiniDarkBase.playerAmbienceMix(palette.accent, 0.58f),
                    0.6f to StyledMiniDarkBase.playerAmbienceMix(palette.accentAlt, 0.34f),
                    1f to StyledMiniDarkBase.playerAmbienceMix(palette.accentAlt, 0.26f)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        }
        val glow = Brush.radialGradient(
            colors = listOf(palette.accent.copy(alpha = 0.30f), Color.Transparent),
            center = Offset(size.height * 0.5f, size.height * 0.5f),
            radius = size.height * 1.6f
        )
        val sheen = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent))
        onDrawWithContent {
            drawRect(fill)
            drawRect(glow)
            drawRect(sheen)
            drawContent()
        }
    }

private fun Modifier.miniOutline(style: MiniPlayerStyle, palette: MiniPlayerPalette, shape: Shape): Modifier =
    when (style) {
        MiniPlayerStyle.LiquidGlass -> border(
            width = LevyraPlayerDesign.Hairline,
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.05f))
            ),
            shape = shape
        )
        MiniPlayerStyle.LiquidCapsule -> border(
            width = 1.dp,
            brush = liquidCapsuleRim(palette.onDark),
            shape = shape
        )
        MiniPlayerStyle.Floating -> if (LevyraIsSpotify) this else if (LevyraIsAppleMusic) border(0.5.dp, AppleMusicDesign.chromeStroke, shape) else border(
            width = LevyraPlayerDesign.Hairline,
            color = palette.content.copy(alpha = 0.08f),
            shape = shape
        )
        else -> this
    }

@Composable
private fun MiniArtworkBackdrop(track: Track, palette: MiniPlayerPalette) {
    Box(modifier = Modifier.fillMaxSize()) {
        CoverImage(
            track = track,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.35f
                    scaleY = 1.35f
                    alpha = 0.85f
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val scrim = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to StyledMiniDarkBase.playerAmbienceMix(palette.accent, 0.30f).copy(alpha = 0.55f),
                            0.45f to StyledMiniDarkBase.playerAmbienceMix(palette.accent, 0.34f).copy(alpha = 0.86f),
                            1f to StyledMiniDarkBase.playerAmbienceMix(palette.accentAlt, 0.24f).copy(alpha = 0.94f)
                        )
                    )
                    onDrawBehind { drawRect(scrim) }
                }
        )
    }
}

@Composable
private fun MiniStyledArtwork(
    track: Track,
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    palette: MiniPlayerPalette,
    modifier: Modifier,
    playing: Boolean = false,
    animated: Boolean = false
) {
    if (LevyraIsSpotify) {
        Box(modifier.clip(SpotifyDesign.coverShape).background(SpotifyDesign.placeholder)) { CoverImage(track, Modifier.fillMaxSize()) }
        return
    }
    if (LevyraIsAppleMusic) {
        Box(modifier.clip(AppleMusicDesign.thumbShape).background(AppleMusicDesign.placeholder)) { CoverImage(track, Modifier.fillMaxSize()) }
        return
    }
    if (LevyraIsYouTubeMusic) {
        Box(modifier.background(YouTubeMusicDesign.placeholder)) { CoverImage(track, Modifier.fillMaxSize()) }
        return
    }
    if (LevyraIsQobuz) {
        Box(modifier.clip(QobuzDesign.artworkShape).background(QobuzDesign.placeholder)) { CoverImage(track, Modifier.fillMaxSize()) }
        return
    }
    if (LevyraIsPrism) {
        SkinSpinningArtwork(track, playing, animated, modifier)
        return
    }
    val shape = miniArtworkShape(tuning.artworkShape)
    val morph = LocalDockMorph.current.takeIf { style != MiniPlayerStyle.LiquidCapsule }
    if (morph != null) {
        Box(modifier = modifier) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 1f - morph.fraction }
                    .clip(shape)
                    .background(palette.accent.copy(alpha = 0.20f))
            ) {
                CoverImage(track, Modifier.fillMaxSize())
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val bloom = morph.fraction
                        alpha = bloom
                        rotationZ = MiniEclipseSpin * (1f - bloom)
                        this.shape = MiniEclipseShape
                        clip = true
                    }
                    .background(palette.accent.copy(alpha = 0.20f))
            ) {
                CoverImage(
                    track,
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = -MiniEclipseSpin * (1f - morph.fraction) }
                )
            }
        }
        return
    }
    Box(
        modifier = modifier
            .then(
                if (style == MiniPlayerStyle.ArtworkFocus) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.40f),
                        spotColor = Color.Black.copy(alpha = 0.50f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(palette.accent.copy(alpha = 0.20f))
    ) {
        CoverImage(track, Modifier.fillMaxSize())
    }
}

@Composable
private fun MiniStyledMetadata(
    track: Track,
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    palette: MiniPlayerPalette,
    subtitle: String,
    playing: Boolean,
    animated: Boolean,
    compaction: () -> Float,
    modifier: Modifier
) {
    val compact = style == MiniPlayerStyle.UltraCompact
    val editorial = tuning.typography == MiniTypography.Editorial
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (style == MiniPlayerStyle.SuperExpressive) {
                MiniEqualizerBars(
                    playing = playing,
                    animated = animated,
                    color = palette.accent.playerMix(Color.White, 0.45f),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(12.dp)
                )
            }
            Text(
                text = track.title,
                color = palette.content,
                style = if (LevyraIsSpotify) MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, lineHeight = 17.sp) else if (LevyraIsQobuz) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium) else if (LevyraIsYouTubeMusic) MaterialTheme.typography.titleMedium else if (LevyraIsAppleMusic) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, lineHeight = 16.sp) else miniTitleStyle(tuning.typography, compact),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.levyraMarquee(enabled = animated, repeatDelayMillis = 3_200)
            )
        }
        Text(
            text = if (editorial) subtitle.uppercase() else subtitle,
            color = palette.contentMuted,
            style = if (LevyraIsSpotify) MaterialTheme.typography.bodySmall else if (LevyraIsQobuz) MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp) else if (LevyraIsYouTubeMusic) MaterialTheme.typography.bodyLarge else if (LevyraIsAppleMusic) MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal) else miniArtistStyle(tuning.typography, compact),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (compact) Modifier else Modifier.dockFoldHeight(compaction)
        )
    }
}

@Composable
private fun MiniEqualizerBars(playing: Boolean, animated: Boolean, color: Color, modifier: Modifier) {
    if (playing && animated) {
        MiniMovingEqualizerBars(color = color, modifier = modifier)
    } else {
        val levels = if (playing) MiniEqualizerPlayingLevels else MiniEqualizerRestingLevels
        Canvas(modifier = modifier) { drawMiniEqualizer(levels, color) }
    }
}

private val MiniEqualizerPlayingLevels = floatArrayOf(0.7f, 1f, 0.55f)
private val MiniEqualizerRestingLevels = floatArrayOf(0.35f, 0.35f, 0.35f)

@Composable
private fun MiniMovingEqualizerBars(color: Color, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "styled-mini-eq")
    val first = transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "styled-mini-eq-1"
    )
    val second = transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(380), RepeatMode.Reverse),
        label = "styled-mini-eq-2"
    )
    val third = transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(640), RepeatMode.Reverse),
        label = "styled-mini-eq-3"
    )
    Canvas(modifier = modifier) {
        drawMiniEqualizer(floatArrayOf(first.value, second.value, third.value), color)
    }
}

private fun DrawScope.drawMiniEqualizer(levels: FloatArray, color: Color) {
    val barWidth = size.width / 5f
    for (index in levels.indices) {
        val barHeight = size.height * levels[index]
        drawRoundRect(
            color = color,
            topLeft = Offset(index * barWidth * 2f, size.height - barHeight),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f)
        )
    }
}

@Composable
private fun MiniLineProgress(
    progress: () -> Float,
    palette: MiniPlayerPalette,
    inset: Dp,
    modifier: Modifier
) {
    val emphasis = if (palette.onDark) Color.White else Color.Black
    val spotify = LevyraIsSpotify
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = inset, end = inset, bottom = if (spotify) 0.dp else 4.dp)
            .height(if (spotify) 2.dp else StyledMiniLineHeight)
            .drawBehind {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(color = palette.content.copy(alpha = if (spotify) 0.3f else 0.14f), cornerRadius = radius)
                val width = size.width * progress().coerceIn(0f, 1f)
                val start = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                drawRoundRect(
                    color = palette.accent.playerMix(emphasis, 0.25f),
                    topLeft = Offset(start, 0f),
                    size = Size(width, size.height),
                    cornerRadius = radius
                )
            }
    )
}

@Composable
private fun MiniEdgeProgress(progress: () -> Float, palette: MiniPlayerPalette, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(StyledMiniEdgeProgressHeight)
            .drawBehind {
                drawRect(color = if (LevyraIsYouTubeMusic) YouTubeMusicDesign.divider else palette.content.copy(alpha = 0.18f))
                val width = size.width * progress().coerceIn(0f, 1f)
                val start = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                drawRect(color = palette.content, topLeft = Offset(start, 0f), size = Size(width, size.height))
            }
    )
}

@Composable
private fun MiniStyledControls(
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    palette: MiniPlayerPalette,
    isPlaying: Boolean,
    isResolving: Boolean,
    liveRadio: Boolean,
    progress: () -> Float,
    animated: Boolean,
    compaction: () -> Float,
    playbackActions: MiniPlayerPlaybackActions,
    isFavorite: Boolean,
    onToggle: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    if (LevyraIsSpotify && !liveRadio) {
        SpotifyMiniControls(isPlaying, isResolving, isFavorite, playbackActions.favorite, onToggle)
        return
    }
    if (LevyraIsQobuz && !liveRadio) {
        QobuzMiniControls(isPlaying, isResolving, isFavorite, playbackActions.favorite, onToggle)
        return
    }
    val skipSize = 38.dp * (tuning.controlScale / 100f)
    val skipBackground = when (tuning.buttons) {
        MiniButtonStyle.Plain, MiniButtonStyle.Outlined -> Color.Transparent
        else -> palette.content.copy(alpha = 0.10f)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (tuning.controls == MiniControlSet.Full && !liveRadio) {
            Box(modifier = Modifier.dockFoldWidth(compaction)) {
                MiniStyledIconButton(
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = strings.previous,
                    tint = palette.content,
                    background = skipBackground,
                    size = skipSize,
                    onClick = playbackActions.previous
                )
            }
        }
        MiniStyledPlayButton(
            style = style,
            tuning = tuning,
            palette = palette,
            isPlaying = isPlaying,
            isResolving = isResolving,
            liveRadio = liveRadio,
            progress = progress,
            animated = animated,
            onToggle = onToggle
        )
        Row(modifier = Modifier.dockFoldWidth(compaction), verticalAlignment = Alignment.CenterVertically) {
            if (tuning.controls != MiniControlSet.Essential && !liveRadio) {
                MiniStyledIconButton(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = strings.next,
                    tint = palette.content,
                    background = skipBackground,
                    size = skipSize,
                    onClick = playbackActions.next
                )
            }
            if (tuning.showClose) {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 40.dp, minHeight = LevyraPlayerDesign.MinimumTouchTarget)
                        .levyraPressable(onClick = playbackActions.close, pressedScale = LevyraPressScale.Control),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = strings.closePlayer,
                        tint = palette.content.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStyledIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    background: Color,
    size: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = LevyraPlayerDesign.MinimumTouchTarget, minHeight = LevyraPlayerDesign.MinimumTouchTarget)
            .levyraPressable(onClick = onClick, pressedScale = LevyraPressScale.Control),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.58f))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MiniStyledPlayButton(
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    palette: MiniPlayerPalette,
    isPlaying: Boolean,
    isResolving: Boolean,
    liveRadio: Boolean,
    progress: () -> Float,
    animated: Boolean,
    onToggle: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val baseCore = if (style == MiniPlayerStyle.UltraCompact) 34.dp else 40.dp
    val core = baseCore * (tuning.controlScale / 100f)
    val ring = tuning.progress == MiniProgressStyle.Ring
    val outer = if (ring) core + 16.dp else core
    val morphShape = rememberLevyraPlayMorphShape(playing = isPlaying, animated = animated)
    val coreShape = if (style == MiniPlayerStyle.SuperExpressive) morphShape else CircleShape
    val fillColor = miniPlayFillColor(tuning.buttons, palette)
    val glyphColor = miniPlayGlyphColor(tuning.buttons, palette, fillColor)
    Box(
        modifier = Modifier
            .sizeIn(minWidth = LevyraPlayerDesign.MinimumTouchTarget, minHeight = LevyraPlayerDesign.MinimumTouchTarget)
            .size(outer)
            .semantics(mergeDescendants = true) {}
            .levyraPressable(onClick = onToggle, pressedScale = LevyraPressScale.Control),
        contentAlignment = Alignment.Center
    ) {
        if (ring) {
            MiniPlayRing(
                palette = palette,
                size = outer,
                isPlaying = isPlaying,
                isResolving = isResolving,
                liveRadio = liveRadio,
                progress = progress,
                animated = animated
            )
        }
        MiniPlayCore(
            size = core,
            shape = coreShape,
            buttons = tuning.buttons,
            fill = fillColor,
            glyph = glyphColor,
            outline = palette.content.copy(alpha = 0.45f),
            loading = isResolving && !ring,
            isPlaying = isPlaying,
            description = if (isPlaying) strings.pause else strings.play
        )
    }
}

@Composable
private fun MiniPlayCore(
    size: Dp,
    shape: Shape,
    buttons: MiniButtonStyle,
    fill: Color,
    glyph: Color,
    outline: Color,
    loading: Boolean,
    isPlaying: Boolean,
    description: String
) {
    val border = if (buttons == MiniButtonStyle.Outlined) Modifier.border(BorderStroke(1.5.dp, outline), shape) else Modifier
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(fill)
            .then(border),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            LevyraLoadingIndicator(modifier = Modifier.size(size * 0.7f), color = glyph)
        } else {
            LevyraPlayPauseGlyph(
                playing = isPlaying,
                color = glyph,
                contentDescription = description,
                modifier = Modifier.size(if (buttons == MiniButtonStyle.Plain) size * 0.68f else size * 0.5f)
            )
        }
    }
}

private fun miniPlayFillColor(buttons: MiniButtonStyle, palette: MiniPlayerPalette): Color = when (buttons) {
    MiniButtonStyle.Filled -> if (LevyraIsIvory) IvoryDesign.ink else if (palette.onDark) palette.accent.playerMix(Color.White, 0.62f) else palette.accent
    MiniButtonStyle.Tonal -> palette.accent.copy(alpha = if (palette.onDark) 0.30f else 0.18f)
    else -> Color.Transparent
}

private fun miniPlayGlyphColor(buttons: MiniButtonStyle, palette: MiniPlayerPalette, fill: Color): Color = when (buttons) {
    MiniButtonStyle.Filled -> if (fill.luminance() > 0.45f) PlayerDarkSurface else Color.White
    MiniButtonStyle.Tonal -> palette.accent.playerMix(
        if (palette.onDark) Color.White else Color.Black,
        if (palette.onDark) 0.70f else 0.25f
    )
    else -> palette.content
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MiniPlayRing(
    palette: MiniPlayerPalette,
    size: Dp,
    isPlaying: Boolean,
    isResolving: Boolean,
    liveRadio: Boolean,
    progress: () -> Float,
    animated: Boolean
) {
    val ringColor = palette.accent.playerMix(
        if (palette.onDark) Color.White else Color.Black,
        if (palette.onDark) 0.62f else 0.15f
    )
    val ringTrack = palette.content.copy(alpha = 0.14f)
    val ringStroke = with(LocalDensity.current) { Stroke(width = StyledMiniRingStroke.toPx(), cap = StrokeCap.Round) }
    when {
        isResolving -> CircularWavyProgressIndicator(
            modifier = Modifier.size(size),
            color = ringColor,
            trackColor = ringTrack,
            stroke = ringStroke,
            trackStroke = ringStroke
        )
        else -> CircularWavyProgressIndicator(
            progress = if (liveRadio) FullRingProgress else progress,
            modifier = Modifier.size(size),
            color = ringColor,
            trackColor = ringTrack,
            stroke = ringStroke,
            trackStroke = ringStroke,
            amplitude = { if (isPlaying && animated && !liveRadio) 1f else 0f }
        )
    }
}

@Composable
internal fun MiniPlayerStylePreview(
    style: MiniPlayerStyle,
    tuning: MiniPlayerTuning,
    model: MiniPlayerModel,
    glass: GlassBackdropState?,
    modifier: Modifier = Modifier
) {
    val playback = remember { MiniPlayerPlaybackActions({}, {}, {}, {}, {}) }
    val expansion = remember { MiniPlayerExpansionActions({}, {}, {}) }
    val anchors = remember { PlayerMorphAnchors() }
    Box(modifier = modifier) {
        if (style == MiniPlayerStyle.Original) {
            MiniPlayer(
                model = model.copy(gesturesEnabled = false),
                morphAnchors = anchors,
                compaction = { 0f },
                artworkHidden = { false },
                playbackActions = playback,
                expansionActions = expansion
            )
        } else {
            StyledMiniPlayer(
                style = style,
                tuning = tuning,
                model = model,
                morphAnchors = null,
                compaction = { 0f },
                artworkHidden = { false },
                playbackActions = playback,
                expansionActions = expansion,
                glass = glass,
                interactive = false
            )
        }
    }
}

@Composable
private fun SkinSpinningArtwork(track: Track, playing: Boolean, animated: Boolean, modifier: Modifier) {
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(playing, animated) {
        if (!playing || !animated) return@LaunchedEffect
        while (true) {
            rotation.snapTo(rotation.value % 360f)
            rotation.animateTo(rotation.value + 360f, tween(14_000, easing = androidx.compose.animation.core.LinearEasing))
        }
    }
    Box(modifier.clip(CircleShape).background(PrismAccent.gradient()).padding(2.dp).clip(CircleShape).background(LevyraBlack).padding(1.dp)) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation.value }.clip(CircleShape)) {
            CoverImage(track, Modifier.fillMaxSize())
        }
        Box(Modifier.align(Alignment.Center).size(7.dp).clip(CircleShape).background(LevyraBlack))
    }
}
