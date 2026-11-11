package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.PrismAccent
import com.luc4n3x.levyra.ui.theme.PrismDesign
import com.luc4n3x.levyra.viewmodel.HomeViewModel

private val PrismCuratedWidth = 196.dp
private val PrismCuratedHeight = 252.dp
private val PrismReleaseSide = 156.dp
private val PrismCyanSplit = ColorFilter.tint(LevyraCyan, BlendMode.Modulate)
private val PrismPinkSplit = ColorFilter.tint(LevyraPink, BlendMode.Modulate)

@Composable
internal fun PrismShelfHeader(title: String, inset: Dp) {
    Row(Modifier.fillMaxWidth().padding(horizontal = inset).padding(top = 22.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(22.dp).height(4.dp).clip(CircleShape).background(PrismAccent.gradient()))
        Spacer(Modifier.width(12.dp))
        Text(
            title, color = LevyraText, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.4).sp),
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
internal fun PrismSplitArtwork(url: String, modifier: Modifier) {
    Box(modifier) {
        AsyncImage(
            url, null, contentScale = ContentScale.Crop, colorFilter = PrismCyanSplit,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                translationX = -5.dp.toPx()
                alpha = 0.55f
            }
        )
        AsyncImage(
            url, null, contentScale = ContentScale.Crop, colorFilter = PrismPinkSplit,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                translationX = 5.dp.toPx()
                alpha = 0.55f
            }
        )
        AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp))
    }
}

@Composable
internal fun PrismCuratedAndReleases(viewModel: HomeViewModel, inset: Dp, showReleases: Boolean) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraMonoHomeStrings.forCode(strings.code) }
    val shelves = rememberCuratedPlaylistShelves(strings.code)
    val newThisWeek by viewModel.appleNewThisWeek.collectAsStateWithLifecycle()
    androidx.lifecycle.compose.LifecycleResumeEffect(strings.code) {
        viewModel.ensureAppleNewThisWeek()
        onPauseOrDispose { }
    }
    val releases = remember(newThisWeek.albums) { newThisWeek.albums.distinctBy(::atelierAlbumKey) }
    Column(Modifier.fillMaxWidth()) {
        shelves.forEachIndexed { index, shelf ->
            val tiles = remember(shelf.items) { shelf.items.distinctBy { it.key }.filter { it.playlist != null } }
            if (tiles.isEmpty()) return@forEachIndexed
            PrismShelfHeader(if (index == 0) copy.mixedForYou else shelf.title, inset)
            LazyRow(contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(tiles, key = { it.key }, contentType = { "prism-curated" }) { tile ->
                    val shape = PrismDesign.cut(22.dp)
                    Box(
                        Modifier.size(PrismCuratedWidth, PrismCuratedHeight).clip(shape).background(LevyraBlack)
                            .levyraPressable(onClick = { tile.playlist?.let(viewModel::openPlaylistHit) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                            .semantics(mergeDescendants = true) { contentDescription = tile.title }
                    ) {
                        PrismSplitArtwork(tile.thumbnailUrl, Modifier.fillMaxSize().clearAndSetSemantics { })
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.42f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.92f))))
                        Box(Modifier.align(Alignment.TopEnd).padding(12.dp).size(10.dp).clip(CircleShape).background(PrismAccent.gradient(false)))
                        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            Text(
                                tile.title, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { }
                            )
                            val subtitle = monoWithoutYoutube(tile.subtitle)
                            if (subtitle.isNotBlank()) {
                                Text(
                                    subtitle, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { }
                                )
                            }
                        }
                    }
                }
            }
        }
        com.luc4n3x.levyra.ui.discovery.DailyDiscoveryShelf(inset)
        if (showReleases && releases.isNotEmpty()) {
            val labels = searchDiscographyCopy(strings.code)
            PrismShelfHeader(copy.newAlbums, inset)
            LazyRow(contentPadding = PaddingValues(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(releases, key = { atelierAlbumKey(it) }, contentType = { "prism-release" }) { album ->
                    PrismReleaseCard(album, prismReleaseBadge(album, labels.album, labels.single)) { viewModel.openAlbum(album) }
                }
            }
        }
    }
}

private fun prismReleaseBadge(album: AlbumHit, albumLabel: String, singleLabel: String): String = when (album.releaseType) {
    ReleaseType.Album, ReleaseType.Compilation -> albumLabel
    ReleaseType.Single -> singleLabel
    ReleaseType.Ep -> "EP"
    ReleaseType.Unknown -> ""
}

@Composable
private fun PrismReleaseCard(album: AlbumHit, badge: String, onOpen: () -> Unit) {
    Column(
        Modifier.width(PrismReleaseSide)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = listOf(album.title, badge, album.artist).filter(String::isNotBlank).joinToString(", ") }
    ) {
        Box(Modifier.size(PrismReleaseSide).clip(PrismDesign.cut(18.dp)).background(LevyraBlack)) {
            PrismSplitArtwork(album.thumbnailUrl, Modifier.fillMaxSize().clearAndSetSemantics { })
            if (badge.isNotBlank()) {
                Text(
                    badge.uppercase(), color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(CircleShape).background(PrismAccent.gradient(false))
                        .padding(horizontal = 10.dp, vertical = 4.dp).clearAndSetSemantics { }
                )
            }
        }
        Text(
            album.title, color = LevyraText, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp).clearAndSetSemantics { }
        )
        Text(
            album.artist, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { }
        )
    }
}
