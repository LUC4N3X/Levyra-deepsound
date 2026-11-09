package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.PodcastShelf
import com.luc4n3x.levyra.data.PodcastShow
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.podcast.PodcastEpisodeProgress
import com.luc4n3x.levyra.feature.podcast.PodcastLibraryUiState
import com.luc4n3x.levyra.feature.podcast.PodcastLibraryViewModel
import com.luc4n3x.levyra.feature.podcast.PodcastShowState
import com.luc4n3x.levyra.feature.podcast.resumePositionMs
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraWavyProgress
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.PodcastCopy
import com.luc4n3x.levyra.ui.i18n.podcastCopy

private val PodcastGutter = 20.dp
private val PodcastCardWidth = 248.dp
private const val PodcastWideRatio = 16f / 9f
private val PodcastRowArtwork = 60.dp
private const val PodcastPaneMillis = 320

internal class PodcastPlayback(val currentTrackId: String?, val isPlaying: Boolean)

@Composable
internal fun PodcastLibraryScreen(
    languageCode: String,
    playback: PodcastPlayback,
    bottomInset: Dp,
    backEnabled: Boolean,
    onBack: () -> Unit,
    onPlay: (List<Track>, Track, Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val factory = remember(context.applicationContext) { PodcastLibraryViewModel.factory(context.applicationContext) }
    val viewModel: PodcastLibraryViewModel = composeViewModel(key = "levyra-podcasts", factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val copy = remember(languageCode) { podcastCopy(languageCode) }
    val backLabel = LocalLevyraStrings.current.back
    LaunchedEffect(languageCode) { viewModel.activate(languageCode) }
    BackHandler(enabled = backEnabled) { if (state.openShow != null) viewModel.closeShow() else onBack() }

    val contentPadding = PaddingValues(
        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 76.dp,
        bottom = bottomInset
    )
    val progress = state.progressById
    val play: (List<Track>, Track) -> Unit = { list, episode -> onPlay(list, episode, resumePositionMs(progress[episode.id])) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AnimatedContent(
            targetState = state.openShow?.show?.browseId,
            transitionSpec = {
                val opening = targetState != null
                val enter = fadeIn(tween(PodcastPaneMillis)) + slideInHorizontally(tween(PodcastPaneMillis)) { if (opening) it / 6 else -it / 6 }
                val exit = fadeOut(tween(PodcastPaneMillis / 2)) + slideOutHorizontally(tween(PodcastPaneMillis)) { if (opening) -it / 6 else it / 6 }
                enter togetherWith exit
            },
            label = "podcast-pane"
        ) { browseId ->
            val open = state.openShow
            if (browseId != null && open != null) {
                PodcastShowPane(open, state, copy, playback, viewModel, play, contentPadding)
            } else {
                PodcastLibraryPane(state, copy, playback, viewModel, play, contentPadding)
            }
        }
        PodcastTopBar(
            title = state.openShow?.show?.title ?: copy.title,
            backLabel = backLabel,
            onBack = { if (state.openShow != null) viewModel.closeShow() else onBack() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun PodcastLibraryPane(
    state: PodcastLibraryUiState,
    copy: PodcastCopy,
    playback: PodcastPlayback,
    viewModel: PodcastLibraryViewModel,
    play: (List<Track>, Track) -> Unit,
    contentPadding: PaddingValues
) {
    val strings = LocalLevyraStrings.current
    val retry = strings.exploreSamplesRetry
    var genre by rememberSaveable { mutableStateOf<String?>(null) }
    val activeGenre = genre?.takeIf { selected -> state.discover.any { it.title == selected } }
    val shelves = if (activeGenre == null) state.discover else state.discover.filter { it.title == activeGenre }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "podcast-search") { PodcastSearchField(state.query, copy.searchHint, viewModel::setQuery) }
        if (state.query.trim().length >= 2) {
            podcastSearchResults(state, copy, retry, viewModel)
            return@LazyColumn
        }
        if (state.discover.size > 1) {
            item(key = "podcast-genres") {
                PodcastGenreRow(
                    genres = state.discover.map { it.title },
                    selected = activeGenre,
                    allLabel = strings.all,
                    onSelect = { genre = it }
                )
            }
        }
        val featuredShelf = shelves.firstOrNull { it.episodes.isNotEmpty() }
        if (featuredShelf != null) {
            item(key = "podcast-featured") {
                val featured = featuredShelf.episodes.first()
                PodcastFeaturedCard(featured, featuredShelf.title, state.progressById[featured.id], copy, playback) {
                    play(featuredShelf.episodes, featured)
                }
            }
        }
        val resume = state.continueListening
        if (resume.isNotEmpty()) {
            item(key = "podcast-resume-title") { PodcastSectionTitle(copy.continueListening) }
            item(key = "podcast-resume") {
                PodcastRail(resume, key = { "resume-${it.track.id}" }) { item ->
                    PodcastEpisodeCard(item.track, item, copy, playback) { play(resume.map { it.track }, item.track) }
                }
            }
        }
        item(key = "podcast-subscriptions-title") { PodcastSectionTitle(copy.subscriptions) }
        item(key = "podcast-subscriptions") {
            if (state.subscriptions.isEmpty()) {
                PodcastNotice(copy.emptyLibrary)
            } else {
                PodcastRail(state.subscriptions, key = { "sub-${it.browseId}" }) { show -> PodcastShowCard(show) { viewModel.openShow(show) } }
            }
        }
        if (state.history.isNotEmpty()) {
            item(key = "podcast-history-title") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { PodcastSectionTitle(copy.history) }
                    TextButton(onClick = viewModel::clearHistory, modifier = Modifier.padding(end = 12.dp)) { Text(copy.clearHistory) }
                }
            }
            val historyTracks = state.history.map { it.track }
            itemsIndexed(state.history, key = { _, item -> "history-${item.track.id}" }, contentType = { _, _ -> "podcast-episode" }) { index, item ->
                PodcastEpisodeRow(index, state.history.size, item.track, item, copy, playback,
                    onPlay = { play(historyTracks, item.track) },
                    onSetPlayed = { viewModel.setPlayed(item.track, it) },
                    onRemove = { viewModel.removeFromHistory(item.track.id) })
            }
        }
        item(key = "podcast-discover-title") { PodcastSectionTitle(copy.discover) }
        when {
            state.discoverLoading && state.discover.isEmpty() -> item(key = "podcast-discover-loading") { PodcastLoading() }
            state.discoverFailed && state.discover.isEmpty() -> item(key = "podcast-discover-failed") {
                PodcastNotice(copy.loadFailed, retry, viewModel::retryDiscover)
            }
        }
        podcastDiscoverShelves(shelves, featuredShelf, state, copy, playback, play)
    }
}

private fun LazyListScope.podcastDiscoverShelves(
    shelves: List<PodcastShelf>,
    featuredShelf: PodcastShelf?,
    state: PodcastLibraryUiState,
    copy: PodcastCopy,
    playback: PodcastPlayback,
    play: (List<Track>, Track) -> Unit
) {
    shelves.forEach { shelf ->
        val railEpisodes = if (shelf === featuredShelf) shelf.episodes.drop(1) else shelf.episodes
        if (railEpisodes.isEmpty()) return@forEach
        item(key = "discover-title-${shelf.title}") {
            Text(shelf.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = PodcastGutter, end = PodcastGutter, top = 18.dp, bottom = 10.dp).semantics { heading() })
        }
        item(key = "discover-${shelf.title}") {
            PodcastRail(railEpisodes, key = { "discover-${shelf.title}-${it.id}" }) { episode ->
                PodcastEpisodeCard(episode, state.progressById[episode.id], copy, playback) { play(shelf.episodes, episode) }
            }
        }
    }
}

private fun LazyListScope.podcastSearchResults(
    state: PodcastLibraryUiState,
    copy: PodcastCopy,
    retry: String,
    viewModel: PodcastLibraryViewModel
) {
    when {
        state.searching && state.results.isEmpty() -> item(key = "podcast-search-loading") { PodcastLoading() }
        state.searchFailed -> item(key = "podcast-search-failed") { PodcastNotice(copy.loadFailed, retry) { viewModel.setQuery(state.query) } }
        state.results.isEmpty() -> item(key = "podcast-search-empty") { PodcastNotice(copy.noResults) }
    }
    val subscribed = state.subscribedIds
    itemsIndexed(state.results, key = { _, show -> "result-${show.browseId}" }, contentType = { _, _ -> "podcast-show" }) { index, show ->
        PodcastShowRow(index, state.results.size, show, show.browseId in subscribed, copy,
            onOpen = { viewModel.openShow(show) }, onToggle = { viewModel.toggleSubscription(show) })
    }
}

@Composable
private fun PodcastShowPane(
    open: PodcastShowState,
    state: PodcastLibraryUiState,
    copy: PodcastCopy,
    playback: PodcastPlayback,
    viewModel: PodcastLibraryViewModel,
    play: (List<Track>, Track) -> Unit,
    contentPadding: PaddingValues
) {
    val retry = LocalLevyraStrings.current.exploreSamplesRetry
    val episodes = open.page?.episodes?.episodes.orEmpty()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "show-header") {
            PodcastShowHeader(open.show, open.page?.description.orEmpty(), open.show.browseId in state.subscribedIds, copy) {
                viewModel.toggleSubscription(open.show)
            }
        }
        when {
            open.loading -> item(key = "show-loading") { PodcastLoading() }
            open.failed -> item(key = "show-failed") { PodcastNotice(copy.loadFailed, retry, viewModel::retryShow) }
            else -> item(key = "show-episodes-title") { PodcastSectionTitle(copy.episodes) }
        }
        itemsIndexed(episodes, key = { _, episode -> "episode-${episode.id}" }, contentType = { _, _ -> "podcast-episode" }) { index, episode ->
            PodcastEpisodeRow(index, episodes.size, episode, state.progressById[episode.id], copy, playback,
                onPlay = { play(episodes, episode) },
                onSetPlayed = { viewModel.setPlayed(episode, it) },
                onRemove = null)
        }
        val continuation = open.page?.episodes?.continuation
        if (continuation != null) {
            item(key = "show-more-$continuation") {
                LaunchedEffect(continuation) { viewModel.loadMoreEpisodes() }
                PodcastLoading()
            }
        }
    }
}

