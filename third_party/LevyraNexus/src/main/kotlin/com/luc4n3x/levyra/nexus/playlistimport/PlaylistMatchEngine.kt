package com.luc4n3x.levyra.nexus.playlistimport

import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import com.luc4n3x.levyra.nexus.identity.TitleIdentity
import com.luc4n3x.levyra.nexus.identity.TrackVersionMarker
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class CandidateOrigin { LOCAL, ONLINE }

enum class CandidateKind { SONG, OFFICIAL_VIDEO, USER_VIDEO, UNKNOWN }

enum class ResolutionPreference { SMART, PREFER_LOCAL, PREFER_ONLINE }

data class MatchCandidate(
    val id: String,
    val title: String,
    val artists: List<String>,
    val album: String = "",
    val durationMs: Long = 0L,
    val isrc: String = "",
    val explicit: Boolean? = null,
    val origin: CandidateOrigin = CandidateOrigin.ONLINE,
    val kind: CandidateKind = CandidateKind.UNKNOWN,
    val artworkUrl: String = "",
    val available: Boolean = true
) {
    val artistLine: String
        get() = artists.joinToString(", ")
}

enum class MatchSignal {
    DIRECT_ID,
    ISRC_MATCH,
    ISRC_CONFLICT,
    TITLE_EXACT,
    TITLE_CLOSE,
    TITLE_WEAK,
    TITLE_MISMATCH,
    ARTIST_EXACT,
    ARTIST_PRIMARY,
    ARTIST_PARTIAL,
    ARTIST_FEATURED_ONLY,
    ARTIST_UNKNOWN,
    ARTIST_MISMATCH,
    ALBUM_SAME,
    ALBUM_EDITION,
    ALBUM_DIFFERENT,
    DURATION_CLOSE,
    DURATION_DRIFT,
    DURATION_FAR,
    DURATION_UNKNOWN,
    VERSION_MATCH,
    VERSION_CONFLICT,
    VERSION_SOFT_DIFFERENCE,
    EXPLICIT_CONFLICT,
    SONG_ENTITY,
    VIDEO_UPLOAD,
    UNAVAILABLE
}

data class MatchReason(
    val signal: MatchSignal,
    val deltaMs: Long = 0L,
    val detail: String = ""
)

enum class MatchConfidence {
    EXACT,
    EXCELLENT,
    GOOD,
    REVIEW,
    UNRESOLVED;

    val autoAccepted: Boolean
        get() = this == EXACT || this == EXCELLENT || this == GOOD

    companion object {
        const val EXACT_SCORE = 98
        const val EXCELLENT_SCORE = 90
        const val AUTO_ACCEPT_SCORE = 80
        const val REVIEW_SCORE = 55

        fun of(score: Int, reasons: List<MatchReason>): MatchConfidence {
            val definitive = reasons.any { it.signal == MatchSignal.DIRECT_ID || it.signal == MatchSignal.ISRC_MATCH }
            return when {
                definitive && score >= EXACT_SCORE -> EXACT
                score >= EXCELLENT_SCORE -> EXCELLENT
                score >= AUTO_ACCEPT_SCORE -> GOOD
                score >= REVIEW_SCORE -> REVIEW
                else -> UNRESOLVED
            }
        }
    }
}

data class MatchEvaluation(
    val candidate: MatchCandidate,
    val score: Int,
    val reasons: List<MatchReason>
) {
    val confidence: MatchConfidence
        get() = MatchConfidence.of(score, reasons)
}

data class MatchOutcome(
    val selected: MatchEvaluation?,
    val confidence: MatchConfidence,
    val alternatives: List<MatchEvaluation>,
    val ambiguous: Boolean
)

