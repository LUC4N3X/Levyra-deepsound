package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.desktop.app.state.DiscoverUiState
import com.luc4n3x.levyra.desktop.app.state.SoundtrackUiState
import com.luc4n3x.levyra.desktop.app.state.soundtrackPrimaryArtist
import com.luc4n3x.levyra.desktop.app.ui.components.CookieShape
import com.luc4n3x.levyra.desktop.app.ui.components.SunnyShape
import com.luc4n3x.levyra.desktop.app.ui.components.CollectionCard
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.ExpressiveSectionHeader
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.SectionPlayButton
import com.luc4n3x.levyra.desktop.app.ui.components.SectionShowAll
import com.luc4n3x.levyra.desktop.app.ui.components.SectionTints
import com.luc4n3x.levyra.desktop.app.ui.components.ShelfRow
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.components.horizontalShelfBleed
import com.luc4n3x.levyra.desktop.app.ui.components.levyraLogoPainter
import androidx.compose.foundation.lazy.LazyListScope
import com.luc4n3x.levyra.ui.i18n.HomeSectionSubtitles
import com.luc4n3x.levyra.desktop.app.ui.components.rememberArtworkRequest
import com.luc4n3x.levyra.desktop.app.state.DailyPick
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.ArtworkPalette
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.desktop.core.catalog.artistCreditMatches
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.LibraryData
import com.luc4n3x.levyra.ui.i18n.LevyraMonoHomeStrings
import com.luc4n3x.levyra.ui.i18n.LevyraYouTubeMusicSkinStrings
import com.luc4n3x.levyra.ui.i18n.discoveryCopy
import com.luc4n3x.levyra.ui.i18n.exploreLocalizationEntries
import com.luc4n3x.levyra.ui.i18n.genreChartCopy
import com.luc4n3x.levyra.ui.i18n.homeSectionSubtitles
import com.luc4n3x.levyra.ui.i18n.homeTrendingVideoShelfTitle
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedStrings

