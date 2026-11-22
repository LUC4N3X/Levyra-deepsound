package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.FolioArtworkMorph
import com.luc4n3x.levyra.ui.components.LevyraMorphShape
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import android.graphics.Bitmap
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.toBitmap
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

private val FolioScrollingStage = 300.dp
private val FolioStageCorner = LevyraPlayerDesign.CornerLg
private val FolioControlHeight = 80.dp
private val FolioControlHeightCompact = 66.dp
private const val FolioArtistAlpha = 0.86f
private const val FolioSpinMillis = 60_000
private const val FolioFixedActionCount = 3

@Composable
internal fun PlayerFolioDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    isFavorite: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    labels: PlayerControlLabels,
    animated: Boolean,
    compact: Boolean,
    scrollable: Boolean,
    gutter: Dp,
    artworkCap: Dp,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    val metrics = folioDeckMetrics(compact)
    val seed = rememberFolioSeed(track, surfaces.glow)
    val tones = remember(seed) { folioTones(seed) }
    val paper by animateColorAsState(
        targetValue = tones.paper,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-folio-paper"
    )
    val ink by animateColorAsState(
        targetValue = tones.ink,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-folio-ink"
    )
    val inkContent by animateColorAsState(
        targetValue = tones.inkContent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-folio-ink-content"
    )
    val artworkShape = rememberFolioArtworkShape(isPlaying, animated)
    Column(
        modifier = modifier
            .background(paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(
                start = gutter,
                end = gutter,
                top = LevyraPlayerDesign.SpaceXs,
                bottom = metrics.bottom
            )
    ) {
        slots.header()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scrollable) Modifier.height(FolioScrollingStage) else Modifier.weight(1f))
                .padding(vertical = metrics.stagePadding),
            contentAlignment = Alignment.Center
        ) {
            val side = minOf(maxWidth, maxHeight, artworkCap)
            slots.stage(
                track,
                Modifier
                    .size(side)
                    .then(artworkShape),
                FolioStageCorner
            )
            slots.gestures(track, Modifier.matchParentSize())
        }
        FolioHeadline(
            track = track,
            ink = ink,
            animated = animated,
            compact = compact,
            onArtistClick = onArtistClick,
            modifier = headlineModifier
        )
        Spacer(Modifier.height(metrics.headlineGap))
        FolioTransport(
            slots = slots,
            isPlaying = isPlaying,
            isResolving = isResolving,
            labels = labels,
            metrics = metrics,
            ink = ink,
            inkContent = inkContent,
            onPrevious = onPrevious,
            onTogglePlay = onTogglePlay,
            onNext = onNext
        )
        Spacer(Modifier.height(metrics.actionGap))
        FolioActionRow(
            track = track,
            slots = slots,
            isFavorite = isFavorite,
            shuffleOn = shuffleOn,
            labels = labels,
            ink = ink,
            inkContent = inkContent,
            animated = animated,
            onToggleFavorite = onToggleFavorite,
            onShuffle = onShuffle
        )
    }
}

@Composable
private fun FolioTransport(
    slots: PlayerDeckSlots,
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    metrics: FolioDeckMetrics,
    ink: Color,
    inkContent: Color,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FolioPlayPill(
            isPlaying = isPlaying,
            isResolving = isResolving,
            labels = labels,
            height = metrics.controlHeight,
            ink = ink,
            inkContent = inkContent,
            onClick = onTogglePlay
        )
        FolioRoundButton(Icons.Rounded.SkipNext, labels.next, metrics.controlHeight, ink, inkContent, onNext)
    }
    Spacer(Modifier.height(metrics.transportGap))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceLg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FolioRoundButton(Icons.Rounded.SkipPrevious, labels.previous, metrics.controlHeight, ink, inkContent, onPrevious)
        Box(modifier = Modifier.weight(1f)) {
            slots.progress(PlayerProgressStyle.Wavy)
        }
    }
}

private class FolioDeckMetrics(
    val controlHeight: Dp,
    val bottom: Dp,
    val stagePadding: Dp,
    val headlineGap: Dp,
    val transportGap: Dp,
    val actionGap: Dp
)

private val FolioRegularMetrics = FolioDeckMetrics(
    controlHeight = FolioControlHeight,
    bottom = LevyraPlayerDesign.SpaceMd,
    stagePadding = LevyraPlayerDesign.SpaceLg,
    headlineGap = LevyraPlayerDesign.SpaceLg,
    transportGap = LevyraPlayerDesign.SpaceMd,
    actionGap = LevyraPlayerDesign.SpaceSm
)