object PlaylistMatchEngine {
    const val MAX_ALTERNATIVES = 5
    private const val AMBIGUITY_MARGIN = 3
    private const val SAME_RECORDING_DURATION_MS = 3_000L
    private val hardMarkers = setOf(
        TrackVersionMarker.LIVE,
        TrackVersionMarker.REMIX,
        TrackVersionMarker.ACOUSTIC,
        TrackVersionMarker.INSTRUMENTAL,
        TrackVersionMarker.KARAOKE,
        TrackVersionMarker.COVER,
        TrackVersionMarker.SPED_UP,
        TrackVersionMarker.SLOWED,
        TrackVersionMarker.REVERB,
        TrackVersionMarker.EXTENDED,
        TrackVersionMarker.DEMO
    )
    private val mediumMarkers = setOf(TrackVersionMarker.RADIO_EDIT, TrackVersionMarker.EDIT, TrackVersionMarker.VERSION)
    private val softMarkers = setOf(TrackVersionMarker.REMASTER, TrackVersionMarker.MONO, TrackVersionMarker.STEREO)
    private val channelSuffix = Regex("\\s*(?:-\\s*topic|vevo)$", RegexOption.IGNORE_CASE)
    private val remixCreditSuffix = Regex("\\s+(?:(?:extended|club|radio|original|dub)\\s+)*(?:remix|mix|remixed|rework|bootleg|mashup)$")
    private val untrustedAlbums = setOf("", "levyra", "youtube", "youtube music", "unknown", "unknown album", "single", "singles")
    private val uploadTitleSeparator = Regex("\\s+[-–—]\\s+")
    private val releaseContextLabel = Regex("(?:^|\\s)(?:soundtrack|film|movie|ost|single|album|original)\\s+version$")
    private const val TITLE_CREDIT_DETAIL = "title-credit"
    private const val INCOMPLETE_CREDITS_DETAIL = "incomplete-credits"
    private const val CORROBORATED_DURATION_MS = 2_000L

    fun evaluate(source: ImportedTrackIdentity, candidate: MatchCandidate): MatchEvaluation {
        if (!candidate.available) {
            return MatchEvaluation(candidate, 0, listOf(MatchReason(MatchSignal.UNAVAILABLE)))
        }
        if (source.directCatalogId.isNotBlank() && source.directCatalogId == candidate.id) {
            return MatchEvaluation(candidate, 100, listOf(MatchReason(MatchSignal.DIRECT_ID)))
        }
        val sourceIsrc = normalizeIsrc(source.isrc)
        val candidateIsrc = normalizeIsrc(candidate.isrc)
        if (sourceIsrc.isNotEmpty() && sourceIsrc == candidateIsrc) {
            return MatchEvaluation(candidate, 100, listOf(MatchReason(MatchSignal.ISRC_MATCH)))
        }
        val direct = evaluateMetadata(source, candidate, titleCredit = false)
        val credited = uploadTitleCredit(source, candidate) ?: return direct
        val reread = evaluateMetadata(source, credited, titleCredit = true)
        return if (reread.score > direct.score) reread.copy(candidate = candidate) else direct
    }

    private fun uploadTitleCredit(source: ImportedTrackIdentity, candidate: MatchCandidate): MatchCandidate? {
        if (candidate.kind == CandidateKind.SONG || source.artists.isEmpty()) return null
        val separator = uploadTitleSeparator.find(candidate.title) ?: return null
        val prefixNames = MusicIdentityText.artistNames(candidate.title.substring(0, separator.range.first))
        val sourceNames = normalizedArtists(source.artists)
        val primary = sourceNames.firstOrNull() ?: return null
        if (prefixNames.none { it == primary || compactEquals(it, primary) }) return null
        val remainder = candidate.title.substring(separator.range.last + 1).trim()
        if (remainder.isBlank()) return null
        return candidate.copy(title = remainder, artists = prefixNames)
    }

