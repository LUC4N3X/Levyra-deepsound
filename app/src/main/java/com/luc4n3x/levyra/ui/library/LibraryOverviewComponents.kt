package com.luc4n3x.levyra.ui.library

import com.luc4n3x.levyra.ui.components.levyraGroupedGridShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.luc4n3x.levyra.domain.LibrarySort
import com.luc4n3x.levyra.domain.LibrarySortDirection
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.ui.components.LevyraFilterPill
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionAction
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.selection.TrackSelectionActions
import com.luc4n3x.levyra.ui.selection.TrackSelectionBar
import com.luc4n3x.levyra.ui.selection.rememberTrackSelectionState
import com.luc4n3x.levyra.ui.selection.resolveSelected
import com.luc4n3x.levyra.ui.selection.trackSelectionKey
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlass
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LibraryViewModel
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as DayTextStyle
import java.util.Locale
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraType
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.domain.ListeningChartProjection

internal val LibraryPillShape = RoundedCornerShape(999.dp)

@Composable
internal fun LibraryHero(title: String, subtitle: String) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = colors.onSurface, style = LevyraType.heroTitle, softWrap = true, modifier = Modifier.semantics { heading() })
        if (subtitle.isNotBlank()) {
            Text(subtitle, color = colors.onSurfaceVariant, style = LevyraType.metadata, softWrap = true)
        }
    }
}

@Composable
internal fun LibraryCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    LevyraFilterPill(label = label, selected = selected, onClick = onClick)
}

