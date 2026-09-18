package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.locallibrary.LocalAlbumGroup
import com.luc4n3x.levyra.data.locallibrary.LocalArtistGroup
import com.luc4n3x.levyra.data.local.LocalMediaEntity
import com.luc4n3x.levyra.data.locallibrary.LocalFolderGroup
import com.luc4n3x.levyra.data.locallibrary.isSafeTagEditorFormat
import com.luc4n3x.levyra.data.locallibrary.matchesFullTagQuery
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.localLibraryRecentFilterLabel
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlass
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.viewmodel.LocalLibraryUiState

internal enum class LocalLibraryTab { Songs, Albums, Artists, Folders }

internal enum class LocalLibraryQualityFilter { All, Lossless, HighBitrate, Recent }

internal fun LocalLibraryTab.label(strings: LevyraStrings): String = when (this) {
    LocalLibraryTab.Songs -> strings.songsPlain
    LocalLibraryTab.Albums -> strings.albumsPlain
    LocalLibraryTab.Artists -> strings.artists
    LocalLibraryTab.Folders -> strings.localFolders
}

internal data class LocalLibraryCallbacks(
    val onPlay: (List<Track>, Track) -> Unit,
    val onAddToQueue: (List<Track>) -> Unit,
    val onToggleFavorite: (Track) -> Unit,
    val onQuickScan: () -> Unit,
    val onFullScan: () -> Unit,
    val onRebuildLevyra: () -> Unit,
    val onGrantPermission: () -> Unit,
    val onToggleFolderHidden: (String, Boolean) -> Unit,
    val onEditTags: (Track) -> Unit
)

