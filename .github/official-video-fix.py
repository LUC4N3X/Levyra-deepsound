from pathlib import Path
import re


def replace_regex(path: str, pattern: str, replacement: str, flags=re.S) -> None:
    p = Path(path)
    text = p.read_text()
    updated, count = re.subn(pattern, replacement, text, count=1, flags=flags)
    if count != 1:
        raise SystemExit(f"{path}: expected one regex match, found {count}")
    p.write_text(updated)


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one match, found {count}")
    p.write_text(text.replace(old, new, 1))


viewmodel = "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"
resolver = "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt"
yt_music = "app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicRepository.kt"
official_test = "app/src/test/java/com/luc4n3x/levyra/data/OfficialVideoCandidateTest.kt"
player_test = "app/src/test/java/com/luc4n3x/levyra/player/LevyraPlayerTest.kt"

# The ViewModel must not decide whether an untyped counterpart is official.
replace_regex(
    viewmodel,
    r'''internal fun youtubePlayableTrack\(track: Track, preferVideo: Boolean = false\): Track\? \{.*?\n\}\n\n\nprivate val YOUTUBE_ENGAGEMENT_VIDEO_ID''',
    '''internal fun youtubePlayableTrack(track: Track, preferVideo: Boolean = false): Track? {
    val counterpart = track.counterpartVideoId.trim().takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    val fromUrl = youtubeVideoId(track.videoUrl).trim().takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    val fromIdUrl = youtubeVideoId(track.id).trim().takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    val rawId = track.id.trim().takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    val canonical = sequenceOf(fromIdUrl, rawId, fromUrl, counterpart)
        .firstOrNull(String::isNotBlank)
        .orEmpty()
    val currentIsOfficialVideo = track.videoType.equals("MUSIC_VIDEO_TYPE_OMV", ignoreCase = true)
    val videoId = when {
        currentIsOfficialVideo && preferVideo -> fromUrl.ifBlank { rawId.ifBlank { canonical } }
        currentIsOfficialVideo && !preferVideo -> counterpart.ifBlank { canonical }
        else -> canonical
    }
    if (videoId.isBlank()) return null
    val existingUrlId = youtubeVideoId(track.videoUrl)
    val videoUrl = track.videoUrl.takeIf { existingUrlId == videoId }
        ?: "https://www.youtube.com/watch?v=$videoId"
    return track.copy(videoUrl = videoUrl)
}


private val YOUTUBE_ENGAGEMENT_VIDEO_ID'''
)

# Expose a strict YouTube Music Videos search. UGC/ATV are deliberately excluded.
replace_once(
    yt_music,
    '''    private fun searchMusicVideoSamples(
''',
    '''    suspend fun searchOfficialMusicVideos(
        query: String,
        limit: Int = 12,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): List<Track> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()
        val root = searchInnerTubeRaw(cleanQuery, languageCode, YOUTUBE_MUSIC_VIDEO_SEARCH_PARAMS)
            ?: return@withContext emptyList()
        val renderers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicResponsiveListItemRenderer", renderers)
        renderers.asSequence()
            .mapNotNull { renderer -> parseMusicRenderer(renderer, cleanQuery) }
            .filter { track ->
                track.id.length == 11 &&
                    track.videoUrl.isNotBlank() &&
                    track.videoType.equals("MUSIC_VIDEO_TYPE_OMV", ignoreCase = true)
            }
            .distinctBy { it.id }
            .take(limit.coerceIn(1, 24))
            .toList()
    }

    private fun searchMusicVideoSamples(
'''
)

