package com.luc4n3x.levyra.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.HomeBlock
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.HomeSectionId
import com.luc4n3x.levyra.domain.ReleaseRadarEntry
import com.luc4n3x.levyra.domain.ResonanceCommentSnippet
import com.luc4n3x.levyra.domain.SpeedDialPin
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.discovery.DiscoveryHomeHandle
import com.luc4n3x.levyra.ui.discovery.discoveryReasonLabel
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.discoveryCopy
import com.luc4n3x.levyra.ui.i18n.homeVideoShelfTitle
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.i18n.radioLibraryCopy
import com.luc4n3x.levyra.viewmodel.DiscoveryFeedStatus

internal class ComposedCard(
    val key: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String,
    val circular: Boolean = false,
    val wide: Boolean = false,
    val explicit: Boolean = false,
    val track: Track? = null,
    val onLongClick: (() -> Unit)? = null,
    val onClick: () -> Unit
)

internal class ComposedQuote(
    val key: String,
    val track: Track,
    val quote: String,
    val meta: String,
    val onOpen: () -> Unit,
    val onPlay: () -> Unit
)

internal class ComposedDiscovery(
    val title: String,
    val lead: String,
    val startLabel: String,
    val loading: Boolean,
    val cards: List<ComposedCard>,
    val onOpen: () -> Unit
)

internal interface SkinHomeKit {
    @Composable
    fun Songs(sectionKey: String, title: String, tracks: List<Track>, ranked: Boolean, currentId: String?, onPlay: (Track) -> Unit, onPlayAll: () -> Unit, onTrackActions: (Track) -> Unit)

    @Composable
    fun Cards(title: String, cards: List<ComposedCard>)

    @Composable
    fun Chips(labels: List<String>, onSelect: (Int) -> Unit)

    @Composable
    fun Hero(hero: StudioHero)

    @Composable
    fun Quotes(title: String, quotes: List<ComposedQuote>)

    @Composable
    fun Discovery(model: ComposedDiscovery)

    @Composable
    fun Notice(message: String, actionLabel: String?, onAction: (() -> Unit)?)

    @Composable
    fun LoadingShelf(title: String)
}

internal class ComposedHomeRequest(
    val blocks: List<HomeBlock>,
    val content: ComposedHomeContent
) {
    val showsSkinFeed: Boolean get() = HomeBlock.SkinFeed in blocks
}

internal class ComposedHomeContent(
    val data: AppleMusicHomeData,
    val actions: AppleMusicHomeActions,
    val deferredReady: Boolean,
    val genreZones: List<ExploreZone>,
    val onOpenZone: (ExploreZone) -> Unit,
    val speedDialPins: List<SpeedDialPin>,
    val onOpenPin: (SpeedDialPin) -> Unit,
    val newReleases: List<Track>,
    val newReleasesTitle: String,
    val videos: List<Track>,
    val onPlayVideo: (List<Track>, Track) -> Unit,
    val releaseRadar: List<ReleaseRadarEntry>,
    val onOpenRelease: (ReleaseRadarEntry) -> Unit,
    val similarArtists: List<com.luc4n3x.levyra.domain.ArtistHit>,
    val moreSections: List<HomeSection>,
    val resonanceTracks: List<Track>,
    val resonanceComments: Map<String, ResonanceCommentSnippet>,
    val onOpenComments: (Track) -> Unit,
    val discovery: DiscoveryHomeHandle?,
    val radio: HomeRadioState,
    val albumsLoading: Boolean,
    val artistsLoading: Boolean,
    val chartsLoading: Boolean,
    val offlineMessage: String?,
    val offlineRetryLabel: String,
    val onOfflineRetry: () -> Unit
)

internal fun LazyListScope.composedHomeSections(
    request: ComposedHomeRequest,
    kit: SkinHomeKit,
    skinFeed: LazyListScope.() -> Unit
) {
    val content = request.content
    content.offlineMessage?.let { message ->
        item(key = "composed-offline", contentType = "composed-notice") {
            kit.Notice(message, content.offlineRetryLabel, content.onOfflineRetry)
        }
    }
    request.blocks.forEach { block ->
        when (block) {
            HomeBlock.SkinFeed -> skinFeed()
            is HomeBlock.Section -> composedSection(block.id, content, kit)
        }
    }
}

