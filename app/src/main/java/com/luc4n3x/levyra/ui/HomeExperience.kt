package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.Alignment
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import com.luc4n3x.levyra.ui.theme.LevyraType
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.domain.ExploreZone
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlin.math.min
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape

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
    if (com.luc4n3x.levyra.ui.theme.LevyraIsNocturne) {
        Box(modifier.fillMaxSize().background(LevyraBlack))
        return
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawHomeBackdrop(
                    canvas = homeCanvasColor(isLight),
                    isLight = isLight,
                    accent = accent
                )
            }
    )
}

private fun DrawScope.drawHomeBackdrop(
    canvas: Color,
    isLight: Boolean,
    accent: Color
) {
    drawRect(canvas)
    val height = min(size.height, HomeBackdropHeight.toPx())
    if (size.width <= 0f || height <= 0f) return

    val primary = mixHomeBackdropColor(accent, HomeBackdropBlue, 0.18f)
    val secondary = mixHomeBackdropColor(accent, HomeBackdropIndigo, 0.46f)
    val tertiary = mixHomeBackdropColor(accent, Color.White, if (isLight) 0.20f else 0.08f)

    val primaryCenter = Offset(
        x = size.width * 0.18f,
        y = height * 0.02f
    )
    val primaryRadius = maxOf(size.width, height) * 0.92f
    val secondaryCenter = Offset(
        x = size.width * 0.96f,
        y = height * 0.12f
    )
    val secondaryRadius = size.width * 0.72f
    val tertiaryCenter = Offset(
        x = size.width * 0.52f,
        y = height * 0.38f
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
                        .background(colors.surfaceContainerHigh)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = zone.label,
                        color = colors.onSurface,
                        style = LevyraType.cardTitle.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.1).sp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeChartPodium(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    contentPadding: PaddingValues,
    onPlay: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) return
    val carouselState = rememberCarouselState { tracks.size }
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = PodiumPreferredWidth,
        itemSpacing = 8.dp,
        contentPadding = contentPadding,
        modifier = modifier
            .fillMaxWidth()
            .height(PodiumCarouselHeight)
    ) { index ->
        val track = tracks[index]
        val active = track.id == currentId
        val shape = MaterialTheme.shapes.extraLarge
        Box(
            modifier = Modifier
                .fillMaxSize()
                .maskClip(shape)
                .levyraPressable(onClick = { onPlay(track) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
        ) {
            CoverImage(
                track = track,
                modifier = Modifier.fillMaxSize(),
                highRes = true
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PodiumScrim)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val info = carouselItemDrawInfo
                        translationX = info.maskRect.left
                        alpha = ((info.size - info.minSize) / (info.maxSize - info.minSize).coerceAtLeast(1f))
                            .coerceIn(0f, 1f)
                    }
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = track.title,
                    color = if (active) PodiumActiveInk else Color.White,
                    style = LevyraType.cardTitle.copy(fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.76f),
                    style = LevyraType.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .graphicsLayer { translationX = carouselItemDrawInfo.maskRect.left }
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.46f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (active) {
                    LevyraPlayingIndicator(
                        playing = isPlaying,
                        color = PodiumActiveInk,
                        size = 12.dp
                    )
                }
                Text(
                    text = "#${index + 1}",
                    color = Color.White,
                    style = LevyraType.overline.copy(fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.4.sp),
                    maxLines = 1
                )
            }
        }
    }
}

private val PodiumPreferredWidth = 236.dp
private val PodiumCarouselHeight = 252.dp
private val PodiumActiveInk = Color(0xFF8FE3FF)
private val PodiumScrim = Brush.verticalGradient(
    0.00f to Color.Black.copy(alpha = 0.30f),
    0.24f to Color.Transparent,
    0.55f to Color.Transparent,
    1.00f to Color.Black.copy(alpha = 0.78f)
)
