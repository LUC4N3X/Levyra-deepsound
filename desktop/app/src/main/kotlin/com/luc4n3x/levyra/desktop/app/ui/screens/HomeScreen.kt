package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.DiscoverUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.SectionHeader
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.LibraryData
import com.luc4n3x.levyra.desktop.core.storage.LocalPlaylist

@Composable
fun HomeScreen(
    library: LibraryData,
    discover: DiscoverUiState,
    currentTrack: Track?,
    actions: TrackActions,
    onOpenSearch: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenNewReleases: () -> Unit,
    onPlayMix: (List<Track>) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onImportUrl: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val accent = LocalAccentColor.current
    var newPlaylistName by remember { mutableStateOf("") }
    var importUrl by remember { mutableStateOf("") }

    val historyTracks = remember(library.history) {
        library.history
            .sortedByDescending { it.playedAt }
            .map { it.track }
            .distinctBy(::trackIdentity)
    }
    val orbitTracks = remember(currentTrack, historyTracks, library.favorites, discover.tracks) {
        buildList {
            currentTrack?.let(::add)
            addAll(historyTracks)
            addAll(library.favorites)
            addAll(discover.tracks)
        }
            .asSequence()
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .distinctBy(::trackIdentity)
            .take(ORBIT_LIMIT)
            .toList()
    }
    val mixTracks = remember(library.favorites, historyTracks, discover.tracks, orbitTracks) {
        (orbitTracks + library.favorites + historyTracks + discover.tracks.take(20))
            .distinctBy(::trackIdentity)
    }
    val featuredQueue = orbitTracks.ifEmpty { historyTracks.ifEmpty { discover.tracks } }
    val featured = featuredQueue.firstOrNull()

    ScrollableColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 30.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = strings.homeGreeting,
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = strings.homeSubtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SearchLauncher(
                label = strings.homeSearchHint,
                onClick = onOpenSearch
            )
        }

        item {
            FeaturedPanel(
                track = featured,
                accent = accent,
                playLabel = strings.playAll,
                discoverLabel = strings.navDiscover,
                emptyTitle = strings.homeEmptyTitle,
                emptyBody = strings.homeEmptyBody,
                onPlay = {
                    if (featuredQueue.isNotEmpty()) {
                        actions.onPlay(featuredQueue, 0)
                    } else {
                        onOpenSearch()
                    }
                },
                onDiscover = onOpenDiscover
            )
        }

        item {
            SectionHeader(title = strings.homeQuickAccess)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessCard(
                    icon = LevyraIcons.Shuffle,
                    title = strings.homeMix,
                    subtitle = strings.homeMixBody,
                    tint = LevyraBrand.violet,
                    enabled = mixTracks.isNotEmpty(),
                    onClick = { onPlayMix(mixTracks) },
                    modifier = Modifier.weight(1f)
                )
                QuickAccessCard(
                    icon = LevyraIcons.HeartFilled,
                    title = strings.homeFavorites,
                    subtitle = if (library.favorites.isEmpty()) {
                        strings.homeFavoritesBody
                    } else {
                        "${library.favorites.size} ${strings.playlistTracks}"
                    },
                    tint = accent,
                    enabled = library.favorites.isNotEmpty(),
                    onClick = { actions.onPlay(library.favorites, 0) },
                    modifier = Modifier.weight(1f)
                )
                QuickAccessCard(
                    icon = LevyraIcons.Disc,
                    title = strings.homeNewReleases,
                    subtitle = strings.homeNewReleasesBody,
                    tint = MaterialTheme.colorScheme.tertiary,
                    enabled = true,
                    onClick = onOpenNewReleases,
                    modifier = Modifier.weight(1f)
                )
                QuickAccessCard(
                    icon = LevyraIcons.Chart,
                    title = strings.homeTop50,
                    subtitle = strings.homeTop50Body,
                    tint = accent,
                    enabled = true,
                    onClick = onOpenDiscover,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (orbitTracks.isNotEmpty()) {
            item {
                PersonalOrbitPanel(
                    tracks = orbitTracks,
                    currentTrackId = currentTrack?.id.orEmpty(),
                    accent = accent,
                    title = strings.homeOrbitTitle,
                    subtitle = strings.homeOrbitSubtitle,
                    playLabel = strings.playAll,
                    onPlayAll = { actions.onPlay(orbitTracks, 0) },
                    onPlayTrack = { index -> actions.onPlay(orbitTracks, index) }
                )
            }
        }

        if (historyTracks.isNotEmpty()) {
            item {
                SectionHeader(title = strings.homeContinueListening) {
                    TextButton(onClick = onClearHistory) {
                        Text(strings.historyClear)
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(historyTracks.take(14), key = { it.id }) { track ->
                        val index = historyTracks.indexOfFirst { it.id == track.id }
                        MusicCard(
                            track = track,
                            onClick = { actions.onPlay(historyTracks, index.coerceAtLeast(0)) }
                        )
                    }
                }
            }
        }

        if (discover.tracks.isNotEmpty()) {
            item {
                SectionHeader(title = strings.homeTopTracks) {
                    TextButton(onClick = onOpenDiscover) {
                        Text(strings.homeSeeAll)
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(discover.tracks.take(12), key = { it.id }) { track ->
                        val index = discover.tracks.indexOfFirst { it.id == track.id }
                        MusicCard(
                            track = track,
                            badge = (index + 1).toString(),
                            onClick = { actions.onPlay(discover.tracks, index.coerceAtLeast(0)) }
                        )
                    }
                }
            }
        }

        if (library.playlists.isNotEmpty()) {
            item {
                SectionHeader(title = strings.homePlaylists)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(library.playlists, key = { it.id }) { playlist ->
                        PlaylistTile(
                            playlist = playlist,
                            onClick = { onOpenPlaylist(playlist.id) }
                        )
                    }
                }
            }
        }

        if (library.favorites.isNotEmpty()) {
            item {
                SectionHeader(title = strings.homeFavorites)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(library.favorites.take(14), key = { it.id }) { track ->
                        val index = library.favorites.indexOfFirst { it.id == track.id }
                        MusicCard(
                            track = track,
                            onClick = { actions.onPlay(library.favorites, index.coerceAtLeast(0)) }
                        )
                    }
                }
            }
        }

        item {
            LibraryTools(
                title = strings.homeLibraryTools,
                body = strings.homeLibraryToolsBody,
                playlistName = newPlaylistName,
                playlistNameLabel = strings.playlistName,
                playlistAction = strings.playlistCreate,
                importUrl = importUrl,
                importLabel = strings.importPlaylistHint,
                importAction = strings.importAction,
                onPlaylistNameChange = { newPlaylistName = it },
                onCreatePlaylist = {
                    onCreatePlaylist(newPlaylistName)
                    newPlaylistName = ""
                },
                onImportUrlChange = { importUrl = it },
                onImport = {
                    onImportUrl(importUrl)
                    importUrl = ""
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SearchLauncher(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = LevyraIcons.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeaturedPanel(
    track: Track?,
    accent: Color,
    playLabel: String,
    discoverLabel: String,
    emptyTitle: String,
    emptyBody: String,
    onPlay: () -> Unit,
    onDiscover: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.28f),
                        LevyraBrand.violet.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = track?.title ?: emptyTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track?.displaySubtitle?.ifBlank { emptyBody } ?: emptyBody,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = onPlay) {
                    Icon(
                        imageVector = LevyraIcons.Play,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(playLabel)
                }
                OutlinedButton(onClick = onDiscover) {
                    Text(discoverLabel)
                }
            }
        }

        Surface(
            modifier = Modifier.size(172.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 18.dp
        ) {
            if (track != null) {
                Artwork(
                    url = track.artworkUrl,
                    modifier = Modifier.size(172.dp),
                    cornerRadius = 18.dp,
                    iconSize = 38.dp
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = LevyraIcons.Disc,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalOrbitPanel(
    tracks: List<Track>,
    currentTrackId: String,
    accent: Color,
    title: String,
    subtitle: String,
    playLabel: String,
    onPlayAll: () -> Unit,
    onPlayTrack: (Int) -> Unit
) {
    val columns = tracks.chunked(2)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.17f),
                            LevyraBrand.violet.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        accent.copy(alpha = 0.95f),
                                        LevyraBrand.violet.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LevyraIcons.Disc,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Button(onClick = onPlayAll) {
                    Icon(
                        imageVector = LevyraIcons.Play,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(playLabel)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                columns.forEachIndexed { columnIndex, columnTracks ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        columnTracks.forEachIndexed { rowIndex, track ->
                            val index = columnIndex * 2 + rowIndex
                            OrbitTrackTile(
                                track = track,
                                position = index + 1,
                                isCurrent = track.id == currentTrackId,
                                accent = accent,
                                onClick = { onPlayTrack(index) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrbitTrackTile(
    track: Track,
    position: Int,
    isCurrent: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrent) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        border = BorderStroke(
            1.dp,
            if (isCurrent) accent.copy(alpha = 0.68f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Artwork(
                url = track.artworkUrl,
                modifier = Modifier.size(58.dp),
                cornerRadius = 11.dp,
                iconSize = 22.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = if (isCurrent) accent else MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = position.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrent) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentAlpha = if (enabled) 1f else 0.46f
    Column(
        modifier = modifier
            .height(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        tint.copy(alpha = if (enabled) 0.18f else 0.07f),
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint.copy(alpha = contentAlpha),
                modifier = Modifier.size(19.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MusicCard(
    track: Track,
    onClick: () -> Unit,
    badge: String = ""
) {
    Column(
        modifier = Modifier
            .width(166.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box {
            Artwork(
                url = track.artworkUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                cornerRadius = 11.dp,
                iconSize = 30.dp
            )
            if (badge.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(29.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistTile(playlist: LocalPlaylist, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .width(166.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Artwork(
            url = playlist.artworkUrl,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            cornerRadius = 11.dp,
            iconSize = 30.dp
        )
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${playlist.tracks.size} ${strings.playlistTracks}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LibraryTools(
    title: String,
    body: String,
    playlistName: String,
    playlistNameLabel: String,
    playlistAction: String,
    importUrl: String,
    importLabel: String,
    importAction: String,
    onPlaylistNameChange: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onImportUrlChange: (String) -> Unit,
    onImport: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = onPlaylistNameChange,
                        label = { Text(playlistNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = onCreatePlaylist,
                        enabled = playlistName.isNotBlank(),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(playlistAction)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = importUrl,
                        onValueChange = onImportUrlChange,
                        label = { Text(importLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = onImport,
                        enabled = importUrl.isNotBlank(),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(importAction)
                    }
                }
            }
        }
    }
}

private fun trackIdentity(track: Track): String = buildString {
    append(track.title.trim().lowercase())
    append('|')
    append(track.artist.trim().lowercase())
}

private const val ORBIT_LIMIT = 8