private val FolioCompactMetrics = FolioDeckMetrics(
    controlHeight = FolioControlHeightCompact,
    bottom = LevyraPlayerDesign.SpaceSm,
    stagePadding = LevyraPlayerDesign.SpaceSm,
    headlineGap = LevyraPlayerDesign.SpaceMd,
    transportGap = LevyraPlayerDesign.SpaceSm,
    actionGap = LevyraPlayerDesign.SpaceXs
)

private fun folioDeckMetrics(compact: Boolean): FolioDeckMetrics =
    if (compact) FolioCompactMetrics else FolioRegularMetrics

@Composable
private fun FolioHeadline(
    track: Track,
    ink: Color,
    animated: Boolean,
    compact: Boolean,
    onArtistClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    AnimatedContent(
        targetState = track,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = { LevyraMotion.contentSwap(animated) },
        contentKey = { it.id },
        label = "player-folio-headline"
    ) { shown ->
        Column(modifier = Modifier.fillMaxWidth()) {
            val titleSize = folioTitleSize(shown.title, compact)
            Text(
                text = shown.title,
                color = ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = titleSize,
                lineHeight = titleSize * 1.06f,
                letterSpacing = (-0.6).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(LevyraPlayerDesign.SpaceSm))
            PlayerArtistText(
                track = shown,
                color = ink.copy(alpha = FolioArtistAlpha),
                style = TextStyle(
                    fontSize = if (compact) 15.sp else 18.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 2.4.sp
                ),
                onClickLabel = strings.openArtist,
                onArtistClick = onArtistClick,
                enabled = shown.id == track.id
            )
        }
    }
}

internal fun folioTitleSize(title: String, compact: Boolean): TextUnit {
    val base = when {
        title.length <= 10 -> 52f
        title.length <= 18 -> 44f
        title.length <= 30 -> 38f
        else -> 32f
    }
    return (if (compact) base * 0.82f else base).sp
}

@Composable
private fun RowScope.FolioPlayPill(
    isPlaying: Boolean,
    isResolving: Boolean,
    labels: PlayerControlLabels,
    height: Dp,
    ink: Color,
    inkContent: Color,
    onClick: () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val restCorner = levyraExpressiveToggleCorner(
        checked = isPlaying,
        unchecked = height * 0.3f,
        checkedCorner = height / 2,
        label = "player-folio-pill-corner"
    )
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = restCorner,
        pressed = height * 0.2f,
        label = "player-folio-pill-press"
    )
    val label = if (isPlaying) labels.pause else labels.play
    Box(
        modifier = Modifier
            .weight(1f)
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(ink)
            .levyraPressable(
                onClick = {
                    haptics.perform(LevyraHapticAction.Transport)
                    onClick()
                },
                interactionSource = interaction,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button
            )
            .semantics { contentDescription = label }
            .padding(horizontal = LevyraPlayerDesign.SpaceLg),
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) {
            LevyraLoadingIndicator(color = inkContent, modifier = Modifier.size(height * 0.44f))
        } else {
            Text(
                text = if (isPlaying) FolioPauseWord else FolioPlayWord,
                color = inkContent,
                fontSize = if (height < FolioControlHeight) 20.sp else 24.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FolioRoundButton(
    icon: ImageVector,
    label: String,
    size: Dp,
    ink: Color,
    inkContent: Color,
    onClick: () -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = size / 2,
        pressed = size * 0.28f,
        label = "player-folio-round-press"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(ink)
            .levyraPressable(
                onClick = {
                    haptics.perform(LevyraHapticAction.Transport)
                    onClick()
                },
                interactionSource = interaction,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(icon, inkContent, Modifier.size(size * 0.38f))
    }
}

@Composable
private fun FolioActionRow(
    track: Track,
    slots: PlayerDeckSlots,
    isFavorite: Boolean,
    shuffleOn: Boolean,
    labels: PlayerControlLabels,
    ink: Color,
    inkContent: Color,
    animated: Boolean,
    onToggleFavorite: () -> Unit,
    onShuffle: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val extras = slots.dockActions(track).filter { it.key in FolioDockKeys }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FolioActionPill(
            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            label = strings.favoritesPlain,
            active = isFavorite,
            ink = ink,
            inkContent = inkContent,
            onClick = onToggleFavorite,
            toggle = true,
            animated = animated
        )
        FolioActionPill(Icons.Rounded.Shuffle, labels.shuffle, shuffleOn, ink, inkContent, onShuffle, toggle = true, animated = animated)
        extras.forEach { action ->
            FolioActionPill(action.icon, action.label, action.active, ink, inkContent, action.onClick, action.enabled, action.toggle, animated)
        }
    }
}

@Composable
private fun RowScope.FolioActionPill(
    icon: ImageVector,
    label: String,
    active: Boolean,
    ink: Color,
    inkContent: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    toggle: Boolean = false,
    animated: Boolean = true
) {
    val haptics = LocalLevyraHaptics.current
    val spec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween<Color>())
    val container by animateColorAsState(if (active) ink else ink.copy(alpha = FolioPillRestAlpha), spec, label = "player-folio-pill")
    val tint by animateColorAsState(if (active) inkContent else ink, spec, label = "player-folio-pill-icon")
    Box(
        modifier = Modifier
            .weight(1f)
            .height(FolioPillHeight)
            .clip(RoundedCornerShape(50))
            .background(container)
            .levyraPressable(
                onClick = {
                    haptics.perform(LevyraHapticAction.Confirm)
                    onClick()
                },
                enabled = enabled,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button
            )
            .semantics {
                contentDescription = label
                if (toggle) toggleableState = androidx.compose.ui.state.ToggleableState(active)
            },
        contentAlignment = Alignment.Center
    ) {
        PlayerIcon(icon, tint.copy(alpha = if (enabled) 1f else 0.4f), Modifier.size(22.dp))
    }
}

