from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one match, found {count}")
    file.write_text(text.replace(old, new, 1))


resolver = "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt"
test = "app/src/test/java/com/luc4n3x/levyra/data/OfficialVideoCandidateTest.kt"

replace_once(
    resolver,
    '''internal fun officialYoutubeMusicVideoCandidate(\n    sourceVideoId: String,\n    playlist: YoutubeMusicWatchPlaylist\n): YoutubeMusicWatchTrack? {\n    val primary = playlist.tracks.firstOrNull { it.videoId == sourceVideoId }\n        ?: playlist.tracks.firstOrNull()\n    return sequenceOf(primary?.counterpart, primary)\n        .filterNotNull()\n        .firstOrNull { candidate ->\n            candidate.videoId.length == 11 &&\n                candidate.videoId.all { it.isLetterOrDigit() || it == '_' || it == '-' } &&\n                candidate.videoType.equals("MUSIC_VIDEO_TYPE_OMV", ignoreCase = true)\n        }\n}\n''',
    '''internal fun officialYoutubeMusicVideoCandidate(\n    sourceVideoId: String,\n    playlist: YoutubeMusicWatchPlaylist\n): YoutubeMusicWatchTrack? {\n    val primary = playlist.tracks.firstOrNull { it.videoId == sourceVideoId }\n        ?: playlist.tracks.firstOrNull()\n    return sequenceOf(primary?.counterpart, primary)\n        .filterNotNull()\n        .firstOrNull { candidate ->\n            candidate.videoId.length == 11 &&\n                candidate.videoId.all { it.isLetterOrDigit() || it == '_' || it == '-' } &&\n                candidate.videoType.equals("MUSIC_VIDEO_TYPE_OMV", ignoreCase = true)\n        }\n}\n\ninternal fun shouldLookupOfficialVideo(videoType: String): Boolean {\n    val type = videoType.uppercase(java.util.Locale.ROOT)\n    return !type.contains("OMV") && !type.contains("UGC")\n}\n'''
)

replace_once(
    resolver,
    '''    private val playbackResolveTimeoutMs = 30_000L\n    private val offlineResolveTimeoutMs = 60_000L''',
    '''    private val playbackResolveTimeoutMs = 30_000L\n    private val officialVideoLookupTimeoutMs = 2_000L\n    private val offlineResolveTimeoutMs = 60_000L'''
)

replace_once(
    resolver,
    '''        if (sourceVideoId.isBlank()) return track\n        if (track.videoType.equals("MUSIC_VIDEO_TYPE_OMV", ignoreCase = true)) return track\n\n        val currentType = track.videoType.uppercase(java.util.Locale.ROOT)\n        val currentIsVisual = currentType.contains("OMV") || currentType.contains("UGC") || currentType.contains("MUSIC_VIDEO")\n        val knownCounterpart = track.counterpartVideoId\n            .trim()\n            .takeIf(youtubeVideoIdRegex::matches)\n            ?.takeIf { it != sourceVideoId && !currentIsVisual }\n        if (knownCounterpart != null) {\n            return track.copy(\n                videoUrl = "https://www.youtube.com/watch?v=$knownCounterpart",\n                counterpartVideoId = knownCounterpart\n            )\n        }\n\n        val watch = runCatchingPreservingCancellation {\n            watchRepository.getWatchPlaylist(sourceVideoId, userPreferences.languageCode(), 1)\n        }.getOrNull() ?: return track''',
    '''        if (sourceVideoId.isBlank() || !shouldLookupOfficialVideo(track.videoType)) return track\n\n        val knownCounterpart = track.counterpartVideoId\n            .trim()\n            .takeIf(youtubeVideoIdRegex::matches)\n            ?.takeIf { it != sourceVideoId }\n        if (knownCounterpart != null) {\n            return track.copy(\n                videoUrl = "https://www.youtube.com/watch?v=$knownCounterpart",\n                counterpartVideoId = knownCounterpart\n            )\n        }\n\n        val watch = runCatchingPreservingCancellation {\n            withTimeout(officialVideoLookupTimeoutMs) {\n                watchRepository.getWatchPlaylist(sourceVideoId, userPreferences.languageCode(), 1)\n            }\n        }.getOrNull() ?: return track'''
)

replace_once(
    test,
    '''    @Test\n    fun rejectsUserGeneratedCounterpartAsOfficial() {''',
    '''    @Test\n    fun artTrackStillLooksForOfficialVideo() {\n        assertEquals(true, shouldLookupOfficialVideo("MUSIC_VIDEO_TYPE_ATV"))\n    }\n\n    @Test\n    fun visualVideoTypesDoNotGetRewritten() {\n        assertEquals(false, shouldLookupOfficialVideo("MUSIC_VIDEO_TYPE_OMV"))\n        assertEquals(false, shouldLookupOfficialVideo("MUSIC_VIDEO_TYPE_UGC"))\n    }\n\n    @Test\n    fun rejectsUserGeneratedCounterpartAsOfficial() {'''
)
