package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistDisplayCandidates
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifyHomeStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import com.luc4n3x.levyra.viewmodel.HomeViewModel
import com.luc4n3x.levyra.viewmodel.LevyraUiState

internal fun spotifyArtistRadioSeeds(state: LevyraUiState, releases: List<Track>): List<Track> =
    (state.recentListens + state.mostPlayedTracks + state.charts + releases)
        .filter { it.artist.isNotBlank() && it.id.isNotBlank() }
        .distinctBy { artistIdentityKey(spotifyRadioArtist(it)) }
        .take(12)

private fun spotifyRadioArtist(track: Track): String = artistDisplayCandidates(track.artist).firstOrNull() ?: track.artist

internal fun LazyListScope.spotifyHomeFilterItems(
    filter: SpotifyHomeFilter, state: LevyraUiState, albums: List<AlbumHit>, releases: List<Track>,
    viewModel: HomeViewModel, onActions: (Track) -> Unit
) {
    when (filter) {
        SpotifyHomeFilter.Following -> item(key = "spotify-following") { SpotifyFollowingPage(state, viewModel) }
        SpotifyHomeFilter.Podcasts -> item(key = "spotify-podcasts-page") { SpotifyPodcastsPage(state, viewModel) }
        SpotifyHomeFilter.Music -> spotifyMusicItems(state, albums, releases, viewModel, onActions)
        else -> Unit
    }
}

private fun LazyListScope.spotifyMusicItems(state: LevyraUiState, albums: List<AlbumHit>, releases: List<Track>, viewModel: HomeViewModel, onActions: (Track) -> Unit) {
    item(key = "spotify-music-radio") { SpotifyArtistRadios(state, releases, viewModel) }
    if (albums.isNotEmpty()) item(key = "spotify-music-albums") {
        SpotifyHomeShelf(LevyraSpotifyHomeStrings.forCode(LocalLevyraStrings.current.code).popularAlbums) {
            items(albums, key = { it.browseId.ifBlank { it.query } }) { album ->
                SpotifyHomeCoverCard(album.title, album.artist, album.thumbnailUrl) { viewModel.openAlbum(album) }
            }
        }
    }
    if (state.recentListens.isNotEmpty()) item(key = "spotify-music-recent") {
        val strings = LocalLevyraStrings.current
        SpotifyHomeShelf(strings.recent) {
            items(state.recentListens.distinctBy(Track::id).take(12), key = Track::id) { track ->
                SpotifyHomeCoverCard(track.title, track.artist, track.thumbnailUrl, compact = true) { viewModel.playFrom(state.recentListens, track) }
            }
        }
    }
    if (state.homeArtists.isNotEmpty()) item(key = "spotify-music-artists") {
        SpotifyHomeShelf(LevyraSpotifyHomeStrings.forCode(LocalLevyraStrings.current.code).popularArtists) {
            items(state.homeArtists.take(12), key = { it.browseId.ifBlank { it.name } }) { artist ->
                SpotifyHomeCoverCard(artist.name, "", artist.thumbnailUrl, round = true) { viewModel.openArtistFromHit(artist) }
            }
        }
    }
    if (state.recentListens.isNotEmpty()) item(key = "spotify-music-recap") {
        val strings = LocalLevyraStrings.current
        SpotifyHomeShelf(strings.listeningRecap) {
            item(key = "tracks") {
                SpotifyHomeCoverCard(strings.songs, strings.recapSubtitle, state.recentListens.first().thumbnailUrl, onClick = viewModel::openListeningRecap)
            }
            state.homeArtists.firstOrNull()?.let { artist -> item(key = "artists") {
                SpotifyHomeCoverCard(strings.topArtistsTitle, artist.name, artist.thumbnailUrl, onClick = viewModel::openListeningRecap)
            } }
        }
    }
    if (releases.isNotEmpty()) item(key = "spotify-music-releases") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SpotifyHomeHeading(LocalLevyraStrings.current.newReleases)
            SpotifyTrackCarousel(releases.take(12), state.currentTrack?.id, state.isPlaying, state.isResolving, { viewModel.playFrom(releases, it) }, onActions)
        }
    }
}

@Composable
private fun SpotifyHomeHeading(title: String) {
    Text(title, color = SpotifyDesign.text, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp).semantics { heading() })
}

