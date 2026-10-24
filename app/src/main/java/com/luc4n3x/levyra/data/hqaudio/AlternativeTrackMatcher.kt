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
    val isrc: String = "",
    val alternateReading: AlternativeTrackQuery? = null
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
    val offers320: Boolean? = null,
    val mediaToken: String = "",
    val language: String = "",
    val nonPerformingArtists: Set<String> = emptySet(),
    val creatorArtists: Set<String> = emptySet(),
    val releaseYear: Int = 0
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
        val primary = evaluateReading(query, candidate)
        if (primary.accepted) return primary
        val alternate = query.alternateReading ?: return primary
        return evaluateReading(alternate, candidate).takeIf { it.accepted } ?: primary
    }

    private fun evaluateReading(query: AlternativeTrackQuery, candidate: AlternativeTrackCandidate): AlternativeMatchEvaluation {
        val expectedTitle = AlternativeTrackText.title(query.title)
        val candidateTitle = AlternativeTrackText.title(candidate.title)
        val expectedCredit = AlternativeTrackText.artistCredit(query.artist)
        val expectedArtists = expectedCredit.names + expectedTitle.featuredArtists
        val candidatePrimary = candidate.primaryArtists.flatMap(AlternativeTrackText::artistNames).distinct()
        val performingPrimary = performers(candidate, candidatePrimary)
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
        val romanizedTitle = expectedTitle.core != candidateTitle.core &&
            romanizedVariant(expectedTitle.core, candidateTitle.core, candidate.language)
        if (expectedTitle.core != candidateTitle.core && !romanizedTitle) return rejected(MatchRejection.TITLE_MISMATCH)
        if (expectedTitle.versionSignature != candidateTitle.versionSignature) {
            return rejected(MatchRejection.VERSION_MISMATCH)
        }
        if (expectedCredit.primary !in candidatePrimary) {
            val featuredOnly = expectedCredit.primary in candidateFeatured
            return rejected(if (featuredOnly) MatchRejection.FEATURED_ARTIST_ONLY else MatchRejection.PRIMARY_ARTIST_MISMATCH)
        }
        val ledByExpectedArtist = candidatePrimary.first() in expectedArtists || performingPrimary.first() in expectedArtists
        val ledByCreditedCreator = !ledByExpectedArtist &&
            expectedArtistLeadsAfterCreators(candidate, performingPrimary, expectedArtists)
        if (!ledByExpectedArtist && !ledByCreditedCreator) {
            return rejected(MatchRejection.PRIMARY_ARTIST_MISMATCH)
        }
        val expectedExplicit = query.explicit ?: expectedTitle.explicitHint
        val candidateExplicit = candidate.explicit ?: candidateTitle.explicitHint
        if (expectedExplicit != null && candidateExplicit != null && expectedExplicit != candidateExplicit) {
            return rejected(MatchRejection.EXPLICIT_MISMATCH)
        }
        val isrcConfirmed = expectedIsrc.isNotEmpty() && expectedIsrc == candidateIsrc
        val relation = albumRelation(query.album, candidate, expectedTitle, candidateTitle)
        if (!isrcConfirmed && (relation == AlbumRelation.REMASTER_CONFLICT || relation == AlbumRelation.MISMATCH)) {
            return rejected(MatchRejection.ALBUM_MISMATCH, relation)
        }
        if (delta > MAXIMUM_DURATION_DELTA_SECONDS) return rejected(MatchRejection.DURATION_OUT_OF_RANGE, relation)
        val recordingConfirmed = isrcConfirmed || (
            albumConfirmsRecording(query, candidate, relation, candidateTitle) &&
                delta <= EXCELLENT_DURATION_DELTA_SECONDS &&
                candidatePrimary.containsAll(expectedCredit.names)
            )
        if (romanizedTitle && !recordingConfirmed) return rejected(MatchRejection.TITLE_MISMATCH, relation)
        if (ledByCreditedCreator && !recordingConfirmed) {
            return rejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, relation)
        }
        val candidateArtists = candidatePrimary.toSet() + candidateFeatured
        if (!candidateArtists.containsAll(expectedCredit.names)) {
            return rejected(MatchRejection.PRIMARY_ARTIST_MISMATCH, relation)
        }
        val artistsExact = expectedArtists.containsAll(performingPrimary) &&
            candidateArtists.containsAll(expectedCredit.names)
        val titleExact = expectedTitle.fullNormalized == candidateTitle.fullNormalized
        val albumAllowsDrift = relation == AlbumRelation.SAME || (relation == AlbumRelation.UNVERIFIED && titleExact)
        if (delta > EXCELLENT_DURATION_DELTA_SECONDS && !(albumAllowsDrift && artistsExact)) {
            return rejected(MatchRejection.DURATION_OUT_OF_RANGE, relation)
        }
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

    fun sharesTitle(query: AlternativeTrackQuery, title: String, language: String): Boolean {
        val expected = AlternativeTrackText.title(query.title).core
        val actual = AlternativeTrackText.title(title).core
        return expected == actual || romanizedVariant(expected, actual, language)
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
                    .thenByDescending { it.candidate.offers320 == true }
            )
        val top = accepted.firstOrNull()
            ?: return AlternativeMatchSelection.Rejected(dominantRejection(evaluations), evaluations)
        val explicitKnown = (query.explicit ?: AlternativeTrackText.title(query.title).explicitHint) != null
        val nearTop = accepted.filter { it.verdict == top.verdict && top.confidence - it.confidence < AMBIGUITY_MARGIN }
        val ambiguous = nearTop.withIndex().any { (index, left) ->
            nearTop.drop(index + 1).any { right -> !sameRecording(left, right, explicitKnown) }
        }
        if (ambiguous) return AlternativeMatchSelection.Rejected(MatchRejection.AMBIGUOUS, evaluations)
        val chosen = accepted
            .filter { it.verdict == top.verdict && sameRecording(it, top, explicitKnown) }
            .maxWith(
                compareBy<AlternativeMatchEvaluation> { it.candidate.explicit != true }
                    .thenBy { it.candidate.offers320 == true }
                    .thenBy { it.confidence }
                    .thenByDescending { it.candidate.releaseYear.takeIf { year -> year > 0 } ?: Int.MAX_VALUE }
                    .thenByDescending { it.durationDeltaSeconds }
            )
        return AlternativeMatchSelection.Accepted(chosen, evaluations)
    }

    private fun sameRecording(
        left: AlternativeMatchEvaluation,
        right: AlternativeMatchEvaluation,
        explicitKnown: Boolean
    ): Boolean {
        val a = left.candidate
        val b = right.candidate
        if (a.providerId == b.providerId && a.providerTrackId == b.providerTrackId) return true
        val aTitle = AlternativeTrackText.title(a.title)
        val bTitle = AlternativeTrackText.title(b.title)
        val sameTitle = aTitle.core == bTitle.core || (
            isIndianRelease(a.language) && isIndianRelease(b.language) && romanizationKey(aTitle.core) == romanizationKey(bTitle.core)
            )
        return sameTitle &&
            aTitle.versionSignature == bTitle.versionSignature &&
            sameFeaturedCredits(featuredSet(a, aTitle), featuredSet(b, bTitle)) &&
            samePrimaryCredits(a, b) &&
            abs(a.durationSeconds - b.durationSeconds) <= SAME_RECORDING_DURATION_SECONDS &&
            (!explicitKnown || a.explicit == b.explicit) &&
            (a.language.isBlank() || b.language.isBlank() || a.language == b.language)
    }

    private fun featuredSet(candidate: AlternativeTrackCandidate, title: TitleIdentity): Set<String> =
        candidate.featuredArtists.flatMap(AlternativeTrackText::artistNames).toSet() + title.featuredArtists

    private fun sameFeaturedCredits(left: Set<String>, right: Set<String>): Boolean =
        left.isEmpty() || right.isEmpty() || left == right

    private fun samePrimaryCredits(a: AlternativeTrackCandidate, b: AlternativeTrackCandidate): Boolean {
        val left = primarySet(a)
        val right = primarySet(b)
        if (left.isEmpty() || right.isEmpty()) return false
        return left == right ||
            (left.containsAll(right) && creators(a).containsAll(left - right)) ||
            (right.containsAll(left) && creators(b).containsAll(right - left))
    }

    private fun expectedArtistLeadsAfterCreators(
        candidate: AlternativeTrackCandidate,
        performingPrimary: List<String>,
        expectedArtists: Collection<String>
    ): Boolean {
        val creators = creators(candidate)
        val afterCreators = performingPrimary.dropWhile { it in creators && it !in expectedArtists }
        if (afterCreators.size == performingPrimary.size) return false
        return afterCreators.firstOrNull()?.let { it in expectedArtists } == true
    }

    private fun isIndianRelease(language: String): Boolean =
        language.lowercase(Locale.ROOT) in indianReleaseLanguages

    private fun romanizationKey(core: String): String = core.replace(lengthenedVowel, "$1")

    private fun romanizedVariant(expectedCore: String, candidateCore: String, language: String): Boolean =
        isIndianRelease(language) && romanizationKey(expectedCore) == romanizationKey(candidateCore)

    private fun albumConfirmsRecording(
        query: AlternativeTrackQuery,
        candidate: AlternativeTrackCandidate,
        relation: AlbumRelation,
        candidateTitle: TitleIdentity
    ): Boolean = when (relation) {
        AlbumRelation.SAME, AlbumRelation.EDITION_VARIANT -> true
        AlbumRelation.SINGLE_RELEASE -> {
            val album = AlternativeTrackText.album(candidate.album)
            album.core == candidateTitle.core ||
                album.editions.any(singleEditions::contains) ||
                album.core in releaseSources(query)
        }
        else -> false
    }

    private fun releaseSources(query: AlternativeTrackQuery): Set<String> =
        listOf(query.title, query.album)
            .flatMap { text -> releaseSource.findAll(AlternativeTrackText.decodeHtmlEntities(text)).map { it.groupValues[1] } }
            .map { AlternativeTrackText.album(it).core }
            .filter { it.isNotBlank() }
            .toSet()

    private fun creators(candidate: AlternativeTrackCandidate): Set<String> =
        candidate.creatorArtists.flatMap(AlternativeTrackText::artistNames).toSet()

    private fun primarySet(candidate: AlternativeTrackCandidate): Set<String> =
        performers(candidate, candidate.primaryArtists.flatMap(AlternativeTrackText::artistNames).distinct()).toSet()

    private fun performers(candidate: AlternativeTrackCandidate, primary: List<String>): List<String> {
        if (candidate.nonPerformingArtists.isEmpty()) return primary
        val nonPerforming = candidate.nonPerformingArtists.flatMap(AlternativeTrackText::artistNames).toSet()
        return primary.filterNot { it in nonPerforming }.ifEmpty { primary }
    }

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
        candidateTrack: AlternativeTrackCandidate,
        expectedTitle: TitleIdentity,
        candidateTitle: TitleIdentity
    ): AlbumRelation {
        val expected = AlternativeTrackText.album(expectedRaw)
        if (expected.isBlank || expected.core in untrustedAlbumNames) return AlbumRelation.UNVERIFIED
        val candidate = AlternativeTrackText.album(candidateTrack.album)
        val candidateLanguage = candidate.language ?: candidateTrack.language.takeIf { it.isNotBlank() }
        if (expected.language != null && candidateLanguage != null && expected.language != candidateLanguage) {
            return AlbumRelation.MISMATCH
        }
        if (candidate.isBlank) return AlbumRelation.UNVERIFIED
        if (expected.core == candidate.core) {
            val expectedRemaster = AlbumEdition.REMASTERED in expected.editions
            val candidateRemaster = AlbumEdition.REMASTERED in candidate.editions
            if (expectedRemaster != candidateRemaster) return AlbumRelation.REMASTER_CONFLICT
            val sameEdition = expected.editions == candidate.editions &&
                expected.language == (candidate.language ?: candidateLanguage.takeIf { expected.language != null })
            return if (sameEdition) AlbumRelation.SAME else AlbumRelation.EDITION_VARIANT
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
        const val SAME_RECORDING_DURATION_SECONDS = 4

        private val singleEditions = setOf(AlbumEdition.SINGLE, AlbumEdition.EP)
        private val lengthenedVowel = Regex("([aeiou])\\1+")
        private val indianReleaseLanguages = setOf(
            "hindi",
            "punjabi",
            "urdu",
            "tamil",
            "telugu",
            "kannada",
            "malayalam",
            "marathi",
            "bengali",
            "gujarati",
            "bhojpuri",
            "odia",
            "haryanvi",
            "rajasthani",
            "assamese"
        )
        private val releaseSource = Regex("[(\\[]\\s*from\\s+[\"“”']?([^\"“”)\\]]+)", RegexOption.IGNORE_CASE)
        internal val untrustedAlbumNames = setOf(
            "levyra",
            "youtube",
            "youtube music",
            "youtube music radio",
            "youtube music related",
            "unknown",
            "unknown album",
            "single",
            "singles"
        )
    }
}
