package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel

private val ProductPanelShape = RoundedCornerShape(24.dp)
private val ProductCardShape = RoundedCornerShape(18.dp)

@Composable
internal fun LevyraProductShell(
    viewModel: LevyraViewModel,
    isInPictureInPicture: Boolean,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LevyraStrings.forCode(state.languageCode)
    val layoutDirection = if (LevyraLanguageCatalog.isRtl(strings.code)) LayoutDirection.Rtl else LayoutDirection.Ltr
    var showSettingsHub by rememberSaveable { mutableStateOf(false) }
    val blockingOverlay = state.showOnboarding || state.showSettings || state.showAlbum || state.showArtist ||
        state.showQueue || state.showLyrics || state.showAudioQualityPanel || state.showUpdatePrompt ||
        state.sharedMediaPreview != null || state.openPlaylist != null
    val showNavigation = shouldShowProductNavigation(
        isInPictureInPicture = isInPictureInPicture,
        selectedTab = state.selectedTab,
        hasBlockingOverlay = blockingOverlay || showSettingsHub
    )

    BackHandler(enabled = showSettingsHub) {
        showSettingsHub = false
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(modifier = modifier.fillMaxSize()) {
            if (!isInPictureInPicture && !blockingOverlay && state.selectedTab == LevyraTab.Home) {
                ProductHomeHeader(
                    state = state,
                    strings = strings,
                    onSearch = { viewModel.selectTab(LevyraTab.Search) },
                    onLibrary = { viewModel.selectTab(LevyraTab.Library) },
                    onSettings = { showSettingsHub = true }
                )
            }

            if (!isInPictureInPicture && !blockingOverlay && state.selectedTab == LevyraTab.Search) {
                ProductSearchScreen(
                    state = state,
                    strings = strings,
                    onQueryChange = viewModel::setQuery,
                    onSearch = viewModel::searchNow,
                    onFilter = viewModel::setSearchFilter,
                    onPlay = viewModel::play,
                    onArtist = viewModel::openArtistFromHit,
                    onAlbum = viewModel::searchAlbum,
                    onClose = { viewModel.selectTab(LevyraTab.Home) }
                )
            }

            if (showSettingsHub && !isInPictureInPicture && !blockingOverlay) {
                ProductSettingsHub(
                    state = state,
                    strings = strings,
                    onClose = { showSettingsHub = false },
                    onOpenAllSettings = {
                        showSettingsHub = false
                        viewModel.openSettings()
                    },
                    onAnimations = viewModel::setAnimationsEnabled,
                    onDynamicColor = viewModel::setDynamicColor,
                    onSponsorBlock = viewModel::setSponsorBlock,
                    onSkipSilence = viewModel::setSkipSilence,
                    onCheckUpdates = { viewModel.checkForUpdates() }
                )
            }

            if (showNavigation) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    state.currentTrack?.let { track ->
                        ProductMiniPlayer(
                            track = track,
                            state = state,
                            strings = strings,
                            onOpen = { viewModel.selectTab(LevyraTab.Player) },
                            onPrevious = viewModel::previous,
                            onTogglePlay = viewModel::togglePlay,
                            onNext = viewModel::next
                        )
                    }
                    ProductBottomNavigation(
                        selected = state.selectedTab,
                        strings = strings,
                        onSelect = viewModel::selectTab,
                        onSettings = { showSettingsHub = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductHomeHeader(
    state: LevyraUiState,
    strings: LevyraStrings,
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = ProductPanelShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Levyra",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        text = strings.quickPicks,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ProductHeaderAction(Icons.Rounded.Search, strings.search, onSearch)
                ProductHeaderAction(Icons.Rounded.LibraryMusic, strings.library, onLibrary)
                ProductHeaderAction(Icons.Rounded.Settings, strings.settings, onSettings)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProductMetric(
                    label = strings.recent,
                    value = state.recentListens.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                ProductMetric(
                    label = strings.favorites,
                    value = state.favorites.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                ProductMetric(
                    label = strings.downloads,
                    value = state.downloads.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProductHeaderAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = label)
    }
}

@Composable
private fun ProductMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(text = value, fontWeight = FontWeight.Bold)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProductSearchScreen(
    state: LevyraUiState,
    strings: LevyraStrings,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFilter: (SearchFilter) -> Unit,
    onPlay: (Track) -> Unit,
    onArtist: (ArtistHit) -> Unit,
    onAlbum: (AlbumHit) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = productOverlayBottomPadding(state.currentTrack != null).dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    placeholder = { Text(strings.searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = strings.search) },
                    trailingIcon = {
                        if (state.query.isNotBlank()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = strings.clear)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() })
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = strings.back)
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ProductFilterChip(strings.all, state.searchFilter == SearchFilter.All) {
                        onFilter(SearchFilter.All)
                    }
                }
                item {
                    ProductFilterChip(strings.songs, state.searchFilter == SearchFilter.Songs) {
                        onFilter(SearchFilter.Songs)
                    }
                }
                item {
                    ProductFilterChip(strings.artists, state.searchFilter == SearchFilter.Artists) {
                        onFilter(SearchFilter.Artists)
                    }
                }
                item {
                    ProductFilterChip(strings.albumsPlain, state.searchFilter == SearchFilter.Albums) {
                        onFilter(SearchFilter.Albums)
                    }
                }
            }

            if (state.isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            }

            ProductSearchResults(
                state = state,
                strings = strings,
                onPlay = onPlay,
                onArtist = onArtist,
                onAlbum = onAlbum,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProductFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun ProductSearchResults(
    state: LevyraUiState,
    strings: LevyraStrings,
    onPlay: (Track) -> Unit,
    onArtist: (ArtistHit) -> Unit,
    onAlbum: (AlbumHit) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = state.searchData
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        state.searchError?.let { error ->
            item {
                Surface(shape = ProductCardShape, color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        data.topTrack?.takeIf { state.searchFilter in setOf(SearchFilter.All, SearchFilter.Songs) }?.let { track ->
            item {
                ProductFeaturedTrack(track = track, strings = strings, onPlay = onPlay)
            }
        }

        val songs = when (state.searchFilter) {
            SearchFilter.All, SearchFilter.Songs -> data.songs
            else -> emptyList()
        }
        if (songs.isNotEmpty()) {
            item { ProductSectionTitle(strings.songs) }
            itemsIndexed(
                items = songs,
                key = { index, track -> "song:${track.id.ifBlank { "${track.artist}|${track.title}" }}:$index" }
            ) { _, track ->
                ProductTrackRow(track = track, strings = strings, onClick = { onPlay(track) })
            }
        }

        val artists = when (state.searchFilter) {
            SearchFilter.All, SearchFilter.Artists -> data.artists
            else -> emptyList()
        }
        if (artists.isNotEmpty()) {
            item { ProductSectionTitle(strings.artists) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(
                        items = artists,
                        key = { index, artist -> "artist:${artist.browseId.ifBlank { artist.name }}:$index" }
                    ) { _, artist ->
                        ProductArtistCard(artist = artist, onClick = { onArtist(artist) })
                    }
                }
            }
        }

        val albums = when (state.searchFilter) {
            SearchFilter.All, SearchFilter.Albums -> data.albums
            else -> emptyList()
        }
        if (albums.isNotEmpty()) {
            item { ProductSectionTitle(strings.albumsPlain) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(
                        items = albums,
                        key = { index, album -> "album:${album.browseId.ifBlank { "${album.artist}|${album.title}" }}:$index" }
                    ) { _, album ->
                        ProductAlbumCard(album = album, onClick = { onAlbum(album) })
                    }
                }
            }
        }

        if (!state.isSearching && state.query.length >= 2 && data.isEmpty) {
            item {
                Text(
                    text = strings.exploreEmpty,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (state.query.length < 2 && state.recentSearches.isNotEmpty()) {
            item { ProductSectionTitle(strings.recentSearches) }
            itemsIndexed(
                items = state.recentSearches,
                key = { index, track -> "recent:${track.id.ifBlank { "${track.artist}|${track.title}" }}:$index" }
            ) { _, track ->
                ProductTrackRow(track = track, strings = strings, onClick = { onPlay(track) })
            }
        }
    }
}

@Composable
private fun ProductSectionTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun ProductFeaturedTrack(track: Track, strings: LevyraStrings, onPlay: (Track) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onPlay(track) },
        shape = ProductPanelShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductArtwork(url = track.thumbnailUrl, label = track.title, size = 78)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = strings.quickPicks, style = MaterialTheme.typography.labelMedium)
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Rounded.PlayArrow, contentDescription = strings.play, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun ProductTrackRow(track: Track, strings: LevyraStrings, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = ProductCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductArtwork(url = track.thumbnailUrl, label = track.title, size = 54)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatProductDuration(track.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(Icons.Rounded.PlayArrow, contentDescription = strings.play)
        }
    }
}

@Composable
private fun ProductArtistCard(artist: ArtistHit, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(116.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist.thumbnailUrl,
            contentDescription = artist.name,
            modifier = Modifier.size(92.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProductAlbumCard(album: AlbumHit, onClick: () -> Unit) {
    Column(modifier = Modifier.width(142.dp).clickable(onClick = onClick)) {
        ProductArtwork(url = album.thumbnailUrl, label = album.title, size = 142)
        Spacer(modifier = Modifier.height(8.dp))
        Text(album.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProductArtwork(url: String, label: String, size: Int) {
    AsyncImage(
        model = url,
        contentDescription = label,
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun ProductSettingsHub(
    state: LevyraUiState,
    strings: LevyraStrings,
    onClose: () -> Unit,
    onOpenAllSettings: () -> Unit,
    onAnimations: (Boolean) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onSponsorBlock: (Boolean) -> Unit,
    onSkipSilence: (Boolean) -> Unit,
    onCheckUpdates: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.settings, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text(strings.settingsSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.back)
                    }
                }
            }

            item {
                ProductSettingsSection(icon = Icons.Rounded.Palette, title = strings.design) {
                    ProductSwitchRow(strings.animations, strings.animationsSubtitle, state.animationsEnabled, onAnimations)
                    HorizontalDivider()
                    ProductSwitchRow(strings.dynamicColor, strings.dynamicColorSubtitle, state.dynamicColor, onDynamicColor)
                }
            }

            item {
                ProductSettingsSection(icon = Icons.Rounded.Bolt, title = strings.playback) {
                    ProductSwitchRow(strings.sponsorBlock, strings.sponsorBlockSubtitle, state.sponsorBlockEnabled, onSponsorBlock)
                    HorizontalDivider()
                    ProductSwitchRow(strings.skipSilence, strings.skipSilenceSubtitle, state.skipSilence, onSkipSilence)
                }
            }

            item {
                ProductSettingsSection(icon = Icons.Rounded.GraphicEq, title = strings.audioEngine) {
                    ProductInfoRow(strings.equalizer, strings.equalizerSubtitle, state.audioSettings.presetId)
                    HorizontalDivider()
                    ProductInfoRow(strings.audioQuality, strings.playback, state.audioQuality)
                }
            }

            item {
                ProductSettingsSection(icon = Icons.Rounded.Download, title = strings.downloads) {
                    ProductInfoRow(strings.downloads, strings.downloadsInProgress, state.downloads.size.toString())
                    HorizontalDivider()
                    ProductInfoRow(strings.offlineDownloadsPlain, strings.downloadsFolder, formatProductBytes(state.downloadStorageBytes))
                }
            }

            item {
                ProductSettingsSection(icon = Icons.Rounded.Tune, title = strings.app) {
                    ProductInfoRow(strings.theme, strings.themeSubtitle, state.themePreset)
                    HorizontalDivider()
                    ProductInfoRow(strings.appFont, strings.appFontSubtitle, state.interfaceSettings.fontPreset.displayName)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onCheckUpdates, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.checkNewVersions)
                    }
                    Button(onClick = onOpenAllSettings, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.settings)
                    }
                }
            }

            state.updateMessage?.let { message ->
                item {
                    Surface(shape = ProductCardShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(message, modifier = Modifier.padding(14.dp))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ProductSettingsSection(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ProductPanelShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ProductSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ProductInfoRow(title: String, subtitle: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = value,
            modifier = Modifier.widthIn(max = 132.dp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProductMiniPlayer(
    track: Track,
    state: LevyraUiState,
    strings: LevyraStrings,
    onOpen: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 10.dp,
        shadowElevation = 12.dp
    ) {
        Column {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(72.dp).clickable(onClick = onOpen).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductArtwork(url = track.thumbnailUrl, label = track.title, size = 52)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = strings.back)
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) strings.pause else strings.play
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = strings.playNext)
                }
            }
        }
    }
}

@Composable
private fun ProductBottomNavigation(
    selected: LevyraTab,
    strings: LevyraStrings,
    onSelect: (LevyraTab) -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(PRODUCT_NAVIGATION_HEIGHT_DP.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 12.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductNavItem(Icons.Rounded.Home, strings.home, selected == LevyraTab.Home, Modifier.weight(1f)) { onSelect(LevyraTab.Home) }
            ProductNavItem(Icons.Rounded.Search, strings.search, selected == LevyraTab.Search, Modifier.weight(1f)) { onSelect(LevyraTab.Search) }
            ProductNavItem(Icons.Rounded.Explore, strings.explore, selected == LevyraTab.Explore, Modifier.weight(1f)) { onSelect(LevyraTab.Explore) }
            ProductNavItem(Icons.Rounded.LibraryMusic, strings.library, selected == LevyraTab.Library, Modifier.weight(1f)) { onSelect(LevyraTab.Library) }
            ProductNavItem(Icons.Rounded.Settings, strings.settings, false, Modifier.weight(1f), onSettings)
        }
    }
}

@Composable
private fun ProductNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(180),
        label = "productNavBackground"
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "productNavForeground"
    )
    Column(
        modifier = modifier
            .widthIn(min = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = foreground, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            color = foreground,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatProductDuration(durationMs: Long): String {
    if (durationMs <= 0L) return ""
    val totalSeconds = durationMs / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun formatProductBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val megabytes = bytes.toDouble() / (1024.0 * 1024.0)
    return if (megabytes < 1024.0) {
        "${megabytes.toInt()} MB"
    } else {
        val gigabytes = megabytes / 1024.0
        "${(gigabytes * 10.0).toInt() / 10.0} GB"
    }
}