private val FolioDockKeys = setOf("lyrics", "queue", "visual-mode")
private val FolioPillHeight = 52.dp
private const val FolioPillRestAlpha = 0.16f
private const val FolioPlayWord = "PLAY"
private const val FolioPauseWord = "PAUSE"

private object FolioSeedCache {
    private val seeds = android.util.LruCache<String, Int>(64)

    fun get(key: String): Int? = seeds.get(key)

    fun put(key: String, seed: Int) {
        seeds.put(key, seed)
    }
}

@Composable
private fun rememberFolioSeed(track: Track, fallback: Color): Color {
    val context = LocalContext.current
    val url = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
    val key = "${track.id}|$url"
    var seed by remember(key) { mutableStateOf(FolioSeedCache.get(key)) }
    LaunchedEffect(key) {
        if (seed != null || url.isBlank()) return@LaunchedEffect
        seed = loadFolioSeed(context, url)?.also { FolioSeedCache.put(key, it) }
    }
    return seed?.let(::Color) ?: fallback
}

private suspend fun loadFolioSeed(context: android.content.Context, url: String): Int? = withContext(Dispatchers.IO) {
    val request = ImageRequest.Builder(context)
        .data(LevyraArtworkCache.small(url))
        .size(FolioSeedSample, FolioSeedSample)
        .allowHardware(false)
        .bitmapConfig(Bitmap.Config.ARGB_8888)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .build()
    val bitmap = try {
        SingletonImageLoader.get(context).execute(request).image?.toBitmap()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null
    } ?: return@withContext null
    withContext(Dispatchers.Default) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        Score.score(QuantizerCelebi.quantize(pixels, FolioSeedColors), 1, FolioNeutralSeed, true).firstOrNull()
    }
}

private const val FolioSeedSample = 112
private const val FolioSeedColors = 128
private const val FolioNeutralSeed = 0xFF7A7A7A.toInt()

private class FolioTones(val paper: Color, val ink: Color, val inkContent: Color)

private fun folioTones(seed: Color): FolioTones {
    val source = Hct.fromInt(seed.copy(alpha = 1f).toArgb())
    val chroma = if (source.chroma < 6.0) source.chroma else source.chroma.coerceIn(30.0, 64.0)
    return FolioTones(
        paper = Color(Hct.from(source.hue, chroma, 24.0).toInt()),
        ink = Color(Hct.from(source.hue, minOf(chroma * 0.4, 20.0), 90.0).toInt()),
        inkContent = Color(Hct.from(source.hue, chroma, 22.0).toInt())
    )
}

@Composable
private fun rememberFolioArtworkShape(playing: Boolean, animated: Boolean): Modifier {
    val morph by animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = if (animated) spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow) else snap(),
        label = "player-folio-artwork-morph"
    )
    val spin = remember { androidx.compose.animation.core.Animatable(0f) }
    val spinning = animated && playing && LocalLevyraVisualCapabilities.current.decorativeMotion
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        while (true) {
            val start = spin.value % 360f
            spin.snapTo(start)
            spin.animateTo(start + 360f, tween(FolioSpinMillis, easing = LinearEasing))
        }
    }
    return Modifier.graphicsLayer {
        shape = LevyraMorphShape(FolioArtworkMorph, morph, spin.value)
        clip = true
    }
}