@Composable
private fun PodcastTopBar(title: String, backLabel: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().background(colors.surface).statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LevyraBackButton(onClick = onBack, contentDescription = backLabel)
        AnimatedContent(targetState = title, label = "podcast-title", modifier = Modifier.weight(1f)) { text ->
            Text(text, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = colors.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
        }
    }
}

@Composable
private fun PodcastSearchField(query: String, hint: String, onQueryChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = PodcastGutter, vertical = 8.dp),
        singleLine = true,
        shape = CircleShape,
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = colors.onSurfaceVariant) },
        placeholder = { Text(hint, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainerHighest,
            unfocusedContainerColor = colors.surfaceContainerHighest,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun PodcastSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = PodcastGutter, end = PodcastGutter, top = 20.dp, bottom = 10.dp).semantics { heading() }
    )
}

@Composable
private fun <T> PodcastRail(items: List<T>, key: (T) -> Any, content: @Composable (T) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = PodcastGutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = key) { content(it) }
    }
}

@Composable
private fun PodcastArtwork(url: String, size: Dp, corner: Dp) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(size).clip(RoundedCornerShape(corner)).background(colors.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Podcasts, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(size / 3))
        if (url.isNotBlank()) AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun PodcastEpisodeCard(episode: Track, progress: PodcastEpisodeProgress?, copy: PodcastCopy, playback: PodcastPlayback, onPlay: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val corners = podcastSkinCorners()
    val corner = levyraExpressiveCorner(interaction, rest = corners.card, pressed = corners.cardPressed, label = "podcast-episode-corner")
    Column(
        Modifier.width(PodcastCardWidth).levyraPressable(
            onClick = onPlay, role = Role.Button, onClickLabel = episode.title, pressedScale = LevyraPressScale.Surface, interactionSource = interaction
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box {
            PodcastWideArtwork(episode.largeThumbnailUrl.ifBlank { episode.thumbnailUrl }, PodcastCardWidth, corner)
            if (playback.currentTrackId == episode.id && playback.isPlaying) {
                PodcastNowPlayingBadge(Modifier.align(Alignment.BottomEnd).padding(8.dp))
            } else {
                PodcastPlayDisc(Modifier.align(Alignment.BottomEnd).padding(8.dp), size = 34.dp)
            }
        }
        Text(episode.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        PodcastProgressLine(progress, copy, fallback = episode.artist)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PodcastNoticeShape(): androidx.compose.ui.graphics.Shape =
    if (com.luc4n3x.levyra.ui.theme.LevyraIsDefaultSkin) androidx.compose.material3.MaterialShapes.Cookie9Sided.toShape() else CircleShape

private data class PodcastSkinCorners(val hero: Dp, val heroPressed: Dp, val card: Dp, val cardPressed: Dp)

private fun podcastSkinCorners(): PodcastSkinCorners = when {
    com.luc4n3x.levyra.ui.theme.LevyraIsDefaultSkin -> PodcastSkinCorners(28.dp, 44.dp, 18.dp, 32.dp)
    com.luc4n3x.levyra.ui.theme.LevyraIsAtelier -> com.luc4n3x.levyra.ui.theme.AtelierDesign.cardCorner.let { PodcastSkinCorners(it, it, it, it) }
    com.luc4n3x.levyra.ui.theme.LevyraIsMono -> PodcastSkinCorners(com.luc4n3x.levyra.ui.theme.MonoDesign.cardCorner, com.luc4n3x.levyra.ui.theme.MonoDesign.cardCorner, com.luc4n3x.levyra.ui.theme.MonoDesign.artCorner, com.luc4n3x.levyra.ui.theme.MonoDesign.artCorner)
    com.luc4n3x.levyra.ui.theme.LevyraIsQobuz -> com.luc4n3x.levyra.ui.theme.QobuzDesign.cardCorner.let { PodcastSkinCorners(it, it, it, it) }
    else -> {
        val base = com.luc4n3x.levyra.ui.theme.LevyraHomeDesign.HeroCorner
        PodcastSkinCorners(base.coerceAtMost(16.dp), base.coerceAtMost(16.dp), base.coerceAtMost(12.dp), base.coerceAtMost(12.dp))
    }
}

@Composable
private fun PodcastGenreRow(genres: List<String>, selected: String?, allLabel: String, onSelect: (String?) -> Unit) {
    val options = listOf<String?>(null) + genres.distinct()
    LazyRow(
        contentPadding = PaddingValues(horizontal = PodcastGutter, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options, key = { it ?: "podcast-genre-all" }) { option ->
            PodcastGenreChip(label = option ?: allLabel, selected = option == selected) { onSelect(option) }
        }
    }
}

@Composable
private fun PodcastGenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val colorSpec: androidx.compose.animation.core.AnimationSpec<Color> =
        if (LocalAnimationsEnabled.current) androidx.compose.animation.core.spring() else androidx.compose.animation.core.snap()
    val container by animateColorAsState(if (selected) colors.onSurface else colors.surfaceContainerHigh, colorSpec, label = "podcast-genre-container")
    val content by animateColorAsState(if (selected) colors.surface else colors.onSurface, colorSpec, label = "podcast-genre-content")
    Box(
        Modifier.heightIn(min = 40.dp).clip(CircleShape).background(container)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = content, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun PodcastWideArtwork(url: String, width: Dp, corner: Dp) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.width(width).aspectRatio(PodcastWideRatio).clip(RoundedCornerShape(corner)).background(colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Podcasts, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(36.dp))
        if (url.isNotBlank()) AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun PodcastPlayDisc(modifier: Modifier = Modifier, size: Dp) {
    Box(modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(size * 0.56f))
    }
}

@Composable
private fun PodcastFeaturedCard(
    episode: Track,
    overline: String,
    progress: PodcastEpisodeProgress?,
    copy: PodcastCopy,
    playback: PodcastPlayback,
    onPlay: () -> Unit
) {
    val playing = playback.currentTrackId == episode.id && playback.isPlaying
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val corners = podcastSkinCorners()
    val corner = levyraExpressiveCorner(interaction, rest = corners.hero, pressed = corners.heroPressed, label = "podcast-featured-corner")
    Column(
        Modifier.padding(horizontal = PodcastGutter, vertical = 8.dp).fillMaxWidth()
            .clip(RoundedCornerShape(corner)).background(colors.surfaceContainerHigh)
            .levyraPressable(onClick = onPlay, role = Role.Button, onClickLabel = episode.title, pressedScale = LevyraPressScale.Surface, interactionSource = interaction)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(PodcastWideRatio).background(colors.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Podcasts, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(40.dp))
            val url = episode.largeThumbnailUrl.ifBlank { episode.thumbnailUrl }
            if (url.isNotBlank()) AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(overline.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.primary, maxLines = 1)
                Text(episode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                PodcastProgressLine(progress, copy, fallback = episode.artist)
            }
            if (playing) {
                PodcastNowPlayingBadge()
            } else {
                Box(Modifier.size(52.dp).clip(PodcastNoticeShape()).background(colors.onSurface), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = colors.surface, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@Composable
private fun PodcastShowCard(show: PodcastShow, onOpen: () -> Unit) {
    Column(
        Modifier.width(120.dp).levyraPressable(onClick = onOpen, role = Role.Button, onClickLabel = show.title, pressedScale = LevyraPressScale.Surface),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PodcastArtwork(show.thumbnailUrl, 120.dp, 28.dp)
        Text(show.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PodcastShowRow(index: Int, count: Int, show: PodcastShow, subscribed: Boolean, copy: PodcastCopy, onOpen: () -> Unit, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.padding(horizontal = PodcastGutter, vertical = 1.dp).fillMaxWidth().clip(levyraGroupedListShape(index, count))
            .background(colors.surfaceContainer)
            .levyraPressable(onClick = onOpen, role = Role.Button, onClickLabel = show.title, pressedScale = LevyraPressScale.Row)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PodcastArtwork(show.thumbnailUrl, PodcastRowArtwork, 16.dp)
        Column(Modifier.weight(1f)) {
            Text(show.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (show.author.isNotBlank()) Text(show.author, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        PodcastSubscribeButton(subscribed, copy, onToggle)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PodcastShowHeader(show: PodcastShow, description: String, subscribed: Boolean, copy: PodcastCopy, onToggle: () -> Unit) {
    if (com.luc4n3x.levyra.ui.theme.LevyraIsMono) return MonoPodcastShowHeader(show, description, subscribed, copy, onToggle)
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = PodcastGutter, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(132.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(colors.surfaceContainerHigh)) {
                if (show.thumbnailUrl.isNotBlank()) AsyncImage(show.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(show.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = colors.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (show.author.isNotBlank()) Text(show.author, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                PodcastSubscribeButton(subscribed, copy, onToggle)
            }
        }
        if (description.isNotBlank()) {
            var expanded by remember(show.browseId) { mutableStateOf(false) }
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.levyraPressable(onClick = { expanded = !expanded }, pressedScale = LevyraPressScale.Row)
            )
        }
    }
}

@Composable
private fun PodcastSubscribeButton(subscribed: Boolean, copy: PodcastCopy, onToggle: () -> Unit) {
    val corner = levyraExpressiveToggleCorner(subscribed, unchecked = 24.dp, checkedCorner = 12.dp, label = "podcast-subscribe-corner")
    val shape = RoundedCornerShape(corner)
    val label = if (subscribed) copy.subscribed else copy.subscribe
    if (subscribed) {
        FilledTonalButton(onClick = onToggle, shape = shape, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
            Text(label, modifier = Modifier.padding(start = 6.dp), maxLines = 1)
        }
    } else {
        Button(onClick = onToggle, shape = shape, modifier = Modifier.heightIn(min = 48.dp)) { Text(label, maxLines = 1) }
    }
}

@Composable
private fun PodcastEpisodeRow(
    index: Int,
    count: Int,
    episode: Track,
    progress: PodcastEpisodeProgress?,
    copy: PodcastCopy,
    playback: PodcastPlayback,
    onPlay: () -> Unit,
    onSetPlayed: (Boolean) -> Unit,
    onRemove: (() -> Unit)?
) {
    if (com.luc4n3x.levyra.ui.theme.LevyraIsMono) return MonoPodcastEpisodeRow(episode, progress, copy, playback, onPlay, onSetPlayed, onRemove)
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    val playing = playback.currentTrackId == episode.id
    Row(
        Modifier.padding(horizontal = PodcastGutter, vertical = 1.dp).fillMaxWidth().clip(levyraGroupedListShape(index, count))
            .background(if (playing) colors.secondaryContainer else colors.surfaceContainer)
            .levyraPressable(onClick = onPlay, role = Role.Button, onClickLabel = episode.title, pressedScale = LevyraPressScale.Row)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box {
            PodcastArtwork(episode.thumbnailUrl, PodcastRowArtwork, 14.dp)
            if (playing && playback.isPlaying) PodcastNowPlayingBadge(Modifier.align(Alignment.Center))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(episode.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            PodcastProgressLine(progress, copy, fallback = episode.album.ifBlank { episode.artist })
        }
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, episode.title, tint = colors.onSurfaceVariant) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                val completed = progress?.completed == true
                DropdownMenuItem(text = { Text(if (completed) copy.markUnplayed else copy.markPlayed) }, onClick = { menuOpen = false; onSetPlayed(!completed) })
                if (onRemove != null) DropdownMenuItem(text = { Text(copy.removeFromHistory) }, onClick = { menuOpen = false; onRemove() })
            }
        }
    }
}

@Composable
private fun PodcastProgressLine(progress: PodcastEpisodeProgress?, copy: PodcastCopy, fallback: String) {
    val colors = MaterialTheme.colorScheme
    when {
        progress?.completed == true -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Rounded.CheckCircle, null, tint = colors.primary, modifier = Modifier.size(14.dp))
            Text(copy.played, style = MaterialTheme.typography.labelMedium, color = colors.primary)
        }
        progress?.inProgress == true && progress.durationMs > 0L -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LevyraWavyProgress(progress.fraction, colors.primary, colors.surfaceContainerHighest, Modifier.fillMaxWidth().height(6.dp))
            Text(copy.minutesLeft(((progress.durationMs - progress.positionMs) / 60_000L).coerceAtLeast(1L)),
                style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
        fallback.isNotBlank() -> Text(fallback, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PodcastNowPlayingBadge(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.size(34.dp).clip(PodcastNoticeShape()).background(colors.primary), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.GraphicEq, null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun PodcastLoading() {
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { LevyraLoadingIndicator(Modifier.size(48.dp)) }
}

@Composable
private fun PodcastNotice(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = PodcastGutter, vertical = 6.dp).clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainer).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(44.dp).clip(PodcastNoticeShape()).background(colors.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Podcasts, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
        }
    }
}
