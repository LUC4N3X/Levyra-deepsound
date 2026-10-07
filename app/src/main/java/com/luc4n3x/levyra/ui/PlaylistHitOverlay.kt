package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.domain.PlaylistHitPreview
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.displayTrackCount
import com.luc4n3x.levyra.domain.nextTrackAfter
import com.luc4n3x.levyra.ui.album.AlbumNeutralPaletteEnd
import com.luc4n3x.levyra.ui.album.AlbumNeutralPaletteStart
import com.luc4n3x.levyra.ui.artwork.ArtworkBackdropWash
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.components.PlayerGlassIconButton
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.library.LibraryEmpty
import com.luc4n3x.levyra.ui.library.LibraryNowPlayingDock
import com.luc4n3x.levyra.ui.library.LibraryTrackRow
import com.luc4n3x.levyra.ui.media.ImmersiveMediaActionRow
import com.luc4n3x.levyra.ui.media.ImmersiveMediaColors
import com.luc4n3x.levyra.ui.media.ImmersiveMediaHero
import com.luc4n3x.levyra.ui.media.ImmersiveMediaPrimaryAction
import com.luc4n3x.levyra.ui.media.ImmersiveMediaTopBar
import com.luc4n3x.levyra.ui.media.animatedImmersiveMediaColors
import com.luc4n3x.levyra.ui.media.immersiveHeroHeight
import com.luc4n3x.levyra.ui.media.immersiveMediaColors
import com.luc4n3x.levyra.ui.media.immersiveMediaGutter
import com.luc4n3x.levyra.ui.media.ImmersiveTopBarButtonFill
import com.luc4n3x.levyra.ui.media.ImmersiveTopBarButtonSize
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlass
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraText

private val PlaylistHitActionShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

@Composable
internal fun PlaylistHitOverlay(
    preview: PlaylistHitPreview,
    currentTrack: Track?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    downloadedTrackIds: Set<String>,
    downloadProgressByTrackId: Map<String, Int>,
    animationsEnabled: Boolean,
    onClose: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onDownload: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onQueueTrack: (Track) -> Unit,
    onTogglePlayback: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val density = LocalDensity.current
    val listState = rememberSaveable(preview.hit.playlistId, saver = LazyListState.Saver) { LazyListState() }
    val tracks = preview.tracks
    val artworkUrl = preview.hit.thumbnailUrl
    val paletteKey = remember(preview.hit.playlistId, preview.hit.browseId, artworkUrl) {
        playlistPaletteKey(preview)
    }
    val fallbackPalette = remember {
        ArtworkPalette(AlbumNeutralPaletteStart.toArgb(), AlbumNeutralPaletteEnd.toArgb())
    }
    val palette by rememberArtworkPalette(paletteKey, artworkUrl, fallbackPalette)
    val lightTheme = LevyraIsLight
    val targetColors = remember(palette, lightTheme) {
        immersiveMediaColors(Color(palette.start), Color(palette.end), lightTheme)
    }
    val colors = animatedImmersiveMediaColors(targetColors, animated = animationsEnabled, labelPrefix = "playlist-hit")
    val countLabel = remember(preview.hit.trackCountLabel, tracks.size, strings) {
        preview.hit.displayTrackCount(tracks.size, strings::formatTrackCount)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.base)
            .windowInsetsPadding(LevyraHorizontalSafeInsets)
    ) {
        val wide = resolvePlayerPane(maxWidth.value, maxHeight.value) == LevyraPlayerPane.SideBySide
        val topBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
        val heroHeight = immersiveHeroHeight(wide, maxWidth, maxHeight, topBarHeight)
        val collapseThreshold = with(density) { (heroHeight - topBarHeight).coerceAtLeast(0.dp).toPx() }
        val collapsedState = remember(listState, collapseThreshold) {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > collapseThreshold
            }
        }

        ArtworkBackdropWash(
            artworkUrl = artworkUrl,
            tint = colors.fieldTop,
            base = colors.base,
            modifier = Modifier.fillMaxSize()
        )
        PlaylistHitList(
            preview = preview,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            favoriteIds = favoriteIds,
            downloadedTrackIds = downloadedTrackIds,
            downloadProgressByTrackId = downloadProgressByTrackId,
            colors = colors,
            wide = wide,
            viewportWidth = maxWidth,
            viewportHeight = maxHeight,
            topBarHeight = topBarHeight,
            countLabel = countLabel,
            artworkUrl = artworkUrl,
            state = listState,
            onPlay = onPlay,
            onShuffle = onShuffle,
            onDownload = onDownload,
            onPlayTrack = onPlayTrack,
            onFavorite = onFavorite,
            onDownloadTrack = onDownloadTrack,
            onQueueTrack = onQueueTrack
        )
        PlaylistHitTopBar(preview, colors, collapsedState, topBarHeight, animationsEnabled, onClose)
        currentTrack?.let { track ->
            PlaylistHitNowPlayingDock(
                preview = preview,
                track = track,
                isPlaying = isPlaying,
                onTogglePlayback = onTogglePlayback,
                onOpenPlayer = onOpenPlayer,
                onPlayTrack = onPlayTrack,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private fun playlistPaletteKey(preview: PlaylistHitPreview): String {
    val artworkUrl = preview.hit.thumbnailUrl
    if (artworkUrl.isBlank()) return ""
    return ArtworkPaletteCache.key(
        trackId = "playlist:${preview.hit.playlistId.ifBlank { preview.hit.browseId }}",
        thumbnailUrl = artworkUrl,
        largeThumbnailUrl = artworkUrl
    )
}

@Composable
private fun PlaylistHitList(
    preview: PlaylistHitPreview,
    currentTrack: Track?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    downloadedTrackIds: Set<String>,
    downloadProgressByTrackId: Map<String, Int>,
    colors: ImmersiveMediaColors,
    wide: Boolean,
    viewportWidth: Dp,
    viewportHeight: Dp,
    topBarHeight: Dp,
    countLabel: String,
    artworkUrl: String,
    state: LazyListState,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onDownload: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onQueueTrack: (Track) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val tracks = preview.tracks
    val gutter = immersiveMediaGutter(viewportWidth)
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = if (currentTrack != null) 220.dp else 110.dp)
    ) {
        item(key = "playlist-hit-hero", contentType = "playlist-hit-hero") {
            ImmersiveMediaHero(
                title = preview.hit.title,
                overline = strings.speedDialCopy().playlist,
                subtitle = preview.hit.author,
                metadata = countLabel,
                colors = colors,
                wide = wide,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
                topBarHeight = topBarHeight,
                onSubtitleClick = null,
                actions = {
                    ImmersiveMediaActionRow(
                        primary = ImmersiveMediaPrimaryAction(
                            enabled = tracks.isNotEmpty(),
                            label = strings.play,
                            contentDescription = strings.play,
                            icon = Icons.Rounded.PlayArrow,
                            onClick = onPlay
                        ),
                        shuffleLabel = strings.shuffle,
                        downloadLabel = strings.downloadPlaylist,
                        colors = colors,
                        shuffleEnabled = tracks.size > 1,
                        downloadEnabled = tracks.isNotEmpty(),
                        onShuffle = onShuffle,
                        onDownload = onDownload
                    )
                },
                artwork = {
                    SeamlessArtworkImage(
                        url = artworkUrl,
                        contentDescription = preview.hit.title,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(AlbumNeutralPaletteStart, AlbumNeutralPaletteEnd)))
                        )
                    }
                }
            )
        }
        when {
            preview.loading -> item(key = "playlist-hit-loading", contentType = "playlist-hit-state") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LevyraLoadingIndicator(modifier = Modifier.size(28.dp), color = colors.accent)
                }
            }
            tracks.isEmpty() -> item(key = "playlist-hit-empty", contentType = "playlist-hit-state") {
                Box(modifier = Modifier.padding(horizontal = gutter)) {
                    LibraryEmpty(Icons.AutoMirrored.Rounded.QueueMusic, strings.albumTracksUnavailable)
                }
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
                    onQueue = { onQueueTrack(track) },
                    modifier = Modifier.padding(horizontal = gutter - LevyraCardDesign.RowHorizontalPadding)
                )
            }
        }
    }
}

