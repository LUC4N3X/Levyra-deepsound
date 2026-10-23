package com.luc4n3x.levyra.ui

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Recommend
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SpeedDial
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.likedTracksByArtist
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraYouTubeMusicSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.YouTubeMusicSkinCopy
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel

private val ArtistBarHeight = 56.dp
private val ArtistTabRowHeight = 52.dp
private val ArtistReleaseCard = 172.dp
private val ArtistSubscriberPattern = Regex("""^[\d.,]+(?:\s*(?:K|M|B|Mln|Mld|mln|mld|mila|Mio|Mrd|Md))?""")

@Composable
internal fun YouTubeMusicArtistScreen(
    state: LevyraUiState,
    viewModel: LevyraViewModel,
    onTrackActions: (Track) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraYouTubeMusicSkinStrings.forCode(strings.code) }
    val profile = state.artistProfile
    val listState = rememberSaveable(state.artistListStateKey, saver = LazyListState.Saver) { LazyListState() }
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxSize().background(YouTubeMusicDesign.background)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val heroHeight = (maxWidth * 0.86f).coerceAtMost(maxHeight * 0.55f)
            val heroPx = with(density) { heroHeight.toPx() }
            val barPx = with(density) { (statusTop + ArtistBarHeight).toPx() }
            val collapse: () -> Float = {
                if (listState.firstVisibleItemIndex > 0) 1f
                else (listState.firstVisibleItemScrollOffset / (heroPx - barPx).coerceAtLeast(1f)).coerceIn(0f, 1f)
            }
            val pinned by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex >= 2 } }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = levyraTabContentBottomPadding(state.currentTrack != null) + 16.dp)
            ) {
                when {
                    state.artistLoading && profile == null -> item(key = "ytm-artist-loading") {
                        Box(Modifier.fillMaxWidth().padding(top = statusTop + 180.dp), contentAlignment = Alignment.Center) {
                            LevyraLoadingIndicator(color = Color.White)
                        }
                    }
                    profile == null || shouldShowArtistError(state.artistError != null, true) && profile.topSongs.isEmpty() && profile.albums.isEmpty() ->
                        item(key = "ytm-artist-error") {
                            Text(
                                strings.artistProfileUnavailable, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = YouTubeMusicDesign.gutter, end = YouTubeMusicDesign.gutter, top = statusTop + 96.dp)
                            )
                        }
                    else -> youTubeMusicArtistItems(profile, state, viewModel, copy, heroHeight, pinned, statusTop, onTrackActions)
                }
            }
            YouTubeMusicArtistBar(profile, state, viewModel, collapse, pinned, copy)
        }
        state.currentTrack?.let { current ->
            AlbumNowPlayingDock(
                track = current,
                isPlaying = state.isPlaying,
                isResolving = state.isResolving,
                progress = if (state.durationMs > 0L) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f,
                animated = state.animationsEnabled,
                onToggle = viewModel::togglePlay,
                onOpenPlayer = viewModel::openPlayerScreen,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, bottom = 14.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.youTubeMusicArtistItems(
    artist: ArtistProfile,
    state: LevyraUiState,
    viewModel: LevyraViewModel,
    copy: YouTubeMusicSkinCopy,
    heroHeight: Dp,
    pinned: Boolean,
    statusTop: Dp,
    onTrackActions: (Track) -> Unit
) {
    val currentId = state.currentTrack?.id
    item(key = "ytm-artist-hero", contentType = "ytm-artist-hero") { YouTubeMusicArtistHero(artist, heroHeight) }
    item(key = "ytm-artist-actions", contentType = "ytm-artist-actions") { YouTubeMusicArtistActions(artist, state, viewModel, copy) }
    stickyHeader(key = "ytm-artist-tabs", contentType = "ytm-artist-tabs") {
        YouTubeMusicArtistTabs(copy.music, Modifier.padding(top = if (pinned) statusTop + ArtistBarHeight else 0.dp))
    }
    latestRelease(artist)?.let { release ->
        item(key = "ytm-artist-promo", contentType = "ytm-artist-promo") {
            YouTubeMusicArtistPromo(release, copy.latestRelease) { viewModel.openArtistRelease(release, artist.name) }
        }
    }
    if (artist.topSongs.isNotEmpty()) {
        item(key = "ytm-artist-top", contentType = "ytm-artist-songs") {
            val songs = artist.topSongs
            ArtistSection(LocalLevyraStrings.current.popularTracks, actionLabel = copy.playAll, onAction = { viewModel.playAll(songs) }) {
                YouTubeMusicSongColumns(
                    remember(songs) {
                        songs.map { track ->
                            YouTubeMusicSongRowModel("top-${track.id}", track, track.artist, { viewModel.playFrom(songs, track) }, { onTrackActions(track) })
                        }
                    },
                    currentId, copy.explicit
                )
            }
        }
    }
    releaseShelf("albums", true, artist.albums + artist.compilations, artist, viewModel)
    releaseShelf("singles", false, artist.singles, artist, viewModel)
    if (artist.videos.isNotEmpty()) {
        item(key = "ytm-artist-videos", contentType = "ytm-artist-videos") {
            val videos = remember(artist.videos) { artist.videos.take(20) }
            ArtistSection(LocalLevyraStrings.current.video) {
                val width = youTubeMusicViewportWidth() - YouTubeMusicDesign.gutter * 3
                LazyRow(
                    contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
                    horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
                ) {
                    items(videos, key = { "video-${it.id}" }) { video ->
                        YouTubeMusicCard(
                            YouTubeMusicCardModel(
                                "video-${video.id}", video.title, video.artist, "", wide = true, track = video,
                                onClick = { viewModel.playFrom(videos, video) }, onLongClick = { onTrackActions(video) }
                            ),
                            width = width
                        )
                    }
                }
            }
        }
    }
    item(key = "ytm-artist-library", contentType = "ytm-artist-library") {
        val liked = remember(state.favorites, artist) { likedTracksByArtist(state.favorites, artist) }
        if (liked.isNotEmpty()) {
            ArtistSection(copy.fromYourLibrary) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp).clip(RoundedCornerShape(12.dp))
                        .background(YouTubeMusicDesign.surface)
                ) {
                    liked.take(3).forEach { track ->
                        YouTubeMusicSongRow(
                            track, listOf(copy.song, track.artist).joinToString(" • "), track.id == currentId, copy.explicit,
                            onClick = { viewModel.playFrom(liked, track) }, onMore = { onTrackActions(track) }
                        )
                    }
                }
            }
        }
    }
    playlistShelf("ytm-artist-playlists", artist.artistPlaylists, viewModel) { copy.playlistsBy(artist.name) }
    playlistShelf("ytm-artist-featured-on", artist.featuredPlaylists, viewModel) {
        searchDiscographyCopy(LocalLevyraStrings.current.code).featuredOn
    }
    if (artist.relatedArtists.isNotEmpty()) {
        item(key = "ytm-artist-fans", contentType = "ytm-artist-cards") {
            val related = artist.relatedArtists
            ArtistSection(copy.fansMightLike) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
                    horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
                ) {
                    items(related, key = { "fan-${it.browseId.ifBlank { it.name }}" }) { hit ->
                        YouTubeMusicArtistCircle(hit.name, hit.subscribers, hit.thumbnailUrl) { viewModel.openArtistFromHit(hit) }
                    }
                }
            }
        }
    }
    if (artist.hasBio) {
        item(key = "ytm-artist-about", contentType = "ytm-artist-about") { YouTubeMusicArtistAbout(copy.about, artist.bio) }
    }
}