private fun LazyListScope.composedSection(id: HomeSectionId, content: ComposedHomeContent, kit: SkinHomeKit) {
    when (id) {
        HomeSectionId.GenreChips -> composedGenres(content, kit)
        HomeSectionId.SpeedDial -> composedSpeedDial(content, kit)
        HomeSectionId.Spotlight -> composedSpotlight(content, kit)
        HomeSectionId.Discovery -> composedDiscovery(content, kit)
        HomeSectionId.PersonalOrbit -> composedOrbit(content, kit)
        HomeSectionId.FavoriteRadio -> composedRadio(content, kit)
        HomeSectionId.EditorialCollections -> composedCollections(content, kit)
        HomeSectionId.QuickPicks -> composedQuickPicks(content, kit)
        HomeSectionId.ForgottenFavorites -> composedForgotten(content, kit)
        HomeSectionId.AlbumsForYou -> composedAlbums(content, kit)
        HomeSectionId.TrendingArtists -> composedArtists(content, kit)
        HomeSectionId.NewReleases -> if (content.deferredReady) composedSongs(content, kit, "new-releases", content.newReleasesTitle, content.newReleases)
        HomeSectionId.MusicVideos -> composedVideos(content, kit)
        HomeSectionId.ReleaseRadar -> composedReleaseRadar(content, kit)
        HomeSectionId.SimilarArtists -> composedSimilarArtists(content, kit)
        HomeSectionId.MoreSections -> composedMoreSections(content, kit)
        HomeSectionId.Resonance -> composedResonance(content, kit)
        HomeSectionId.Charts -> composedCharts(content, kit)
    }
}

private fun LazyListScope.composedSongs(
    content: ComposedHomeContent,
    kit: SkinHomeKit,
    key: String,
    title: String,
    tracks: List<Track>,
    ranked: Boolean = false
) {
    if (tracks.isEmpty() || title.isBlank()) return
    val actions = content.actions
    item(key = "composed-$key", contentType = "composed-songs") {
        kit.Songs(key, title, tracks, ranked, content.data.currentId, { track -> actions.playFrom(tracks, track) }, { actions.playAll(tracks) }, actions.onTrackActions)
    }
}

private fun LazyListScope.composedGenres(content: ComposedHomeContent, kit: SkinHomeKit) {
    if (content.genreZones.isEmpty()) return
    item(key = "composed-genres", contentType = "composed-chips") {
        kit.Chips(remember(content.genreZones) { content.genreZones.map { it.label } }) { index ->
            content.genreZones.getOrNull(index)?.let(content.onOpenZone)
        }
    }
}

private fun LazyListScope.composedSpeedDial(content: ComposedHomeContent, kit: SkinHomeKit) {
    if (content.speedDialPins.isEmpty()) return
    item(key = "composed-speed-dial", contentType = "composed-cards") {
        val strings = LocalLevyraStrings.current
        val pins = remember(content.speedDialPins) {
            content.speedDialPins.map { pin ->
                ComposedCard("pin-${pin.key}", pin.title, pin.subtitle, pin.artworkUrl, track = pin.track) { content.onOpenPin(pin) }
            }
        }
        kit.Cards(strings.speedDialCopy().title, pins)
    }
}

private fun LazyListScope.composedSpotlight(content: ComposedHomeContent, kit: SkinHomeKit) {
    val hero = content.data.soundtrack ?: return
    item(key = "composed-spotlight", contentType = "composed-hero") { kit.Hero(hero) }
}

private fun LazyListScope.composedDiscovery(content: ComposedHomeContent, kit: SkinHomeKit) {
    val handle = content.discovery ?: return
    item(key = "composed-discovery", contentType = "composed-discovery") { ComposedDiscoverySection(handle, kit) }
}

private fun LazyListScope.composedOrbit(content: ComposedHomeContent, kit: SkinHomeKit) {
    val data = content.data
    if (!data.showPersonal || data.personalTracks.isEmpty()) return
    val actions = content.actions
    item(key = "composed-orbit", contentType = "composed-songs") {
        val title = LocalLevyraStrings.current.personalOrbitTitle
        val tracks = data.personalTracks
        if (tracks.isNotEmpty()) kit.Songs("orbit", title, tracks, false, data.currentId, { actions.playFrom(tracks, it) }, { actions.playAll(tracks) }, actions.onTrackActions)
    }
}

private fun LazyListScope.composedRadio(content: ComposedHomeContent, kit: SkinHomeKit) {
    val radio = content.radio
    if (!radio.visible || radio.stations.isEmpty()) return
    item(key = "composed-radio", contentType = "composed-cards") {
        val title = radioLibraryCopy(LocalLevyraStrings.current.code).favorites
        val stations = remember(radio.stations) {
            radio.stations.map { station ->
                ComposedCard("radio-${station.uuid}", station.name, station.country, station.faviconUrl) { radio.onPlay(station) }
            }
        }
        kit.Cards(title, stations)
    }
}

