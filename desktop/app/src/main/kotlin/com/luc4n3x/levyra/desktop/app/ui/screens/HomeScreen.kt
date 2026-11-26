package com.luc4n3x.levyra.desktop.app.ui.screens

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.desktop.app.state.DiscoverUiState
import com.luc4n3x.levyra.desktop.app.state.SoundtrackUiState
import com.luc4n3x.levyra.desktop.app.state.soundtrackPrimaryArtist
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.ExpressiveSectionHeader
import com.luc4n3x.levyra.desktop.app.ui.components.ScrollableColumn
import com.luc4n3x.levyra.desktop.app.ui.components.SectionPlayButton
import com.luc4n3x.levyra.desktop.app.ui.components.SectionShowAll
import com.luc4n3x.levyra.desktop.app.ui.components.SectionTints
import com.luc4n3x.levyra.desktop.app.ui.components.ShelfRow
import com.luc4n3x.levyra.desktop.app.ui.components.SunnyShape
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.TrackRow
import com.luc4n3x.levyra.desktop.app.ui.components.horizontalShelfBleed
import com.luc4n3x.levyra.desktop.app.ui.components.levyraIconPainter
import com.luc4n3x.levyra.desktop.app.ui.components.rememberArtworkRequest
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.ArtworkPalette
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
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
import kotlin.random.Random
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
    onOpenPodcast: () -> Unit,
    onOpenNewReleases: () -> Unit,
    onPlayMix: (List<Track>) -> Unit,
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
    val knownIds = remember(library.history, library.favorites) {
        (library.history.map { it.track.id } + library.favorites.map { it.id }).toHashSet()
    }
    val orbitTracks = remember(currentTrack, historyTracks, library.favorites, discover.tracks) {
        (listOfNotNull(currentTrack) + historyTracks + library.favorites + discover.tracks)
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .distinctBy(::trackIdentity)
            .take(ORBIT_LIMIT)
    }
    val quickPicks = remember(library.favorites, historyTracks, discover.tracks, orbitTracks) {
        (orbitTracks + library.favorites + historyTracks + discover.tracks.take(20)).distinctBy(::trackIdentity).take(QUICK_PICKS_LIMIT)
    }
    val discoveryTracks = remember(discover.freshCurrents, discover.genres, knownIds) {
        (discover.freshCurrents + discover.genres.flatMap { it.tracks })
            .filterNot { it.id in knownIds }
            .distinctBy(::trackIdentity)
    }
    val dailyFallback = remember(discover.genres, knownIds) {
        discover.genres.flatMap { it.tracks }
            .filterNot { it.id in knownIds }
            .distinctBy(::trackIdentity)
            .shuffled(Random(LocalDate.now().toEpochDay()))
            .take(DAILY_LIMIT)
    }
    val dailyPicks = remember(feed.daily, dailyFallback) {
        feed.daily.ifEmpty { dailyFallback.map { com.luc4n3x.levyra.desktop.app.state.DailyPick(it, it) } }
    }
    val dailyTracks = remember(dailyPicks) { dailyPicks.map { it.track } }
    val trendingArtists = remember(discover.tracks) {
        discover.tracks.map { soundtrackPrimaryArtist(it.artist) }
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
            val soundtrackQueue = remember(soundtrack.tracks, quickPicks) {
                (soundtrack.tracks.take(1) + quickPicks + soundtrack.tracks.drop(1)).distinctBy(::trackIdentity)
            }
            val leadArtists = remember(soundtrack.artist, quickPicks) {
                (listOf(soundtrack.artist) + quickPicks.map { soundtrackPrimaryArtist(it.artist) })
                    .filter(String::isNotBlank)
                    .distinctBy { it.lowercase() }
                    .take(3)
            }
            val companions = remember(quickPicks) {
                quickPicks.distinctBy { soundtrackPrimaryArtist(it.artist).lowercase() }.map { it.artworkUrl }.filter(String::isNotBlank).take(3)
            }
            val heroPlaying = isPlaying && currentTrack != null && soundtrackQueue.any { it.id == currentTrack.id }
            Box(Modifier.horizontalShelfBleed(Gutter)) {
                SoundtrackHero(
                    overline = strings.homeGreeting,
                    overlineHighlight = displayName,
                    title = shared.homeSoundtrackTodayTitle,
                    lead = remember(leadArtists, shared) { shared.homeSoundtrackLead(leadArtists) },
                    portraitUrl = soundtrack.portraitUrl,
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
                    badge = copy.reasonNewRelease,
                    covers = discoveryTracks.take(3).map { it.artworkUrl },
                    onStart = { onPlayMix(discoveryTracks) }
                )
            }
        }

        if (feed.curated.isNotEmpty()) {
            item(key = "curated") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ExpressiveSectionHeader(
                        LevyraIcons.Playlist,
                        SectionTints.Lavender,
                        remember(code) { LevyraMonoHomeStrings.forCode(code).mixedForYou },
                        subtitles.curatedPlaylists
                    )
                    ShelfRow(
                        modifier = Modifier.horizontalShelfBleed(Gutter),
                        contentPadding = PaddingValues(horizontal = Gutter),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(feed.curated, key = { "curated-${it.id}" }) { playlist ->
                            CuratedCard(playlist.title, playlist.subtitle, playlist.artworkUrl) { onOpenCollection(playlist.toCollectionRef()) }
                        }
                    }
                }
            }
        }

        if (dailyTracks.isNotEmpty()) {
            item(key = "daily") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ExpressiveSectionHeader(
                        LevyraIcons.Radio,
                        SectionTints.Sky,
                        remember(code) { LevyraYouTubeMusicSkinStrings.forCode(code).dailyDiscovery },
                        subtitles.dailyDiscover
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
                                badge = if (pick.seed.id != pick.track.id) copy.reasonSimilarTo(pick.seed.title, soundtrackPrimaryArtist(pick.seed.artist)) else null,
                                onClick = { actions.onPlay(dailyTracks, dailyTracks.indexOf(pick.track).coerceAtLeast(0)) }
                            )
                        }
                    }
                }
            }
        }

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
                            VideoCard(video, onClick = { actions.onPlay(feed.videos, feed.videos.indexOf(video).coerceAtLeast(0)) })
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
}