    private fun evaluateMetadata(source: ImportedTrackIdentity, candidate: MatchCandidate, titleCredit: Boolean): MatchEvaluation {
        val sourceIsrc = normalizeIsrc(source.isrc)
        val candidateIsrc = normalizeIsrc(candidate.isrc)
        val reasons = ArrayList<MatchReason>()
        var score = 0
        var cap = 100
        val limit: (Int) -> Unit = { value -> cap = min(cap, value) }

        if (sourceIsrc.isNotEmpty() && candidateIsrc.isNotEmpty()) {
            reasons += MatchReason(MatchSignal.ISRC_CONFLICT)
            score -= 20
            limit(79)
        }

        val sourceTitle = MusicIdentityText.title(source.title)
        val candidateTitle = MusicIdentityText.title(candidate.title)
        val titleScore = scoreTitle(sourceTitle, candidateTitle, reasons, limit)
            ?: return MatchEvaluation(candidate, score.coerceIn(0, 20), reasons)
        score += titleScore
        val durationCorroborated = source.durationMs > 0L && candidate.durationMs > 0L &&
            abs(source.durationMs - candidate.durationMs) <= CORROBORATED_DURATION_MS
        score += scoreArtists(source, sourceTitle, candidate, candidateTitle, reasons, limit)
        score += scoreVersion(sourceTitle, candidateTitle, durationCorroborated, reasons, limit)
        score += scoreExplicit(
            source.explicit ?: sourceTitle.explicitHint,
            candidate.explicit ?: candidateTitle.explicitHint,
            reasons,
            limit
        )
        score += scoreDuration(source.durationMs, candidate.durationMs, reasons, limit)
        score += scoreAlbum(source, sourceTitle, candidate, candidateTitle, reasons)
        score += scoreKind(candidate, reasons, limit)
        val signals = reasons.map { it.signal }.toSet()
        val cleanRecording = MatchSignal.TITLE_EXACT in signals && durationCorroborated &&
            MatchSignal.VERSION_CONFLICT !in signals && MatchSignal.EXPLICIT_CONFLICT !in signals
        if (reasons.any { it.detail == INCOMPLETE_CREDITS_DETAIL } &&
            !(cleanRecording && candidate.kind == CandidateKind.SONG &&
                (MatchSignal.ALBUM_SAME in signals || MatchSignal.ALBUM_EDITION in signals))
        ) {
            limit(79)
        }
        if (titleCredit) {
            reasons += MatchReason(MatchSignal.VIDEO_UPLOAD, detail = TITLE_CREDIT_DETAIL)
            val unlabeledUpload = MatchSignal.TITLE_EXACT in signals && MatchSignal.DURATION_UNKNOWN in signals &&
                MatchSignal.VERSION_CONFLICT !in signals && MatchSignal.VERSION_SOFT_DIFFERENCE !in signals &&
                MatchSignal.EXPLICIT_CONFLICT !in signals && sourceTitle.markers.isEmpty() && candidateTitle.markers.isEmpty()
            limit(if ((cleanRecording || unlabeledUpload) && MatchSignal.ARTIST_EXACT in signals) 84 else 79)
        }
        return MatchEvaluation(candidate, score.coerceIn(0, cap), reasons)
    }

    private fun scoreTitle(
        sourceTitle: TitleIdentity,
        candidateTitle: TitleIdentity,
        reasons: MutableList<MatchReason>,
        limit: (Int) -> Unit
    ): Int? {
        val similarity = titleSimilarity(sourceTitle, candidateTitle)
        val short = isShortTitle(sourceTitle.core)
        return when {
            sourceTitle.core.isNotEmpty() && sourceTitle.core == candidateTitle.core ->
                45.also { reasons += MatchReason(MatchSignal.TITLE_EXACT) }
            ImportTransliteration.related(sourceTitle.core, candidateTitle.core) -> {
                reasons += MatchReason(MatchSignal.TITLE_CLOSE, detail = "transliteration")
                limit(84)
                42
            }
            similarity >= 0.85 && !short -> 32.also { reasons += MatchReason(MatchSignal.TITLE_CLOSE) }
            spellingVariant(sourceTitle.core, candidateTitle.core) ->
                32.also { reasons += MatchReason(MatchSignal.TITLE_CLOSE, detail = "spelling") }
            similarity >= 0.6 && !short -> {
                reasons += MatchReason(MatchSignal.TITLE_WEAK)
                limit(79)
                18
            }
            else -> {
                reasons += MatchReason(MatchSignal.TITLE_MISMATCH)
                null
            }
        }
    }

    private fun scoreExplicit(
        sourceExplicit: Boolean?,
        candidateExplicit: Boolean?,
        reasons: MutableList<MatchReason>,
        limit: (Int) -> Unit
    ): Int {
        if (sourceExplicit == null || candidateExplicit == null || sourceExplicit == candidateExplicit) return 0
        reasons += MatchReason(MatchSignal.EXPLICIT_CONFLICT, detail = if (sourceExplicit) "explicit" else "clean")
        limit(79)
        return -12
    }

