package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class AlternativeTrackQuery(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val explicit: Boolean?,
    val isrc: String = ""
)

data class AlternativeTrackCandidate(
    val providerId: String,
    val providerTrackId: String,
    val title: String,
    val primaryArtists: List<String>,
    val featuredArtists: List<String>,
    val album: String,
    val durationSeconds: Int,
    val explicit: Boolean?,
    val isrc: String = "",
    val offers320: Boolean = false,
    val mediaToken: String = ""
)

enum class MatchRejection {
    NO_CANDIDATES,
    INSUFFICIENT_IDENTITY,
    DURATION_UNKNOWN,
    ISRC_MISMATCH,
    TITLE_MISMATCH,
    VERSION_MISMATCH,
    PRIMARY_ARTIST_MISMATCH,
    FEATURED_ARTIST_ONLY,
    EXPLICIT_MISMATCH,
    ALBUM_MISMATCH,
    DURATION_OUT_OF_RANGE,
    LOW_CONFIDENCE,
    AMBIGUOUS
}

enum class AlbumRelation {
    SAME,
    EDITION_VARIANT,
    SINGLE_RELEASE,
    UNVERIFIED,
    REMASTER_CONFLICT,
    MISMATCH
}

data class AlternativeMatchEvaluation(
    val candidate: AlternativeTrackCandidate,
    val verdict: AlternativeMatchVerdict,
    val confidence: Int,
    val rejection: MatchRejection?,
    val durationDeltaSeconds: Int,
    val albumRelation: AlbumRelation?,
    val versionSignature: Set<String>
) {
    val accepted: Boolean
        get() = verdict != AlternativeMatchVerdict.REJECTED
}

sealed interface AlternativeMatchSelection {
    val evaluations: List<AlternativeMatchEvaluation>

    data class Accepted(
        val evaluation: AlternativeMatchEvaluation,
        override val evaluations: List<AlternativeMatchEvaluation>
    ) : AlternativeMatchSelection

    data class Rejected(
        val reason: MatchRejection,
        override val evaluations: List<AlternativeMatchEvaluation>
    ) : AlternativeMatchSelection
}