@Composable
private fun PlaylistHitTopBar(
    preview: PlaylistHitPreview,
    colors: ImmersiveMediaColors,
    collapsedState: State<Boolean>,
    height: Dp,
    animationsEnabled: Boolean,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    var menuExpanded by rememberSaveable(preview.hit.playlistId) { mutableStateOf(false) }
    ImmersiveMediaTopBar(
        title = preview.hit.title,
        colors = colors,
        collapsedState = collapsedState,
        height = height,
        animated = animationsEnabled,
        backLabel = strings.back,
        onBack = onClose,
        actions = {
            Box {
                PlayerGlassIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = strings.more,
                    onClick = { menuExpanded = true },
                    size = ImmersiveTopBarButtonSize,
                    tint = Color.White,
                    fill = ImmersiveTopBarButtonFill,
                    borderTop = Color.Transparent,
                    borderBottom = Color.Transparent
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(colors.fieldTop)
                ) {
                    DropdownMenuItem(
                        text = { Text(strings.share) },
                        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            val id = preview.hit.playlistId.ifBlank { preview.hit.browseId.removePrefix("VL") }
                            val text = buildString {
                                append(preview.hit.title)
                                if (id.isNotBlank()) append("\nhttps://music.youtube.com/playlist?list=").append(id)
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, strings.share))
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun PlaylistHitNowPlayingDock(
    preview: PlaylistHitPreview,
    track: Track,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onOpenPlayer: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val tracks = preview.tracks
    val nextTrack = remember(tracks, track.id) { preview.nextTrackAfter(track.id) }
    val onNext = nextTrack?.let { next -> { onPlayTrack(next) } }
    LibraryNowPlayingDock(
        track = track,
        isPlaying = isPlaying,
        onToggle = onTogglePlayback,
        onOpen = onOpenPlayer,
        onNext = onNext,
        nextEnabled = nextTrack != null,
        nextLabel = nextTrack?.let { "${strings.next}: ${it.title}" }.orEmpty(),
        modifier = modifier
            .navigationBarsPadding()
            .padding(14.dp)
    )
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
