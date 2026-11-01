package com.luc4n3x.levyra.feature.discovery

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.domain.Track
import kotlin.math.abs

enum class BubbleLevel(val storageValue: String, val minDistance: Float, val maxDistance: Float, val sweetSpot: Float) {
    Near("near", 0f, 0.5f, 0.28f),
    Adventurous("adventurous", 0.35f, 0.8f, 0.6f),
    OutOfOrbit("out_of_orbit", 0.62f, 1f, 0.86f);

    companion object {
        fun fromStorage(value: String?): BubbleLevel = entries.firstOrNull { it.storageValue == value } ?: Near
    }
}

@Immutable
sealed interface DiscoveryLens {
    data object Open : DiscoveryLens
    data class Country(val regionId: String, val label: String) : DiscoveryLens
    data class Scene(val params: String, val title: String) : DiscoveryLens
    data class Decade(val startYear: Int) : DiscoveryLens
    data class City(val name: String) : DiscoveryLens
}

@Immutable
data class DiscoveryRequest(
    val level: BubbleLevel = BubbleLevel.Near,
    val lens: DiscoveryLens = DiscoveryLens.Open
) {
    val usesDistanceBand: Boolean get() = lens == DiscoveryLens.Open
}

object BubbleDistance {
    private const val FAMILIAR_CEILING = 0.2f
    private const val SIMILAR_CEILING = 0.34f

    fun base(kind: DiscoveryReasonKind): Float = when (kind) {
        DiscoveryReasonKind.FollowedArtist -> 0.05f
        DiscoveryReasonKind.SimilarToTrack -> 0.3f
        DiscoveryReasonKind.SimilarArtist -> 0.36f
        DiscoveryReasonKind.Trending -> 0.4f
        DiscoveryReasonKind.NewRelease -> 0.42f
        DiscoveryReasonKind.Editorial -> 0.45f
        DiscoveryReasonKind.SecondHop -> 0.6f
        DiscoveryReasonKind.Scene -> 0.7f
        DiscoveryReasonKind.Decade -> 0.72f
        DiscoveryReasonKind.City -> 0.8f
        DiscoveryReasonKind.Country -> 0.82f
    }

    fun estimate(candidate: DiscoveryCandidate, profile: DiscoveryTasteProfile): Float {
        val track = candidate.track
        var distance = base(candidate.reason.kind)
        if (candidate.reason.kind == DiscoveryReasonKind.Scene && sharesTaste(track, profile)) distance -= 0.18f
        val affinity = profile.affinityFor(track.artist)
        if (profile.followsAny(track.artist)) return 0.04f
        if (affinity > 0) distance = minOf(distance, FAMILIAR_CEILING - 0.12f * (affinity.coerceAtMost(80) / 80f))
        if (profile.nearSimilar(track.artist)) distance = minOf(distance, SIMILAR_CEILING)
        return distance.coerceIn(0f, 1f)
    }

    fun admits(request: DiscoveryRequest, distance: Float, candidate: DiscoveryCandidate, profile: DiscoveryTasteProfile, slack: Float): Boolean {
        val artist = candidate.track.artist
        when (request.level) {
            BubbleLevel.Near -> Unit
            BubbleLevel.Adventurous -> if (profile.followsAny(artist) || profile.affinityFor(artist) >= 20) return false
            BubbleLevel.OutOfOrbit -> if (profile.followsAny(artist) || profile.affinityFor(artist) > 0 || profile.nearSimilar(artist)) return false
        }
        if (!request.usesDistanceBand || request.level == BubbleLevel.Near) return true
        return distance >= request.level.minDistance - slack && distance <= request.level.maxDistance + slack
    }

    fun bandScore(level: BubbleLevel, distance: Float): Int =
        (30f * (1f - abs(distance - level.sweetSpot) / 0.5f)).toInt().coerceIn(-30, 30)

    private fun sharesTaste(track: Track, profile: DiscoveryTasteProfile): Boolean {
        if (profile.tasteTags.isEmpty()) return false
        val words = "${track.album} ${track.moodTags.joinToString(" ")}".lowercase().split(' ', ',', '/', '&')
        return words.any { it.length >= 3 && it in profile.tasteTags }
    }
}

object BubbleRegions {
    private val languages: Map<String, String> = mapOf(
        "it" to "it", "us" to "en", "gb" to "en", "es" to "es", "fr" to "fr", "de" to "de", "pt" to "pt", "br" to "pt",
        "mx" to "es", "nl" to "nl", "pl" to "pl", "ro" to "ro", "gr" to "el", "se" to "sv", "dk" to "da", "cz" to "cs",
        "ua" to "uk", "ru" to "ru", "tr" to "tr", "sa" to "ar", "cn" to "zh", "jp" to "ja", "kr" to "ko", "in" to "hi",
        "id" to "id", "vn" to "vi", "th" to "th", "ph" to "fil", "il" to "he", "tw" to "zh", "hu" to "hu", "bg" to "bg",
        "fi" to "fi", "no" to "nb", "hr" to "hr", "sk" to "sk", "my" to "ms"
    )

    fun languageOf(regionId: String): String = languages[regionId.lowercase()].orEmpty()

    fun distant(regions: List<Pair<String, String>>, market: String, languageCode: String): List<Pair<String, String>> {
        val ownLanguage = languageCode.substringBefore('-').lowercase()
        val marketLanguage = languageOf(market)
        return regions.filter { (id, _) ->
            val language = languageOf(id)
            id != market.lowercase() && language.isNotEmpty() && language != ownLanguage && language != marketLanguage
        }
    }
}
