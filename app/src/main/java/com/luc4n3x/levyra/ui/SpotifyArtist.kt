package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.ReleaseType
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifySkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import com.luc4n3x.levyra.ui.theme.SpotifyDesign

private const val SPOTIFY_RELEASES_COLLAPSED = 4

@Composable
internal fun SpotifyArtistSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title, color = SpotifyDesign.text, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(horizontal = SpotifyDesign.gutter).semantics { heading() }
    )
}

@Composable
internal fun SpotifyOutlinePill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.heightIn(min = 48.dp).levyraPressable(onClick = onClick, role = Role.Button),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text, color = SpotifyDesign.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                modifier = Modifier.border(1.dp, SpotifyDesign.faint, CircleShape).padding(horizontal = 16.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
internal fun SpotifyArtistReleases(
    albums: List<ArtistRelease>,
    singles: List<ArtistRelease>,
    artistName: String,
    onOpen: (ArtistRelease, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraSpotifySkinStrings.forCode(strings.code) }
    val discography = remember(strings.code) { searchDiscographyCopy(strings.code) }
    val releases = remember(albums, singles) { spotifyPopularReleases(albums, singles) }
    if (releases.isEmpty()) return
    var expanded by rememberSaveable(artistName) { mutableStateOf(false) }
    val latest = releases.first()
    val shown = if (expanded) releases else releases.take(SPOTIFY_RELEASES_COLLAPSED)
    Column(modifier.fillMaxWidth()) {
        SpotifyArtistSectionTitle(copy.popularReleases)
        Spacer(Modifier.height(8.dp))
        shown.forEach { release ->
            SpotifyArtistReleaseRow(
                release = release,
                kicker = copy.latestRelease.takeIf { release === latest && release.year.isNotBlank() },
                typeLabel = spotifyReleaseTypeLabel(release, discography.album, discography.single),
                onClick = { onOpen(release, artistName) }
            )
        }
        if (releases.size > SPOTIFY_RELEASES_COLLAPSED) {
            Spacer(Modifier.height(8.dp))
            SpotifyOutlinePill(if (expanded) strings.showLess else copy.showDiscography, onClick = { expanded = !expanded })
        }
    }
}

@Composable
private fun SpotifyArtistReleaseRow(release: ArtistRelease, kicker: String?, typeLabel: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().levyraPressable(onClick = onClick, onClickLabel = release.title, role = Role.Button)
            .padding(horizontal = SpotifyDesign.gutter, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(84.dp).clip(SpotifyDesign.coverShape).background(SpotifyDesign.placeholder), contentAlignment = Alignment.Center) {
            if (release.thumbnailUrl.isNotBlank()) {
                StableRemoteArtwork(release.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            } else {
                Icon(Icons.Rounded.Album, null, tint = SpotifyDesign.faint, modifier = Modifier.size(32.dp))
            }
        }
        Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (kicker != null) Text(kicker, color = SpotifyDesign.subdued, fontSize = 13.sp, maxLines = 1)
            Text(release.title, color = SpotifyDesign.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(typeLabel, release.year).filter(String::isNotBlank).joinToString(" • "),
                color = SpotifyDesign.subdued, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun SpotifyArtistAbout(imageUrl: String, name: String, audience: String, biography: String, modifier: Modifier = Modifier) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraSpotifySkinStrings.forCode(strings.code) }
    var expanded by rememberSaveable(name) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        SpotifyArtistSectionTitle(copy.about)
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.padding(horizontal = SpotifyDesign.gutter).fillMaxWidth().clip(RoundedCornerShape(SpotifyDesign.cornerLarge))
                .background(SpotifyDesign.highlight)
                .levyraPressable(onClick = { expanded = !expanded }, enabled = biography.isNotBlank(), role = Role.Button)
        ) {
            if (imageUrl.isNotBlank()) {
                Box(Modifier.fillMaxWidth().aspectRatio(1.35f)) {
                    StableRemoteArtwork(imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize(), highRes = true)
                    Box(Modifier.matchParentSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to SpotifyDesign.highlight)))
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, color = SpotifyDesign.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (audience.isNotBlank()) Text(audience, color = SpotifyDesign.subdued, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (biography.isNotBlank()) {
                    Text(
                        biography, color = SpotifyDesign.subdued, fontSize = 14.sp, lineHeight = 20.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun SpotifyFansAlsoLike(artists: List<ArtistHit>, onOpen: (ArtistHit) -> Unit, modifier: Modifier = Modifier) {
    if (artists.isEmpty()) return
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraSpotifySkinStrings.forCode(strings.code) }
    Column(modifier.fillMaxWidth()) {
        SpotifyArtistSectionTitle(copy.fansAlsoLike)
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = SpotifyDesign.gutter), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(artists, key = { index, artist -> "spotify-fan-$index-${artist.browseId.ifBlank { artist.name }}" }) { _, artist ->
                Column(
                    Modifier.width(140.dp).levyraPressable(onClick = { onOpen(artist) }, onClickLabel = artist.name, role = Role.Button),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.size(140.dp).clip(CircleShape).background(SpotifyDesign.placeholder)) {
                        if (artist.thumbnailUrl.isNotBlank()) {
                            StableRemoteArtwork(artist.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                        }
                    }
                    Text(
                        artist.name, color = SpotifyDesign.text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun SpotifyArtistRank(rank: Int, isCurrent: Boolean) {
    Text(
        rank.toString(), color = if (isCurrent) SpotifyDesign.green else SpotifyDesign.text,
        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.width(24.dp)
    )
}

internal fun spotifyPopularReleases(albums: List<ArtistRelease>, singles: List<ArtistRelease>): List<ArtistRelease> {
    val all = (albums + singles).distinctBy { it.browseId.ifBlank { it.title } }
    val newest = all.maxByOrNull { it.year.toIntOrNull() ?: Int.MIN_VALUE } ?: return emptyList()
    return listOf(newest) + all.filterNot { it === newest }
}

private fun spotifyReleaseTypeLabel(release: ArtistRelease, albumLabel: String, singleLabel: String): String = when (release.releaseType) {
    ReleaseType.Album, ReleaseType.Compilation -> albumLabel
    ReleaseType.Single -> singleLabel
    ReleaseType.Ep -> "EP"
    ReleaseType.Unknown -> ""
}
