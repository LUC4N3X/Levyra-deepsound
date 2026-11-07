package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText

@Composable
internal fun TorqueTopResultCard(
    hero: Track,
    artist: ArtistHit?,
    enabled: Boolean,
    onArtist: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onMix: () -> Unit,
    rows: @Composable () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val name = artist?.name?.ifBlank { null } ?: hero.artist
    val subtitle = artist?.subscribers?.trim()?.takeIf(String::isNotBlank) ?: strings.artistLabel
    val image = artist?.thumbnailUrl?.ifBlank { null } ?: hero.largeThumbnailUrl.ifBlank { hero.thumbnailUrl }
    Column(Modifier.fillMaxWidth().background(LevyraPanel)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.35f).background(LevyraPanelSoft)
                .clickable(enabled = enabled, onClickLabel = strings.openArtist, role = Role.Button, onClick = onArtist)
        ) {
            AsyncImage(image, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.3f to Color.Transparent, 0.8f to Color.Black.copy(alpha = 0.75f), 1f to Color.Black.copy(alpha = 0.92f))))
            Text(
                strings.topResult.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.6.sp, maxLines = 1,
                modifier = Modifier.align(Alignment.TopStart).background(Color.Black).padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 18.dp, end = 104.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(36.dp).height(4.dp).background(LevyraActivePalette.blue))
                Text(
                    name.uppercase(), color = Color.White, fontSize = 30.sp, lineHeight = 33.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                )
                Text(subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 18.dp).size(68.dp).clip(CircleShape)
                    .background(Color.Black).border(5.dp, Color.White, CircleShape)
                    .clickable(enabled = enabled, onClickLabel = strings.play, role = Role.Button, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.PlayArrow, strings.play, tint = Color.White, modifier = Modifier.size(34.dp)) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TorqueActionTag(Icons.Rounded.Shuffle, strings.shuffle, LevyraActivePalette.blue, Color.White, enabled, Modifier.weight(1f), onShuffle)
            TorqueActionTag(Icons.Rounded.Radio, strings.mix, LevyraPanelSoft, LevyraText, enabled, Modifier.weight(1f), onMix)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(LevyraPanelSoft))
        Column(Modifier.padding(vertical = 6.dp)) { rows() }
    }
}

@Composable
internal fun TorquePlaylistHitRow(
    playlists: List<com.luc4n3x.levyra.domain.PlaylistHit>,
    onOpen: (com.luc4n3x.levyra.domain.PlaylistHit) -> Unit,
    onPlay: (com.luc4n3x.levyra.domain.PlaylistHit) -> Unit,
    onDownload: (com.luc4n3x.levyra.domain.PlaylistHit) -> Unit
) {
    val strings = LocalLevyraStrings.current
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(
            playlists,
            key = { index, playlist -> "torque-playlist-hit-$index-${playlist.playlistId.ifBlank { playlist.title }}" },
            contentType = { _, _ -> "torque-playlist-hit" }
        ) { index, playlist ->
            Column(
                Modifier.width(164.dp).background(LevyraPanel)
                    .clickable(onClickLabel = strings.open, role = Role.Button) { onOpen(playlist) }
            ) {
                Box(Modifier.size(164.dp).background(LevyraPanelSoft)) {
                    if (playlist.thumbnailUrl.isNotBlank()) AsyncImage(playlist.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clearAndSetSemantics { })
                    Box(Modifier.align(Alignment.TopStart).padding(8.dp).clearAndSetSemantics { }) { TorqueRank(index + 1, false) }
                    Box(
                        Modifier.align(Alignment.TopEnd).size(48.dp).clickable(onClickLabel = strings.downloadPlaylist, role = Role.Button) { onDownload(playlist) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                            Icon(androidx.compose.material.icons.Icons.Rounded.Download, strings.downloadPlaylist, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(10.dp).size(48.dp).clip(CircleShape).background(Color.Black).border(3.dp, Color.White, CircleShape)
                            .clickable(onClickLabel = strings.play, role = Role.Button) { onPlay(playlist) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.PlayArrow, strings.play, tint = Color.White, modifier = Modifier.size(24.dp)) }
                }
                Box(Modifier.fillMaxWidth().height(3.dp).background(LevyraActivePalette.blue))
                Column(Modifier.padding(10.dp)) {
                    Text(playlist.title.uppercase(), color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val detail = listOf(playlist.author, playlist.trackCountLabel.trim()).filter(String::isNotBlank).joinToString(" · ")
                    if (detail.isNotBlank()) Text(detail, color = com.luc4n3x.levyra.ui.theme.LevyraMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun TorqueActionTag(icon: ImageVector, label: String, background: Color, content: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.heightIn(min = 48.dp).clip(TorqueTagShape).background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label.uppercase(), color = content, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
