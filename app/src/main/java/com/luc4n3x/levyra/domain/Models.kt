package com.luc4n3x.levyra.domain

import kotlin.math.absoluteValue

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val streamUrl: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val largeThumbnailUrl: String,
    val source: String,
    val moodTags: Set<String>,
    val energy: Int,
    val vocal: Int,
    val replayScore: Int,
    val cacheScore: Int,
    val accentStart: Int,
    val accentEnd: Int
) {
    val hasPlayableStream: Boolean
        get() = streamUrl.isNotBlank()
}

data class Mood(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val energyTarget: Int,
    val tags: Set<String>,
    val accentStart: Int,
    val accentEnd: Int
)

data class Taste(
    val id: String,
    val label: String,
    val emoji: String,
    val query: String
)

data class HomeSection(
    val title: String,
    val tracks: List<Track>
)

data class ChartRegion(
    val id: String,
    val label: String,
    val emoji: String,
    val country: String
)

object ChartsCatalog {
    val regions: List<ChartRegion> = listOf(
        ChartRegion("it", "Italia", "🇮🇹", "it"),
        ChartRegion("us", "USA", "🇺🇸", "us"),
        ChartRegion("gb", "UK", "🇬🇧", "gb"),
        ChartRegion("es", "Spagna", "🇪🇸", "es"),
        ChartRegion("fr", "Francia", "🇫🇷", "fr"),
        ChartRegion("de", "Germania", "🇩🇪", "de"),
        ChartRegion("br", "Brasile", "🇧🇷", "br"),
        ChartRegion("mx", "Messico", "🇲🇽", "mx"),
        ChartRegion("nl", "Olanda", "🇳🇱", "nl"),
        ChartRegion("jp", "Giappone", "🇯🇵", "jp")
    )

    fun region(id: String): ChartRegion = regions.firstOrNull { it.id == id } ?: regions.first()
}

data class LyricLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val translated: String
)

data class CacheReport(
    val offlineReady: Int,
    val smartCached: Int,
    val nextPreload: Int,
    val totalTracks: Int
)

enum class LevyraTab {
    Home,
    Search,
    Library,
    Player
}

enum class RepeatMode {
    Off,
    All,
    One
}

data class SponsorSegment(
    val startMs: Long,
    val endMs: Long,
    val category: String
)

fun Track.smartWeightFor(mood: Mood?): Int {
    if (mood == null) return replayScore
    val tagScore = mood.tags.intersect(moodTags).size * 18
    val energyPenalty = (energy - mood.energyTarget).absoluteValue
    return (replayScore + cacheScore + tagScore - energyPenalty).coerceIn(0, 100)
}
