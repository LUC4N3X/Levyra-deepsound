package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet

/**
 * Artwork-led Home backdrop. It intentionally contains no always-running animation: palette changes
 * crossfade, while the wave and halos are cached drawing primitives that keep scrolling cheap.
 */
@Composable
internal fun LevyraHomeAtmosphere(
    accentStart: Color,
    accentEnd: Color,
    isLight: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val primary by animateColorAsState(
        targetValue = accentStart,
        animationSpec = if (animationsEnabled) tween(620) else snap(),
        label = "homeAuraPrimary"
    )
    val secondary by animateColorAsState(
        targetValue = accentEnd,
        animationSpec = if (animationsEnabled) tween(620) else snap(),
        label = "homeAuraSecondary"
    )

    Box(
        modifier = modifier
            .height(LevyraHomeDesign.AtmosphereHeight)
            .drawWithCache {
                val width = size.width
                val height = size.height
                val safeRadius = width.coerceAtLeast(1f)
                val base = if (isLight) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF9FAFF),
                            Color(0xFFF4F6FC),
                            Color(0xFFF1F3F8)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to LevyraHomeDesign.CanvasMid,
                            0.34f to LevyraHomeDesign.CanvasDark,
                            1f to Color.Black
                        )
                    )
                }
                val leftCenter = Offset(width * 0.12f, height * 0.06f)
                val rightCenter = Offset(width * 0.96f, height * 0.22f)
                val leftHalo = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = if (isLight) 0.16f else 0.28f),
                        primary.copy(alpha = if (isLight) 0.045f else 0.075f),
                        Color.Transparent
                    ),
                    center = leftCenter,
                    radius = safeRadius * 0.94f
                )
                val rightHalo = Brush.radialGradient(
                    colors = listOf(
                        secondary.copy(alpha = if (isLight) 0.10f else 0.20f),
                        secondary.copy(alpha = if (isLight) 0.028f else 0.055f),
                        Color.Transparent
                    ),
                    center = rightCenter,
                    radius = safeRadius * 0.78f
                )
                val wave = Path().apply {
                    moveTo(-width * 0.08f, height * 0.29f)
                    cubicTo(
                        width * 0.18f,
                        height * 0.19f,
                        width * 0.33f,
                        height * 0.37f,
                        width * 0.54f,
                        height * 0.25f
                    )
                    cubicTo(
                        width * 0.72f,
                        height * 0.15f,
                        width * 0.89f,
                        height * 0.31f,
                        width * 1.08f,
                        height * 0.21f
                    )
                }
                val echo = Path().apply {
                    moveTo(-width * 0.06f, height * 0.32f)
                    cubicTo(
                        width * 0.19f,
                        height * 0.23f,
                        width * 0.36f,
                        height * 0.41f,
                        width * 0.56f,
                        height * 0.29f
                    )
                    cubicTo(
                        width * 0.74f,
                        height * 0.19f,
                        width * 0.91f,
                        height * 0.34f,
                        width * 1.07f,
                        height * 0.25f
                    )
                }
                val waveBrush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        primary.copy(alpha = if (isLight) 0.08f else 0.16f),
                        secondary.copy(alpha = if (isLight) 0.06f else 0.12f),
                        Color.Transparent
                    )
                )
                val fadeTop = height * 0.46f
                val bottomFade = Brush.verticalGradient(
                    colors = if (isLight) {
                        listOf(Color.Transparent, Color(0xFFF1F3F8).copy(alpha = 0.86f), Color(0xFFF1F3F8))
                    } else {
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f), Color.Black)
                    },
                    startY = fadeTop,
                    endY = height
                )

                onDrawBehind {
                    drawRect(base)
                    drawCircle(leftHalo, radius = safeRadius * 0.94f, center = leftCenter)
                    drawCircle(rightHalo, radius = safeRadius * 0.78f, center = rightCenter)
                    drawPath(wave, brush = waveBrush, style = Stroke(width = 1.25.dp.toPx()))
                    drawPath(
                        echo,
                        color = if (isLight) primary.copy(alpha = 0.035f) else Color.White.copy(alpha = 0.035f),
                        style = Stroke(width = 0.75.dp.toPx())
                    )
                    drawRect(
                        brush = bottomFade,
                        topLeft = Offset(0f, fadeTop),
                        size = Size(width, height - fadeTop)
                    )
                }
            }
    )
}