private fun LazyListScope.playlistShelf(
    key: String,
    playlists: List<PlaylistHit>,
    viewModel: LevyraViewModel,
    title: @Composable () -> String
) {
    if (playlists.isEmpty()) return
    item(key = key, contentType = "ytm-artist-cards") {
        ArtistSection(title()) {
            YouTubeMusicCardCarousel(
                remember(playlists) {
                    playlists.map { playlist ->
                        YouTubeMusicCardModel(
                            "playlist-${playlist.playlistId.ifBlank { playlist.browseId.ifBlank { playlist.title } }}", playlist.title,
                            listOf(playlist.author, playlist.trackCountLabel).filter(String::isNotBlank).joinToString(" • "),
                            playlist.thumbnailUrl,
                            onClick = { viewModel.openPlaylistHit(playlist) }
                        )
                    }
                }
            )
        }
    }
}

private fun LazyListScope.releaseShelf(
    key: String,
    albums: Boolean,
    releases: List<ArtistRelease>,
    artist: ArtistProfile,
    viewModel: LevyraViewModel
) {
    if (releases.isEmpty()) return
    item(key = "ytm-artist-$key", contentType = "ytm-artist-releases") {
        val strings = LocalLevyraStrings.current
        ArtistSection(if (albums) strings.albumsPlain else strings.singlesAndEps) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = YouTubeMusicDesign.gutter),
                horizontalArrangement = Arrangement.spacedBy(YouTubeMusicDesign.itemGap)
            ) {
                items(releases, key = { "$key-${it.browseId.ifBlank { it.title }}" }) { release ->
                    YouTubeMusicReleaseCard(release) { viewModel.openArtistRelease(release, artist.name) }
                }
            }
        }
    }
}

