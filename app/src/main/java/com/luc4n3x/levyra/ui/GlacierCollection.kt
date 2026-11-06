package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.i18n.likeAllCopy
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Download
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.data.fullResolutionArtworkUrl
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.theme.MonoCodeFont
import com.luc4n3x.levyra.ui.theme.MonoDesign

@Composable
private fun GlacierRoundKey(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(MonoDesign.raised)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = if (enabled) MonoDesign.text else MonoDesign.textFaint, modifier = Modifier.size(21.dp)) }
}

@Composable
internal fun GlacierCollectionScreen(
    title: String,
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onTrackActions: (Track) -> Unit,
    bottomInset: Dp,
    artworkUrl: String = "",
    byline: String = "Levyra",
    loading: Boolean = false,
    glow: Color = MonoDesign.accent,
    ranked: Boolean = false,
    failedLabel: String? = null,
    retryLabel: String = "",
    onRetry: () -> Unit = {},
    cover: (@Composable () -> Unit)? = null,
    onDownloadAll: (() -> Unit)? = null,
    onLikeAll: (() -> Unit)? = null
) {
    BackHandler(onBack = onDismiss)
    val strings = LocalLevyraStrings.current
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val mosaic = remember(tracks) { tracks.distinctBy { it.thumbnailUrl.ifBlank { it.id } }.take(4) }
    val artists = remember(tracks) { tracks.map { it.artist.trim() }.filter(String::isNotBlank).distinct().take(6).joinToString(" · ") }
    val totalMs = remember(tracks) { tracks.sumOf { it.durationMs.coerceAtLeast(0L) } }
    val playing = isPlaying && tracks.any { it.id == currentId }
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(MonoDesign.base)) {
        Box(
            Modifier.fillMaxWidth().height(520.dp).drawBehind {
                drawRect(Brush.verticalGradient(0f to glow.copy(alpha = 0.22f), 0.55f to glow.copy(alpha = 0.06f), 1f to Color.Transparent))
            }
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = top + 56.dp, bottom = bottomInset + 24.dp)) {
            item(key = "glacier-collection-cover") {
                Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(248.dp).shadow(24.dp, MonoDesign.cardShape).clip(MonoDesign.cardShape).background(MonoDesign.raised)) {
                        when {
                            cover != null -> cover()
                            artworkUrl.isNotBlank() -> MonoArtwork(fullResolutionArtworkUrl(artworkUrl), Modifier.fillMaxSize())
                            mosaic.size >= 4 -> Column {
                                mosaic.chunked(2).forEach { row ->
                                    Row(Modifier.weight(1f)) {
                                        row.forEach { track -> Box(Modifier.weight(1f).fillMaxSize()) { CoverImage(track, Modifier.fillMaxSize()) } }
                                    }
                                }
                            }
                            mosaic.isNotEmpty() -> CoverImage(mosaic.first(), Modifier.fillMaxSize(), highRes = true)
                        }
                    }
                }
            }
            item(key = "glacier-collection-title") {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = MonoDesign.gutter).padding(top = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        title, style = MaterialTheme.typography.headlineMedium, color = MonoDesign.text, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        "$byline · ${strings.formatTrackCount(tracks.size)} · ${strings.formatLibraryDuration(totalMs)}",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoCodeFont), color = MonoDesign.textSecondary, maxLines = 1
                    )
                    if (artists.isNotBlank()) {
                        Text(
                            artists, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary, textAlign = TextAlign.Center,
                            maxLines = if (expanded) 6 else 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp).clickable(role = Role.Button) { expanded = !expanded }
                        )
                    }
                }
            }
            item(key = "glacier-collection-actions") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onDownloadAll?.let { GlacierRoundKey(Icons.Rounded.Download, strings.downloadPlaylist, tracks.isNotEmpty(), it) }
                    onLikeAll?.let { GlacierRoundKey(Icons.Rounded.FavoriteBorder, strings.likeAllCopy().action, tracks.isNotEmpty(), it) }
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).border(1.dp, MonoDesign.hairlineStrong, CircleShape)
                            .clickable(enabled = tracks.size > 1, role = Role.Button, onClickLabel = strings.shuffle, onClick = onShuffle)
                            .semantics { contentDescription = strings.shuffle },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Shuffle, null, tint = MonoDesign.text, modifier = Modifier.size(22.dp)) }
                    Box(
                        Modifier.size(72.dp).clip(rememberLevyraPlayMorphShape(playing = playing, animated = LocalAnimationsEnabled.current))
                            .background(MonoDesign.accent)
                            .clickable(enabled = tracks.isNotEmpty(), role = Role.Button, onClickLabel = strings.playAll, onClick = onPlayAll),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.PlayArrow, strings.playAll, tint = MonoDesign.onAccent, modifier = Modifier.size(34.dp)) }
                }
            }
            if (loading && tracks.isEmpty()) {
                item(key = "glacier-collection-loading") {
                    Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        MonoWaveLine(playing = true, color = MonoDesign.accent, modifier = Modifier.size(width = 120.dp, height = 28.dp))
                    }
                }
            }
            if (failedLabel != null && !loading && tracks.isEmpty()) {
                item(key = "glacier-collection-failed") {
                    Column(Modifier.fillMaxWidth().padding(MonoDesign.gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(failedLabel, style = MaterialTheme.typography.bodyLarge, color = MonoDesign.textSecondary)
                        if (retryLabel.isNotBlank()) MonoPill(retryLabel, selected = false, onClick = onRetry)
                    }
                }
            }
            itemsIndexed(tracks, key = { index, track -> "glacier-collection-$index-${track.id}" }) { index, track ->
                MonoCompactRow(
                    track = track,
                    isCurrent = track.id == currentId,
                    isPlaying = isPlaying,
                    actionsLabel = strings.songOptions,
                    onPlay = { onPlay(track) },
                    onActions = { onTrackActions(track) },
                    rank = if (ranked) index + 1 else null
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = top + 4.dp, start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MonoDesign.void.copy(alpha = 0.4f)).clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = MonoDesign.text, modifier = Modifier.size(22.dp)) }
        }
    }
}