private fun LazyListScope.composedCollections(content: ComposedHomeContent, kit: SkinHomeKit) {
    val data = content.data
    if (!content.deferredReady || data.collections.isEmpty()) return
    item(key = "composed-collections", contentType = "composed-cards") {
        val strings = LocalLevyraStrings.current
        val collections = remember(data.collections) {
            data.collections.map { collection ->
                val cover = collection.tracks.firstOrNull()
                ComposedCard(
                    "collection-${collection.id}", collection.title,
                    collection.tracks.map { it.artist }.distinct().take(2).joinToString(" · "),
                    cover?.largeThumbnailUrl?.ifBlank { cover.thumbnailUrl }.orEmpty()
                ) { content.actions.openCollection(collection.id) }
            }
        }
        kit.Cards(strings.collectionsTitle, collections)
    }
}

private fun LazyListScope.composedQuickPicks(content: ComposedHomeContent, kit: SkinHomeKit) {
    if (!content.deferredReady) return
    val data = content.data
    val section = data.quickPicks ?: return
    val actions = content.actions
    item(key = "composed-quick-picks", contentType = "composed-songs") {
        val title = section.title.ifBlank { LocalLevyraStrings.current.quickPicks }
        kit.Songs("quick-picks", title, section.tracks, false, data.currentId, { actions.playFrom(section.tracks, it) }, { actions.playAll(section.tracks) }, actions.onTrackActions)
    }
}

private fun LazyListScope.composedForgotten(content: ComposedHomeContent, kit: SkinHomeKit) {
    val data = content.data
    if (!content.deferredReady || data.forgottenFavorites.isEmpty()) return
    val actions = content.actions
    item(key = "composed-forgotten", contentType = "composed-songs") {
        val tracks = data.forgottenFavorites
        kit.Songs("forgotten", LocalLevyraStrings.current.forgottenFavorites, tracks, false, data.currentId, { actions.playFrom(tracks, it) }, { actions.playAll(tracks) }, actions.onTrackActions)
    }
}

private fun LazyListScope.composedAlbums(content: ComposedHomeContent, kit: SkinHomeKit) {
    val data = content.data
    if (!content.deferredReady || !data.showAlbums || (data.albums.isEmpty() && !content.albumsLoading)) return
    item(key = "composed-albums", contentType = "composed-cards") {
        val title = LocalLevyraStrings.current.albumsForYou
        val albums = remember(data.albums) {
            data.albums.map { album ->
                ComposedCard("album-${album.browseId.ifBlank { album.title + album.artist }}", album.title, album.artist, album.thumbnailUrl, explicit = album.explicit) { content.actions.openAlbum(album) }
            }
        }
        if (albums.isNotEmpty()) kit.Cards(title, albums) else if (content.albumsLoading) kit.LoadingShelf(title)
    }
}

private fun LazyListScope.composedArtists(content: ComposedHomeContent, kit: SkinHomeKit) {
    val data = content.data
    if (!content.deferredReady || !data.showArtists || (data.artists.isEmpty() && !content.artistsLoading)) return
    item(key = "composed-artists", contentType = "composed-cards") {
        val title = LocalLevyraStrings.current.trendingArtists
        val artists = remember(data.artists) {
            data.artists.map { artist ->
                ComposedCard("artist-${artist.browseId.ifBlank { artist.name }}", artist.name, "", artist.thumbnailUrl, circular = true) { content.actions.openArtist(artist) }
            }
        }
        if (artists.isNotEmpty()) kit.Cards(title, artists) else if (content.artistsLoading) kit.LoadingShelf(title)
    }
}

private fun LazyListScope.composedVideos(content: ComposedHomeContent, kit: SkinHomeKit) {
    if (!content.deferredReady || content.videos.isEmpty()) return
    val actions = content.actions
    item(key = "composed-videos", contentType = "composed-cards") {
        val videos = content.videos
        val cards = remember(videos) {
            videos.map { track ->
                ComposedCard("video-${track.id}", track.title, track.artist, track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }, wide = true, track = track, onLongClick = { actions.onTrackActions(track) }) {
                    content.onPlayVideo(videos, track)
                }
            }
        }
        kit.Cards(LocalLevyraStrings.current.homeVideoShelfTitle, cards)
    }
}

