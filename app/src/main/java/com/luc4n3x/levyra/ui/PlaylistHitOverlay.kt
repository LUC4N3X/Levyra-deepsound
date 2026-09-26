package com.luc4n3x.levyra.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.PlaylistHitPreview
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.displayTrackCount
import com.luc4n3x.levyra.domain.nextTrackAfter
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.library.LibraryArtwork
import com.luc4n3x.levyra.ui.library.LibraryEmpty
import com.luc4n3x.levyra.ui.library.LibraryNowPlayingDock
import com.luc4n3x.levyra.ui.library.LibraryTrackRow
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlass
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm

private val PlaylistHitActionShape = RoundedCornerShape(16.dp)
private val PlaylistHitCoverShape = RoundedCornerShape(20.dp)

@Composable
internal fun PlaylistHitOverlay(
    preview: PlaylistHitPreview,
    currentTrack: Track?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    downloadedTrackIds: Set<String>,
    downloadProgressByTrackId: Map<String, Int>,
    onClose: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onDownload: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onQueueTrack: (Track) -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val listState = rememberSaveable(preview.hit.playlistId, saver = LazyListState.Saver) { LazyListState() }
    val tracks = preview.tracks
    val nextTrack = remember(tracks, currentTrack?.id) { preview.nextTrackAfter(currentTrack?.id) }
    val currentInPlaylist = remember(tracks, currentTrack?.id) {
        currentTrack != null && tracks.any { it.id == currentTrack.id }
    }

    Box(modifier = Modifier.fillMaxSize().background(LevyraInk)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = if (currentTrack != null) 220.dp else 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "playlist-hit-header", contentType = "playlist-hit-header") {
                PlaylistHitHeader(
                    hit = preview.hit,
                    loadedTracks = tracks.size,
                    actionsEnabled = tracks.isNotEmpty(),
                    onClose = onClose,
                    onPlay = onPlay,
                    onShuffle = onShuffle,
                    onDownload = onDownload
                )
            }
            when {
                preview.loading -> item(key = "playlist-hit-loading", contentType = "playlist-hit-state") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp,
                            color = LevyraCyan
                        )
                    }
                }
                tracks.isEmpty() -> item(key = "playlist-hit-empty", contentType = "playlist-hit-state") {
                    LibraryEmpty(Icons.AutoMirrored.Rounded.QueueMusic, strings.albumTracksUnavailable)
                }
                else -> itemsIndexed(
                    items = tracks,
                    key = { index, track -> "playlist-hit-track-$index-${track.id}" },
                    contentType = { _, _ -> "playlist-hit-track" }
                ) { _, track ->
                    val isCurrent = track.id == currentTrack?.id
                    LibraryTrackRow(
                        track = track,
                        selected = false,
                        selectionActive = false,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        isFavorite = track.id in favoriteIds,
                        isDownloaded = track.id in downloadedTrackIds,
                        downloadProgress = downloadProgressByTrackId[track.id],
                        onClick = { onPlayTrack(track) },
                        onLongClick = null,
                        onFavorite = { onFavorite(track) },
                        onDownload = { onDownloadTrack(track) },
                        onQueue = { onQueueTrack(track) }
                    )
                }
            }
        }
        if (currentTrack != null) {
            LibraryNowPlayingDock(
                track = currentTrack,
                isPlaying = isPlaying,
                onToggle = onTogglePlayback,
                onOpen = onOpenPlayer,
                onNext = when {
                    tracks.isEmpty() -> null
                    currentInPlaylist -> onSkipNext
                    else -> ({ if (nextTrack != null) onPlayTrack(nextTrack) })
                },
                nextEnabled = currentInPlaylist || nextTrack != null,
                nextLabel = if (currentInPlaylist) "" else nextTrack?.let { "${strings.next}: ${it.title}" }.orEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(14.dp)
            )
        }
    }
}

@Composable
private fun PlaylistHitHeader(
    hit: PlaylistHit,
    loadedTracks: Int,
    actionsEnabled: Boolean,
    onClose: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onDownload: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val countLabel = remember(hit.trackCountLabel, loadedTracks, strings) {
        hit.displayTrackCount(loadedTracks, strings::formatTrackCount)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = LevyraGlass,
            border = BorderStroke(1.dp, LevyraGlassBorder),
            shape = CircleShape,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .levyraPressable(onClick = onClose, role = Role.Button)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.back,
                    tint = LevyraText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LibraryArtwork(
                url = hit.thumbnailUrl,
                title = hit.title,
                modifier = Modifier
                    .size(124.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = PlaylistHitCoverShape,
                        clip = false,
                        ambientColor = LevyraCyan.copy(alpha = 0.20f),
                        spotColor = Color.Black.copy(alpha = 0.60f)
                    ),
                shape = PlaylistHitCoverShape,
                selected = false
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = hit.title,
                    color = LevyraText,
                    fontSize = 22.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(22.sp),
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.6).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (hit.author.isNotBlank()) {
                    Text(
                        text = hit.author,
                        color = LevyraMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (countLabel.isNotBlank()) {
                    Text(
                        text = countLabel,
                        color = LevyraMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistHitActionButton(
                icon = Icons.Rounded.PlayArrow,
                text = strings.play,
                showText = true,
                primary = true,
                enabled = actionsEnabled,
                onClick = onPlay,
                modifier = Modifier.weight(1f)
            )
            PlaylistHitActionButton(
                icon = Icons.Rounded.Shuffle,
                text = strings.shuffle,
                showText = false,
                primary = false,
                enabled = actionsEnabled,
                onClick = onShuffle
            )
            PlaylistHitActionButton(
                icon = Icons.Rounded.Download,
                text = strings.downloadPlaylist,
                showText = false,
                primary = false,
                enabled = actionsEnabled,
                onClick = onDownload
            )
        }
    }
}

@Composable
internal fun PlaylistHitActionButton(
    icon: ImageVector,
    text: String,
    showText: Boolean,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        color = if (primary) LevyraCyan else LevyraGlass,
        contentColor = if (primary) LevyraOnAccent else LevyraText,
        border = if (primary) null else BorderStroke(1.dp, LevyraGlassBorder),
        shape = PlaylistHitActionShape,
        modifier = modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(PlaylistHitActionShape)
            .levyraPressable(
                onClick = onClick,
                enabled = enabled,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (showText) 14.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = if (showText) null else text,
                modifier = Modifier.size(if (showText) 22.dp else 24.dp)
            )
            if (showText) {
                Spacer(Modifier.width(6.dp))
                Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