@Composable
fun HomeScreen(
    library: LibraryData,
    discover: DiscoverUiState,
    soundtrack: SoundtrackUiState,
    artistPortraits: Map<String, String>,
    feed: com.luc4n3x.levyra.desktop.app.state.HomeFeedState,
    currentTrack: Track?,
    isPlaying: Boolean,
    displayName: String,
    actions: TrackActions,
    onTogglePlayback: () -> Unit,
    onRequestPortraits: (List<String>) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenMoods: () -> Unit,
    onOpenCuratedAll: () -> Unit,
    onOpenPodcast: () -> Unit,
    onOpenNewReleases: () -> Unit,
    onOpenForYou: (List<com.luc4n3x.levyra.desktop.app.state.DiscoveryPick>) -> Unit,
    onOpenCollection: (com.luc4n3x.levyra.desktop.core.model.CollectionRef) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val code = strings.languageCode
    val shared = remember(code) { SharedStrings.forCode(code) }
    val subtitles = remember(code) { homeSectionSubtitles(code) }

    val historyTracks = remember(library.history) {
        library.history.sortedByDescending { it.playedAt }.map { it.track }.distinctBy(::trackIdentity)
    }
    val excluded = library.excludedArtists.values
    val suggestedTracks = remember(discover.tracks, library.excludedArtists) {
        discover.tracks.filterNot { track -> excluded.any { artistCreditMatches(track.artist, it) } }
    }
    val knownIds = remember(library.history, library.favorites) {
        (library.history.map { it.track.id } + library.favorites.map { it.id }).toHashSet()
    }
    val orbitTracks = remember(currentTrack, historyTracks, library.favorites, suggestedTracks) {
        (listOfNotNull(currentTrack) + historyTracks + library.favorites + suggestedTracks)
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .distinctBy(::trackIdentity)
            .take(ORBIT_LIMIT)
    }
    val quickPicks = remember(library.favorites, historyTracks, suggestedTracks, orbitTracks) {
        (orbitTracks + library.favorites + historyTracks + suggestedTracks.take(20)).distinctBy(::trackIdentity).take(QUICK_PICKS_LIMIT)
    }
    val discoveryTracks = remember(discover.freshCurrents, discover.genres, knownIds, library.excludedArtists) {
        (discover.freshCurrents + discover.genres.flatMap { it.tracks })
            .filterNot { track -> track.id in knownIds || excluded.any { artistCreditMatches(track.artist, it) } }
            .distinctBy(::trackIdentity)
    }
    val dailyFallback = remember(discover.genres, knownIds, library.excludedArtists) {
        discover.genres.flatMap { it.tracks }
            .filterNot { track -> track.id in knownIds || excluded.any { artistCreditMatches(track.artist, it) } }
            .distinctBy(::trackIdentity)
            .shuffled(Random(LocalDate.now().toEpochDay()))
            .take(DAILY_LIMIT)
    }
    val dailyPicks = remember(feed.daily, dailyFallback, library.excludedArtists) {
        feed.daily.filterNot { pick -> excluded.any { artistCreditMatches(pick.track.artist, it) || artistCreditMatches(pick.seed.artist, it) } }.ifEmpty { dailyFallback.map { DailyPick(it, it) } }
    }
    val dailyTracks = remember(dailyPicks) { dailyPicks.map { it.track } }
    val trendingArtists = remember(suggestedTracks) {
        suggestedTracks.map { soundtrackPrimaryArtist(it.artist) }
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .take(ARTIST_LIMIT)
    }
    LaunchedEffect(trendingArtists) { if (trendingArtists.isNotEmpty()) onRequestPortraits(trendingArtists) }

    ScrollableColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = Gutter, end = Gutter, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item(key = "soundtrack") {
            HomeSoundtrack(
                soundtrack, quickPicks, library.excludedArtists, currentTrack, isPlaying, displayName,
                actions, onTogglePlayback, onOpenMoods, onOpenPodcast
            )
        }

        if (library.pinnedArtists.isNotEmpty()) {
            item(key = "home-pinned-artists") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExpressiveSectionHeader(LevyraIcons.Disc, SectionTints.Lavender, shared.speedDialCopy().title)
                    ShelfRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(library.pinnedArtists, key = { it.id }) { artist ->
                            CollectionCard(artist, onClick = { onOpenCollection(artist) }, modifier = Modifier.width(152.dp))
                        }
                    }
                }
            }
        }

        if (orbitTracks.isNotEmpty()) {
            item(key = "orbit") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ExpressiveSectionHeader(LevyraIcons.Disc, SectionTints.Lavender, shared.personalOrbitTitle, subtitles.orbit)
                    OrbitGrid(orbitTracks, onPlay = { index -> actions.onPlay(orbitTracks, index) })
                }
            }
        }

        if (discoveryTracks.isNotEmpty()) {
            item(key = "discovery") {
                val copy = remember(code) { discoveryCopy(code) }
                DiscoveryCard(
                    title = copy.title,
                    lead = copy.lead,
                    start = copy.start,
                    covers = remember(discoveryTracks) {
                        discoveryTracks.map { it.artworkUrl }.filter(String::isNotBlank).distinct().take(3)
                    },
                    onStart = { onOpenForYou(com.luc4n3x.levyra.desktop.app.state.homeDiscoveryPicks(discover, feed, library)) }
                )
            }
        }

        if (feed.curatedShelves.isEmpty()) {
            item(key = "curated-empty-shelf") {
                val copy = remember(code) { LevyraMonoHomeStrings.forCode(code) }
                ExpressiveSectionHeader(
                    LevyraIcons.Playlist,
                    SectionTints.Lavender,
                    copy.mixedForYou,
                    copy.rotationOverline
                ) { SectionShowAll(shared.showAll, onOpenCuratedAll) }
            }
        }
        feed.curatedShelves.forEachIndexed { index, shelf ->
            item(key = "curated-$index-${shelf.title}") {
                val copy = remember(code) { LevyraMonoHomeStrings.forCode(code) }
                val curatedTitle = if (index == 0) copy.mixedForYou else shelf.title
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ExpressiveSectionHeader(
                        LevyraIcons.Playlist,
                        SectionTints.Lavender,
                        curatedTitle,
                        if (index == 0) copy.rotationOverline else shelf.subtitle.ifBlank { subtitles.curatedPlaylists }
                    ) { if (index == 0) SectionShowAll(shared.showAll, onOpenCuratedAll) }
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val gap = 14.dp
                        val cardWidth = ((maxWidth - gap * 6) / 7).coerceIn(136.dp, CuratedSize)
                        ShelfRow(
                            modifier = Modifier.horizontalShelfBleed(Gutter),
                            contentPadding = PaddingValues(horizontal = Gutter),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            items(shelf.playlists, key = { "curated-${it.id}" }) { playlist ->
                                CuratedCard(playlist.title, playlist.subtitle, playlist.artworkUrl, Modifier.width(cardWidth)) {
                                    onOpenCollection(playlist.toCollectionRef())
                                }
                            }
                        }
                    }
                }
            }
        }

        dailyDiscovery(dailyPicks, dailyTracks, subtitles.dailyDiscover, actions)

        if (quickPicks.isNotEmpty()) {
            item(key = "quick") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExpressiveSectionHeader(LevyraIcons.Shuffle, SectionTints.Amber, shared.quickPicks, subtitles.quickPicks) {
                        SectionPlayButton(strings.playAll) { actions.onPlay(quickPicks, 0) }
                    }
                    TrackColumns(quickPicks, actions, ranked = false)
                }
            }
        }

        homeCatalogSections(discover, feed, actions, artistPortraits, trendingArtists, code, strings, shared, subtitles, onOpenArtist, onOpenDiscover, onOpenNewReleases)
    }
}