@Composable
private fun HomeTopBar(moodLabel: String, podcastLabel: String, onOpenMoods: () -> Unit, onOpenPodcast: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(60.dp)
                .clip(SunnyShape)
                .background(Color(0xFF1C1C22))
                .border(1.dp, Color.White.copy(alpha = 0.10f), SunnyShape),
            contentAlignment = Alignment.Center
        ) {
            Image(levyraIconPainter(), contentDescription = null, modifier = Modifier.size(48.dp))
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
private fun DiscoveryCard(title: String, lead: String, start: String, badge: String, covers: List<String>, onStart: () -> Unit) {
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val tint by produceState(LevyraBrand.soft, covers.firstOrNull()) {
        value = covers.firstOrNull()?.let { ArtworkPalette.accentFor(it) } ?: LevyraBrand.soft
    }
    val container = lerp(surface, tint, 0.22f)
    val action = lerp(tint, Color.White, 0.35f)
    Row(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(container, surface)))
            .padding(horizontal = 32.dp, vertical = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text(lead, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onStart).padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(action), contentAlignment = Alignment.Center) {
                    Icon(LevyraIcons.Play, contentDescription = start, tint = Color(0xFF0E1A12), modifier = Modifier.size(22.dp))
                }
                Text(start, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        FannedCovers(covers, badge)
    }
}

@Composable
private fun FannedCovers(covers: List<String>, badge: String) {
    Box(Modifier.width(320.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
        covers.getOrNull(1)?.let { Artwork(it, Modifier.offset(x = (-78).dp).size(118.dp).graphicsLayer { rotationZ = -9f }, cornerRadius = 16.dp) }
        covers.getOrNull(2)?.let { Artwork(it, Modifier.offset(x = 78.dp).size(118.dp).graphicsLayer { rotationZ = 9f }, cornerRadius = 16.dp) }
        covers.firstOrNull()?.let { Artwork(it, Modifier.size(156.dp), cornerRadius = 22.dp) }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .clip(CircleShape)
                .background(Color(0xFF9BE3B5))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(LevyraIcons.Radio, contentDescription = null, tint = Color(0xFF0E2A18), modifier = Modifier.size(13.dp))
            Text(badge, style = MaterialTheme.typography.labelMedium, color = Color(0xFF0E2A18))
        }
    }
}

@Composable
private fun PosterCard(track: Track, rank: Int? = null, badge: String? = null, onClick: () -> Unit) {
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
        if (badge != null) {
            Text(
                badge,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 10.dp, vertical = 5.dp).hoverMarquee(hovered)
            )
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
private fun CuratedCard(title: String, subtitle: String, artworkUrl: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        Modifier.width(CuratedSize).clip(RoundedCornerShape(28.dp)).hoverable(interaction).clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(CuratedSize)) {
            Box(
                Modifier.align(Alignment.TopCenter).padding(horizontal = 14.dp).fillMaxWidth().height(CuratedSize)
                    .clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
            Artwork(
                artworkUrl,
                Modifier.align(Alignment.BottomCenter).padding(top = 10.dp).size(CuratedSize - 10.dp).graphicsLayer {
                    val scale = if (hovered) 1.03f else 1f
                    scaleX = scale
                    scaleY = scale
                },
                cornerRadius = 28.dp
            )
        }
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = LevyraBrand.soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.hoverMarquee(hovered))
    }
}

@Composable
private fun VideoCard(track: Track, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        Modifier.width(VideoWidth).clip(RoundedCornerShape(22.dp)).hoverable(interaction).clickable(role = Role.Button, onClick = onClick),
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
        }
        Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        Text(track.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AlbumCard(track: Track, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        Modifier
            .width(AlbumSize)
            .clip(RoundedCornerShape(20.dp))
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
private val ColumnWidth = 400.dp
private const val ORBIT_LIMIT = 12
private const val QUICK_PICKS_LIMIT = 20
private const val DAILY_LIMIT = 14
private const val ARTIST_LIMIT = 14
private const val RANKED_CARDS = 10
private const val ROWS_PER_COLUMN = 4