private fun latestRelease(artist: ArtistProfile): ArtistRelease? =
    (artist.singles + artist.albums).filter { it.thumbnailUrl.isNotBlank() }
        .maxByOrNull { it.year.toIntOrNull() ?: 0 }

@Composable
private fun ArtistSection(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = YouTubeMusicDesign.sectionGap)) {
        YouTubeMusicSectionHeader(title, actionLabel = actionLabel, onAction = onAction)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun YouTubeMusicArtistHero(artist: ArtistProfile, height: Dp) {
    val artwork = artist.thumbnailUrl.ifBlank { artist.bannerUrl }
    Box(Modifier.fillMaxWidth().height(height).background(YouTubeMusicDesign.placeholder)) {
        if (artwork.isNotBlank()) StableRemoteArtwork(artwork, artist.name, Modifier.fillMaxSize(), ContentScale.Crop, highRes = true)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.32f), 0.22f to Color.Transparent,
                    0.62f to Color.Transparent, 1f to YouTubeMusicDesign.background
                )
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = YouTubeMusicDesign.gutter)) {
            Text(
                artist.name, color = YouTubeMusicDesign.text, fontSize = 40.sp, lineHeight = 44.sp,
                fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            val audience = artist.monthlyListeners.ifBlank { artist.subscribers }
            if (audience.isNotBlank()) {
                Text(
                    audience, color = Color(0xFFCCCCCC), fontSize = 16.sp, lineHeight = 20.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun YouTubeMusicArtistActions(
    artist: ArtistProfile,
    state: LevyraUiState,
    viewModel: LevyraViewModel,
    copy: YouTubeMusicSkinCopy
) {
    val strings = LocalLevyraStrings.current
    val followed = (artist.browseId.isNotBlank() && artist.browseId in state.followedArtistKeys) ||
        artist.name.trim().lowercase() in state.followedArtistKeys
    val count = remember(artist.subscribers) { ArtistSubscriberPattern.find(artist.subscribers.trim())?.value.orEmpty() }
    val songs = artist.topSongs
    Row(
        Modifier.fillMaxWidth().padding(start = YouTubeMusicDesign.gutter, end = YouTubeMusicDesign.gutter, top = 18.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val content = if (followed) Color.White else Color.Black
        Row(
            Modifier.height(40.dp).clip(YouTubeMusicDesign.pillShape)
                .background(if (followed) Color.Transparent else Color.White)
                .border(1.dp, if (followed) YouTubeMusicDesign.outline else Color.Transparent, YouTubeMusicDesign.pillShape)
                .levyraPressable(onClick = viewModel::toggleFollowArtist, role = Role.Button, pressedScale = LevyraPressScale.Control)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (followed) strings.followingArtist else copy.subscribe, color = content, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            if (count.isNotBlank()) Text(count, color = content, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        if (songs.size > 1) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(YouTubeMusicDesign.buttonFill)
                    .levyraPressable(onClick = { viewModel.playAll(songs.shuffled()) }, role = Role.Button, onClickLabel = copy.shuffle, pressedScale = LevyraPressScale.Control),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Shuffle, copy.shuffle, tint = Color.White, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
        }
        songs.firstOrNull()?.let { first ->
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(Color.White)
                    .levyraPressable(onClick = { viewModel.playArtistSong(first) }, role = Role.Button, onClickLabel = strings.play, pressedScale = LevyraPressScale.Control),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.PlayArrow, strings.play, tint = Color.Black, modifier = Modifier.size(32.dp)) }
        }
    }
}

@Composable
private fun YouTubeMusicArtistTabs(label: String, modifier: Modifier = Modifier) {
    val divider = YouTubeMusicDesign.divider
    Box(Modifier.fillMaxWidth().background(YouTubeMusicDesign.background).then(modifier)) {
        Box(
            Modifier.fillMaxWidth().height(ArtistTabRowHeight)
                .drawBehind { drawRect(divider, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx())) }
        ) {
            Column(Modifier.padding(start = 24.dp).fillMaxHeight().width(IntrinsicSize.Max), verticalArrangement = Arrangement.Bottom) {
                Text(label, color = YouTubeMusicDesign.text, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White))
            }
        }
    }
}

@Composable
private fun YouTubeMusicArtistBar(
    artist: ArtistProfile?,
    state: LevyraUiState,
    viewModel: LevyraViewModel,
    collapse: () -> Float,
    pinned: Boolean,
    copy: YouTubeMusicSkinCopy
) {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    var menu by remember(artist?.browseId) { mutableStateOf(false) }
    val background = YouTubeMusicDesign.background
    Row(
        Modifier.fillMaxWidth()
            .drawBehind { drawRect(background, alpha = if (pinned) 1f else ((collapse() - 0.7f) / 0.3f).coerceIn(0f, 1f)) }
            .statusBarsPadding().height(ArtistBarHeight).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        YouTubeMusicIconAction(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, viewModel::closeArtist)
        Text(
            artist?.name.orEmpty(), color = YouTubeMusicDesign.text, fontSize = 22.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
                .graphicsLayer { alpha = if (pinned) 1f else ((collapse() - 0.85f) / 0.15f).coerceIn(0f, 1f) }
        )
        if (artist == null) return@Row
        if (artist.browseId.isNotBlank()) {
            YouTubeMusicIconAction(Icons.Outlined.Share, copy.share, {
                runCatching {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, artist.name)
                        putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/channel/${artist.browseId}")
                    }
                    context.startActivity(Intent.createChooser(intent, copy.share).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            })
        }
        Box {
            YouTubeMusicIconAction(Icons.Rounded.MoreVert, copy.more, { menu = true })
            YouTubeMusicArtistMenu(artist, state, viewModel, menu) { menu = false }
        }
    }
}

@Composable
private fun YouTubeMusicArtistMenu(
    artist: ArtistProfile,
    state: LevyraUiState,
    viewModel: LevyraViewModel,
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val speedDialCopy = remember(strings) { strings.speedDialCopy() }
    val excluded = state.artistExclusions.excludesBrowseId(artist.browseId) || state.artistExclusions.excludesArtistName(artist.name)
    val pinned = SpeedDial.artistKey(artist.name, artist.browseId)?.let { key -> state.speedDialPins.any { it.key == key } } == true
    val full = state.speedDialPins.size >= SpeedDial.MAX_PINS
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = YouTubeMusicDesign.surface) {
        DropdownMenuItem(
            text = { Text(if (excluded) strings.includeArtist else strings.excludeArtist, color = YouTubeMusicDesign.text) },
            leadingIcon = { Icon(if (excluded) Icons.Rounded.Recommend else Icons.Rounded.DoNotDisturbOn, null, tint = YouTubeMusicDesign.text) },
            onClick = {
                onDismiss()
                viewModel.toggleExcludeArtist(artist.browseId, artist.name)
            }
        )
        DropdownMenuItem(
            text = {
                Text(
                    when {
                        pinned -> speedDialCopy.removeFromHome
                        full -> speedDialCopy.homeFull
                        else -> speedDialCopy.addToHome
                    },
                    color = YouTubeMusicDesign.text
                )
            },
            leadingIcon = { Icon(Icons.Rounded.PushPin, null, tint = YouTubeMusicDesign.text) },
            enabled = pinned || !full,
            onClick = {
                onDismiss()
                viewModel.toggleSpeedDialArtist(artist.name, artist.browseId, artist.thumbnailUrl)
            }
        )
    }
}

@Composable
private fun YouTubeMusicArtistPromo(release: ArtistRelease, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = YouTubeMusicDesign.gutter, end = YouTubeMusicDesign.gutter, top = 28.dp)
            .clip(RoundedCornerShape(12.dp)).background(YouTubeMusicDesign.surface)
            .levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Surface)
            .padding(start = 14.dp, top = 18.dp, bottom = 18.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(56.dp).clip(YouTubeMusicDesign.thumbShape).background(YouTubeMusicDesign.placeholder)) {
            StableRemoteArtwork(release.thumbnailUrl, null, Modifier.fillMaxSize(), ContentScale.Crop)
        }
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(label, color = YouTubeMusicDesign.text, fontSize = 18.sp, lineHeight = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(release.title, color = YouTubeMusicDesign.textSecondary, fontSize = 18.sp, lineHeight = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = YouTubeMusicDesign.text, modifier = Modifier.padding(horizontal = 12.dp).size(28.dp))
    }
}