@Composable
private fun HomeSoundtrack(
    soundtrack: SoundtrackUiState,
    quickPicks: List<Track>,
    excludedArtists: Map<String, String>,
    currentTrack: Track?,
    isPlaying: Boolean,
    displayName: String,
    actions: TrackActions,
    onTogglePlayback: () -> Unit,
    onOpenMoods: () -> Unit,
    onOpenPodcast: () -> Unit
) {
    val strings = LocalStrings.current
    val code = strings.languageCode
    val shared = remember(code) { SharedStrings.forCode(code) }
    val excluded = excludedArtists.values
    val soundtrackTracks = remember(soundtrack.tracks, excludedArtists) {
        soundtrack.tracks.filterNot { track -> excluded.any { artistCreditMatches(track.artist, it) } }
    }
    val soundtrackQueue = remember(soundtrackTracks, quickPicks, excludedArtists) {
        soundtrackTracks.ifEmpty {
            quickPicks.filterNot { track -> excluded.any { artistCreditMatches(track.artist, it) } }
        }.distinctBy(::trackIdentity)
    }
    val leadArtists = remember(soundtrackQueue) {
        soundtrackQueue.map { soundtrackPrimaryArtist(it.artist) }
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .take(3)
    }
    val companions = remember(soundtrackQueue) {
        soundtrackQueue.map { it.artworkUrl }.filter(String::isNotBlank).distinct().take(3)
    }
    val heroPlaying = isPlaying && currentTrack != null && soundtrackQueue.any { it.id == currentTrack.id }
    Box(Modifier.horizontalShelfBleed(Gutter)) {
        SoundtrackHero(
            overline = strings.homeGreeting,
            overlineHighlight = displayName,
            title = shared.homeSoundtrackTodayTitle,
            lead = remember(leadArtists, shared) { shared.homeSoundtrackLead(leadArtists) },
            portraitUrl = soundtrack.portraitUrl.takeIf { soundtrackTracks.isNotEmpty() }.orEmpty(),
            fallbackArtworkUrl = soundtrackQueue.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl.orEmpty(),
            companionArtworkUrls = companions,
            playing = heroPlaying,
            loading = soundtrack.loading && soundtrackQueue.isEmpty(),
            playLabel = strings.playAll,
            enabled = soundtrackQueue.isNotEmpty(),
            onPlay = { if (heroPlaying) onTogglePlayback() else actions.onPlay(soundtrackQueue, 0) }
        )
        HomeTopBar(
            moodLabel = remember(code) { exploreLocalizationEntries(code).values.firstOrNull().orEmpty() },
            podcastLabel = remember(code) { com.luc4n3x.levyra.ui.i18n.podcastCopy(code).title },
            onOpenMoods = onOpenMoods,
            onOpenPodcast = onOpenPodcast,
            modifier = Modifier.padding(start = Gutter, top = 22.dp)
        )
    }
}

