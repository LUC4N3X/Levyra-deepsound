package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.YoutubeMusicHomeItem
import com.luc4n3x.levyra.domain.YoutubeMusicHomeShelf
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.LevyraSectionShowAll
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.HOME_SUBTITLE_CURATED_PLAYLISTS
import com.luc4n3x.levyra.ui.i18n.defaultHomeSubtitle
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.viewmodel.CuratedFeaturedPlaylist

private val CuratedArtShape = RoundedCornerShape(28.dp)
private val CuratedCardWidth = 164.dp
private val CuratedStackPeek = 16.dp

internal fun defaultHomeOwnsCuratedPlaylists(): Boolean = defaultHomeUsesNewReleases(true)

@Composable
internal fun rememberDefaultCuratedShelves(): List<YoutubeMusicHomeShelf> {
    if (!defaultHomeOwnsCuratedPlaylists()) return emptyList()
    val code = LocalLevyraStrings.current.code
    val shelves = rememberCuratedPlaylistShelves(code)
    return remember(shelves) { shelves.filter { shelf -> shelf.items.any { it.playlist != null } } }
}

@Composable
internal fun DefaultCuratedPlaylistShelf(index: Int, shelf: YoutubeMusicHomeShelf, onOpen: (PlaylistHit) -> Unit) {
    val code = LocalLevyraStrings.current.code
    val copy = remember(code) { LevyraMonoHomeStrings.forCode(code) }
    val railMotion = defaultRailMotionEnabled()
    val items = remember(shelf.items) { shelf.items.filter { it.playlist != null }.distinctBy { it.key } }
    Column {
        LevyraSectionHeader(
            title = if (index == 0) copy.mixedForYou else shelf.title,
            subtitle = if (index == 0) copy.rotationOverline else shelf.strapline.ifBlank { null } ?: defaultHomeSubtitle(HOME_SUBTITLE_CURATED_PLAYLISTS, ""),
            modifier = Modifier.padding(horizontal = LevyraHomeDesign.HorizontalInset).padding(bottom = 12.dp),
            actions = { if (index == 0) LocalCuratedPlaylistsOpener.current?.let { open -> LevyraSectionShowAll(onClick = open) } }
        )
        val rowState = rememberLazyListState()
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = LevyraHomeDesign.HorizontalInset),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.key }, contentType = { "default-curated-playlist" }) { item ->
                Box(Modifier.defaultRailItemMotion(railMotion, rowState, item.key)) {
                    DefaultCuratedPlaylistCard(item, Modifier.defaultRailParallax(railMotion, rowState, item.key), Modifier.width(CuratedCardWidth)) {
                        item.playlist?.let(onOpen)
                    }
                }
            }
        }
    }
}

@Composable
internal fun DefaultCuratedPlaylistCard(item: YoutubeMusicHomeItem, artworkMotion: Modifier, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val newReleasesLabel = LocalLevyraStrings.current.newReleases
    val subtitle = remember(item.subtitle, item.playlist?.playlistId, newReleasesLabel) {
        if (item.playlist?.playlistId == CuratedFeaturedPlaylist.YOUTUBE_ID) newReleasesLabel else monoWithoutYoutube(item.subtitle)
    }
    var failed by remember(item.thumbnailUrl) { mutableStateOf(false) }
    val fallback = remember(scheme.primary, scheme.tertiary) { ArtworkPalette(scheme.primary.toArgb(), scheme.tertiary.toArgb()) }
    val palette by rememberArtworkPalette(paletteKey = "curated-${item.key}-${item.thumbnailUrl}", artworkUrl = item.thumbnailUrl, fallback = fallback)
    val backSheet = lerp(Color(palette.end), scheme.surfaceContainerHighest, 0.55f)
    val midSheet = lerp(Color(palette.start), scheme.surfaceContainerHighest, 0.25f)
    Column(
        modifier
            .levyraPressable(onClick = onClick, pressedScale = LevyraPressScale.Tile, role = Role.Button)
            .semantics(mergeDescendants = true) { contentDescription = listOf(item.title, subtitle).filter(String::isNotBlank).joinToString(", ") }
    ) {
        Box(Modifier.fillMaxWidth().padding(top = CuratedStackPeek)) {
            CuratedSheet(backSheet, scale = 0.84f, rotation = -5f, lift = CuratedStackPeek)
            CuratedSheet(midSheet, scale = 0.92f, rotation = 4f, lift = CuratedStackPeek * 0.5f)
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(CuratedArtShape).background(scheme.surfaceContainerHighest)) {
                if (item.thumbnailUrl.isNotBlank() && !failed) {
                    AsyncImage(item.thumbnailUrl, null, contentScale = ContentScale.Crop, onError = { failed = true }, modifier = Modifier.fillMaxSize().then(artworkMotion))
                }
            }
        }
        run {
            Text(
                subtitle, color = lerp(Color(palette.start), scheme.onSurface, 0.35f), style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp).clearAndSetSemantics { }
            )
        }
        Text(
            item.title, color = scheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp).clearAndSetSemantics { }
        )
    }
}

@Composable
private fun BoxScope.CuratedSheet(color: Color, scale: Float, rotation: Float, lift: Dp) {
    Box(
        Modifier.matchParentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
                translationY = -lift.toPx() - size.height * (1f - scale)
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .clip(CuratedArtShape)
            .background(color)
    )
}
