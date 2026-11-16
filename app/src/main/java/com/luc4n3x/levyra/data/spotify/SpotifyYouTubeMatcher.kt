package com.luc4n3x.levyra.data.spotify

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import com.luc4n3x.levyra.nexus.identity.TitleIdentity
import com.luc4n3x.levyra.nexus.identity.TrackVersionMarker
import com.luc4n3x.levyra.nexus.playlistimport.ImportTransliteration
import java.util.Locale
import kotlin.math.abs

internal data class SpotifyYouTubeMatch(
    val candidate: Track,
    val confidence: Double
)

private data class MatchEvidence(
    val total: Double,
    val title: Double,
    val artist: Double,
    val durationDeltaMs: Long?
)

internal object SpotifyYouTubeMatcher {
    const val DEFAULT_CONFIDENCE_THRESHOLD = 0.94
    private const val TITLE_WEIGHT = 0.45
    private const val ARTIST_WEIGHT = 0.35
    private const val DURATION_WEIGHT = 0.20
    private const val SAME_WORK_TITLE_THRESHOLD = 0.85
    private const val SAME_WORK_ARTIST_HINT = 0.6
    private const val SAME_WORK_MIN_DRIFT_MS = 15_000L
    private const val SAME_WORK_DRIFT_PERCENT = 8L
    private const val SAME_RECORDING_ARTIST_THRESHOLD = 0.9
    private const val SAME_RECORDING_TITLE_THRESHOLD = 0.6
    private const val SAME_RECORDING_MAX_DRIFT_MS = 3_000L
    private const val ROMANIZED_ARTIST_THRESHOLD = 0.85
    private val workPrefix = Regex("^([^:\\d]{2,40}):\\s+(.*\\p{L}.*)$")
    private val catalogNumberSpacing = Regex("\\b(\\p{L}{1,4}) (?=\\d)")
    private val workTitleSignature = Regex(
        "\\b(?:op|opus|bwv|kv?|d|s|h|hob|trv|av|woo|rv|hwv|sz|bb|mwv|wwv)\\.?\\s?\\d|:\\s*\\S",
        RegexOption.IGNORE_CASE
    )
    private val versionDescriptor = Regex("[(\\[{]([^)\\]}]*)[)\\]}]|\\s[-–—]\\s(.+)$")

