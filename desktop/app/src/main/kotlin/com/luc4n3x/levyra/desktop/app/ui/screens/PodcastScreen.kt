package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.PodcastController
import com.luc4n3x.levyra.desktop.app.state.PodcastShowState
import com.luc4n3x.levyra.desktop.app.state.PodcastUiState
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.CookieShape
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.ShelfRow
import com.luc4n3x.levyra.desktop.app.ui.components.tracksTextInputFocus
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.podcast.PodcastEpisodeProgress
import com.luc4n3x.levyra.desktop.core.podcast.PodcastLocale
import com.luc4n3x.levyra.desktop.core.podcast.PodcastShelf
import com.luc4n3x.levyra.desktop.core.podcast.PodcastShow
import com.luc4n3x.levyra.ui.i18n.PodcastCopy
import com.luc4n3x.levyra.ui.i18n.podcastCopy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedLevyraStrings

private val PodcastGutter = 28.dp
private val PodcastCardWidth = 280.dp
private val PodcastShowCardWidth = 156.dp
private const val PodcastWideRatio = 16f / 9f
private val PodcastRowArtwork = 64.dp
private const val PodcastPaneMillis = 320

private class PodcastPlayback(val currentTrackId: String?, val isPlaying: Boolean)

private class PodcastLabels(val copy: PodcastCopy, val all: String, val retry: String, val back: String)

@Composable
fun PodcastScreen(
    controller: PodcastController,
    locale: PodcastLocale,
    currentTrackId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by controller.state.collectAsState()
    val languageCode = LocalStrings.current.languageCode
    val labels = remember(languageCode) {
        val shared = SharedLevyraStrings.forCode(languageCode)
        PodcastLabels(podcastCopy(languageCode), shared.all, shared.exploreSamplesRetry, shared.back)
    }
    val playback = PodcastPlayback(currentTrackId, isPlaying)
    LaunchedEffect(locale) { controller.activate(locale) }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        PodcastTopBar(
            title = state.openShow?.show?.title ?: labels.copy.title,
            backLabel = labels.back,
            onBack = { if (state.openShow != null) controller.closeShow() else onBack() }
        )
        AnimatedContent(
            targetState = state.openShow?.show?.browseId,
            transitionSpec = {
                val opening = targetState != null
                val enter = fadeIn(tween(PodcastPaneMillis)) + slideInHorizontally(tween(PodcastPaneMillis)) { if (opening) it / 6 else -it / 6 }
                val exit = fadeOut(tween(PodcastPaneMillis / 2)) + slideOutHorizontally(tween(PodcastPaneMillis)) { if (opening) -it / 6 else it / 6 }
                enter togetherWith exit
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = "podcast-pane"
        ) { browseId ->
            val open = state.openShow
            if (browseId != null && open != null) {
                PodcastShowPane(open, state, labels, playback, controller)
            } else {
                PodcastLibraryPane(state, labels, playback, controller)
            }
        }
    }
}

