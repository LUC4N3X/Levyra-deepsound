package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.ui.selection.TrackSelectionState
import com.luc4n3x.levyra.ui.selection.trackSelectionKey
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraYouTubeMusicSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign

internal sealed interface YouTubeMusicSearchEntry {
    val key: String

    data class Song(val track: Track) : YouTubeMusicSearchEntry {
        override val key: String get() = "song-${track.id}"
    }

    data class Video(val track: Track) : YouTubeMusicSearchEntry {
        override val key: String get() = "video-${track.id}"
    }

    data class Artist(val hit: ArtistHit) : YouTubeMusicSearchEntry {
        override val key: String get() = "artist-${hit.browseId.ifBlank { hit.name }}"
    }

    data class Playlist(val hit: PlaylistHit) : YouTubeMusicSearchEntry {
        override val key: String get() = "playlist-${hit.playlistId.ifBlank { hit.browseId.ifBlank { hit.title } }}"
    }

    data class Album(val hit: AlbumHit) : YouTubeMusicSearchEntry {
        override val key: String get() = "album-${hit.browseId.ifBlank { hit.title + "|" + hit.artist }}"
    }
}

internal fun youTubeMusicSearchEntries(
    songs: List<Track>,
    artists: List<ArtistHit>,
    videos: List<Track>,
    playlists: List<PlaylistHit>,
    albums: List<AlbumHit>,
    leadingSongs: Int = YouTubeMusicSearchLeadingSongs
): List<YouTubeMusicSearchEntry> {
    val mixed = listOf(
        artists.map { YouTubeMusicSearchEntry.Artist(it) },
        videos.map { YouTubeMusicSearchEntry.Video(it) },
        playlists.map { YouTubeMusicSearchEntry.Playlist(it) },
        albums.map { YouTubeMusicSearchEntry.Album(it) }
    )
    val interleaved = buildList {
        val longest = mixed.maxOfOrNull { it.size } ?: 0
        for (index in 0 until longest) mixed.forEach { group -> group.getOrNull(index)?.let(::add) }
    }
    return (songs.take(leadingSongs).map { YouTubeMusicSearchEntry.Song(it) } + interleaved + songs.drop(leadingSongs).map { YouTubeMusicSearchEntry.Song(it) })
        .distinctBy { it.key }
}

internal fun youTubeMusicFilteredEntries(
    filter: SearchFilter,
    songs: List<Track>,
    data: SearchResults,
    topArtist: ArtistHit?
): List<YouTubeMusicSearchEntry> = when (filter) {
    SearchFilter.All -> youTubeMusicSearchEntries(songs, data.artists.filterNot { it == topArtist }, data.videos, data.playlists, data.albums)
    SearchFilter.Songs -> songs.map { YouTubeMusicSearchEntry.Song(it) }
    SearchFilter.Videos -> data.videos.map { YouTubeMusicSearchEntry.Video(it) }
    SearchFilter.Artists -> data.artists.map { YouTubeMusicSearchEntry.Artist(it) }
    SearchFilter.Playlists -> data.playlists.map { YouTubeMusicSearchEntry.Playlist(it) }
    SearchFilter.Albums -> data.albums.map { YouTubeMusicSearchEntry.Album(it) }
}.distinctBy { it.key }

internal class YouTubeMusicSearchActions(
    val playSong: (List<Track>, Track) -> Unit,
    val openArtist: (ArtistHit) -> Unit,
    val openTopResult: () -> Unit,
    val shuffle: () -> Unit,
    val mix: () -> Unit,
    val openPlaylist: (PlaylistHit) -> Unit,
    val openAlbum: (AlbumHit) -> Unit,
    val trackActions: (Track) -> Unit,
    val selection: TrackSelectionState? = null
)

internal fun LazyListScope.youTubeMusicSearchResults(
    topTracks: List<Track>,
    topArtist: ArtistHit?,
    entries: List<YouTubeMusicSearchEntry>,
    songs: List<Track>,
    videos: List<Track>,
    currentTrackId: String?,
    rowModifier: Modifier,
    playsLabel: (Track) -> String,
    actions: YouTubeMusicSearchActions
) {
    if (topTracks.isNotEmpty()) {
        item(key = "ytm-search-top", contentType = "ytm-search-top") {
            Box(rowModifier) { YouTubeMusicSearchTopCard(topTracks, topArtist, currentTrackId, playsLabel, actions) }
        }
    }
    items(entries, key = { "ytm-search-${it.key}" }, contentType = { it::class.simpleName }) { entry ->
        Box(rowModifier) { YouTubeMusicSearchEntryRow(entry, songs, videos, currentTrackId, actions) }
    }
}

