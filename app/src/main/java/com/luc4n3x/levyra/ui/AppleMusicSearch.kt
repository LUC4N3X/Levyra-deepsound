package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.apple.AppleCatalogArtwork
import com.luc4n3x.levyra.data.apple.AppleCatalogItem
import com.luc4n3x.levyra.data.apple.AppleExploreCatalog
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraAppleMusicStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign
import com.luc4n3x.levyra.viewmodel.AppleMusicEditorialViewModel
import com.luc4n3x.levyra.viewmodel.ExploreViewModel
import com.luc4n3x.levyra.viewmodel.LevyraUiState

private val AppleSearchTitleBarHeight = 55.dp
private val AppleSearchFieldHeight = 48.dp
private val AppleSearchFieldPadding = 10.dp
private val AppleSearchGridTop = 16.dp
internal val AppleSearchGridGap = 12.dp
private const val AppleSearchCuratorSectionIndex = -7
private const val AppleSearchArtworkWidth = 640
private const val AppleSearchArtworkHeight = 360
private const val AppleCategoryArtworkPrefetch = 24
private const val AppleSearchModeMs = 250

@Immutable
internal class AppleSearchTile(
    val key: String,
    val label: String,
    val destination: String,
    val artworkUrl: String = "",
    val tint: Int? = null,
    val toned: Boolean = false,
    val zone: ExploreZone? = null,
    val artworkParams: String = ""
)

internal fun appleSearchTiles(
    apple: List<AppleCatalogItem>,
    categories: List<ExploreCategory>,
    categoryArtwork: Map<String, String>,
    zones: List<ExploreZone>
): List<AppleSearchTile> {
    val claimed = HashSet<String>()
    val pairedParams = HashSet<String>()
    val providerCategories = categories
        .filter { it.title.isNotBlank() && it.params.isNotBlank() }
        .distinctBy { it.params }
    val appleTiles = apple.mapNotNull { item ->
        val identity = AppleExploreCatalog.identity(item.title)
        if (identity.isBlank() || !claimed.add(identity)) return@mapNotNull null
        val partner = providerCategories.firstOrNull { AppleExploreCatalog.curatorId(it.params) == item.id }
            ?: providerCategories.firstOrNull { category ->
                AppleExploreCatalog.curatorId(category.params) == null && AppleExploreCatalog.identity(category.title) == identity
            }
        if (partner != null) {
            pairedParams += partner.params
            claimed += AppleExploreCatalog.identity(partner.title)
        }
        AppleSearchTile(
            key = "apple-${item.id}",
            label = item.title,
            destination = if (partner != null) exploreCategoryDestination(partner)
            else exploreCategoryDestination(ExploreCategory(item.title, "apple-curator:${item.id}", sectionIndex = AppleSearchCuratorSectionIndex)),
            artworkUrl = AppleCatalogArtwork.url(item.artworkTemplate, AppleSearchArtworkWidth, AppleSearchArtworkHeight),
            tint = item.backgroundColor,
            toned = true
        )
    }
    val categoryTiles = providerCategories
        .filter { it.params !in pairedParams && claimed.add(AppleExploreCatalog.identity(it.title)) }
        .map { category ->
            AppleSearchTile(
                key = "category-${category.params}",
                label = category.title,
                destination = exploreCategoryDestination(category),
                artworkUrl = categoryArtwork[category.params].orEmpty(),
                artworkParams = category.params
            )
        }
    val zoneTiles = zones
        .filter { it.id != ExploreCatalog.NEW_RELEASES_ZONE_ID && claimed.add(AppleExploreCatalog.identity(it.label)) }
        .distinctBy { it.id }
        .map { zone ->
            AppleSearchTile(
                key = "zone-${zone.id}",
                label = zone.label,
                destination = exploreMoodDestination(zone.id),
                tint = zone.accentStart,
                zone = zone
            )
        }
    return appleTiles + categoryTiles + zoneTiles
}

internal fun appleSearchColumns(widthDp: Int): Int = when {
    widthDp >= 1195 -> 6
    widthDp >= 982 -> 5
    widthDp >= 727 -> 4
    widthDp >= 501 -> 3
    else -> 2
}

