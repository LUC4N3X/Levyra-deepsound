package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraBlue
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import java.nio.charset.StandardCharsets
import java.util.Base64

internal const val ExploreNewReleasesDestination = "explore-destination-new-releases"
internal const val ExploreMoodsDestination = "explore-destination-moods"
private const val ExploreMoodDestinationPrefix = "explore-destination-mood:"
private const val ExploreCategoryDestinationPrefix = "explore-destination-provider-category:"
private val ExploreDestinationHeaderHeight = 66.dp

internal fun exploreMoodDestination(zoneId: String): String = "$ExploreMoodDestinationPrefix$zoneId"
internal fun exploreMoodDestinationId(destination: String?): String? =
    destination?.takeIf { it.startsWith(ExploreMoodDestinationPrefix) }
        ?.removePrefix(ExploreMoodDestinationPrefix)
        ?.takeIf(String::isNotBlank)

internal fun exploreCategoryDestination(category: ExploreCategory): String {
    val encoder = Base64.getUrlEncoder().withoutPadding()
    val fields = listOf(category.params, category.title, category.section).map { value ->
        encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }
    return ExploreCategoryDestinationPrefix + fields.joinToString(".") + ".${category.sectionIndex}"
}

internal fun exploreCategoryDestinationValue(destination: String?): ExploreCategory? {
    val payload = destination
        ?.takeIf { value -> value.startsWith(ExploreCategoryDestinationPrefix) }
        ?.removePrefix(ExploreCategoryDestinationPrefix)
        ?: return null
    val fields = payload.split('.', limit = 4)
    if (fields.size != 4) return null
    return runCatching {
        val decoder = Base64.getUrlDecoder()
        fun decode(value: String): String = String(decoder.decode(value), StandardCharsets.UTF_8)
        ExploreCategory(
            params = decode(fields[0]),
            title = decode(fields[1]),
            section = decode(fields[2]),
            sectionIndex = fields[3].toInt()
        )
    }.getOrNull()?.takeIf { category -> category.params.isNotBlank() && category.title.isNotBlank() }
}

@Composable
internal fun ExploreCollectionDestinationScreen(
    identity: String,
    title: String,
    subtitle: String?,
    zone: ExploreZone?,
    tracks: List<Track>,
    isLoading: Boolean,
    currentTrackId: String?,
    isPlaying: Boolean,
    strings: LevyraStrings,
    backEnabled: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    BackHandler(enabled = backEnabled, onBack = onBack)
    val rotationBucket = remember(identity) {
        exploreGenreRotationBucket(System.currentTimeMillis())
    }
    val trackStructure = remember(tracks) { tracks.map { track -> track.id } }
    val editorialStructure = remember(trackStructure, identity, rotationBucket) {
        buildExploreGenreEditorial(
            tracks = tracks,
            zoneId = zone?.id ?: identity,
            rotationBucket = rotationBucket
        )
    }
    val editorial = remember(editorialStructure, tracks) {
        refreshExploreGenreEditorialMetadata(editorialStructure, tracks)
    }

    ExploreDestinationSurface(
        title = title,
        subtitle = subtitle,
        strings = strings,
        onBack = onBack,
        trailing = if (tracks.isNotEmpty()) {
            { ExploreCollectionPlayAllButton(strings = strings, onPlayAll = onPlayAll) }
        } else {
            null
        }
    ) { contentPadding ->
        ExploreCollectionDestinationContent(
            identity = identity,
            title = title,
            subtitle = subtitle,
            zone = zone,
            tracks = tracks,
            editorial = editorial,
            isLoading = isLoading,
            currentTrackId = currentTrackId,
            isPlaying = isPlaying,
            strings = strings,
            contentPadding = contentPadding,
            onPlayTrack = onPlayTrack,
            onRequestTrackArtwork = onRequestTrackArtwork
        )
    }
}

@Composable
private fun ExploreCollectionPlayAllButton(
    strings: LevyraStrings,
    onPlayAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(LevyraCyan, CircleShape)
            .semantics { role = Role.Button }
            .clickable(onClick = onPlayAll),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = strings.play,
            tint = LevyraBlack,
            modifier = Modifier.size(23.dp)
        )
    }
}

