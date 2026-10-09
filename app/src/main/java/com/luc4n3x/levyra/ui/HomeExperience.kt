package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.components.LevyraCardCaption
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.carouselDepthEnabled
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.levyraCarouselDepth
import com.luc4n3x.levyra.domain.Track
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.border
import com.luc4n3x.levyra.ui.theme.LevyraType
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.domain.ExploreZone
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import kotlin.math.min

internal val LocalHomeCompactDensity = staticCompositionLocalOf { false }

@Composable
internal fun homeHeaderContentGap(): Dp =
    LevyraHomeDesign.headerContentGap(LocalHomeCompactDensity.current)

internal fun homeCanvasColor(isLight: Boolean): Color =
    if (isLight) LevyraHomeDesign.CanvasLight else LevyraBlack

@Composable
internal fun LevyraHomeAtmosphere(
    isLight: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val decorativeMotion = LocalLevyraVisualCapabilities.current.decorativeMotion
    val driftPhase = if (decorativeMotion) {
        val transition = rememberInfiniteTransition(label = "home-backdrop-drift")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 24_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "home-backdrop-phase"
        )
    } else {
        null
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawHomeBackdrop(
                    canvas = homeCanvasColor(isLight),
                    isLight = isLight,
                    accent = accent,
                    driftPhase = driftPhase?.value ?: 0.5f
                )
            }
    )
}

private fun DrawScope.drawHomeBackdrop(
    canvas: Color,
    isLight: Boolean,
    accent: Color,
    driftPhase: Float
) {
    drawRect(canvas)
    val height = min(size.height, HomeBackdropHeight.toPx())
    if (size.width <= 0f || height <= 0f) return

    val primary = mixHomeBackdropColor(accent, HomeBackdropBlue, 0.18f)
    val secondary = mixHomeBackdropColor(accent, HomeBackdropIndigo, 0.46f)
    val tertiary = mixHomeBackdropColor(accent, Color.White, if (isLight) 0.20f else 0.08f)
    val drift = (driftPhase.coerceIn(0f, 1f) - 0.5f) * 2f

    val primaryCenter = Offset(
        x = size.width * (0.18f + 0.06f * drift),
        y = height * (0.02f + 0.025f * drift)
    )
    val primaryRadius = maxOf(size.width, height) * 0.92f
    val secondaryCenter = Offset(
        x = size.width * (0.96f - 0.05f * drift),
        y = height * (0.12f + 0.035f * drift)
    )
    val secondaryRadius = size.width * 0.72f
    val tertiaryCenter = Offset(
        x = size.width * (0.52f - 0.035f * drift),
        y = height * (0.38f - 0.025f * drift)
    )
    val tertiaryRadius = maxOf(size.width, height) * 0.58f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primary.copy(alpha = if (isLight) 0.09f else 0.13f),
                primary.copy(alpha = if (isLight) 0.025f else 0.035f),
                Color.Transparent
            ),
            center = primaryCenter,
            radius = primaryRadius
        ),
        center = primaryCenter,
        radius = primaryRadius
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                secondary.copy(alpha = if (isLight) 0.045f else 0.065f),
                Color.Transparent
            ),
            center = secondaryCenter,
            radius = secondaryRadius
        ),
        center = secondaryCenter,
        radius = secondaryRadius
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                tertiary.copy(alpha = if (isLight) 0.028f else 0.04f),
                Color.Transparent
            ),
            center = tertiaryCenter,
            radius = tertiaryRadius
        ),
        center = tertiaryCenter,
        radius = tertiaryRadius
    )
}

private fun mixHomeBackdropColor(first: Color, second: Color, amount: Float): Color {
    val ratio = amount.coerceIn(0f, 1f)
    return Color(
        red = first.red + (second.red - first.red) * ratio,
        green = first.green + (second.green - first.green) * ratio,
        blue = first.blue + (second.blue - first.blue) * ratio,
        alpha = 1f
    )
}

private val HomeBackdropHeight = 420.dp
private val HomeChipVisualHeight = 40.dp
private val HomeChipRestCorner = 20.dp
private val HomeChipPressedCorner = 12.dp
private val HomeBackdropBlue = Color(0xFF0A84FF)
private val HomeBackdropIndigo = Color(0xFF5E5CE6)

@Composable
internal fun HomeGenreChips(
    zones: List<ExploreZone>,
    contentPadding: PaddingValues,
    onSelect: (ExploreZone) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(zones, key = { zone -> "home-chip-${zone.id}" }) { zone ->
            val interaction = remember(zone.id) { MutableInteractionSource() }
            val corner = if (LocalAnimationsEnabled.current) {
                levyraExpressiveCorner(
                    interactionSource = interaction,
                    rest = HomeChipRestCorner,
                    pressed = HomeChipPressedCorner,
                    label = "homeGenreChipCorner"
                )
            } else {
                HomeChipRestCorner
            }
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .levyraPressable(
                        onClick = { onSelect(zone) },
                        interactionSource = interaction,
                        pressedScale = LevyraPressScale.Control,
                        role = Role.Button
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(HomeChipVisualHeight)
                        .clip(RoundedCornerShape(corner))
                        .background(colors.surfaceContainerHigh.copy(alpha = 0.72f))
                        .border(1.dp, colors.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(corner))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = zone.label,
                        color = colors.onSurface,
                        style = LevyraType.cardTitle,
                        maxLines = 1
                    )
                }
            }
        }
    }
}


@Composable
internal fun HomeChartPodium(
    tracks: List<Track>,
    currentId: String?,
    contentPadding: PaddingValues,
    onPlay: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val depthEnabled = carouselDepthEnabled()
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(tracks, key = { index, track -> "chart-podium-$index-${track.id}" }) { index, track ->
            HomeChartPodiumCard(
                rank = index + 1,
                track = track,
                active = track.id == currentId,
                onPlay = { onPlay(track) },
                modifier = Modifier.levyraCarouselDepth(listState, "chart-podium-$index-${track.id}", depthEnabled)
            )
        }
    }
}

@Composable
private fun HomeChartPodiumCard(
    rank: Int,
    track: Track,
    active: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rankText = rank.toString()
    Box(
        modifier = modifier
            .size(width = if (rankText.length > 1) 236.dp else 196.dp, height = 200.dp)
            .clip(LevyraCardDesign.EditorialShape)
            .levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Tile)
    ) {
        Text(
            text = rankText,
            color = LevyraText.copy(alpha = 0.72f),
            style = TextStyle(
                fontSize = 112.sp,
                lineHeight = 112.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-7).sp,
                drawStyle = Stroke(width = 3.5f)
            ),
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 2.dp, y = 14.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(132.dp),
            verticalArrangement = Arrangement.spacedBy(LevyraCardDesign.CaptionTopGap)
        ) {
            Box(modifier = Modifier.size(132.dp)) {
                CoverImage(
                    track = track,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(PodiumArtworkShape),
                    highRes = false
                )
                if (active) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(40.dp)
                            .clip(podiumActiveBadgeShape())
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        LevyraPlayingIndicator(
                            playing = false,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            size = 16.dp
                        )
                    }
                }
            }
            LevyraCardCaption(
                title = track.title,
                subtitle = track.artist,
                titleColor = if (active) LevyraCyan else LevyraText,
                titleLines = 1
            )
        }
    }
}

private val PodiumArtworkShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun podiumActiveBadgeShape(): Shape = MaterialShapes.Cookie9Sided.toShape()
