package com.luc4n3x.levyra.feature.discovery

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.viewmodel.playbackTitleKey
import java.util.Locale

enum class DiscoveryReasonKind { SimilarToTrack, FollowedArtist, NewRelease, SimilarArtist, Trending, Editorial }

@Immutable
data class DiscoveryReason(val kind: DiscoveryReasonKind, val seedTitle: String = "")

@Immutable
data class DiscoveryCandidate(
    val track: Track,
    val reason: DiscoveryReason,
    val fresh: Boolean = false,
    val confidence: Int = 0
)

@Immutable
data class DiscoveryCard(val track: Track, val reason: DiscoveryReason) {
    val key: String get() = discoveryTrackKey(track)
}

data class DiscoveryTasteProfile(
    val languageCode: String,
    val market: String,
    val seeds: List<Track>,
    val artistAffinity: Map<String, Int>,
    val followedArtistKeys: Set<String>,
    val excludedArtistKeys: Set<String>,
    val knownTrackKeys: Set<String>,
    val similarArtistNames: List<String>
) {
    val isColdStart: Boolean get() = seeds.size < COLD_START_SEEDS

    fun affinityFor(artist: String): Int = discoveryArtistKeys(artist).sumOf { artistAffinity[it] ?: 0 }

    fun followsAny(artist: String): Boolean = discoveryArtistKeys(artist).any { it in followedArtistKeys }

    fun excludes(artist: String): Boolean = discoveryArtistKeys(artist).any { it in excludedArtistKeys }

    companion object {
        const val COLD_START_SEEDS = 3
        private const val MAX_SEEDS = 8
        private const val FOLLOWED_WEIGHT = 40
        private const val FAVORITE_WEIGHT = 10
        private const val RECENT_WEIGHT = 6
        private const val PLAYLIST_WEIGHT = 3
        private const val MAX_ARTIST_WEIGHT = 80

        fun from(
            languageCode: String,
            market: String,
            recentListens: List<Track>,
            favorites: List<Track>,
            playlistTracks: List<Track>,
            followedArtists: List<String>,
            excludedArtists: List<String>,
            similarArtists: List<String>
        ): DiscoveryTasteProfile {
            val weights = HashMap<String, Int>()
            fun credit(artist: String, amount: Int) {
                discoveryArtistKeys(artist).forEach { key -> weights[key] = ((weights[key] ?: 0) + amount).coerceAtMost(MAX_ARTIST_WEIGHT) }
            }
            recentListens.forEach { credit(it.artist, RECENT_WEIGHT) }
            favorites.forEach { credit(it.artist, FAVORITE_WEIGHT) }
            playlistTracks.forEach { credit(it.artist, PLAYLIST_WEIGHT) }
            followedArtists.forEach { credit(it, FOLLOWED_WEIGHT) }
            val seeds = (recentListens.take(12) + favorites.take(12))
                .filter { it.title.isNotBlank() }
                .distinctBy(::discoveryTrackKey)
                .distinctBy { discoveryPrimaryArtistKey(it.artist) }
                .take(MAX_SEEDS)
            val known = (recentListens + favorites + playlistTracks).mapTo(HashSet(), ::discoveryTrackKey)
            return DiscoveryTasteProfile(
                languageCode = languageCode,
                market = market,
                seeds = seeds,
                artistAffinity = weights,
                followedArtistKeys = followedArtists.flatMapTo(HashSet(), ::discoveryArtistKeys),
                excludedArtistKeys = excludedArtists.flatMapTo(HashSet(), ::discoveryArtistKeys),
                knownTrackKeys = known,
                similarArtistNames = similarArtists.filter(String::isNotBlank).distinctBy { it.lowercase(Locale.ROOT) }
            )
        }
    }
}

private val ARTIST_SEPARATORS = Regex("""\s*(?:,|&|\s+x\s+|\s+feat\.?\s+|\s+ft\.?\s+|\s+featuring\s+|\s+with\s+)\s*""", RegexOption.IGNORE_CASE)
private val WHITESPACE = Regex("""\s+""")

internal fun discoveryArtistKeys(artist: String): List<String> =
    artist.split(ARTIST_SEPARATORS)
        .map { it.trim().lowercase(Locale.ROOT).replace(WHITESPACE, " ") }
        .filter { it.length >= 2 }
        .distinct()

internal fun discoveryPrimaryArtistKey(artist: String): String = discoveryArtistKeys(artist).firstOrNull().orEmpty()

internal fun discoveryTrackKey(track: Track): String =
    playbackTitleKey(track.title) + "|" + discoveryPrimaryArtistKey(track.artist)
