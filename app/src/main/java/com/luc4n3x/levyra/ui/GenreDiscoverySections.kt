package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.luc4n3x.levyra.ui.components.carouselDepthEnabled
import com.luc4n3x.levyra.ui.components.levyraCarouselDepth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.AppleArtistArtworkRepository
import com.luc4n3x.levyra.data.ArtistPortraitFallbackRepository
import com.luc4n3x.levyra.data.SpotifyArtistArtworkRepository
import com.luc4n3x.levyra.data.apple.AppleCatalogItem
import com.luc4n3x.levyra.data.apple.AppleCatalogItemKind
import com.luc4n3x.levyra.data.apple.AppleCatalogShelf
import com.luc4n3x.levyra.data.apple.AppleCatalogShelfKind
import com.luc4n3x.levyra.data.apple.AppleCatalogTracks
import com.luc4n3x.levyra.data.lastfm.LastFmDiscoveryAlbum
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.GenreDiscoveryCopy
import com.luc4n3x.levyra.viewmodel.ExploreGenreDiscoveryState
import com.luc4n3x.levyra.viewmodel.ExploreViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal class GenreDiscoveryActions(
    val playTracks: (List<Track>, Track) -> Unit,
    val playVideo: (List<Track>, Track) -> Unit,
    val openAlbum: (AlbumHit) -> Unit,
    val openPlaylist: (PlaylistHit) -> Unit,
    val openArtist: (String) -> Unit
) {
    fun open(item: AppleCatalogItem, shelf: AppleCatalogShelf) {
        when (item.kind) {
            AppleCatalogItemKind.Album -> openAlbum(item.toAlbumHit())
            AppleCatalogItemKind.Playlist -> openPlaylist(item.toApplePlaylistHit())
            AppleCatalogItemKind.Artist -> openArtist(item.title)
            AppleCatalogItemKind.Song -> AppleCatalogTracks.fromItem(item)?.let { track ->
                val siblings = shelf.items.mapNotNull(AppleCatalogTracks::fromItem)
                playTracks(siblings.ifEmpty { listOf(track) }, track)
            }
            AppleCatalogItemKind.Video -> AppleCatalogTracks.track(
                item.title, item.subtitle, artworkUrl = item.artworkTemplate, durationMs = item.durationMs
            )?.let { track -> playVideo(listOf(track), track) }
            AppleCatalogItemKind.Other -> Unit
        }
    }

    fun open(album: LastFmDiscoveryAlbum) = openAlbum(lastFmAlbumHit(album))
}

internal fun lastFmAlbumHit(album: LastFmDiscoveryAlbum): AlbumHit = AlbumHit(
    title = album.title,
    artist = album.artist,
    year = "",
    thumbnailUrl = album.imageUrl,
    query = listOf(album.title, album.artist, "album").joinToString(" ")
)

internal class GenreDiscoveryBinding(val state: ExploreGenreDiscoveryState, val actions: GenreDiscoveryActions)

@Composable
internal fun rememberExploreGenreDiscovery(
    viewModel: ExploreViewModel,
    identity: String,
    languageCode: String,
    chartId: String,
    load: () -> Unit
): GenreDiscoveryBinding {
    val discovery by viewModel.exploreGenreDiscovery.collectAsStateWithLifecycle()
    LaunchedEffect(identity, languageCode, chartId) { load() }
    val state = discovery.takeIf { it.key == "$languageCode:$identity" } ?: ExploreGenreDiscoveryState()
    val actions = remember(viewModel, state.storefront) {
        GenreDiscoveryActions(
            playTracks = { tracks, track -> viewModel.playFrom(tracks, track) },
            playVideo = { tracks, track -> viewModel.playVideoFrom(tracks, track) },
            openAlbum = viewModel::openAlbum,
            openPlaylist = { playlist -> viewModel.openApplePlaylist(playlist, state.storefront) },
            openArtist = viewModel::openArtistByName
        )
    }
    return GenreDiscoveryBinding(state, actions)
}

internal class GenreDiscoveryTile(
    val key: String,
    val index: Int,
    val title: String,
    val subtitle: String,
    val artworkUrl: String,
    val onClick: () -> Unit
)

internal class GenreDiscoverySkin(
    val keyPrefix: String,
    val rowModifier: Modifier,
    val rowPadding: PaddingValues,
    val itemGap: Dp,
    val header: @Composable (String) -> Unit,
    val cover: @Composable (GenreDiscoveryTile) -> Unit,
    val wide: @Composable (GenreDiscoveryTile) -> Unit,
    val artist: @Composable (GenreDiscoveryTile) -> Unit,
    val about: @Composable (String, GenreDiscoveryCopy) -> Unit,
    val credit: @Composable (String) -> Unit,
    val carouselDepth: Boolean = false
)

