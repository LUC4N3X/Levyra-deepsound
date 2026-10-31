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
    val data = content.data
    val actions = content.actions
    fun songs(key: String, title: String, tracks: List<Track>, ranked: Boolean = false) {
        if (tracks.isEmpty() || title.isBlank()) return
        item(key = "composed-$key", contentType = "composed-songs") {
            kit.Songs(key, title, tracks, ranked, data.currentId, { track -> actions.playFrom(tracks, track) }, { actions.playAll(tracks) }, actions.onTrackActions)
        }
    }
    fun loadingOr(key: String, loading: Boolean, title: String, empty: Boolean, body: () -> Unit) {
        when {
            !empty -> body()
            loading -> item(key = "composed-$key-loading", contentType = "composed-loading") { kit.LoadingShelf(title) }
        }
    }
    when (id) {
        HomeSectionId.GenreChips -> if (content.genreZones.isNotEmpty()) {
            item(key = "composed-genres", contentType = "composed-chips") {
                kit.Chips(remember(content.genreZones) { content.genreZones.map { it.label } }) { index ->
                    content.genreZones.getOrNull(index)?.let(content.onOpenZone)
                }
            }
        }
        HomeSectionId.SpeedDial -> if (content.speedDialPins.isNotEmpty()) {
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
        HomeSectionId.Spotlight -> data.soundtrack?.let { hero ->
            item(key = "composed-spotlight", contentType = "composed-hero") { kit.Hero(hero) }
        }
        HomeSectionId.Discovery -> content.discovery?.let { handle ->
            item(key = "composed-discovery", contentType = "composed-discovery") { ComposedDiscoverySection(handle, kit) }
        }
        HomeSectionId.PersonalOrbit -> if (data.showPersonal && data.personalTracks.isNotEmpty()) {
            item(key = "composed-orbit", contentType = "composed-songs") {
                val title = LocalLevyraStrings.current.personalOrbitTitle
                val tracks = data.personalTracks
                if (tracks.isNotEmpty()) kit.Songs("orbit", title, tracks, false, data.currentId, { actions.playFrom(tracks, it) }, { actions.playAll(tracks) }, actions.onTrackActions)
            }
        }
        HomeSectionId.FavoriteRadio -> if (content.radio.visible && content.radio.stations.isNotEmpty()) {
            item(key = "composed-radio", contentType = "composed-cards") {
                val title = radioLibraryCopy(LocalLevyraStrings.current.code).favorites
                val radio = content.radio
                val stations = remember(radio.stations) {
                    radio.stations.map { station ->
                        ComposedCard("radio-${station.uuid}", station.name, station.country, station.faviconUrl) { radio.onPlay(station) }
                    }
                }
                kit.Cards(title, stations)
            }
        }
        HomeSectionId.EditorialCollections -> if (content.deferredReady && data.collections.isNotEmpty()) {
            item(key = "composed-collections", contentType = "composed-cards") {
                val strings = LocalLevyraStrings.current
                val collections = remember(data.collections) {
                    data.collections.map { collection ->
                        val cover = collection.tracks.firstOrNull()
                        ComposedCard(
                            "collection-${collection.id}", collection.title,
                            collection.tracks.map { it.artist }.distinct().take(2).joinToString(" · "),
                            cover?.largeThumbnailUrl?.ifBlank { cover.thumbnailUrl }.orEmpty()
                        ) { actions.openCollection(collection.id) }
                    }
                }
                kit.Cards(strings.collectionsTitle, collections)
            }
        }
        HomeSectionId.QuickPicks -> if (content.deferredReady) data.quickPicks?.let { section ->
            item(key = "composed-quick-picks", contentType = "composed-songs") {
                val title = section.title.ifBlank { LocalLevyraStrings.current.quickPicks }
                kit.Songs("quick-picks", title, section.tracks, false, data.currentId, { actions.playFrom(section.tracks, it) }, { actions.playAll(section.tracks) }, actions.onTrackActions)
            }
        }
        HomeSectionId.ForgottenFavorites -> if (content.deferredReady && data.forgottenFavorites.isNotEmpty()) {
            item(key = "composed-forgotten", contentType = "composed-songs") {
                val tracks = data.forgottenFavorites
                kit.Songs("forgotten", LocalLevyraStrings.current.forgottenFavorites, tracks, false, data.currentId, { actions.playFrom(tracks, it) }, { actions.playAll(tracks) }, actions.onTrackActions)
            }
        }
        HomeSectionId.AlbumsForYou -> if (content.deferredReady && data.showAlbums && (data.albums.isNotEmpty() || content.albumsLoading)) {
            item(key = "composed-albums", contentType = "composed-cards") {
                val title = LocalLevyraStrings.current.albumsForYou
                val albums = remember(data.albums) {
                    data.albums.map { album ->
                        ComposedCard("album-${album.browseId.ifBlank { album.title + album.artist }}", album.title, album.artist, album.thumbnailUrl, explicit = album.explicit) { actions.openAlbum(album) }
                    }
                }
                if (albums.isNotEmpty()) kit.Cards(title, albums) else if (content.albumsLoading) kit.LoadingShelf(title)
            }
        }
        HomeSectionId.TrendingArtists -> if (content.deferredReady && data.showArtists && (data.artists.isNotEmpty() || content.artistsLoading)) {
            item(key = "composed-artists", contentType = "composed-cards") {
                val title = LocalLevyraStrings.current.trendingArtists
                val artists = remember(data.artists) {
                    data.artists.map { artist ->
                        ComposedCard("artist-${artist.browseId.ifBlank { artist.name }}", artist.name, "", artist.thumbnailUrl, circular = true) { actions.openArtist(artist) }
                    }
                }
                if (artists.isNotEmpty()) kit.Cards(title, artists) else if (content.artistsLoading) kit.LoadingShelf(title)
            }
        }
        HomeSectionId.NewReleases -> if (content.deferredReady) songs("new-releases", content.newReleasesTitle, content.newReleases)
        HomeSectionId.MusicVideos -> if (content.deferredReady && content.videos.isNotEmpty()) {
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
        HomeSectionId.ReleaseRadar -> if (content.deferredReady && content.releaseRadar.isNotEmpty()) {
            item(key = "composed-release-radar", contentType = "composed-cards") {
                val cards = remember(content.releaseRadar) {
                    content.releaseRadar.map { entry ->
                        ComposedCard("radar-${entry.artistBrowseId}-${entry.release.title}", entry.release.title, entry.artistName, entry.release.thumbnailUrl) { content.onOpenRelease(entry) }
                    }
                }
                kit.Cards(LocalLevyraStrings.current.releaseRadar, cards)
            }
        }
        HomeSectionId.SimilarArtists -> if (content.deferredReady && content.similarArtists.isNotEmpty()) {
            item(key = "composed-similar", contentType = "composed-cards") {
                val cards = remember(content.similarArtists) {
                    content.similarArtists.map { artist ->
                        ComposedCard("similar-${artist.browseId.ifBlank { artist.name }}", artist.name, "", artist.thumbnailUrl, circular = true) { actions.openArtist(artist) }
                    }
                }
                kit.Cards(LocalLevyraStrings.current.similarToFollowed, cards)
            }
        }
        HomeSectionId.MoreSections -> if (content.deferredReady) {
            content.moreSections.forEachIndexed { index, section -> songs("more-$index-${section.title.hashCode()}", section.title, section.tracks) }
        }
        HomeSectionId.Resonance -> if (content.deferredReady && content.resonanceTracks.isNotEmpty()) {
            item(key = "composed-resonance", contentType = "composed-quotes") {
                val tracks = content.resonanceTracks
                val quotes = remember(tracks, content.resonanceComments) {
                    tracks.map { track ->
                        val snippet = content.resonanceComments[track.id]
                        ComposedQuote(
                            "quote-${track.id}", track,
                            snippet?.text?.takeIf { it.isNotBlank() } ?: track.artist,
                            listOfNotNull(snippet?.countText?.takeIf { it.isNotBlank() }, snippet?.author?.takeIf { it.isNotBlank() }).joinToString(" · "),
                            onOpen = { content.onOpenComments(track) },
                            onPlay = { actions.playFrom(tracks, track) }
                        )
                    }
                }
                kit.Quotes(LocalLevyraStrings.current.mostCommentedTracks, quotes)
            }
        }
        HomeSectionId.Charts -> loadingOr("charts", content.chartsLoading, data.chartsTitle, data.charts.isEmpty()) {
            songs("charts", data.chartsTitle, data.charts, ranked = true)
        }
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
