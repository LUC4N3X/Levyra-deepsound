package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.components.carouselDepthEnabled
import com.luc4n3x.levyra.ui.components.levyraCarouselDepth
import com.luc4n3x.levyra.domain.Track
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal fun homeCanvasColor(isLight: Boolean): Color =
    if (isLight) LevyraHomeDesign.CanvasLight else LevyraBlack

@Composable
internal fun LevyraHomeAtmosphere(
    accentStart: Color,
    accentEnd: Color,
    isLight: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = animateColorAsState(
        targetValue = accentStart,
        animationSpec = if (animationsEnabled) tween(620) else snap(),
        label = "homeAuraPrimary"
    )
    val secondary = animateColorAsState(
        targetValue = accentEnd,
        animationSpec = if (animationsEnabled) tween(620) else snap(),
        label = "homeAuraSecondary"
    )
    val drift: State<Float> = if (animationsEnabled && LocalLevyraVisualCapabilities.current.decorativeMotion) {
        rememberInfiniteTransition(label = "homeAuraDrift").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(HomeAuroraCycleMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "homeAuraPhase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawHomeAurora(
                    canvas = homeCanvasColor(isLight),
                    primary = primary.value,
                    secondary = secondary.value,
                    phase = drift.value,
                    isLight = isLight
                )
            }
    )
}

private fun DrawScope.drawHomeAurora(
    canvas: Color,
    primary: Color,
    secondary: Color,
    phase: Float,
    isLight: Boolean
) {
    drawRect(canvas)
    val width = size.width
    val height = min(size.height, HomeAuroraHeight.toPx())
    if (width <= 0f || height <= 0f) return
    val angle = phase * 2f * PI.toFloat()
    val strength = if (isLight) 0.22f else 0.55f
    val blend = blendHomeAccents(primary, secondary)

    drawAuroraBlob(
        color = primary,
        alpha = strength,
        center = Offset(
            x = width * (0.16f + 0.10f * sin(angle)),
            y = height * (0.10f + 0.06f * cos(angle))
        ),
        radius = width * 0.82f
    )
    drawAuroraBlob(
        color = secondary,
        alpha = strength * 0.86f,
        center = Offset(
            x = width * (0.88f + 0.08f * cos(angle * 2f)),
            y = height * (0.26f + 0.07f * sin(angle))
        ),
        radius = width * 0.74f
    )
    drawAuroraBlob(
        color = blend,
        alpha = strength * 0.55f,
        center = Offset(
            x = width * (0.50f + 0.18f * sin(angle * 2f + 1.3f)),
            y = height * (0.52f + 0.05f * cos(angle * 2f))
        ),
        radius = width * 0.62f
    )
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.55f to canvas.copy(alpha = 0.35f),
                1f to canvas
            ),
            startY = 0f,
            endY = height
        ),
        size = Size(width, height)
    )
}

private fun DrawScope.drawAuroraBlob(color: Color, alpha: Float, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = alpha),
                0.45f to color.copy(alpha = alpha * 0.42f),
                1f to Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private const val HomeAuroraCycleMs = 24_000
private val HomeAuroraHeight = 620.dp

private fun blendHomeAccents(first: Color, second: Color): Color = Color(
    red = (first.red + second.red) / 2f,
    green = (first.green + second.green) / 2f,
    blue = (first.blue + second.blue) / 2f,
    alpha = 1f
)

@Composable
internal fun HomeGenreChips(
    zones: List<ExploreZone>,
    contentPadding: PaddingValues,
    onSelect: (ExploreZone) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(zones, key = { zone -> "home-chip-${zone.id}" }) { zone ->
            Text(
                text = zone.label,
                color = LevyraText,
                style = LevyraType.cardTitle,
                maxLines = 1,
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(HomeChipShape)
                    .background(LevyraText.copy(alpha = 0.10f))
                    .clickable(role = Role.Button) { onSelect(zone) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

private val HomeChipShape = RoundedCornerShape(10.dp)

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
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onPlay)
    ) {
        Text(
            text = rankText,
            color = LevyraText.copy(alpha = 0.92f),
            style = TextStyle(
                fontSize = 150.sp,
                lineHeight = 150.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-10).sp,
                drawStyle = Stroke(width = 5f)
            ),
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(y = 26.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(132.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CoverImage(
                track = track,
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (active) Modifier.border(2.dp, LevyraCyan, RoundedCornerShape(14.dp)) else Modifier
                    ),
                highRes = false
            )
            Text(
                text = track.title,
                color = if (active) LevyraCyan else LevyraText,
                style = LevyraType.cardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = LevyraMuted,
                style = LevyraType.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