    private fun scoreKind(candidate: MatchCandidate, reasons: MutableList<MatchReason>, limit: (Int) -> Unit): Int {
        val score = when (candidate.kind) {
            CandidateKind.SONG -> 3.also { reasons += MatchReason(MatchSignal.SONG_ENTITY) }
            CandidateKind.USER_VIDEO -> -6.also { reasons += MatchReason(MatchSignal.VIDEO_UPLOAD) }
            CandidateKind.OFFICIAL_VIDEO, CandidateKind.UNKNOWN -> 0
        }
        if (!candidate.available) {
            reasons += MatchReason(MatchSignal.UNAVAILABLE)
            limit(50)
        }
        return score
    }

    fun select(
        source: ImportedTrackIdentity,
        candidates: List<MatchCandidate>,
        preference: ResolutionPreference = ResolutionPreference.SMART
    ): MatchOutcome {
        val sourceTitle = MusicIdentityText.title(source.title).fullNormalized
        val evaluations = candidates
            .distinctBy { it.id }
            .map { evaluate(source, it) }
            .sortedWith(
                compareByDescending<MatchEvaluation> { it.score }
                    .thenByDescending { tokenSimilarity(sourceTitle, MusicIdentityText.title(it.candidate.title).fullNormalized) }
                    .thenBy { durationDelta(source, it.candidate) }
                    .thenBy { kindRank(it.candidate.kind) }
            )
        if (evaluations.isEmpty()) return MatchOutcome(null, MatchConfidence.UNRESOLVED, emptyList(), false)
        val preferred = applyPreference(evaluations, preference)
        val ambiguous = isAmbiguous(source, preferred, evaluations)
        val raw = preferred.confidence
        val confidence = when {
            raw == MatchConfidence.UNRESOLVED -> MatchConfidence.UNRESOLVED
            ambiguous && raw != MatchConfidence.EXACT -> MatchConfidence.REVIEW
            else -> raw
        }
        val alternatives = (listOf(preferred) + evaluations.filter { it.candidate.id != preferred.candidate.id })
            .take(MAX_ALTERNATIVES)
        return MatchOutcome(
            selected = preferred.takeIf { confidence != MatchConfidence.UNRESOLVED },
            confidence = confidence,
            alternatives = alternatives,
            ambiguous = ambiguous
        )
    }

    fun sameRecording(left: MatchCandidate, right: MatchCandidate): Boolean {
        if (left.id == right.id) return true
        val leftIsrc = normalizeIsrc(left.isrc)
        if (leftIsrc.isNotEmpty() && leftIsrc == normalizeIsrc(right.isrc)) return true
        val a = MusicIdentityText.title(left.title)
        val b = MusicIdentityText.title(right.title)
        val leftCredits = normalizedArtists(left.artists).toSet() + a.featuredArtists
        if (leftCredits.isEmpty()) return false
        val durationsCompatible = left.durationMs <= 0L || right.durationMs <= 0L ||
            abs(left.durationMs - right.durationMs) <= SAME_RECORDING_DURATION_MS
        return a.core == b.core &&
            versionIdentity(a) == versionIdentity(b) &&
            leftCredits.any { it in normalizedArtists(right.artists) || it in b.featuredArtists } &&
            durationsCompatible
    }

    fun sourceKey(identity: ImportedTrackIdentity): String {
        if (identity.sourceTrackId.isNotBlank()) return "id:${identity.sourceTrackId}"
        if (identity.directCatalogId.isNotBlank()) return "yt:${identity.directCatalogId}"
        val title = MusicIdentityText.title(identity.title)
        val artist = normalizedArtists(identity.artists).firstOrNull().orEmpty()
        val signature = title.versionSignature.sorted().joinToString("+")
        return "meta:${title.core}|$artist|$signature|${MusicIdentityText.album(identity.album).core}|${identity.durationMs}"
    }

    fun provisional(evaluation: MatchEvaluation): Boolean = evaluation.reasons.any { it.detail == TITLE_CREDIT_DETAIL }

