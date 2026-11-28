package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.PlaybackSourceIdentity
import com.luc4n3x.levyra.data.VideoSourceChoiceStore
import com.luc4n3x.levyra.data.VideoUploadEvidence
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.YoutubeMusicWatchTrack
import com.luc4n3x.levyra.data.preserveNativeSampleClip
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.VideoQualityLadder
import com.luc4n3x.levyra.domain.VideoQualityRung
import com.luc4n3x.levyra.domain.VideoQualityTarget
import com.luc4n3x.levyra.domain.VideoRebufferPolicy
import com.luc4n3x.levyra.domain.YoutubeMusicVideoType
import com.luc4n3x.levyra.domain.audioPartnerForAdaptiveRung
import com.luc4n3x.levyra.domain.hasVideoPlaybackPayload
import com.luc4n3x.levyra.domain.withSelectedVideoQuality
import com.luc4n3x.levyra.feature.providers.LevyraProviderRouter
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.player.LevyraPlayer
import com.luc4n3x.levyra.player.isTransientNetworkFailure
import com.luc4n3x.levyra.player.isVideoContentFailure
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import java.util.Collections
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class VideoModeController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val player: LevyraPlayer,
    private val preferences: LevyraPreferences,
    private val resolver: PlaybackResolver,
    private val providerRouter: LevyraProviderRouter,
    private val repository: YoutubeMusicRepository,
    private val queueEngine: PersistentQueueEngine,
    val videoSourceChoices: VideoSourceChoiceStore,
    private val isLocalPlaybackTrack: (Track) -> Boolean,
    private val audioModeOrigin: () -> Track?
) {
    val videoFrameDropHandler: () -> Unit = { onVideoSustainedFrameDrops() }
    private var videoSourceJob: Job? = null
    val videoSourcesState = MutableStateFlow(VideoSourceUiState())
    val videoSources: StateFlow<VideoSourceUiState> = videoSourcesState.asStateFlow()
    val verifiedVideoIdentityCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, VideoSourceDiscovery>(VERIFIED_VIDEO_IDENTITY_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, VideoSourceDiscovery>?): Boolean {
                return size > VERIFIED_VIDEO_IDENTITY_CACHE_SIZE
            }
        }
    )
    private val videoRebufferPolicy = VideoRebufferPolicy()
    private var videoQualityTrackId: String? = null

    fun selectVideoSubtitle(trackId: String?) {
        val snapshot = uiState.value
        if (!snapshot.isVideoMode) return
        val subtitle = snapshot.currentTrack?.videoSubtitleTracks
            ?.firstOrNull { it.id == trackId }
        player.selectVideoSubtitle(subtitle?.id)
        uiState.update { it.copy(selectedVideoSubtitleId = subtitle?.id) }
    }

    fun setDefaultVideoQuality(target: VideoQualityTarget) {
        preferences.setVideoQualityTarget(target)
        resolver.invalidateVideoQualitySelection()
    }

    fun selectVideoQuality(targetLabel: String?) {
        val snapshot = uiState.value
        val track = snapshot.currentTrack ?: return
        if (!snapshot.isVideoMode) return
        if (track.isLiveRadio() || isLocalPlaybackTrack(track)) return
        if (snapshot.isResolving || snapshot.videoQuality.switching) return
        val manifest = track.playbackManifest ?: return
        val rung = resolveVideoRung(snapshot.videoQuality.ladder, targetLabel, manifest) ?: return
        if (isVideoRungActive(rung, track, manifest)) return

        val positionMs = player.positionMs.coerceAtLeast(0L)
        val shouldPlay = player.isPlaying || snapshot.isPlaying
        val audioPartner = if (rung.progressive) "" else audioPartnerForAdaptiveRung(manifest, track.streamUrl)
        val switchTrack = track.withSelectedVideoQuality(rung, audioPartner)
        applyVideoQualitySwitch(switchTrack, positionMs, shouldPlay, rung.label)
    }

    private fun resolveVideoRung(
        ladder: List<VideoQualityRung>,
        targetLabel: String?,
        manifest: ResolvedPlaybackManifest
    ): VideoQualityRung? {
        if (ladder.isEmpty()) return null
        val rung = if (targetLabel == null) {
            VideoQualityLadder.selectRung(ladder, VideoQualityTarget.AUTO, resolver.videoAutoTargetHeight())
        } else {
            ladder.firstOrNull { it.label == targetLabel }
        } ?: return null
        if (rung.expiresAtMs in 1L..System.currentTimeMillis()) return null
        if (!rung.progressive && manifest.isMuxed &&
            manifest.streams.none { it.kind == PlaybackStreamKind.AUDIO && it.url.isNotBlank() }
        ) {
            return null
        }
        return rung
    }

    private fun isVideoRungActive(
        rung: VideoQualityRung,
        track: Track,
        manifest: ResolvedPlaybackManifest
    ): Boolean {
        val activeRungUrl = when {
            track.videoStreamUrl.isNotBlank() -> track.videoStreamUrl
            manifest.isMuxed -> track.streamUrl
            else -> ""
        }
        return activeRungUrl.isNotBlank() && rung.url == activeRungUrl
    }

    private fun applyVideoQualitySwitch(
        switchTrack: Track,
        positionMs: Long,
        shouldPlay: Boolean,
        activeLabel: String
    ) {
        uiState.update {
            it.copy(videoQuality = it.videoQuality.copy(switching = true))
        }
        try {
            repository.replace(switchTrack)
            player.replaceSource(
                track = switchTrack,
                positionMs = positionMs,
                videoMode = true,
                playWhenReady = shouldPlay
            )
            videoRebufferPolicy.reset()
            val currentIndex = queueEngine.state.value.currentIndex
            if (currentIndex >= 0) queueEngine.updateActiveTrackAt(currentIndex, switchTrack)
            queueEngine.updatePosition(positionMs)
            uiState.update {
                it.copy(
                    currentTrack = switchTrack,
                    isPlaying = shouldPlay,
                    positionMs = positionMs,
                    bufferedPositionMs = positionMs,
                    videoQuality = it.videoQuality.copy(
                        activeLabel = activeLabel,
                        switching = false
                    )
                )
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            uiState.update {
                it.copy(videoQuality = it.videoQuality.copy(switching = false))
            }
        }
    }

    fun onVideoMidPlayStall() {
        val snapshot = uiState.value
        val track = automaticVideoDowngradeTrack(snapshot) ?: return
        val nowMs = System.currentTimeMillis()
        videoRebufferPolicy.onMidPlayStall(nowMs)
        if (!videoRebufferPolicy.shouldDowngrade(nowMs)) return
        videoRebufferPolicy.reset()
        stepDownVideoQuality(snapshot, track)
    }

    private fun onVideoSustainedFrameDrops() {
        if (!player.isPlaying) return
        val snapshot = uiState.value
        val track = automaticVideoDowngradeTrack(snapshot) ?: return
        videoRebufferPolicy.reset()
        stepDownVideoQuality(snapshot, track)
    }

    private fun automaticVideoDowngradeTrack(snapshot: LevyraUiState): Track? {
        if (!snapshot.isVideoMode) return null
        val track = snapshot.currentTrack ?: return null
        if (track.isLiveRadio() || isLocalPlaybackTrack(track)) return null
        if (snapshot.isResolving || snapshot.videoQuality.switching) return null
        if (snapshot.videoQuality.ladder.isEmpty()) return null
        return track
    }

    private fun stepDownVideoQuality(snapshot: LevyraUiState, track: Track) {
        val ladder = snapshot.videoQuality.ladder
        val activeLabel = snapshot.videoQuality.activeLabel
            ?: VideoQualityLadder.activeLabelFor(track, ladder)
        VideoQualityLadder.rungBelow(ladder, activeLabel)?.let { below ->
            selectVideoQuality(below.label)
        }
    }

    fun refreshVideoQualityState(track: Track) {
        val videoMode = uiState.value.isVideoMode
        val trackChanged = videoQualityTrackId != track.id
        if (trackChanged) {
            videoQualityTrackId = track.id
            videoRebufferPolicy.reset()
        }
        val ladder = if (videoMode && !track.isLiveRadio() && !isLocalPlaybackTrack(track)) {
            track.playbackManifest?.takeIf { track.hasVideoPlaybackPayload() }?.let { manifest ->
                VideoQualityLadder.build(manifest.streams) { descriptor ->
                    resolver.supportsVideoDescriptor(descriptor)
                }
            }.orEmpty()
        } else {
            emptyList()
        }
        val activeLabel = if (ladder.isNotEmpty()) VideoQualityLadder.activeLabelFor(track, ladder) else null
        uiState.update {
            it.copy(
                videoQuality = VideoQualityUiState(
                    ladder = ladder,
                    activeLabel = activeLabel,
                    switching = false
                )
            )
        }
    }

    suspend fun recoveryVideoSource(failedTrack: Track, errorMessage: String): Track? {
        if (!isVideoContentFailure(errorMessage)) return preferredVideoPlaybackTrack(failedTrack)
        val failedVideoId = youtubeVideoId(failedTrack.videoUrl)
        val sources = videoPlaybackSources(failedTrack, forceDiscovery = true)
        return sources.firstOrNull { youtubeVideoId(it.videoUrl) != failedVideoId } ?: sources.firstOrNull()
    }

    suspend fun preferredVideoPlaybackTrack(track: Track): Track? =
        videoPlaybackSources(track).firstOrNull()

    fun videoSourceKey(track: Track): String =
        track.audioVideoId.trim()
            .takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches)
            .orEmpty()
            .ifBlank { PlaybackSourceIdentity.sourceVideoId(track) }
            .ifBlank { playbackIdentity(track) }

    fun videoSourceSeed(track: Track): Track =
        audioModeOrigin()?.takeIf { it.id == track.id } ?: track

    private suspend fun videoSourceDiscovery(track: Track, key: String): VideoSourceDiscovery =
        verifiedVideoIdentityCache[key]
            ?: discoverVideoSources(track, PlaybackSourceIdentity.sourceVideoId(track)).also { found ->
                if (found.ranked.isNotEmpty()) verifiedVideoIdentityCache[key] = found
            }

    private suspend fun videoPlaybackSources(track: Track, forceDiscovery: Boolean = false): List<Track> {
        val sourceId = PlaybackSourceIdentity.sourceVideoId(track)
        val key = videoSourceKey(track)
        val choice = videoSourceChoices.get(key)
        val ranked = if (choice != null && !forceDiscovery && !verifiedVideoIdentityCache.containsKey(key)) {
            emptyList()
        } else {
            videoSourceDiscovery(track, key).ranked
        }
        val chosen = choice?.let { selected ->
            ranked.firstOrNull { videoCandidateId(it) == selected.videoId }
                ?: track.copy(
                    id = selected.videoId,
                    videoUrl = "https://www.youtube.com/watch?v=${selected.videoId}",
                    videoType = selected.videoType
                )
        }
        val ordered = listOfNotNull(chosen) + ranked.filter { videoCandidateId(it) != choice?.videoId }
        val pairedVideo = youtubePlayableTrack(track, preferVideo = true)
        return (ordered.mapNotNull { candidate -> track.withVerifiedVideoCandidate(candidate, sourceId) } + listOfNotNull(pairedVideo))
            .distinctBy { source -> youtubeVideoId(source.videoUrl) }
    }

    private suspend fun discoverVideoSources(track: Track, sourceId: String): VideoSourceDiscovery {
        val languageCode = uiState.value.languageCode
        val queryTitle = videoSourceQueryTitle(track.title)
        val catalogQuery = "$queryTitle ${track.artist}".trim()
        val webQuery = "${track.artist} $queryTitle official video".trim()
        return coroutineScope {
            val watch = async(Dispatchers.IO) {
                withTimeoutOrNull(VIDEO_IDENTITY_LOOKUP_TIMEOUT_MS) {
                    repository.getWatchPlaylist(track, languageCode, limit = 3)
                }
            }
            val search = async(Dispatchers.IO) {
                withTimeoutOrNull(VIDEO_IDENTITY_LOOKUP_TIMEOUT_MS) {
                    repository.searchMusicVideos(catalogQuery, VIDEO_SOURCE_SEARCH_LIMIT, languageCode)
                }.orEmpty()
            }
            val web = async(Dispatchers.IO) {
                withTimeoutOrNull(VIDEO_IDENTITY_LOOKUP_TIMEOUT_MS) {
                    repository.searchYoutubeVideoEvidence(webQuery, VIDEO_SOURCE_SEARCH_LIMIT, languageCode)
                }.orEmpty()
            }
            val playlist = watch.await()
            val primary = playlist?.tracks?.firstOrNull { it.videoId == sourceId }
                ?: playlist?.tracks?.firstOrNull()
            val watchCandidates = sequenceOf(primary, primary?.counterpart)
                .filterNotNull()
                .map { item -> item.asVideoCandidate(track) }
                .toList()
            val catalogCandidates = watchCandidates + search.await()
            val evidence = web.await().associateBy { it.videoId }
            val catalogIds = catalogCandidates.mapTo(HashSet(), ::videoCandidateId)
            val webCandidates = evidence.values
                .filter { it.videoId !in catalogIds }
                .map { upload -> upload.asVideoCandidate(track) }
            val authoritativeIds = watchCandidates.map(::videoCandidateId).filter { it.isNotBlank() }.toSet()
            VideoSourceDiscovery(
                ranked = rankVideoPlaybackCandidates(
                    track,
                    catalogCandidates + webCandidates,
                    authoritativeIds,
                    evidence
                ).take(MAX_VIDEO_SOURCE_OPTIONS),
                evidence = evidence
            )
        }
    }

    private fun VideoUploadEvidence.asVideoCandidate(seed: Track): Track = seed.copy(
        id = videoId,
        title = title,
        artist = channelName,
        album = "",
        durationMs = durationMs,
        videoUrl = "https://www.youtube.com/watch?v=$videoId",
        thumbnailUrl = thumbnailUrl.ifBlank { seed.thumbnailUrl },
        largeThumbnailUrl = thumbnailUrl.ifBlank { seed.largeThumbnailUrl },
        source = "YouTube",
        videoType = WEB_VIDEO_TYPE,
        artistBrowseIds = emptyList(),
        isrc = "",
        audioVideoId = "",
        counterpartVideoId = "",
        youtubeViewCount = viewCount
    )

    suspend fun resolveVideoModeSource(track: Track): Track {
        val attempted = HashSet<String>()
        var failure: Throwable? = null
        for (forceDiscovery in booleanArrayOf(false, true)) {
            for (source in videoPlaybackSources(track, forceDiscovery)) {
                if (attempted.size >= MAX_VIDEO_SOURCE_ATTEMPTS) break
                if (!attempted.add(youtubeVideoId(source.videoUrl))) continue
                val baseTrack = source.withPreservedAudioIdentity(track).forModeResolution()
                try {
                    return preserveNativeSampleClip(track, withContext(Dispatchers.IO) {
                        resolver.cached(baseTrack, true) ?: providerRouter.resolve(baseTrack, true)
                    })
                } catch (error: Throwable) {
                    if (error is CancellationException || isTransientNetworkFailure(error)) throw error
                    resolver.invalidate(baseTrack, true)
                    failure = error
                }
            }
            if (failure == null || attempted.size >= MAX_VIDEO_SOURCE_ATTEMPTS) break
        }
        throw failure ?: IllegalStateException("Nessuna sorgente YouTube riproducibile per ${track.title}")
    }

    fun loadVideoSources() {
        val snapshot = uiState.value
        val track = snapshot.currentTrack ?: return
        if (!snapshot.isVideoMode || track.isLiveRadio() || isLocalPlaybackTrack(track)) return
        val seed = videoSourceSeed(track)
        val key = videoSourceKey(seed)
        val activeVideoId = youtubeVideoId(track.videoUrl)
        videoSourceJob?.cancel()
        videoSourcesState.update {
            VideoSourceUiState(
                trackKey = key,
                options = it.options.takeIf { _ -> it.trackKey == key }.orEmpty(),
                activeVideoId = activeVideoId,
                loading = true
            )
        }
        videoSourceJob = scope.launch {
            val options = try {
                videoSourceOptions(seed, videoSourceDiscovery(seed, key))
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                emptyList()
            }
            val activeOption = options.firstOrNull { it.videoId == activeVideoId }
                ?: VideoSourceOption(
                    videoId = activeVideoId,
                    title = track.title,
                    channel = track.artist,
                    durationMs = track.durationMs,
                    thumbnailUrl = "https://i.ytimg.com/vi/$activeVideoId/mqdefault.jpg",
                    official = YoutubeMusicVideoType.isOfficialVideo(track.videoType)
                ).takeIf { YOUTUBE_PLAYABLE_VIDEO_ID.matches(activeVideoId) }
            val merged = (listOfNotNull(activeOption) + options).distinctBy { it.videoId }
            videoSourcesState.update {
                if (it.trackKey != key) it else it.copy(options = merged, loading = false)
            }
        }
    }

    private fun YoutubeMusicWatchTrack.asVideoCandidate(seed: Track): Track {
        val resolvedArtist = artists.joinToString(", ") { artist -> artist.name }.ifBlank { seed.artist }
        return seed.copy(
            id = videoId,
            title = title.ifBlank { seed.title },
            artist = resolvedArtist,
            album = albumTitle.ifBlank { seed.album },
            durationMs = durationMs.takeIf { it > 0L } ?: seed.durationMs,
            videoUrl = "https://www.youtube.com/watch?v=$videoId",
            thumbnailUrl = thumbnailUrl.ifBlank { seed.thumbnailUrl },
            largeThumbnailUrl = thumbnailUrl.ifBlank { seed.largeThumbnailUrl },
            videoType = videoType,
            artistBrowseIds = artists.map { artist -> artist.browseId }.filter { it.isNotBlank() }
        )
    }

    private companion object {
        private const val VIDEO_IDENTITY_LOOKUP_TIMEOUT_MS = 9_000L
        private const val VERIFIED_VIDEO_IDENTITY_CACHE_SIZE = 64
        private const val VIDEO_SOURCE_SEARCH_LIMIT = 10
        private const val MAX_VIDEO_SOURCE_OPTIONS = 8
        private const val MAX_VIDEO_SOURCE_ATTEMPTS = 3
        private const val WEB_VIDEO_TYPE = "MUSIC_VIDEO_TYPE_UGC"
    }
}

internal fun Track.withVerifiedVideoCandidate(candidate: Track, sourceId: String): Track? {
    val videoId = youtubeVideoId(candidate.videoUrl)
        .ifBlank { candidate.id.trim() }
        .takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches)
        ?: return null
    val targetVideoTyped = videoType.contains("OMV", ignoreCase = true) ||
        videoType.contains("UGC", ignoreCase = true)
    if (!targetVideoTyped && videoId == sourceId.trim()) return null
    return copy(
        videoUrl = "https://www.youtube.com/watch?v=$videoId",
        counterpartVideoId = videoId,
        videoType = candidate.videoType,
        audioVideoId = audioVideoId.ifBlank { sourceId }
    )
}