class AlternativeTrackMatcher {
    fun evaluate(query: AlternativeTrackQuery, candidate: AlternativeTrackCandidate): AlternativeMatchEvaluation {
        val expectedTitle = AlternativeTrackText.title(query.title)
        val candidateTitle = AlternativeTrackText.title(candidate.title)
        val expectedCredit = AlternativeTrackText.artistCredit(query.artist)
        val expectedArtists = expectedCredit.names + expectedTitle.featuredArtists
        val candidatePrimary = candidate.primaryArtists.flatMap(AlternativeTrackText::artistNames).distinct()
        val candidateFeatured = candidate.featuredArtists.flatMap(AlternativeTrackText::artistNames).toSet() +
            candidateTitle.featuredArtists
        val delta = durationDeltaSeconds(query.durationMs, candidate.durationSeconds)

        fun rejected(reason: MatchRejection, relation: AlbumRelation? = null) = AlternativeMatchEvaluation(
            candidate = candidate,
            verdict = AlternativeMatchVerdict.REJECTED,
            confidence = 0,
            rejection = reason,
            durationDeltaSeconds = delta,
            albumRelation = relation,
            versionSignature = candidateTitle.versionSignature
        )

        if (expectedTitle.core.isBlank() || expectedCredit.primary.isBlank()) {
            return rejected(MatchRejection.INSUFFICIENT_IDENTITY)
        }
        if (delta < 0) return rejected(MatchRejection.DURATION_UNKNOWN)
        val expectedIsrc = normalizedIsrc(query.isrc)
        val candidateIsrc = normalizedIsrc(candidate.isrc)
        if (expectedIsrc.isNotEmpty() && candidateIsrc.isNotEmpty() && expectedIsrc != candidateIsrc) {
            return rejected(MatchRejection.ISRC_MISMATCH)
        }
        if (expectedTitle.core != candidateTitle.core) return rejected(MatchRejection.TITLE_MISMATCH)
        if (expectedTitle.versionSignature != candidateTitle.versionSignature) {
            return rejected(MatchRejection.VERSION_MISMATCH)
        }
        if (expectedCredit.primary !in candidatePrimary) {
            val featuredOnly = expectedCredit.primary in candidateFeatured
            return rejected(if (featuredOnly) MatchRejection.FEATURED_ARTIST_ONLY else MatchRejection.PRIMARY_ARTIST_MISMATCH)
        }
        if (candidatePrimary.first() !in expectedArtists) return rejected(MatchRejection.PRIMARY_ARTIST_MISMATCH)
        val expectedExplicit = query.explicit ?: expectedTitle.explicitHint
        val candidateExplicit = candidate.explicit ?: candidateTitle.explicitHint
        if (expectedExplicit != null && candidateExplicit != null && expectedExplicit != candidateExplicit) {
            return rejected(MatchRejection.EXPLICIT_MISMATCH)
        }
        val relation = albumRelation(query.album, candidate.album, expectedTitle, candidateTitle)
        if (relation == AlbumRelation.REMASTER_CONFLICT || relation == AlbumRelation.MISMATCH) {
            return rejected(MatchRejection.ALBUM_MISMATCH, relation)
        }
        if (delta > MAXIMUM_DURATION_DELTA_SECONDS) return rejected(MatchRejection.DURATION_OUT_OF_RANGE, relation)
        val artistsExact = expectedArtists.containsAll(candidatePrimary) &&
            (candidatePrimary + candidateFeatured).containsAll(expectedCredit.names)
        if (delta > EXCELLENT_DURATION_DELTA_SECONDS && !(relation == AlbumRelation.SAME && artistsExact)) {
            return rejected(MatchRejection.DURATION_OUT_OF_RANGE, relation)
        }
        val isrcConfirmed = expectedIsrc.isNotEmpty() && expectedIsrc == candidateIsrc
        val titleExact = expectedTitle.fullNormalized == candidateTitle.fullNormalized
        val confidence = confidence(titleExact, artistsExact, relation, delta, isrcConfirmed)
        val verdict = when {
            titleExact && artistsExact && relation == AlbumRelation.SAME && delta <= EXCELLENT_DURATION_DELTA_SECONDS ->
                AlternativeMatchVerdict.EXACT
            confidence >= MINIMUM_ACCEPTED_CONFIDENCE -> AlternativeMatchVerdict.HIGH
            else -> return rejected(MatchRejection.LOW_CONFIDENCE, relation)
        }
        return AlternativeMatchEvaluation(
            candidate = candidate,
            verdict = verdict,
            confidence = confidence,
            rejection = null,
            durationDeltaSeconds = delta,
            albumRelation = relation,
            versionSignature = candidateTitle.versionSignature
        )
    }

    fun select(query: AlternativeTrackQuery, candidates: List<AlternativeTrackCandidate>): AlternativeMatchSelection {
        val evaluations = candidates
            .distinctBy { it.providerId to it.providerTrackId }
            .map { evaluate(query, it) }
        val accepted = evaluations
            .filter { it.accepted }
            .sortedWith(
                compareBy<AlternativeMatchEvaluation> { it.verdict.ordinal }
                    .thenByDescending { it.confidence }
                    .thenByDescending { it.candidate.offers320 }
            )
        val top = accepted.firstOrNull()
            ?: return AlternativeMatchSelection.Rejected(dominantRejection(evaluations), evaluations)
        val contender = accepted.firstOrNull { it.verdict == top.verdict && !sameRecording(it, top) }
        if (contender != null && top.confidence - contender.confidence < AMBIGUITY_MARGIN) {
            return AlternativeMatchSelection.Rejected(MatchRejection.AMBIGUOUS, evaluations)
        }
        val chosen = accepted
            .filter { it.verdict == top.verdict && sameRecording(it, top) }
            .maxWith(compareBy<AlternativeMatchEvaluation> { it.candidate.offers320 }.thenBy { it.confidence })
        return AlternativeMatchSelection.Accepted(chosen, evaluations)
    }

