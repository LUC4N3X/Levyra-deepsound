package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.AppleSearchCopy
import com.luc4n3x.levyra.ui.i18n.LevyraAppleMusicSearchStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign

internal val AppleSearchActiveTop = 16.dp
internal val AppleSearchMutedText = Color(0xFF8E8E93)
private val AppleResultArtwork = 56.dp
private val AppleResultVideoWidth = 99.dp
private val AppleResultInset = 16.dp
private val AppleResultTextGap = 10.dp
private val AppleTabHeight = 48.dp
private val AppleTabPadding = 12.dp
private const val AppleTabIndicatorMs = 250

internal fun Modifier.appleGutterBleed(gutter: Dp = AppleMusicDesign.gutter): Modifier = layout { measurable, constraints ->
    val bleed = gutter.roundToPx()
    val width = constraints.maxWidth + bleed * 2
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed, 0) }
}

internal class AppleSearchSuffixTransformation(private val suffix: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val length = text.length
        val transformed = buildAnnotatedString {
            append(text)
            withStyle(SpanStyle(color = AppleSearchMutedText)) { append(" $suffix") }
        }
        return TransformedText(transformed, object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int = offset.coerceAtMost(length)
        })
    }
}

internal fun appleSearchCopy(code: String): AppleSearchCopy = LevyraAppleMusicSearchStrings.forCode(code)

internal sealed interface AppleSearchResult {
    val key: String

    class Song(val track: Track, val video: Boolean = false) : AppleSearchResult {
        override val key: String get() = (if (video) "video-" else "song-") + track.id
    }

    class Artist(val hit: ArtistHit) : AppleSearchResult {
        override val key: String get() = "artist-${hit.browseId.ifBlank { hit.name }}"
    }

    class Album(val hit: AlbumHit) : AppleSearchResult {
        override val key: String get() = "album-${hit.browseId.ifBlank { hit.title + "|" + hit.artist }}"
    }

    class Playlist(val hit: PlaylistHit) : AppleSearchResult {
        override val key: String get() = "playlist-${hit.playlistId.ifBlank { hit.browseId.ifBlank { hit.title } }}"
    }
}

internal fun appleSearchResults(filter: SearchFilter, songs: List<Track>, topArtist: ArtistHit?, data: SearchResults): List<AppleSearchResult> {
    val artists = listOfNotNull(topArtist) + data.artists.filterNot { it == topArtist }
    val groups = when (filter) {
        SearchFilter.All -> listOf(
            songs.map { AppleSearchResult.Song(it) },
            artists.map { AppleSearchResult.Artist(it) },
            data.albums.map { AppleSearchResult.Album(it) },
            data.playlists.map { AppleSearchResult.Playlist(it) },
            data.videos.map { AppleSearchResult.Song(it, video = true) }
        )
        SearchFilter.Songs -> listOf(songs.map { AppleSearchResult.Song(it) })
        SearchFilter.Artists -> listOf(artists.map { AppleSearchResult.Artist(it) })
        SearchFilter.Albums -> listOf(data.albums.map { AppleSearchResult.Album(it) })
        SearchFilter.Playlists -> listOf(data.playlists.map { AppleSearchResult.Playlist(it) })
        SearchFilter.Videos -> listOf(data.videos.map { AppleSearchResult.Song(it, video = true) })
    }
    val longest = groups.maxOfOrNull { it.size } ?: 0
    return buildList {
        for (index in 0 until longest) groups.forEach { group -> group.getOrNull(index)?.let(::add) }
    }.distinctBy { it.key }
}

internal fun appleSearchFilters(data: SearchResults): List<SearchFilter> = buildList {
    add(SearchFilter.All)
    if (data.artists.isNotEmpty()) add(SearchFilter.Artists)
    if (data.albums.isNotEmpty()) add(SearchFilter.Albums)
    if (data.songs.isNotEmpty()) add(SearchFilter.Songs)
    if (data.playlists.isNotEmpty()) add(SearchFilter.Playlists)
    if (data.videos.isNotEmpty()) add(SearchFilter.Videos)
}

internal fun LazyListScope.appleMusicSearchResults(
    filters: List<SearchFilter>,
    selected: SearchFilter,
    results: List<AppleSearchResult>,
    currentTrackId: String?,
    onSelect: (SearchFilter) -> Unit,
    actions: YouTubeMusicSearchActions
) {
    item(key = "apple-search-tabs", contentType = "apple-search-tabs") { AppleSearchTabs(filters, selected, onSelect) }
    val songs = results.mapNotNull { (it as? AppleSearchResult.Song)?.takeIf { song -> !song.video }?.track }
    val videos = results.mapNotNull { (it as? AppleSearchResult.Song)?.takeIf(AppleSearchResult.Song::video)?.track }
    items(results, key = { it.key }, contentType = { it::class }) { result ->
        AppleSearchResultRow(result, currentTrackId, songs, videos, actions)
    }
}

