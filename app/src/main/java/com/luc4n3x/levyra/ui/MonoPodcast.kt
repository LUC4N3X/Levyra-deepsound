package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.PodcastShow
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.podcast.PodcastEpisodeProgress
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraMonoMediaStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.PodcastCopy
import com.luc4n3x.levyra.ui.media.MonoLiveWave
import com.luc4n3x.levyra.ui.media.monoNumeralStyle
import com.luc4n3x.levyra.ui.media.monoRowRule
import com.luc4n3x.levyra.ui.theme.MonoDesign

private val MonoShowCover = 128.dp
private val MonoEpisodeArtwork = 56.dp

@Composable
internal fun MonoPodcastShowHeader(show: PodcastShow, description: String, subscribed: Boolean, copy: PodcastCopy, onToggle: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val mono = remember(strings.code) { LevyraMonoMediaStrings.forCode(strings.code) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.size(MonoShowCover).clip(MonoDesign.artShape).background(MonoDesign.surface)
                    .border(1.dp, MonoDesign.hairlineStrong, MonoDesign.artShape)
            ) {
                if (show.thumbnailUrl.isNotBlank()) {
                    AsyncImage(show.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MonoOverline(copy.title)
                Text(
                    show.title, style = MaterialTheme.typography.headlineSmall, color = MonoDesign.text,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                )
                if (show.author.isNotBlank()) {
                    Text(show.author, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        MonoPill(
            if (subscribed) copy.subscribed else copy.subscribe, selected = !subscribed, onClick = onToggle,
            modifier = Modifier.heightIn(min = 48.dp).semantics { stateDescription = if (subscribed) copy.subscribed else copy.subscribe }
        )
        if (description.isNotBlank()) {
            var expanded by rememberSaveable(show.browseId) { mutableStateOf(false) }
            Column(
                Modifier.fillMaxWidth().monoPanel()
                    .levyraPressable(onClick = { expanded = !expanded }, role = Role.Button, pressedScale = LevyraPressScale.Row,
                        onClickLabel = if (expanded) strings.showLess else strings.readAll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonoOverline(mono.aboutShow)
                Text(
                    description, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary,
                    maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun monoEpisodeParts(episode: Track): Pair<String, String> {
    val parts = episode.album.split(" • ", " · ").map(String::trim).filter { it.isNotBlank() && it != episode.artist }
    return parts.firstOrNull().orEmpty() to parts.drop(1).joinToString(" · ")
}

@Composable
internal fun MonoPodcastEpisodeRow(
    episode: Track,
    progress: PodcastEpisodeProgress?,
    copy: PodcastCopy,
    playback: PodcastPlayback,
    onPlay: () -> Unit,
    onSetPlayed: (Boolean) -> Unit,
    onRemove: (() -> Unit)?
) {
    val strings = LocalLevyraStrings.current
    val current = playback.currentTrackId == episode.id
    val playing = current && playback.isPlaying
    val (date, duration) = remember(episode.album, episode.artist) { monoEpisodeParts(episode) }
    val status = when {
        progress?.completed == true -> copy.played
        progress?.inProgress == true && progress.durationMs > 0L ->
            copy.minutesLeft(((progress.durationMs - progress.positionMs) / 60_000L).coerceAtLeast(1L))
        else -> duration
    }
    val fraction = progress?.takeIf { it.inProgress && it.durationMs > 0L }?.fraction
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.padding(horizontal = MonoDesign.gutter).fillMaxWidth().heightIn(min = 76.dp)
            .monoRowRule(null)
            .levyraPressable(onClick = onPlay, role = Role.Button, onClickLabel = episode.title, pressedScale = LevyraPressScale.Row)
            .semantics { if (current) stateDescription = strings.playing }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(MonoEpisodeArtwork).clip(MonoDesign.artShape).background(MonoDesign.surface)
                .border(1.dp, MonoDesign.hairline, MonoDesign.artShape),
            contentAlignment = Alignment.Center
        ) {
            if (episode.thumbnailUrl.isNotBlank()) {
                AsyncImage(episode.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (date.isNotBlank()) MonoOverline(date)
            Text(
                episode.title, style = MaterialTheme.typography.titleSmall,
                color = if (current) MonoDesign.accent else MonoDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (progress?.completed == true) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = MonoDesign.accent, modifier = Modifier.size(12.dp))
                }
                if (status.isNotBlank()) {
                    Text(
                        status, style = monoNumeralStyle(),
                        color = if (progress?.completed == true) MonoDesign.accent else MonoDesign.textSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (fraction != null) {
                Box(
                    Modifier.padding(top = 4.dp).fillMaxWidth().height(2.dp).clearAndSetSemantics {}.drawBehind {
                        val radius = CornerRadius(size.height / 2f)
                        drawRoundRect(MonoDesign.hairlineStrong, cornerRadius = radius)
                        val width = size.width * fraction
                        val start = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                        drawRoundRect(MonoDesign.accent, Offset(start, 0f), Size(width, size.height), radius)
                    }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Control, onClickLabel = strings.play),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape)
                        .then(if (playing) Modifier.background(MonoDesign.accentGlow) else Modifier)
                        .border(1.dp, if (current) MonoDesign.accent else MonoDesign.hairlineStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (playing) {
                        MonoLiveWave(true)
                    } else {
                        Icon(Icons.Rounded.PlayArrow, strings.play, tint = if (current) MonoDesign.accent else MonoDesign.text, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, episode.title, tint = MonoDesign.textSecondary, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = MonoDesign.raised) {
                    val completed = progress?.completed == true
                    DropdownMenuItem(
                        text = { Text(if (completed) copy.markUnplayed else copy.markPlayed, color = MonoDesign.text) },
                        onClick = { menuOpen = false; onSetPlayed(!completed) }
                    )
                    if (onRemove != null) {
                        DropdownMenuItem(text = { Text(copy.removeFromHistory, color = MonoDesign.text) }, onClick = { menuOpen = false; onRemove() })
                    }
                }
            }
        }
    }
}