@Composable
internal fun LibraryToolbar(
    category: LibraryCategory,
    sort: LibrarySort,
    direction: LibrarySortDirection,
    layout: LibraryLayout,
    sortExpanded: Boolean,
    onSortExpanded: (Boolean) -> Unit,
    onSort: (LibrarySort, LibrarySortDirection) -> Unit,
    onToggleDirection: () -> Unit,
    onLayout: () -> Unit,
    onSelectAll: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    color = colors.surfaceContainerHigh,
                    shape = LevyraCardDesign.ArtworkShape,
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(LevyraCardDesign.ArtworkShape)
                        .clickable(onClick = { onSortExpanded(true) })
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = sort.libraryLabel(strings),
                            color = colors.onSurface,
                            style = LevyraType.caption,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { onSortExpanded(false) }) {
                    LibrarySort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.libraryLabel(strings)) },
                            leadingIcon = if (option == sort) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.primary) }
                            } else null,
                            onClick = {
                                if (option == sort) {
                                    onSort(option, direction.inverted)
                                } else {
                                    onSort(option, option.defaultDirection)
                                }
                                onSortExpanded(false)
                            }
                        )
                    }
                }
            }
            Surface(
                color = colors.surfaceContainerHigh,
                shape = LevyraCardDesign.ArtworkShape,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(LevyraCardDesign.ArtworkShape)
                    .clickable(onClick = onToggleDirection)
                    .semantics {
                        contentDescription = "${strings.librarySortDirection}: ${sort.directionLabel(direction, strings)}"
                    }
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (direction == LibrarySortDirection.Ascending) {
                            Icons.Rounded.ArrowUpward
                        } else {
                            Icons.Rounded.ArrowDownward
                        },
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSelectAll) {
                Icon(
                    Icons.Rounded.DoneAll,
                    contentDescription = strings.all,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (category != LibraryCategory.Offline && category != LibraryCategory.Songs) {
                IconButton(onClick = onLayout) {
                    Icon(
                        if (layout == LibraryLayout.List) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = strings.options,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun LibrarySectionTitle(
    title: String,
    detail: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    LevyraSectionHeader(
        title = title,
        subtitle = detail,
        titleColor = colors.onSurface,
        subtitleColor = colors.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp)
    ) {
        if (action != null && onAction != null) {
            LevyraSectionAction(
                label = action,
                onClick = onAction,
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight
            )
        }
    }
}

@Composable
internal fun SmartCollectionGrid(
    favorites: List<Track>,
    downloads: List<Track>,
    recent: List<Track>,
    mostPlayed: List<Track>,
    onOpenCollection: (String) -> Unit,
    onOpenOffline: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val cards = listOf(
        SmartCollection(
            id = SMART_COLLECTION_FAVORITES,
            title = strings.favoritesPlain,
            detail = strings.formatTrackCount(favorites.size),
            icon = Icons.Rounded.Favorite,
            accent = LevyraPink,
            tracks = favorites,
            onClick = { onOpenCollection(SMART_COLLECTION_FAVORITES) }
        ),
        SmartCollection(
            id = "offline",
            title = strings.offline,
            detail = strings.formatTrackCount(downloads.size),
            icon = Icons.Rounded.DownloadDone,
            accent = LevyraCyan,
            tracks = downloads,
            enabledWhenEmpty = true,
            onClick = onOpenOffline
        ),
        SmartCollection(
            id = SMART_COLLECTION_RECENT,
            title = strings.recent,
            detail = strings.formatTrackCount(recent.size),
            icon = Icons.Rounded.History,
            accent = LevyraViolet,
            tracks = recent,
            onClick = { onOpenCollection(SMART_COLLECTION_RECENT) }
        ),
        SmartCollection(
            id = SMART_COLLECTION_MOST_PLAYED,
            title = strings.pulsePlays,
            detail = strings.formatTrackCount(mostPlayed.size),
            icon = Icons.Rounded.Replay,
            accent = Color(0xFFFFC857),
            tracks = mostPlayed,
            onClick = { onOpenCollection(SMART_COLLECTION_MOST_PLAYED) }
        )
    )

    val rows = cards.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEachIndexed { columnIndex, card ->
                    SmartCollectionShortcut(
                        card = card,
                        shape = levyraGroupedGridShape(rowIndex, columnIndex, rows.size, 2),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private data class SmartCollection(
    val id: String,
    val title: String,
    val detail: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val tracks: List<Track>,
    val enabledWhenEmpty: Boolean = false,
    val onClick: () -> Unit
)

internal const val SMART_COLLECTION_FAVORITES = "favorites"
internal const val SMART_COLLECTION_RECENT = "recent"
internal const val SMART_COLLECTION_MOST_PLAYED = "mostPlayed"

private data class SmartCollectionStyle(
    val title: String,
    val subtitle: String,
    val accent: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun smartCollectionStyle(collectionId: String): SmartCollectionStyle {
    val strings = LocalLevyraStrings.current
    return when (collectionId) {
        SMART_COLLECTION_RECENT -> SmartCollectionStyle(
            title = strings.recent,
            subtitle = strings.listeningHistorySubtitle,
            accent = LevyraViolet,
            icon = Icons.Rounded.History
        )
        SMART_COLLECTION_MOST_PLAYED -> SmartCollectionStyle(
            title = strings.pulsePlays,
            subtitle = strings.pulseSubtitle,
            accent = Color(0xFFFFC857),
            icon = Icons.Rounded.Replay
        )
        else -> SmartCollectionStyle(
            title = strings.favoritesPlain,
            subtitle = strings.tapHeartToAdd,
            accent = LevyraPink,
            icon = Icons.Rounded.Favorite
        )
    }
}

@Composable
internal fun SmartCollectionDetail(
    collectionId: String,
    state: LevyraUiState,
    tracks: List<Track>,
    viewModel: LibraryViewModel,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val style = smartCollectionStyle(collectionId)
    val selection = rememberTrackSelectionState()
    var batchAddTargets by remember { mutableStateOf<List<Track>>(emptyList()) }
    val selectableIds = remember(tracks) { tracks.map(::trackSelectionKey) }
    val lazyItemKeys = remember(tracks) {
        val occurrences = HashMap<String, Int>()
        tracks.map { track ->
            val key = trackSelectionKey(track)
            val occurrence = (occurrences[key] ?: 0) + 1
            occurrences[key] = occurrence
            if (occurrence == 1) key else key + "#" + occurrence
        }
    }
    val selectedTracks = remember(tracks, selection.selectedIds) {
        selection.resolveSelected(tracks, ::trackSelectionKey)
    }
    LaunchedEffect(selectableIds) { selection.retainAvailable(selectableIds) }
    BackHandler(enabled = selection.isActive) { selection.exit() }

    Surface(color = com.luc4n3x.levyra.ui.theme.LevyraInk, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = if (state.currentTrack != null || selection.isActive) 230.dp else 116.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "smart-hero") {
                    SmartCollectionHero(style, strings.formatTrackCount(tracks.size), onClose)
                }
                item(key = "smart-actions") {
                    SmartCollectionActions(
                        accent = style.accent,
                        enabled = tracks.isNotEmpty() && !selection.isActive,
                        onPlay = { tracks.firstOrNull()?.let { viewModel.playFrom(tracks, it) } },
                        onShuffle = {
                            val shuffled = tracks.shuffled()
                            shuffled.firstOrNull()?.let { viewModel.playFrom(shuffled, it) }
                        }
                    )
                }
                if (tracks.isEmpty()) {
                    item(key = "smart-empty") { LibraryEmpty(style.icon, strings.emptySearchPrompt) }
                } else {
                    items(
                        count = tracks.size,
                        key = { index -> "smart-" + collectionId + "-" + lazyItemKeys[index] }
                    ) { index ->
                        val track = tracks[index]
                        val key = trackSelectionKey(track)
                        SmartCollectionTrackRow(
                            state = state,
                            tracks = tracks,
                            track = track,
                            viewModel = viewModel,
                            selected = selection.isSelected(key),
                            selectionActive = selection.isActive,
                            onClick = {
                                if (selection.isActive) selection.toggle(key) else viewModel.playFrom(tracks, track)
                            },
                            onLongClick = { selection.start(key) }
                        )
                    }
                }
            }

            TrackSelectionBar(
                state = selection,
                allVisibleIds = selectableIds,
                actions = TrackSelectionActions(
                    onPlayNext = selectedTracks.takeIf { it.isNotEmpty() }?.let {
                        {
                            viewModel.playTracksNext(selectedTracks)
                            selection.exit()
                        }
                    },
                    onAddToQueue = selectedTracks.takeIf { it.isNotEmpty() }?.let {
                        {
                            viewModel.addTracksToQueue(selectedTracks)
                            selection.exit()
                        }
                    },
                    onAddToPlaylist = selectedTracks
                        .filter { it.id.isNotBlank() }
                        .takeIf { it.isNotEmpty() }
                        ?.let { playlistTracks -> { batchAddTargets = playlistTracks } },
                    onFavorite = selectedTracks.takeIf { it.isNotEmpty() }?.let {
                        {
                            viewModel.toggleFavorites(selectedTracks)
                            selection.exit()
                        }
                    },
                    onDownload = selectedTracks.takeIf { it.isNotEmpty() }?.let {
                        {
                            viewModel.exportTracks(selectedTracks, strings.offline)
                            selection.exit()
                        }
                    }
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = if (state.currentTrack != null) 84.dp else 12.dp
                    )
            )

            if (batchAddTargets.isNotEmpty()) {
                AddTracksToPlaylistDialog(
                    tracks = batchAddTargets,
                    playlists = state.playlists,
                    onDismiss = { batchAddTargets = emptyList() },
                    onAdd = { playlistId ->
                        viewModel.addTracksToPlaylist(playlistId, batchAddTargets)
                        batchAddTargets = emptyList()
                        selection.exit()
                    },
                    onCreate = { name ->
                        viewModel.createPlaylistWithTracks(name, batchAddTargets)
                        batchAddTargets = emptyList()
                        selection.exit()
                    }
                )
            }
        }
    }
}

@Composable
private fun SmartCollectionHero(style: SmartCollectionStyle, countLabel: String, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(style.accent.copy(alpha = 0.30f), LevyraPanel.copy(alpha = 0.92f))
                )
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(end = 40.dp)
        ) {
            Surface(shape = RoundedCornerShape(18.dp), color = style.accent.copy(alpha = 0.22f)) {
                Icon(
                    style.icon,
                    contentDescription = null,
                    tint = style.accent,
                    modifier = Modifier.padding(13.dp).size(28.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(style.title, color = LevyraText, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text(style.subtitle, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(countLabel, color = style.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.Rounded.Close, contentDescription = null, tint = LevyraMuted)
        }
    }
}

@Composable
private fun SmartCollectionActions(
    accent: Color,
    enabled: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SmartCollectionAction(
            label = strings.play,
            icon = Icons.Rounded.PlayArrow,
            accent = accent,
            filled = true,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = onPlay
        )
        SmartCollectionAction(
            label = strings.shuffle,
            icon = Icons.Rounded.Shuffle,
            accent = accent,
            filled = false,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = onShuffle
        )
    }
}

@Composable
private fun SmartCollectionTrackRow(
    state: LevyraUiState,
    tracks: List<Track>,
    track: Track,
    viewModel: LibraryViewModel,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    LibraryTrackRow(
        track = track,
        selected = selected,
        selectionActive = selectionActive,
        isCurrent = track.id == state.currentTrack?.id,
        isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
        isFavorite = track.id in state.favoriteIds,
        isDownloaded = libraryDownloadForTrack(track, state.downloads) != null,
        downloadProgress = downloadProgressFor(track, state),
        onClick = onClick,
        onLongClick = onLongClick,
        onFavorite = { viewModel.toggleFavorite(track) },
        onDownload = { viewModel.exportTrack(track) }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SmartCollectionAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    filled: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (filled) accent.copy(alpha = if (enabled) 0.92f else 0.35f) else LevyraPanel.copy(alpha = 0.85f),
        shape = RoundedCornerShape(18.dp),
        border = if (filled) null else BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier.height(46.dp).clip(RoundedCornerShape(18.dp)).combinedClickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (filled) Color.Black else accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = if (filled) Color.Black else LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SmartCollectionShortcut(card: SmartCollection, shape: Shape, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val artworkUrl = remember(card.tracks) {
        card.tracks.asSequence()
            .map { track -> track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } }
            .firstOrNull(String::isNotBlank)
            .orEmpty()
    }
    val enabled = card.tracks.isNotEmpty() || card.enabledWhenEmpty
    Row(
        modifier = modifier
            .height(SmartCollectionShortcutHeight)
            .clip(shape)
            .background(colors.surfaceContainerHigh.copy(alpha = 0.62f))
            .semantics(mergeDescendants = true) {}
            .levyraPressable(onClick = card.onClick, enabled = enabled, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(if (artworkUrl.isNotBlank()) LevyraCardDesign.ArtworkShape else MaterialShapes.Cookie4Sided.toShape())
                .background(card.accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            if (artworkUrl.isNotBlank()) {
                AsyncImage(model = artworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            } else {
                Icon(card.icon, contentDescription = null, tint = card.accent, modifier = Modifier.size(22.dp))
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            BasicText(
                text = card.title,
                style = LevyraType.cardTitle.copy(color = colors.onSurface),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = LevyraType.cardTitle.fontSize)
            )
            Text(card.detail, color = colors.onSurfaceVariant, style = LevyraType.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val SmartCollectionShortcutHeight = 64.dp

@Composable
internal fun LibraryListeningDashboard(
    pulse: ListeningPulse,
    onOpenInsights: (() -> Unit)? = null
) {
    val strings = LocalLevyraStrings.current
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }
    val weekMinutes = pulse.week.takeLast(7).sumOf { it.listenedMs } / 60_000L
    val peakHour = if (pulse.peakHour in 0..23) {
        LocalTime.of(pulse.peakHour, 0).format(timeFormatter)
    } else {
        "—"
    }
    val topArtist = pulse.topArtists.firstOrNull()?.name.orEmpty().ifBlank { "—" }

    val openSurface = if (onOpenInsights != null) {
        Modifier.levyraPressable(
            onClick = onOpenInsights,
            pressedScale = LevyraPressScale.Surface,
            role = Role.Button,
            onClickLabel = strings.listeningInsights
        )
    } else {
        Modifier
    }

    Surface(
        modifier = Modifier.fillMaxWidth().then(openSurface),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = LevyraCardDesign.SurfaceShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${number.format(weekMinutes)} ${strings.pulseMinuteShort}",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = LevyraType.heroTitle
                        )
                        Text(
                            text = strings.pulseWeek,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = LevyraType.metadata
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                LibraryWeekChart(
                    pulse = pulse,
                    locale = locale,
                    durationLabel = { strings.formatLibraryDuration(it) }
                )

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PulsePreviewInsight(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.LocalFireDepartment,
                        label = strings.pulseStreak,
                        value = number.format(pulse.streakDays),
                        accent = LevyraViolet
                    )
                    PulsePreviewInsight(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.Schedule,
                        label = strings.pulsePeakHour,
                        value = peakHour,
                        accent = LevyraCyan
                    )
                    PulsePreviewInsight(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.Person,
                        label = strings.pulseTopArtists,
                        value = topArtist,
                        accent = LevyraPink
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsePreviewInsight(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = accent.copy(alpha = 0.12f), shape = LevyraCardDesign.ThumbShape) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(10.dp).size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = LevyraType.caption)
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = LevyraType.contentTitle, softWrap = true)
        }
    }
}

@Composable
private fun ReplayPeriodMetric(
    modifier: Modifier,
    periodLabel: String,
    minutes: Long,
    plays: Int,
    minuteLabel: String,
    playLabel: String,
    accent: Color,
    number: NumberFormat
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.025f),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(periodLabel, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${number.format(minutes)} $minuteLabel", color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text("${number.format(plays)} $playLabel", color = LevyraMuted, fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun LibraryInsightMetric(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    accent: Color
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                Text(
                    text = label,
                    color = LevyraMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                color = LevyraText,
                fontSize = 17.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(17.sp),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val LibraryRingPalette = listOf(
    LevyraCyan,
    LevyraViolet,
    LevyraPink,
    Color(0xFFFFC857),
    Color(0xFF5AD8A6)
)

private fun peakHourLabel(prefix: String, hour: Int): String =
    prefix + " · " + hour.toString().padStart(2, '0') + ":00"

@Composable
private fun LibraryWeekChart(
    pulse: ListeningPulse,
    locale: Locale,
    durationLabel: (Long) -> String
) {
    val week = remember(pulse.week) { pulse.week.takeLast(7) }
    val fractions = remember(week) { ListeningChartProjection.weekFractions(week, 1L) }
    val today = week.lastIndex
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val colors = MaterialTheme.colorScheme
    val gridColor = colors.outlineVariant.copy(alpha = 0.30f)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (week.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                Text(
                    week.first().date.format(dateFormatter) + " · " + week.last().date.format(dateFormatter),
                    color = colors.onSurfaceVariant, style = LevyraType.caption,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    durationLabel(week.maxOf { it.listenedMs }),
                    color = colors.onSurfaceVariant, style = LevyraType.caption, textAlign = TextAlign.End
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(112.dp).drawBehind {
                for (line in 0..2) {
                    val y = size.height * line / 2f
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
            },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (week.isEmpty()) {
                repeat(7) {
                    Box(Modifier.weight(1f).height(4.dp).clip(LevyraCardDesign.ThumbShape).background(colors.surfaceContainerHighest))
                }
            } else {
                week.forEachIndexed { index, day ->
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().semantics {
                            contentDescription = day.date.toString() + " · " +
                                day.date.dayOfWeek.getDisplayName(DayTextStyle.FULL, locale) + " · " + durationLabel(day.listenedMs)
                        },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            Modifier.width(18.dp).fillMaxHeight(fractions[index].coerceAtLeast(0.025f))
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (day.listenedMs == 0L) colors.outlineVariant
                                    else if (index == today) colors.primary
                                    else colors.secondary.copy(alpha = 0.82f)
                                )
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            week.forEachIndexed { index, day ->
                Column(
                    Modifier.weight(1f).clip(LevyraCardDesign.ThumbShape)
                        .background(if (index == today) colors.primaryContainer else Color.Transparent).padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        day.date.dayOfWeek.getDisplayName(DayTextStyle.NARROW, locale),
                        color = if (index == today) colors.onPrimaryContainer else colors.onSurfaceVariant,
                        style = LevyraType.caption, textAlign = TextAlign.Center
                    )
                    Text(
                        day.date.dayOfMonth.toString(), color = if (index == today) colors.onPrimaryContainer else colors.onSurfaceVariant,
                        style = LevyraType.caption, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