@Composable
private fun PodcastLibraryPane(state: PodcastUiState, labels: PodcastLabels, playback: PodcastPlayback, controller: PodcastController) {
    val copy = labels.copy
    var genre by rememberSaveable { mutableStateOf<String?>(null) }
    val activeGenre = genre?.takeIf { selected -> state.discover.any { it.title == selected } }
    val shelves = if (activeGenre == null) state.discover else state.discover.filter { it.title == activeGenre }
    ScrollableColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item(key = "podcast-search") { PodcastSearchField(state.query, copy.searchHint, controller::setQuery) }
        if (state.query.trim().length >= 2) {
            podcastSearchResults(state, labels, controller)
            return@ScrollableColumn
        }
        if (state.discover.size > 1) {
            item(key = "podcast-genres") {
                PodcastGenreRow(state.discover.map { it.title }, activeGenre, labels.all) { genre = it }
            }
        }
        val featuredShelf = shelves.firstOrNull { it.episodes.isNotEmpty() }
        if (featuredShelf != null) {
            item(key = "podcast-featured") {
                val featured = featuredShelf.episodes.first()
                PodcastFeaturedCard(featured, featuredShelf.title, state.progressById[featured.id], copy, playback) {
                    controller.play(featuredShelf.episodes, featured)
                }
            }
        }
        val resume = state.continueListening
        if (resume.isNotEmpty()) {
            item(key = "podcast-resume-title") { PodcastSectionTitle(copy.continueListening) }
            item(key = "podcast-resume") {
                PodcastRail(resume, key = { "resume-${it.track.id}" }) { item ->
                    PodcastEpisodeCard(item.track, item, copy, playback) { controller.play(resume.map { it.track }, item.track) }
                }
            }
        }
        item(key = "podcast-subscriptions-title") { PodcastSectionTitle(copy.subscriptions) }
        item(key = "podcast-subscriptions") {
            if (state.subscriptions.isEmpty()) {
                PodcastNotice(copy.emptyLibrary)
            } else {
                PodcastRail(state.subscriptions, key = { "sub-${it.browseId}" }) { show -> PodcastShowCard(show) { controller.openShow(show) } }
            }
        }
        if (state.history.isNotEmpty()) {
            item(key = "podcast-history-title") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { PodcastSectionTitle(copy.history) }
                    TextButton(onClick = controller::clearHistory, modifier = Modifier.padding(end = PodcastGutter - 12.dp)) { Text(copy.clearHistory) }
                }
            }
            val historyTracks = state.history.map { it.track }
            itemsIndexed(state.history, key = { _, item -> "history-${item.track.id}" }, contentType = { _, _ -> "podcast-episode" }) { index, item ->
                PodcastEpisodeRow(index, state.history.size, item.track, item, copy, playback,
                    onPlay = { controller.play(historyTracks, item.track) },
                    onSetPlayed = { controller.setPlayed(item.track, it) },
                    onRemove = { controller.removeFromHistory(item.track.id) })
            }
        }
        item(key = "podcast-discover-title") { PodcastSectionTitle(copy.discover) }
        when {
            state.discoverLoading && state.discover.isEmpty() -> item(key = "podcast-discover-loading") { PodcastLoading() }
            state.discoverFailed && state.discover.isEmpty() -> item(key = "podcast-discover-failed") {
                PodcastNotice(copy.loadFailed, labels.retry, controller::retryDiscover)
            }
        }
        podcastDiscoverShelves(shelves, featuredShelf, state, copy, playback, controller)
    }
}

private fun LazyListScope.podcastDiscoverShelves(
    shelves: List<PodcastShelf>,
    featuredShelf: PodcastShelf?,
    state: PodcastUiState,
    copy: PodcastCopy,
    playback: PodcastPlayback,
    controller: PodcastController
) {
    shelves.forEachIndexed { index, shelf ->
        val railEpisodes = if (shelf === featuredShelf) shelf.episodes.drop(1) else shelf.episodes
        if (railEpisodes.isEmpty()) return@forEachIndexed
        item(key = "discover-title-$index-${shelf.title}") {
            Text(shelf.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = PodcastGutter, end = PodcastGutter, top = 18.dp, bottom = 10.dp).semantics { heading() })
        }
        item(key = "discover-$index-${shelf.title}") {
            PodcastRail(railEpisodes, key = { "discover-$index-${it.id}" }) { episode ->
                PodcastEpisodeCard(episode, state.progressById[episode.id], copy, playback) { controller.play(shelf.episodes, episode) }
            }
        }
    }
}

private fun LazyListScope.podcastSearchResults(state: PodcastUiState, labels: PodcastLabels, controller: PodcastController) {
    val copy = labels.copy
    when {
        state.searching && state.results.isEmpty() -> item(key = "podcast-search-loading") { PodcastLoading() }
        state.searchFailed -> item(key = "podcast-search-failed") { PodcastNotice(copy.loadFailed, labels.retry) { controller.setQuery(state.query) } }
        state.results.isEmpty() -> item(key = "podcast-search-empty") { PodcastNotice(copy.noResults) }
    }
    val subscribed = state.subscribedIds
    itemsIndexed(state.results, key = { _, show -> "result-${show.browseId}" }, contentType = { _, _ -> "podcast-show" }) { index, show ->
        PodcastShowRow(index, state.results.size, show, show.browseId in subscribed, copy,
            onOpen = { controller.openShow(show) }, onToggle = { controller.toggleSubscription(show) })
    }
}

