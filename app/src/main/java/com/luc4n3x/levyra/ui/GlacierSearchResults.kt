package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.library.LibraryArtwork
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.MonoCodeFont
import com.luc4n3x.levyra.ui.theme.MonoDesign

private val GlacierTopAvatar = 76.dp
private const val GlacierTopTracks = 3

@Composable
internal fun GlacierTopResultCard(
    tracks: List<Track>,
    artist: ArtistHit?,
    currentId: String?,
    strings: LevyraStrings,
    onPlay: (Track) -> Unit,
    onArtist: () -> Unit,
    onShuffle: () -> Unit,
    onMix: () -> Unit,
    onMore: (Track) -> Unit
) {
    val hero = tracks.firstOrNull() ?: return
    val backdrop = hero.largeThumbnailUrl.ifBlank { hero.thumbnailUrl }
    Box(
        Modifier.padding(horizontal = MonoDesign.gutter, vertical = 6.dp).fillMaxWidth()
            .clip(MonoDesign.panelShape).background(MonoDesign.surface)
            .border(1.dp, MonoDesign.hairline, MonoDesign.panelShape)
    ) {
        if (backdrop.isNotBlank()) {
            AsyncImage(backdrop, null, contentScale = ContentScale.Crop, alpha = 0.42f, modifier = Modifier.matchParentSize())
        }
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(0f to MonoDesign.surface.copy(alpha = 0.35f), 0.45f to MonoDesign.surface.copy(alpha = 0.85f), 1f to MonoDesign.surface)
            )
        )
        Box(Modifier.matchParentSize().background(Brush.radialGradient(listOf(MonoDesign.accent.copy(alpha = 0.14f), Color.Transparent))))
        Column(Modifier.padding(top = 18.dp, bottom = 10.dp)) {
            MonoOverline(strings.topResult, Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp), color = MonoDesign.accent)
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = strings.openArtist, onClick = onArtist).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val name = artist?.name?.ifBlank { null } ?: hero.artist
                LibraryArtwork(
                    artist?.thumbnailUrl?.ifBlank { null } ?: hero.thumbnailUrl, name,
                    Modifier.size(GlacierTopAvatar).border(1.dp, MonoDesign.hairlineStrong, CircleShape), CircleShape, false
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(name, style = MaterialTheme.typography.headlineSmall, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        artist?.subscribers?.trim()?.takeIf(String::isNotBlank) ?: strings.artistLabel,
                        style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MonoDesign.textSecondary, modifier = Modifier.size(24.dp))
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(MonoDesign.accent)
                        .clickable(role = Role.Button, onClickLabel = strings.play) { onPlay(hero) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PlayArrow, strings.play, tint = MonoDesign.onAccent, modifier = Modifier.size(30.dp))
                }
                GlacierTopAction(Icons.Rounded.Shuffle, strings.shuffle, onShuffle)
                GlacierTopAction(Icons.Rounded.Radio, strings.mix, onMix)
            }
            tracks.take(GlacierTopTracks).forEachIndexed { index, track ->
                val current = track.id == currentId
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { selected = current }
                        .clickable(role = Role.Button) { onPlay(track) }
                        .padding(start = 20.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        (index + 1).toString().padStart(2, '0'), style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoCodeFont),
                        color = if (current) MonoDesign.accent else MonoDesign.textFaint, modifier = Modifier.width(28.dp)
                    )
                    Box(Modifier.size(44.dp).clip(MonoDesign.artShape).background(MonoDesign.raised)) { CoverImage(track, Modifier.matchParentSize()) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            track.title, style = if (index == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                            color = if (current) MonoDesign.accent else MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            listOf(track.artist, monoSearchPlays(track, strings)).filter(String::isNotBlank).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    MonoSearchIconTarget(Icons.Rounded.MoreVert, strings.more, MonoDesign.textSecondary) { onMore(track) }
                }
            }
        }
    }
}

@Composable
private fun GlacierTopAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.heightIn(min = 48.dp).clip(MonoDesign.pillShape)
            .border(1.dp, MonoDesign.hairlineStrong, MonoDesign.pillShape)
            .background(MonoDesign.raised.copy(alpha = 0.6f))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = MonoDesign.accent, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MonoDesign.text, maxLines = 1)
    }
}

@Composable
internal fun GlacierGridCard(title: String, subtitle: String, imageUrl: String, modifier: Modifier, menu: @Composable () -> Unit, onClick: () -> Unit) {
    Column(modifier.clip(MonoDesign.cardShape).clickable(role = Role.Button, onClickLabel = title, onClick = onClick).padding(bottom = 4.dp)) {
        LibraryArtwork(
            imageUrl, title, Modifier.fillMaxWidth().aspectRatio(1f).border(1.dp, MonoDesign.hairline, MonoDesign.cardShape), MonoDesign.cardShape, false
        )
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            menu()
        }
    }
}
