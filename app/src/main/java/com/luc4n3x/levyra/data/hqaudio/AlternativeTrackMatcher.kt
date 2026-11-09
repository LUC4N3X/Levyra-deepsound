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
    val alternateReading: AlternativeTrackQuery? = null,
    val musicVideo: Boolean = false
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
    val artistRolesUnknown: Boolean = false,
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
    fun evaluateReadings(query: AlternativeTrackQuery, candidate: AlternativeTrackCandidate): AlternativeMatchEvaluation {
        val primary = evaluate(query, candidate)
        if (primary.accepted) return primary
        val alternate = query.alternateReading
        alternate?.let { evaluate(it, candidate) }?.takeIf { it.accepted }?.let { return it }
        return misspelledArtistReading(query, candidate, primary)
            ?: misspelledTitleReading(query, candidate, primary)
            ?: primary
    }

    private fun misspelledArtistReading(
        query: AlternativeTrackQuery,
        candidate: AlternativeTrackCandidate,
        rejected: AlternativeMatchEvaluation
    ): AlternativeMatchEvaluation? {
        if (rejected.rejection != MatchRejection.PRIMARY_ARTIST_MISMATCH) return null
        val expected = AlternativeTrackText.artistCredit(query.artist)
        if (expected.names.isEmpty()) return null
        val delta = durationDeltaSeconds(query.durationMs, candidate.durationSeconds)
        if (delta !in 0..MAXIMUM_DURATION_DELTA_SECONDS) return null
        val expectedTitle = AlternativeTrackText.title(query.title)
        val candidateTitle = AlternativeTrackText.title(candidate.title)
        if (expectedTitle.core != candidateTitle.core || expectedTitle.versionSignature != candidateTitle.versionSignature) return null
        val candidateNames = candidate.primaryArtists.flatMap(AlternativeTrackText::artistNames)
        val missing = expected.names.filterNot { it in candidateNames }
        if (missing.size != 1) return null
        val expectedName = missing.single()
        if (expectedName.length < MISSPELLING_MIN_NAME_LENGTH) return null
        val correctedName = candidateNames.firstOrNull { singleLetterTypo(it, expectedName) } ?: return null
        val correctedArtist = expected.names.joinToString(", ") { if (it == expectedName) correctedName else it }
        val evaluation = evaluate(query.copy(artist = correctedArtist, alternateReading = null), candidate)
        if (!evaluation.accepted) return null
        return evaluation.copy(
            verdict = AlternativeMatchVerdict.HIGH,
            confidence = (evaluation.confidence - MISSPELLING_PENALTY).coerceAtLeast(MINIMUM_ACCEPTED_CONFIDENCE)
        )
    }

    private fun misspelledTitleReading(
        query: AlternativeTrackQuery,
        candidate: AlternativeTrackCandidate,
        rejected: AlternativeMatchEvaluation
    ): AlternativeMatchEvaluation? {
        if (rejected.rejection != MatchRejection.TITLE_MISMATCH) return null
        val expectedTitle = AlternativeTrackText.title(query.title)
        val candidateTitle = AlternativeTrackText.title(candidate.title)
        if (expectedTitle.core.length < MISSPELLING_MIN_NAME_LENGTH || candidateTitle.core.length < MISSPELLING_MIN_NAME_LENGTH) return null
        if (expectedTitle.versionSignature != candidateTitle.versionSignature) return null
        if (!singleLetterTypo(expectedTitle.core, candidateTitle.core)) return null
        val delta = durationDeltaSeconds(query.durationMs, candidate.durationSeconds)
        if (delta !in 0..EXCELLENT_DURATION_DELTA_SECONDS) return null
        val relation = albumRelation(query.album, candidate, expectedTitle, candidateTitle)
        val expectedIsrc = normalizedIsrc(query.isrc)
        val isrcConfirmed = expectedIsrc.isNotEmpty() && expectedIsrc == normalizedIsrc(candidate.isrc)
        if (!isrcConfirmed && !albumConfirmsRecording(query, candidate, relation, candidateTitle)) return null
        val evaluation = evaluate(query.copy(title = candidate.title, alternateReading = null), candidate)
        if (!evaluation.accepted) return null
        return evaluation.copy(
            verdict = AlternativeMatchVerdict.HIGH,
            confidence = (evaluation.confidence - MISSPELLING_PENALTY).coerceAtLeast(MINIMUM_ACCEPTED_CONFIDENCE)
        )
    }

    private fun singleLetterTypo(a: String, b: String): Boolean =
        a != b && numberTokens(a) == numberTokens(b) && editDistanceAtMostOne(a, b)

    private fun numberTokens(text: String): List<String> =
        numberToken.findAll(text).map { it.value.trim() }.toList()

    private fun editDistanceAtMostOne(a: String, b: String): Boolean {
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                i++
                j++
                continue
            }
            if (++edits > 1) return false
            when {
                a.length > b.length -> i++
                a.length < b.length -> j++
                else -> { i++; j++ }
            }
        }
        return edits + (a.length - i) + (b.length - j) <= 1
    }

    fun evaluate(query: AlternativeTrackQuery, candidate: AlternativeTrackCandidate): AlternativeMatchEvaluation {
        val expectedTitle = AlternativeTrackText.title(query.title)
        val candidateTitle = AlternativeTrackText.title(candidate.title)
        val expectedCredit = AlternativeTrackText.artistCredit(query.artist)
        val delta = effectiveDelta(query, candidate, expectedTitle, candidateTitle)
        basicRejection(query, candidate, expectedTitle, candidateTitle, expectedCredit, delta)?.let {
            return rejectedEvaluation(candidate, candidateTitle, delta, it)
        }
        val expectedArtists = expectedCredit.names + expectedTitle.featuredArtists
        val candidatePrimary = candidate.primaryArtists.flatMap(AlternativeTrackText::artistNames).distinct()
        val performingPrimary = performers(candidate, candidatePrimary)
        val candidateFeatured = candidate.featuredArtists.flatMap(AlternativeTrackText::artistNames).toSet() +
            candidateTitle.featuredArtists
        val ledByExpectedArtist = candidatePrimary.firstOrNull() in expectedArtists ||
            performingPrimary.firstOrNull() in expectedArtists
        val ledByCreditedCreator = !ledByExpectedArtist &&
            expectedArtistLeadsAfterCreators(candidate, performingPrimary, expectedArtists)
        artistOrExplicitRejection(
            query,
            candidate,
            expectedTitle,
            candidateTitle,
            expectedCredit,
            candidatePrimary,
            candidateFeatured,
            ledByExpectedArtist || ledByCreditedCreator
        )?.let { return rejectedEvaluation(candidate, candidateTitle, delta, it) }

        val isrcConfirmed = isIsrcConfirmed(query.isrc, candidate.isrc)
        val relation = albumRelation(query.album, candidate, expectedTitle, candidateTitle)
        albumOrVersionRejection(query, candidate, expectedTitle, candidateTitle, relation, delta, isrcConfirmed)?.let {
            return rejectedEvaluation(candidate, candidateTitle, delta, it, relation)
        }
        val candidateArtists = candidatePrimary.toSet() + candidateFeatured
        val artistsExact = expectedArtists.containsAll(performingPrimary) &&
            candidateArtists.containsAll(expectedCredit.names)
        confirmationOrDriftRejection(
            query,
            candidate,
            expectedTitle,
            candidateTitle,
            expectedCredit,
            candidatePrimary,
            candidateArtists,
            artistsExact,
            ledByCreditedCreator,
            relation,
            delta,
            isrcConfirmed
        )?.let { return rejectedEvaluation(candidate, candidateTitle, delta, it, relation) }

        return scoreAcceptedEvaluation(
            candidate,
            expectedTitle,
            candidateTitle,
            expectedArtists,
            candidateArtists,
            candidateFeatured,
            artistsExact,
            relation,
            delta,
            isrcConfirmed
        )
    }

    private fun rejectedEvaluation(
        candidate: AlternativeTrackCandidate,
        candidateTitle: TitleIdentity,
        delta: Int,
        reason: MatchRejection,
        relation: AlbumRelation? = null
    ) = AlternativeMatchEvaluation(
        candidate = candidate,
        verdict = AlternativeMatchVerdict.REJECTED,
        confidence = 0,
        rejection = reason,
        durationDeltaSeconds = delta,
        albumRelation = relation,
        versionSignature = candidateTitle.versionSignature
    )

    private fun basicRejection(
        query: AlternativeTrackQuery,
        candidate: AlternativeTrackCandidate,
        expectedTitle: TitleIdentity,
        candidateTitle: TitleIdentity,
        expectedCredit: ArtistCredit,
        delta: Int
    ): MatchRejection? = when {
        expectedTitle.core.isBlank() || expectedCredit.primary.isBlank() -> MatchRejection.INSUFFICIENT_IDENTITY
        delta < 0 -> MatchRejection.DURATION_UNKNOWN
        hasIsrcMismatch(query.isrc, candidate.isrc) -> MatchRejection.ISRC_MISMATCH
        !matchesTitleCoreOrVariant(expectedTitle, candidateTitle, candidate.language) -> MatchRejection.TITLE_MISMATCH
        expectedTitle.versionSignature != candidateTitle.versionSignature &&
            !isRemasterOfUnmarkedTitle(expectedTitle, candidateTitle) -> MatchRejection.VERSION_MISMATCH
        else -> null
    }

    private fun artistOrExplicitRejection(
        query: AlternativeTrackQuery,
        candidate: AlternativeTrackCandidate,
        expectedTitle: TitleIdentity,
        candidateTitle: TitleIdentity,
        expectedCredit: ArtistCredit,
        candidatePrimary: List<String>,
        candidateFeatured: Set<String>,
        ledByExpectedOrCreator: Boolean
    ): MatchRejection? {
        if (expectedCredit.primary !in candidatePrimary) {
            return if (expectedCredit.primary in candidateFeatured) {
                MatchRejection.FEATURED_ARTIST_ONLY
            } else {
                MatchRejection.PRIMARY_ARTIST_MISMATCH
            }
        }
        if (!ledByExpectedOrCreator) return MatchRejection.PRIMARY_ARTIST_MISMATCH
        val expectedExplicit = query.explicit ?: expectedTitle.explicitHint
        val candidateExplicit = candidate.explicit ?: candidateTitle.explicitHint
        if (expectedExplicit != null && candidateExplicit != null && expectedExplicit != candidateExplicit) {
            return MatchRejection.EXPLICIT_MISMATCH
        }
        return null
    }

    private fun albumOrVersionRejection(
        query: AlternativeTrackQuery,
        candidate: AlternativeTrackCandidate,
        expectedTitle: TitleIdentity,
        candidateTitle: TitleIdentity,
        relation: AlbumRelation,
        delta: Int,
        isrcConfirmed: Boolean
    ): MatchRejection? = when {
        isrcConfirmed -> null
        albumHasConflictingVersion(query, candidate, expectedTitle) ->
            MatchRejection.VERSION_MISMATCH
        isRemasterOfUnmarkedTitle(expectedTitle, candidateTitle) && !allowsUnmarkedRemasterAlbum(relation, delta) ->
            MatchRejection.VERSION_MISMATCH
        isAlbumMismatch(expectedTitle, candidate.album, relation, delta) -> MatchRejection.ALBUM_MISMATCH
        else -> null
    }

    private fun confirmationOrDriftRejection(
        query: AlternativeTrackQuery,
        candidate: AlternativeTrackCandidate,
        expectedTitle: TitleIdentity,
        candidateTitle: TitleIdentity,
        expectedCredit: ArtistCredit,
        candidatePrimary: List<String>,
        candidateArtists: Set<String>,
        artistsExact: Boolean,
        ledByCreditedCreator: Boolean,
        relation: AlbumRelation,
        delta: Int,
        isrcConfirmed: Boolean
    ): MatchRejection? {
        if (delta > MAXIMUM_DURATION_DELTA_SECONDS) return MatchRejection.DURATION_OUT_OF_RANGE
        val recordingConfirmed = isRecordingConfirmed(
            query,
            candidate,
            candidateTitle,
            expectedCredit,
            candidatePrimary,
            relation,
            delta,
            isrcConfirmed
        )
        return when {
            expectedTitle.core != candidateTitle.core && !recordingConfirmed -> MatchRejection.TITLE_MISMATCH
            ledByCreditedCreator && !recordingConfirmed -> MatchRejection.PRIMARY_ARTIST_MISMATCH
            !candidateArtists.containsAll(expectedCredit.names) -> MatchRejection.PRIMARY_ARTIST_MISMATCH
            isRemasterOfUnmarkedTitle(expectedTitle, candidateTitle) && !isrcConfirmed && !artistsExact ->
                MatchRejection.VERSION_MISMATCH
            else -> null
        }
    }

    private fun scoreAcceptedEvaluation(
        candidate: AlternativeTrackCandidate,
        expectedTitle: TitleIdentity,
        candidateTitle: TitleIdentity,
        expectedArtists: Set<String>,
        candidateArtists: Set<String>,
        candidateFeatured: Set<String>,
        artistsExact: Boolean,
        relation: AlbumRelation,
        delta: Int,
        isrcConfirmed: Boolean
    ): AlternativeMatchEvaluation {
        val titleExact = titleMatchesExactly(expectedTitle, candidateTitle, candidateArtists)
        val albumAllowsDrift = relation == AlbumRelation.SAME || (relation == AlbumRelation.UNVERIFIED && titleExact)
        if (delta > EXCELLENT_DURATION_DELTA_SECONDS && !(albumAllowsDrift && artistsExact)) {
            return rejectedEvaluation(candidate, candidateTitle, delta, MatchRejection.DURATION_OUT_OF_RANGE, relation)
        }
        val confidence = confidence(titleExact, artistsExact, relation, delta, isrcConfirmed) -
            confidencePenalty(
                candidate,
                candidateArtists - expectedArtists,
                candidateFeatured - expectedArtists,
                relation,
                isrcConfirmed
            )
        val verdict = verdictFor(titleExact, artistsExact, relation, delta, confidence)
            ?: return rejectedEvaluation(candidate, candidateTitle, delta, MatchRejection.LOW_CONFIDENCE, relation)
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

    private fun verdictFor(
        titleExact: Boolean,
        artistsExact: Boolean,
        relation: AlbumRelation,
        delta: Int,
        confidence: Int
    ): AlternativeMatchVerdict? = when {
        titleExact && artistsExact && relation == AlbumRelation.SAME && delta <= EXCELLENT_DURATION_DELTA_SECONDS ->
            AlternativeMatchVerdict.EXACT
        confidence >= MINIMUM_ACCEPTED_CONFIDENCE -> AlternativeMatchVerdict.HIGH
        else -> null
    }

    private fun hasIsrcMismatch(expectedRaw: String, candidateRaw: String): Boolean {
        val expectedIsrc = normalizedIsrc(expectedRaw)
        val candidateIsrc = normalizedIsrc(candidateRaw)
        return expectedIsrc.isNotEmpty() && candidateIsrc.isNotEmpty() && expectedIsrc != candidateIsrc
    }

    private fun isIsrcConfirmed(expectedRaw: String, candidateRaw: String): Boolean {
        val expectedIsrc = normalizedIsrc(expectedRaw)
        return expectedIsrc.isNotEmpty() && expectedIsrc == normalizedIsrc(candidateRaw)
    }

    private fun matchesTitleCoreOrVariant(
        expectedTitle: TitleIdentity,
        candidateTitle: TitleIdentity,
        language: String
    ): Boolean = expectedTitle.core == candidateTitle.core ||
        romanizedVariant(expectedTitle.core, candidateTitle.core, language) ||
        parentheticalSubtitleVariant(expectedTitle, candidateTitle, language)

    private fun isRemasterOfUnmarkedTitle(expectedTitle: TitleIdentity, candidateTitle: TitleIdentity): Boolean =
        expectedTitle.versionSignature.isEmpty() && candidateTitle.markers == setOf(TrackVersionMarker.REMASTER)

    private fun allowsUnmarkedRemasterAlbum(relation: AlbumRelation, delta: Int): Boolean =
        delta <= EXCELLENT_DURATION_DELTA_SECONDS &&
            (relation == AlbumRelation.SINGLE_RELEASE || relation == AlbumRelation.UNVERIFIED)

    private fun isAlbumMismatch(
        expectedTitle: TitleIdentity,
        candidateAlbum: String,
        relation: AlbumRelation,
        delta: Int
    ): Boolean {
        if (relation == AlbumRelation.REMASTER_CONFLICT) return true
        if (relation != AlbumRelation.MISMATCH) return false
        val sameMarkedRecording = expectedTitle.versionSignature.isNotEmpty() &&
            delta <= 1 &&
            !namesAnotherPerformance(candidateAlbum)
        return !sameMarkedRecording
    }

    private fun isRecordingConfirmed(
        query: AlternativeTrackQuery,
        candidate: AlternativeTrackCandidate,
        candidateTitle: TitleIdentity,
        expectedCredit: ArtistCredit,
        candidatePrimary: List<String>,
        relation: AlbumRelation,
        delta: Int,
        isrcConfirmed: Boolean
    ): Boolean = isrcConfirmed || (
        albumConfirmsRecording(query, candidate, relation, candidateTitle) &&
            delta <= EXCELLENT_DURATION_DELTA_SECONDS &&
            candidatePrimary.containsAll(expectedCredit.names)
        )

    private fun titleMatchesExactly(expected: TitleIdentity, actual: TitleIdentity, candidateArtists: Set<String>): Boolean =
        expected.fullNormalized == actual.fullNormalized || (
            expected.core == actual.core &&
                expected.versionSignature == actual.versionSignature &&
                expected.featuredArtists.isNotEmpty() &&
                candidateArtists.containsAll(expected.featuredArtists) &&
                actual.featuredArtists.all { it in expected.featuredArtists }
            )

    private fun effectiveDelta(
        query: AlternativeTrackQuery,
        candidate: AlternativeTrackCandidate,
        expectedTitle: TitleIdentity,
        candidateTitle: TitleIdentity
    ): Int {
        val measured = durationDeltaSeconds(query.durationMs, candidate.durationSeconds)
        val paddedVideo = query.musicVideo &&
            measured in (MAXIMUM_DURATION_DELTA_SECONDS + 1)..MAXIMUM_MUSIC_VIDEO_PADDING_SECONDS &&
            candidate.durationSeconds * 1_000L < query.durationMs &&
            candidateTitle.versionSignature.isEmpty() &&
            videoIdentityMatches(query, candidate, expectedTitle, candidateTitle)
        return if (paddedVideo) 0 else measured
    }

    private fun videoIdentityMatches(
        query: AlternativeTrackQuery,
        candidate: AlternativeTrackCandidate,
        expectedTitle: TitleIdentity,
        candidateTitle: TitleIdentity
    ): Boolean {
        val expectedCredit = AlternativeTrackText.artistCredit(query.artist)
        val expectedArtists = (expectedCredit.names + expectedTitle.featuredArtists).toSet()
        val candidatePrimary = candidate.primaryArtists.flatMap(AlternativeTrackText::artistNames).distinct()
        val candidateArtists = candidatePrimary.toSet() + candidate.featuredArtists.flatMap(AlternativeTrackText::artistNames) +
            candidateTitle.featuredArtists
        val artistsExact = expectedArtists.containsAll(performers(candidate, candidatePrimary)) &&
            candidateArtists.containsAll(expectedCredit.names)
        val isrc = normalizedIsrc(query.isrc)
        val albumConfirmed = query.album.isBlank() || (isrc.isNotEmpty() && isrc == normalizedIsrc(candidate.isrc)) ||
            albumRelation(query.album, candidate, expectedTitle, candidateTitle) == AlbumRelation.SAME
        return artistsExact && albumConfirmed && titleMatchesExactly(expectedTitle, candidateTitle, candidateArtists)
    }

    private fun confidencePenalty(
        candidate: AlternativeTrackCandidate,
        unrequestedArtists: Set<String>,
        unrequestedFeatured: Set<String>,
        relation: AlbumRelation,
        isrcConfirmed: Boolean
    ): Int = when {
        isrcConfirmed -> 0
        candidate.artistRolesUnknown && relation != AlbumRelation.SAME && unrequestedArtists.isNotEmpty() -> UNVERIFIED_ROLES_PENALTY
        unrequestedFeatured.isNotEmpty() -> AMBIGUITY_MARGIN
        else -> 0
    }

    fun sharesTitle(query: AlternativeTrackQuery, title: String, language: String): Boolean {
        val expected = AlternativeTrackText.title(query.title)
        val actual = AlternativeTrackText.title(title)
        return expected.core == actual.core ||
            romanizedVariant(expected.core, actual.core, language) ||
            parentheticalSubtitleVariant(expected, actual, language) ||
            (expected.core.length >= MISSPELLING_MIN_NAME_LENGTH &&
                actual.core.length >= MISSPELLING_MIN_NAME_LENGTH &&
                singleLetterTypo(expected.core, actual.core))
    }

    fun select(query: AlternativeTrackQuery, candidates: List<AlternativeTrackCandidate>): AlternativeMatchSelection {
        val selection = selectWithRating(query, candidates)
        if (selection is AlternativeMatchSelection.Accepted || query.explicit != false) return selection
        return selectWithRating(withoutRating(query), candidates) as? AlternativeMatchSelection.Accepted ?: selection
    }

    fun evaluateSelected(query: AlternativeTrackQuery, candidate: AlternativeTrackCandidate): AlternativeMatchEvaluation {
        val evaluation = evaluateReadings(query, candidate)
        if (evaluation.accepted || evaluation.rejection != MatchRejection.EXPLICIT_MISMATCH || query.explicit != false) return evaluation
        return evaluateReadings(withoutRating(query), candidate)
    }

    private fun withoutRating(query: AlternativeTrackQuery): AlternativeTrackQuery =
        query.copy(explicit = null, alternateReading = query.alternateReading?.copy(explicit = null))

    private fun selectWithRating(query: AlternativeTrackQuery, candidates: List<AlternativeTrackCandidate>): AlternativeMatchSelection {
        val evaluations = candidates
            .distinctBy { it.providerId to it.providerTrackId }
            .map { evaluateReadings(query, it) }
        val accepted = evaluations
            .filter { it.accepted }
            .sortedWith(
                compareBy<AlternativeMatchEvaluation> { it.verdict.ordinal }
                    .thenByDescending { it.confidence }
                    .thenByDescending { it.candidate.offers320 == true }
            )
        val top = accepted.firstOrNull()
            ?: return AlternativeMatchSelection.Rejected(dominantRejection(evaluations), evaluations)
        val expectedExplicit = query.explicit ?: AlternativeTrackText.title(query.title).explicitHint
        val nearTop = accepted.filter { it.verdict == top.verdict && top.confidence - it.confidence < AMBIGUITY_MARGIN }
        val ambiguous = nearTop.withIndex().any { (index, left) ->
            nearTop.drop(index + 1).any { right -> !sameRecordingOrRelease(left, right, expectedExplicit != null) }
        }
        if (ambiguous) return AlternativeMatchSelection.Rejected(MatchRejection.AMBIGUOUS, evaluations)
        val chosen = accepted
            .filter { it.verdict == top.verdict && sameRecordingOrRelease(it, top, expectedExplicit != null) }
            .maxWith(
                compareBy<AlternativeMatchEvaluation> {
                    expectedExplicit != null && candidateExplicit(it.candidate) == expectedExplicit
                }
                    .thenBy { it.candidate.offers320 == true }
                    .thenBy { it.confidence }
                    .thenByDescending { it.candidate.releaseYear.takeIf { year -> year > 0 } ?: Int.MAX_VALUE }
                    .thenBy { albumPreference(it.albumRelation) }
                    .thenBy { !isCompilationAlbum(it.candidate.album) }
                    .thenBy { AlternativeTrackText.title(it.candidate.title).let { t -> t.core == t.baseCore } }
                    .thenByDescending { it.durationDeltaSeconds }
            )
        return AlternativeMatchSelection.Accepted(chosen, evaluations)
    }

    private fun albumPreference(relation: AlbumRelation?): Int = when (relation) {
        AlbumRelation.SAME -> 4
        AlbumRelation.EDITION_VARIANT -> 3
        AlbumRelation.SINGLE_RELEASE -> 2
        AlbumRelation.UNVERIFIED -> 1
        else -> 0
    }

    private fun isCompilationAlbum(album: String): Boolean =
        compilationAlbumPhrase.containsMatchIn(AlternativeTrackText.normalize(album))

    private fun sameRecordingOrRelease(left: AlternativeMatchEvaluation, right: AlternativeMatchEvaluation, explicitRequired: Boolean): Boolean =
        sameExactRelease(left, right, explicitRequired) || sameRecording(left, right, explicitRequired)

    private fun sameExactRelease(left: AlternativeMatchEvaluation, right: AlternativeMatchEvaluation, explicitRequired: Boolean): Boolean {
        val leftExplicit = candidateExplicit(left.candidate)
        val rightExplicit = candidateExplicit(right.candidate)
        return left.verdict == AlternativeMatchVerdict.EXACT && right.verdict == AlternativeMatchVerdict.EXACT &&
            left.albumRelation == AlbumRelation.SAME && right.albumRelation == AlbumRelation.SAME &&
            left.versionSignature == right.versionSignature &&
            abs(left.candidate.durationSeconds - right.candidate.durationSeconds) <= 1 &&
            (!explicitRequired || leftExplicit == null || rightExplicit == null || leftExplicit == rightExplicit)
    }

    private fun sameRecording(left: AlternativeMatchEvaluation, right: AlternativeMatchEvaluation, explicitRequired: Boolean): Boolean {
        val a = left.candidate
        val b = right.candidate
        if (a.providerId == b.providerId && a.providerTrackId == b.providerTrackId) return true
        val aTitle = AlternativeTrackText.title(a.title)
        val bTitle = AlternativeTrackText.title(b.title)
        val aExplicit = candidateExplicit(a)
        val bExplicit = candidateExplicit(b)
        return sameSong(a, aTitle, b, bTitle) &&
            aTitle.versionSignature == bTitle.versionSignature &&
            sameFeaturedCredits(featuredSet(a, aTitle), featuredSet(b, bTitle)) &&
            abs(a.durationSeconds - b.durationSeconds) <= SAME_RECORDING_DURATION_SECONDS &&
            (!explicitRequired || aExplicit == null || bExplicit == null || aExplicit == bExplicit) &&
            (a.language.isBlank() || b.language.isBlank() || a.language == b.language ||
                !(isIndianRelease(a.language) || isIndianRelease(b.language)))
    }

    private fun sameSong(
        a: AlternativeTrackCandidate,
        aTitle: TitleIdentity,
        b: AlternativeTrackCandidate,
        bTitle: TitleIdentity
    ): Boolean {
        val sameTitle = aTitle.core == bTitle.core || (
            isIndianRelease(a.language) && isIndianRelease(b.language) &&
                romanizationKey(aTitle.baseCore) == romanizationKey(bTitle.baseCore)
            )
        return sameTitle && samePrimaryCredits(a, b)
    }

    private fun candidateExplicit(candidate: AlternativeTrackCandidate): Boolean? =
        candidate.explicit ?: AlternativeTrackText.title(candidate.title).explicitHint

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

    private fun parentheticalSubtitleVariant(
        expectedTitle: TitleIdentity,
        candidateTitle: TitleIdentity,
        language: String
    ): Boolean {
        if (expectedTitle.core == candidateTitle.core || candidateTitle.baseCore == candidateTitle.core) return false
        if (!isIndianRelease(language)) return false
        return expectedTitle.core == candidateTitle.baseCore ||
            romanizedVariant(expectedTitle.core, candidateTitle.baseCore, language)
    }

    private fun albumHasConflictingVersion(
        query: AlternativeTrackQuery,
        candidate: AlternativeTrackCandidate,
        expectedTitle: TitleIdentity
    ): Boolean {
        if (candidate.album.isBlank()) return false
        val candidateAlbumMarkers = AlternativeTrackText.album(candidate.album).versionMarkers intersect conflictingAlbumVersionMarkers
        if (candidateAlbumMarkers.isEmpty()) return false
        val expectedMarkers = (expectedTitle.markers + AlternativeTrackText.album(query.album).versionMarkers) intersect conflictingAlbumVersionMarkers
        return !expectedMarkers.containsAll(candidateAlbumMarkers)
    }

    private fun albumConfirmsRecording(
        query: AlternativeTrackQuery,
        candidate: AlternativeTrackCandidate,
        relation: AlbumRelation,
        candidateTitle: TitleIdentity
    ): Boolean = when (relation) {
        AlbumRelation.SAME, AlbumRelation.EDITION_VARIANT -> true
        AlbumRelation.SINGLE_RELEASE -> {
            val album = AlternativeTrackText.album(candidate.album)
            isSingleAlbum(album, candidateTitle) ||
                album.core in releaseSources(query.title, query.album) ||
                AlternativeTrackText.album(query.album).core in releaseSources(candidate.title, candidate.album)
        }
        else -> false
    }

    private fun isSingleAlbum(album: AlbumIdentity, title: TitleIdentity): Boolean =
        album.core == title.core || album.core == title.baseCore || album.editions.any(singleEditions::contains)

    private fun releaseSources(vararg texts: String): Set<String> =
        texts.asSequence()
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
        if (expected.core == candidate.core) return sameCoreRelation(expected, candidate, candidateLanguage, continuation = false)
        if (continuesAlbum(candidateTrack.album, expected.core)) {
            return sameCoreRelation(expected, candidate, candidateLanguage, continuation = true)
        }
        val expectedSingle = isSingleAlbum(expected, expectedTitle)
        val candidateSingle = isSingleAlbum(candidate, candidateTitle)
        return if (expectedSingle || candidateSingle) AlbumRelation.SINGLE_RELEASE else AlbumRelation.MISMATCH
    }

    private fun namesAnotherPerformance(album: String): Boolean =
        venueLivePhrase.containsMatchIn(AlternativeTrackText.normalize(album))

    private fun continuesAlbum(candidateRaw: String, expectedCore: String): Boolean =
        candidateRaw.contains(" - ") && AlternativeTrackText.album(candidateRaw.substringBefore(" - ")).core == expectedCore

    private fun sameCoreRelation(
        expected: AlbumIdentity,
        candidate: AlbumIdentity,
        candidateLanguage: String?,
        continuation: Boolean
    ): AlbumRelation {
        val expectedRemaster = AlbumEdition.REMASTERED in expected.editions
        val candidateRemaster = AlbumEdition.REMASTERED in candidate.editions
        if (expectedRemaster != candidateRemaster) return AlbumRelation.REMASTER_CONFLICT
        val sameEdition = expected.editions == candidate.editions &&
            expected.language == (candidate.language ?: candidateLanguage.takeIf { expected.language != null })
        return if (sameEdition && !continuation) AlbumRelation.SAME else AlbumRelation.EDITION_VARIANT
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
        const val MAXIMUM_MUSIC_VIDEO_PADDING_SECONDS = 150
        private const val UNVERIFIED_ROLES_PENALTY = 100
        private const val MISSPELLING_MIN_NAME_LENGTH = 6
        private const val MISSPELLING_PENALTY = 8
        const val MINIMUM_ACCEPTED_CONFIDENCE = 75
        const val PERSISTABLE_CONFIDENCE = 85
        const val AMBIGUITY_MARGIN = 8
        const val SAME_RECORDING_DURATION_SECONDS = 4

        private val conflictingAlbumVersionMarkers = setOf(
            TrackVersionMarker.LIVE,
            TrackVersionMarker.KARAOKE,
            TrackVersionMarker.INSTRUMENTAL,
            TrackVersionMarker.COVER,
            TrackVersionMarker.SPED_UP,
            TrackVersionMarker.SLOWED,
            TrackVersionMarker.REVERB,
            TrackVersionMarker.REMIX,
            TrackVersionMarker.ACOUSTIC,
            TrackVersionMarker.DEMO,
            TrackVersionMarker.VERSION
        )
        private val numberToken = Regex("\\d+|(?:^|\\s)(?:i|ii|iii|iv|v|vi|vii|viii|ix|x)(?=\\s|$)")
        private val compilationAlbumPhrase = Regex(
            "(?:^|\\s)(?:best of|greatest hits|all time hits|superhits|super hits|top hits|mega hits|hit songs|love songs|trending|heartbeats|valentine|valentines special|master class|essentials|anthology|collection|compilations?|the highlights|papercuts|chartbusters|blockbusters|non stop|party hits|romantic hits|sad songs|bollywood hits)(?:\\s|$)"
        )
        private val venueLivePhrase =
            Regex("""(?:^|\s)(?:live|ao vivo|en vivo|en directo|dal vivo)\s+(?:at|from|in|on|no|na|em|en|desde|da|dal|del)\s""")
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