    fun isrcSearchCorroborates(source: ImportedTrackIdentity, candidate: MatchCandidate): Boolean {
        if (normalizeIsrc(source.isrc).isEmpty() || candidate.kind != CandidateKind.SONG) return false
        val signals = evaluateMetadata(source.copy(isrc = ""), candidate.copy(isrc = ""), titleCredit = false)
            .reasons.map { it.signal }.toSet()
        return MatchSignal.TITLE_EXACT in signals &&
            (MatchSignal.ARTIST_EXACT in signals || MatchSignal.ARTIST_PRIMARY in signals) &&
            MatchSignal.VERSION_CONFLICT !in signals && MatchSignal.EXPLICIT_CONFLICT !in signals &&
            MatchSignal.DURATION_FAR !in signals && MatchSignal.DURATION_DRIFT !in signals
    }

    fun normalizeIsrc(value: String): String =
        value.uppercase(Locale.ROOT).filter { it in 'A'..'Z' || it in '0'..'9' }.takeIf { it.length == 12 }.orEmpty()

    private fun applyPreference(evaluations: List<MatchEvaluation>, preference: ResolutionPreference): MatchEvaluation {
        val best = evaluations.first()
        val bestLocal = evaluations.firstOrNull { it.candidate.origin == CandidateOrigin.LOCAL }
        val bestOnline = evaluations.firstOrNull { it.candidate.origin == CandidateOrigin.ONLINE }
        return when (preference) {
            ResolutionPreference.SMART ->
                bestLocal?.takeIf {
                    it.score >= MatchConfidence.EXCELLENT_SCORE && it.score >= (bestOnline?.score ?: 0) - AMBIGUITY_MARGIN
                } ?: best
            ResolutionPreference.PREFER_LOCAL ->
                bestLocal?.takeIf { it.score >= MatchConfidence.AUTO_ACCEPT_SCORE } ?: best
            ResolutionPreference.PREFER_ONLINE ->
                bestOnline?.takeIf { it.score >= MatchConfidence.AUTO_ACCEPT_SCORE } ?: best
        }
    }

    private fun isAmbiguous(source: ImportedTrackIdentity, selected: MatchEvaluation, evaluations: List<MatchEvaluation>): Boolean {
        if (selected.confidence == MatchConfidence.EXACT) return false
        return evaluations.any { other ->
            other.candidate.id != selected.candidate.id &&
                other.score >= selected.score - AMBIGUITY_MARGIN &&
                other.score >= MatchConfidence.AUTO_ACCEPT_SCORE &&
                !sameRecording(selected.candidate, other.candidate) &&
                !(source.durationMs <= 0L && sameWork(selected.candidate, other.candidate)) &&
                !durationDecides(source, selected.candidate, other.candidate) &&
                !(provisional(other) && !provisional(selected))
        }
    }

    private fun durationDelta(source: ImportedTrackIdentity, candidate: MatchCandidate): Long =
        if (source.durationMs <= 0L || candidate.durationMs <= 0L) Long.MAX_VALUE else abs(source.durationMs - candidate.durationMs)

    private fun durationDecides(source: ImportedTrackIdentity, selected: MatchCandidate, other: MatchCandidate): Boolean {
        val chosen = durationDelta(source, selected)
        val rival = durationDelta(source, other)
        return chosen <= CORROBORATED_DURATION_MS && rival != Long.MAX_VALUE && rival >= chosen + 3_000L
    }

    private fun sameWork(left: MatchCandidate, right: MatchCandidate): Boolean {
        val a = MusicIdentityText.title(left.title)
        val b = MusicIdentityText.title(right.title)
        val longer = max(left.durationMs, right.durationMs)
        val comparableLength = left.durationMs <= 0L || right.durationMs <= 0L ||
            abs(left.durationMs - right.durationMs) * 4 <= longer
        return comparableLength && a.core == b.core &&
            versionIdentity(a) == versionIdentity(b) &&
            a.featuredArtists == b.featuredArtists &&
            normalizedArtists(left.artists).toSet() == normalizedArtists(right.artists).toSet()
    }

