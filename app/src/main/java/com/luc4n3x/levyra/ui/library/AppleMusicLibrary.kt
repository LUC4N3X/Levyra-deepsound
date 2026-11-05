package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.background
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign

private const val AppleLibraryRecentColumns = 2
private const val AppleLibraryRecentLimit = 6

@Composable
internal fun AppleLibrarySectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title, color = AppleMusicDesign.label,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
internal fun AppleLibraryRecentlyAdded(tracks: List<Track>, onPlay: (Track) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        tracks.take(AppleLibraryRecentLimit).chunked(AppleLibraryRecentColumns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { track ->
                    Column(
                        Modifier.weight(1f).levyraPressable(onClick = { onPlay(track) }, onClickLabel = track.title, role = Role.Button, pressedScale = LevyraPressScale.Tile),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        CoverImage(track, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(AppleMusicDesign.cornerM)).background(AppleMusicDesign.fill))
                        Spacer(Modifier.padding(top = 4.dp))
                        Text(track.title, color = AppleMusicDesign.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = AppleMusicDesign.secondaryLabel, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat(AppleLibraryRecentColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
internal fun AppleLibraryLinkRow(label: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .levyraPressable(onClick = onClick, onClickLabel = label, role = Role.Button, pressedScale = LevyraPressScale.Control)
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                label, color = AppleMusicDesign.accent, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowForwardIos, null,
                tint = AppleMusicDesign.tertiaryLabel, modifier = Modifier.padding(start = 4.dp).size(16.dp)
            )
        }
        androidx.compose.material3.HorizontalDivider(
            Modifier.align(androidx.compose.ui.Alignment.BottomEnd), thickness = 0.5.dp, color = AppleMusicDesign.separator
        )
    }
}