@Composable
private fun PodcastShowPane(open: PodcastShowState, state: PodcastUiState, labels: PodcastLabels, playback: PodcastPlayback, controller: PodcastController) {
    val copy = labels.copy
    val episodes = open.page?.episodes?.episodes.orEmpty()
    ScrollableColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item(key = "show-header") {
            PodcastShowHeader(open.show, open.page?.description.orEmpty(), open.show.browseId in state.subscribedIds, copy) {
                controller.toggleSubscription(open.show)
            }
        }
        when {
            open.loading -> item(key = "show-loading") { PodcastLoading() }
            open.failed -> item(key = "show-failed") { PodcastNotice(copy.loadFailed, labels.retry, controller::retryShow) }
            else -> item(key = "show-episodes-title") { PodcastSectionTitle(copy.episodes) }
        }
        itemsIndexed(episodes, key = { _, episode -> "episode-${episode.id}" }, contentType = { _, _ -> "podcast-episode" }) { index, episode ->
            PodcastEpisodeRow(index, episodes.size, episode, state.progressById[episode.id], copy, playback,
                onPlay = { controller.play(episodes, episode) },
                onSetPlayed = { controller.setPlayed(episode, it) },
                onRemove = null)
        }
        val continuation = open.page?.episodes?.continuation
        if (continuation != null) {
            item(key = "show-more-$continuation") {
                LaunchedEffect(continuation) { controller.loadMoreEpisodes() }
                PodcastLoading()
            }
        }
    }
}