@Composable
private fun YouTubeMusicSearchTopCard(
    tracks: List<Track>,
    artist: ArtistHit?,
    currentTrackId: String?,
    playsLabel: (Track) -> String,
    actions: YouTubeMusicSearchActions
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraYouTubeMusicSkinStrings.forCode(strings.code) }
    val hero = tracks.first()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = YouTubeMusicDesign.gutter, vertical = 8.dp)
            .clip(YouTubeMusicSearchCardShape)
            .background(YouTubeMusicDesign.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .levyraPressable(onClick = actions.openTopResult, role = Role.Button, pressedScale = LevyraPressScale.Row)
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            YouTubeMusicSearchArtwork(
                artist?.thumbnailUrl?.takeIf(String::isNotBlank) ?: hero.largeThumbnailUrl.ifBlank { hero.thumbnailUrl },
                if (artist != null) CircleShape else YouTubeMusicDesign.thumbShape,
                YouTubeMusicSearchTopAvatar
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    artist?.name ?: hero.title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                val subtitle = artist?.subscribers?.takeIf(String::isNotBlank) ?: listOf(copy.song, hero.artist).joinToString(" • ")
                Text(subtitle, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = YouTubeMusicDesign.text, modifier = Modifier.size(28.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            YouTubeMusicSearchPill(Icons.Rounded.Shuffle, copy.shuffle, filled = true, onClick = actions.shuffle, modifier = Modifier.weight(1f))
            YouTubeMusicSearchPill(Icons.Rounded.Sensors, strings.mix, filled = false, onClick = actions.mix, modifier = Modifier.weight(1f))
        }
        tracks.take(YouTubeMusicSearchTopSongs).forEach { track ->
            val plays = playsLabel(track)
            YouTubeMusicSongRow(
                track = track,
                subtitle = listOf(copy.song, plays.ifBlank { track.artist }).filter(String::isNotBlank).joinToString(" • "),
                current = track.id == currentTrackId,
                explicitLabel = copy.explicit,
                onClick = { actions.playSong(tracks, track) },
                onMore = { actions.trackActions(track) },
                onLongClick = { actions.selection?.start(trackSelectionKey(track)) ?: actions.trackActions(track) },
                selectionActive = actions.selection?.isActive == true,
                selected = actions.selection?.isSelected(trackSelectionKey(track)) == true,
                background = YouTubeMusicDesign.surfaceRaised
            )
        }
    }
}

@Composable
private fun YouTubeMusicSearchPill(icon: ImageVector, label: String, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(YouTubeMusicSearchPillHeight)
            .clip(YouTubeMusicDesign.pillShape)
            .background(if (filled) Color.White else Color.Transparent)
            .border(1.dp, if (filled) Color.Transparent else YouTubeMusicDesign.outline, YouTubeMusicDesign.pillShape)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = if (filled) Color.Black else YouTubeMusicDesign.text
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = tint, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun YouTubeMusicSearchEntryRow(
    entry: YouTubeMusicSearchEntry,
    songs: List<Track>,
    videos: List<Track>,
    currentTrackId: String?,
    actions: YouTubeMusicSearchActions
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraYouTubeMusicSkinStrings.forCode(strings.code) }
    val discography = remember(strings.code) { searchDiscographyCopy(strings.code) }
    when (entry) {
        is YouTubeMusicSearchEntry.Song -> YouTubeMusicSongRow(
            track = entry.track,
            subtitle = listOf(copy.song, entry.track.artist).filter(String::isNotBlank).joinToString(" • "),
            current = entry.track.id == currentTrackId,
            explicitLabel = copy.explicit,
            onClick = { actions.playSong(songs, entry.track) },
            onLongClick = { actions.selection?.start(trackSelectionKey(entry.track)) ?: actions.trackActions(entry.track) },
            selectionActive = actions.selection?.isActive == true,
            selected = actions.selection?.isSelected(trackSelectionKey(entry.track)) == true,
            onMore = { actions.trackActions(entry.track) }
        )
        is YouTubeMusicSearchEntry.Video -> YouTubeMusicSongRow(
            track = entry.track,
            subtitle = listOf(copy.video, entry.track.artist).filter(String::isNotBlank).joinToString(" • "),
            current = entry.track.id == currentTrackId,
            explicitLabel = copy.explicit,
            onClick = { actions.playSong(videos, entry.track) },
            onLongClick = { actions.selection?.start(trackSelectionKey(entry.track)) ?: actions.trackActions(entry.track) },
            selectionActive = actions.selection?.isActive == true,
            selected = actions.selection?.isSelected(trackSelectionKey(entry.track)) == true,
            onMore = { actions.trackActions(entry.track) },
            leading = {
                YouTubeMusicSearchArtwork(
                    entry.track.thumbnailUrl.ifBlank { entry.track.largeThumbnailUrl },
                    YouTubeMusicDesign.thumbShape,
                    YouTubeMusicDesign.listArtwork,
                    aspect = YouTubeMusicSearchVideoAspect
                )
            }
        )
        is YouTubeMusicSearchEntry.Artist -> YouTubeMusicSearchRow(
            title = entry.hit.name,
            subtitle = listOf(strings.artistLabel, entry.hit.subscribers).filter(String::isNotBlank).joinToString(" • "),
            imageUrl = entry.hit.thumbnailUrl,
            shape = CircleShape,
            moreLabel = strings.options,
            onClick = { actions.openArtist(entry.hit) }
        )
        is YouTubeMusicSearchEntry.Playlist -> YouTubeMusicSearchRow(
            title = entry.hit.title,
            subtitle = listOf(discography.playlist, entry.hit.author).filter(String::isNotBlank).joinToString(" • "),
            imageUrl = entry.hit.thumbnailUrl,
            shape = YouTubeMusicDesign.thumbShape,
            moreLabel = strings.options,
            onClick = { actions.openPlaylist(entry.hit) }
        )
        is YouTubeMusicSearchEntry.Album -> YouTubeMusicSearchRow(
            title = entry.hit.title,
            subtitle = listOf(entry.hit.searchCardModel(discography.album, discography.single).badge, entry.hit.artist, entry.hit.year)
                .filter(String::isNotBlank).joinToString(" • "),
            imageUrl = entry.hit.thumbnailUrl,
            shape = YouTubeMusicDesign.thumbShape,
            moreLabel = strings.options,
            onClick = { actions.openAlbum(entry.hit) }
        )
    }
}

@Composable
private fun YouTubeMusicSearchRow(
    title: String,
    subtitle: String,
    imageUrl: String,
    shape: Shape,
    moreLabel: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(YouTubeMusicDesign.listRow)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .padding(start = YouTubeMusicDesign.gutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        YouTubeMusicSearchArtwork(imageUrl, shape, YouTubeMusicDesign.listArtwork)
        Spacer(Modifier.width(YouTubeMusicDesign.gutter))
        Column(Modifier.weight(1f)) {
            Text(title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        YouTubeMusicIconAction(Icons.Rounded.MoreVert, moreLabel, onClick, iconSize = 24.dp)
    }
}

@Composable
private fun YouTubeMusicSearchArtwork(url: String, shape: Shape, height: Dp, aspect: Float = 1f) {
    Box(
        Modifier
            .height(height)
            .width(height * aspect)
            .clip(shape)
            .background(YouTubeMusicDesign.placeholder)
    ) {
        if (url.isNotBlank()) StableRemoteArtwork(url, null, Modifier.fillMaxSize(), ContentScale.Crop)
    }
}

@Composable
internal fun YouTubeMusicSearchHeader(
    query: String,
    placeholder: String,
    isSearching: Boolean,
    recognitionAvailable: Boolean,
    recognitionBusy: Boolean,
    onQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onVoice: () -> Unit,
    onRecognize: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        Modifier.fillMaxWidth().height(YouTubeMusicSearchHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        YouTubeMusicIconAction(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, onBack, iconSize = 24.dp)
        Row(
            Modifier
                .weight(1f)
                .height(YouTubeMusicSearchFieldHeight)
                .clip(YouTubeMusicDesign.pillShape)
                .background(YouTubeMusicDesign.surfaceHigh)
                .padding(start = 18.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = YouTubeMusicDesign.text, fontSize = 18.sp),
                cursorBrush = SolidColor(YouTubeMusicDesign.text),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                modifier = Modifier.weight(1f),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(placeholder, color = YouTubeMusicDesign.textSecondary, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        field()
                    }
                }
            )
            when {
                isSearching -> LevyraLoadingIndicator(color = YouTubeMusicDesign.text, modifier = Modifier.padding(horizontal = 8.dp).size(20.dp))
                query.isNotEmpty() -> YouTubeMusicIconAction(Icons.Rounded.Close, strings.clear, onClear, iconSize = 22.dp)
            }
        }
        YouTubeMusicSearchCircle(Icons.Rounded.Mic, strings.voice, active = false, onClick = onVoice)
        if (recognitionAvailable) {
            YouTubeMusicSearchCircle(Icons.Rounded.GraphicEq, strings.recognizeMusic, active = recognitionBusy, onClick = onRecognize)
        }
    }
}

@Composable
private fun YouTubeMusicSearchCircle(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(YouTubeMusicSearchFieldHeight)
            .clip(CircleShape)
            .background(if (active) Color.White else YouTubeMusicDesign.surfaceHigh)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (active) Color.Black else YouTubeMusicDesign.text, modifier = Modifier.size(24.dp))
    }
}

private val YouTubeMusicSearchCardShape = RoundedCornerShape(8.dp)
private val YouTubeMusicSearchTopAvatar = 56.dp
private val YouTubeMusicSearchPillHeight = 40.dp
private val YouTubeMusicSearchHeaderHeight = 64.dp
private val YouTubeMusicSearchFieldHeight = 48.dp
private const val YouTubeMusicSearchVideoAspect = 16f / 9f
private const val YouTubeMusicSearchTopSongs = 3
private const val YouTubeMusicSearchLeadingSongs = 5
