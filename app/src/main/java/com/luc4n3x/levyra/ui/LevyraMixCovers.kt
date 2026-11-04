package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.LevyraMixKind
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.materialkolor.hct.Hct

@Immutable
internal data class LevyraMixCoverSources(
    val personalized: List<Track> = emptyList(),
    val rotation: List<Track> = emptyList(),
    val throwback: List<Track> = emptyList()
)

private val CoverSize = 148.dp

private class MixCover(
    val kind: LevyraMixKind,
    val title: String,
    val icon: ImageVector,
    val hueShift: Double,
    val tracks: List<Track>
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun coverShapes(): List<Shape> = listOf(
    MaterialShapes.Cookie9Sided.toShape(),
    MaterialShapes.Sunny.toShape(),
    MaterialShapes.Clover4Leaf.toShape(),
    MaterialShapes.SoftBurst.toShape()
)

@Composable
internal fun LevyraMixCoverRail(
    sources: LevyraMixCoverSources,
    loading: Boolean,
    accent: Color,
    mixForYou: String,
    onRepeat: String,
    throwback: String,
    surprise: String,
    onStartMix: (LevyraMixKind) -> Unit,
    modifier: Modifier = Modifier
) {
    var pending by rememberSaveable { mutableStateOf<LevyraMixKind?>(null) }
    LaunchedEffect(loading) { if (!loading) pending = null }
    val covers = remember(sources, mixForYou, onRepeat, throwback, surprise) {
        listOf(
            MixCover(LevyraMixKind.Personalized, mixForYou, Icons.Rounded.AutoAwesome, 0.0, sources.personalized),
            MixCover(LevyraMixKind.CurrentRotation, onRepeat, Icons.Rounded.Repeat, 55.0, sources.rotation),
            MixCover(LevyraMixKind.Rediscover, throwback, Icons.Rounded.History, -60.0, sources.throwback),
            MixCover(LevyraMixKind.SurpriseMe, surprise, Icons.Rounded.Casino, 150.0, emptyList())
        )
    }
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        covers.forEachIndexed { index, cover ->
            MixCoverCard(
                cover = cover,
                index = index,
                accent = accent,
                loading = loading && pending == cover.kind,
                enabled = !loading,
                onClick = {
                    pending = cover.kind
                    onStartMix(cover.kind)
                }
            )
        }
    }
}

@Composable
private fun MixCoverCard(
    cover: MixCover,
    index: Int,
    accent: Color,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shapes = coverShapes()
    val dark = scheme.surface.luminance() < 0.5f
    val tint = remember(accent, cover.hueShift, dark) { shiftedTone(accent, cover.hueShift, if (dark) 68.0 else 46.0) }
    val deep = remember(accent, cover.hueShift, dark) { shiftedTone(accent, cover.hueShift, if (dark) 22.0 else 88.0) }
    val onTint = if (tint.luminance() > 0.45f) Color.Black else Color.White
    val coverShape = if (index % 2 == 0) {
        RoundedCornerShape(topStart = 32.dp, topEnd = 14.dp, bottomEnd = 32.dp, bottomStart = 14.dp)
    } else {
        RoundedCornerShape(topStart = 14.dp, topEnd = 32.dp, bottomEnd = 14.dp, bottomStart = 32.dp)
    }
    val artworks = remember(cover.tracks) {
        cover.tracks.asSequence()
            .map { it.largeThumbnailUrl.ifBlank { it.thumbnailUrl } }
            .filter { it.isNotBlank() }
            .distinct()
            .take(4)
            .toList()
    }
    val artists = remember(cover.tracks) {
        cover.tracks.asSequence().map { it.artist.trim() }.filter { it.isNotEmpty() }.distinct().take(3).joinToString(", ")
    }
    Column(
        modifier = Modifier
            .width(CoverSize)
            .semantics(mergeDescendants = true) { contentDescription = cover.title }
            .levyraPressable(onClick = onClick, enabled = enabled, role = Role.Button, pressedScale = LevyraPressScale.Tile),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(CoverSize)) {
            Box(Modifier.fillMaxSize().clip(coverShape).background(deep)) {
                if (artworks.size >= 4) {
                    ArtworkMosaic(artworks)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to deep.copy(alpha = 0.92f))))
                } else {
                    GenerativeCover(index, shapes, tint, deep, cover.icon, onTint)
                }
                Text(
                    cover.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (deep.luminance() > 0.45f) Color.Black else Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 52.dp, bottom = 12.dp)
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(48.dp)
                    .clip(shapes[0])
                    .background(tint),
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    DiscoveryLoadingIndicator(modifier = Modifier.size(22.dp), color = onTint)
                } else {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = onTint, modifier = Modifier.size(26.dp))
                }
            }
        }
        if (artists.isNotEmpty()) {
            Text(
                artists,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

@Composable
internal fun ArtworkMosaic(artworks: List<String>) {
    Column(Modifier.fillMaxSize()) {
        for (row in 0 until 2) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (column in 0 until 2) {
                    AsyncImage(
                        model = artworks[row * 2 + column],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
internal fun GenerativeCover(index: Int, shapes: List<Shape>, tint: Color, deep: Color, icon: ImageVector, onTint: Color) {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(tint.copy(alpha = 0.55f), deep)))) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 26.dp, y = (-22).dp)
                .size(118.dp)
                .rotate(index * 24f)
                .clip(shapes[(index + 1) % shapes.size])
                .background(tint.copy(alpha = 0.85f))
        )
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .size(36.dp)
                .clip(shapes[(index + 2) % shapes.size])
                .background(deep.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = 34.dp)
                .size(28.dp)
                .clip(shapes[(index + 3) % shapes.size])
                .background(onTint.copy(alpha = 0.18f))
        )
    }
}

internal fun shiftedTone(accent: Color, hueShift: Double, tone: Double): Color {
    val source = Hct.fromInt(accent.toArgb())
    val hue = ((source.hue + hueShift) % 360.0 + 360.0) % 360.0
    return Color(Hct.from(hue, source.chroma.coerceAtLeast(36.0), tone).toInt())
}