@Composable
private fun SpotifyHomeShelf(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SpotifyHomeHeading(title)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
private fun SpotifyHomeCoverCard(title: String, subtitle: String, url: String, round: Boolean = false, compact: Boolean = false, onClick: () -> Unit) {
    val size = if (compact) 104.dp else 176.dp
    Column(Modifier.width(size).levyraPressable(onClick = onClick, onClickLabel = title, role = Role.Button),
        horizontalAlignment = if (round) Alignment.CenterHorizontally else Alignment.Start) {
        StableRemoteArtwork(url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(if (round) CircleShape else RoundedCornerShape(4.dp)).background(SpotifyDesign.placeholder))
        Text(title, color = SpotifyDesign.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2,
            overflow = TextOverflow.Ellipsis, textAlign = if (round) TextAlign.Center else TextAlign.Start, modifier = Modifier.padding(top = 8.dp))
        if (subtitle.isNotBlank()) Text(subtitle, color = SpotifyDesign.subdued, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SpotifyArtistRadios(state: LevyraUiState, releases: List<Track>, viewModel: HomeViewModel) {
    val strings = LocalLevyraStrings.current
    val seeds = remember(state.recentListens, state.mostPlayedTracks, state.charts, releases) { spotifyArtistRadioSeeds(state, releases) }
    if (seeds.isEmpty()) return
    SpotifyHomeShelf(LevyraSpotifyHomeStrings.forCode(strings.code).popularRadio) {
        items(seeds, key = Track::id) { track ->
            val name = spotifyRadioArtist(track)
            val portrait = state.homeArtists.firstOrNull { artistIdentityKey(it.name) == artistIdentityKey(name) }?.thumbnailUrl ?: track.thumbnailUrl
            SpotifyArtistRadioCard(name, portrait, track.artist) { SpotifyRadioNavigator.open(name, track) }
        }
    }
}

@Composable
private fun SpotifyArtistRadioCard(name: String, portrait: String, subtitle: String, onClick: () -> Unit) {
    val palette = listOf(Color(0xFFF59DD2), Color(0xFFCF9CF5), Color(0xFF95E0CD), Color(0xFFECB66D))
    val fill = palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
    Column(Modifier.width(176.dp).levyraPressable(onClick = onClick, onClickLabel = name, role = Role.Button)) {
        Box(Modifier.size(176.dp).clip(RoundedCornerShape(4.dp)).background(fill)) {
            Text(LocalLevyraStrings.current.continuousRadio, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            StableRemoteArtwork(portrait, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.align(Alignment.Center).size(112.dp).clip(CircleShape))
            Text(name, color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
        }
        Text(subtitle, color = SpotifyDesign.subdued, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun SpotifyFollowingPage(state: LevyraUiState, viewModel: HomeViewModel) {
    val strings = LocalLevyraStrings.current
    val copy = LevyraSpotifyHomeStrings.forCode(strings.code)
    var choosing by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SpotifyHomeHeading(copy.latestReleases)
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF1F1F1F)).padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SpotifyFollowingPortraits(state.homeArtists)
            Text(copy.betterRecommendations, color = Color.White, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(copy.chooseDescription, color = SpotifyDesign.subdued, fontSize = 16.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
            Box(Modifier.padding(top = 6.dp).clip(CircleShape).background(Color.White)
                .levyraPressable(onClick = { choosing = true }, onClickLabel = copy.chooseArtists, role = Role.Button).padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(copy.chooseArtists, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        state.releaseRadar.take(12).forEach { entry ->
            Row(Modifier.fillMaxWidth().levyraPressable(onClick = { viewModel.openReleaseRadarEntry(entry) }, onClickLabel = entry.release.title, role = Role.Button)
                .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                StableRemoteArtwork(entry.release.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(entry.release.title, color = SpotifyDesign.text, fontWeight = FontWeight.Bold)
                    Text(entry.artistName, color = SpotifyDesign.subdued)
                }
            }
        }
    }
    if (choosing) SpotifyFollowArtistPicker(state, viewModel) { choosing = false }
}

@Composable
private fun SpotifyFollowingPortraits(artists: List<ArtistHit>) {
    Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
        artists.take(5).forEachIndexed { index, artist ->
            val size = if (index == 2) 88.dp else if (index == 1 || index == 3) 76.dp else 60.dp
            StableRemoteArtwork(artist.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier =                 Modifier.offset(x = ((index - 2) * 38).dp).size(size).clip(CircleShape))
        }
    }
}

@Composable
private fun SpotifyFollowArtistPicker(state: LevyraUiState, viewModel: HomeViewModel, onDismiss: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val chart = remember(strings.code) { com.luc4n3x.levyra.ui.i18n.LevyraSpotifyChartStrings.forCode(strings.code) }
    var query by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    val artists = remember(state.homeArtists, state.similarArtists, state.followedArtists, query) {
        (state.homeArtists + state.similarArtists).filter { it.browseId.isNotBlank() }.distinctBy { it.browseId }
            .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    }
    LaunchedEffect(Unit) { if (state.homeArtists.isEmpty()) viewModel.refreshHomeArtists() }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize().background(SpotifyDesign.base)) {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "picker-title", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    Text(
                        com.luc4n3x.levyra.ui.i18n.LevyraSpotifyChartStrings.pickerTitle(strings.code), color = Color.White,
                        fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp,
                        modifier = Modifier.padding(top = 20.dp).semantics { heading() }
                    )
                }
                item(key = "picker-search", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 16.dp).height(44.dp).clip(RoundedCornerShape(4.dp)).background(Color.White).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(Icons.Rounded.Search, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                        Box(Modifier.weight(1f).padding(start = 10.dp)) {
                            if (query.isEmpty()) Text(chart.search, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            androidx.compose.foundation.text.BasicTextField(
                                value = query, onValueChange = { query = it }, singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                artists.forEach { artist ->
                    item(key = "picker-${artist.browseId}") {
                        val followed = selected[artist.browseId] ?: state.followedArtists.any { it.browseId == artist.browseId || it.name.equals(artist.name, ignoreCase = true) }
                        Column(
                            Modifier.levyraPressable(onClick = {
                                selected[artist.browseId] = !followed
                                viewModel.toggleFollowArtist(artist)
                            },
                                onClickLabel = if (followed) strings.followingArtist else strings.followArtist, role = Role.Checkbox),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp).aspectRatio(1f)) {
                                StableRemoteArtwork(
                                    artist.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape).background(SpotifyDesign.placeholder)
                                        .graphicsLayer { alpha = if (followed) 0.6f else 1f }
                                )
                                if (followed) Box(
                                    Modifier.align(Alignment.TopEnd).size(28.dp).clip(CircleShape).background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) { androidx.compose.material3.Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(20.dp)) }
                            }
                            Text(
                                artist.name, color = Color.White, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                }
                if (query.isBlank()) item(key = "picker-more") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().padding(horizontal = 6.dp).aspectRatio(1f).clip(CircleShape).background(Color(0xFF2D46B9))
                                .levyraPressable(onClick = viewModel::refreshHomeArtists, onClickLabel = com.luc4n3x.levyra.ui.i18n.LevyraSpotifyChartStrings.pickerMore(strings.code), role = Role.Button)
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                com.luc4n3x.levyra.ui.i18n.LevyraSpotifyChartStrings.pickerMore(strings.code), color = Color.White,
                                fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                                maxLines = 3, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp).height(56.dp).clip(CircleShape).background(Color.White)
                    .levyraPressable(onClick = onDismiss, onClickLabel = chart.done, role = Role.Button).padding(horizontal = 36.dp),
                contentAlignment = Alignment.Center
            ) { Text(chart.done, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SpotifyPodcastsPage(state: LevyraUiState, viewModel: HomeViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(context) { com.luc4n3x.levyra.data.YoutubePodcastsRepository(context.applicationContext) }
    var attempt by remember { mutableIntStateOf(0) }
    val loaded by androidx.compose.runtime.produceState<List<com.luc4n3x.levyra.data.PodcastShelf>?>(null, state.languageCode, attempt) {
        value = null
        value = try {
            repository.shelves(state.languageCode)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            emptyList()
        }
    }
    val shelves = loaded
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        when {
            shelves == null -> Text(LocalLevyraStrings.current.searchingYouTubeMusic, color = SpotifyDesign.subdued, modifier = Modifier.padding(16.dp))
            shelves.isEmpty() -> Box(
                Modifier.padding(16.dp).height(40.dp).clip(CircleShape).background(Color.White)
                    .levyraPressable(onClick = { attempt++ }, onClickLabel = LocalLevyraStrings.current.exploreSamplesRetry, role = Role.Button)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) { Text(LocalLevyraStrings.current.exploreSamplesRetry, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
        shelves.orEmpty().forEach { shelf ->
            SpotifyHomeShelf(shelf.title) {
                items(shelf.episodes, key = { "podcast-${shelf.title}-${it.id}" }) { episode ->
                    SpotifyHomeCoverCard(episode.title, episode.artist, episode.thumbnailUrl) { viewModel.playFrom(shelf.episodes, episode) }
                }
            }
        }
    }
}
