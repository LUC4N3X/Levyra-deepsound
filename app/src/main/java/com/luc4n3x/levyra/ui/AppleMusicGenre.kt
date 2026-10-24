package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.apple.*
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.LevyraAppleMusicStrings
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign
import com.luc4n3x.levyra.viewmodel.AppleMusicEditorialViewModel
import com.luc4n3x.levyra.viewmodel.ExploreViewModel
import com.luc4n3x.levyra.viewmodel.MoodGenrePageViewModel
import com.luc4n3x.levyra.viewmodel.appleGenreKey

@Composable
internal fun AppleMusicGenreScreen(
    title: String,
    categoryParams: String,
    languageCode: String,
    storefront: String,
    currentId: String?,
    bottomInset: Dp,
    exploreViewModel: ExploreViewModel,
    onTrackActions: (Track) -> Unit,
    onOpenPlaylist: (PlaylistHit) -> Unit,
    backEnabled: Boolean,
    onBack: () -> Unit,
    fallback: @Composable () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAppleMusicStrings.forCode(strings.code) }
    val editorialViewModel = viewModel<AppleMusicEditorialViewModel>()
    val state by editorialViewModel.state.collectAsStateWithLifecycle()
    val front = remember(storefront) { AppleStorefronts.normalize(storefront) }
    val language = remember(strings.code) { AppleStorefronts.language(strings.code) }
    val key = appleGenreKey(front, language, title, categoryParams)
    val page = state.genrePage.takeIf { state.genreKey == key }
    LaunchedEffect(key) { editorialViewModel.loadGenre(title, categoryParams, front, language) }
    DisposableEffect(key) { onDispose { editorialViewModel.closeCollection() } }
    val shelves = remember(page) { appleGenreShelves(page) }
    val appleSettledEmpty = state.genreKey == key && !state.genreLoading && shelves.isEmpty()
    val moodFallbackActive = appleSettledEmpty && categoryParams.isNotBlank()
    val moodViewModel = viewModel<MoodGenrePageViewModel>()
    val moodState by moodViewModel.state.collectAsStateWithLifecycle()
    val moodKey = "$languageCode:$categoryParams"
    LaunchedEffect(moodFallbackActive, moodKey) {
        if (moodFallbackActive) moodViewModel.load(categoryParams, languageCode)
    }
    if (moodFallbackActive && moodState.key == moodKey && moodState.failed) {
        fallback()
        return
    }
    val moodSections = moodState.sections.takeIf { moodFallbackActive && moodState.key == moodKey }.orEmpty()
    BackHandler(enabled = backEnabled, onBack = onBack)
    var seeAllId by rememberSaveable(key) { mutableStateOf<String?>(null) }
    var menuOpen by remember(key) { mutableStateOf(false) }
    val songs = remember(page) { page?.shelves.orEmpty().flatMap { shelf ->
        shelf.items.mapNotNull(AppleCatalogTracks::fromItem)
    }.distinctBy { it.id } }
    val displayTitle = page?.shelves?.firstOrNull { it.kind == AppleCatalogShelfKind.Header }
        ?.items?.firstOrNull()?.title ?: title
    val listState = rememberLazyListState()
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val openItem: (AppleCatalogItem, List<Track>) -> Unit = { item, siblings ->
        when (item.kind) {
            AppleCatalogItemKind.Album -> exploreViewModel.openAlbum(item.toAlbumHit())
            AppleCatalogItemKind.Playlist -> editorialViewModel.openCollection(item)
            AppleCatalogItemKind.Artist -> exploreViewModel.openArtistByName(item.title)
            AppleCatalogItemKind.Song -> AppleCatalogTracks.fromItem(item)?.let { track ->
                exploreViewModel.playFrom(siblings.ifEmpty { listOf(track) }, track)
            }
            AppleCatalogItemKind.Video -> AppleCatalogTracks.track(
                item.title, item.subtitle, artworkUrl = item.artworkTemplate, durationMs = item.durationMs
            )?.let { track -> exploreViewModel.playVideoFrom(listOf(track), track) }
            else -> Unit
        }
    }
    Box(Modifier.fillMaxSize().background(AppleMusicDesign.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp,
                bottom = bottomInset + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AppleMusicDesign.sectionGap)
        ) {
            item(key = "genre-title") {
                Text(
                    displayTitle, color = AppleMusicDesign.label,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = AppleMusicDesign.gutter).semantics { heading() }
                )
            }
            if (moodSections.isNotEmpty()) {
                moodSections.forEachIndexed { index, section ->
                    item(key = "genre-mood-$index", contentType = "genre-mood") {
                        Column {
                            AppleMusicSectionHeader(section.title)
                            AppleMusicCardCarousel(remember(section) {
                                section.playlists.map { playlist ->
                                    AppleMusicCardModel(
                                        key = playlist.playlistId.ifBlank { playlist.browseId },
                                        title = playlist.title,
                                        subtitle = playlist.description,
                                        artworkUrl = playlist.thumbnailUrl,
                                        onClick = { onOpenPlaylist(playlist.toHit()) }
                                    )
                                }
                            })
                        }
                    }
                }
            } else if (shelves.isEmpty()) {
                item(key = "genre-status") {
                    if (!appleSettledEmpty || moodFallbackActive) {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppleMusicDesign.accent)
                        }
                    } else {
                        AppleMusicNotice(copy.editorialUnavailable, copy.editorialRetry) {
                            editorialViewModel.loadGenre(title, categoryParams, front, language, force = true)
                        }
                    }
                }
            }
            shelves.forEachIndexed { index, shelf ->
                item(key = "genre-${shelf.shelf.id}-$index", contentType = shelf.role) {
                    if (shelf.role == AppleHomeShelfRole.Hero) {
                        AppleMusicHeroCarousel(remember(shelf) {
                            shelf.shelf.items.map { item ->
                                AppleMusicHeroModel(
                                    key = item.id, heading = item.heading, title = item.title, subtitle = item.subtitle,
                                    description = item.description, imageUrl = item.heroUrl(1200), coverUrl = "",
                                    backgroundColor = item.backgroundColor?.let { androidx.compose.ui.graphics.Color(it) },
                                    onClick = { openItem(item, emptyList()) }
                                )
                            }
                        })
                    } else if (shelf.shelf.kind == AppleCatalogShelfKind.Videos) {
                        AppleMusicGenreVideos(shelf.shelf, openItem)
                    } else {
                        AppleMusicEditorialShelf(shelf, currentId, copy.explicit, openItem, onTrackActions) { seeAllId = shelf.shelf.id }
                    }
                }
            }
        }
        AppleMusicNavigationBar(displayTitle, collapsed, onBack)
        Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 8.dp).height(52.dp)) {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.align(Alignment.Center)) {
                Icon(Icons.Rounded.MoreVert, strings.actions, tint = AppleMusicDesign.accent)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(strings.playAll) }, enabled = songs.isNotEmpty(), onClick = {
                    menuOpen = false
                    exploreViewModel.playAll(songs)
                })
                DropdownMenuItem(text = { Text(strings.shuffle) }, enabled = songs.isNotEmpty(), onClick = {
                    menuOpen = false
                    exploreViewModel.playAll(songs.shuffled())
                })
            }
        }
        shelves.firstOrNull { it.shelf.id == seeAllId }?.let { shelf ->
            AppleMusicShelfScreen(shelf, currentId, copy.explicit, bottomInset, openItem, onTrackActions) { seeAllId = null }
        }
        state.collection?.let { collection ->
            AppleMusicCollectionScreen(
                title = collection.title, curator = collection.curator, caption = "", description = collection.description,
                artworkUrl = collection.artworkUrl, artworkTrack = collection.tracks.firstOrNull(), tracks = collection.tracks,
                currentId = currentId, loading = collection.loading, failed = collection.failed, bottomInset = bottomInset,
                onDismiss = editorialViewModel::closeCollection,
                onPlay = { exploreViewModel.playFrom(collection.tracks, it) },
                onPlayAll = { exploreViewModel.playAll(collection.tracks) },
                onShuffle = { exploreViewModel.playAll(collection.tracks.shuffled()) },
                onTrackActions = onTrackActions, onRetry = editorialViewModel::retryCollection
            )
        }
    }
}

