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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.desktop.app.state.CollectionUiState
import com.luc4n3x.levyra.desktop.app.ui.components.CollectionCard
import com.luc4n3x.levyra.desktop.app.ui.components.EmptyState
import com.luc4n3x.levyra.desktop.app.ui.components.ErrorBanner
import com.luc4n3x.levyra.desktop.app.ui.components.LoadingRow
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.core.model.ArtistDetail
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import java.util.Locale

@Composable
fun ArtistScreen(
    state: CollectionUiState,
    actions: TrackActions,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onEnqueueAll: () -> Unit,
    onOpenCollection: (CollectionRef) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val artist = state.artist

    when {
        artist == null && state.loading -> {
            Column(
                modifier = modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ArtistBackRow(title = state.ref?.title.orEmpty(), onBack = onBack)
                LoadingRow(label = strings.loading)
            }
            return
        }

        artist == null && state.error.isNotBlank() -> {
            Column(
                modifier = modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ArtistBackRow(title = state.ref?.title.orEmpty(), onBack = onBack)
                ErrorBanner(message = state.error, actionLabel = strings.close, onAction = onBack)
            }
            return
        }

        artist == null -> {
            EmptyState(
                icon = LevyraIcons.Disc,
                title = strings.searchNoResults,
                modifier = modifier
            )
            return
        }
    }

    ScrollableColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ArtistHero(
                artist = artist,
                onBack = onBack,
                onPlayAll = onPlayAll,
                onShuffleAll = onShuffleAll,
                onEnqueueAll = onEnqueueAll
            )
        }

        if (state.error.isNotBlank()) {
            item {
                ErrorBanner(message = state.error, actionLabel = strings.close, onAction = onBack)
            }
        }

        if (artist.biography.isNotBlank()) {
            item {
                ArtistBiographyCard(artist.biography)
            }
        }

        if (artist.tracks.isNotEmpty()) {
            item { ArtistSectionTitle(strings.homeTopTracks) }
            itemsIndexed(
                artist.tracks.take(MAX_VISIBLE_TRACKS),
                key = { _, track -> "artist-track-${track.id}" }
            ) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = track.id == actions.currentTrackId,
                    isFavorite = actions.isFavorite(track),
                    onPlay = { actions.onPlay(artist.tracks, index) },
                    onPlayNext = { actions.onPlayNext(track) },
                    onEnqueue = { actions.onEnqueue(track) },
                    onToggleFavorite = { actions.onToggleFavorite(track) },
                    onAddToPlaylist = { actions.onAddToPlaylist(track) },
                    position = index + 1
                )
            }
        }

        if (artist.albums.isNotEmpty()) {
            item {
                ArtistCollectionSection(
                    title = strings.filterAlbums,
                    collections = artist.albums,
                    onOpenCollection = onOpenCollection
                )
            }
        }

        if (artist.videos.isNotEmpty()) {
            item { ArtistSectionTitle(strings.filterVideos) }
            itemsIndexed(
                artist.videos.take(MAX_VISIBLE_VIDEOS),
                key = { _, track -> "artist-video-${track.id}" }
            ) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = track.id == actions.currentTrackId,
                    isFavorite = actions.isFavorite(track),
                    onPlay = { actions.onPlay(artist.videos, index) },
                    onPlayNext = { actions.onPlayNext(track) },
                    onEnqueue = { actions.onEnqueue(track) },
                    onToggleFavorite = { actions.onToggleFavorite(track) },
                    onAddToPlaylist = { actions.onAddToPlaylist(track) },
                    position = index + 1
                )
            }
        }

        if (artist.playlists.isNotEmpty()) {
            item {
                ArtistCollectionSection(
                    title = strings.filterPlaylists,
                    collections = artist.playlists,
                    onOpenCollection = onOpenCollection
                )
            }
        }

        if (artist.relatedArtists.isNotEmpty()) {
            item {
                ArtistRelatedSection(
                    title = strings.filterArtists,
                    artists = artist.relatedArtists,
                    onOpenArtist = onOpenCollection
                )
            }
        }

        if (!artist.hasContent) {
            item {
                EmptyState(icon = LevyraIcons.Disc, title = strings.searchNoResults)
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ArtistHero(
    artist: ArtistDetail,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onEnqueueAll: () -> Unit
) {
    val strings = LocalStrings.current
    val accent = LocalAccentColor.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        shadowElevation = 14.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.42f),
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
        ) {
            if (artist.bannerUrl.isNotBlank()) {
                AsyncImage(
                    model = artist.bannerUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.08f),
                                Color.Black.copy(alpha = 0.36f),
                                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.98f)
                            )
                        )
                    )
            )

            Surface(
                modifier = Modifier.padding(18.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = LevyraIcons.ChevronLeft,
                        contentDescription = strings.onboardingBack,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                ArtistPortrait(artist.portraitUrl)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (artist.subscriberCount >= 0L) {
                        Text(
                            text = "${formatArtistCount(artist.subscriberCount)} ${strings.artistSubscribers}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onPlayAll, enabled = artist.tracks.isNotEmpty()) {
                            Text(strings.playAll)
                        }
                        OutlinedButton(onClick = onShuffleAll, enabled = artist.tracks.isNotEmpty()) {
                            Text(strings.shufflePlay)
                        }
                        OutlinedButton(onClick = onEnqueueAll, enabled = artist.tracks.isNotEmpty()) {
                            Text(strings.addToQueue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistPortrait(url: String) {
    Surface(
        modifier = Modifier.size(176.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
        shadowElevation = 16.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (url.isBlank()) {
                Icon(
                    imageVector = LevyraIcons.Disc,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(54.dp)
                )
            } else {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ArtistBiographyCard(text: String) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.settingsAbout,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArtistCollectionSection(
    title: String,
    collections: List<CollectionRef>,
    onOpenCollection: (CollectionRef) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ArtistSectionTitle(title)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 18.dp)
        ) {
            items(collections, key = { it.id }) { ref ->
                CollectionCard(
                    ref = ref,
                    onClick = { onOpenCollection(ref) },
                    modifier = Modifier.width(176.dp)
                )
            }
        }
    }
}

@Composable
private fun ArtistRelatedSection(
    title: String,
    artists: List<CollectionRef>,
    onOpenArtist: (CollectionRef) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ArtistSectionTitle(title)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 18.dp)
        ) {
            items(artists, key = { it.id }) { artist ->
                Column(
                    modifier = Modifier
                        .width(132.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenArtist(artist) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(112.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        if (artist.artworkUrl.isBlank()) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = LevyraIcons.Disc,
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        } else {
                            AsyncImage(
                                model = artist.artworkUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Text(
                        text = artist.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ArtistBackRow(title: String, onBack: () -> Unit) {
    val strings = LocalStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = LevyraIcons.ChevronLeft,
                contentDescription = strings.onboardingBack,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun formatArtistCount(value: Long): String {
    val safe = value.coerceAtLeast(0L)
    return when {
        safe >= 1_000_000_000L -> String.format(Locale.ROOT, "%.1fB", safe / 1_000_000_000.0)
        safe >= 1_000_000L -> String.format(Locale.ROOT, "%.1fM", safe / 1_000_000.0)
        safe >= 1_000L -> String.format(Locale.ROOT, "%.1fK", safe / 1_000.0)
        else -> safe.toString()
    }.replace(".0", "")
}

private const val MAX_VISIBLE_TRACKS = 12
private const val MAX_VISIBLE_VIDEOS = 8
