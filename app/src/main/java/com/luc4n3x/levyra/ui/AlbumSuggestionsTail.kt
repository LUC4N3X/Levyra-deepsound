package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.AlbumDetail
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.ui.components.LevyraMediaTitle
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.media.ImmersiveMediaColors
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.AlbumSuggestionShelf
import com.luc4n3x.levyra.domain.HomeDensity
import com.luc4n3x.levyra.domain.HomeSectionLayout
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsMono
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsQobuz
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic
import com.luc4n3x.levyra.ui.theme.LevyraSkinManagesLayout

internal val albumSuggestionsSupported: Boolean
    get() = !LevyraIsYouTubeMusic && !LevyraIsSpotify && !LevyraIsAppleMusic && !LevyraIsQobuz

internal fun albumSuggestionShelvesFor(detail: AlbumDetail?, tracks: List<Track>): List<AlbumSuggestionShelf> =
    if (tracks.isNotEmpty() && albumSuggestionsSupported) detail?.suggestionShelves.orEmpty() else emptyList()

internal fun LazyListScope.albumSuggestionsItem(
    shelves: List<AlbumSuggestionShelf>,
    gutter: Dp,
    colors: ImmersiveMediaColors,
    onOpen: (AlbumHit) -> Unit
) {
    if (shelves.isEmpty()) return
    item(key = "album-suggestions", contentType = "album-suggestions") {
        AlbumSuggestionsTail(shelves, gutter, colors, onOpen)
    }
}

@Composable
internal fun AlbumSuggestionsTail(
    shelves: List<AlbumSuggestionShelf>,
    gutter: Dp,
    colors: ImmersiveMediaColors,
    onOpen: (AlbumHit) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().padding(top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(if (LevyraIsAtelier) 0.dp else 24.dp)
    ) {
        shelves.forEach { shelf ->
            when {
                LevyraIsAtelier -> AtelierSuggestionShelf(shelf, onOpen)
                LevyraIsMono -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MonoSectionHeader(shelf.title)
                    MonoAlbumRail(shelf.albums, MonoAlbumTile, onOpen)
                }
                LevyraSkinManagesLayout -> StudioSuggestionShelf(shelf, gutter, onOpen)
                else -> CleanSuggestionShelf(shelf, gutter, colors, onOpen)
            }
        }
    }
}

@Composable
private fun AtelierSuggestionShelf(shelf: AlbumSuggestionShelf, onOpen: (AlbumHit) -> Unit) {
    Column {
        AtelierArtistSectionHeader(shelf.title, null, null)
        LazyRow(
            contentPadding = PaddingValues(horizontal = AtelierDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)
        ) {
            items(shelf.albums, key = { "atelier-suggestion-${atelierAlbumKey(it)}" }, contentType = { "AlbumSuggestionsTail-albums" }) { album ->
                AtelierAlbumTile(album, Modifier.width(AtelierDesign.albumCard)) { onOpen(album) }
            }
        }
    }
}

@Composable
private fun CleanSuggestionShelf(shelf: AlbumSuggestionShelf, gutter: Dp, colors: ImmersiveMediaColors, onOpen: (AlbumHit) -> Unit) {
    val singleArtist = remember(shelf) { shelf.albums.map { it.artist.trim().lowercase() }.distinct().size == 1 }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = shelf.title,
            color = colors.content,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 2,
            modifier = Modifier.padding(horizontal = gutter).semantics { heading() }
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shelf.albums, key = { "clean-suggestion-${atelierAlbumKey(it)}" }, contentType = { "clean-suggestion" }) { album ->
                val subtitle = if (singleArtist) album.year.ifBlank { album.artist } else album.artist
                Column(
                    Modifier
                        .width(CleanCardWidth)
                        .clip(RoundedCornerShape(CleanCardCorner))
                        .levyraPressable(onClick = { onOpen(album) }, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = album.title)
                ) {
                    AsyncImage(
                        model = album.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(CleanCardCorner)).background(colors.secondaryFill)
                    )
                    LevyraMediaTitle(
                        text = album.title,
                        explicit = album.explicit,
                        color = colors.content,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, color = colors.contentMuted, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private val CleanCardWidth = 148.dp
private val CleanCardCorner = 10.dp

@Composable
private fun StudioSuggestionShelf(shelf: AlbumSuggestionShelf, gutter: Dp, onOpen: (AlbumHit) -> Unit) {
    val style = HomeStyle.Minimal
    val nocturne = LevyraIsNocturne
    val spec = remember(style, nocturne, gutter) {
        homeStyleSpec(style, HomeDensity.Balanced, nocturne).copy(inset = gutter)
    }
    val items = rememberAlbumShelfItems(shelf.albums, onOpen)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioSectionHeader(style, spec, shelf.title, featured = false, onPlayAll = null)
        StudioShelf(style, spec, HomeSectionLayout.Carousel, items, isPlaying = false, actionsLabel = "")
    }
}
