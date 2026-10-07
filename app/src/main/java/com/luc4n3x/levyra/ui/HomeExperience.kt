package com.luc4n3x.levyra.ui

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import kotlin.math.min

internal fun homeCanvasColor(isLight: Boolean): Color =
    if (isLight) LevyraHomeDesign.CanvasLight else LevyraBlack

@Composable
internal fun LevyraHomeAtmosphere(
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawHomeBackdrop(
                    canvas = homeCanvasColor(isLight),
                    isLight = isLight
                )
            }
    )
}

private fun DrawScope.drawHomeBackdrop(canvas: Color, isLight: Boolean) {
    drawRect(canvas)
    val height = min(size.height, HomeBackdropHeight.toPx())
    if (size.width <= 0f || height <= 0f) return
    val blueCenter = Offset(size.width * 0.18f, 0f)
    val blueRadius = maxOf(size.width, height) * 0.92f
    val indigoCenter = Offset(size.width * 0.96f, height * 0.12f)
    val indigoRadius = size.width * 0.72f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                HomeBackdropBlue.copy(alpha = if (isLight) 0.09f else 0.13f),
                HomeBackdropBlue.copy(alpha = if (isLight) 0.025f else 0.035f),
                Color.Transparent
            ),
            center = blueCenter,
            radius = blueRadius
        ),
        center = blueCenter,
        radius = blueRadius
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                HomeBackdropIndigo.copy(alpha = if (isLight) 0.045f else 0.065f),
                Color.Transparent
            ),
            center = indigoCenter,
            radius = indigoRadius
        ),
        center = indigoCenter,
        radius = indigoRadius
    )
}

private val HomeBackdropHeight = 420.dp
private val HomeBackdropBlue = Color(0xFF0A84FF)
private val HomeBackdropIndigo = Color(0xFF5E5CE6)

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