private fun LazyListScope.composedReleaseRadar(content: ComposedHomeContent, kit: SkinHomeKit) {
    if (!content.deferredReady || content.releaseRadar.isEmpty()) return
    item(key = "composed-release-radar", contentType = "composed-cards") {
        val cards = remember(content.releaseRadar) {
            content.releaseRadar.map { entry ->
                ComposedCard("radar-${entry.artistBrowseId}-${entry.release.title}", entry.release.title, entry.artistName, entry.release.thumbnailUrl) { content.onOpenRelease(entry) }
            }
        }
        kit.Cards(LocalLevyraStrings.current.releaseRadar, cards)
    }
}

private fun LazyListScope.composedSimilarArtists(content: ComposedHomeContent, kit: SkinHomeKit) {
    if (!content.deferredReady || content.similarArtists.isEmpty()) return
    item(key = "composed-similar", contentType = "composed-cards") {
        val cards = remember(content.similarArtists) {
            content.similarArtists.map { artist ->
                ComposedCard("similar-${artist.browseId.ifBlank { artist.name }}", artist.name, "", artist.thumbnailUrl, circular = true) { content.actions.openArtist(artist) }
            }
        }
        kit.Cards(LocalLevyraStrings.current.similarToFollowed, cards)
    }
}

private fun LazyListScope.composedMoreSections(content: ComposedHomeContent, kit: SkinHomeKit) {
    if (!content.deferredReady) return
    content.moreSections.forEachIndexed { index, section ->
        composedSongs(content, kit, "more-$index-${section.title.hashCode()}", section.title, section.tracks)
    }
}

private fun LazyListScope.composedResonance(content: ComposedHomeContent, kit: SkinHomeKit) {
    if (!content.deferredReady || content.resonanceTracks.isEmpty()) return
    item(key = "composed-resonance", contentType = "composed-quotes") {
        val tracks = content.resonanceTracks
        val quotes = remember(tracks, content.resonanceComments) {
            tracks.map { track -> composedQuote(track, tracks, content) }
        }
        kit.Quotes(LocalLevyraStrings.current.mostCommentedTracks, quotes)
    }
}

private fun composedQuote(track: Track, tracks: List<Track>, content: ComposedHomeContent): ComposedQuote {
    val snippet = content.resonanceComments[track.id]
    return ComposedQuote(
        "quote-${track.id}", track,
        snippet?.text?.takeIf { it.isNotBlank() } ?: track.artist,
        listOfNotNull(snippet?.countText?.takeIf { it.isNotBlank() }, snippet?.author?.takeIf { it.isNotBlank() }).joinToString(" · "),
        onOpen = { content.onOpenComments(track) },
        onPlay = { content.actions.playFrom(tracks, track) }
    )
}

private fun LazyListScope.composedCharts(content: ComposedHomeContent, kit: SkinHomeKit) {
    val data = content.data
    when {
        data.charts.isNotEmpty() -> composedSongs(content, kit, "charts", data.chartsTitle, data.charts, ranked = true)
        content.chartsLoading -> item(key = "composed-charts-loading", contentType = "composed-loading") { kit.LoadingShelf(data.chartsTitle) }
    }
}

@Composable
private fun ComposedDiscoverySection(handle: DiscoveryHomeHandle, kit: SkinHomeKit) {
    val ui by handle.state.collectAsStateWithLifecycle()
    LaunchedEffect(handle) { handle.onWarm() }
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { discoveryCopy(strings.code) }
    val teaser = ui.teaser
    if (teaser.isEmpty() && (ui.status == DiscoveryFeedStatus.Empty || ui.status == DiscoveryFeedStatus.Error)) {
        kit.Notice(copy.lead, if (ui.status == DiscoveryFeedStatus.Error) strings.offlineHomeRetry else null, handle.onRetry)
        return
    }
    val cards = remember(teaser, copy) {
        teaser.take(DISCOVERY_CARD_LIMIT).map { card ->
            ComposedCard(
                "discovery-${card.key}", card.track.title, discoveryReasonLabel(card, copy),
                card.track.largeThumbnailUrl.ifBlank { card.track.thumbnailUrl }, track = card.track, onClick = handle.onOpen
            )
        }
    }
    kit.Discovery(
        ComposedDiscovery(
            title = copy.title, lead = copy.lead, startLabel = copy.start,
            loading = ui.status == DiscoveryFeedStatus.Loading || ui.status == DiscoveryFeedStatus.Idle,
            cards = cards, onOpen = handle.onOpen
        )
    )
}

private const val DISCOVERY_CARD_LIMIT = 8
