package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.SpotifyDesign

internal fun spotifyRadioPastel(accent: Color): Color = lerp(accent.copy(alpha = 1f), Color.White, 0.42f)

@Composable
internal fun SpotifyRadioCircles(tracks: List<Track>, backdrop: Color, size: Dp, modifier: Modifier = Modifier) {
    val ring = size * 0.035f
    Box(modifier.fillMaxWidth().height(size), contentAlignment = Alignment.Center) {
        tracks.getOrNull(1)?.let { side ->
            SpotifyRadioCircle(side, backdrop, size * 0.7f, ring, Modifier.align(Alignment.CenterStart).offset(x = -(size * 0.18f)))
        }
        tracks.getOrNull(2)?.let { side ->
            SpotifyRadioCircle(side, backdrop, size * 0.7f, ring, Modifier.align(Alignment.CenterEnd).offset(x = size * 0.18f))
        }
        tracks.firstOrNull()?.let { lead -> SpotifyRadioCircle(lead, backdrop, size, ring, Modifier) }
    }
}

@Composable
private fun SpotifyRadioCircle(track: Track, backdrop: Color, size: Dp, ring: Dp, modifier: Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(backdrop).padding(ring).clip(CircleShape).background(SpotifyDesign.placeholder)) {
        CoverImage(track = track, modifier = Modifier.fillMaxWidth().aspectRatio(1f), highRes = size > 160.dp)
    }
}

@Composable
internal fun SpotifyRadioCard(title: String, subtitle: String, accent: Color, tracks: List<Track>, onClick: () -> Unit) {
    val pastel = spotifyRadioPastel(accent)
    Column(Modifier.width(SpotifyDesign.radioCard).levyraPressable(onClick = onClick, onClickLabel = title, role = Role.Button)) {
        BoxWithConstraints(Modifier.size(SpotifyDesign.radioCard).clip(SpotifyDesign.coverShape).background(pastel)) {
            SpotifyRadioCircles(tracks, pastel, maxWidth * 0.5f, Modifier.align(Alignment.Center).offset(y = -(maxWidth * 0.06f)))
            Text(
                title, color = Color.Black, fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = SpotifyDesign.subdued, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun SpotifyCollectionHero(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    accent: Color,
    playLabel: String,
    shuffleLabel: String,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    val pastel = spotifyRadioPastel(accent)
    Column(Modifier.fillMaxWidth()) {
        SpotifyRadioCircles(tracks, pastel, 220.dp, Modifier.padding(top = 8.dp))
        Text(
            title, color = Color.White, fontSize = 44.sp, lineHeight = 46.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.4).sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = SpotifyDesign.gutter, end = SpotifyDesign.gutter, top = 4.dp).semantics { heading() }
        )
        Text(
            subtitle, color = SpotifyDesign.subdued, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = SpotifyDesign.gutter, vertical = 12.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(start = SpotifyDesign.gutter, end = SpotifyDesign.gutter, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
        ) {
            Box(Modifier.size(48.dp).levyraPressable(onClick = onShuffle, onClickLabel = shuffleLabel, role = Role.Button), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Shuffle, shuffleLabel, tint = SpotifyDesign.green, modifier = Modifier.size(30.dp))
            }
            Box(
                Modifier.size(SpotifyDesign.playButton).clip(CircleShape).background(SpotifyDesign.green)
                    .levyraPressable(onClick = onPlayAll, onClickLabel = playLabel, role = Role.Button),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, playLabel, tint = Color.Black, modifier = Modifier.size(32.dp))
            }
        }
    }
}
