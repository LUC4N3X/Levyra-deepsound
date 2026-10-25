package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.StableRemoteArtwork
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifySkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.trackCreditsCopy
import com.luc4n3x.levyra.ui.theme.SpotifyDesign

private val SpotifyPanelShape = RoundedCornerShape(16.dp)

@Composable
internal fun SpotifyArtistCards(track: Track, panel: PlayerArtistPanel) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraSpotifySkinStrings.forCode(strings.code) }
    val profile = panel.profile
    val artistName = profile?.name?.ifBlank { null } ?: panel.artistName
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (profile != null) SpotifyAboutArtistCard(profile, artistName, copy.aboutArtist, strings.showMore, panel.onOpenArtist)
        SpotifyExploreCard(track, profile, artistName, panel, copy.explore, copy.songsBy, copy.similarTo)
        SpotifyCreditsCard(artistName, panel.writers, copy.credits, copy.mainArtist, strings.trackCreditsCopy().composition, strings.showAll, panel.onCredits)
    }
}

@Composable
private fun SpotifyPanelTitle(text: String, modifier: Modifier = Modifier, color: Color = SpotifyDesign.text) {
    Text(
        text, color = color, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics { heading() }
    )
}

@Composable
private fun SpotifyAboutArtistCard(profile: ArtistProfile, name: String, title: String, more: String, onOpen: () -> Unit) {
    val image = profile.bannerUrl.ifBlank { profile.thumbnailUrl }
    val bio = profile.biography?.text?.trim().orEmpty()
    Column(
        Modifier.fillMaxWidth().clip(SpotifyPanelShape).background(SpotifyDesign.highlight)
            .clickable(onClickLabel = name, role = Role.Button, onClick = onOpen)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.5f).background(SpotifyDesign.placeholder)) {
            if (image.isNotBlank()) StableRemoteArtwork(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxWidth().height(96.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent))))
            SpotifyPanelTitle(title, Modifier.padding(16.dp))
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(name, color = SpotifyDesign.text, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val listeners = profile.monthlyListeners.ifBlank { profile.subscribers }
            if (listeners.isNotBlank()) Text(listeners, color = SpotifyDesign.subdued, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            if (bio.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                val excerpt = remember(bio) { bio.take(180).trimEnd() }
                Text(
                    buildAnnotatedString {
                        append(excerpt)
                        if (bio.length > excerpt.length) {
                            append("… ")
                            withStyle(SpanStyle(color = SpotifyDesign.text, fontWeight = FontWeight.Bold)) { append(more) }
                        }
                    },
                    color = SpotifyDesign.subdued, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SpotifyExploreCard(
    track: Track, profile: ArtistProfile?, artistName: String, panel: PlayerArtistPanel,
    exploreFormat: String, songsByFormat: String, similarFormat: String
) {
    val related = profile?.relatedArtists?.firstOrNull()
    Column(
        Modifier.fillMaxWidth().clip(SpotifyPanelShape).background(SpotifyDesign.highlight).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SpotifyPanelTitle(exploreFormat.format(artistName))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val tileModifier = Modifier.weight(1f)
            SpotifyExploreTile(
                songsByFormat.format(artistName), tileModifier, onClick = panel.onOpenArtist
            ) { modifier ->
                val url = profile?.thumbnailUrl.orEmpty()
                if (url.isNotBlank()) StableRemoteArtwork(url, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
                else CoverImage(track, modifier, highRes = true)
            }
            if (related != null) SpotifyExploreTile(
                similarFormat.format(artistName), tileModifier, onClick = { panel.onOpenRelated(related) }
            ) { modifier ->
                if (related.thumbnailUrl.isNotBlank()) StableRemoteArtwork(related.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
            }
            SpotifyExploreTile(similarFormat.format(panel.trackTitle), tileModifier, onClick = panel.onSongRadio) { modifier ->
                CoverImage(track, modifier, highRes = true)
            }
        }
    }
}

@Composable
private fun SpotifyExploreTile(label: String, modifier: Modifier, onClick: () -> Unit, artwork: @Composable (Modifier) -> Unit) {
    Box(
        modifier.aspectRatio(9f / 16f).clip(RoundedCornerShape(SpotifyDesign.cornerLarge)).background(SpotifyDesign.placeholder)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
    ) {
        artwork(Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.72f))))
        Text(
            label, color = Color.White, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
        )
    }
}

@Composable
private fun SpotifyCreditsCard(
    artistName: String, writers: List<String>, title: String, mainArtist: String, writerRole: String, showAll: String, onShowAll: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(SpotifyPanelShape).background(SpotifyDesign.highlight).padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            SpotifyPanelTitle(title, Modifier.weight(1f))
            Box(
                Modifier.heightIn(min = 48.dp).clickable(onClickLabel = showAll, role = Role.Button, onClick = onShowAll).padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) { Text(showAll, color = SpotifyDesign.green, style = MaterialTheme.typography.titleSmall, maxLines = 1) }
        }
        SpotifyCreditRow(artistName, mainArtist)
        writers.take(4).forEach { SpotifyCreditRow(it, writerRole) }
    }
}

@Composable
private fun SpotifyCreditRow(name: String, role: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(name, color = SpotifyDesign.text, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(role, color = SpotifyDesign.subdued, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