@Composable
private fun YouTubeMusicReleaseCard(release: ArtistRelease, onClick: () -> Unit) {
    Column(Modifier.width(ArtistReleaseCard).levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Surface)) {
        Box(Modifier.size(ArtistReleaseCard).clip(YouTubeMusicDesign.cardShape).background(YouTubeMusicDesign.placeholder)) {
            if (release.thumbnailUrl.isNotBlank()) StableRemoteArtwork(release.thumbnailUrl, release.title, Modifier.fillMaxSize(), ContentScale.Crop, highRes = true)
        }
        Spacer(Modifier.height(10.dp))
        Text(release.title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (release.explicit) YouTubeMusicExplicitBadge("E", Modifier.padding(end = 6.dp))
            Text(
                release.year.ifBlank { release.subtitle }, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun YouTubeMusicArtistCircle(name: String, subtitle: String, imageUrl: String, onClick: () -> Unit) {
    Column(
        Modifier.width(ArtistReleaseCard).levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(ArtistReleaseCard).clip(CircleShape).background(YouTubeMusicDesign.placeholder)) {
            if (imageUrl.isNotBlank()) StableRemoteArtwork(imageUrl, name, Modifier.fillMaxSize(), ContentScale.Crop, highRes = true)
        }
        Spacer(Modifier.height(10.dp))
        Text(name, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (subtitle.isNotBlank()) {
            Text(subtitle, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun YouTubeMusicArtistAbout(title: String, bio: String) {
    var expanded by rememberSaveable(bio) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 40.dp).animateContentSize()) {
        Text(title, color = YouTubeMusicDesign.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(16.dp))
        Text(
            bio, color = YouTubeMusicDesign.textSecondary, fontSize = 18.sp, lineHeight = 24.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 6, overflow = TextOverflow.Ellipsis
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            YouTubeMusicIconAction(
                if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, title,
                { expanded = !expanded }, tint = YouTubeMusicDesign.textSecondary, iconSize = 28.dp
            )
        }
    }
}
