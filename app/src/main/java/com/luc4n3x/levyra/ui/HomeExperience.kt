package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
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
            .homeAtmosphereBackground(primary, secondary, isLight)
    )
}

private fun Modifier.homeAtmosphereBackground(
    primary: Color,
    secondary: Color,
    isLight: Boolean
): Modifier = drawWithCache {
    val width = size.width
    val height = size.height
    val safeRadius = width.coerceAtLeast(1f)
    val leftCenter = Offset(width * 0.12f, height * 0.06f)
    val rightCenter = Offset(width * 0.96f, height * 0.22f)
    val leftRadius = safeRadius * 0.94f
    val rightRadius = safeRadius * 0.78f
    val fadeTop = height * 0.46f
    val base = homeBaseBrush(isLight)
    val leftHalo = homeHaloBrush(primary, isLight, leftCenter, leftRadius, prominent = true)
    val rightHalo = homeHaloBrush(secondary, isLight, rightCenter, rightRadius, prominent = false)
    val wave = homeWavePath(width, height)
    val echo = homeEchoPath(width, height)
    val waveBrush = homeWaveBrush(primary, secondary, isLight)
    val bottomFade = homeBottomFadeBrush(isLight, fadeTop, height)

    onDrawBehind {
        drawRect(base)
        drawCircle(leftHalo, radius = leftRadius, center = leftCenter)
        drawCircle(rightHalo, radius = rightRadius, center = rightCenter)
        drawPath(wave, brush = waveBrush, style = Stroke(width = 1.25.dp.toPx()))
        drawPath(
            echo,
            color = homeEchoColor(primary, isLight),
            style = Stroke(width = 0.75.dp.toPx())
        )
        drawRect(
            brush = bottomFade,
            topLeft = Offset(0f, fadeTop),
            size = Size(width, height - fadeTop)
        )
    }
}

private fun homeBaseBrush(isLight: Boolean): Brush = if (isLight) {
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

private fun homeHaloBrush(
    color: Color,
    isLight: Boolean,
    center: Offset,
    radius: Float,
    prominent: Boolean
): Brush {
    val leadingAlpha = homeHaloAlpha(isLight, prominent, leading = true)
    val trailingAlpha = homeHaloAlpha(isLight, prominent, leading = false)
    return Brush.radialGradient(
        colors = listOf(
            color.copy(alpha = leadingAlpha),
            color.copy(alpha = trailingAlpha),
            Color.Transparent
        ),
        center = center,
        radius = radius
    )
}

private fun homeHaloAlpha(isLight: Boolean, prominent: Boolean, leading: Boolean): Float = when {
    isLight && prominent && leading -> 0.16f
    isLight && prominent -> 0.045f
    isLight && leading -> 0.10f
    isLight -> 0.028f
    prominent && leading -> 0.28f
    prominent -> 0.075f
    leading -> 0.20f
    else -> 0.055f
}

private fun homeWaveBrush(primary: Color, secondary: Color, isLight: Boolean): Brush =
    Brush.horizontalGradient(
        listOf(
            Color.Transparent,
            primary.copy(alpha = if (isLight) 0.08f else 0.16f),
            secondary.copy(alpha = if (isLight) 0.06f else 0.12f),
            Color.Transparent
        )
    )

private fun homeBottomFadeBrush(isLight: Boolean, fadeTop: Float, height: Float): Brush =
    Brush.verticalGradient(
        colors = if (isLight) {
            listOf(
                Color.Transparent,
                Color(0xFFF1F3F8).copy(alpha = 0.86f),
                Color(0xFFF1F3F8)
            )
        } else {
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f), Color.Black)
        },
        startY = fadeTop,
        endY = height
    )

private fun homeEchoColor(primary: Color, isLight: Boolean): Color =
    if (isLight) primary.copy(alpha = 0.035f) else Color.White.copy(alpha = 0.035f)