internal fun LazyListScope.genreDiscoveryItems(
    discovery: ExploreGenreDiscoveryState,
    copy: GenreDiscoveryCopy,
    skin: GenreDiscoverySkin,
    actions: GenreDiscoveryActions
) {
    val prefix = skin.keyPrefix
    val about = discovery.lastFm?.about.orEmpty()
    if (about.isNotBlank()) {
        item(key = "$prefix-discovery-about", contentType = "$prefix-discovery-about") {
            Column {
                skin.header(copy.aboutTitle)
                skin.about(about, copy)
            }
        }
    }
    discovery.apple.forEachIndexed { shelfIndex, shelf ->
        item(key = "$prefix-discovery-apple-$shelfIndex-${shelf.id}", contentType = "$prefix-discovery-${shelf.kind}") {
            Column {
                if (shelf.title.isNotBlank()) skin.header(shelf.title)
                GenreDiscoveryRow(skin, shelf.items, { item -> "${item.kind}:${item.id}" }) { index, item ->
                    val tile = GenreDiscoveryTile(
                        key = item.id,
                        index = index,
                        title = item.title,
                        subtitle = item.subtitle,
                        artworkUrl = if (shelf.kind == AppleCatalogShelfKind.Hero || shelf.kind == AppleCatalogShelfKind.Videos) {
                            item.heroUrl(GenreDiscoveryWidePx)
                        } else {
                            item.coverUrl(GenreDiscoveryCoverPx)
                        },
                        onClick = { actions.open(item, shelf) }
                    )
                    when {
                        shelf.kind == AppleCatalogShelfKind.Hero || shelf.kind == AppleCatalogShelfKind.Videos -> skin.wide(tile)
                        item.kind == AppleCatalogItemKind.Artist -> skin.artist(tile)
                        else -> skin.cover(tile)
                    }
                }
            }
        }
    }
    if (discovery.apple.isNotEmpty()) {
        item(key = "$prefix-discovery-apple-credit", contentType = "$prefix-discovery-credit") {
            skin.credit(copy.appleCredit)
        }
    }
}

internal fun LazyListScope.genreArtistRowItems(
    entries: List<GenreArtistEntry>,
    title: String,
    skin: GenreDiscoverySkin,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit,
    onOpenArtist: (String) -> Unit
) {
    if (entries.isEmpty()) return
    item(key = "${skin.keyPrefix}-genre-artists", contentType = "${skin.keyPrefix}-genre-artists") {
        Column {
            skin.header(title)
            GenreDiscoveryRow(skin, entries, GenreArtistEntry::key) { index, entry ->
                entry.track?.let { track -> LaunchedEffect(track.id) { onRequestTrackArtwork(track) } }
                val portrait = rememberGenreArtistPortrait(entry.name).ifBlank { entry.fallbackArtworkUrl }
                skin.artist(
                    GenreDiscoveryTile(entry.key, index, entry.name, "", portrait) {
                        entry.track?.let(onPlayTrack) ?: onOpenArtist(entry.name)
                    }
                )
            }
        }
    }
}

internal fun LazyListScope.genreAlbumRowItems(
    entries: List<GenreAlbumEntry>,
    title: String,
    skin: GenreDiscoverySkin,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit,
    onOpenAlbum: (AlbumHit) -> Unit
) {
    if (entries.isEmpty()) return
    item(key = "${skin.keyPrefix}-genre-albums", contentType = "${skin.keyPrefix}-genre-albums") {
        Column {
            skin.header(title)
            GenreDiscoveryRow(skin, entries, GenreAlbumEntry::key) { index, entry ->
                entry.track?.let { track -> LaunchedEffect(track.id) { onRequestTrackArtwork(track) } }
                skin.cover(
                    GenreDiscoveryTile(entry.key, index, entry.title, entry.artist, entry.artworkUrl) {
                        entry.track?.let(onPlayTrack)
                            ?: onOpenAlbum(lastFmAlbumHit(LastFmDiscoveryAlbum(entry.title, entry.artist, entry.artworkUrl)))
                    }
                )
            }
        }
    }
}

