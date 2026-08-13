package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LibraryViewModel
import java.time.format.TextStyle as DayTextStyle
import java.util.Locale

@Composable
internal fun LibraryHero(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = LevyraText,
                fontSize = 32.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.8).sp
            )
            Text(
                text = subtitle,
                color = LevyraMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(14.dp))
        Surface(
            color = LevyraPanel.copy(alpha = 0.82f),
            shape = CircleShape,
            border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.22f))
        ) {
            Icon(
                imageVector = Icons.Rounded.LibraryMusic,
                contentDescription = null,
                tint = LevyraCyan,
                modifier = Modifier.padding(12.dp).size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) LevyraViolet.copy(alpha = 0.28f) else LevyraPanel.copy(alpha = 0.50f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            1.dp,
            if (selected) LevyraViolet.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.09f)
        ),
        modifier = Modifier.height(38.dp).combinedClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = LevyraText, modifier = Modifier.size(15.dp))
            }
            Text(
                text = label,
                color = if (selected) LevyraText else LevyraMuted,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun LibraryToolbar(
    category: LibraryCategory,
    sort: LibrarySort,
    layout: LibraryLayout,
    sortExpanded: Boolean,
    onSortExpanded: (Boolean) -> Unit,
    onSort: (LibrarySort) -> Unit,
    onLayout: () -> Unit,
    onSelectAll: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box {
            TextButton(
                onClick = { onSortExpanded(true) },
                colors = ButtonDefaults.textButtonColors(contentColor = LevyraText),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Sort,
                    contentDescription = null,
                    tint = LevyraCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(sort.libraryLabel(strings), fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            DropdownMenu(expanded = sortExpanded, onDismissRequest = { onSortExpanded(false) }) {
                LibrarySort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.libraryLabel(strings)) },
                        leadingIcon = if (option == sort) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, tint = LevyraCyan) }
                        } else null,
                        onClick = {
                            onSort(option)
                            onSortExpanded(false)
                        }
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onSelectAll,
                colors = ButtonDefaults.textButtonColors(contentColor = LevyraText),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Rounded.DoneAll, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.all, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            if (category != LibraryCategory.Offline && category != LibraryCategory.Songs) {
                IconButton(onClick = onLayout) {
                    Icon(
                        if (layout == LibraryLayout.List) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = null,
                        tint = LevyraMuted
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LevyraText, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(detail, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, color = LevyraCyan, fontWeight = FontWeight.Bold)
            }
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
            title = strings.favoritesPlain,
            detail = strings.formatTrackCount(favorites.size),
            icon = Icons.Rounded.Favorite,
            accent = LevyraPink,
            tracks = favorites,
            onClick = { onOpenCollection(SMART_COLLECTION_FAVORITES) }
        ),
        SmartCollection(
            title = strings.offline,
            detail = strings.formatTrackCount(downloads.size),
            icon = Icons.Rounded.DownloadDone,
            accent = LevyraCyan,
            tracks = downloads,
            enabledWhenEmpty = true,
            onClick = onOpenOffline
        ),
        SmartCollection(
            title = strings.recent,
            detail = strings.formatTrackCount(recent.size),
            icon = Icons.Rounded.History,
            accent = LevyraViolet,
            tracks = recent,
            onClick = { onOpenCollection(SMART_COLLECTION_RECENT) }
        ),
        SmartCollection(
            title = strings.pulsePlays,
            detail = strings.formatTrackCount(mostPlayed.size),
            icon = Icons.Rounded.Replay,
            accent = Color(0xFFFFC857),
            tracks = mostPlayed,
            onClick = { onOpenCollection(SMART_COLLECTION_MOST_PLAYED) }
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cards.chunked(2).forEach { rowCards ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowCards.forEach { card -> SmartCollectionCard(card, Modifier.weight(1f)) }
                if (rowCards.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class SmartCollection(
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
    Surface(color = com.luc4n3x.levyra.ui.theme.LevyraInk, modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = if (state.currentTrack != null) 230.dp else 116.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "smart-hero") {
                SmartCollectionHero(style, strings.formatTrackCount(tracks.size), onClose)
            }
            item(key = "smart-actions") {
                SmartCollectionActions(
                    accent = style.accent,
                    enabled = tracks.isNotEmpty(),
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
                items(tracks.size, key = { index -> "smart-$collectionId-${libraryTrackKey(tracks[index])}" }) { index ->
                    SmartCollectionTrackRow(state, tracks, tracks[index], viewModel)
                }
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
            Icon(androidx.compose.material.icons.rounded.Close, contentDescription = null, tint = LevyraMuted)
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
    viewModel: LibraryViewModel
) {
    LibraryTrackRow(
        track = track,
        selected = false,
        selectionActive = false,
        isCurrent = track.id == state.currentTrack?.id,
        isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
        isFavorite = track.id in state.favoriteIds,
        isDownloaded = libraryDownloadForTrack(track, state.downloads) != null,
        downloadProgress = downloadProgressFor(track, state),
        onClick = { viewModel.playFrom(tracks, track) },
        onLongClick = {},
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SmartCollectionCard(card: SmartCollection, modifier: Modifier = Modifier) {
    val artworkUrl = card.tracks.firstOrNull()?.let { track ->
        track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
    }.orEmpty()

    Surface(
        color = LevyraPanel.copy(alpha = 0.88f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, card.accent.copy(alpha = 0.20f)),
        modifier = modifier.height(96.dp).combinedClickable(
            enabled = card.tracks.isNotEmpty() || card.enabledWhenEmpty,
            onClick = card.onClick
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(card.accent.copy(alpha = 0.16f), LevyraPanel.copy(alpha = 0.88f)))
            )
        ) {
            if (artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.align(Alignment.CenterEnd).width(66.dp).fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp))
                )
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd).width(78.dp).fillMaxHeight().background(
                        Brush.horizontalGradient(listOf(LevyraPanel.copy(alpha = 0.96f), Color.Transparent))
                    )
                )
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(color = card.accent.copy(alpha = 0.15f), shape = CircleShape) {
                    Icon(
                        imageVector = card.icon,
                        contentDescription = null,
                        tint = card.accent,
                        modifier = Modifier.padding(7.dp).size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = card.title,
                        color = LevyraText,
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = card.detail,
                        color = LevyraMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun LibraryListeningDashboard(
    pulse: ListeningPulse,
    artistCount: Int,
    trackCount: Int,
    playlistCount: Int,
    offlineCount: Int
) {
    val strings = LocalLevyraStrings.current
    val week = pulse.week.takeLast(7)
    val weekMinutes = week.sumOf { it.listenedMs } / 60_000L
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LevyraPanel.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        shape = RoundedCornerShape(26.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(
                    listOf(
                        LevyraViolet.copy(alpha = 0.18f),
                        LevyraPanel.copy(alpha = 0.96f),
                        LevyraCyan.copy(alpha = 0.10f)
                    )
                )
            ).padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(17.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Insights, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                            Text(strings.pulseTitle, color = LevyraText, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                        Text(strings.pulseSubtitle, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatListeningTime(pulse.totalListenMs),
                            color = LevyraText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        Text(strings.pulseMinutes, color = LevyraMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LibraryInsightMetric(Modifier.weight(1f), Icons.Rounded.Person, artistCount.toString(), strings.statArtists, LevyraViolet)
                        LibraryInsightMetric(Modifier.weight(1f), Icons.Rounded.MusicNote, trackCount.toString(), strings.statTracks, LevyraPink)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LibraryInsightMetric(Modifier.weight(1f), Icons.Rounded.LibraryMusic, playlistCount.toString(), strings.playlists, LevyraCyan)
                        LibraryInsightMetric(Modifier.weight(1f), Icons.Rounded.OfflinePin, offlineCount.toString(), strings.offline, Color(0xFFFFC857))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.pulseWeek, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("$weekMinutes ${strings.pulseMinuteShort}", color = LevyraCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    LibraryWeekChart(pulse = pulse, locale = locale)
                }

                if (pulse.topArtists.isNotEmpty() || pulse.peakHour >= 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (pulse.topArtists.isNotEmpty()) {
                            Text(strings.pulseTopArtists, color = LevyraMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                pulse.topArtists.take(4).forEach { artist ->
                                    Surface(
                                        color = LevyraViolet.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, LevyraViolet.copy(alpha = 0.20f)),
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            text = artist.name,
                                            color = LevyraText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (pulse.peakHour >= 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = LevyraMuted, modifier = Modifier.size(15.dp))
                                Text(
                                    text = "${strings.pulsePeakHour} · ${pulse.peakHour.toString().padStart(2, '0')}:00",
                                    color = LevyraMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
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
        color = Color.Black.copy(alpha = 0.18f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(color = accent.copy(alpha = 0.14f), shape = CircleShape) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.padding(7.dp).size(16.dp))
            }
            Column {
                Text(value, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(label, color = LevyraMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun LibraryWeekChart(pulse: ListeningPulse, locale: Locale) {
    val week = pulse.week.takeLast(7)
    val peak = week.maxOfOrNull { it.listenedMs } ?: 0L

    if (week.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(82.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(7) {
                Box(
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                )
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(76.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            week.forEach { day ->
                val active = day.listenedMs > 0L
                val fraction = if (peak > 0L) {
                    (day.listenedMs.toFloat() / peak.toFloat()).coerceIn(0.10f, 1f)
                } else 0.10f
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    LevyraCyan.copy(alpha = if (active) 0.95f else 0.13f),
                                    LevyraViolet.copy(alpha = if (active) 0.70f else 0.08f)
                                )
                            )
                        )
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            week.forEach { day ->
                val label = day.date.dayOfWeek.getDisplayName(DayTextStyle.NARROW, locale).uppercase(locale)
                Text(
                    text = label,
                    color = LevyraMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun formatListeningTime(totalMs: Long): String {
    val totalMinutes = (totalMs / 60_000L).coerceAtLeast(0L)
    if (totalMinutes < 60L) return "$totalMinutes min"
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
}