    private fun scoreArtists(
        source: ImportedTrackIdentity,
        sourceTitle: TitleIdentity,
        candidate: MatchCandidate,
        candidateTitle: TitleIdentity,
        reasons: MutableList<MatchReason>,
        limit: (Int) -> Unit
    ): Int {
        val sourceNames = normalizedArtists(source.artists)
        if (sourceNames.isEmpty()) {
            reasons += MatchReason(MatchSignal.ARTIST_UNKNOWN)
            limit(79)
            return 14
        }
        val sourceFeatured = source.featuredArtists.flatMap(MusicIdentityText::artistNames).toSet() + sourceTitle.featuredArtists
        val primary = sourceNames.first()
        val candidatePrimary = normalizedArtists(candidate.artists)
        val versionCredits = matchingRemixerCredits(sourceNames, sourceFeatured, sourceTitle, candidatePrimary, candidateTitle)
        val candidateAll = candidatePrimary.toSet() + candidateTitle.featuredArtists + versionCredits
        val expected = sourceNames.toSet() + sourceFeatured + versionCredits
        return when {
            primary in candidatePrimary -> {
                val leading = candidatePrimary.firstOrNull() == primary
                val exact = sameArtistCredits(candidateAll, expected)
                val differentCredits = leading && !exact &&
                    expected.none { name -> name in sourceTitle.featuredArtists && !containsCredits(candidateAll, setOf(name)) }
                when {
                    exact -> Unit
                    differentCredits -> limit(84)
                    else -> limit(79)
                }
                reasons += if (exact) {
                    MatchReason(MatchSignal.ARTIST_EXACT)
                } else {
                    MatchReason(MatchSignal.ARTIST_PRIMARY, detail = if (differentCredits) INCOMPLETE_CREDITS_DETAIL else "")
                }
                if (exact) 32 else 27
            }
            primary in candidateTitle.featuredArtists -> {
                reasons += MatchReason(MatchSignal.ARTIST_FEATURED_ONLY)
                limit(70)
                10
            }
            candidatePrimary.firstOrNull()?.let { ImportTransliteration.related(primary, it) } == true -> {
                reasons += MatchReason(MatchSignal.ARTIST_PARTIAL, detail = "transliteration")
                limit(84)
                30
            }
            candidatePrimary.any { ImportTransliteration.related(primary, it) } -> {
                reasons += MatchReason(MatchSignal.ARTIST_PARTIAL, detail = "transliteration")
                limit(79)
                26
            }
            candidateAll.any { tokenSimilarity(primary, it) >= 0.75 || compactEquals(primary, it) } -> {
                reasons += MatchReason(MatchSignal.ARTIST_PARTIAL)
                limit(84)
                18
            }
            candidatePrimary.firstOrNull()?.let { nameAlias(primary, it) } == true &&
                (sourceNames.drop(1) + sourceFeatured).any { it in candidateAll } -> {
                reasons += MatchReason(MatchSignal.ARTIST_PARTIAL, detail = "alias")
                limit(84)
                26
            }
            else -> {
                reasons += MatchReason(MatchSignal.ARTIST_MISMATCH)
                limit(40)
                0
            }
        }
    }

    private fun matchingRemixerCredits(
        sourceNames: List<String>,
        sourceFeatured: Set<String>,
        sourceTitle: TitleIdentity,
        candidateNames: List<String>,
        candidateTitle: TitleIdentity
    ): Set<String> = if (
        sourceTitle.markers == candidateTitle.markers && versionLabels(sourceTitle) == versionLabels(candidateTitle) &&
        TrackVersionMarker.REMIX in sourceTitle.markers
    ) {
        val remixers = versionLabels(sourceTitle).mapNotNull { label ->
            remixCreditSuffix.find(label)?.let { MusicIdentityText.normalizeArtist(label.substring(0, it.range.first)) }
        }.toSet()
        (sourceNames.drop(1) + candidateNames.drop(1)).filter { name ->
            name !in sourceFeatured && name !in candidateTitle.featuredArtists && name in remixers
        }.toSet()
    } else emptySet()

    private fun sameArtistCredits(left: Set<String>, right: Set<String>): Boolean =
        containsCredits(right, left) && containsCredits(left, right)

    private fun containsCredits(all: Set<String>, required: Set<String>): Boolean =
        required.all { name -> all.any { it == name || compactEquals(it, name) } }

    private fun nameAlias(left: String, right: String): Boolean {
        val a = left.split(' ').filter(String::isNotBlank)
        val b = right.split(' ').filter(String::isNotBlank)
        if (a.size < 2 || b.size < 2 || a.last() != b.last() || a.last().length < 3) return false
        val first = a.first()
        val other = b.first()
        return first.length >= 3 && other.length >= 3 && first.take(3) == other.take(3)
    }