@Composable
private fun ExploreCollectionDestinationContent(
    identity: String,
    title: String,
    subtitle: String?,
    zone: ExploreZone?,
    tracks: List<Track>,
    editorial: ExploreGenreEditorial,
    isLoading: Boolean,
    currentTrackId: String?,
    isPlaying: Boolean,
    strings: LevyraStrings,
    contentPadding: PaddingValues,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    when {
        isLoading && tracks.isEmpty() -> ExploreCollectionLoading(contentPadding)
        tracks.isEmpty() -> ExploreCollectionEmpty(
            contentPadding = contentPadding,
            message = strings.exploreEmpty
        )
        else -> ExploreCollectionList(
            identity = identity,
            title = title,
            subtitle = subtitle,
            zone = zone,
            tracks = tracks,
            editorial = editorial,
            currentTrackId = currentTrackId,
            isPlaying = isPlaying,
            strings = strings,
            contentPadding = contentPadding,
            onPlayTrack = onPlayTrack,
            onRequestTrackArtwork = onRequestTrackArtwork
        )
    }
}

@Composable
private fun ExploreCollectionLoading(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = LevyraCyan, strokeWidth = 3.dp)
    }
}

@Composable
private fun ExploreCollectionEmpty(
    contentPadding: PaddingValues,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = LevyraMuted,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ExploreCollectionList(
    identity: String,
    title: String,
    subtitle: String?,
    zone: ExploreZone?,
    tracks: List<Track>,
    editorial: ExploreGenreEditorial,
    currentTrackId: String?,
    isPlaying: Boolean,
    strings: LevyraStrings,
    contentPadding: PaddingValues,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = contentPadding.calculateTopPadding() + 10.dp,
            bottom = 130.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        exploreCollectionHeroItem(
            identity = identity,
            title = title,
            subtitle = subtitle,
            zone = zone,
            track = tracks.first(),
            onRequestTrackArtwork = onRequestTrackArtwork
        )
        exploreFeaturedItems(
            tracks = editorial.featured,
            currentTrackId = currentTrackId,
            strings = strings,
            onPlayTrack = onPlayTrack,
            onRequestTrackArtwork = onRequestTrackArtwork
        )
        exploreArtistItems(
            artists = editorial.artists,
            strings = strings,
            onPlayTrack = onPlayTrack,
            onRequestTrackArtwork = onRequestTrackArtwork
        )
        exploreAlbumItems(
            albums = editorial.albums,
            strings = strings,
            onPlayTrack = onPlayTrack,
            onRequestTrackArtwork = onRequestTrackArtwork
        )
        exploreEssentialItems(
            tracks = editorial.essentials,
            currentTrackId = currentTrackId,
            isPlaying = isPlaying,
            strings = strings,
            onPlayTrack = onPlayTrack,
            onRequestTrackArtwork = onRequestTrackArtwork
        )
    }
}

private fun LazyListScope.exploreCollectionHeroItem(
    identity: String,
    title: String,
    subtitle: String?,
    zone: ExploreZone?,
    track: Track,
    onRequestTrackArtwork: (Track) -> Unit
) {
    item(key = "explore-collection-hero") {
        LaunchedEffect(track.id) {
            onRequestTrackArtwork(track)
        }
        ExploreCollectionHero(
            title = title,
            subtitle = subtitle,
            leadTrack = track,
            identity = identity,
            zone = zone
        )
    }
}

private fun LazyListScope.exploreFeaturedItems(
    tracks: List<Track>,
    currentTrackId: String?,
    strings: LevyraStrings,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    if (tracks.isEmpty()) return
    item(key = "explore-genre-popular-header") {
        ExploreGenreSectionHeader(strings.popularTracks)
    }
    item(key = "explore-genre-popular") {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(
                items = tracks,
                key = { track -> "explore-featured-${track.id}" }
            ) { track ->
                LaunchedEffect(track.id) {
                    onRequestTrackArtwork(track)
                }
                ExploreGenreTrackCard(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    onClick = { onPlayTrack(track) }
                )
            }
        }
    }
}

private fun LazyListScope.exploreArtistItems(
    artists: List<ExploreGenreArtistCard>,
    strings: LevyraStrings,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    if (artists.isEmpty()) return
    item(key = "explore-genre-artists-header") {
        ExploreGenreSectionHeader(strings.artists)
    }
    item(key = "explore-genre-artists") {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(
                items = artists,
                key = { artist -> "explore-artist-${artist.key}" }
            ) { artist ->
                LaunchedEffect(artist.track.id) {
                    onRequestTrackArtwork(artist.track)
                }
                ExploreGenreArtistCard(
                    artist = artist,
                    onClick = { onPlayTrack(artist.track) }
                )
            }
        }
    }
}