@Composable
private fun <T> GenreDiscoveryRow(
    skin: GenreDiscoverySkin,
    entries: List<T>,
    key: (T) -> String,
    content: @Composable (Int, T) -> Unit
) {
    val state = rememberLazyListState()
    val depth = skin.carouselDepth && carouselDepthEnabled()
    LazyRow(
        state = state,
        modifier = skin.rowModifier,
        contentPadding = skin.rowPadding,
        horizontalArrangement = Arrangement.spacedBy(skin.itemGap)
    ) {
        itemsIndexed(entries, key = { _, entry -> key(entry) }) { index, entry ->
            if (skin.carouselDepth) {
                Box(Modifier.levyraCarouselDepth(state, key(entry), depth)) { content(index, entry) }
            } else {
                content(index, entry)
            }
        }
    }
}

@Composable
internal fun GenreDiscoveryCoverCard(
    tile: GenreDiscoveryTile,
    width: Dp,
    shape: Shape,
    panel: Color,
    text: Color,
    muted: Color,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null,
    uppercase: Boolean = false,
    titleWeight: FontWeight = FontWeight.SemiBold,
    aspectRatio: Float = 1f
) {
    Column(
        modifier
            .width(width)
            .levyraPressable(onClick = tile.onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) {
                contentDescription = listOf(tile.title, tile.subtitle).filter(String::isNotBlank).joinToString(", ")
            }
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(aspectRatio).clip(shape).background(panel)) {
            if (tile.artworkUrl.isNotBlank()) {
                AsyncImage(
                    tile.artworkUrl, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clearAndSetSemantics { }
                )
            }
        }
        Column(Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp).clearAndSetSemantics { }) {
            Text(
                if (uppercase) tile.title.uppercase() else tile.title, color = text, fontFamily = fontFamily,
                fontSize = 14.sp, lineHeight = 18.sp, fontWeight = titleWeight, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            run {
                Text(
                    tile.subtitle, color = muted, fontFamily = fontFamily, fontSize = 12.sp, lineHeight = 16.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun GenreDiscoveryArtistCard(
    tile: GenreDiscoveryTile,
    size: Dp,
    shape: Shape,
    panel: Color,
    text: Color,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null,
    uppercase: Boolean = false
) {
    Column(
        modifier
            .width(size + 8.dp)
            .levyraPressable(onClick = tile.onClick, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics(mergeDescendants = true) { contentDescription = tile.title },
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(size).clip(shape).background(panel)) {
            if (tile.artworkUrl.isNotBlank()) {
                AsyncImage(
                    tile.artworkUrl, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clearAndSetSemantics { }
                )
            }
        }
        Text(
            if (uppercase) tile.title.uppercase() else tile.title, color = text, fontFamily = fontFamily,
            fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp).clearAndSetSemantics { }
        )
    }
}

@Composable
internal fun GenreDiscoveryAboutText(
    text: String,
    copy: GenreDiscoveryCopy,
    textColor: Color,
    mutedColor: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    var overflowing by remember(text) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Text(
            text, color = textColor, fontFamily = fontFamily, fontSize = 15.sp, lineHeight = 22.sp,
            maxLines = if (expanded) Int.MAX_VALUE else GenreDiscoveryAboutLines, overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout -> if (!expanded) overflowing = layout.hasVisualOverflow }
        )
        if (overflowing || expanded) {
            Text(
                if (expanded) copy.readLess else copy.readMore, color = accent, fontFamily = fontFamily,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .padding(vertical = 14.dp)
            )
        }
        GenreDiscoveryCredit(copy.aboutCredit, mutedColor, fontFamily = fontFamily)
    }
}

@Composable
internal fun GenreDiscoveryCredit(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null
) {
    Text(text, color = color, fontFamily = fontFamily, fontSize = 11.5.sp, lineHeight = 15.sp, modifier = modifier.fillMaxWidth())
}

@Composable
private fun rememberGenreArtistPortrait(name: String): String {
    val context = LocalContext.current
    val spotify = remember(context) { SpotifyArtistArtworkRepository.get(context) }
    var url by remember(name) { mutableStateOf(spotify.cachedArtistPortrait(name)) }
    LaunchedEffect(name) {
        if (url.isNotBlank()) return@LaunchedEffect
        val apple = AppleArtistArtworkRepository.get(context)
        val fallback = ArtistPortraitFallbackRepository.get(context)
        url = GenreArtistPortraitLookups.withPermit {
            resolveExploreMoodPortrait(
                candidates = listOf(name),
                providers = listOf(
                    spotify::resolveArtistPortrait,
                    apple::resolveArtistPortrait,
                    fallback::youtubePortrait,
                    fallback::deezerPortrait
                )
            )
        }
    }
    return url
}

private val GenreArtistPortraitLookups = Semaphore(3)
private const val GenreDiscoveryAboutLines = 4
private const val GenreDiscoveryCoverPx = 400
private const val GenreDiscoveryWidePx = 800
