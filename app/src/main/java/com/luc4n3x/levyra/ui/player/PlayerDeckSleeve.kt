package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.PlayerDarkSurface
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities

private val SleeveScrollingStage = 300.dp
private const val SleeveSpread = 1.36f
private const val SleeveDiscScale = 0.94f
private const val SleeveDiscRest = 0.05f
private const val SleeveDiscOut = 0.4f
private const val SleeveLabelScale = 0.36f
private const val SleeveGrooveCount = 7
private const val SleeveSpinMillis = 4_800
private const val SleeveLabelDepth = 0.35f
private const val SleeveArtistAlpha = 0.72f

@Composable
internal fun PlayerSleeveDeck(
    track: Track,
    slots: PlayerDeckSlots,
    surfaces: PlayerSurfaceTokens,
    isFavorite: Boolean,
    queuePosition: PlayerDeckQueuePosition?,
    isPlaying: Boolean,
    animated: Boolean,
    compact: Boolean,
    scrollable: Boolean,
    gutter: Dp,
    artworkCap: Dp,
    stepDirection: Int,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    headlineModifier: Modifier = Modifier
) {
    val label by animateColorAsState(
        targetValue = surfaces.glow.copy(alpha = 1f),
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.paletteTween()),
        label = "player-sleeve-label"
    )
    val slide = animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = if (animated) {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow)
        } else {
            snap()
        },
        label = "player-sleeve-slide"
    )
    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(
                start = gutter,
                end = gutter,
                top = LevyraPlayerDesign.SpaceXs,
                bottom = if (compact) LevyraPlayerDesign.SpaceMd else LevyraPlayerDesign.SpaceXl
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        slots.header()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scrollable) Modifier.height(SleeveScrollingStage) else Modifier.weight(1f))
                .padding(vertical = if (compact) LevyraPlayerDesign.SpaceSm else LevyraPlayerDesign.SpaceLg),
            contentAlignment = Alignment.Center
        ) {
            val side = minOf(maxHeight, maxWidth / SleeveSpread, artworkCap)
            Box(modifier = Modifier.size(width = side * SleeveSpread, height = side)) {
                SleeveDisc(
                    label = label,
                    playing = isPlaying,
                    animated = animated,
                    slide = slide,
                    side = side,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(side * SleeveDiscScale)
                )
                slots.stage(track, Modifier.align(Alignment.CenterStart).size(side), null)
            }
            slots.gestures(track, Modifier.matchParentSize())
        }
        SleeveHeadline(
            track = track,
            surfaces = surfaces,
            isFavorite = isFavorite,
            queuePosition = queuePosition,
            animated = animated,
            compact = compact,
            stepDirection = stepDirection,
            onArtistClick = onArtistClick,
            onToggleFavorite = onToggleFavorite,
            modifier = headlineModifier
        )
        Spacer(Modifier.height(if (compact) LevyraPlayerDesign.SpaceSm else LevyraPlayerDesign.SpaceMd))
        slots.controlsWithoutMetadata(this, track)
    }
}

@Composable
private fun SleeveDisc(
    label: Color,
    playing: Boolean,
    animated: Boolean,
    slide: State<Float>,
    side: Dp,
    modifier: Modifier = Modifier
) {
    val spin = rememberSleeveSpin(playing, animated && LocalLevyraVisualCapabilities.current.decorativeMotion)
    val labelShade = label.playerMix(PlayerDarkSurface, SleeveLabelDepth)
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    Box(
        modifier = modifier.graphicsLayer {
            translationX = direction * (side * lerp(SleeveDiscRest, SleeveDiscOut, slide.value)).toPx()
        }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) { drawSleeveVinyl() }
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = spin.value }
        ) { drawSleeveLabel(label, labelShade) }
    }
}

@Composable
private fun rememberSleeveSpin(playing: Boolean, enabled: Boolean): Animatable<Float, AnimationVector1D> {
    val spin = remember { Animatable(0f) }
    LaunchedEffect(playing, enabled) {
        if (!playing || !enabled) return@LaunchedEffect
        while (true) {
            spin.animateTo(spin.value + 360f, tween(SleeveSpinMillis, easing = LinearEasing))
            spin.snapTo(spin.value % 360f)
        }
    }
    return spin
}

private fun DrawScope.drawSleeveVinyl() {
    val radius = size.minDimension / 2f
    drawCircle(Color(0xFF0B0B0D), radius)
    drawCircle(
        brush = Brush.sweepGradient(
            0f to Color.White.copy(alpha = 0.0f),
            0.12f to Color.White.copy(alpha = 0.10f),
            0.25f to Color.White.copy(alpha = 0.0f),
            0.62f to Color.White.copy(alpha = 0.0f),
            0.74f to Color.White.copy(alpha = 0.08f),
            0.86f to Color.White.copy(alpha = 0.0f),
            center = center
        ),
        radius = radius
    )
    val labelRadius = radius * SleeveLabelScale
    val step = (radius - labelRadius) / (SleeveGrooveCount + 1)
    for (index in 1..SleeveGrooveCount) {
        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = labelRadius + step * index,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun DrawScope.drawSleeveLabel(label: Color, labelShade: Color) {
    val radius = size.minDimension / 2f * SleeveLabelScale
    drawCircle(label, radius)
    drawArc(
        color = labelShade,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f)
    )
    drawCircle(Color(0xFF0B0B0D), radius * 0.12f)
}

@Composable
private fun SleeveHeadline(
    track: Track,
    surfaces: PlayerSurfaceTokens,
    isFavorite: Boolean,
    queuePosition: PlayerDeckQueuePosition?,
    animated: Boolean,
    compact: Boolean,
    stepDirection: Int,
    onArtistClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(LevyraPlayerDesign.MinimumTouchTarget)) {
                if (queuePosition != null) {
                    Text(
                        text = "${queuePosition.indexLabel}/${queuePosition.totalLabel}",
                        color = surfaces.contentFaint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        maxLines = 1
                    )
                }
            }
            val eyebrow = playerDeckAlbumEyebrow(track)
            Text(
                text = eyebrow,
                color = surfaces.contentFaint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = LevyraPlayerDesign.SpaceSm)
            )
            PlayerFavoriteButton(
                trackId = track.id,
                isFavorite = isFavorite,
                surfaces = surfaces,
                label = strings.favoritesPlain,
                animated = animated,
                onToggle = onToggleFavorite
            )
        }
        AnimatedContent(
            targetState = track,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = { LevyraMotion.trackChange(animated, stepDirection) },
            contentKey = { it.id },
            label = "player-sleeve-headline"
        ) { shown ->
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val titleSize = sleeveTitleSize(shown.title, compact)
                Text(
                    text = shown.title,
                    color = surfaces.content,
                    fontSize = titleSize,
                    lineHeight = titleSize * 1.08f,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(LevyraPlayerDesign.SpaceXs))
                PlayerArtistText(
                    track = shown,
                    color = surfaces.content.copy(alpha = SleeveArtistAlpha),
                    style = TextStyle(
                        fontSize = if (compact) 14.sp else 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    ),
                    onClickLabel = strings.openArtist,
                    onArtistClick = onArtistClick,
                    enabled = shown.id == track.id
                )
            }
        }
    }
}

internal fun sleeveTitleSize(title: String, compact: Boolean): TextUnit {
    val length = title.trim().length
    val base = when {
        length <= 14 -> 30f
        length <= 26 -> 26f
        length <= 40 -> 23f
        else -> 21f
    }
    return (if (compact) base - 3f else base).sp
}
