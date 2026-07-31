from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: str, old: str, new: str, label: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one anchor in {path}, found {count}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


def append_before_once(path: str, marker: str, addition: str, label: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    if addition.strip() in text:
        return
    count = text.count(marker)
    if count != 1:
        raise RuntimeError(f"{label}: expected one marker in {path}, found {count}")
    target.write_text(text.replace(marker, addition + marker, 1), encoding="utf-8")


ORBIT = "app/src/main/java/com/luc4n3x/levyra/domain/LevyraPersonalOrbit.kt"
APP = "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"
VM = "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"
PLAYER_TEST = "app/src/test/java/com/luc4n3x/levyra/player/LevyraPlayerTest.kt"
ORBIT_TEST = "app/src/test/java/com/luc4n3x/levyra/domain/LevyraPersonalOrbitIdentityTest.kt"

replace_once(
    ORBIT,
    '''    fun identityKey(track: Track): String {
        val title = normalizedMusicText(track.title)
        val artist = normalizedMusicText(track.artist)
        return if (title.isNotBlank() && artist.isNotBlank()) "$title|$artist" else stableKey(track)
    }
''',
    '''    fun identityKey(track: Track): String {
        val title = normalizedMusicTitle(track.title)
        val artist = normalizedArtistSignature(track.artist)
        return if (title.isNotBlank() && artist.isNotBlank()) "$artist|$title" else stableKey(track)
    }

    fun sameRecording(first: Track, second: Track): Boolean {
        val firstIsrc = normalizedIsrc(first.isrc)
        val secondIsrc = normalizedIsrc(second.isrc)
        if (firstIsrc.isNotBlank() && secondIsrc.isNotBlank()) {
            return firstIsrc == secondIsrc
        }
        return identityKey(first) == identityKey(second)
    }

    fun distinctRecordings(tracks: List<Track>): List<Track> {
        val result = ArrayList<Track>(tracks.size)
        tracks.forEach { candidate ->
            val existingIndex = result.indexOfFirst { existing -> sameRecording(existing, candidate) }
            if (existingIndex < 0) {
                result += candidate
            } else {
                result[existingIndex] = mergeRecordingMetadata(result[existingIndex], candidate)
            }
        }
        return result
    }
''',
    "canonical recording identity",
)

replace_once(
    ORBIT,
    '''    private fun normalizedMusicText(value: String): String {
        return value.lowercase()
            .replace(Regex("""\\([^)]*\\)|\\[[^]]*]"""), " ")
            .replace(Regex("""feat\\.?|featuring|ft\\.?"""), " ")
            .replace(Regex("""official audio|official video|lyrics?|visuali[sz]er|music video"""), " ")
            .replace(Regex("""[^\\p{L}\\p{M}\\p{N}\\s]"""), " ")
            .replace(Regex("""\\s+"""), " ")
            .trim()
    }
''',
    '''    private fun normalizedMusicTitle(value: String): String {
        return value.lowercase()
            .replace(
                Regex(
                    """(?i)[(\\[]\\s*(?:(?:official\\s+)?(?:music\\s+)?(?:video|audio)|lyrics?|visuali[sz]er|feat\\.?|ft\\.?|featuring)[^)\\]]*[)\\]]"""
                ),
                " "
            )
            .replace(Regex("""(?i)\\b(?:feat\\.?|ft\\.?|featuring)\\b.*$"""), " ")
            .replace(Regex("""(?i)\\b(?:official\\s+)?(?:music\\s+)?(?:video|audio)|lyrics?|visuali[sz]er\\b"""), " ")
            .replace(Regex("""[^\\p{L}\\p{M}\\p{N}\\s]"""), " ")
            .replace(Regex("""\\s+"""), " ")
            .trim()
    }

    private fun normalizedArtistSignature(value: String): String {
        val normalized = value.lowercase()
            .replace(Regex("""[^\\p{L}\\p{M}\\p{N}\\s]"""), " ")
            .replace(
                Regex("""(?i)\\b(?:feat|featuring|ft|and|with|e|ed|y|et|und|x)\\b"""),
                " "
            )
            .replace(Regex("""\\s+"""), " ")
            .trim()
        return normalized.split(' ')
            .filter(String::isNotBlank)
            .sorted()
            .joinToString(" ")
    }

    private fun normalizedIsrc(value: String): String =
        value.uppercase(java.util.Locale.ROOT).filter(Char::isLetterOrDigit)

    private fun mergeRecordingMetadata(first: Track, second: Track): Track {
        val firstScore = recordingMetadataScore(first)
        val secondScore = recordingMetadataScore(second)
        val preferred = if (secondScore > firstScore) second else first
        val fallback = if (preferred === first) second else first
        return preferred.copy(
            videoUrl = preferred.videoUrl.ifBlank { fallback.videoUrl },
            thumbnailUrl = preferred.thumbnailUrl.ifBlank { fallback.thumbnailUrl },
            largeThumbnailUrl = preferred.largeThumbnailUrl.ifBlank { fallback.largeThumbnailUrl },
            isrc = preferred.isrc.ifBlank { fallback.isrc },
            upc = preferred.upc.ifBlank { fallback.upc },
            releaseDate = preferred.releaseDate.ifBlank { fallback.releaseDate },
            albumBrowseId = preferred.albumBrowseId.ifBlank { fallback.albumBrowseId },
            artistBrowseIds = preferred.artistBrowseIds.ifEmpty { fallback.artistBrowseIds },
            counterpartVideoId = preferred.counterpartVideoId.ifBlank { fallback.counterpartVideoId },
            videoType = preferred.videoType.ifBlank { fallback.videoType },
            metadataProvider = preferred.metadataProvider.ifBlank { fallback.metadataProvider },
            metadataConfidence = maxOf(preferred.metadataConfidence, fallback.metadataConfidence),
            canonicalAlbumUrl = preferred.canonicalAlbumUrl.ifBlank { fallback.canonicalAlbumUrl }
        )
    }

    private fun recordingMetadataScore(track: Track): Int {
        var score = track.metadataConfidence.coerceIn(0, 100)
        if (track.isrc.isNotBlank()) score += 120
        if (track.counterpartVideoId.isNotBlank()) score += 100
        if (track.videoUrl.isNotBlank()) score += 50
        if (track.albumBrowseId.isNotBlank()) score += 35
        if (hasSquareAlbumArtwork(track)) score += 30
        if (track.largeThumbnailUrl.isNotBlank()) score += 12
        return score
    }
''',
    "recording text normalization",
)

replace_once(
    APP,
    '''    val personalTracks = remember(state.personalOrbitTracks) {
        state.personalOrbitTracks.take(LevyraPersonalOrbit.DISPLAY_LIMIT)
    }
''',
    '''    val personalTracks = remember(state.personalOrbitTracks) {
        LevyraPersonalOrbit.distinctRecordings(state.personalOrbitTracks)
            .take(LevyraPersonalOrbit.DISPLAY_LIMIT)
    }
''',
    "home orbit deduplication",
)

replace_once(
    APP,
    '''    val otherSections = homeDerivedState.otherSections
    val spotlightCandidates = homeDerivedState.spotlightCandidates
''',
    '''    val rawOtherSections = homeDerivedState.otherSections
    val homeVideoTracks = remember(state.exploreVideos, rawOtherSections, state.charts) {
        val sectionVideos = rawOtherSections
            .filter { section -> isMusicVideoSectionTitle(section.title) }
            .flatMap { section -> section.tracks }
        val chartVideos = state.charts.filter { track ->
            track.counterpartVideoId.isNotBlank() || track.videoUrl.isNotBlank()
        }
        LevyraPersonalOrbit.distinctRecordings(state.exploreVideos + sectionVideos + chartVideos)
            .filter { track ->
                track.counterpartVideoId.isNotBlank() ||
                    track.videoType.contains("video", ignoreCase = true) ||
                    track.videoUrl.isNotBlank()
            }
            .take(12)
    }
    val otherSections = remember(rawOtherSections, homeVideoTracks) {
        if (homeVideoTracks.isEmpty()) rawOtherSections
        else rawOtherSections.filterNot { section -> isMusicVideoSectionTitle(section.title) }
    }
    val spotlightCandidates = homeDerivedState.spotlightCandidates
''',
    "stable home video candidates",
)

replace_once(
    APP,
    '''    val visiblePersonalTracks = remember(personalTracks, spotlightCandidate?.track?.id) {
        personalTracks.filterNot { it.id == spotlightCandidate?.track?.id }
    }
''',
    '''    val visiblePersonalTracks = remember(personalTracks, spotlightCandidate?.track) {
        val spotlightTrack = spotlightCandidate?.track
        LevyraPersonalOrbit.distinctRecordings(personalTracks).filterNot { track ->
            spotlightTrack != null && LevyraPersonalOrbit.sameRecording(track, spotlightTrack)
        }
    }
''',
    "spotlight orbit identity filtering",
)

replace_once(
    APP,
    '''        if (visibleEditorialCollections.isNotEmpty()) {
''',
    '''        if (showDeferredHomeSections && homeVideoTracks.isNotEmpty()) {
            item(key = "home-music-videos", contentType = "home-horizontal-row") {
                HomeMusicVideoShelf(
                    title = strings.exploreNewVideos,
                    tracks = homeVideoTracks,
                    currentId = state.currentTrack?.id,
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    onPlay = { track -> viewModel.playFrom(homeVideoTracks, track) },
                    onPlayAll = { viewModel.playAll(homeVideoTracks) }
                )
            }
        }
        if (visibleEditorialCollections.isNotEmpty()) {
''',
    "home video shelf placement",
)

append_before_once(
    APP,
    '''@Composable
private fun PersonalListeningShelf(
''',
    '''@Composable
private fun HomeMusicVideoShelf(
    title: String,
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit
) {
    val videos = remember(tracks) { LevyraPersonalOrbit.distinctRecordings(tracks).take(12) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionInset { SectionHeaderAction(title, onPlayAll) }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding)
        ) {
            itemsIndexed(
                items = videos,
                key = { index, track -> "home-video-$index-${LevyraPersonalOrbit.identityKey(track)}" },
                contentType = { _, _ -> "home-video-card" }
            ) { _, track ->
                val active = currentId != null && track.id == currentId
                Column(
                    modifier = Modifier.width(154.dp).pressable(onClick = { onPlay(track) }),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .border(
                                if (active) 1.5.dp else Dp.Hairline,
                                if (active) LevyraCyan.copy(alpha = 0.82f) else LevyraAdaptiveSoftHairline,
                                RoundedCornerShape(22.dp)
                            )
                    ) {
                        CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = true, zoom = 1.03f)
                        Box(
                            modifier = Modifier.matchParentSize().background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.30f))
                                )
                            )
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.62f),
                            border = BorderStroke(Dp.Hairline, Color.White.copy(alpha = 0.16f)),
                            shape = CircleShape,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when {
                                    active && isResolving -> CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = LevyraCyan
                                    )
                                    active && isPlaying -> ActiveTrackEqualizer(
                                        color = LevyraCyan,
                                        isPlaying = true,
                                        width = 16.dp,
                                        height = 12.dp
                                    )
                                    else -> Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = track.title,
                        color = LevyraText,
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = LevyraMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun isMusicVideoSectionTitle(title: String): Boolean {
    val normalized = title.lowercase(java.util.Locale.ROOT)
    return normalized.contains("video musical") ||
        normalized.contains("music video") ||
        normalized.contains("official video") ||
        normalized.contains("videoclip") ||
        normalized.contains("video per te") ||
        normalized.contains("new videos") ||
        normalized.contains("top videos")
}

''',
    "home video shelf composable",
)

replace_once(
    APP,
    '''    val shelfTracks = remember(tracks) { tracks.distinctBy { LevyraPersonalOrbit.identityKey(it) }.take(LevyraPersonalOrbit.DISPLAY_LIMIT) }
''',
    '''    val shelfTracks = remember(tracks) {
        LevyraPersonalOrbit.distinctRecordings(tracks).take(LevyraPersonalOrbit.DISPLAY_LIMIT)
    }
''',
    "personal shelf canonical deduplication",
)

replace_once(
    APP,
    '''                        if (track != null && track.videoUrl.isNotBlank()) {
''',
    '''                        if (track != null && (track.videoUrl.isNotBlank() || track.counterpartVideoId.isNotBlank())) {
''',
    "player mode switch visibility",
)

replace_once(
    VM,
    '''    private fun loadHomeFeed(deferUntilHomeIdle: Boolean = false) {
        val requestGeneration = homeFeedRequestGeneration.incrementAndGet()
''',
    '''    private fun loadHomeFeed(deferUntilHomeIdle: Boolean = false) {
        ensureMusicVideosLoaded()
        val requestGeneration = homeFeedRequestGeneration.incrementAndGet()
''',
    "home video loading trigger",
)

replace_once(
    VM,
    '''    private val exploreCache = ConcurrentHashMap<String, List<Track>>()
    private var exploreVideosLoaded = false
    private var exploreJob: Job? = null

    fun ensureExplore(strings: LevyraStrings) {
        if (_state.value.exploreZoneId == null) {
            selectExploreZone(ExploreCatalog.getZones(strings).first())
        }
        if (!exploreVideosLoaded) {
            exploreVideosLoaded = true
            viewModelScope.launch {
                val videos = runCatching { repository.newMusicVideos(_state.value.languageCode, 12) }.getOrDefault(emptyList())
                if (videos.isEmpty()) exploreVideosLoaded = false
                _state.update { it.copy(exploreVideos = videos) }
                refreshOfficialMetadataBatch(videos, 6)
            }
        }
    }
''',
    '''    private val exploreCache = ConcurrentHashMap<String, List<Track>>()
    private var musicVideosLoadedLanguage = ""
    private var musicVideosJob: Job? = null
    private var exploreJob: Job? = null

    private fun ensureMusicVideosLoaded() {
        val snapshot = _state.value
        val languageCode = snapshot.languageCode
        if (musicVideosJob?.isActive == true) return
        if (musicVideosLoadedLanguage == languageCode && snapshot.exploreVideos.isNotEmpty()) return
        musicVideosJob = viewModelScope.launch {
            val remoteVideos = withContext(Dispatchers.IO) {
                runCatching { repository.newMusicVideos(languageCode, 12) }.getOrDefault(emptyList())
            }
            if (_state.value.languageCode != languageCode) return@launch
            val current = _state.value
            val chartFallback = current.charts.filter { track ->
                track.counterpartVideoId.isNotBlank() || track.videoType.contains("video", ignoreCase = true)
            }
            val videos = LevyraPersonalOrbit.distinctRecordings(remoteVideos + chartFallback)
                .take(12)
                .ifEmpty { current.exploreVideos }
            if (videos.isNotEmpty()) {
                musicVideosLoadedLanguage = languageCode
                _state.update { state ->
                    if (state.languageCode == languageCode) state.copy(exploreVideos = videos) else state
                }
                refreshOfficialMetadataBatch(videos, 6)
            } else {
                musicVideosLoadedLanguage = ""
            }
        }
    }

    fun ensureExplore(strings: LevyraStrings) {
        if (_state.value.exploreZoneId == null) {
            selectExploreZone(ExploreCatalog.getZones(strings).first())
        }
        ensureMusicVideosLoaded()
    }
''',
    "shared music video loader",
)

replace_once(
    VM,
    '''        exploreCache.clear()
        exploreVideosLoaded = false
        _state.update {
''',
    '''        exploreCache.clear()
        musicVideosJob?.cancel()
        musicVideosJob = null
        musicVideosLoadedLanguage = ""
        _state.update {
''',
    "language video loader reset",
)

replace_once(
    VM,
    '''                exploreZoneId = null,
                exploreTracks = emptyList(),
                exploreVideos = emptyList()
''',
    '''                exploreZoneId = null,
                exploreTracks = emptyList()
''',
    "preserve video shelf during language refresh",
)

replace_once(
    VM,
    '''    fun toggleVideoMode() {
        val snapshot = _state.value
        val track = snapshot.currentTrack ?: return
        if (track.videoUrl.isBlank() || snapshot.isResolving) return
''',
    '''    fun toggleVideoMode() {
        val snapshot = _state.value
        val track = snapshot.currentTrack ?: return
        if (youtubePlayableTrack(track, preferVideo = true) == null || snapshot.isResolving) return
''',
    "official counterpart video guard",
)

replace_once(
    VM,
    '''                val baseTrack = (youtubePlayableTrack(track) ?: track).copy(streamUrl = "", videoStreamUrl = "")
''',
    '''                val baseTrack = (youtubePlayableTrack(track, preferVideo = targetMode) ?: track)
                    .copy(streamUrl = "", videoStreamUrl = "")
''',
    "mode-aware video resolution",
)

replace_once(
    VM,
    '''                val baseTrack = (youtubePlayableTrack(failedTrack) ?: failedTrack).copy(streamUrl = "", videoStreamUrl = "")
''',
    '''                val baseTrack = (youtubePlayableTrack(failedTrack, preferVideo = videoMode) ?: failedTrack)
                    .copy(streamUrl = "", videoStreamUrl = "")
''',
    "mode-aware playback recovery",
)

replace_once(
    VM,
    '''        if (track.id.isBlank() || track.videoUrl.isBlank() || track.source.equals("Offline", true)) return
        alternateModePrefetchJob = viewModelScope.launch(Dispatchers.IO) {
            val targetVideoMode = !activeVideoMode
            val cleanTrack = (youtubePlayableTrack(track) ?: track).copy(streamUrl = "", videoStreamUrl = "")
            val resolved = resolver.prefetch(cleanTrack, targetVideoMode) ?: return@launch
''',
    '''        if (track.source.equals("Offline", true)) return
        alternateModePrefetchJob = viewModelScope.launch(Dispatchers.IO) {
            val targetVideoMode = !activeVideoMode
            val cleanTrack = youtubePlayableTrack(track, preferVideo = targetVideoMode)
                ?.copy(streamUrl = "", videoStreamUrl = "")
                ?: return@launch
            val resolved = resolver.prefetch(cleanTrack, targetVideoMode) ?: return@launch
''',
    "counterpart-aware alternate prefetch",
)

replace_once(
    VM,
    '''        val playableTrack = youtubePlayableTrack(track) ?: track
        val instant = localDownloadedTrack(track) ?: resolver.cached(playableTrack, _state.value.isVideoMode)
''',
    '''        val requestedVideoMode = _state.value.isVideoMode
        val playableTrack = youtubePlayableTrack(track, preferVideo = requestedVideoMode) ?: track
        val instant = localDownloadedTrack(track) ?: resolver.cached(playableTrack, requestedVideoMode)
''',
    "mode-aware initial playback cache",
)

replace_once(
    VM,
    '''    private suspend fun resolveForPlayback(track: Track): Track {
        youtubePlayableTrack(track)?.let { return preserveEditorialArtwork(track, resolvePlayableTrack(it)) }
''',
    '''    private suspend fun resolveForPlayback(track: Track): Track {
        youtubePlayableTrack(track, preferVideo = _state.value.isVideoMode)?.let {
            return preserveEditorialArtwork(track, resolvePlayableTrack(it))
        }
''',
    "mode-aware playback resolution",
)

replace_once(
    VM,
    '''internal fun playbackIdentity(track: Track): String = youtubePlayableTrack(track)?.id?.takeIf { it.isNotBlank() }
    ?: track.id.ifBlank { track.videoUrl.ifBlank { "${track.artist}|${track.title}" } }.trim().lowercase()

internal fun youtubePlayableTrack(track: Track): Track? {
    val videoId = youtubeVideoId(track.videoUrl)
        .ifBlank { youtubeVideoId(track.id) }
        .ifBlank { track.id.takeUnless { it.startsWith("chart-") || it.contains("://") }.orEmpty() }
    if (videoId.isBlank()) return null
    val videoUrl = track.videoUrl.ifBlank { "https://www.youtube.com/watch?v=$videoId" }
    return track.copy(id = videoId, videoUrl = videoUrl)
}


private val YOUTUBE_ENGAGEMENT_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
''',
    '''internal fun playbackIdentity(track: Track): String = LevyraPersonalOrbit.identityKey(track)

private val YOUTUBE_PLAYABLE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

internal fun youtubePlayableTrack(track: Track, preferVideo: Boolean = false): Track? {
    val counterpart = track.counterpartVideoId.trim().takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    val fromUrl = youtubeVideoId(track.videoUrl).trim().takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    val fromIdUrl = youtubeVideoId(track.id).trim().takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    val rawId = track.id.trim().takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    val regular = sequenceOf(fromUrl, fromIdUrl, rawId).firstOrNull(String::isNotBlank).orEmpty()
    val videoId = if (preferVideo) counterpart.ifBlank { regular } else regular.ifBlank { counterpart }
    if (videoId.isBlank()) return null
    val existingUrlId = youtubeVideoId(track.videoUrl)
    val videoUrl = track.videoUrl.takeIf { existingUrlId == videoId }
        ?: "https://www.youtube.com/watch?v=$videoId"
    return track.copy(id = videoId, videoUrl = videoUrl)
}


private val YOUTUBE_ENGAGEMENT_VIDEO_ID = YOUTUBE_PLAYABLE_VIDEO_ID
''',
    "counterpart-aware YouTube identity",
)

replace_once(
    VM,
    '''        homeSnapshotJob?.cancel()
        super.onCleared()
''',
    '''        homeSnapshotJob?.cancel()
        musicVideosJob?.cancel()
        super.onCleared()
''',
    "music video loader cleanup",
)

replace_once(
    PLAYER_TEST,
    '''            videoUrl = "https://www.youtube.com/watch?v=video-123"
''',
    '''            videoUrl = "https://www.youtube.com/watch?v=video123456"
''',
    "valid chart video test id",
)
replace_once(
    PLAYER_TEST,
    '''        assertEquals("video-123", playable?.id)
        assertEquals("https://www.youtube.com/watch?v=video-123", playable?.videoUrl)
    }
''',
    '''        assertEquals("video123456", playable?.id)
        assertEquals("https://www.youtube.com/watch?v=video123456", playable?.videoUrl)
    }

    @Test
    fun youtubePlayableTrackPrefersOfficialCounterpartInVideoMode() {
        val songTrack = track(streamUrl = "").copy(
            id = "audio123456",
            videoUrl = "https://www.youtube.com/watch?v=audio123456",
            counterpartVideoId = "video123456"
        )

        val playable = youtubePlayableTrack(songTrack, preferVideo = true)

        assertEquals("video123456", playable?.id)
        assertEquals("https://www.youtube.com/watch?v=video123456", playable?.videoUrl)
    }
''',
    "official counterpart regression test",
)
replace_once(
    PLAYER_TEST,
    '''        id = "video-123",
''',
    '''        id = "video123456",
''',
    "valid default test track id",
)
replace_once(
    PLAYER_TEST,
    '''        videoUrl = "https://www.youtube.com/watch?v=video-123",
''',
    '''        videoUrl = "https://www.youtube.com/watch?v=video123456",
''',
    "valid default test track URL",
)

orbit_test = ROOT / ORBIT_TEST
orbit_test.parent.mkdir(parents=True, exist_ok=True)
orbit_test.write_text(
    '''package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraPersonalOrbitIdentityTest {
    @Test
    fun deduplicatesSameRecordingAcrossDifferentYoutubeLinksAndArtistSeparators() {
        val audio = track(
            id = "audio123456",
            title = "Dai Dai (Official Audio)",
            artist = "Shakira & Burna Boy",
            videoUrl = "https://www.youtube.com/watch?v=audio123456"
        )
        val video = track(
            id = "video123456",
            title = "Dai Dai [Official Music Video]",
            artist = "Burna Boy, Shakira",
            videoUrl = "https://www.youtube.com/watch?v=video123456",
            counterpartVideoId = "video123456"
        )

        val result = LevyraPersonalOrbit.distinctRecordings(listOf(audio, video))

        assertEquals(1, result.size)
        assertEquals("video123456", result.single().counterpartVideoId)
        assertTrue(LevyraPersonalOrbit.sameRecording(audio, video))
    }

    @Test
    fun keepsMeaningfullyDifferentVersionsSeparate() {
        val studio = track(id = "studio12345", title = "Dai Dai", artist = "Shakira, Burna Boy")
        val live = track(id = "live1234567", title = "Dai Dai (Live)", artist = "Shakira & Burna Boy")

        assertEquals(2, LevyraPersonalOrbit.distinctRecordings(listOf(studio, live)).size)
    }

    private fun track(
        id: String,
        title: String,
        artist: String,
        videoUrl: String = "https://www.youtube.com/watch?v=$id",
        counterpartVideoId: String = ""
    ): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = 220_000L,
        streamUrl = "",
        videoUrl = videoUrl,
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 80,
        cacheScore = 80,
        accentStart = 0,
        accentEnd = 0,
        counterpartVideoId = counterpartVideoId
    )
}
''',
    encoding="utf-8",
)

print("Guarded Home/video/recording identity fixes staged successfully.")