private fun homeWavePath(width: Float, height: Float): Path = Path().apply {
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

private fun homeEchoPath(width: Float, height: Float): Path = Path().apply {
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

internal data class LevyraHomeQuickAccessTracks(
    val current: Track?,
    val mix: Track?,
    val favorite: Track?,
    val release: Track?,
    val chart: Track?
)

internal data class LevyraHomeQuickAccessAvailability(
    val hasMix: Boolean,
    val hasFavorites: Boolean,
    val hasNewReleases: Boolean,
    val hasCharts: Boolean
)

internal data class LevyraHomeQuickAccessPlayback(
    val isPlaying: Boolean,
    val isResolving: Boolean
)

internal data class LevyraHomeQuickAccessState(
    val tracks: LevyraHomeQuickAccessTracks,
    val availability: LevyraHomeQuickAccessAvailability,
    val playback: LevyraHomeQuickAccessPlayback,
    val isLight: Boolean
)

internal data class LevyraHomeQuickAccessActions(
    val onContinue: () -> Unit,
    val onMix: () -> Unit,
    val onFavorites: () -> Unit,
    val onNewReleases: () -> Unit,
    val onCharts: () -> Unit,
    val onSearch: () -> Unit
)

@Composable
internal fun LevyraHomeQuickAccessGrid(
    state: LevyraHomeQuickAccessState,
    actions: LevyraHomeQuickAccessActions
) {
    val items = homeQuickAccessItems(LocalLevyraStrings.current, state, actions)

    Column(verticalArrangement = Arrangement.spacedBy(LevyraHomeDesign.TileGap)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LevyraHomeDesign.TileGap)
            ) {
                rowItems.forEach { item ->
                    HomeQuickAccessTile(
                        item = item,
                        isLight = state.isLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun homeQuickAccessItems(
    strings: LevyraStrings,
    state: LevyraHomeQuickAccessState,
    actions: LevyraHomeQuickAccessActions
): List<HomeQuickAccessItem> {
    val tracks = state.tracks
    val availability = state.availability
    val playback = state.playback
    return listOf(
        HomeQuickAccessItem(
            label = strings.continueListening,
            track = tracks.current,
            icon = Icons.Rounded.History,
            accent = LevyraCyan,
            enabled = tracks.current != null,
            active = tracks.current != null,
            playing = tracks.current != null && playback.isPlaying,
            resolving = tracks.current != null && playback.isResolving,
            onClick = actions.onContinue
        ),
        HomeQuickAccessItem(
            label = strings.mixForYou,
            track = tracks.mix,
            icon = Icons.Rounded.GraphicEq,
            accent = LevyraViolet,
            enabled = availability.hasMix,
            onClick = actions.onMix
        ),
        HomeQuickAccessItem(
            label = strings.favoritesPlain,
            track = tracks.favorite,
            icon = Icons.Rounded.Favorite,
            accent = LevyraPink,
            enabled = availability.hasFavorites,
            onClick = actions.onFavorites
        ),
        HomeQuickAccessItem(
            label = strings.newReleases,
            track = tracks.release,
            icon = Icons.Rounded.Bolt,
            accent = Color(0xFFB08CFF),
            enabled = availability.hasNewReleases,
            onClick = actions.onNewReleases
        ),
        HomeQuickAccessItem(
            label = strings.top50Charts,
            track = tracks.chart,
            icon = Icons.Rounded.LocalFireDepartment,
            accent = Color(0xFFFFA760),
            enabled = availability.hasCharts,
            onClick = actions.onCharts
        ),
        HomeQuickAccessItem(
            label = strings.search,
            track = null,
            icon = Icons.Rounded.Search,
            accent = Color(0xFF76B8FF),
            enabled = true,
            onClick = actions.onSearch
        )
    )
}

private data class HomeQuickAccessItem(
    val label: String,
    val track: Track?,
    val icon: ImageVector,
    val accent: Color,
    val enabled: Boolean,
    val active: Boolean = false,
    val playing: Boolean = false,
    val resolving: Boolean = false,
    val onClick: () -> Unit
)

private data class HomeQuickAccessVisuals(
    val surface: Color,
    val border: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val shadowElevation: androidx.compose.ui.unit.Dp
)

@Composable
private fun HomeQuickAccessTile(
    item: HomeQuickAccessItem,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    val visuals = homeQuickAccessVisuals(item, isLight)
    val artworkUrl = item.track
        ?.let { track -> track.thumbnailUrl.ifBlank { track.largeThumbnailUrl } }
        .orEmpty()

    Surface(
        color = visuals.surface,
        contentColor = visuals.primaryText,
        shape = LevyraHomeDesign.TileShape,
        modifier = modifier
            .height(LevyraHomeDesign.TileHeight)
            .alpha(if (item.enabled) 1f else 0.46f)
            .clickable(enabled = item.enabled, onClick = item.onClick),
        border = BorderStroke(1.dp, visuals.border),
        shadowElevation = visuals.shadowElevation
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeQuickAccessArtwork(item, artworkUrl)
            HomeQuickAccessCopy(
                item = item,
                primaryText = visuals.primaryText,
                secondaryText = visuals.secondaryText,
                modifier = Modifier.weight(1f)
            )
            HomeQuickAccessTrailingStatus(item)
        }
    }
}

private fun homeQuickAccessVisuals(
    item: HomeQuickAccessItem,
    isLight: Boolean
): HomeQuickAccessVisuals {
    val border = when {
        item.active -> item.accent.copy(alpha = 0.52f)
        isLight -> LevyraHomeDesign.TileBorderLight
        else -> LevyraHomeDesign.TileBorderDark
    }
    return HomeQuickAccessVisuals(
        surface = if (isLight) LevyraHomeDesign.TileSurfaceLight else LevyraHomeDesign.TileSurfaceDark,
        border = border,
        primaryText = if (isLight) LevyraText else LevyraHomeDesign.TextPrimaryDark,
        secondaryText = if (isLight) LevyraMuted else LevyraHomeDesign.TextSecondaryDark,
        shadowElevation = if (isLight) 2.dp else 0.dp
    )
}

@Composable
private fun HomeQuickAccessArtwork(item: HomeQuickAccessItem, artworkUrl: String) {
    Box(
        modifier = Modifier
            .size(LevyraHomeDesign.ArtworkSize)
            .clip(LevyraHomeDesign.ArtworkShape)
            .background(homeQuickAccessArtworkBrush(item.accent))
            .border(1.dp, Color.White.copy(alpha = 0.08f), LevyraHomeDesign.ArtworkShape),
        contentAlignment = Alignment.Center
    ) {
        if (artworkUrl.isNotBlank()) {
            HomeQuickAccessArtworkImage(item, artworkUrl)
        }
        HomeQuickAccessArtworkState(item, artworkUrl.isBlank())
    }
}

private fun homeQuickAccessArtworkBrush(accent: Color): Brush = Brush.linearGradient(
    listOf(
        accent.copy(alpha = 0.74f),
        accent.copy(alpha = 0.22f),
        Color(0xFF111218)
    )
)

@Composable
private fun HomeQuickAccessArtworkImage(item: HomeQuickAccessItem, artworkUrl: String) {
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

@Composable
private fun HomeQuickAccessArtworkState(item: HomeQuickAccessItem, artworkMissing: Boolean) {
    when {
        item.resolving -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.2.dp,
            color = Color.White
        )
        item.playing -> Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(23.dp)
        )
        artworkMissing -> Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(23.dp)
        )
    }
}

@Composable
private fun HomeQuickAccessCopy(
    item: HomeQuickAccessItem,
    primaryText: Color,
    secondaryText: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 11.dp),
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
}

@Composable
private fun HomeQuickAccessTrailingStatus(item: HomeQuickAccessItem) {
    if (item.active && !item.resolving) {
        Icon(
            imageVector = if (item.playing) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = item.accent,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(17.dp)
        )
    }
}