private fun LazyListScope.dailyDiscovery(
    dailyPicks: List<DailyPick>,
    dailyTracks: List<Track>,
    subtitle: String,
    actions: TrackActions
) {
    if (dailyTracks.isNotEmpty()) {
        item(key = "daily") {
            val strings = LocalStrings.current
            val code = strings.languageCode
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ExpressiveSectionHeader(
                    LevyraIcons.Radio,
                    SectionTints.Sky,
                    remember(code) { LevyraYouTubeMusicSkinStrings.forCode(code).dailyDiscovery },
                    subtitle
                ) { SectionPlayButton(strings.playAll) { actions.onPlay(dailyTracks, 0) } }
                ShelfRow(
                    modifier = Modifier.horizontalShelfBleed(Gutter),
                    contentPadding = PaddingValues(horizontal = Gutter),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(dailyPicks, key = { "daily-${it.track.id}" }) { pick ->
                        val copy = remember(code) { discoveryCopy(code) }
                        PosterCard(
                            pick.track,
                            reason = when {
                                pick.seed.id != pick.track.id && pick.seed.artist.isNotBlank() ->
                                    copy.reasonSimilarTo(pick.seed.title, soundtrackPrimaryArtist(pick.seed.artist))
                                pick.track.artist.isNotBlank() ->
                                    copy.reasonForFansOf(soundtrackPrimaryArtist(pick.track.artist))
                                else -> null
                            },
                            onClick = { actions.onPlay(dailyTracks, dailyTracks.indexOf(pick.track).coerceAtLeast(0)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(moodLabel: String, podcastLabel: String, onOpenMoods: () -> Unit, onOpenPodcast: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(levyraLogoPainter(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = 1.18f
                scaleY = 1.18f
            })
        }
        HomeChip(LevyraIcons.Radio, podcastLabel, onOpenPodcast)
        HomeChip(LevyraIcons.Chart, moodLabel, onOpenMoods)
    }
}

@Composable
private fun HomeChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    run {
        if (label.isNotBlank()) {
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.28f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = LevyraBrand.soft, modifier = Modifier.size(18.dp))
                Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White)
            }
        }
    }
}

@Composable
private fun OrbitGrid(tracks: List<Track>, onPlay: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = (maxWidth / 190.dp).toInt().coerceIn(3, 6)
        val visible = tracks.take(columns * 2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            visible.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { track ->
                        OverlayTile(track, Modifier.weight(1f)) { onPlay(tracks.indexOf(track)) }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun OverlayTile(track: Track, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier
            .aspectRatio(1f)
            .clip(TileShape)
            .hoverable(interaction)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Artwork(track.artworkUrl, Modifier.fillMaxSize().graphicsLayer {
            val scale = if (hovered) 1.04f else 1f
            scaleX = scale
            scaleY = scale
        }, cornerRadius = 0.dp)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))))
        Text(
            track.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
        )
    }
}