    private fun versionLabels(title: String): Set<String> = versionDescriptor.findAll(title)
        .map { it.groupValues[1].ifBlank { it.groupValues[2] } }
        .filter { MusicIdentityText.title("Track ($it)").markers.isNotEmpty() }
        .map(MusicIdentityText::normalize)
        .toSet()

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
            val winsTie = score == bestScore && score > 0.0 &&
                isAudioTrack(candidate) && bestCandidate?.let(::isAudioTrack) == false
            if (score > bestScore || winsTie) {
                bestScore = score
                bestCandidate = candidate
            }
        }

        val match = bestCandidate ?: return null
        return if (bestScore >= threshold) {
            SpotifyYouTubeMatch(match, bestScore)
        } else {
            null
        }
    }

    fun findPlayableMatch(catalogTrack: Track, candidates: List<Track>): SpotifyYouTubeMatch? {
        findBestMatch(catalogTrack, candidates)?.let { return it }
        return candidates
            .mapNotNull { candidate ->
                evidence(catalogTrack, candidate)
                    ?.takeIf { isSameWork(catalogTrack, it) }
                    ?.let { candidate to it }
            }
            .sortedWith(
                compareByDescending<Pair<Track, MatchEvidence>> { it.second.artist >= SAME_WORK_ARTIST_HINT }
                    .thenBy { it.second.durationDeltaMs ?: Long.MAX_VALUE }
                    .thenByDescending { it.second.total }
            )
            .firstOrNull()
            ?.let { (candidate, evidence) -> SpotifyYouTubeMatch(candidate, evidence.total) }
    }

    private fun isSameWork(catalogTrack: Track, evidence: MatchEvidence): Boolean {
        if (isSameRecording(evidence)) return true
        if (evidence.title < SAME_WORK_TITLE_THRESHOLD) return false
        if (evidence.artist < SAME_WORK_ARTIST_HINT && !workTitleSignature.containsMatchIn(catalogTrack.title)) return false
        val delta = evidence.durationDeltaMs ?: return evidence.artist >= SAME_WORK_ARTIST_HINT
        return delta <= maxOf(SAME_WORK_MIN_DRIFT_MS, catalogTrack.durationMs * SAME_WORK_DRIFT_PERCENT / 100L)
    }

    private fun isSameRecording(evidence: MatchEvidence): Boolean {
        val delta = evidence.durationDeltaMs ?: return false
        return evidence.artist >= SAME_RECORDING_ARTIST_THRESHOLD &&
            delta <= SAME_RECORDING_MAX_DRIFT_MS &&
            evidence.title >= SAME_RECORDING_TITLE_THRESHOLD
    }

    private fun isAudioTrack(track: Track): Boolean = track.videoType.contains("ATV", ignoreCase = true)

    fun scoreMatch(spotifyTrack: Track, ytCandidate: Track): Double = evidence(spotifyTrack, ytCandidate)?.total ?: 0.0

    private fun evidence(spotifyTrack: Track, ytCandidate: Track): MatchEvidence? {
        val sTitleId = MusicIdentityText.title(spotifyTrack.title)
        val yTitleId = MusicIdentityText.title(ytCandidate.title)
        if (sTitleId.markers != yTitleId.markers) return null
        if (hasVersionConflict(spotifyTrack, ytCandidate, sTitleId, yTitleId)) return null

        val sArtistCredit = MusicIdentityText.artistCredit(spotifyTrack.artist)
        val yArtistCredit = MusicIdentityText.artistCredit(ytCandidate.artist)
        val artistScore = computeArtistSimilarity(sArtistCredit.primary, sArtistCredit.names, yArtistCredit.primary, yArtistCredit.names, ytCandidate.title)
        val durationDeltaMs = if (spotifyTrack.durationMs > 0L && ytCandidate.durationMs > 0L) {
            abs(spotifyTrack.durationMs - ytCandidate.durationMs)
        } else null
        val sIsrc = spotifyTrack.isrc.trim().uppercase(Locale.ROOT)
        val yIsrc = ytCandidate.isrc.trim().uppercase(Locale.ROOT)
        if (sIsrc.isNotEmpty() && yIsrc.isNotEmpty()) return isrcEvidence(sIsrc, yIsrc, artistScore, durationDeltaMs)

        val titleScore = titleSimilarity(spotifyTrack.title, ytCandidate.title)
        val durationScore = computeDurationScore(spotifyTrack.durationMs, ytCandidate.durationMs)
        val weighted = (titleScore * TITLE_WEIGHT) + (artistScore * ARTIST_WEIGHT) + (durationScore * DURATION_WEIGHT)
        val sharedSpecialMarkers = yTitleId.markers.any { it in sTitleId.markers && it in UNREQUESTED_PENALTY_MARKERS }
        val total = adjustedTotal(weighted, artistScore, durationDeltaMs, sharedSpecialMarkers, isAudioTrack(ytCandidate))
        return MatchEvidence(total, titleScore, artistScore, durationDeltaMs)
    }

    private fun hasVersionConflict(
        spotifyTrack: Track,
        ytCandidate: Track,
        sTitleId: TitleIdentity,
        yTitleId: TitleIdentity
    ): Boolean {
        val sVersionLabels = sTitleId.versionLabels + versionLabels(spotifyTrack.title)
        val yVersionLabels = yTitleId.versionLabels + versionLabels(ytCandidate.title)
        if (sVersionLabels.isNotEmpty() && yVersionLabels.isNotEmpty() && sVersionLabels != yVersionLabels) return true
        val sExplicitHint = sTitleId.explicitHint ?: true.takeIf { spotifyTrack.explicit }
        val yExplicitHint = yTitleId.explicitHint ?: true.takeIf { ytCandidate.explicit }
        return sExplicitHint != null && yExplicitHint != null && sExplicitHint != yExplicitHint
    }

    private fun isrcEvidence(sIsrc: String, yIsrc: String, artistScore: Double, durationDeltaMs: Long?): MatchEvidence? {
        if (sIsrc != yIsrc || artistScore < 0.35) return null
        if (durationDeltaMs != null && durationDeltaMs > 30_000L) return null
        return MatchEvidence(1.0, 1.0, artistScore, durationDeltaMs)
    }

    private fun adjustedTotal(
        weighted: Double,
        artistScore: Double,
        durationDeltaMs: Long?,
        sharedSpecialMarkers: Boolean,
        audioTrack: Boolean
    ): Double {
        // Homonym protection: if artists do not match at all, cap total score so different artists never match
        var total = if (artistScore < 0.35) (weighted * 0.50).coerceAtMost(0.40) else weighted
        // Duration penalty for large discrepancies (>30s or >60s)
        total -= when {
            durationDeltaMs == null -> 0.0
            durationDeltaMs > 60_000L -> 0.30
            durationDeltaMs > 30_000L -> 0.15
            else -> 0.0
        }
        if (sharedSpecialMarkers) total += 0.10
        if (audioTrack) total += 0.05
        return total.coerceIn(0.0, 1.0)
    }

    private fun titleSimilarity(sourceTitle: String, candidateTitle: String): Double {
        val direct = compactTitleSimilarity(sourceTitle, candidateTitle)
        val source = withoutWorkPrefix(sourceTitle, candidateTitle)
        val candidate = withoutWorkPrefix(candidateTitle, sourceTitle)
        if (listOfNotNull(source, candidate).size != 1) return direct
        return maxOf(direct, compactTitleSimilarity(source ?: sourceTitle, candidate ?: candidateTitle))
    }

    private fun compactTitleSimilarity(sourceTitle: String, candidateTitle: String): Double {
        val source = MusicIdentityText.title(sourceTitle)
        val candidate = MusicIdentityText.title(candidateTitle)
        return computeTitleSimilarity(
            compactCatalogNumbers(source.core),
            compactCatalogNumbers(source.fullNormalized),
            compactCatalogNumbers(candidate.core),
            compactCatalogNumbers(candidate.fullNormalized)
        )
    }

    private fun withoutWorkPrefix(title: String, otherTitle: String): String? {
        val match = workPrefix.find(title.trim()) ?: return null
        val prefix = MusicIdentityText.normalize(match.groupValues[1])
        if (prefix.isBlank() || prefix.split(' ').size > 4) return null
        if (" ${MusicIdentityText.normalize(otherTitle)} ".contains(" $prefix ")) return null
        return match.groupValues[2]
    }

    private fun compactCatalogNumbers(text: String): String = text.replace(catalogNumberSpacing, "$1")

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
        if (containsArtistCredit(normalizedYtTitle, sPrimary)) return 0.95

        for (name in sNames) {
            if (name in yNames || containsArtistCredit(normalizedYtTitle, name)) return 0.90
        }

        if (romanizedArtistSimilarity(sNames, yNames) >= ROMANIZED_ARTIST_THRESHOLD) return 0.90

        val sJoined = sNames.joinToString(" ")
        val yJoined = yNames.joinToString(" ")
        return diceCoefficient(sJoined, yJoined)
    }

    private fun romanizedArtistSimilarity(sNames: Set<String>, yNames: Set<String>): Double {
        val sCyrillic = sNames.any(ImportTransliteration::containsCyrillic)
        val yCyrillic = yNames.any(ImportTransliteration::containsCyrillic)
        if (sCyrillic == yCyrillic) return 0.0
        val yKeys = yNames.map(ImportTransliteration::romanizedKey)
        return sNames.maxOfOrNull { name ->
            val key = ImportTransliteration.romanizedKey(name)
            yKeys.maxOfOrNull { diceCoefficient(key, it) } ?: 0.0
        } ?: 0.0
    }

    private fun containsArtistCredit(text: String, artist: String): Boolean {
        if (text.isBlank() || artist.isBlank()) return false
        val textTokens = text.split(' ').filter(String::isNotBlank)
        val artistTokens = artist.split(' ').filter(String::isNotBlank)
        if (artistTokens.isEmpty() || artistTokens.size > textTokens.size) return false
        return textTokens.windowed(artistTokens.size).any { it == artistTokens }
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

        return 2.0 * matches / (bigrams1.size + bigrams2.size)
    }
}
