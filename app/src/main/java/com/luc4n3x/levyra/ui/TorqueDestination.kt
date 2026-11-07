package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraMediaTitle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText

@Composable
internal fun TorqueCollectionDestination(
    title: String,
    subtitle: String?,
    tracks: List<Track>,
    editorial: ExploreGenreEditorial,
    isLoading: Boolean,
    currentTrackId: String?,
    strings: LevyraStrings,
    bottomInset: Dp,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().background(LevyraBlack), contentPadding = PaddingValues(bottom = bottomInset + 32.dp)) {
        item(key = "torque-destination-hero") {
            val lead = tracks.firstOrNull()
            if (lead != null) LaunchedEffect(lead.id) { onRequestTrackArtwork(lead) }
            TorqueDestinationHero(
                title, subtitle?.takeIf(String::isNotBlank) ?: strings.explore, lead?.let(::atelierCoverUrl).orEmpty(),
                tracks.size, strings, onBack, onPlayAll.takeIf { tracks.isNotEmpty() }
            )
        }
        when {
            isLoading && tracks.isEmpty() -> item(key = "torque-destination-loading") {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    LevyraLoadingIndicator(color = LevyraActivePalette.blue, modifier = Modifier.size(36.dp))
                }
            }
            tracks.isEmpty() -> item(key = "torque-destination-empty") {
                Text(strings.exploreEmpty, color = LevyraMuted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(TorqueGutter))
            }
            else -> {
                torqueCoverRail("featured", strings.popularTracks, editorial.featured.map { Triple(it.title, it.artist, it) }, onPlayTrack, onRequestTrackArtwork)
                if (editorial.artists.isNotEmpty()) item(key = "torque-destination-artists") {
                    Column(Modifier.padding(top = 18.dp)) {
                        SkinSectionTitle(strings.artists, TorqueGutter)
                        LazyRow(contentPadding = PaddingValues(horizontal = TorqueGutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(editorial.artists, key = { it.key }) { artist ->
                                LaunchedEffect(artist.track.id) { onRequestTrackArtwork(artist.track) }
                                Box(
                                    Modifier.size(width = 132.dp, height = 168.dp).background(LevyraPanel)
                                        .levyraPressable(onClick = { onPlayTrack(artist.track) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                                        .semantics(mergeDescendants = true) { contentDescription = artist.name }
                                ) {
                                    AsyncImage(
                                        artist.artworkUrl.ifBlank { atelierCoverUrl(artist.track) }, null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clearAndSetSemantics { }
                                    )
                                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
                                    Column(Modifier.align(Alignment.BottomStart).padding(10.dp).clearAndSetSemantics { }) {
                                        Box(Modifier.width(22.dp).height(3.dp).background(LevyraActivePalette.blue))
                                        Text(
                                            artist.name.uppercase(), color = Color.White, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                torqueCoverRail("albums", strings.albumsPlain, editorial.albums.map { Triple(it.title, it.artist, it.track) }, onPlayTrack, onRequestTrackArtwork)
                if (editorial.essentials.isNotEmpty()) {
                    item(key = "torque-destination-essentials") { Box(Modifier.padding(top = 18.dp)) { SkinSectionTitle(strings.songsPlain, TorqueGutter) } }
                    itemsIndexed(editorial.essentials, key = { _, t -> "essential-${t.id}" }) { index, track ->
                        LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
                        TorqueEssentialRow(index, track, track.id == currentTrackId) { onPlayTrack(track) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TorqueDestinationHero(
    title: String,
    eyebrow: String,
    artwork: String,
    count: Int,
    strings: LevyraStrings,
    onBack: () -> Unit,
    onPlay: (() -> Unit)?
) {
    Box(Modifier.fillMaxWidth().aspectRatio(0.9f).background(LevyraPanel)) {
        AsyncImage(artwork, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.5f), 0.3f to Color.Transparent, 0.65f to Color.Black.copy(alpha = 0.55f), 1f to LevyraBlack)
            )
        )
        IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp).size(48.dp).background(Color.Black)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = Color.White)
        }
        if (count > 0) Text(
            count.toString().padStart(2, '0'), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp).background(Color.Black)
                .padding(horizontal = 10.dp, vertical = 6.dp).clearAndSetSemantics { }
        )
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = TorqueGutter, end = TorqueGutter, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    eyebrow.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp, maxLines = 1,
                    modifier = Modifier.clip(TorqueTagShape).background(LevyraActivePalette.blue).padding(horizontal = 14.dp, vertical = 5.dp)
                )
                Text(
                    title.uppercase(), color = Color.White, fontSize = 38.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                )
            }
            if (onPlay != null) Box(
                Modifier.padding(start = 12.dp).size(68.dp).clip(CircleShape).background(Color.Black).border(5.dp, Color.White, CircleShape)
                    .clickable(role = Role.Button, onClickLabel = strings.playAll, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
        }
    }
}

private fun LazyListScope.torqueCoverRail(
    key: String,
    title: String,
    entries: List<Triple<String, String, Track>>,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    if (entries.isEmpty()) return
    item(key = "torque-destination-$key") {
        Column(Modifier.padding(top = 18.dp)) {
            SkinSectionTitle(title, TorqueGutter)
            LazyRow(contentPadding = PaddingValues(horizontal = TorqueGutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(entries, key = { _, entry -> entry.third.id + key }) { index, (name, artist, track) ->
                    LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
                    Column(
                        Modifier.width(156.dp).background(LevyraPanel)
                            .levyraPressable(onClick = { onPlayTrack(track) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                            .semantics(mergeDescendants = true) { contentDescription = "$name, $artist" }
                    ) {
                        Box(Modifier.size(156.dp)) {
                            AsyncImage(atelierCoverUrl(track), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
                            Box(Modifier.align(Alignment.TopStart).padding(8.dp)) { TorqueRank(index + 1, false) }
                        }
                        Column(Modifier.padding(10.dp).clearAndSetSemantics { }) {
                            LevyraMediaTitle(name, explicit = track.explicit, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(artist, color = LevyraMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TorqueEssentialRow(index: Int, track: Track, current: Boolean, onPlay: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = TorqueGutter, vertical = 2.dp).heightIn(min = 72.dp)
            .background(if (current) LevyraPanelSoft else LevyraPanel)
            .levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TorqueRank(index + 1, index < 3)
        Box(Modifier.size(52.dp)) { CoverImage(track, Modifier.fillMaxSize()) }
        Column(Modifier.weight(1f)) {
            LevyraMediaTitle(track.title, explicit = track.explicit,  color = LevyraText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = LevyraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