@Composable
internal fun DiscoveryCard(title: String, lead: String, start: String, covers: List<String>, onStart: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint by produceState(colors.primary, covers.firstOrNull()) {
        value = covers.firstOrNull()?.let { ArtworkPalette.accentFor(it) } ?: colors.primary
    }
    val top = lerp(colors.surfaceContainer, tint, 0.22f)
    val bottom = colors.surfaceContainer
    val shape = RoundedCornerShape(32.dp)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val lift by animateFloatAsState(if (hovered) 1f else 0f, label = "discovery-deck")
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BoxWithConstraints(
            Modifier.widthIn(max = 1040.dp).fillMaxWidth()
                .clip(shape).background(Brush.verticalGradient(listOf(top, bottom)))
                .border(1.dp, colors.outlineVariant.copy(alpha = 0.45f), shape).hoverable(interaction)
        ) {
            val panelWidth = maxWidth
            val compact = panelWidth < 630.dp
            Row(
                Modifier.fillMaxWidth().padding(horizontal = if (compact) 20.dp else 36.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        title,
                        style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(lead, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Row(
                        Modifier.padding(top = 12.dp).clip(CircleShape).background(colors.surfaceContainerHigh)
                            .clickable(role = Role.Button, onClick = onStart).padding(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(52.dp).clip(CookieShape).background(colors.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(LevyraIcons.Play, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(24.dp))
                        }
                        Text(start, style = MaterialTheme.typography.labelLarge, color = colors.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
                if (covers.isNotEmpty() && panelWidth >= 360.dp) {
                    val width = if (compact) 132.dp else (panelWidth * 0.44f).coerceAtMost(440.dp)
                    DiscoveryArtwork(covers, tint, width, compact) { lift }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryArtwork(covers: List<String>, accent: Color, width: Dp, compact: Boolean, lift: () -> Float) {
    val size = if (compact) 94.dp else 172.dp
    val edge = MaterialTheme.colorScheme.surfaceContainer
    val turn = remember(covers) { Animatable(0f) }
    val moving = covers.size == 3
    LaunchedEffect(turn, moving) {
        if (!moving) return@LaunchedEffect
        repeat(covers.size * 2) { step ->
            delay(2300)
            turn.animateTo((step + 1).toFloat(), tween(680))
        }
    }
    val front by remember(turn) { derivedStateOf { turn.value.roundToInt() % covers.size } }
    Box(Modifier.width(width).height(if (compact) 138.dp else 202.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size * 1.12f).graphicsLayer { rotationZ = 12f + lift() * 8f + turn.value * 12f }
                .clip(SunnyShape).background(accent.copy(alpha = 0.32f))
        )
        covers.forEachIndexed { index, url ->
            val top = index == front
            val shape = RoundedCornerShape(28.dp)
            Artwork(
                url,
                Modifier.size(size).zIndex(if (top) 1f else 0f).graphicsLayer {
                    val offset = if (moving) {
                        (index - turn.value + 19.5f) % 3f - 1.5f
                    } else if (index == 0) 0f else -1f
                    val distance = abs(offset).coerceIn(0f, 1f)
                    val travelFraction = if (compact) 0.24f else 0.54f
                    val movement = size * travelFraction
                    translationX = movement.toPx() * offset
                    translationY = (9.dp * distance - 5.dp * lift() * (1f - distance)).toPx()
                    val scale = 1f - distance * 0.25f + lift() * (1f - distance) * 0.03f
                    scaleX = scale
                    scaleY = scale
                    rotationZ = offset * 15f - 3f * (1f - distance)
                    alpha = 1f - (abs(offset) - 1f).coerceIn(0f, 0.5f) * 2f
                }.border(3.dp, edge, shape),
                cornerRadius = 28.dp
            )
        }
    }
}

@Composable
private fun PosterCard(track: Track, rank: Int? = null, reason: String? = null, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier
            .width(212.dp)
            .height(288.dp)
            .clip(PosterShape)
            .hoverable(interaction)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Artwork(track.artworkUrl, Modifier.fillMaxSize().graphicsLayer {
            val scale = if (hovered) 1.04f else 1f
            scaleX = scale
            scaleY = scale
        }, cornerRadius = 0.dp)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
        if (reason != null) {
            Row(
                modifier = Modifier.padding(12.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.70f)).padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(LevyraIcons.Radio, contentDescription = null, tint = LevyraBrand.soft, modifier = Modifier.size(14.dp))
                Text(
                    reason,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (rank != null) {
            Text(
                "#$rank",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = LevyraBrand.onSoft,
                modifier = Modifier.padding(12.dp).clip(CircleShape).background(LevyraBrand.soft).padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.hoverMarquee(hovered))
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.hoverMarquee(hovered))
            }
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = if (hovered) 0.95f else 0.82f)), contentAlignment = Alignment.Center) {
                Icon(LevyraIcons.Play, contentDescription = null, tint = Color(0xFF111114), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
internal fun CuratedCard(title: String, subtitle: String, artworkUrl: String, modifier: Modifier = Modifier.width(CuratedSize), onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        modifier.hoverable(interaction).clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val stack by produceState(MaterialTheme.colorScheme.surfaceContainerHighest, artworkUrl) {
            value = ArtworkPalette.accentFor(artworkUrl)?.copy(alpha = 0.55f) ?: value
        }
        Box(Modifier.fillMaxWidth().padding(top = CuratedPeek).aspectRatio(1f)) {
            Box(
                Modifier.matchParentSize().offset(y = -CuratedPeek).padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(28.dp)).background(stack)
            )
            Artwork(
                artworkUrl,
                Modifier.fillMaxSize().graphicsLayer {
                    val scale = if (hovered) 1.03f else 1f
                    scaleX = scale
                    scaleY = scale
                },
                cornerRadius = 28.dp
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.hoverMarquee(hovered))
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun VideoCard(
    track: Track,
    code: String,
    playsLabel: String,
    menu: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val meta = remember(track.artist, track.viewCount, code, playsLabel) {
        val plays = formatViewCount(track.viewCount, code).takeIf(String::isNotBlank)?.let { "$it $playsLabel" }.orEmpty()
        listOf(track.artist.trim(), plays).filter(String::isNotBlank).joinToString(" • ")
    }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        Modifier.width(VideoWidth).hoverable(interaction).clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.width(VideoWidth).aspectRatio(16f / 9f).clip(RoundedCornerShape(22.dp))) {
            Artwork(track.artworkUrl, Modifier.fillMaxSize().graphicsLayer {
                val scale = if (hovered) 1.03f else 1f
                scaleX = scale
                scaleY = scale
            }, cornerRadius = 0.dp)
            Box(
                Modifier.align(Alignment.Center).size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = if (hovered) 0.6f else 0.42f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(LevyraIcons.Play, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            if (menu != null) Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) { menu() }
        }
        Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        Text(meta, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AlbumCard(track: Track, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        Modifier
            .width(AlbumSize)
            .hoverable(interaction)
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Artwork(
            track.artworkUrl,
            Modifier.size(AlbumSize).graphicsLayer {
                val scale = if (hovered) 1.03f else 1f
                scaleX = scale
                scaleY = scale
            },
            cornerRadius = 20.dp
        )
        Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        Text(track.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ArtistBubble(name: String, portraitUrl: String, onClick: () -> Unit) {
    Column(
        Modifier.width(150.dp).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Artwork(portraitUrl, Modifier.size(132.dp).clip(CircleShape), cornerRadius = 66.dp, iconSize = 32.dp)
        Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun GenreCard(title: String, badge: String, artists: String, artworkUrl: String, onClick: () -> Unit) {
    val request = rememberArtworkRequest(artworkUrl)
    Column(Modifier.width(280.dp).clickable(role = Role.Button, onClick = onClick), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            if (request != null) {
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))))
            Column(Modifier.align(Alignment.BottomCenter).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(badge, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = LevyraBrand.soft)
            }
        }
        Text(artists, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TrackColumns(tracks: List<Track>, actions: TrackActions, ranked: Boolean, startIndex: Int = 0) {
    val pages = remember(tracks, startIndex) { tracks.drop(startIndex).chunked(ROWS_PER_COLUMN) }
    ShelfRow(
        modifier = Modifier.horizontalShelfBleed(Gutter),
        contentPadding = PaddingValues(horizontal = Gutter),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(pages.size, key = { "page-$startIndex-$it" }) { page ->
            Column(Modifier.width(ColumnWidth), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                pages[page].forEachIndexed { offset, track ->
                    val index = startIndex + page * ROWS_PER_COLUMN + offset
                    TrackRow(
                        track = track,
                        isCurrent = track.id == actions.currentTrackId,
                        isFavorite = actions.isFavorite(track),
                        onPlay = { actions.onPlay(tracks, index) },
                        onPlayNext = { actions.onPlayNext(track) },
                        onEnqueue = { actions.onEnqueue(track) },
                        onToggleFavorite = { actions.onToggleFavorite(track) },
                        onAddToPlaylist = { actions.onAddToPlaylist(track) },
                        position = if (ranked) index + 1 else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                }
            }
        }
    }
}

private fun Modifier.hoverMarquee(active: Boolean): Modifier =
    if (active) this.then(Modifier.basicMarquee(iterations = Int.MAX_VALUE)) else this

internal fun formatViewCount(views: Long, languageCode: String): String {
    if (views <= 0L) return ""
    val locale = java.util.Locale.forLanguageTag(languageCode.replace('_', '-'))
    val italian = locale.language == "it"
    val (divisor, suffix) = when {
        views >= 1_000_000_000L -> 1_000_000_000.0 to if (italian) "Mld" else "B"
        views >= 1_000_000L -> 1_000_000.0 to if (italian) "Mln" else "M"
        views >= 1_000L -> 1_000.0 to if (italian) "mila" else "K"
        else -> 1.0 to ""
    }
    val scaled = views / divisor
    val pattern = if (divisor > 1.0 && scaled < 10.0 && kotlin.math.abs(scaled - scaled.toLong()) > 0.05) "%.1f" else "%.0f"
    val number = String.format(locale, pattern, scaled)
    return if (suffix.isBlank()) number else "$number $suffix"
}

private fun trackIdentity(track: Track): String = buildString {
    append(track.title.trim().lowercase())
    append('|')
    append(track.artist.trim().lowercase())
}

private val Gutter: Dp = 32.dp
private val TileShape = RoundedCornerShape(18.dp)
private val PosterShape = RoundedCornerShape(26.dp)
private val AlbumSize = 196.dp
private val VideoWidth = 340.dp
private val CuratedSize = 196.dp
private val CuratedPeek = 12.dp
private val ColumnWidth = 400.dp
private const val ORBIT_LIMIT = 12
private const val QUICK_PICKS_LIMIT = 20
private const val DAILY_LIMIT = 14
private const val ARTIST_LIMIT = 14
private const val RANKED_CARDS = 10
private const val ROWS_PER_COLUMN = 4


private fun LazyListScope.homeCatalogSections(
    discover: DiscoverUiState,
    feed: com.luc4n3x.levyra.desktop.app.state.HomeFeedState,
    actions: TrackActions,
    artistPortraits: Map<String, String>,
    trendingArtists: List<String>,
    code: String,
    strings: com.luc4n3x.levyra.desktop.app.ui.i18n.LevyraStrings,
    shared: SharedStrings,
    subtitles: HomeSectionSubtitles,
    onOpenArtist: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenNewReleases: () -> Unit
) {
    if (discover.freshCurrents.isNotEmpty()) {
        item(key = "albums") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ExpressiveSectionHeader(
                    LevyraIcons.Disc,
                    SectionTints.Periwinkle,
                    remember(code) { LevyraMonoHomeStrings.forCode(code).newAlbums },
                    subtitles.newAlbums
                ) { SectionShowAll(shared.showAll, onOpenNewReleases) }
                ShelfRow(
                    modifier = Modifier.horizontalShelfBleed(Gutter),
                    contentPadding = PaddingValues(horizontal = Gutter),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(discover.freshCurrents, key = { "album-${it.id}" }) { track ->
                        AlbumCard(track, onClick = {
                            actions.onPlay(discover.freshCurrents, discover.freshCurrents.indexOf(track).coerceAtLeast(0))
                        })
                    }
                }
            }
        }
    }

    if (trendingArtists.isNotEmpty()) {
        item(key = "artists") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ExpressiveSectionHeader(LevyraIcons.Artist, SectionTints.Rose, shared.trendingArtists, subtitles.trendingArtists)
                ShelfRow(
                    modifier = Modifier.horizontalShelfBleed(Gutter),
                    contentPadding = PaddingValues(horizontal = Gutter),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(trendingArtists, key = { "artist-$it" }) { name ->
                        ArtistBubble(name, artistPortraits[name.lowercase()].orEmpty(), onClick = { onOpenArtist(name) })
                    }
                }
            }
        }
    }

    if (discover.genres.isNotEmpty()) {
        item(key = "genres") {
            val copy = remember(code) { genreChartCopy(code) }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ExpressiveSectionHeader(LevyraIcons.Chart, SectionTints.Periwinkle, copy.shelfTitle, copy.shelfSubtitle)
                ShelfRow(
                    modifier = Modifier.horizontalShelfBleed(Gutter),
                    contentPadding = PaddingValues(horizontal = Gutter),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(discover.genres, key = { "genre-${it.id}" }) { shelf ->
                        GenreCard(
                            title = shelf.title,
                            badge = copy.title,
                            artists = shelf.tracks.map { soundtrackPrimaryArtist(it.artist) }.distinct().take(4).joinToString(", "),
                            artworkUrl = shelf.tracks.firstOrNull { it.hasArtwork }?.artworkUrl.orEmpty(),
                            onClick = { if (shelf.tracks.isNotEmpty()) actions.onPlay(shelf.tracks, 0) }
                        )
                    }
                }
            }
        }
    }

    if (feed.videos.isNotEmpty()) {
        item(key = "videos") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ExpressiveSectionHeader(LevyraIcons.Play, SectionTints.Sky, shared.homeTrendingVideoShelfTitle, subtitles.trendingVideos) {
                    SectionPlayButton(strings.playAll) { actions.onPlay(feed.videos, 0) }
                }
                ShelfRow(
                    modifier = Modifier.horizontalShelfBleed(Gutter),
                    contentPadding = PaddingValues(horizontal = Gutter),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(feed.videos, key = { "video-${it.id}" }) { video ->
                        VideoCard(video, code, shared.pulsePlays, onClick = { actions.onPlay(feed.videos, feed.videos.indexOf(video).coerceAtLeast(0)) })
                    }
                }
            }
        }
    }

    if (discover.tracks.isNotEmpty()) {
        item(key = "top") {
            val country = remember(discover.country, code) {
                Locale("", discover.country).getDisplayCountry(Locale.forLanguageTag(code)).ifBlank { discover.country }
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ExpressiveSectionHeader(
                    LevyraIcons.Chart,
                    SectionTints.Amber,
                    shared.chartMarketTitle(discover.tracks.size.coerceAtMost(50), country),
                    subtitles.chartMarket
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionShowAll(shared.showAll, onOpenDiscover)
                        SectionPlayButton(strings.playAll) { actions.onPlay(discover.tracks, 0) }
                    }
                }
                ShelfRow(
                    modifier = Modifier.horizontalShelfBleed(Gutter),
                    contentPadding = PaddingValues(horizontal = Gutter),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(discover.tracks.take(RANKED_CARDS), key = { "rank-${it.id}" }) { track ->
                        val index = discover.tracks.indexOf(track)
                        PosterCard(track, rank = index + 1, onClick = { actions.onPlay(discover.tracks, index.coerceAtLeast(0)) })
                    }
                }
                if (discover.tracks.size > RANKED_CARDS) {
                    TrackColumns(discover.tracks, actions, ranked = true, startIndex = RANKED_CARDS)
                }
            }
        }
    }
}