# Resolver owns official-video selection and performs bounded Watch + strict OMV search in parallel.
replace_once(
    resolver,
    '''import kotlinx.coroutines.CompletableDeferred
''',
    '''import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
'''
)
replace_once(
    resolver,
    '''    private val watchRepository = YoutubeMusicWatchRepository(context)
''',
    '''    private val watchRepository = YoutubeMusicWatchRepository(context)
    private val youtubeMusicRepository = YoutubeMusicRepository(context)
'''
)
replace_regex(
    resolver,
    r'''internal fun shouldLookupOfficialVideo\(videoType: String\): Boolean \{.*?\n\}\n''',
    '''internal fun shouldLookupOfficialVideo(videoType: String): Boolean {
    val type = videoType.uppercase(java.util.Locale.ROOT)
    return !type.contains("OMV") && !type.contains("PODCAST")
}

internal fun officialVideoSearchScore(source: Track, candidate: Track): Int {
    if (!candidate.videoType.equals("MUSIC_VIDEO_TYPE_OMV", ignoreCase = true)) return Int.MIN_VALUE
    if (candidate.id.length != 11 || candidate.videoUrl.isBlank()) return Int.MIN_VALUE

    val sourceTitle = officialVideoTextKey(source.title)
    val candidateTitle = officialVideoTextKey(candidate.title)
    if (sourceTitle.isBlank() || candidateTitle.isBlank()) return Int.MIN_VALUE
    val titleScore = when {
        candidateTitle == sourceTitle -> 260
        candidateTitle.contains(sourceTitle) || sourceTitle.contains(candidateTitle) -> 210
        officialVideoTokenCoverage(sourceTitle, candidateTitle) >= 0.8 -> 160
        else -> return Int.MIN_VALUE
    }

    val sourceIds = source.artistBrowseIds.map(String::trim).filter(String::isNotBlank).toSet()
    val candidateIds = candidate.artistBrowseIds.map(String::trim).filter(String::isNotBlank).toSet()
    val browseIdScore = when {
        sourceIds.isNotEmpty() && candidateIds.isNotEmpty() && sourceIds.intersect(candidateIds).isNotEmpty() -> 190
        sourceIds.isNotEmpty() && candidateIds.isNotEmpty() -> return Int.MIN_VALUE
        else -> 0
    }

    val sourceArtist = officialVideoTextKey(source.artist)
    val candidateArtist = officialVideoTextKey(candidate.artist)
    val artistScore = if (browseIdScore > 0) {
        browseIdScore
    } else if (sourceArtist.isBlank()) {
        0
    } else {
        when {
            candidateArtist == sourceArtist -> 150
            candidateArtist.contains(sourceArtist) || sourceArtist.contains(candidateArtist) -> 110
            officialVideoTokenCoverage(sourceArtist, "$candidateArtist $candidateTitle") >= 0.6 -> 80
            else -> return Int.MIN_VALUE
        }
    }

    val durationScore = if (source.durationMs > 0L && candidate.durationMs > 0L) {
        when (kotlin.math.abs(source.durationMs - candidate.durationMs)) {
            in 0L..5_000L -> 80
            in 5_001L..20_000L -> 45
            in 20_001L..45_000L -> 0
            else -> -180
        }
    } else {
        0
    }
    return titleScore + artistScore + durationScore
}

private fun officialVideoTextKey(value: String): String {
    return value.lowercase(java.util.Locale.ROOT)
        .replace(Regex("(?i)\\b(?:official\\s+visual\\s+video|official\\s+music\\s+video|official\\s+video|official\\s+visualizer|music\\s+video|visual\\s+video|visualizer|video\\s+ufficiale)\\b"), " ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun officialVideoTokenCoverage(target: String, candidate: String): Double {
    val targetTokens = target.split(' ').filter(String::isNotBlank)
    if (targetTokens.isEmpty()) return 0.0
    val candidateTokens = candidate.split(' ').filter(String::isNotBlank).toSet()
    return targetTokens.count { it in candidateTokens }.toDouble() / targetTokens.size.toDouble()
}
'''
)