@Composable
internal fun LevyraHomeQuickAccessGrid(
    currentTrack: Track?,
    mixTrack: Track?,
    favoriteTrack: Track?,
    releaseTrack: Track?,
    chartTrack: Track?,
    isPlaying: Boolean,
    isResolving: Boolean,
    hasMix: Boolean,
    hasFavorites: Boolean,
    hasNewReleases: Boolean,
    hasCharts: Boolean,
    isLight: Boolean,
    onContinue: () -> Unit,
    onMix: () -> Unit,
    onFavorites: () -> Unit,
    onNewReleases: () -> Unit,
    onCharts: () -> Unit,
    onSearch: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val items = remember(
        currentTrack,
        mixTrack,
        favoriteTrack,
        releaseTrack,
        chartTrack,
        isPlaying,
        isResolving,
        hasMix,
        hasFavorites,
        hasNewReleases,
        hasCharts,
        strings
    ) {
        listOf(
            HomeQuickAccessItem(
                label = strings.continueListening,
                track = currentTrack,
                icon = Icons.Rounded.History,
                accent = LevyraCyan,
                enabled = currentTrack != null,
                active = currentTrack != null && isPlaying,
                resolving = currentTrack != null && isResolving,
                onClick = onContinue
            ),
            HomeQuickAccessItem(
                label = strings.mixForYou,
                track = mixTrack,
                icon = Icons.Rounded.GraphicEq,
                accent = LevyraViolet,
                enabled = hasMix,
                onClick = onMix
            ),
            HomeQuickAccessItem(
                label = strings.favoritesPlain,
                track = favoriteTrack,
                icon = Icons.Rounded.Favorite,
                accent = LevyraPink,
                enabled = hasFavorites,
                onClick = onFavorites
            ),
            HomeQuickAccessItem(
                label = strings.newReleases,
                track = releaseTrack,
                icon = Icons.Rounded.Bolt,
                accent = Color(0xFFB08CFF),
                enabled = hasNewReleases,
                onClick = onNewReleases
            ),
            HomeQuickAccessItem(
                label = strings.top50Charts,
                track = chartTrack,
                icon = Icons.Rounded.LocalFireDepartment,
                accent = Color(0xFFFFA760),
                enabled = hasCharts,
                onClick = onCharts
            ),
            HomeQuickAccessItem(
                label = strings.search,
                track = null,
                icon = Icons.Rounded.Search,
                accent = Color(0xFF76B8FF),
                enabled = true,
                onClick = onSearch
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(LevyraHomeDesign.TileGap)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LevyraHomeDesign.TileGap)
            ) {
                rowItems.forEach { item ->
                    HomeQuickAccessTile(
                        item = item,
                        isLight = isLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private data class HomeQuickAccessItem(
    val label: String,
    val track: Track?,
    val icon: ImageVector,
    val accent: Color,
    val enabled: Boolean,
    val active: Boolean = false,
    val resolving: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun HomeQuickAccessTile(
    item: HomeQuickAccessItem,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    val artworkUrl = item.track?.thumbnailUrl
        ?.ifBlank { item.track.largeThumbnailUrl }
        .orEmpty()
    val surface = if (isLight) LevyraHomeDesign.TileSurfaceLight else LevyraHomeDesign.TileSurfaceDark
    val border = when {
        item.active -> item.accent.copy(alpha = 0.52f)
        isLight -> LevyraHomeDesign.TileBorderLight
        else -> LevyraHomeDesign.TileBorderDark
    }
    val primaryText = if (isLight) LevyraText else LevyraHomeDesign.TextPrimaryDark
    val secondaryText = if (isLight) LevyraMuted else LevyraHomeDesign.TextSecondaryDark

    Surface(
        color = surface,
        contentColor = primaryText,
        shape = LevyraHomeDesign.TileShape,
        modifier = modifier
            .height(LevyraHomeDesign.TileHeight)
            .alpha(if (item.enabled) 1f else 0.46f)
            .clickable(enabled = item.enabled, onClick = item.onClick),
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        shadowElevation = if (isLight) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(LevyraHomeDesign.ArtworkSize)
                    .fillMaxHeight()
                    .clip(LevyraHomeDesign.ArtworkShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                item.accent.copy(alpha = 0.74f),
                                item.accent.copy(alpha = 0.22f),
                                Color(0xFF111218)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), LevyraHomeDesign.ArtworkShape),
                contentAlignment = Alignment.Center
            ) {
                if (artworkUrl.isNotBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = if (item.active || item.resolving) 0.34f else 0.06f))
                    )
                }
                when {
                    item.resolving -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.2.dp,
                        color = Color.White
                    )
                    item.active -> Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                    artworkUrl.isBlank() -> Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.label,
                    color = primaryText,
                    fontSize = 13.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                item.track?.artist?.takeIf { it.isNotBlank() }?.let { artist ->
                    Text(
                        text = artist,
                        color = secondaryText,
                        fontSize = 10.5.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (item.active && !item.resolving) {
                Icon(
                    imageVector = if (isPlayingIcon(item)) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = item.accent,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(17.dp)
                )
            }
        }
    }
}

private fun isPlayingIcon(item: HomeQuickAccessItem): Boolean = item.active