@Composable
private fun PodcastTopBar(title: String, backLabel: String, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().background(colors.surface).padding(start = PodcastGutter - 6.dp, end = PodcastGutter, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(colors.surfaceContainerHigh)
                .clickable(role = Role.Button, onClickLabel = backLabel, onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(LevyraIcons.ChevronLeft, backLabel, tint = colors.onSurface, modifier = Modifier.size(22.dp))
        }
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
        modifier = Modifier.padding(horizontal = PodcastGutter, vertical = 8.dp).widthIn(max = 720.dp).fillMaxWidth().tracksTextInputFocus(),
        singleLine = true,
        shape = CircleShape,
        leadingIcon = { Icon(LevyraIcons.Search, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
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
    ShelfRow(contentPadding = PaddingValues(horizontal = PodcastGutter), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(items, key = key, contentType = { "podcast-rail-item" }) { content(it) }
    }
}

@Composable
private fun expressiveCorner(interaction: MutableInteractionSource, rest: Dp, active: Dp): Dp {
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(if (hovered || pressed) active else rest, label = "podcast-corner")
    return corner
}

@Composable
private fun Modifier.podcastClick(interaction: MutableInteractionSource, label: String, onClick: () -> Unit): Modifier =
    hoverable(interaction).clickable(
        interactionSource = interaction,
        indication = LocalIndication.current,
        role = Role.Button,
        onClickLabel = label,
        onClick = onClick
    )

@Composable
private fun PodcastEpisodeCard(episode: Track, progress: PodcastEpisodeProgress?, copy: PodcastCopy, playback: PodcastPlayback, onPlay: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val corner = expressiveCorner(interaction, rest = 18.dp, active = 32.dp)
    Column(Modifier.width(PodcastCardWidth).podcastClick(interaction, episode.title, onPlay), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            Artwork(episode.artworkUrl, Modifier.width(PodcastCardWidth).aspectRatio(PodcastWideRatio), cornerRadius = corner, iconSize = 36.dp)
            if (playback.currentTrackId == episode.id && playback.isPlaying) {
                PodcastNowPlayingBadge(Modifier.align(Alignment.BottomEnd).padding(8.dp))
            } else {
                PodcastPlayDisc(Modifier.align(Alignment.BottomEnd).padding(8.dp), size = 36.dp)
            }
        }
        Text(episode.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        PodcastProgressLine(progress, copy, fallback = episode.artist)
    }
}

@Composable
private fun PodcastGenreRow(genres: List<String>, selected: String?, allLabel: String, onSelect: (String?) -> Unit) {
    val options = listOf<String?>(null) + genres.distinct()
    ShelfRow(contentPadding = PaddingValues(horizontal = PodcastGutter, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options, key = { it ?: "podcast-genre-all" }, contentType = { "podcast-genre" }) { option ->
            PodcastGenreChip(option ?: allLabel, option == selected) { onSelect(option) }
        }
    }
}

@Composable
private fun PodcastGenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) colors.onSurface else colors.surfaceContainerHigh, label = "podcast-genre-container")
    val content by animateColorAsState(if (selected) colors.surface else colors.onSurface, label = "podcast-genre-content")
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
private fun PodcastPlayDisc(modifier: Modifier = Modifier, size: Dp) {
    Box(modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
        Icon(LevyraIcons.Play, null, tint = Color.Black, modifier = Modifier.size(size * 0.5f))
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
    val corner = expressiveCorner(interaction, rest = 28.dp, active = 44.dp)
    BoxWithConstraints(Modifier.padding(horizontal = PodcastGutter, vertical = 8.dp).fillMaxWidth()) {
        val wide = maxWidth >= 720.dp
        val artWidth = (maxWidth * 0.48f).coerceAtMost(560.dp)
        val shape = RoundedCornerShape(corner)
        val info: @Composable (Modifier) -> Unit = { infoModifier ->
            Row(infoModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(overline.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.primary, maxLines = 1)
                    Text(episode.title, style = if (wide) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = if (wide) 3 else 2, overflow = TextOverflow.Ellipsis)
                    PodcastProgressLine(progress, copy, fallback = episode.artist)
                }
                if (playing) {
                    PodcastNowPlayingBadge()
                } else {
                    Box(Modifier.size(52.dp).clip(CookieShape).background(colors.onSurface), contentAlignment = Alignment.Center) {
                        Icon(LevyraIcons.Play, null, tint = colors.surface, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        val cardModifier = Modifier.fillMaxWidth().clip(shape).background(colors.surfaceContainerHigh).podcastClick(interaction, episode.title, onPlay)
        if (wide) {
            Row(cardModifier, verticalAlignment = Alignment.CenterVertically) {
                Artwork(episode.artworkUrl, Modifier.width(artWidth).aspectRatio(PodcastWideRatio), cornerRadius = 0.dp, iconSize = 40.dp)
                info(Modifier.weight(1f).padding(horizontal = 24.dp, vertical = 18.dp))
            }
        } else {
            Column(cardModifier) {
                Artwork(episode.artworkUrl, Modifier.fillMaxWidth().aspectRatio(PodcastWideRatio), cornerRadius = 0.dp, iconSize = 40.dp)
                info(Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 16.dp))
            }
        }
    }
}

@Composable
private fun PodcastShowCard(show: PodcastShow, onOpen: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val corner = expressiveCorner(interaction, rest = 28.dp, active = 40.dp)
    Column(Modifier.width(PodcastShowCardWidth).podcastClick(interaction, show.title, onOpen), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Artwork(show.thumbnailUrl, Modifier.size(PodcastShowCardWidth), cornerRadius = corner, iconSize = 36.dp)
        Text(show.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun groupedShape(index: Int, count: Int): RoundedCornerShape {
    val outer = 20.dp
    val inner = 6.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner
    )
}

@Composable
private fun PodcastShowRow(index: Int, count: Int, show: PodcastShow, subscribed: Boolean, copy: PodcastCopy, onOpen: () -> Unit, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier.padding(horizontal = PodcastGutter, vertical = 1.dp).fillMaxWidth().clip(groupedShape(index, count))
            .background(colors.surfaceContainer)
            .podcastClick(interaction, show.title, onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Artwork(show.thumbnailUrl, Modifier.size(PodcastRowArtwork), cornerRadius = 16.dp, iconSize = 22.dp)
        Column(Modifier.weight(1f)) {
            Text(show.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (show.author.isNotBlank()) Text(show.author, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        PodcastSubscribeButton(subscribed, copy, onToggle)
    }
}

@Composable
private fun PodcastShowHeader(show: PodcastShow, description: String, subscribed: Boolean, copy: PodcastCopy, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = PodcastGutter, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(Modifier.size(180.dp).clip(CookieShape).background(colors.surfaceContainerHigh)) {
                Artwork(show.thumbnailUrl, Modifier.fillMaxSize(), cornerRadius = 0.dp, iconSize = 48.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(show.title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = colors.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (show.author.isNotBlank()) Text(show.author, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                modifier = Modifier.widthIn(max = 960.dp).clickable(role = Role.Button) { expanded = !expanded }
            )
        }
    }
}

@Composable
private fun PodcastSubscribeButton(subscribed: Boolean, copy: PodcastCopy, onToggle: () -> Unit) {
    val corner by animateDpAsState(if (subscribed) 12.dp else 24.dp, label = "podcast-subscribe-corner")
    val shape = RoundedCornerShape(corner)
    val label = if (subscribed) copy.subscribed else copy.subscribe
    if (subscribed) {
        FilledTonalButton(onClick = onToggle, shape = shape, modifier = Modifier.heightIn(min = 44.dp)) {
            Icon(PodcastCheckIcon, null, modifier = Modifier.size(18.dp))
            Text(label, modifier = Modifier.padding(start = 6.dp), maxLines = 1)
        }
    } else {
        Button(onClick = onToggle, shape = shape, modifier = Modifier.heightIn(min = 44.dp)) { Text(label, maxLines = 1) }
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
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    val playing = playback.currentTrackId == episode.id
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier.padding(horizontal = PodcastGutter, vertical = 1.dp).fillMaxWidth().clip(groupedShape(index, count))
            .background(if (playing) colors.secondaryContainer else colors.surfaceContainer)
            .podcastClick(interaction, episode.title, onPlay)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box {
            Artwork(episode.artworkUrl, Modifier.size(PodcastRowArtwork), cornerRadius = 14.dp, iconSize = 22.dp)
            if (playing && playback.isPlaying) PodcastNowPlayingBadge(Modifier.align(Alignment.Center))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(episode.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            PodcastProgressLine(progress, copy, fallback = episode.album.ifBlank { episode.artist }, modifier = Modifier.widthIn(max = 420.dp))
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(LevyraIcons.More, episode.title, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                val completed = progress?.completed == true
                DropdownMenuItem(text = { Text(if (completed) copy.markUnplayed else copy.markPlayed) }, onClick = { menuOpen = false; onSetPlayed(!completed) })
                if (onRemove != null) DropdownMenuItem(text = { Text(copy.removeFromHistory) }, onClick = { menuOpen = false; onRemove() })
            }
        }
    }
}

@Composable
private fun PodcastProgressLine(progress: PodcastEpisodeProgress?, copy: PodcastCopy, fallback: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    when {
        progress?.completed == true -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(PodcastCheckIcon, null, tint = colors.primary, modifier = Modifier.size(14.dp))
            Text(copy.played, style = MaterialTheme.typography.labelMedium, color = colors.primary)
        }
        progress?.inProgress == true && progress.durationMs > 0L -> Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LinearProgressIndicator(
                progress = { progress.fraction },
                color = colors.primary,
                trackColor = colors.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
            Text(copy.minutesLeft(((progress.durationMs - progress.positionMs) / 60_000L).coerceAtLeast(1L)),
                style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
        fallback.isNotBlank() -> Text(fallback, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
    }
}

@Composable
private fun PodcastNowPlayingBadge(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.size(36.dp).clip(CookieShape).background(colors.primary), contentAlignment = Alignment.Center) {
        Icon(PodcastEqualizerIcon, null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PodcastLoading() {
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(40.dp), strokeWidth = 3.dp)
    }
}

@Composable
private fun PodcastNotice(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.padding(horizontal = PodcastGutter, vertical = 6.dp).widthIn(max = 720.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainer).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(44.dp).clip(CookieShape).background(colors.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            Icon(LevyraIcons.Radio, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
        }
    }
}

private fun podcastStrokeIcon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData = PathData(block),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ).build()

private val PodcastCheckIcon: ImageVector by lazy {
    podcastStrokeIcon("PodcastCheck") {
        moveTo(5f, 12.5f)
        lineTo(10f, 17.5f)
        lineTo(19f, 7f)
    }
}

private val PodcastEqualizerIcon: ImageVector by lazy {
    podcastStrokeIcon("PodcastEqualizer") {
        moveTo(6f, 10f); lineTo(6f, 14f)
        moveTo(10f, 6f); lineTo(10f, 18f)
        moveTo(14f, 8f); lineTo(14f, 16f)
        moveTo(18f, 11f); lineTo(18f, 13f)
    }
}
