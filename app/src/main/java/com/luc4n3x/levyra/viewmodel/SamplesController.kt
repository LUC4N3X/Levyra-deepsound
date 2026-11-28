package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.YoutubeShortsCache
import com.luc4n3x.levyra.data.YoutubeShortsRepository
import com.luc4n3x.levyra.data.isYoutubeShortTrack
import com.luc4n3x.levyra.data.youtubeShortsRetryDelayMs
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val EXPLORE_SHORTS_FEED_LIMIT = 60

private data class SamplesDiscoveryInput(
    val seeds: List<Track>,
    val preferredArtists: List<String>,
    val preferredChannelIds: List<String>
)

internal interface SamplesFeedHost {
    fun samplesSessionTrack(): Track?

    fun enhanceVideoSection(
        videos: List<Track>,
        languageCode: String,
        requestGeneration: Long,
        publish: (List<Track>) -> Unit
    )
}

internal class SamplesController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val preferences: LevyraPreferences,
    private val shortsRepository: YoutubeShortsRepository,
    private val shortsCache: YoutubeShortsCache,
    private val host: SamplesFeedHost
) {
    private var musicVideosLoadedLanguage = ""
    private var musicVideosLoadedProfileSignature = ""
    private var musicVideosRequestLanguage = ""
    private var musicVideosRequestProfileSignature = ""
    var musicVideosRequestGeneration = 0L
        private set
    private var musicVideosRetryLanguage = ""
    private var musicVideosRetryAfterMs = 0L
    private var musicVideosFailureCount = 0
    private var musicVideosJob: Job? = null

    fun refreshExploreSamples() {
        musicVideosLoadedLanguage = ""
        musicVideosLoadedProfileSignature = ""
        musicVideosRetryLanguage = ""
        musicVideosRetryAfterMs = 0L
        musicVideosFailureCount = 0
        ensureMusicVideosLoaded(force = true)
    }

    fun resetLanguage() {
        musicVideosJob?.cancel()
        musicVideosJob = null
        musicVideosLoadedLanguage = ""
    }

    fun cancelFeed() {
        musicVideosJob?.cancel()
    }

    private fun samplesDiscoveryProfileSignature(snapshot: LevyraUiState): String {
        val input = samplesDiscoveryInput(snapshot)
        return buildString {
            append(LevyraLanguageCatalog.normalize(snapshot.languageCode))
            append('|').append(input.preferredArtists.joinToString("|"))
            append('|').append(input.seeds.take(24).joinToString("|") { it.id })
            append('|').append(preferences.tastes().sorted().joinToString("|"))
        }
    }

    fun ensureMusicVideosLoaded(force: Boolean = false) {
        val snapshot = uiState.value
        if (!force && snapshot.isSamplesOpen && snapshot.exploreSamples.isNotEmpty() && musicVideosLoadedLanguage == snapshot.languageCode) return
        val languageCode = snapshot.languageCode
        val profileSignature = samplesDiscoveryProfileSignature(snapshot)
        val sameActiveRequest = musicVideosRequestLanguage == languageCode &&
            musicVideosRequestProfileSignature == profileSignature
        if (musicVideosJob?.isActive == true) {
            if (!force && sameActiveRequest) return
            musicVideosJob?.cancel()
        }
        if (
            !force &&
            musicVideosLoadedLanguage == languageCode &&
            musicVideosLoadedProfileSignature == profileSignature &&
            snapshot.exploreSamples.isNotEmpty()
        ) return
        if (musicVideosRetryLanguage != languageCode) {
            musicVideosRetryLanguage = languageCode
            musicVideosRetryAfterMs = 0L
            musicVideosFailureCount = 0
        }
        if (!force && System.currentTimeMillis() < musicVideosRetryAfterMs) return

        val requestGeneration = ++musicVideosRequestGeneration
        musicVideosRequestLanguage = languageCode
        musicVideosRequestProfileSignature = profileSignature
        val keepVisibleSamples = musicVideosLoadedLanguage == languageCode &&
            musicVideosLoadedProfileSignature == profileSignature
        updateSamplesState(languageCode) { current ->
            current.copy(
                exploreSamples = if (keepVisibleSamples) current.exploreSamples else emptyList(),
                isSamplesLoading = true,
                samplesLoadFailed = false
            )
        }
        musicVideosJob = scope.launch {
            try {
                if (!force && publishCachedSamples(languageCode, profileSignature, requestGeneration)) return@launch
                refreshSamplesFeed(languageCode, profileSignature, requestGeneration)
            } finally {
                if (musicVideosRequestGeneration == requestGeneration) {
                    updateSamplesState(languageCode) { current ->
                        if (!current.isSamplesLoading) current else current.copy(isSamplesLoading = false)
                    }
                    musicVideosJob = null
                }
            }
        }
    }

    private fun updateSamplesState(languageCode: String, transform: (LevyraUiState) -> LevyraUiState) {
        uiState.update { current ->
            if (current.languageCode == languageCode) transform(current) else current
        }
    }

    private suspend fun publishCachedSamples(
        languageCode: String,
        profileSignature: String,
        requestGeneration: Long
    ): Boolean {
        if (uiState.value.exploreSamples.isNotEmpty()) return false
        val cached = withContext(Dispatchers.IO) { shortsCache.load(languageCode, profileSignature) }
        if (cached.tracks.isEmpty()) return false
        if (musicVideosRequestGeneration != requestGeneration || uiState.value.languageCode != languageCode) return false
        updateSamplesState(languageCode) { current ->
            current.withPublishedSamples(cached.tracks, loading = !cached.isFresh(), failed = false)
        }
        if (!cached.isFresh()) return false
        musicVideosLoadedLanguage = languageCode
        musicVideosLoadedProfileSignature = profileSignature
        return true
    }

    private suspend fun refreshSamplesFeed(
        languageCode: String,
        profileSignature: String,
        requestGeneration: Long
    ) {
        val resolvedFeedTracks = resolveSamplesFeed(samplesDiscoveryInput(uiState.value), languageCode)
        if (musicVideosRequestGeneration != requestGeneration || uiState.value.languageCode != languageCode) return
        if (resolvedFeedTracks == null) {
            updateSamplesState(languageCode) { current ->
                current.copy(isSamplesLoading = false, samplesLoadFailed = true)
            }
            registerShortsFeedFailure(languageCode)
            return
        }

        musicVideosLoadedLanguage = languageCode
        musicVideosLoadedProfileSignature = profileSignature
        musicVideosRetryLanguage = ""
        musicVideosRetryAfterMs = 0L
        musicVideosFailureCount = 0
        updateSamplesState(languageCode) { current ->
            current.withPublishedSamples(resolvedFeedTracks, loading = false, failed = false)
        }
        host.enhanceVideoSection(
            videos = resolvedFeedTracks,
            languageCode = languageCode,
            requestGeneration = requestGeneration
        ) { enhanced ->
            updateSamplesState(languageCode) { current ->
                current.withPublishedSamples(enhanced, loading = false, failed = false)
            }
        }
        withContext(Dispatchers.IO) {
            shortsCache.save(
                languageCode = languageCode,
                tracks = resolvedFeedTracks,
                profileSignature = profileSignature
            )
        }
    }

    private fun samplesDiscoveryInput(snapshot: LevyraUiState): SamplesDiscoveryInput {
        val seeds = buildList {
            (host.samplesSessionTrack() ?: snapshot.currentTrack)?.let { track -> add(track) }
            addAll(snapshot.recentListens)
            addAll(snapshot.favorites)
            addAll(snapshot.personalOrbitTracks)
            addAll(snapshot.homeResonanceTracks)
            addAll(snapshot.exploreTracks)
            addAll(snapshot.charts)
            snapshot.homeSections.forEach { section -> addAll(section.tracks) }
            addAll(snapshot.tracks)
        }
            .filterNot(::isYoutubeShortTrack)
            .distinctBy { track -> track.id }
            .take(48)
        val preferredChannelIds = buildList {
            addAll(snapshot.followedArtists.map { artist -> artist.browseId })
            seeds.forEach { track -> addAll(track.artistBrowseIds) }
        }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(20)
        return SamplesDiscoveryInput(
            seeds = seeds,
            preferredArtists = (snapshot.followedArtists.map { it.name } + seeds.map { it.artist })
                .map(String::trim).filter(String::isNotBlank)
                .distinctBy { it.lowercase(java.util.Locale.ROOT) }.take(16),
            preferredChannelIds = preferredChannelIds
        )
    }

    private suspend fun resolveSamplesFeed(
        input: SamplesDiscoveryInput,
        languageCode: String
    ): List<Track>? {
        val feed = try {
            shortsRepository.feed(
                seeds = input.seeds,
                languageCode = languageCode,
                preferredArtists = input.preferredArtists,
                preferredChannelIds = input.preferredChannelIds,
                limit = EXPLORE_SHORTS_FEED_LIMIT
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "YouTube Shorts feed failed for %s", languageCode)
            null
        }
        if (feed == null || !feed.isConclusive || feed.tracks.isEmpty()) return null
        return feed.tracks
    }

    private fun registerShortsFeedFailure(languageCode: String) {
        if (musicVideosRetryLanguage != languageCode) {
            musicVideosRetryLanguage = languageCode
            musicVideosFailureCount = 0
        }
        musicVideosFailureCount = (musicVideosFailureCount + 1).coerceAtMost(5)
        musicVideosRetryAfterMs = System.currentTimeMillis() +
            youtubeShortsRetryDelayMs(musicVideosFailureCount)
    }
}