internal fun LazyListScope.localLibrarySection(
    library: LocalLibraryUiState,
    tab: LocalLibraryTab,
    onTab: (LocalLibraryTab) -> Unit,
    qualityFilter: LocalLibraryQualityFilter,
    onQualityFilter: (LocalLibraryQualityFilter) -> Unit,
    expandedGroupKey: String?,
    onExpandGroup: (String?) -> Unit,
    query: String,
    currentTrack: Track?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    unavailableUris: Set<String>,
    callbacks: LocalLibraryCallbacks
) {
    item(key = "local-actions", contentType = "local-actions") {
        LocalLibraryActions(library, callbacks)
    }
    if (!library.permissionGranted) {
        item(key = "local-permission", contentType = "local-permission") {
            LocalLibraryPermissionCard(callbacks.onGrantPermission)
        }
        return
    }
    item(key = "local-tabs", contentType = "local-tabs") {
        LocalLibraryTabs(
            selected = tab,
            onSelect = { entry ->
                onExpandGroup(null)
                onTab(entry)
            }
        )
    }
    if (library.catalog.totalCount == 0) {
        item(key = "local-empty", contentType = "local-empty") {
            LibraryEmpty(Icons.Rounded.LibraryMusic, LocalLevyraStrings.current.localEmpty)
        }
        return
    }
    val availableQualityFilters = localLibraryQualityFilters(
        library.catalog.mediaByUri.values,
        System.currentTimeMillis()
    )
    if (availableQualityFilters.size > 1) {
        item(key = "local-quality-filters", contentType = "local-quality-filters") {
            LocalLibraryQualityFilters(
                filters = availableQualityFilters,
                selected = qualityFilter,
                onSelect = onQualityFilter
            )
        }
    }
    when (tab) {
        LocalLibraryTab.Songs -> {
            val songs = library.catalog.songs
                .filterLocalTracks(query, library.catalog.mediaByUri)
                .filterByLocalQuality(qualityFilter, library.catalog.mediaByUri)
            localTrackItems(
                keyPrefix = "local-song",
                tracks = songs,
                context = songs,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                favoriteIds = favoriteIds,
                unavailableUris = unavailableUris,
                mediaByUri = library.catalog.mediaByUri,
                callbacks = callbacks
            )
        }
        LocalLibraryTab.Albums -> items(
            library.catalog.albums
                .filterLocalAlbums(query, library.catalog.mediaByUri)
                .mapNotNull { it.filteredByLocalQuality(qualityFilter, library.catalog.mediaByUri) },
            key = { "local-album-${it.key}" },
            contentType = { "local-group" }
        ) { album ->
            LocalGroupBlock(
                title = album.title.ifBlank { LocalLevyraStrings.current.localUnknownAlbum },
                subtitle = localGroupSubtitle(album.artist, album.tracks.size, album.year),
                artworkModel = album.artworkModel,
                icon = Icons.Rounded.Album,
                expanded = expandedGroupKey == album.key,
                onToggle = { onExpandGroup(if (expandedGroupKey == album.key) null else album.key) },
                onPlay = { album.tracks.firstOrNull()?.let { callbacks.onPlay(album.tracks, it) } },
                tracks = album.tracks,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                favoriteIds = favoriteIds,
                unavailableUris = unavailableUris,
                mediaByUri = library.catalog.mediaByUri,
                callbacks = callbacks
            )
        }
        LocalLibraryTab.Artists -> items(
            library.catalog.artists
                .filterLocalArtists(query, library.catalog.mediaByUri)
                .mapNotNull { it.filteredByLocalQuality(qualityFilter, library.catalog.mediaByUri) },
            key = { "local-artist-${it.key}" },
            contentType = { "local-group" }
        ) { artist ->
            val strings = LocalLevyraStrings.current
            LocalGroupBlock(
                title = artist.name.ifBlank { strings.localUnknownArtist },
                subtitle = localGroupSubtitle(
                    "${artist.albumCount} ${strings.albumsPlain}",
                    artist.tracks.size,
                    ""
                ),
                artworkModel = artist.artworkModel,
                icon = Icons.Rounded.Person,
                expanded = expandedGroupKey == artist.key,
                onToggle = { onExpandGroup(if (expandedGroupKey == artist.key) null else artist.key) },
                onPlay = { artist.tracks.firstOrNull()?.let { callbacks.onPlay(artist.tracks, it) } },
                tracks = artist.tracks,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                favoriteIds = favoriteIds,
                unavailableUris = unavailableUris,
                mediaByUri = library.catalog.mediaByUri,
                callbacks = callbacks
            )
        }
        LocalLibraryTab.Folders -> items(
            library.catalog.folders
                .filterLocalFolders(query, library.catalog.mediaByUri)
                .mapNotNull { it.filteredByLocalQuality(qualityFilter, library.catalog.mediaByUri) },
            key = { "local-folder-${it.key}" },
            contentType = { "local-group" }
        ) { folder ->
            LocalGroupBlock(
                title = folder.name.ifBlank { folder.volumeName },
                subtitle = localGroupSubtitle(folder.path, folder.tracks.size, ""),
                artworkModel = folder.tracks.firstOrNull()?.thumbnailUrl.orEmpty(),
                icon = Icons.Rounded.Folder,
                expanded = expandedGroupKey == folder.key,
                onToggle = { onExpandGroup(if (expandedGroupKey == folder.key) null else folder.key) },
                onPlay = { folder.tracks.firstOrNull()?.let { callbacks.onPlay(folder.tracks, it) } },
                tracks = folder.tracks,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                favoriteIds = favoriteIds,
                unavailableUris = unavailableUris,
                mediaByUri = library.catalog.mediaByUri,
                callbacks = callbacks,
                onHide = { callbacks.onToggleFolderHidden(folder.key, true) }
            )
        }
    }
    if (library.catalog.hiddenDuplicateCount > 0) {
        item(key = "local-duplicates", contentType = "local-note") {
            Text(
                text = "${LocalLevyraStrings.current.localDuplicatesHidden} · ${library.catalog.hiddenDuplicateCount}",
                color = LevyraMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

private fun LazyListScope.localTrackItems(
    keyPrefix: String,
    tracks: List<Track>,
    context: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    unavailableUris: Set<String>,
    mediaByUri: Map<String, LocalMediaEntity>,
    callbacks: LocalLibraryCallbacks
) {
    items(tracks, key = { "$keyPrefix-${it.streamUrl}" }, contentType = { "local-track" }) { track ->
        LibraryTrackRow(
            track = track,
            selected = false,
            selectionActive = false,
            isCurrent = currentTrack?.streamUrl == track.streamUrl,
            isPlaying = isPlaying && currentTrack?.streamUrl == track.streamUrl,
            isFavorite = track.id in favoriteIds,
            isDownloaded = true,
            downloadProgress = null,
            metadata = if (track.streamUrl in unavailableUris) {
                LocalLevyraStrings.current.localFileUnavailable
            } else {
                mediaByUri[track.streamUrl]?.localAudioSummary()
            },
            onClick = { callbacks.onPlay(context, track) },
            onLongClick = { callbacks.onAddToQueue(listOf(track)) },
            onFavorite = { callbacks.onToggleFavorite(track) },
            onDownload = {},
            onQueue = { callbacks.onAddToQueue(listOf(track)) },
            onEditTags = mediaByUri[track.streamUrl]
                ?.takeIf { it.isSafeTagEditorFormat() }
                ?.let { { callbacks.onEditTags(track) } }
        )
    }
}

@Composable
private fun LocalLibraryTabs(
    selected: LocalLibraryTab,
    onSelect: (LocalLibraryTab) -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LocalLibraryTab.entries.forEach { entry ->
            val active = entry == selected
            Column(
                modifier = Modifier.clickable { onSelect(entry) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entry.label(strings),
                    color = if (active) LevyraText else LevyraMuted,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 2.dp)
                        .background(
                            if (active) LevyraCyan else Color.Transparent,
                            RoundedCornerShape(99.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun LocalLibraryQualityFilters(
    filters: List<LocalLibraryQualityFilter>,
    selected: LocalLibraryQualityFilter,
    onSelect: (LocalLibraryQualityFilter) -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { filter ->
            val active = filter == selected
            val label = when (filter) {
                LocalLibraryQualityFilter.All -> strings.all
                LocalLibraryQualityFilter.Lossless -> "LOSSLESS"
                LocalLibraryQualityFilter.HighBitrate -> "320K+"
                LocalLibraryQualityFilter.Recent -> strings.localLibraryRecentFilterLabel()
            }
            Surface(
                color = if (active) LevyraCyan.copy(alpha = 0.14f) else Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    if (active) LevyraCyan.copy(alpha = 0.48f) else Color.White.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable { onSelect(filter) }
            ) {
                Text(
                    text = label,
                    color = if (active) LevyraCyan else LevyraMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

internal fun localLibraryQualityFilters(
    media: Collection<LocalMediaEntity>,
    nowMs: Long
): List<LocalLibraryQualityFilter> = buildList {
    add(LocalLibraryQualityFilter.All)
    if (media.any(LocalMediaEntity::isLosslessLocalMedia)) add(LocalLibraryQualityFilter.Lossless)
    if (media.any { it.bitrate >= LOCAL_HIGH_BITRATE_BPS }) add(LocalLibraryQualityFilter.HighBitrate)
    if (media.any { it.dateAddedMs >= nowMs - LOCAL_RECENT_WINDOW_MS }) add(LocalLibraryQualityFilter.Recent)
}

internal fun LocalMediaEntity.matchesLocalQualityFilter(
    filter: LocalLibraryQualityFilter,
    nowMs: Long = System.currentTimeMillis()
): Boolean = when (filter) {
    LocalLibraryQualityFilter.All -> true
    LocalLibraryQualityFilter.Lossless -> isLosslessLocalMedia()
    LocalLibraryQualityFilter.HighBitrate -> bitrate >= LOCAL_HIGH_BITRATE_BPS
    LocalLibraryQualityFilter.Recent -> dateAddedMs >= nowMs - LOCAL_RECENT_WINDOW_MS
}

private fun LocalMediaEntity.isLosslessLocalMedia(): Boolean {
    val mime = mimeType.lowercase()
    val extension = displayName.substringAfterLast('.', "").lowercase()
    return mime.contains("flac") ||
        mime.contains("alac") ||
        mime.contains("wav") ||
        mime.contains("aiff") ||
        mime.contains("ape") ||
        extension in LOCAL_LOSSLESS_EXTENSIONS
}

private fun List<Track>.filterByLocalQuality(
    filter: LocalLibraryQualityFilter,
    mediaByUri: Map<String, LocalMediaEntity>
): List<Track> {
    if (filter == LocalLibraryQualityFilter.All) return this
    val nowMs = System.currentTimeMillis()
    return filter { track -> mediaByUri[track.streamUrl]?.matchesLocalQualityFilter(filter, nowMs) == true }
}

private fun LocalAlbumGroup.filteredByLocalQuality(
    filter: LocalLibraryQualityFilter,
    mediaByUri: Map<String, LocalMediaEntity>
): LocalAlbumGroup? {
    val filtered = tracks.filterByLocalQuality(filter, mediaByUri)
    if (filtered.isEmpty()) return null
    return copy(
        artworkModel = filtered.firstOrNull()?.thumbnailUrl.orEmpty(),
        durationMs = filtered.sumOf { it.durationMs.coerceAtLeast(0L) },
        tracks = filtered
    )
}

private fun LocalArtistGroup.filteredByLocalQuality(
    filter: LocalLibraryQualityFilter,
    mediaByUri: Map<String, LocalMediaEntity>
): LocalArtistGroup? {
    val filtered = tracks.filterByLocalQuality(filter, mediaByUri)
    if (filtered.isEmpty()) return null
    return copy(
        artworkModel = filtered.firstOrNull()?.thumbnailUrl.orEmpty(),
        albumCount = filtered.map { it.album.trim().lowercase() }.filter(String::isNotEmpty).distinct().size,
        tracks = filtered
    )
}

private fun LocalFolderGroup.filteredByLocalQuality(
    filter: LocalLibraryQualityFilter,
    mediaByUri: Map<String, LocalMediaEntity>
): LocalFolderGroup? {
    val filtered = tracks.filterByLocalQuality(filter, mediaByUri)
    if (filtered.isEmpty()) return null
    return copy(
        durationMs = filtered.sumOf { it.durationMs.coerceAtLeast(0L) },
        tracks = filtered
    )
}

private fun LocalMediaEntity.localAudioSummary(): String? {
    val extension = displayName.substringAfterLast('.', "").uppercase()
    val format = when {
        extension.isNotBlank() -> extension
        mimeType.contains("mpeg", ignoreCase = true) -> "MP3"
        mimeType.contains("flac", ignoreCase = true) -> "FLAC"
        mimeType.contains("wav", ignoreCase = true) -> "WAV"
        mimeType.contains("ogg", ignoreCase = true) -> "OGG"
        mimeType.contains("opus", ignoreCase = true) -> "OPUS"
        else -> mimeType.substringAfter('/').substringBefore(';').uppercase()
    }
    val bitrateLabel = bitrate
        .takeIf { it > 0 }
        ?.div(1_000)
        ?.let { "$it kbps" }
    return listOf(format, bitrateLabel)
        .filterNotNull()
        .filter(String::isNotBlank)
        .joinToString(" · ")
        .takeIf(String::isNotBlank)
}

private const val LOCAL_HIGH_BITRATE_BPS = 320_000
private const val LOCAL_RECENT_WINDOW_MS = 30L * 24L * 60L * 60L * 1_000L
private val LOCAL_LOSSLESS_EXTENSIONS = setOf("flac", "alac", "wav", "wave", "aiff", "aif", "ape")

@Composable
private fun LocalLibraryActions(library: LocalLibraryUiState, callbacks: LocalLibraryCallbacks) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = LevyraGlass,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.localOnDevice, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(
                        text = library.message.ifBlank { strings.localOnDeviceSubtitle },
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (library.scanning) {
                    CircularProgressIndicator(color = LevyraCyan, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    IconButton(onClick = callbacks.onQuickScan) {
                        Icon(Icons.Rounded.Refresh, strings.localScanQuick, tint = LevyraCyan)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LibraryCategoryChip(strings.localScanFull, selected = false, onClick = callbacks.onFullScan)
                LibraryCategoryChip(strings.localRebuildLevyra, selected = false, onClick = callbacks.onRebuildLevyra)
            }
        }
    }
}

@Composable
private fun LocalLibraryPermissionCard(onGrant: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = LevyraPanelSoft,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onGrant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    strings.localPermissionRequired,
                    color = LevyraText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(strings.localGrantPermission, color = LevyraCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LocalGroupBlock(
    title: String,
    subtitle: String,
    artworkModel: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPlay: () -> Unit,
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    unavailableUris: Set<String>,
    mediaByUri: Map<String, LocalMediaEntity>,
    callbacks: LocalLibraryCallbacks,
    onHide: (() -> Unit)? = null
) {
    val strings = LocalLevyraStrings.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            color = if (expanded) LevyraCyan.copy(alpha = 0.08f) else Color.Transparent,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (artworkModel.isNotBlank()) {
                    LibraryArtwork(
                        url = artworkModel,
                        title = title,
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        selected = false
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(LevyraPanelSoft, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = LevyraMuted, modifier = Modifier.size(24.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = LevyraText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (onHide != null) {
                    IconButton(onClick = onHide, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            strings.localHideFolder,
                            tint = LevyraMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = onPlay, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.PlayArrow, strings.playAll, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (expanded) {
            tracks.forEach { track ->
                LibraryTrackRow(
                    track = track,
                    selected = false,
                    selectionActive = false,
                    isCurrent = currentTrack?.streamUrl == track.streamUrl,
                    isPlaying = isPlaying && currentTrack?.streamUrl == track.streamUrl,
                    isFavorite = track.id in favoriteIds,
                    isDownloaded = true,
                    downloadProgress = null,
                    metadata = if (track.streamUrl in unavailableUris) strings.localFileUnavailable else null,
                    onClick = { callbacks.onPlay(tracks, track) },
                    onLongClick = { callbacks.onAddToQueue(listOf(track)) },
                    onFavorite = { callbacks.onToggleFavorite(track) },
                    onDownload = {},
                    onQueue = { callbacks.onAddToQueue(listOf(track)) },
                    onEditTags = mediaByUri[track.streamUrl]
                        ?.takeIf { it.isSafeTagEditorFormat() }
                        ?.let { { callbacks.onEditTags(track) } },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

private fun localGroupSubtitle(detail: String, trackCount: Int, year: String): String = listOf(
    detail.trim(),
    "$trackCount",
    year.trim()
).filter { it.isNotEmpty() }.joinToString(" · ")

internal fun List<Track>.filterLocalTracks(
    query: String,
    mediaByUri: Map<String, LocalMediaEntity>
): List<Track> {
    val clean = normalizeLibraryText(query)
    if (clean.isEmpty()) return this
    return filter { track ->
        mediaByUri[track.streamUrl]?.matchesFullTagQuery(query) == true ||
            normalizeLibraryText(track.title).contains(clean) ||
            normalizeLibraryText(track.artist).contains(clean) ||
            normalizeLibraryText(track.album).contains(clean)
    }
}

internal fun List<LocalAlbumGroup>.filterLocalAlbums(
    query: String,
    mediaByUri: Map<String, LocalMediaEntity>
): List<LocalAlbumGroup> {
    val clean = normalizeLibraryText(query)
    if (clean.isEmpty()) return this
    return filter { group ->
        normalizeLibraryText(group.title).contains(clean) ||
            normalizeLibraryText(group.artist).contains(clean) ||
            group.tracks.any { mediaByUri[it.streamUrl]?.matchesFullTagQuery(query) == true }
    }
}

internal fun List<LocalArtistGroup>.filterLocalArtists(
    query: String,
    mediaByUri: Map<String, LocalMediaEntity>
): List<LocalArtistGroup> {
    val clean = normalizeLibraryText(query)
    if (clean.isEmpty()) return this
    return filter { group ->
        normalizeLibraryText(group.name).contains(clean) ||
            group.tracks.any { mediaByUri[it.streamUrl]?.matchesFullTagQuery(query) == true }
    }
}

internal fun List<LocalFolderGroup>.filterLocalFolders(
    query: String,
    mediaByUri: Map<String, LocalMediaEntity>
): List<LocalFolderGroup> {
    val clean = normalizeLibraryText(query)
    if (clean.isEmpty()) return this
    return filter { group ->
        normalizeLibraryText(group.name).contains(clean) ||
            normalizeLibraryText(group.path).contains(clean) ||
            group.tracks.any { mediaByUri[it.streamUrl]?.matchesFullTagQuery(query) == true }
    }
}