replace_regex(
    resolver,
    r'''    private suspend fun preferOfficialVideo\(track: Track\): Track \{.*?\n    \}\n\n    private suspend fun resolveAudioFast''',
    '''    private suspend fun preferOfficialVideo(track: Track): Track = coroutineScope {
        val sourceVideoId = extractVideoId(track.videoUrl)
            .ifBlank { track.id.takeIf(youtubeVideoIdRegex::matches).orEmpty() }
        if (sourceVideoId.isBlank() || !shouldLookupOfficialVideo(track.videoType)) return@coroutineScope track

        val languageCode = userPreferences.languageCode()
        val knownCounterpart = track.counterpartVideoId
            .trim()
            .takeIf(youtubeVideoIdRegex::matches)
            ?.takeIf { it != sourceVideoId }
            .orEmpty()
        val baseQuery = listOf(track.artist.trim(), track.title.trim())
            .filter(String::isNotBlank)
            .joinToString(" ")
            .trim()

        val watchDeferred = async {
            runCatchingPreservingCancellation {
                withTimeout(officialVideoLookupTimeoutMs) {
                    watchRepository.getWatchPlaylist(sourceVideoId, languageCode, 1)
                }
            }.getOrNull()?.let { watch -> officialYoutubeMusicVideoCandidate(sourceVideoId, watch) }
        }
        val searchDeferred = async {
            if (baseQuery.length < 2) return@async emptyList<Track>()
            val queries = listOf(baseQuery, "$baseQuery official video").distinct()
            coroutineScope {
                queries.map { query ->
                    async {
                        runCatchingPreservingCancellation {
                            withTimeout(officialVideoLookupTimeoutMs) {
                                youtubeMusicRepository.searchOfficialMusicVideos(query, 10, languageCode)
                            }
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll().flatten()
            }
        }

        val watchOfficial = watchDeferred.await()
        if (watchOfficial != null) {
            searchDeferred.cancel()
            Timber.d("official video selected via YouTube Music counterpart: %s -> %s", sourceVideoId, watchOfficial.videoId)
            return@coroutineScope track.copy(
                videoUrl = "https://www.youtube.com/watch?v=${watchOfficial.videoId}",
                counterpartVideoId = watchOfficial.videoId
            )
        }

        val searched = searchDeferred.await()
            .distinctBy { it.id }
            .map { candidate ->
                val score = officialVideoSearchScore(track, candidate) +
                    if (knownCounterpart.isNotBlank() && candidate.id == knownCounterpart) 120 else 0
                candidate to score
            }
            .filter { (_, score) -> score >= 260 }
            .maxByOrNull { (_, score) -> score }
            ?.first

        if (searched == null) return@coroutineScope track
        Timber.d("official video selected via YouTube Music video search: %s -> %s", sourceVideoId, searched.id)
        track.copy(
            videoUrl = "https://www.youtube.com/watch?v=${searched.id}",
            counterpartVideoId = searched.id
        )
    }

    private suspend fun resolveAudioFast'''
)

# Extract video hedge into a helper and retry the canonical source only if the verified official source fails.
video_block = re.compile(r'''        if \(isVideoMode\) \{\n            val resolved = coroutineScope \{.*?\n            throw PlaybackBlockedException\(reason\)\n        \}\n\n        val resolved = resolveAudioFast''', re.S)
text = Path(resolver).read_text()
match = video_block.search(text)
if not match:
    raise SystemExit("PlaybackResolver.kt: video mode block not found")
new_video_block = '''        if (isVideoMode) {
            val officialResolved = resolveVideoTrack(sourceTrack, errors, audioQuality)
            val selectedSource: Track
            val resolved: Track?
            if (officialResolved != null) {
                selectedSource = sourceTrack
                resolved = officialResolved
            } else if (sourceTrack.videoUrl != track.videoUrl) {
                Timber.w("official video playback failed, falling back to canonical source: %s", sourceTrack.videoUrl)
                selectedSource = track
                resolved = resolveVideoTrack(track, errors, audioQuality)
            } else {
                selectedSource = sourceTrack
                resolved = null
            }
            if (resolved != null) {
                store(track, resolved, isVideoMode, audioQuality)
                if (selectedSource.videoUrl != track.videoUrl) store(selectedSource, resolved, isVideoMode, audioQuality)
                persistResolvedSource(selectedSource, resolved, isVideoMode, audioQuality, 92, preferMp4Audio)
                return@withContext resolved
            }
            val reason = errors.firstOrNull { it.contains("age", true) || it.contains("login", true) }
                ?: errors.firstOrNull()
                ?: "Video non disponibile"
            throw PlaybackBlockedException(reason)
        }

        val resolved = resolveAudioFast'''
text = text[:match.start()] + new_video_block + text[match.end():]
Path(resolver).write_text(text)

replace_once(
    resolver,
    '''    private suspend fun preferOfficialVideo(track: Track): Track = coroutineScope {
''',
    '''    private suspend fun resolveVideoTrack(
        track: Track,
        errors: MutableList<String>,
        audioQuality: String
    ): Track? = coroutineScope {
        val winner = CompletableDeferred<Track?>()
        val extractorJob = launch {
            delay(LevyraResolverLatency.extractorHedgeDelayMs(isVideoMode = true, preferMp4Audio = false))
            if (winner.isCompleted) return@launch
            val result = runCatchingPreservingCancellation { resolveVideoWithLevyraExtractor(track, audioQuality) }
            result.onSuccess { winner.complete(it) }
                .onFailure { error -> errors += "LevyraExtractor video: ${error.playbackDiagnostic()}" }
        }
        val innerTubeJob = launch {
            delay(LevyraResolverLatency.innerTubeFallbackDelayMs(isVideoMode = true, preferMp4Audio = false))
            if (winner.isCompleted) return@launch
            val stream = runCatchingPreservingCancellation { hedgedInnerTube(track, errors, true, audioQuality) }.getOrNull()
            if (stream != null) winner.complete(track.withDirectStream(stream))
        }
        launch {
            extractorJob.join()
            innerTubeJob.join()
            winner.complete(null)
        }
        val result = winner.await()
        coroutineContext.cancelChildren()
        result
    }

    private suspend fun preferOfficialVideo(track: Track): Track = coroutineScope {
'''
)

