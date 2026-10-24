@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.luc4n3x.levyra.ui.rewind

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.RewindSummary
import com.luc4n3x.levyra.ui.experiences.ExperienceArtwork
import com.luc4n3x.levyra.ui.experiences.MusicExperienceStyle
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.recentSearchesBleed
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import java.text.NumberFormat

@Composable
internal fun ColumnScope.RewindArtists(summary: RewindSummary, style: MusicExperienceStyle, number: NumberFormat, reveal: Float) {
    val strings = LocalLevyraStrings.current
    val artist = summary.snapshot.topArtists.first()
    val artwork = artist.artworkUrl.ifBlank {
        summary.snapshot.topTracks.firstOrNull { it.artist.trim().equals(artist.name.trim(), ignoreCase = true) && it.artworkUrl.isNotBlank() }?.artworkUrl
            ?: summary.albums.firstOrNull { it.artist.trim().equals(artist.name.trim(), ignoreCase = true) }?.artworkUrl.orEmpty()
    }
    RewindEyebrow(strings.topArtistsTitle, style)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("01", style = MaterialTheme.typography.displayLargeEmphasized, color = style.accent, fontSize = 80.sp, modifier = Modifier.weight(0.35f))
        ExperienceArtwork(artwork, artist.name, style,
            Modifier.weight(0.65f).aspectRatio(0.85f).graphicsLayer { if (style.expressive) rotationZ = (1f - reveal) * 8f },
            shape = if (style.expressive) MaterialShapes.Arch.toShape() else style.shape)
    }
    Text(artist.name, style = MaterialTheme.typography.displayMediumEmphasized, color = style.text)
    RewindStatLine(number.format(artist.listenedMs / 60_000L), strings.receiptMinutesListened, style)
    RewindStatLine(number.format(artist.plays), strings.statPlays, style)
    summary.snapshot.topArtists.drop(1).take(4).forEachIndexed { index, item ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(number.format(index + 2), style = MaterialTheme.typography.titleMedium, color = style.muted)
            Text(item.name, style = MaterialTheme.typography.headlineSmall, color = style.text, modifier = Modifier.weight(1f))
            Text(number.format(item.listenedMs / 60_000L), color = style.accent, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
internal fun ColumnScope.RewindTracks(summary: RewindSummary, style: MusicExperienceStyle, number: NumberFormat, reveal: Float) {
    val strings = LocalLevyraStrings.current
    var selected by rememberSaveable(summary.year) { mutableIntStateOf(0) }
    val tracks = summary.snapshot.topTracks.take(5)
    val selectedTrack = tracks.getOrElse(selected) { tracks.first() }
    val animations = LocalAnimationsEnabled.current
    RewindEyebrow(strings.topTracksTitle, style)
    BoxWithConstraints(Modifier.widthIn(max = 400.dp).fillMaxWidth().align(Alignment.CenterHorizontally).aspectRatio(1.05f)) {
        val lead = maxWidth * 0.61f
        tracks.take(4).reversed().forEachIndexed { reverseIndex, track ->
            val index = minOf(3, tracks.lastIndex) - reverseIndex
            val active = index == selected
            val shape = if (!style.expressive) style.shape else when (index % 3) {
                0 -> MaterialShapes.Cookie9Sided.toShape()
                1 -> MaterialShapes.Gem.toShape()
                else -> MaterialShapes.Clover4Leaf.toShape()
            }
            ExperienceArtwork(track.artworkUrl, track.title, style,
                Modifier.size(if (index == 0) lead else maxWidth * 0.42f)
                    .align(when (index) { 0 -> Alignment.CenterStart; 1 -> Alignment.TopEnd; 2 -> Alignment.BottomEnd; else -> Alignment.BottomCenter })
                    .graphicsLayer {
                        rotationZ = if (style.expressive) (index - 1) * 8f * reveal else 0f
                        scaleX = if (active) 1f else 0.94f
                        scaleY = scaleX
                    }
                    .clickable(role = Role.Button, onClickLabel = track.title) { selected = index }, shape)
        }
    }
    Text(selectedTrack.title, style = MaterialTheme.typography.headlineLargeEmphasized, color = style.text)
    Text(selectedTrack.artist, style = MaterialTheme.typography.titleLarge, color = style.muted)
    Text("${number.format(selectedTrack.plays)} ${strings.statPlays} · ${number.format(selectedTrack.listenedMs / 60_000L)} ${strings.recapUnitMinutes}", color = style.accent, style = MaterialTheme.typography.titleMedium)
    val motion = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.animateContentSize(if (animations) motion else snap())) {
        tracks.forEachIndexed { index, track ->
            Surface(onClick = { selected = index }, color = if (selected == index) style.accent else style.surface,
                contentColor = if (selected == index) style.onAccent else style.text, shape = MaterialTheme.shapes.large) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(number.format(index + 1), fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                    Text(track.title, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text(number.format(track.plays), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
internal fun ColumnScope.RewindAlbums(summary: RewindSummary, style: MusicExperienceStyle, number: NumberFormat) {
    val strings = LocalLevyraStrings.current
    val carousel = rememberCarouselState { summary.albums.size }
    val selected = summary.albums[carousel.currentItem.coerceIn(summary.albums.indices)]
    RewindEyebrow(strings.topAlbumsTitle, style)
    Text(selected.title, style = MaterialTheme.typography.displaySmallEmphasized, color = style.text)
    HorizontalMultiBrowseCarousel(
        state = carousel, preferredItemWidth = 248.dp,
        modifier = Modifier.recentSearchesBleed(24.dp).fillMaxWidth(), itemSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) { index ->
        val album = summary.albums[index]
            Box {
                ExperienceArtwork(album.artworkUrl, album.title, style, Modifier.height(280.dp).fillMaxWidth().maskClip(MaterialTheme.shapes.extraLarge), shape = MaterialTheme.shapes.extraLarge)
                Surface(color = style.background, contentColor = style.text, shape = CircleShape, modifier = Modifier.padding(12.dp).size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(number.format(index + 1), style = MaterialTheme.typography.titleLarge) }
                }
            }
    }
    Text(selected.artist, color = style.muted, style = MaterialTheme.typography.headlineSmall)
    RewindStatLine(number.format(selected.listenedMs / 60_000L), strings.receiptMinutesListened, style)
    RewindStatLine(number.format(selected.plays), strings.statPlays, style)
}
