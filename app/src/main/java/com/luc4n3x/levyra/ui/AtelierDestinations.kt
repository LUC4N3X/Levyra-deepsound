package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.AtelierDesign

private val AtelierDestinationSleeveSide = 148.dp
private val AtelierDestinationPortrait = 112.dp

@Composable
private fun AtelierDestinationTopBar(title: String?, backLabel: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = AtelierDesign.gutter, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AtelierIconAction(Icons.AutoMirrored.Rounded.ArrowBack, backLabel, onBack, AtelierDesign.text)
        if (title != null) {
            Text(
                title, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = AtelierDesign.text,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp).semantics { heading() }
            )
        }
    }
}

@Composable
internal fun AtelierCollectionDestination(
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
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    LazyColumn(
        Modifier.fillMaxSize().background(AtelierDesign.room),
        contentPadding = PaddingValues(bottom = bottomInset + AtelierDesign.sectionGap)
    ) {
        item(key = "atelier-destination-bar", contentType = "atelier-destination-bar") {
            AtelierDestinationTopBar(null, strings.back, onBack)
        }
        item(key = "atelier-destination-poster", contentType = "atelier-destination-poster") {
            val lead = tracks.firstOrNull()
            if (lead != null) LaunchedEffect(lead.id) { onRequestTrackArtwork(lead) }
            AtelierDestinationPoster(
                title = title,
                stamp = subtitle?.takeIf(String::isNotBlank) ?: strings.explore,
                artwork = lead?.let(::atelierCoverUrl).orEmpty(),
                meta = if (tracks.isNotEmpty()) copy.tracksCount.format(tracks.size) else "",
                playLabel = strings.playAll,
                onPlayAll = onPlayAll.takeIf { tracks.isNotEmpty() }
            )
        }
        when {
            isLoading && tracks.isEmpty() -> item(key = "atelier-destination-loading", contentType = "atelier-destination-status") {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    LevyraLoadingIndicator(color = AtelierDesign.accentBright, modifier = Modifier.size(36.dp))
                }
            }
            tracks.isEmpty() -> item(key = "atelier-destination-empty", contentType = "atelier-destination-status") {
                Text(
                    strings.exploreEmpty, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 16.sp,
                    color = AtelierDesign.textSecondary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(28.dp)
                )
            }
            else -> {
                atelierDestinationSleeves("featured", strings.popularTracks, editorial.featured, currentTrackId, onPlayTrack, onRequestTrackArtwork)
                if (editorial.artists.isNotEmpty()) {
                    item(key = "atelier-destination-artists", contentType = "atelier-destination-artists") {
                        Column {
                            AtelierSectionLabel(strings.artists)
                            LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                                items(editorial.artists, key = { it.key }, contentType = { "atelier-destination-artist" }) { artist ->
                                    LaunchedEffect(artist.track.id) { onRequestTrackArtwork(artist.track) }
                                    AtelierDestinationArtist(artist.name, artist.artworkUrl.ifBlank { atelierCoverUrl(artist.track) }) { onPlayTrack(artist.track) }
                                }
                            }
                        }
                    }
                }
                if (editorial.albums.isNotEmpty()) {
                    item(key = "atelier-destination-albums", contentType = "atelier-destination-albums") {
                        Column {
                            AtelierSectionLabel(strings.albumsPlain)
                            LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                                items(editorial.albums, key = { it.key }, contentType = { "atelier-destination-sleeve" }) { album ->
                                    LaunchedEffect(album.track.id) { onRequestTrackArtwork(album.track) }
                                    AtelierDestinationSleeve(
                                        album.title, album.artist, album.artworkUrl.ifBlank { atelierCoverUrl(album.track) }, false
                                    ) { onPlayTrack(album.track) }
                                }
                            }
                        }
                    }
                }
                if (editorial.essentials.isNotEmpty()) {
                    item(key = "atelier-destination-essentials-label", contentType = "atelier-label") { AtelierSectionLabel(strings.songsPlain) }
                    itemsIndexed(editorial.essentials, key = { _, track -> "essential-${track.id}" }, contentType = { _, _ -> "atelier-track-line" }) { index, track ->
                        LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
                        AtelierTrackLine(
                            track, index + 1, track.id == currentTrackId, Icons.Rounded.PlayArrow, strings.play,
                            onPlay = { onPlayTrack(track) }, onAction = { onPlayTrack(track) }
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.atelierDestinationSleeves(
    key: String,
    title: String,
    tracks: List<Track>,
    currentTrackId: String?,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    if (tracks.isEmpty()) return
    item(key = "atelier-destination-$key", contentType = "atelier-destination-sleeves") {
        Column {
            AtelierSectionLabel(title)
            LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                items(tracks, key = { it.id }, contentType = { "atelier-destination-sleeve" }) { track ->
                    LaunchedEffect(track.id) { onRequestTrackArtwork(track) }
                    AtelierDestinationSleeve(track.title, track.artist, atelierCoverUrl(track), track.id == currentTrackId) { onPlayTrack(track) }
                }
            }
        }
    }
}

@Composable
private fun AtelierDestinationPoster(title: String, stamp: String, artwork: String, meta: String, playLabel: String, onPlayAll: (() -> Unit)?) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(top = 6.dp)) {
        val posterWidth = maxWidth * 0.9f
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier.width(posterWidth).height(posterWidth / 0.82f)
                    .graphicsLayer {
                        rotationZ = -1.2f
                        shadowElevation = 24.dp.toPx()
                        shape = RectangleShape
                        clip = true
                    }
                    .background(AtelierPaper)
                    .padding(9.dp)
                    .border(1.dp, AtelierInk.copy(alpha = 0.5f))
            ) {
                AtelierPrintImage(artwork, AtelierPosterFilter, title.take(1).uppercase(), Modifier.fillMaxSize().atelierAgedPrint())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to AtelierInk.copy(alpha = 0.93f))))
                AtelierPosterStamp(stamp, Modifier.align(Alignment.TopStart).padding(14.dp))
                Text(
                    title, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 44.sp,
                    color = AtelierPaper, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(18.dp).semantics { heading() }
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onPlayAll != null) {
                    AtelierHardwareKey(playLabel, lit = false, onClick = onPlayAll, modifier = Modifier.width(92.dp).height(56.dp)) {
                        AtelierPlayGlyph(false, 28.dp)
                    }
                    Spacer(Modifier.width(14.dp))
                }
                if (meta.isNotBlank()) {
                    Text(meta, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = AtelierDesign.textSecondary, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun AtelierDestinationSleeve(title: String, subtitle: String, artwork: String, current: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(AtelierDestinationSleeveSide)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = "$title, $subtitle" }
    ) {
        Box(
            Modifier.size(AtelierDestinationSleeveSide)
                .graphicsLayer {
                    shadowElevation = 10.dp.toPx()
                    shape = AtelierDesign.sleeveShape
                    clip = true
                }
                .background(AtelierPaper).padding(5.dp)
        ) { AtelierArtwork(artwork, Modifier.clearAndSetSemantics { }) }
        Text(
            title, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 19.sp,
            color = if (current) AtelierDesign.accentBright else AtelierDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp).clearAndSetSemantics { }
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { }
            )
        }
    }
}

