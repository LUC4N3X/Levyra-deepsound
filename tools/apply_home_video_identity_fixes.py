from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"
VM = ROOT / "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"
PLAYER_TEST = ROOT / "app/src/test/java/com/luc4n3x/levyra/player/LevyraPlayerTest.kt"


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    if new in text:
        return
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one anchor in {path}, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    VM,
    '''internal fun youtubePlayableTrack(track: Track, preferVideo: Boolean = false): Track? {
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
''',
    '''internal fun youtubePlayableTrack(track: Track, preferVideo: Boolean = false): Track? {
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
    return track.copy(videoUrl = videoUrl)
}
''',
    "preserve canonical track id during mode switching",
)

replace_once(
    APP,
    '''            .filter { section -> isMusicVideoSectionTitle(section.title) }
''',
    '''            .filter { section -> isMusicVideoSectionTitle(section.title, strings) }
''',
    "localized video section inclusion",
)
replace_once(
    APP,
    '''        else rawOtherSections.filterNot { section -> isMusicVideoSectionTitle(section.title) }
''',
    '''        else rawOtherSections.filterNot { section -> isMusicVideoSectionTitle(section.title, strings) }
''',
    "localized video section removal",
)
replace_once(
    APP,
    '''private fun isMusicVideoSectionTitle(title: String): Boolean {
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
    '''private fun isMusicVideoSectionTitle(title: String, strings: LevyraStrings): Boolean {
    val normalized = title.trim().lowercase(java.util.Locale.ROOT)
    val localizedLabels = listOf(strings.exploreNewVideos)
        .map { label -> label.trim().lowercase(java.util.Locale.ROOT) }
        .filter(String::isNotBlank)
    return localizedLabels.any { label -> normalized == label || normalized.contains(label) } ||
        normalized.contains("video musical") ||
        normalized.contains("music video") ||
        normalized.contains("official video") ||
        normalized.contains("videoclip") ||
        normalized.contains("video per te") ||
        normalized.contains("new videos") ||
        normalized.contains("top videos")
}
''',
    "localized video section detection",
)

replace_once(
    PLAYER_TEST,
    '''        assertEquals("video123456", playable?.id)
        assertEquals("https://www.youtube.com/watch?v=video123456", playable?.videoUrl)
''',
    '''        assertEquals("chart-abc", playable?.id)
        assertEquals("https://www.youtube.com/watch?v=video123456", playable?.videoUrl)
''',
    "chart video keeps canonical id",
)
replace_once(
    PLAYER_TEST,
    '''        assertEquals("video123456", playable?.id)
        assertEquals("https://www.youtube.com/watch?v=video123456", playable?.videoUrl)
    }

    private fun track''',
    '''        assertEquals("audio123456", playable?.id)
        assertEquals("https://www.youtube.com/watch?v=video123456", playable?.videoUrl)

        val restoredAudio = youtubePlayableTrack(playable!!, preferVideo = false)
        assertEquals("audio123456", restoredAudio?.id)
        assertEquals("https://www.youtube.com/watch?v=audio123456", restoredAudio?.videoUrl)
    }

    private fun track''',
    "video audio round trip",
)

print("Remaining Home/video fixes are staged.")