private fun LazyListScope.exploreAlbumItems(
    albums: List<ExploreGenreAlbumCard>,
    strings: LevyraStrings,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    if (albums.isEmpty()) return
    item(key = "explore-genre-albums-header") {
        ExploreGenreSectionHeader(strings.albumsPlain)
    }
    item(key = "explore-genre-albums") {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(
                items = albums,
                key = { album -> "explore-album-${album.key}" }
            ) { album ->
                LaunchedEffect(album.track.id) {
                    onRequestTrackArtwork(album.track)
                }
                ExploreGenreAlbumCard(
                    album = album,
                    onClick = { onPlayTrack(album.track) }
                )
            }
        }
    }
}

private fun LazyListScope.exploreEssentialItems(
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    strings: LevyraStrings,
    onPlayTrack: (Track) -> Unit,
    onRequestTrackArtwork: (Track) -> Unit
) {
    item(key = "explore-genre-songs-header") {
        ExploreGenreSectionHeader(strings.songsPlain)
    }
    items(
        items = tracks,
        key = { track -> "explore-destination-track-${track.id}" }
    ) { track ->
        LaunchedEffect(track.id) {
            onRequestTrackArtwork(track)
        }
        ExploreDestinationTrackRow(
            track = track,
            isCurrent = track.id == currentTrackId,
            isPlaying = isPlaying && track.id == currentTrackId,
            onClick = { onPlayTrack(track) }
        )
    }
}

