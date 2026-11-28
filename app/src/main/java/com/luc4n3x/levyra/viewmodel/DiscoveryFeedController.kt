package com.luc4n3x.levyra.viewmodel

import android.app.Application
import com.luc4n3x.levyra.data.ChartsRepository
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.preserveEditorialArtwork
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.OnboardingArtists
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.providers.LevyraProviderRouter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val DISCOVERY_PLAYLIST_SIGNAL_LIMIT = 300
private const val DISCOVERY_SOURCE_LIMIT = 16
private const val DISCOVERY_REGIONAL_LIMIT = 30
private const val DISCOVERY_RELEASE_RADAR_LIMIT = 5
private const val DISCOVERY_ARTIST_LIMIT = 8
private const val DISCOVERY_SCENE_LANGUAGE = "en"
private const val DISCOVERY_SCENE_PLAYLISTS = 2
private const val DISCOVERY_SCOUT_PAGES = 5
private const val DISCOVERY_SCOUT_PAGE = 40
private const val DISCOVERY_SCOUT_ARTISTS = 10
private const val DISCOVERY_RESOLVE_TIMEOUT_MS = 12_000L
private const val DISCOVERY_MATCH_SEARCH_LIMIT = 8
private const val DISCOVERY_MATCH_DURATION_TOLERANCE_MS = 20_000L
private const val DISCOVERY_MATCH_MIN_SCORE = 160
private const val ONBOARDING_DISCOVERY_SEED_ARTISTS = 4
private const val CHART_DISCOVERY_SEEDS = 8

