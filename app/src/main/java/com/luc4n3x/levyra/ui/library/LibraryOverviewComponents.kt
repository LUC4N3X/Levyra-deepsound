package com.luc4n3x.levyra.ui.library

import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.ui.components.LevyraFilterPill
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSectionAction
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.selection.TrackSelectionActions
import com.luc4n3x.levyra.ui.selection.TrackSelectionBar
import com.luc4n3x.levyra.ui.selection.rememberTrackSelectionState
import com.luc4n3x.levyra.ui.selection.resolveSelected
import com.luc4n3x.levyra.ui.selection.trackSelectionKey
import com.luc4n3x.levyra.ui.theme.LevyraGlass
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LibraryViewModel
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraType

internal val LibraryPillShape = RoundedCornerShape(999.dp)

@Composable
internal fun LibraryHero(title: String, subtitle: String, onBack: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val strings = LocalLevyraStrings.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = if (onBack == null) 8.dp else 0.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (onBack != null) {
            LevyraBackButton(onClick = onBack, contentDescription = strings.back, modifier = Modifier.padding(bottom = 4.dp))
        }
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
    recent: List<Track>,
    mostPlayed: List<Track>,
    onOpenCollection: (String) -> Unit,
    onPlayTracks: (List<Track>) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val shortcuts = listOf(
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

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        LibraryFavoritesSpotlight(
            tracks = favorites,
            shape = LibrarySpotlightShape,
            onOpen = { onOpenCollection(SMART_COLLECTION_FAVORITES) },
            onPlay = { onPlayTracks(favorites) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            shortcuts.forEachIndexed { columnIndex, card ->
                SmartCollectionShortcut(
                    card = card,
                    shape = libraryShortcutShape(columnIndex),
                    modifier = Modifier.weight(1f)
                )
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
            accent = MaterialTheme.colorScheme.primary,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SmartCollectionHero(style: SmartCollectionStyle, countLabel: String, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val strings = LocalLevyraStrings.current
    val badgeShape = MaterialShapes.Cookie9Sided.toShape()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LevyraCardDesign.SurfaceShape)
            .background(Brush.verticalGradient(listOf(style.accent.copy(alpha = 0.24f), colors.surfaceContainer)))
            .padding(start = 12.dp, end = 20.dp, top = 12.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LevyraBackButton(onClick = onClose, contentDescription = strings.back)
        Row(
            modifier = Modifier.padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(76.dp).clip(badgeShape).background(style.accent.copy(alpha = 0.26f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(style.icon, contentDescription = null, tint = style.accent, modifier = Modifier.size(34.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    style.title,
                    color = colors.onSurface,
                    style = LevyraType.screenTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Text(countLabel, color = colors.onSurfaceVariant, style = LevyraType.metadata, maxLines = 1)
            }
        }
        if (style.subtitle.isNotBlank()) {
            Text(
                style.subtitle,
                color = colors.onSurfaceVariant,
                style = LevyraType.caption,
                modifier = Modifier.padding(start = 8.dp)
            )
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
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = 26.dp,
        pressed = 12.dp,
        label = "smart-collection-action-corner"
    )
    val shape = RoundedCornerShape(corner)
    val container = if (filled) colors.primary else colors.secondaryContainer
    val content = if (filled) colors.onPrimary else colors.onSecondaryContainer
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(if (enabled) container else container.copy(alpha = 0.38f))
            .levyraPressable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interaction,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) content else accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = content, style = LevyraType.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