@Immutable
internal class AppleMusicSearchCatalog(val tiles: List<AppleSearchTile>, val loading: Boolean)

@Composable
internal fun rememberAppleMusicSearchCatalog(state: LevyraUiState, ensureExplore: (com.luc4n3x.levyra.ui.i18n.LevyraStrings) -> Unit): AppleMusicSearchCatalog {
    val strings = LocalLevyraStrings.current
    val editorial = viewModel<AppleMusicEditorialViewModel>()
    val editorialState by editorial.state.collectAsStateWithLifecycle()
    val storefront = remember(state.selectedChartId) { ChartsCatalog.region(state.selectedChartId).country }
    LaunchedEffect(storefront, strings.code) { editorial.loadBrowse(storefront, strings.code) }
    LaunchedEffect(strings) { ensureExplore(strings) }
    val zones = remember(strings) { ExploreCatalog.getZones(strings) }
    val tiles = remember(editorialState.browse, state.exploreCategories, state.exploreCategoryArtwork, zones) {
        appleSearchTiles(editorialState.browse, state.exploreCategories, state.exploreCategoryArtwork, zones)
    }
    return AppleMusicSearchCatalog(tiles, editorialState.browseLoading)
}

@Composable
internal fun AppleMusicSearchHeader(
    query: String,
    recognitionAvailable: Boolean,
    recognitionBusy: Boolean,
    onQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onSettings: () -> Unit,
    onVoice: () -> Unit,
    onRecognize: () -> Unit,
    focus: MutableState<Boolean>?
) {
    val strings = LocalLevyraStrings.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { focus?.value = focused }
    val active = focused || query.isNotEmpty()
    var menuOpen by remember { mutableStateOf(false) }
    val titleHeight by androidx.compose.animation.core.animateDpAsState(
        if (active) AppleSearchActiveTop else AppleSearchTitleBarHeight + 1.dp,
        androidx.compose.animation.core.tween(AppleSearchModeMs), label = "apple-search-title-height"
    )
    val titleAlpha by androidx.compose.animation.core.animateFloatAsState(
        if (active) 0f else 1f, androidx.compose.animation.core.tween(AppleSearchModeMs), label = "apple-search-title-alpha"
    )
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().height(titleHeight).clipToBounds()
                .alpha(titleAlpha)
        ) {
        Box(Modifier.fillMaxWidth().height(AppleSearchTitleBarHeight)) {
            Text(
                strings.search, color = AppleMusicDesign.label,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterStart).padding(end = 48.dp).semantics { heading() }
            )
            Box(Modifier.align(Alignment.CenterEnd).offset(x = 18.dp)) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, strings.actions, tint = AppleMusicDesign.accent)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(strings.voice) }, onClick = { menuOpen = false; onVoice() })
                    if (recognitionAvailable) DropdownMenuItem(
                        text = { Text(strings.recognizeMusic, color = if (recognitionBusy) AppleMusicDesign.accent else Color.Unspecified) },
                        onClick = { menuOpen = false; onRecognize() }
                    )
                    DropdownMenuItem(text = { Text(strings.settings) }, onClick = { menuOpen = false; onSettings() })
                }
            }
        }
        HorizontalDivider(Modifier.appleGutterBleed(), thickness = 1.dp, color = AppleMusicDesign.opaqueSeparator)
        }
        Row(
            Modifier.padding(vertical = AppleSearchFieldPadding).fillMaxWidth().height(AppleSearchFieldHeight)
                .clip(RoundedCornerShape(AppleMusicDesign.cornerL)).background(AppleMusicDesign.fill),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (active) IconButton(
                onClick = {
                    onClear()
                    focusManager.clearFocus()
                    keyboard?.hide()
                },
                modifier = Modifier.size(AppleSearchFieldHeight).focusProperties { canFocus = false }
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, tint = AppleMusicDesign.accent)
            }
            val textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 22.sp, color = AppleMusicDesign.label)
            val suffix = remember(strings.code) { appleSearchCopy(strings.code).inCatalog }
            val transformation = remember(focused, suffix) {
                if (focused) VisualTransformation.None else AppleSearchSuffixTransformation(suffix)
            }
            Box(Modifier.weight(1f).padding(start = if (active) 14.dp else 16.dp), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) Text(
                    appleSearchCopy(strings.code).hint, style = textStyle, color = AppleMusicDesign.label.copy(alpha = 0.6f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    interactionSource = interaction,
                    textStyle = textStyle,
                    visualTransformation = transformation,
                    cursorBrush = SolidColor(AppleMusicDesign.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        keyboard?.hide()
                        onSearch(query)
                    }),
                    modifier = Modifier.fillMaxWidth()
                        .semantics { contentDescription = strings.search }
                )
            }
            if (query.isNotEmpty()) IconButton(onClick = onClear, modifier = Modifier.size(AppleSearchFieldHeight).focusProperties { canFocus = false }) {
                Icon(Icons.Rounded.Close, strings.clear, tint = AppleSearchMutedText)
            }
        }
    }
}