internal class DiscoveryFeedController(
    private val application: Application,
    private val uiState: StateFlow<LevyraUiState>,
    private val preferences: LevyraPreferences,
    private val repository: YoutubeMusicRepository,
    private val chartsRepository: ChartsRepository,
    private val providerRouter: LevyraProviderRouter
) {
    internal fun discoveryProfile(): com.luc4n3x.levyra.feature.discovery.DiscoveryTasteProfile {
        val snapshot = uiState.value
        val market = com.luc4n3x.levyra.domain.LevyraContentLocales.forLanguage(snapshot.languageCode).chartCountry
        return com.luc4n3x.levyra.feature.discovery.DiscoveryTasteProfile.from(
            languageCode = snapshot.languageCode,
            market = market,
            recentListens = snapshot.recentListens,
            favorites = snapshot.favorites,
            playlistTracks = snapshot.playlists.asSequence().flatMap { it.tracks.asSequence() }.take(DISCOVERY_PLAYLIST_SIGNAL_LIMIT).toList(),
            followedArtists = snapshot.followedArtists.map { it.name },
            excludedArtists = snapshot.excludedArtists.map { it.name },
            similarArtists = snapshot.similarArtists.map { it.name } + OnboardingArtists.selectedNames(preferences.tastes())
        )
    }

    internal suspend fun onboardingDiscoverySeeds(): List<Track> = withContext(Dispatchers.IO) {
        val language = uiState.value.languageCode
        OnboardingArtists.selectedNames(preferences.tastes())
            .take(ONBOARDING_DISCOVERY_SEED_ARTISTS)
            .mapNotNull { artist ->
                runCatchingPreservingCancellation { repository.search(artist, 1, language).firstOrNull() }.getOrNull()
            }
    }

    internal suspend fun chartDiscoverySeeds(market: String): List<Track> = withContext(Dispatchers.IO) {
        val language = uiState.value.languageCode
        runCatchingPreservingCancellation { chartsRepository.topSongs(market, CHART_DISCOVERY_SEEDS) }.getOrNull().orEmpty()
            .mapNotNull { track ->
                if (com.luc4n3x.levyra.data.PlaybackSourceIdentity.sourceVideoId(track).isNotBlank()) {
                    track
                } else {
                    runCatchingPreservingCancellation { repository.search("${track.artist} ${track.title}", 1, language).firstOrNull() }.getOrNull()
                }
            }
    }

    internal fun discoverySources(): com.luc4n3x.levyra.feature.discovery.DiscoverySources =
        com.luc4n3x.levyra.feature.discovery.DiscoverySources(
            similarToTrack = { seed -> repository.radio(seed, uiState.value.languageCode, limit = DISCOVERY_SOURCE_LIMIT, diversify = true) },
            newMusic = { market, language -> discoveryNewMusic(market, language) },
            followedReleases = { discoveryFollowedReleases() },
            similarArtistTracks = { artist -> discoveryArtistTracks(artist) },
            regional = { market -> chartsRepository.topSongs(market, DISCOVERY_REGIONAL_LIMIT) },
            editorial = { language ->
                com.luc4n3x.levyra.domain.LevyraContentLocales.forLanguage(language).homeQueries.take(2)
                    .flatMap { query -> repository.search(query, DISCOVERY_SOURCE_LIMIT, language) }
            },
            sceneCategories = { discoveryScenes() },
            sceneTracks = { params -> discoverySceneTracks(params) },
            cityTracks = { city, rotation -> discoveryCityTracks(city, rotation) },
            decadeTracks = { year, rotation -> discoveryDecadeTracks(year, rotation) },
            distantRegions = { market, language ->
                com.luc4n3x.levyra.feature.discovery.BubbleRegions.distant(
                    com.luc4n3x.levyra.domain.ChartsCatalog.regions.map { it.id to it.label },
                    market,
                    language
                )
            }
        )

    private val discoveryScout by lazy { com.luc4n3x.levyra.feature.discovery.MusicBrainzScout() }
    private val discoverySceneMutex = kotlinx.coroutines.sync.Mutex()
    @Volatile private var discoverySceneCache: List<com.luc4n3x.levyra.domain.ExploreCategory> = emptyList()

    internal suspend fun discoveryScenes(): List<com.luc4n3x.levyra.domain.ExploreCategory> = discoverySceneMutex.withLock {
        discoverySceneCache.takeIf { it.isNotEmpty() }?.let { return@withLock it }
        val all = repository.moodCategories(DISCOVERY_SCENE_LANGUAGE)
        val genres = all.filter { it.section.contains("genre", ignoreCase = true) }
            .ifEmpty { all.filter { com.luc4n3x.levyra.domain.MoodCategoryKind.fromTitle(it.title) == null } }
            .filter { it.params.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.params }
        if (genres.isNotEmpty()) discoverySceneCache = genres
        genres
    }

    private suspend fun discoverySceneTracks(params: String): List<Track> {
        val language = uiState.value.languageCode
        val playlists = repository.moodPlaylists(params, language, DISCOVERY_SCENE_PLAYLISTS * 3)
            .filter { it.playlistId.isNotBlank() }
            .shuffled(kotlin.random.Random(params.hashCode().toLong() + java.time.LocalDate.now().toEpochDay()))
            .take(DISCOVERY_SCENE_PLAYLISTS)
        return coroutineScope {
            playlists.map { playlist ->
                async { runCatchingPreservingCancellation { repository.playlist(playlist.playlistId, language, DISCOVERY_SOURCE_LIMIT * 2)?.tracks }.getOrNull().orEmpty() }
            }.flatMap { it.await() }
        }
    }

    private suspend fun discoveryCityTracks(city: String, rotation: Long): List<Track> {
        val offset = Math.floorMod(rotation, DISCOVERY_SCOUT_PAGES.toLong()).toInt() * DISCOVERY_SCOUT_PAGE
        val artists = discoveryScout.artistsFromArea(city, offset).ifEmpty { if (offset > 0) discoveryScout.artistsFromArea(city, 0) else emptyList() }
        return coroutineScope {
            artists.take(DISCOVERY_SCOUT_ARTISTS).map { artist ->
                async { runCatchingPreservingCancellation { discoveryArtistTracks(artist.name).take(2) }.getOrDefault(emptyList()) }
            }.flatMap { it.await() }
        }
    }

    private suspend fun discoveryDecadeTracks(startYear: Int, rotation: Long): List<Track> {
        val offset = Math.floorMod(rotation, DISCOVERY_SCOUT_PAGES.toLong()).toInt() * DISCOVERY_SCOUT_PAGE
        val releases = discoveryScout.releasesFromDecade(startYear, offset)
        val language = uiState.value.languageCode
        return coroutineScope {
            releases.take(DISCOVERY_SCOUT_ARTISTS).map { release ->
                async {
                    runCatchingPreservingCancellation {
                        val artistKey = com.luc4n3x.levyra.feature.discovery.discoveryPrimaryArtistKey(release.artist)
                        val albumKey = playbackTitleKey(release.title)
                        repository.search("${release.artist} ${release.title}", DISCOVERY_ARTIST_LIMIT, language)
                            .filter { candidate ->
                                artistKey.isNotBlank() &&
                                    com.luc4n3x.levyra.feature.discovery.discoveryArtistKeys(candidate.artist).contains(artistKey) &&
                                    (playbackTitleKey(candidate.album) == albumKey ||
                                        candidate.year.take(4).toIntOrNull()?.let { it in startYear..startYear + 9 } == true)
                            }
                            .take(2)
                    }.getOrDefault(emptyList())
                }
            }.flatMap { it.await() }
        }
    }

    private suspend fun discoveryNewMusic(market: String, language: String): List<Track> = coroutineScope {
        val apple = async {
            runCatchingPreservingCancellation {
                com.luc4n3x.levyra.data.apple.AppleMusicEditorialRepository.get(application)
                    .newMusic(market, language)?.page?.shelves.orEmpty()
                    .filter { it.kind == com.luc4n3x.levyra.data.apple.AppleCatalogShelfKind.Songs }
                    .flatMap { shelf -> shelf.items.mapNotNull(com.luc4n3x.levyra.data.apple.AppleCatalogTracks::fromItem) }
            }.getOrDefault(emptyList())
        }
        val fresh = async {
            runCatchingPreservingCancellation { chartsRepository.freshCurrentTracks(country = market, limit = DISCOVERY_SOURCE_LIMIT) }
                .getOrDefault(emptyList())
        }
        val tracks = fresh.await() + apple.await()
        if (tracks.isEmpty()) throw IllegalStateException("No new music available")
        tracks
    }

    private suspend fun discoveryFollowedReleases(): List<Track> {
        val snapshot = uiState.value
        val entries = snapshot.releaseRadar.sortedByDescending { it.isFresh }.take(DISCOVERY_RELEASE_RADAR_LIMIT)
        if (entries.isEmpty()) return emptyList()
        return coroutineScope {
            entries.map { entry ->
                async {
                    val artistKey = com.luc4n3x.levyra.feature.discovery.discoveryPrimaryArtistKey(entry.artistName)
                    runCatchingPreservingCancellation {
                        repository.search(releaseRadarSearchQuery(entry), 4, snapshot.languageCode)
                            .firstOrNull { candidate ->
                                artistKey.isNotBlank() && com.luc4n3x.levyra.feature.discovery.discoveryArtistKeys(candidate.artist).contains(artistKey)
                            }
                    }.getOrNull()
                }
            }.mapNotNull { it.await() }
        }
    }

    private suspend fun discoveryArtistTracks(artist: String): List<Track> {
        val artistKey = com.luc4n3x.levyra.feature.discovery.discoveryPrimaryArtistKey(artist)
        if (artistKey.isBlank()) return emptyList()
        return repository.search(artist, DISCOVERY_ARTIST_LIMIT, uiState.value.languageCode)
            .filter { com.luc4n3x.levyra.feature.discovery.discoveryArtistKeys(it.artist).contains(artistKey) }
    }

    internal suspend fun resolveDiscoveryAudio(track: Track): Track? = withTimeoutOrNull(DISCOVERY_RESOLVE_TIMEOUT_MS) {
        try {
            val matched = youtubePlayableTrack(track) ?: matchDiscoveryCandidate(track) ?: return@withTimeoutOrNull null
            val resolved = providerRouter.resolve(matched.copy(streamUrl = ""), false)
            if (resolved.id != matched.id || resolved.streamUrl.isBlank()) null
            else preserveEditorialArtwork(track, resolved)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Timber.d(error, "Discovery preview resolution failed")
            null
        }
    }

    private suspend fun matchDiscoveryCandidate(track: Track): Track? {
        val query = "${track.title} ${track.artist}".trim()
        if (query.length < 2) return null
        return repository.search(query, DISCOVERY_MATCH_SEARCH_LIMIT, uiState.value.languageCode)
            .asSequence()
            .filter { isPlaybackCandidateCompatible(track, it) && youtubePlayableTrack(it) != null }
            .filter { candidate ->
                track.durationMs <= 0L || candidate.durationMs <= 0L ||
                    kotlin.math.abs(candidate.durationMs - track.durationMs) <= DISCOVERY_MATCH_DURATION_TOLERANCE_MS
            }
            .map { it to playbackCandidateScore(track, it) }
            .filter { it.second >= DISCOVERY_MATCH_MIN_SCORE }
            .maxByOrNull { it.second }
            ?.first
            ?.let { LevyraPersonalOrbit.preferAlbumArtwork(youtubePlayableTrack(it) ?: it, track) }
    }
}