private fun appleGenreShelves(page: AppleCatalogPage?): List<AppleHomeShelf> = page?.shelves.orEmpty().mapNotNull { shelf ->
    val role = AppleMusicHomeShelves.roleOf(shelf)
        ?: AppleHomeShelfRole.Cards.takeIf { shelf.kind == AppleCatalogShelfKind.Videos }
        ?: return@mapNotNull null
    val items = shelf.items.filter { it.kind != AppleCatalogItemKind.Other }
    if (items.isEmpty()) null else AppleHomeShelf(shelf.copy(items = items), role)
}

@Composable
private fun AppleMusicGenreVideos(shelf: AppleCatalogShelf, openItem: (AppleCatalogItem, List<Track>) -> Unit) {
    val width = AppleMusicDesign.cardWidth(appleMusicViewportWidth())
    Column {
        AppleMusicSectionHeader(shelf.title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = AppleMusicDesign.gutter),
            horizontalArrangement = Arrangement.spacedBy(AppleMusicDesign.itemGap)
        ) {
            items(shelf.items, key = { "${it.kind}:${it.id}" }) { item ->
                Column(Modifier.width(width).clickable(role = Role.Button) { openItem(item, emptyList()) }) {
                    StableRemoteArtwork(
                        item.heroUrl(600), item.title,
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(AppleMusicDesign.artworkShape)
                            .background(AppleMusicDesign.placeholder), ContentScale.Crop
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(item.title, color = AppleMusicDesign.label, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.subtitle, color = AppleMusicDesign.secondaryLabel, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
