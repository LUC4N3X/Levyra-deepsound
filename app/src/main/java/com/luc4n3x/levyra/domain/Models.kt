package com.luc4n3x.levyra.domain

import com.luc4n3x.levyra.ui.i18n.LevyraStrings
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
        ChartRegion("es", "España", "🇪🇸", "es"),
        ChartRegion("fr", "France", "🇫🇷", "fr"),
        ChartRegion("de", "Deutschland", "🇩🇪", "de"),
        ChartRegion("br", "Brasil", "🇧🇷", "br"),
        ChartRegion("mx", "México", "🇲🇽", "mx"),
        ChartRegion("nl", "Nederland", "🇳🇱", "nl"),
        ChartRegion("pl", "Polska", "🇵🇱", "pl"),
        ChartRegion("ro", "România", "🇷🇴", "ro"),
        ChartRegion("gr", "Ελλάδα", "🇬🇷", "gr"),
        ChartRegion("se", "Sverige", "🇸🇪", "se"),
        ChartRegion("dk", "Danmark", "🇩🇰", "dk"),
        ChartRegion("cz", "Česko", "🇨🇿", "cz"),
        ChartRegion("ua", "Україна", "🇺🇦", "ua"),
        ChartRegion("jp", "日本", "🇯🇵", "jp")
    )

    fun region(id: String): ChartRegion = regions.firstOrNull { it.id == id } ?: regions.first()

    fun defaultRegionForLanguage(languageCode: String): ChartRegion {
        val locale = LevyraContentLocales.forLanguage(languageCode)
        return region(locale.chartRegionId)
    }
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

data class SmartMusicTasteSeed(
    val label: String,
    val query: String,
    val weight: Int
)

data class SmartMusicProfile(
    val plays: Int = 0,
    val completedPlays: Int = 0,
    val favoriteSignals: Int = 0,
    val downloadSignals: Int = 0,
    val albumOpenSignals: Int = 0,
    val topArtists: List<SmartMusicTasteSeed> = emptyList(),
    val topAlbums: List<SmartMusicTasteSeed> = emptyList(),
    val topMoods: List<SmartMusicTasteSeed> = emptyList(),
    val lastUpdated: Long = 0L
) {
    val isWarm: Boolean
        get() = plays + completedPlays + favoriteSignals + downloadSignals + albumOpenSignals >= 4

    val albumQueries: List<String>
        get() = topAlbums.map { it.query }

    val artistQueries: List<String>
        get() = topArtists.map { it.query }
}


enum class LevyraTab {
    Home,
    Search,
    Explore,
    Library,
    Player
}

data class ExploreZone(
    val id: String,
    val label: String,
    val emoji: String,
    val query: String,
    val accentStart: Int,
    val accentEnd: Int
)

object ExploreCatalog {
    fun getZones(strings: LevyraStrings): List<ExploreZone> {
        val locale = LevyraContentLocales.forLanguage(strings.code)
        val localQuery = strings.localWaveQuery.ifBlank { locale.homeQueries.firstOrNull().orEmpty() }
        val newReleaseQuery = "${locale.homeQueries.firstOrNull().orEmpty()} new releases 2026".trim()
        val rapQuery = locale.queryForTaste("rap")
        val popQuery = locale.queryForTaste("pop")
        val partyQuery = locale.queryForTaste("party")
        val electroQuery = locale.queryForTaste("electro")
        val rnbQuery = locale.queryForTaste("rnb")
        val rockQuery = locale.queryForTaste("rock")
        val chillQuery = locale.queryForTaste("chill")
        val latinoQuery = when (locale.languageCode) {
            "es" -> "reggaeton latino música latina nueva 2026"
            "pt" -> "funk brasileiro latino hits 2026"
            else -> "latin music new hits 2026"
        }
        return listOf(
            ExploreZone("nuove-uscite", strings.exploreNewReleases, "🌊", newReleaseQuery, 0xFF00E5FF.toInt(), 0xFF2979FF.toInt()),
            ExploreZone("local-wave", strings.localWaveName, strings.localWaveEmoji, localQuery, 0xFF00E676.toInt(), 0xFF00B0FF.toInt()),
            ExploreZone("rap-drill", strings.exploreRapDrill, "🐙", rapQuery, 0xFF9D4EDD.toInt(), 0xFF7C4DFF.toInt()),
            ExploreZone("elettronica", strings.exploreElectronic, "⚡", electroQuery, 0xFF18FFFF.toInt(), 0xFF9D4EDD.toInt()),
            ExploreZone("pop-global", strings.explorePopGlobal, "🌍", popQuery, 0xFFFF4081.toInt(), 0xFF7C4DFF.toInt()),
            ExploreZone("rnb-soul", strings.exploreRnbSoul, "🌒", rnbQuery, 0xFFB388FF.toInt(), 0xFFFF4081.toInt()),
            ExploreZone("rock-alt", strings.exploreRockAlt, "🦑", rockQuery, 0xFFFF6E40.toInt(), 0xFFFF1744.toInt()),
            ExploreZone("latino", strings.exploreLatino, "🔥", latinoQuery, 0xFFFFC400.toInt(), 0xFFFF6E40.toInt()),
            ExploreZone("lofi-chill", strings.exploreLofiChill, "🫧", chillQuery, 0xFF64FFDA.toInt(), 0xFF00B0FF.toInt()),
            ExploreZone("anime-jpop", strings.exploreJpopAnime, "🏮", partyQuery, 0xFFFF5252.toInt(), 0xFFB388FF.toInt())
        )
    }

    fun byId(id: String?, strings: LevyraStrings): ExploreZone? = getZones(strings).firstOrNull { it.id == id }
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
    val accentEnd: Int,
    val relatedArtists: List<ArtistHit> = emptyList()
) {
    val hasBio: Boolean
        get() = bio.isNotBlank()
}

data class FollowedArtist(
    val browseId: String,
    val name: String,
    val thumbnailUrl: String,
    val followedAt: Long
) {
    val key: String
        get() = browseId.ifBlank { name.trim().lowercase() }
}

data class ReleaseRadarEntry(
    val artistName: String,
    val artistBrowseId: String,
    val release: ArtistRelease,
    val isFresh: Boolean
)

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
    val query: String,
    val browseId: String = ""
)

data class AlbumDetail(
    val album: AlbumHit,
    val description: String,
    val tracks: List<Track>
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
