package com.luc4n3x.levyra.viewmodel

import android.app.Application
import com.luc4n3x.levyra.data.FavoritesStore
import com.luc4n3x.levyra.data.HomeStartupWorkPlan
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.OfficialArtworkRepository
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.albumRecommendationDeduplicationKey
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.search.LevyraSearchEngine
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private const val OFFICIAL_METADATA_MAX_BATCH_SIZE = 8
private const val OFFICIAL_METADATA_SEARCH_BACKLOG = OFFICIAL_METADATA_MAX_BATCH_SIZE * 4
private const val OFFICIAL_METADATA_CONCURRENCY = 2

internal interface OfficialMetadataHost {
    val smartOrbitDiscoveries: List<Track>
    val exploreCache: ConcurrentHashMap<String, List<Track>>

    fun homeStartupWorkPlan(): HomeStartupWorkPlan

    suspend fun awaitHomeUiIdle(plan: HomeStartupWorkPlan = homeStartupWorkPlan())

    fun smartOrbitExclusion(): (Track) -> Boolean

    fun searchAlbumDeduplicationKey(album: AlbumHit): String

    fun persistHomeSnapshot()
}

internal class OfficialMetadataController(
    private val scope: CoroutineScope,
    private val application: Application,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val officialArtworkRepository: OfficialArtworkRepository,
    private val repository: YoutubeMusicRepository,
    private val queueEngine: PersistentQueueEngine,
    private val favoritesStore: FavoritesStore,
    private val playlistStore: PlaylistStore,
    private val preferences: LevyraPreferences,
    private val searchEngine: LevyraSearchEngine,
    private val host: OfficialMetadataHost
) {
    private val officialMetadataSignal = Channel<Unit>(Channel.CONFLATED)
    private val officialMetadataPending = ConcurrentHashMap<String, Track>()
    @Volatile private var deferredSearchArtwork: List<Track> = emptyList()
    private val officialMetadataInFlightKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val officialMetadataDeferUntilIdle = AtomicBoolean(false)
    private var orbitArtworkJob: Job? = null

    fun release() {
        orbitArtworkJob?.cancel()
        officialMetadataSignal.close()
    }

    fun enqueueSearchArtwork(songs: List<Track>) {
        if (officialMetadataPending.size < OFFICIAL_METADATA_SEARCH_BACKLOG) {
            deferredSearchArtwork = emptyList()
            enqueueOfficialMetadata(songs, OFFICIAL_METADATA_MAX_BATCH_SIZE, false)
        } else {
            deferredSearchArtwork = songs.take(OFFICIAL_METADATA_MAX_BATCH_SIZE)
        }
    }

    fun refreshMissingOfficialOrbitArtwork(tracks: List<Track>, deferUntilHomeIdle: Boolean = false) {
        orbitArtworkJob?.cancel()
        orbitArtworkJob = scope.launch {
            val startupPlan = host.homeStartupWorkPlan()
            delay(if (deferUntilHomeIdle) startupPlan.secondaryStartDelayMs else 250L)
            if (deferUntilHomeIdle) host.awaitHomeUiIdle(startupPlan)
            enqueueOfficialMetadata(tracks, LevyraPersonalOrbit.DISPLAY_LIMIT, false)
            uiState.value.quickPickSeeds
                .chunked(OFFICIAL_METADATA_MAX_BATCH_SIZE)
                .forEach { seeds -> enqueueOfficialMetadata(seeds, OFFICIAL_METADATA_MAX_BATCH_SIZE, false) }
        }
    }

    fun enqueueOfficialMetadata(
        tracks: List<Track>,
        limit: Int,
        deferUntilHomeIdle: Boolean
    ) {
        val pending = tracks
            .asSequence()
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .filter(::needsOfficialMetadata)
            .distinctBy { LevyraPersonalOrbit.identityKey(it) }
            .take(limit.coerceIn(1, OFFICIAL_METADATA_MAX_BATCH_SIZE))
            .toList()
        if (pending.isEmpty()) return
        pending.forEach { track ->
            val key = LevyraPersonalOrbit.identityKey(track)
            if (key !in officialMetadataInFlightKeys) {
                officialMetadataPending.merge(key, track) { existing, incoming -> richerOfficialMetadataSeed(existing, incoming) }
            }
        }
        if (officialMetadataPending.isEmpty()) return
        if (deferUntilHomeIdle) officialMetadataDeferUntilIdle.set(true)
        officialMetadataSignal.trySend(Unit)
    }

    private fun richerOfficialMetadataSeed(existing: Track, incoming: Track): Track {
        return if (officialMetadataSeedScore(incoming) >= officialMetadataSeedScore(existing)) incoming else existing
    }

    private fun officialMetadataSeedScore(track: Track): Int {
        return listOf(
            track.album,
            track.thumbnailUrl,
            track.largeThumbnailUrl,
            track.isrc,
            track.upc,
            track.releaseDate,
            track.albumBrowseId,
            track.canonicalAlbumUrl
        ).count(String::isNotBlank) * 10 + track.metadataConfidence.coerceIn(0, 100)
    }

    suspend fun consumeOfficialMetadataQueue() {
        for (ignored in officialMetadataSignal) {
            while (currentCoroutineContext().isActive) {
                if (officialMetadataDeferUntilIdle.getAndSet(false)) {
                    host.awaitHomeUiIdle(host.homeStartupWorkPlan())
                }
                val batch = officialMetadataPending.entries
                    .asSequence()
                    .mapNotNull { entry ->
                        if (officialMetadataInFlightKeys.add(entry.key)) entry.key to entry.value else null
                    }
                    .take(OFFICIAL_METADATA_MAX_BATCH_SIZE)
                    .toList()
                if (batch.isEmpty()) break
                batch.forEach { (key, track) -> officialMetadataPending.remove(key, track) }
                val deferredSearch = deferredSearchArtwork
                if (deferredSearch.isNotEmpty() && officialMetadataPending.size < OFFICIAL_METADATA_SEARCH_BACKLOG) {
                    deferredSearchArtwork = emptyList()
                    enqueueOfficialMetadata(deferredSearch, OFFICIAL_METADATA_MAX_BATCH_SIZE, false)
                }
                val enrichedTracks = Collections.synchronizedList(mutableListOf<Track>())
                val semaphore = Semaphore(OFFICIAL_METADATA_CONCURRENCY)
                coroutineScope {
                    batch.map { (key, track) ->
                        launch {
                            try {
                                semaphore.withPermit {
                                    if (!isActive) return@withPermit
                                    host.awaitHomeUiIdle()
                                    val enriched = resolveOfficialOrbitArtwork(track, uiState.value.languageCode)
                                        ?: return@withPermit
                                    enrichedTracks += enriched
                                }
                            } finally {
                                officialMetadataInFlightKeys.remove(key)
                            }
                        }
                    }.forEach { it.join() }
                }
                if (enrichedTracks.isNotEmpty()) {
                    host.awaitHomeUiIdle()
                    applyOfficialOrbitArtworkBatch(enrichedTracks.toList())
                }
                if (officialMetadataPending.isEmpty()) break
            }
        }
    }

    private suspend fun resolveOfficialOrbitArtwork(track: Track, languageCode: String): Track? {
        if (!needsOfficialMetadata(track)) return track
        val selectedCountry = ChartsCatalog.regions
            .firstOrNull { it.id == uiState.value.selectedChartId }
            ?.country
            .orEmpty()
            .ifBlank { ChartsCatalog.defaultRegionForLanguage(languageCode).country }
        val official = try {
            officialArtworkRepository.find(track, selectedCountry)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        if (official != null) {
            val enrichedGenres = official.genres.map { it.lowercase(Locale.ROOT) }.toSet()
            val combinedMoodTags = (track.moodTags + enrichedGenres).filter { it.isNotBlank() }.toSet()
            return track.copy(
                album = official.album.ifBlank { track.album },
                thumbnailUrl = official.thumbnailUrl,
                largeThumbnailUrl = official.largeThumbnailUrl,
                isrc = official.isrc.ifBlank { track.isrc },
                upc = official.upc.ifBlank { track.upc },
                releaseDate = official.releaseDate.ifBlank { track.releaseDate },
                year = official.year.ifBlank { track.year },
                trackNumber = official.trackNumber.takeIf { it > 0 } ?: track.trackNumber,
                discNumber = official.discNumber.takeIf { it > 0 } ?: track.discNumber,
                trackTotal = official.trackTotal.takeIf { it > 0 } ?: track.trackTotal,
                discTotal = official.discTotal.takeIf { it > 0 } ?: track.discTotal,
                composer = official.composer.ifBlank { track.composer },
                albumArtist = official.albumArtist.ifBlank { track.albumArtist },
                copyright = official.copyright.ifBlank { track.copyright },
                appleSongId = official.appleSongId.ifBlank { track.appleSongId },
                appleAlbumId = official.appleAlbumId.ifBlank { track.appleAlbumId },
                explicit = official.explicit || track.explicit,
                metadataProvider = official.provider.ifBlank { track.metadataProvider },
                metadataConfidence = maxOf(track.metadataConfidence, officialMetadataConfidence(official.score)),
                canonicalAlbumUrl = official.canonicalAlbumUrl.ifBlank { track.canonicalAlbumUrl },
                moodTags = combinedMoodTags
            )
        }
        val musicMatches = runCatching {
            repository.search("${track.title} ${track.artist}", 10, languageCode)
        }.getOrDefault(emptyList())
        val officialTrack = LevyraPersonalOrbit.prepareForOrbit(track, musicMatches)
        return officialTrack.takeIf { LevyraPersonalOrbit.hasSquareAlbumArtwork(it) }
    }

    private suspend fun applyOfficialOrbitArtworkBatch(enrichedTracks: List<Track>) {
        val enrichedByKey = enrichedTracks
            .distinctBy { LevyraPersonalOrbit.identityKey(it) }
            .associateBy { LevyraPersonalOrbit.identityKey(it) }
        if (enrichedByKey.isEmpty()) return
        val orbitExclusion = host.smartOrbitExclusion()
        var persistedHistory: List<Track> = emptyList()
        var persistedOrbit: List<Track> = emptyList()
        var persistedHomeAlbums: List<AlbumHit> = emptyList()
        var persistedFavorites: List<Track> = emptyList()
        var persistedPlaylists: List<Playlist> = emptyList()
        var shouldPersistFavorites = false
        var shouldPersistPlaylists = false
        var languageCode = uiState.value.languageCode
        var searchArtworkPatch: ((SearchResults) -> SearchResults)? = null
        uiState.update { current ->
            fun withArtwork(item: Track): Track {
                if (com.luc4n3x.levyra.data.spotify.preservesPersonalMetadata(item)) return item
                val enriched = enrichedByKey[LevyraPersonalOrbit.identityKey(item)] ?: return item
                val keepsArtwork = LevyraPersonalOrbit.hasSquareAlbumArtwork(item) &&
                    artworkPreference(item.thumbnailUrl) >= artworkPreference(enriched.thumbnailUrl)
                return item.copy(
                    album = if (keepsArtwork) item.album.ifBlank { enriched.album } else enriched.album.ifBlank { item.album },
                    thumbnailUrl = if (keepsArtwork) item.thumbnailUrl else enriched.thumbnailUrl.ifBlank { item.thumbnailUrl },
                    largeThumbnailUrl = if (keepsArtwork) item.largeThumbnailUrl else enriched.largeThumbnailUrl.ifBlank { item.largeThumbnailUrl },
                    isrc = enriched.isrc.ifBlank { item.isrc },
                    upc = enriched.upc.ifBlank { item.upc },
                    releaseDate = enriched.releaseDate.ifBlank { item.releaseDate },
                    year = enriched.year.ifBlank { item.year },
                    trackNumber = enriched.trackNumber.takeIf { it > 0 } ?: item.trackNumber,
                    discNumber = enriched.discNumber.takeIf { it > 0 } ?: item.discNumber,
                    trackTotal = enriched.trackTotal.takeIf { it > 0 } ?: item.trackTotal,
                    discTotal = enriched.discTotal.takeIf { it > 0 } ?: item.discTotal,
                    composer = enriched.composer.ifBlank { item.composer },
                    albumArtist = enriched.albumArtist.ifBlank { item.albumArtist },
                    copyright = enriched.copyright.ifBlank { item.copyright },
                    appleSongId = enriched.appleSongId.ifBlank { item.appleSongId },
                    appleAlbumId = enriched.appleAlbumId.ifBlank { item.appleAlbumId },
                    explicit = enriched.explicit || item.explicit,
                    metadataProvider = enriched.metadataProvider.ifBlank { item.metadataProvider },
                    metadataConfidence = maxOf(item.metadataConfidence, enriched.metadataConfidence),
                    canonicalAlbumUrl = enriched.canonicalAlbumUrl.ifBlank { item.canonicalAlbumUrl },
                    moodTags = (item.moodTags + enriched.moodTags).filter { it.isNotBlank() }.toSet()
                )
            }

            fun normalizedMetadataText(value: String): String =
                com.luc4n3x.levyra.nexus.identity.MusicIdentityText.normalize(value)

            fun withAlbumMetadata(item: AlbumHit): AlbumHit {
                val targetTitle = normalizedMetadataText(item.title)
                val targetArtist = normalizedMetadataText(item.artist)
                val enriched = enrichedByKey.values
                    .asSequence()
                    .mapNotNull { track ->
                        val candidateAlbum = normalizedMetadataText(track.album)
                        val candidateArtist = normalizedMetadataText(track.artist)
                        val albumScore = when {
                            targetTitle.isBlank() || candidateAlbum.isBlank() -> 0
                            targetTitle == candidateAlbum -> 4
                            targetTitle.contains(candidateAlbum) || candidateAlbum.contains(targetTitle) -> 2
                            else -> 0
                        }
                        val artistScore = when {
                            targetArtist.isBlank() || candidateArtist.isBlank() -> 0
                            targetArtist == candidateArtist -> 3
                            targetArtist.contains(candidateArtist) || candidateArtist.contains(targetArtist) -> 1
                            else -> 0
                        }
                        val sameArtistBrowseId = item.artistBrowseId.isNotBlank() &&
                            track.artistBrowseIds.any { it.equals(item.artistBrowseId, ignoreCase = true) }
                        val sameAlbumBrowseId = item.browseId.isNotBlank() &&
                            track.albumBrowseId.equals(item.browseId, ignoreCase = true)
                        val sameUpc = item.upc.isNotBlank() && track.upc.isNotBlank() &&
                            item.upc.equals(track.upc, ignoreCase = true)
                        val strongIdentifier = sameAlbumBrowseId || sameUpc
                        val artistCompatible = artistScore > 0 || sameArtistBrowseId
                        if (!com.luc4n3x.levyra.data.spotify.sameSpotifyAlbum(item.title, track.album)) {
                            return@mapNotNull null
                        }
                        if (!artistCompatible) return@mapNotNull null
                        val score = albumScore + artistScore + if (strongIdentifier) 4 else 0
                        (track to score).takeIf { score >= 5 }
                    }
                    .maxWithOrNull(compareBy<Pair<Track, Int>> { it.second }.thenBy { it.first.metadataConfidence })
                    ?.first
                    ?: return item
                return item.copy(
                    title = enriched.album.ifBlank { item.title },
                    year = enriched.year.ifBlank { item.year },
                    thumbnailUrl = enriched.largeThumbnailUrl.ifBlank {
                        enriched.thumbnailUrl.ifBlank { item.thumbnailUrl }
                    },
                    explicit = enriched.explicit || item.explicit,
                    releaseDate = enriched.releaseDate.ifBlank { item.releaseDate },
                    upc = enriched.upc.ifBlank { item.upc },
                    canonicalUrl = enriched.canonicalAlbumUrl.ifBlank { item.canonicalUrl },
                    metadataProvider = enriched.metadataProvider.ifBlank { item.metadataProvider },
                    metadataConfidence = maxOf(item.metadataConfidence, enriched.metadataConfidence)
                )
            }

            val currentTrack = current.currentTrack?.let(::withArtwork)
            val recentSearches = current.recentSearches.map(::withArtwork)
            val cachedOrbit = current.personalOrbitTracks.map(::withArtwork)
            val tracks = current.tracks.map(::withArtwork)
            val queue = current.queue.map(::withArtwork)
            val searchResults = current.searchResults.map(::withArtwork)
            val favorites = current.favorites.map(::withArtwork)
            val charts = current.charts.map(::withArtwork)
            val quickPickSeeds = current.quickPickSeeds.map(::withArtwork)
            val homeAlbums = current.homeAlbums
                .map(::withAlbumMetadata)
                .distinctBy(::albumRecommendationDeduplicationKey)
            val homeSections = current.homeSections.map { section ->
                section.copy(tracks = section.tracks.map(::withArtwork))
            }
            val exploreTracks = current.exploreTracks.map(::withArtwork)
            val exploreFreshTracks = current.exploreFreshTracks.map(::withArtwork)
            val exploreWorldFreshTracks = current.exploreWorldFreshTracks.map(::withArtwork)
            val exploreVideos = current.exploreVideos.map(::withArtwork)
            val exploreSamples = current.exploreSamples.map(::withArtwork)
            val recentListens = current.recentListens.map(::withArtwork)
            val playlists = current.playlists.map { playlist ->
                playlist.copy(tracks = playlist.tracks.map(::withArtwork))
            }
            val openPlaylist = current.openPlaylist?.let { playlist ->
                playlist.copy(tracks = playlist.tracks.map(::withArtwork))
            }
            val patchSearchArtwork: (SearchResults) -> SearchResults = { results ->
                results.copy(
                    topTrack = results.topTrack?.let(::withArtwork),
                    songs = results.songs.map(::withArtwork),
                    albums = results.albums
                        .map(::withAlbumMetadata)
                        .distinctBy(host::searchAlbumDeduplicationKey)
                )
            }
            searchArtworkPatch = patchSearchArtwork
            val searchData = patchSearchArtwork(current.searchData)
            val albumDetail = current.albumDetail?.let { detail ->
                detail.copy(
                    album = withAlbumMetadata(detail.album),
                    tracks = detail.tracks.map(::withArtwork),
                    otherVersions = detail.otherVersions.map(::withAlbumMetadata)
                )
            }
            val artistProfile = current.artistProfile?.let { profile ->
                profile.copy(
                    topSongs = profile.topSongs.map(::withArtwork),
                    videos = profile.videos.map(::withArtwork)
                )
            }
            val orbit = LevyraPersonalOrbit.build(
                currentTrack = currentTrack,
                recentSearches = recentSearches,
                favorites = favorites,
                tracks = tracks,
                homeSections = homeSections,
                charts = charts,
                cachedOrbit = cachedOrbit,
                limit = LevyraPersonalOrbit.DISPLAY_LIMIT,
                languageCode = current.languageCode,
                discoveries = host.smartOrbitDiscoveries,
                excluded = orbitExclusion
            )
            persistedHistory = recentSearches
            persistedOrbit = orbit
            persistedHomeAlbums = homeAlbums
            persistedFavorites = favorites
            persistedPlaylists = (playlists + listOfNotNull(openPlaylist)).distinctBy { it.id }
            shouldPersistFavorites = current.favorites.any {
                LevyraPersonalOrbit.identityKey(it) in enrichedByKey.keys
            }
            shouldPersistPlaylists = persistedPlaylists.any { playlist ->
                playlist.tracks.any { LevyraPersonalOrbit.identityKey(it) in enrichedByKey.keys }
            }
            languageCode = current.languageCode
            current.copy(
                currentTrack = currentTrack,
                recentSearches = recentSearches,
                personalOrbitTracks = orbit,
                tracks = tracks,
                queue = queue,
                searchResults = searchResults,
                favorites = favorites,
                charts = charts,
                quickPickSeeds = quickPickSeeds,
                homeAlbums = homeAlbums,
                homeSections = homeSections,
                exploreTracks = exploreTracks,
                exploreFreshTracks = exploreFreshTracks,
                exploreWorldFreshTracks = exploreWorldFreshTracks,
                exploreVideos = exploreVideos,
                exploreSamples = exploreSamples,
                recentListens = recentListens,
                playlists = playlists,
                openPlaylist = openPlaylist,
                searchData = searchData,
                albumDetail = albumDetail,
                artistProfile = artistProfile
            )
        }
        val exploreSnapshot = uiState.value
        val exploreCacheKey = when {
            exploreSnapshot.exploreCategoryParams != null ->
                "provider:${exploreSnapshot.languageCode}:${exploreSnapshot.exploreCategoryParams}"
            exploreSnapshot.exploreZoneId != null ->
                "zone:${exploreSnapshot.languageCode}:${exploreSnapshot.exploreZoneId}"
            else -> null
        }
        if (exploreCacheKey != null && exploreSnapshot.exploreTracks.isNotEmpty()) {
            host.exploreCache[exploreCacheKey] = exploreSnapshot.exploreTracks
        }
        searchArtworkPatch?.let(searchEngine::patchResults)
        host.persistHomeSnapshot()
        enrichedByKey.values.forEach { queueEngine.updateTrackMetadata(it) }
        val appContext = application.applicationContext
        val artworkTracks = persistedOrbit
            .filter { LevyraPersonalOrbit.identityKey(it) in enrichedByKey.keys }
            .take(LevyraPersonalOrbit.DISPLAY_LIMIT)
        withContext(Dispatchers.IO) {
            preferences.saveRecentSearches(persistedHistory)
            preferences.savePersonalOrbitTracks(persistedOrbit, languageCode)
            preferences.saveHomeAlbums(persistedHomeAlbums, languageCode)
            if (shouldPersistFavorites) favoritesStore.saveSuspending(persistedFavorites)
            if (shouldPersistPlaylists) playlistStore.updateTrackMetadata(persistedPlaylists)
            if (artworkTracks.isNotEmpty()) {
                LevyraArtworkCache.cachePersistent(appContext, artworkTracks, artworkTracks.size)
            }
        }
        if (artworkTracks.isNotEmpty()) {
            LevyraArtworkCache.preloadPriority(
                appContext,
                artworkTracks,
                artworkTracks.size
            )
        }
    }
}

internal fun needsOfficialMetadata(track: Track): Boolean {
    if (com.luc4n3x.levyra.data.spotify.preservesPersonalMetadata(track)) return false
    return !LevyraPersonalOrbit.hasSquareAlbumArtwork(track) ||
        !isAppleArtworkUrl(track.thumbnailUrl) ||
        (track.canonicalAlbumUrl.isBlank() && track.releaseDate.isBlank())
}

private fun isAppleArtworkUrl(url: String): Boolean =
    url.startsWith("https://") && url.substringAfter("://").substringBefore('/').endsWith(".mzstatic.com")

internal fun officialMetadataConfidence(score: Int): Int = when {
    score >= 500 -> 100
    score >= 420 -> 96
    score >= 360 -> 91
    score >= 320 -> 86
    score >= 280 -> 80
    score >= 240 -> 72
    score >= 200 -> 64
    else -> (score / 4).coerceIn(0, 63)
}

private fun isSpotifyArtworkUrl(url: String): Boolean = url.startsWith("https://i.scdn.co/image/")

private fun artworkPreference(url: String): Int = when {
    isAppleArtworkUrl(url) -> 2
    isSpotifyArtworkUrl(url) -> 1
    else -> 0
}
