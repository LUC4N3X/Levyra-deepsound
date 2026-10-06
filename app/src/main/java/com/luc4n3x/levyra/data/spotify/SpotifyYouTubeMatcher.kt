package com.luc4n3x.levyra.data.spotify

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import com.luc4n3x.levyra.nexus.identity.TrackVersionMarker
import java.util.Locale
import kotlin.math.abs

internal data class SpotifyYouTubeMatch(
    val candidate: Track,
    val confidence: Double
)

internal object SpotifyYouTubeMatcher {
    const val DEFAULT_CONFIDENCE_THRESHOLD = 0.58
    private const val TITLE_WEIGHT = 0.45
    private const val ARTIST_WEIGHT = 0.35
    private const val DURATION_WEIGHT = 0.20

    private val UNREQUESTED_PENALTY_MARKERS = setOf(
        TrackVersionMarker.LIVE,
        TrackVersionMarker.REMIX,
        TrackVersionMarker.KARAOKE,
        TrackVersionMarker.SPED_UP,
        TrackVersionMarker.SLOWED,
        TrackVersionMarker.ACOUSTIC,
        TrackVersionMarker.INSTRUMENTAL,
        TrackVersionMarker.COVER
    )

    fun findBestMatch(
        spotifyTrack: Track,
        candidates: List<Track>,
        threshold: Double = DEFAULT_CONFIDENCE_THRESHOLD
    ): SpotifyYouTubeMatch? {
        if (candidates.isEmpty()) return null

        var bestCandidate: Track? = null
        var bestScore = 0.0

        for (candidate in candidates) {
            val score = scoreMatch(spotifyTrack, candidate)
            if (score > bestScore) {
                bestScore = score
                bestCandidate = candidate
                if (score >= 0.96) break
            }
        }

        val match = bestCandidate ?: return null
        return if (bestScore >= threshold) {
            SpotifyYouTubeMatch(match, bestScore)
        } else {
            null
        }
    }

    fun scoreMatch(spotifyTrack: Track, ytCandidate: Track): Double {
        val sIsrc = spotifyTrack.isrc.trim().uppercase(Locale.ROOT)
        val yIsrc = ytCandidate.isrc.trim().uppercase(Locale.ROOT)
        if (sIsrc.isNotEmpty() && yIsrc.isNotEmpty()) {
            if (sIsrc == yIsrc) return 1.0
            return 0.0
        }

        val sTitleId = MusicIdentityText.title(spotifyTrack.title)
        val yTitleId = MusicIdentityText.title(ytCandidate.title)

        val sArtistCredit = MusicIdentityText.artistCredit(spotifyTrack.artist)
        val yArtistCredit = MusicIdentityText.artistCredit(ytCandidate.artist)

        val titleScore = computeTitleSimilarity(sTitleId.core, sTitleId.fullNormalized, yTitleId.core, yTitleId.fullNormalized)
        val artistScore = computeArtistSimilarity(sArtistCredit.primary, sArtistCredit.names, yArtistCredit.primary, yArtistCredit.names, ytCandidate.title)
        val durationScore = computeDurationScore(spotifyTrack.durationMs, ytCandidate.durationMs)

        var total = (titleScore * TITLE_WEIGHT) + (artistScore * ARTIST_WEIGHT) + (durationScore * DURATION_WEIGHT)

        // Homonym protection: if artists do not match at all, cap total score so different artists never match
        if (artistScore < 0.35) {
            total = (total * 0.50).coerceAtMost(0.40)
        }

        // Duration penalty for large discrepancies (>30s or >60s)
        if (spotifyTrack.durationMs > 0L && ytCandidate.durationMs > 0L) {
            val deltaMs = abs(spotifyTrack.durationMs - ytCandidate.durationMs)
            if (deltaMs > 60_000L) {
                total -= 0.30
            } else if (deltaMs > 30_000L) {
                total -= 0.15
            }
        }

        val unrequestedMarkers = yTitleId.markers.filter { it in UNREQUESTED_PENALTY_MARKERS && it !in sTitleId.markers }
        if (unrequestedMarkers.isNotEmpty()) {
            total -= (0.35 * unrequestedMarkers.size).coerceAtMost(0.60)
        }

        val sharedSpecialMarkers = yTitleId.markers.filter { it in sTitleId.markers && it in UNREQUESTED_PENALTY_MARKERS }
        if (sharedSpecialMarkers.isNotEmpty()) {
            total += 0.10
        }

        if (ytCandidate.videoType.contains("ATV", ignoreCase = true)) {
            total += 0.05
        }

        return total.coerceIn(0.0, 1.0)
    }

    private fun computeTitleSimilarity(
        sCore: String,
        sFull: String,
        yCore: String,
        yFull: String
    ): Double {
        if (sCore.isNotEmpty() && sCore == yCore) return 1.0
        if (sFull.isNotEmpty() && sFull == yFull) return 0.98

        val coreDice = diceCoefficient(sCore, yCore)
        val fullDice = diceCoefficient(sFull, yFull)

        val maxDice = maxOf(coreDice, fullDice)
        if (sCore.isNotEmpty() && yCore.isNotEmpty() && (sCore.contains(yCore) || yCore.contains(sCore))) {
            return maxOf(maxDice, 0.85)
        }
        return maxDice
    }

    private fun computeArtistSimilarity(
        sPrimary: String,
        sNames: Set<String>,
        yPrimary: String,
        yNames: Set<String>,
        ytTitle: String
    ): Double {
        if (sPrimary.isNotEmpty() && (sPrimary == yPrimary || sPrimary in yNames)) return 1.0

        val normalizedYtTitle = MusicIdentityText.normalize(ytTitle)
        if (sPrimary.isNotEmpty() && normalizedYtTitle.contains(sPrimary)) return 0.95

        for (name in sNames) {
            if (name in yNames || normalizedYtTitle.contains(name)) return 0.90
        }

        val sJoined = sNames.joinToString(" ")
        val yJoined = yNames.joinToString(" ")
        return diceCoefficient(sJoined, yJoined)
    }

    private fun computeDurationScore(sDurationMs: Long, yDurationMs: Long): Double {
        if (sDurationMs <= 0L || yDurationMs <= 0L) return 0.60
        val deltaMs = abs(sDurationMs - yDurationMs)
        return when {
            deltaMs <= 2_500L -> 1.0
            deltaMs <= 5_000L -> 0.90
            deltaMs <= 10_000L -> 0.70
            deltaMs <= 20_000L -> 0.40
            deltaMs <= 40_000L -> 0.15
            else -> 0.0
        }
    }

    fun diceCoefficient(a: String, b: String): Double {
        val s1 = a.trim().lowercase(Locale.ROOT)
        val s2 = b.trim().lowercase(Locale.ROOT)
        if (s1 == s2) return 1.0
        if (s1.length < 2 || s2.length < 2) return 0.0

        val bigrams1 = (0 until s1.length - 1).map { s1.substring(it, it + 2) }
        val bigrams2 = (0 until s2.length - 1).map { s2.substring(it, it + 2) }

        val freq2 = HashMap<String, Int>()
        for (bg in bigrams2) {
            freq2[bg] = (freq2[bg] ?: 0) + 1
        }

        var matches = 0
        for (bg in bigrams1) {
            val count = freq2[bg] ?: 0
            if (count > 0) {
                matches++
                freq2[bg] = count - 1
            }
        }

        return (2.0 * matches) / (bigrams1.size + bigrams2.size)
    }
}