@Composable
private fun AtelierDestinationArtist(name: String, artwork: String, onClick: () -> Unit) {
    Column(
        Modifier.width(AtelierDestinationPortrait + 8.dp)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = name },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(AtelierDestinationPortrait).clip(CircleShape).background(AtelierPaper).padding(4.dp).clip(CircleShape)) {
            AtelierArtwork(artwork, Modifier.clearAndSetSemantics { })
        }
        Text(
            name, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AtelierDesign.text,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp).clearAndSetSemantics { }
        )
    }
}

@Composable
internal fun AtelierNewReleasesDestination(
    releases: List<AlbumHit>,
    isLoading: Boolean,
    strings: LevyraStrings,
    bottomInset: Dp,
    onBack: () -> Unit,
    onOpenRelease: (AlbumHit) -> Unit
) {
    val rows = remember(releases) { releases.distinctBy(::atelierAlbumKey).chunked(2) }
    LazyColumn(
        Modifier.fillMaxSize().background(AtelierDesign.room),
        contentPadding = PaddingValues(bottom = bottomInset + AtelierDesign.sectionGap)
    ) {
        item(key = "atelier-releases-bar", contentType = "atelier-destination-bar") {
            Column {
                AtelierDestinationTopBar(strings.exploreNewReleases, strings.back, onBack)
                Row(Modifier.padding(horizontal = AtelierDesign.gutter, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(36.dp).height(1.dp).background(AtelierDesign.accent))
                    Text(" ✦ ", fontFamily = AtelierSerif, fontSize = 12.sp, color = AtelierDesign.accent, modifier = Modifier.clearAndSetSemantics { })
                    Box(Modifier.width(36.dp).height(1.dp).background(AtelierDesign.accent))
                }
            }
        }
        when {
            isLoading && releases.isEmpty() -> item(key = "atelier-releases-loading", contentType = "atelier-destination-status") {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    LevyraLoadingIndicator(color = AtelierDesign.accentBright, modifier = Modifier.size(36.dp))
                }
            }
            releases.isEmpty() -> item(key = "atelier-releases-empty", contentType = "atelier-destination-status") {
                Text(
                    strings.exploreEmpty, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 16.sp,
                    color = AtelierDesign.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(28.dp)
                )
            }
            else -> items(rows, key = { row -> "atelier-release-${atelierAlbumKey(row.first())}" }, contentType = { "atelier-release-row" }) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)
                ) {
                    row.forEach { release -> AtelierAlbumTile(release, Modifier.weight(1f)) { onOpenRelease(release) } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