@Composable
private fun AppleSearchTabs(filters: List<SearchFilter>, selected: SearchFilter, onSelect: (SearchFilter) -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { appleSearchCopy(strings.code) }
    val bounds = remember { androidx.compose.runtime.mutableStateMapOf<SearchFilter, Pair<Dp, Dp>>() }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val target = bounds[selected]
    val indicatorStart by androidx.compose.animation.core.animateDpAsState(target?.first ?: 0.dp, androidx.compose.animation.core.tween(AppleTabIndicatorMs), label = "apple-tab-start")
    val indicatorWidth by androidx.compose.animation.core.animateDpAsState(target?.second ?: 0.dp, androidx.compose.animation.core.tween(AppleTabIndicatorMs), label = "apple-tab-width")
    Column(Modifier.fillMaxWidth().appleGutterBleed().padding(top = 10.dp)) {
        Box(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
        Row(Modifier.padding(horizontal = AppleMusicDesign.gutter)) {
            filters.forEach { filter ->
                val active = filter == selected
                val label = when (filter) {
                    SearchFilter.All -> copy.topResults
                    SearchFilter.Artists -> strings.artistsLabelPlural
                    SearchFilter.Albums -> strings.albumsPlain
                    SearchFilter.Songs -> strings.songsPlain
                    SearchFilter.Playlists -> strings.playlistsPlain
                    SearchFilter.Videos -> strings.video
                }
                val color by androidx.compose.animation.animateColorAsState(
                    if (active) AppleMusicDesign.accent else AppleSearchMutedText,
                    androidx.compose.animation.core.tween(AppleTabIndicatorMs), label = "apple-tab-color"
                )
                Box(
                    Modifier.height(AppleTabHeight).semantics { this.selected = active }
                        .onGloballyPositioned { coordinates ->
                            with(density) {
                                bounds[filter] = coordinates.positionInParent().x.toDp() + AppleMusicDesign.gutter to coordinates.size.width.toDp()
                            }
                        }
                        .clickable(role = Role.Tab) { onSelect(filter) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label.uppercase(), maxLines = 1, color = color,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 20.sp),
                        modifier = Modifier.padding(horizontal = AppleTabPadding)
                    )
                }
            }
        }
        if (target != null) Box(
            Modifier.align(Alignment.BottomStart).offset(x = indicatorStart).width(indicatorWidth).height(2.dp)
                .background(AppleMusicDesign.accent)
        )
        }
        HorizontalDivider(thickness = 0.5.dp, color = AppleMusicDesign.opaqueSeparator)
    }
}

@Composable
private fun AppleSearchResultRow(
    result: AppleSearchResult,
    currentTrackId: String?,
    songs: List<Track>,
    videos: List<Track>,
    actions: YouTubeMusicSearchActions
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { appleSearchCopy(strings.code) }
    val title: String
    val subtitle: String
    val artwork: String
    var artworkWidth = AppleResultArtwork
    var circular = false
    var track: Track? = null
    val onClick: () -> Unit
    when (result) {
        is AppleSearchResult.Song -> {
            track = result.track
            title = result.track.title
            subtitle = listOf(if (result.video) copy.musicVideo else copy.song, result.track.artist).filter(String::isNotBlank).joinToString(" · ")
            artwork = result.track.largeThumbnailUrl.ifBlank { result.track.thumbnailUrl }
            if (result.video) artworkWidth = AppleResultVideoWidth
            onClick = { actions.playSong(if (result.video) videos else songs, result.track) }
        }
        is AppleSearchResult.Artist -> {
            title = result.hit.name
            subtitle = copy.artist
            artwork = result.hit.thumbnailUrl
            circular = true
            onClick = { actions.openArtist(result.hit) }
        }
        is AppleSearchResult.Album -> {
            title = result.hit.title
            subtitle = listOf(copy.album, result.hit.artist).filter(String::isNotBlank).joinToString(" · ")
            artwork = result.hit.thumbnailUrl
            onClick = { actions.openAlbum(result.hit) }
        }
        is AppleSearchResult.Playlist -> {
            title = result.hit.title
            subtitle = listOf(copy.playlist, result.hit.author).filter(String::isNotBlank).joinToString(" · ")
            artwork = result.hit.thumbnailUrl
            onClick = { actions.openPlaylist(result.hit) }
        }
    }
    val shape = if (circular) CircleShape else AppleMusicDesign.thumbShape
    Column(Modifier.fillMaxWidth().appleGutterBleed()) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
                .padding(start = AppleResultInset, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(width = artworkWidth, height = AppleResultArtwork).clip(shape)
                    .background(AppleMusicDesign.placeholder).border(0.5.dp, AppleMusicDesign.artworkStroke, shape)
            ) {
                if (track != null && artwork.isBlank()) CoverImage(track, Modifier.fillMaxSize())
                else if (artwork.isNotBlank()) StableRemoteArtwork(artwork, null, Modifier.fillMaxSize(), ContentScale.Crop)
            }
            Spacer(Modifier.width(AppleResultTextGap))
            Column(Modifier.weight(1f)) {
                Text(
                    title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (track != null && track.id == currentTrackId) AppleMusicDesign.accent else AppleMusicDesign.label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 22.sp)
                )
                Text(
                    subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, color = AppleSearchMutedText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp)
                )
            }
            if (track != null) Box(
                Modifier.size(width = 44.dp, height = AppleResultArtwork).clickable(role = Role.Button) { actions.trackActions(track) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MoreVert, strings.actions, tint = AppleMusicDesign.label, modifier = Modifier.size(22.dp))
            } else Spacer(Modifier.width(44.dp))
        }
        HorizontalDivider(Modifier.padding(start = AppleResultInset), thickness = 0.5.dp, color = AppleMusicDesign.opaqueSeparator)
    }
}