@Composable
private fun ExploreGenreSectionHeader(title: String) {
    Text(
        text = title,
        color = LevyraText,
        fontSize = 20.sp,
        lineHeight = LevyraTypeRhythm.lineHeight(20.sp),
        fontWeight = FontWeight.Black,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ExploreGenreTrackCard(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val artwork = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
    Column(
        modifier = Modifier
            .width(148.dp)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(LevyraPanel)
                .border(
                    BorderStroke(
                        1.dp,
                        if (isCurrent) LevyraCyan.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.09f)
                    ),
                    RoundedCornerShape(16.dp)
                )
        ) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(34.dp)
                    .background(if (isCurrent) LevyraCyan else LevyraBlack.copy(alpha = 0.78f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = if (isCurrent) LevyraBlack else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = track.title,
            color = LevyraText,
            fontSize = 13.5.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(13.5.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = LevyraMuted,
            fontSize = 11.5.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(11.5.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExploreGenreArtistCard(
    artist: ExploreGenreArtistCard,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(116.dp)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = artist.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(LevyraPanel)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), CircleShape)
        )
        Text(
            text = artist.name,
            color = LevyraText,
            fontSize = 12.5.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(12.5.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ExploreGenreAlbumCard(
    album: ExploreGenreAlbumCard,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        AsyncImage(
            model = album.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LevyraPanel)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)), RoundedCornerShape(14.dp))
        )
        Text(
            text = album.title,
            color = LevyraText,
            fontSize = 13.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(13.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (album.artist.isNotBlank()) {
            Text(
                text = album.artist,
                color = LevyraMuted,
                fontSize = 11.5.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(11.5.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExploreCollectionHero(
    title: String,
    subtitle: String?,
    leadTrack: Track,
    identity: String,
    zone: ExploreZone?
) {
    val categoryPalette = exploreCategoryPalette(identity)
    val accentStart = zone?.let { value -> Color(value.accentStart) } ?: categoryPalette.first
    val accentEnd = zone?.let { value -> Color(value.accentEnd) } ?: categoryPalette.second
    val artwork = leadTrack.largeThumbnailUrl.ifBlank { leadTrack.thumbnailUrl }
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(214.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(accentStart, accentEnd)))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape)
    ) {
        if (artwork.isNotBlank()) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(204.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accentStart.copy(alpha = 0.99f),
                            accentStart.copy(alpha = 0.92f),
                            accentEnd.copy(alpha = 0.36f),
                            accentEnd.copy(alpha = 0f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(LevyraBlack.copy(alpha = 0f), LevyraBlack.copy(alpha = 0.58f))
                    )
                )
        )
        zone?.emoji?.takeIf(String::isNotBlank)?.let { emoji ->
            Text(
                text = emoji,
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(18.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.74f)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(28.sp),
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.takeIf(String::isNotBlank)?.let { detail ->
                Text(
                    text = detail,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.5.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(12.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun ExploreNewReleasesDestinationScreen(
    releases: List<AlbumHit>,
    isLoading: Boolean,
    strings: LevyraStrings,
    backEnabled: Boolean,
    onBack: () -> Unit,
    onOpenRelease: (AlbumHit) -> Unit
) {
    BackHandler(enabled = backEnabled, onBack = onBack)
    ExploreDestinationSurface(
        title = strings.exploreNewReleases,
        subtitle = null,
        strings = strings,
        onBack = onBack
    ) { contentPadding ->
        when {
            isLoading && releases.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LevyraCyan, strokeWidth = 3.dp)
            }
            releases.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.exploreEmpty,
                    color = LevyraMuted,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = contentPadding.calculateTopPadding() + 10.dp,
                    bottom = 130.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = releases,
                    key = { release -> "ytm-release-${release.browseId.ifBlank { release.title + release.artist }}" }
                ) { release ->
                    ExploreDestinationReleaseRow(
                        release = release,
                        onClick = { onOpenRelease(release) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreDestinationReleaseRow(
    release: AlbumHit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 78.dp)
            .clip(shape)
            .background(LevyraPanel.copy(alpha = 0.72f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = release.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(62.dp).clip(RoundedCornerShape(9.dp))
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = release.title,
                color = LevyraText,
                fontSize = 15.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(15.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(release.artist, release.year)
                    .filter(String::isNotBlank)
                    .joinToString(" • "),
                color = LevyraMuted,
                fontSize = 12.5.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(12.5.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = LevyraText,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
internal fun ExploreMoodsDestinationScreen(
    zones: List<ExploreZone>,
    categories: List<ExploreCategory>,
    categoryArtwork: Map<String, String>,
    isLoading: Boolean,
    strings: LevyraStrings,
    backEnabled: Boolean,
    onBack: () -> Unit,
    onOpenZone: (ExploreZone) -> Unit,
    onOpenCategory: (ExploreCategory) -> Unit,
    onRequestCategoryArtwork: (String, Boolean) -> Unit
) {
    BackHandler(enabled = backEnabled, onBack = onBack)
    val sections = remember(categories) { buildExploreCategorySections(categories) }
    val hasProviderGenres = sections.any { section ->
        section.presentation == ExploreCategoryPresentation.Structured
    }
    val fallbackGenres = remember(zones, hasProviderGenres) {
        if (hasProviderGenres) emptyList() else exploreFallbackGenres(zones)
    }
    ExploreDestinationSurface(
        title = strings.exploreMoods,
        subtitle = strings.exploreSubtitle,
        strings = strings,
        onBack = onBack
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 18.dp,
                bottom = 130.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading && sections.isEmpty()) {
                item(key = "provider-moods-loading-title") {
                    ExploreCategorySectionHeader(strings.exploreMoodSection)
                }
                items(
                    count = 2,
                    key = { index -> "provider-mood-loading-row-$index" }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(108.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExploreDiscoveryCategoryPlaceholder(Modifier.weight(1f).fillMaxHeight())
                        ExploreDiscoveryCategoryPlaceholder(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }

            sections.forEach { section ->
                val title = section.providerTitle.ifBlank {
                    when (section.presentation) {
                        ExploreCategoryPresentation.Atmospheric -> strings.exploreMoodSection
                        ExploreCategoryPresentation.Structured -> strings.genres
                        ExploreCategoryPresentation.Mixed -> strings.exploreMoods
                    }
                }
                item(key = "${section.key}-header") {
                    ExploreCategorySectionHeader(title)
                }
                items(
                    items = section.categories.chunked(2),
                    key = { pair -> "${section.key}-${pair.joinToString("|") { it.params }}" }
                ) { pair ->
                    val prominent = section.presentation == ExploreCategoryPresentation.Atmospheric
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (prominent) 112.dp else 102.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { category ->
                            LaunchedEffect(category.params) {
                                onRequestCategoryArtwork(category.params, prominent)
                            }
                            ExploreDiscoveryCategoryCard(
                                title = category.title,
                                identity = category.params,
                                artworkUrl = categoryArtwork[category.params].orEmpty(),
                                prominent = prominent,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { onOpenCategory(category) }
                            )
                        }
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (fallbackGenres.isNotEmpty()) {
                item(key = "editorial-genres-header") {
                    ExploreCategorySectionHeader(strings.genres)
                }
                items(
                    items = fallbackGenres.chunked(2),
                    key = { pair -> "editorial-genres-${pair.joinToString("|") { it.id }}" }
                ) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(102.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { zone ->
                            ExploreDiscoveryCategoryCard(
                                title = zone.label,
                                identity = zone.id,
                                emoji = zone.emoji,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onClick = { onOpenZone(zone) }
                            )
                        }
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (!isLoading && sections.isEmpty() && fallbackGenres.isEmpty()) {
                item(key = "moods-and-genres-empty") {
                    Text(
                        text = strings.exploreEmpty,
                        color = LevyraMuted,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 42.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreCategorySectionHeader(title: String) {
    Text(
        text = title,
        color = LevyraText,
        fontSize = 22.sp,
        lineHeight = LevyraTypeRhythm.lineHeight(22.sp),
        fontWeight = FontWeight.Black,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp)
            .semantics { heading() }
    )
}

@Composable
private fun ExploreDiscoveryCategoryCard(
    title: String,
    identity: String,
    artworkUrl: String = "",
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    emoji: String = "",
    onClick: () -> Unit
) {
    val (accentStart, accentEnd) = exploreCategoryPalette(identity)
    val shape = RoundedCornerShape(14.dp)
    val artworkSize = if (prominent) 86.dp else 78.dp
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accentStart,
                        accentEnd,
                        accentEnd.copy(alpha = 0.88f)
                    )
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            LevyraBlack.copy(alpha = 0.18f),
                            LevyraBlack.copy(alpha = 0.02f),
                            LevyraBlack.copy(alpha = 0.10f)
                        )
                    )
                )
        )

        if (artworkUrl.isNotBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 14.dp, y = 12.dp)
                    .size(artworkSize)
                    .rotate(13f)
                    .clip(RoundedCornerShape(9.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 14.dp, y = 12.dp)
                    .size(artworkSize)
                    .rotate(13f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (emoji.isNotBlank()) {
                    Text(
                        text = emoji,
                        fontSize = if (prominent) 30.sp else 26.sp,
                        maxLines = 1
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(if (prominent) 38.dp else 34.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    )
                }
            }
        }

        Text(
            text = title,
            color = Color.White,
            fontSize = if (prominent) 17.sp else 16.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(if (prominent) 17.sp else 16.sp),
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(if (prominent) 0.62f else 0.60f)
                .padding(start = 14.dp, top = 14.dp, end = 4.dp)
        )
    }
}

@Composable
private fun ExploreDiscoveryCategoryPlaceholder(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        LevyraPanelSoft,
                        LevyraPanel,
                        LevyraPanel.copy(alpha = 0.92f)
                    )
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), shape)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 14.dp, y = 12.dp)
                .size(78.dp)
                .rotate(13f)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.06f))
        )
    }
}

@Composable
private fun exploreCategoryPalette(identity: String): Pair<Color, Color> {
    val palette = listOf(
        Color(0xFF7C3AED) to Color(0xFF4C1D95),
        Color(0xFFD81B60) to Color(0xFF7A1538),
        Color(0xFF0F8A78) to Color(0xFF075E54),
        Color(0xFF246BCE) to Color(0xFF173F7A),
        Color(0xFFB96A16) to Color(0xFF70400F),
        Color(0xFF238636) to Color(0xFF145325),
        Color(0xFFB335B5) to Color(0xFF64236C),
        Color(0xFFD9571C) to Color(0xFF7B3215)
    )
    return palette[(identity.hashCode() and Int.MAX_VALUE) % palette.size]
}

@Composable
private fun ExploreDestinationSurface(
    title: String,
    subtitle: String?,
    strings: LevyraStrings,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraBlack)
            .zIndex(24f)
    ) {
        content(PaddingValues(top = statusBarTop + ExploreDestinationHeaderHeight))
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .background(LevyraBlack.copy(alpha = 0.96f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(LevyraPanel, CircleShape)
                    .semantics { role = Role.Button }
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.back,
                    tint = LevyraText,
                    modifier = Modifier.size(21.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = LevyraText,
                    fontSize = 21.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(21.sp),
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.takeIf(String::isNotBlank)?.let { detail ->
                    Text(
                        text = detail,
                        color = LevyraMuted,
                        fontSize = 12.5.sp,
                        lineHeight = LevyraTypeRhythm.lineHeight(12.5.sp),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun ExploreDestinationTrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(if (isCurrent) LevyraCyan.copy(alpha = 0.11f) else LevyraPanel.copy(alpha = 0.72f))
            .border(
                BorderStroke(
                    1.dp,
                    if (isCurrent) LevyraCyan.copy(alpha = 0.44f) else Color.White.copy(alpha = 0.08f)
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = track.thumbnailUrl.ifBlank { track.largeThumbnailUrl },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(9.dp))
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = track.title,
                color = LevyraText,
                fontSize = 14.5.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(14.5.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = LevyraMuted,
                fontSize = 12.5.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(12.5.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier.size(38.dp).background(
                if (isCurrent) LevyraCyan else Color.White.copy(alpha = 0.08f),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (isCurrent) LevyraBlack else LevyraText,
                modifier = Modifier.size(if (isPlaying) 20.dp else 21.dp)
            )
        }
    }
}