    private fun scoreVersion(
        sourceTitle: TitleIdentity,
        candidateTitle: TitleIdentity,
        durationCorroborated: Boolean,
        reasons: MutableList<MatchReason>,
        limit: (Int) -> Unit
    ): Int {
        val difference = sourceTitle.markers.union(candidateTitle.markers) - sourceTitle.markers.intersect(candidateTitle.markers)
        val hard = difference.filter { it in hardMarkers }
        val medium = difference.filter { it in mediumMarkers }
        val soft = difference.filter { it in softMarkers }
        val sourceLabels = versionLabels(sourceTitle)
        val candidateLabels = versionLabels(candidateTitle)
        val labelsDiffer = sourceLabels != candidateLabels
        var score = 0
        if (hard.isNotEmpty()) {
            reasons += MatchReason(MatchSignal.VERSION_CONFLICT, detail = versionDetail(hard, candidateTitle.markers))
            score -= 40
            limit(50)
        }
        val differingLabels = sourceLabels - candidateLabels + (candidateLabels - sourceLabels)
        val radioEditOnly = medium.isNotEmpty() && TrackVersionMarker.RADIO_EDIT in medium &&
            medium.all { it == TrackVersionMarker.RADIO_EDIT || it == TrackVersionMarker.EDIT }
        val releaseContextOnly = medium.all { it == TrackVersionMarker.VERSION } && differingLabels.isNotEmpty() &&
            differingLabels.all { releaseContextLabel.containsMatchIn(it) }
        val sameAudioEdit = durationCorroborated && hard.isEmpty() &&
            (radioEditOnly && !labelsDiffer || releaseContextOnly)
        if ((medium.isNotEmpty() || labelsDiffer) && sameAudioEdit) {
            reasons += MatchReason(MatchSignal.VERSION_SOFT_DIFFERENCE, detail = versionDetail(medium, candidateTitle.markers))
            limit(84)
            score -= 4
        } else if (medium.isNotEmpty() || labelsDiffer) {
            val detail = versionDetail(medium, candidateTitle.markers).ifBlank { candidateTitle.versionLabels.joinToString("+") }
            reasons += MatchReason(MatchSignal.VERSION_CONFLICT, detail = detail)
            score -= 15
            limit(79)
        }
        if (soft.isNotEmpty()) {
            reasons += MatchReason(MatchSignal.VERSION_SOFT_DIFFERENCE, detail = versionDetail(soft, candidateTitle.markers))
            score -= 4
        }
        if (hard.isEmpty() && medium.isEmpty() && !labelsDiffer) {
            val matched = sourceTitle.markers.filter { it in hardMarkers }
            reasons += MatchReason(MatchSignal.VERSION_MATCH, detail = matched.joinToString("+") { it.name.lowercase(Locale.ROOT) })
            score += 6
        }
        return score
    }

    private fun versionDetail(markers: Collection<TrackVersionMarker>, candidateMarkers: Set<TrackVersionMarker>): String =
        markers.joinToString("+") { marker ->
            (if (marker in candidateMarkers) "+" else "-") + marker.name.lowercase(Locale.ROOT)
        }

    private fun scoreDuration(sourceMs: Long, candidateMs: Long, reasons: MutableList<MatchReason>, limit: (Int) -> Unit): Int {
        if (sourceMs <= 0L || candidateMs <= 0L) {
            reasons += MatchReason(MatchSignal.DURATION_UNKNOWN)
            limit(89)
            return 6
        }
        val delta = candidateMs - sourceMs
        val magnitude = abs(delta)
        return when {
            magnitude <= 2_000L -> 12.also { reasons += MatchReason(MatchSignal.DURATION_CLOSE, delta) }
            magnitude <= 5_000L -> 9.also { reasons += MatchReason(MatchSignal.DURATION_CLOSE, delta) }
            magnitude <= 10_000L -> 4.also { reasons += MatchReason(MatchSignal.DURATION_DRIFT, delta) }
            magnitude <= 20_000L -> {
                reasons += MatchReason(MatchSignal.DURATION_DRIFT, delta)
                limit(84)
                -6
            }
            magnitude <= 45_000L -> {
                reasons += MatchReason(MatchSignal.DURATION_FAR, delta)
                limit(70)
                -18
            }
            else -> {
                reasons += MatchReason(MatchSignal.DURATION_FAR, delta)
                limit(54)
                -30
            }
        }
    }

