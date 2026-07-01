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
    val accentEnd: Int,
    val videoStreamUrl: String = ""
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

data class AppUpdateInfo(
    val currentVersionName: String,
    val latestVersionName: String,
    val latestTag: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val publishedAt: String,
    val downloadUrl: String,
    val releaseUrl: String,
    val assetName: String,
    val directApk: Boolean,
    val isNewer: Boolean
)

fun Track.smartWeightFor(mood: Mood?): Int {
    if (mood == null) return replayScore
    val tagScore = mood.tags.intersect(moodTags).size * 18
    val energyPenalty = (energy - mood.energyTarget).absoluteValue
    return (replayScore + cacheScore + tagScore - energyPenalty).coerceIn(0, 100)
}

data class ArtistProfile(
    val browseId: String,
    val name: String,
    val bio: String,
    val subscribers: String,
    val monthlyListeners: String,
    val thumbnailUrl: String,
    val bannerUrl: String,
    val topSongs: List<Track>,
    val albums: List<ArtistRelease>,
    val singles: List<ArtistRelease>,
    val accentStart: Int,
    val accentEnd: Int
) {
    val hasBio: Boolean
        get() = bio.isNotBlank()
}

data class ArtistRelease(
    val browseId: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val year: String
)

data class DownloadedTrack(
    val id: Long,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val fileName: String,
    val uri: String,
    val mimeType: String,
    val embeddedMetadata: Boolean,
    val savedAt: Long
)

data class ArtistHit(
    val name: String,
    val subscribers: String,
    val thumbnailUrl: String,
    val accentStart: Int,
    val accentEnd: Int
)

data class AlbumHit(
    val title: String,
    val artist: String,
    val year: String,
    val thumbnailUrl: String,
    val query: String
)

data class SearchResults(
    val topTrack: Track? = null,
    val songs: List<Track> = emptyList(),
    val artists: List<ArtistHit> = emptyList(),
    val albums: List<AlbumHit> = emptyList()
) {
    val isEmpty: Boolean
        get() = topTrack == null && songs.isEmpty() && artists.isEmpty() && albums.isEmpty()
}

enum class SearchFilter {
    All,
    Songs,
    Artists,
    Albums
}

data class Playlist(
    val id: String,
    val name: String,
    val coverUrl: String,
    val tracks: List<Track>,
    val createdAt: Long,
    val updatedAt: Long
) {
    val size: Int get() = tracks.size
}