    private fun sameRecording(left: AlternativeMatchEvaluation, right: AlternativeMatchEvaluation): Boolean {
        val a = left.candidate
        val b = right.candidate
        if (a.providerId == b.providerId && a.providerTrackId == b.providerTrackId) return true
        return AlternativeTrackText.title(a.title).fullNormalized == AlternativeTrackText.title(b.title).fullNormalized &&
            primarySet(a) == primarySet(b) &&
            abs(a.durationSeconds - b.durationSeconds) <= 1 &&
            a.explicit == b.explicit
    }

    private fun primarySet(candidate: AlternativeTrackCandidate): Set<String> =
        candidate.primaryArtists.flatMap(AlternativeTrackText::artistNames).toSet()

    private fun dominantRejection(evaluations: List<AlternativeMatchEvaluation>): MatchRejection {
        val counts = evaluations.mapNotNull { it.rejection }.groupingBy { it }.eachCount()
        if (counts.isEmpty()) return MatchRejection.NO_CANDIDATES
        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<MatchRejection, Int>> { it.value }.thenBy { it.key.ordinal })
            .first()
            .key
    }

    private fun albumRelation(
        expectedRaw: String,
        candidateRaw: String,
        expectedTitle: TitleIdentity,
        candidateTitle: TitleIdentity
    ): AlbumRelation {
        val expected = AlternativeTrackText.album(expectedRaw)
        if (expected.isBlank || expected.core in untrustedAlbumNames) return AlbumRelation.UNVERIFIED
        val candidate = AlternativeTrackText.album(candidateRaw)
        if (candidate.isBlank) return AlbumRelation.UNVERIFIED
        if (expected.core == candidate.core) {
            val expectedRemaster = AlbumEdition.REMASTERED in expected.editions
            val candidateRemaster = AlbumEdition.REMASTERED in candidate.editions
            if (expectedRemaster != candidateRemaster) return AlbumRelation.REMASTER_CONFLICT
            return if (expected.editions == candidate.editions) AlbumRelation.SAME else AlbumRelation.EDITION_VARIANT
        }
        val expectedSingle = expected.core == expectedTitle.core || expected.editions.any(singleEditions::contains)
        val candidateSingle = candidate.core == candidateTitle.core || candidate.editions.any(singleEditions::contains)
        return if (expectedSingle || candidateSingle) AlbumRelation.SINGLE_RELEASE else AlbumRelation.MISMATCH
    }

    private fun confidence(
        titleExact: Boolean,
        artistsExact: Boolean,
        relation: AlbumRelation,
        delta: Int,
        isrcConfirmed: Boolean
    ): Int {
        if (isrcConfirmed) return 100
        val title = if (titleExact) 35 else 31
        val artists = if (artistsExact) 25 else 18
        val album = when (relation) {
            AlbumRelation.SAME -> 20
            AlbumRelation.EDITION_VARIANT -> 15
            AlbumRelation.SINGLE_RELEASE -> 13
            else -> 11
        }
        val duration = when (delta) {
            0, 1 -> 20
            2 -> 17
            3 -> 12
            4 -> 9
            else -> 6
        }
        return (title + artists + album + duration).coerceAtMost(100)
    }

    private fun durationDeltaSeconds(expectedMs: Long, candidateSeconds: Int): Int {
        if (expectedMs <= 0L || candidateSeconds <= 0) return -1
        return abs(expectedMs / 1000.0 - candidateSeconds).roundToInt()
    }

    private fun normalizedIsrc(value: String): String =
        value.uppercase(Locale.ROOT).filter { it in 'A'..'Z' || it in '0'..'9' }

    companion object {
        const val EXCELLENT_DURATION_DELTA_SECONDS = 2
        const val MAXIMUM_DURATION_DELTA_SECONDS = 5
        const val MINIMUM_ACCEPTED_CONFIDENCE = 75
        const val PERSISTABLE_CONFIDENCE = 85
        const val AMBIGUITY_MARGIN = 8

        private val singleEditions = setOf(AlbumEdition.SINGLE, AlbumEdition.EP)
        private val untrustedAlbumNames = setOf(
            "levyra",
            "youtube",
            "youtube music",
            "unknown",
            "unknown album",
            "single",
            "singles"
        )
    }
}