    private fun scoreAlbum(
        source: ImportedTrackIdentity,
        sourceTitle: TitleIdentity,
        candidate: MatchCandidate,
        candidateTitle: TitleIdentity,
        reasons: MutableList<MatchReason>
    ): Int {
        val expected = MusicIdentityText.album(source.album)
        val actual = MusicIdentityText.album(candidate.album)
        if (expected.core in untrustedAlbums || actual.core in untrustedAlbums) return 0
        if (expected.core == actual.core) {
            val sameEdition = expected.editions == actual.editions
            reasons += MatchReason(if (sameEdition) MatchSignal.ALBUM_SAME else MatchSignal.ALBUM_EDITION)
            return if (sameEdition) 6 else 3
        }
        if (expected.core == sourceTitle.core || actual.core == candidateTitle.core) return 0
        reasons += MatchReason(MatchSignal.ALBUM_DIFFERENT)
        return -3
    }

    private fun normalizedArtists(raw: List<String>): List<String> =
        raw.flatMap { MusicIdentityText.artistNames(it.replace(channelSuffix, "")) }
            .filter(String::isNotBlank)
            .distinct()

    private fun versionIdentity(title: TitleIdentity): Set<String> =
        title.markers.filter { it in hardMarkers || it in mediumMarkers }.map { it.name }.toSet() + versionLabels(title)

    private fun versionLabels(title: TitleIdentity): Set<String> =
        if (TrackVersionMarker.REMIX in title.markers) {
            title.versionLabels.map { label ->
                if (label.endsWith(" mix")) label.removeSuffix(" mix") + " remix" else label
            }.toSet()
        } else title.versionLabels

    private fun titleSimilarity(a: TitleIdentity, b: TitleIdentity): Double {
        if (a.core.isEmpty() || b.core.isEmpty()) return 0.0
        return max(tokenSimilarity(a.core, b.core), containment(a.core, b.core))
    }

    private fun containment(a: String, b: String): Double {
        val shorter = if (a.length <= b.length) a else b
        val longer = if (a.length <= b.length) b else a
        if (shorter.length < 4) return 0.0
        return if (" $longer ".contains(" $shorter ")) 0.7 else 0.0
    }

    private fun spellingVariant(a: String, b: String): Boolean {
        val left = a.replace(" ", "")
        val right = b.replace(" ", "")
        if (min(left.length, right.length) < 8 || abs(left.length - right.length) > 2) return false
        if (left == right) return true
        if (left.any(Char::isDigit) || right.any(Char::isDigit)) return false
        return editDistance(left, right, 2) <= 2
    }

    private fun editDistance(a: String, b: String, limit: Int): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            var rowMin = current[0]
            for (j in b.indices) {
                current[j + 1] = minOf(previous[j + 1] + 1, current[j] + 1, previous[j] + if (a[i] == b[j]) 0 else 1)
                rowMin = min(rowMin, current[j + 1])
            }
            if (rowMin > limit) return rowMin
            previous = current
        }
        return previous[b.length]
    }

    private fun compactEquals(a: String, b: String): Boolean {
        val left = a.replace(" ", "")
        return left.length >= 4 && left == b.replace(" ", "")
    }

    internal fun tokenSimilarity(a: String, b: String): Double {
        val left = a.split(' ').filter(String::isNotBlank).toSet()
        val right = b.split(' ').filter(String::isNotBlank).toSet()
        if (left.isEmpty() || right.isEmpty()) return 0.0
        return 2.0 * left.intersect(right).size / (left.size + right.size).toDouble()
    }

    private fun isShortTitle(core: String): Boolean = core.length < 4 || (!core.contains(' ') && core.length < 6)

    private fun kindRank(kind: CandidateKind): Int = when (kind) {
        CandidateKind.SONG -> 0
        CandidateKind.OFFICIAL_VIDEO -> 1
        CandidateKind.UNKNOWN -> 2
        CandidateKind.USER_VIDEO -> 3
    }
}