# Update helper expectations: untyped counterpart is not selected in the ViewModel anymore.
replace_once(
    player_test,
    '''        assertEquals("audio123456", video?.id)
        assertEquals("https://www.youtube.com/watch?v=video123456", video?.videoUrl)

        val audio = youtubePlayableTrack(video!!, preferVideo = false)
''',
    '''        assertEquals("audio123456", video?.id)
        assertEquals("https://www.youtube.com/watch?v=audio123456", video?.videoUrl)

        val audio = youtubePlayableTrack(video!!, preferVideo = false)
'''
)
replace_once(
    player_test,
    '''        assertEquals("audio123456", video?.id)
        assertEquals("https://www.youtube.com/watch?v=video123456", video?.videoUrl)
        assertEquals("audio123456", audio?.id)
''',
    '''        assertEquals("audio123456", video?.id)
        assertEquals("https://www.youtube.com/watch?v=audio123456", video?.videoUrl)
        assertEquals("audio123456", audio?.id)
'''
)

# UGC can have a better official OMV; only an already-OMV source skips lookup.
replace_once(
    official_test,
    '''    fun visualVideoTypesDoNotGetRewritten() {
        assertEquals(false, shouldLookupOfficialVideo("MUSIC_VIDEO_TYPE_OMV"))
        assertEquals(false, shouldLookupOfficialVideo("MUSIC_VIDEO_TYPE_UGC"))
    }
''',
    '''    fun officialVideoSkipsLookupButUgcCanUpgradeToOfficial() {
        assertEquals(false, shouldLookupOfficialVideo("MUSIC_VIDEO_TYPE_OMV"))
        assertEquals(true, shouldLookupOfficialVideo("MUSIC_VIDEO_TYPE_UGC"))
    }
'''
)
replace_once(
    official_test,
    '''import org.junit.Assert.assertEquals
''',
    '''import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
'''
)
insert = '''
    @Test
    fun officialSearchMatchesOfficialVisualVideoForSameRecording() {
        val source = track("audio123456", "Da Dio", "Bresh", "MUSIC_VIDEO_TYPE_ATV", 173_000L)
        val official = track("XxSIyhr_bFc", "Bresh - Da Dio (Official Visual Video)", "Bresh", "MUSIC_VIDEO_TYPE_OMV", 173_000L)

        assertEquals(true, officialVideoSearchScore(source, official) >= 260)
    }

    @Test
    fun officialSearchRejectsOmvFromWrongArtist() {
        val source = track("audio123456", "Da Dio", "Bresh", "MUSIC_VIDEO_TYPE_ATV", 173_000L)
        val wrong = track("other123456", "Da Dio (Official Video)", "Another Artist", "MUSIC_VIDEO_TYPE_OMV", 173_000L)

        assertEquals(Int.MIN_VALUE, officialVideoSearchScore(source, wrong))
    }

    private fun track(id: String, title: String, artist: String, videoType: String, durationMs: Long) = Track(
        id = id,
        title = title,
        artist = artist,
        album = "",
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0,
        videoType = videoType
    )
'''
replace_once(
    official_test,
    '''    private fun watchTrack(
''',
    insert + '''
    private fun watchTrack(
'''
)

# Guard against accidental broad YouTube web fallback in the official-video path.
for path in (resolver, yt_music, viewmodel, official_test, player_test):
    text = Path(path).read_text()
    if "officialVideoSearchScore" in text and "youtube.com/results?search_query" in text and path == resolver:
        # The repository has a separate AUDIO fallback that uses YouTube Web. It is intentionally not part of preferOfficialVideo.
        prefer = text[text.index("private suspend fun preferOfficialVideo"):text.index("private suspend fun resolveAudioFast")]
        if "youtube.com/results" in prefer or "searchYouTubeWebCandidates" in prefer:
            raise SystemExit("official video path must not use generic YouTube web search")

print("official video resolver patch applied")