internal fun LazyListScope.appleMusicSearchLanding(
    catalog: AppleMusicSearchCatalog,
    columns: Int,
    onOpen: (AppleSearchTile) -> Unit,
    onRequestArtwork: (String) -> Unit
) {
    if (catalog.tiles.isEmpty()) {
        item(key = "apple-search-landing-status", contentType = "apple-search-landing-status") {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                if (catalog.loading) CircularProgressIndicator(color = AppleMusicDesign.accent)
            }
        }
        return
    }
    itemsIndexed(
        items = catalog.tiles.chunked(columns),
        key = { index, _ -> "apple-search-row-$index" },
        contentType = { _, _ -> "apple-search-row" }
    ) { index, row ->
        Row(
            Modifier.fillMaxWidth().padding(top = if (index == 0) AppleSearchGridTop else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(AppleSearchGridGap)
        ) {
            row.forEach { tile -> AppleSearchCategoryTile(tile, Modifier.weight(1f), onOpen, onRequestArtwork) }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun AppleSearchCategoryTile(
    tile: AppleSearchTile,
    modifier: Modifier,
    onOpen: (AppleSearchTile) -> Unit,
    onRequestArtwork: (String) -> Unit
) {
    if (tile.artworkParams.isNotBlank()) LaunchedEffect(tile.artworkParams) { onRequestArtwork(tile.artworkParams) }
    val artwork = tile.zone?.let { rememberExploreMoodArtworkUrl(it) } ?: tile.artworkUrl
    AppleMusicCategoryTile(
        label = tile.label, portrait = artwork, active = false, onClick = { onOpen(tile) }, onLongClick = null,
        longClickLabel = tile.label, onPortraitError = {}, modifier = modifier,
        tint = tile.tint?.let(::Color), toned = tile.toned
    )
}

@Composable
internal fun appleMusicSearchColumns(): Int = appleSearchColumns(LocalConfiguration.current.screenWidthDp)

@Composable
internal fun AppleMusicSearchDestination(
    destination: String?,
    exploreViewModel: ExploreViewModel,
    backEnabled: Boolean,
    onTrackActions: (Track) -> Unit,
    onClose: () -> Unit
) {
    destination ?: return
    val exploreState by exploreViewModel.state.collectAsStateWithLifecycle()
    val strings = LocalLevyraStrings.current
    val category = exploreCategoryDestinationValue(destination)
    val zone = remember(strings, destination) {
        exploreMoodDestinationId(destination)?.let { id -> ExploreCatalog.getZones(strings).firstOrNull { it.id == id } }
    }
    Box(Modifier.fillMaxSize().background(AppleMusicDesign.background).pointerInput(Unit) {}) {
        if (category != null) {
            val curator = category.sectionIndex == AppleSearchCuratorSectionIndex
            AppleMusicGenreScreen(
                title = category.title,
                categoryParams = if (curator) "" else category.params,
                languageCode = exploreState.languageCode,
                storefront = ChartsCatalog.region(exploreState.selectedChartId).country,
                currentId = exploreState.currentTrack?.id,
                bottomInset = exploreDestinationBottomInset(exploreState),
                exploreViewModel = exploreViewModel,
                onTrackActions = onTrackActions,
                onOpenPlaylist = exploreViewModel::openPlaylistHit,
                backEnabled = backEnabled,
                onBack = onClose,
                fallback = {
                    if (!curator) AppleMusicTrackCategoryScreen(
                        title = category.title, identity = "category:${category.params}",
                        active = exploreState.exploreCategoryParams == category.params,
                        state = exploreState, exploreViewModel = exploreViewModel, backEnabled = backEnabled,
                        onTrackActions = onTrackActions, onBack = onClose,
                        onSelect = { exploreViewModel.selectExploreCategory(category) }
                    )
                }
            )
        } else if (zone != null) {
            AppleMusicTrackCategoryScreen(
                title = zone.label, identity = "zone:${zone.id}", active = exploreState.exploreZoneId == zone.id,
                state = exploreState, exploreViewModel = exploreViewModel, backEnabled = backEnabled,
                onTrackActions = onTrackActions, onBack = onClose,
                onSelect = { exploreViewModel.selectExploreZone(zone) }
            )
        }
    }
}

@Composable
private fun AppleMusicTrackCategoryScreen(
    title: String,
    identity: String,
    active: Boolean,
    state: LevyraUiState,
    exploreViewModel: ExploreViewModel,
    backEnabled: Boolean,
    onTrackActions: (Track) -> Unit,
    onBack: () -> Unit,
    onSelect: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAppleMusicStrings.forCode(strings.code) }
    LaunchedEffect(identity, state.languageCode, active) { if (!active) onSelect() }
    BackHandler(enabled = backEnabled, onBack = onBack)
    val ready = active && !state.isExploreLoading
    val tracks = if (ready) state.exploreTracks else emptyList()
    val albums = remember(tracks) {
        tracks.filter { it.album.isNotBlank() && it.albumBrowseId.isNotBlank() }.distinctBy { it.albumBrowseId }
    }
    LaunchedEffect(tracks) { tracks.take(AppleCategoryArtworkPrefetch).forEach(exploreViewModel::ensureExploreTrackArtwork) }
    val listState = rememberLazyListState()
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    var menuOpen by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(AppleMusicDesign.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp,
                bottom = exploreDestinationBottomInset(state) + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AppleMusicDesign.sectionGap)
        ) {
            item(key = "category-title") {
                Text(
                    title, color = AppleMusicDesign.label,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = AppleMusicDesign.gutter).semantics { heading() }
                )
            }
            if (tracks.isEmpty()) {
                item(key = "category-status") {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        if (!ready) CircularProgressIndicator(color = AppleMusicDesign.accent)
                        else AppleMusicNotice(copy.collectionUnavailable, copy.editorialRetry, onSelect)
                    }
                }
            } else {
                item(key = "category-songs", contentType = "apple-songs") {
                    Column {
                        AppleMusicSectionHeader(strings.songs)
                        AppleMusicSongColumns(
                            tracks, 4, state.currentTrack?.id, copy.explicit,
                            { track -> exploreViewModel.playFrom(tracks, track) }, onTrackActions
                        )
                    }
                }
                if (albums.size > 1) item(key = "category-albums", contentType = "apple-cards") {
                    Column {
                        AppleMusicSectionHeader(strings.albumsPlain)
                        AppleMusicCardCarousel(remember(albums) {
                            albums.map { track ->
                                AppleMusicCardModel(
                                    key = "album-${track.albumBrowseId}", title = track.album, subtitle = track.artist,
                                    artworkUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }, explicit = track.explicit,
                                    onClick = { exploreViewModel.openAlbum(trackAlbumHit(track)) }
                                )
                            }
                        }, explicitLabel = copy.explicit)
                    }
                }
            }
        }
        AppleMusicNavigationBar(title, collapsed, onBack)
        Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 8.dp).height(52.dp)) {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.align(Alignment.Center)) {
                Icon(Icons.Rounded.MoreVert, strings.actions, tint = AppleMusicDesign.accent)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(strings.playAll) }, enabled = tracks.isNotEmpty(), onClick = {
                    menuOpen = false
                    exploreViewModel.playAll(tracks)
                })
                DropdownMenuItem(text = { Text(strings.shuffle) }, enabled = tracks.isNotEmpty(), onClick = {
                    menuOpen = false
                    exploreViewModel.playAll(tracks.shuffled())
                })
            }
        }
    }
}
