package com.luc4n3x.levyra.nexus.playlistimport

import kotlin.math.abs

enum class ImportEntryStatus { PENDING, MATCHED, REVIEW, MISSING, SKIPPED }

enum class ImportChoiceOrigin { AUTOMATIC, USER }

enum class ImportFlag { DUPLICATE_SOURCE, COLLISION, SUSPICIOUS_DURATION, AMBIGUOUS, UNAVAILABLE, HEALED }

enum class ImportReviewFilter { ALL, MATCHED, REVIEW, MISSING, DUPLICATES }

data class ImportEntry(
    val identity: ImportedTrackIdentity,
    val resolved: Boolean = false,
    val alternatives: List<MatchEvaluation> = emptyList(),
    val selected: MatchEvaluation? = null,
    val confidence: MatchConfidence = MatchConfidence.UNRESOLVED,
    val automaticId: String? = null,
    val automaticConfidence: MatchConfidence = MatchConfidence.UNRESOLVED,
    val origin: ImportChoiceOrigin = ImportChoiceOrigin.AUTOMATIC,
    val skipped: Boolean = false,
    val flags: Set<ImportFlag> = emptySet(),
    val duplicateGroup: Int = -1
) {
    val status: ImportEntryStatus
        get() = when {
            skipped -> ImportEntryStatus.SKIPPED
            !resolved -> ImportEntryStatus.PENDING
            selected == null -> ImportEntryStatus.MISSING
            origin == ImportChoiceOrigin.USER -> ImportEntryStatus.MATCHED
            !confidence.autoAccepted || flags.any { it in ATTENTION_FLAGS } -> ImportEntryStatus.REVIEW
            else -> ImportEntryStatus.MATCHED
        }

    companion object {
        val ATTENTION_FLAGS = setOf(ImportFlag.COLLISION, ImportFlag.SUSPICIOUS_DURATION, ImportFlag.AMBIGUOUS, ImportFlag.UNAVAILABLE)
    }
}

data class ImportReviewCounts(
    val total: Int = 0,
    val matched: Int = 0,
    val review: Int = 0,
    val missing: Int = 0,
    val skipped: Int = 0,
    val pending: Int = 0,
    val duplicates: Int = 0,
    val manual: Int = 0,
    val local: Int = 0,
    val online: Int = 0,
    val ready: Int = 0,
    val mergedRepeats: Int = 0
) {
    val matchPercent: Int
        get() = if (total == 0) 0 else (matched + review) * 100 / total
}

object PlaylistImportReview {
    fun applyOutcome(entry: ImportEntry, outcome: MatchOutcome): ImportEntry {
        if (entry.origin == ImportChoiceOrigin.USER || entry.skipped) {
            return entry.copy(resolved = true, alternatives = mergeAlternatives(entry.alternatives, outcome.alternatives))
        }
        val flags = entry.flags - ImportFlag.AMBIGUOUS + if (outcome.ambiguous) setOf(ImportFlag.AMBIGUOUS) else emptySet()
        return entry.copy(
            resolved = true,
            alternatives = outcome.alternatives,
            selected = outcome.selected,
            confidence = outcome.confidence,
            automaticId = outcome.selected?.candidate?.id,
            automaticConfidence = outcome.confidence,
            flags = flags
        )
    }

    fun choose(entry: ImportEntry, candidateId: String): ImportEntry {
        val evaluation = entry.alternatives.firstOrNull { it.candidate.id == candidateId } ?: return entry
        return entry.copy(selected = evaluation, confidence = evaluation.confidence, origin = ImportChoiceOrigin.USER, skipped = false)
    }

    fun chooseManual(entry: ImportEntry, candidate: MatchCandidate): ImportEntry {
        val evaluation = PlaylistMatchEngine.evaluate(entry.identity, candidate)
        val alternatives = listOf(evaluation) + entry.alternatives.filter { it.candidate.id != candidate.id }
        return entry.copy(
            resolved = true,
            alternatives = alternatives.take(PlaylistMatchEngine.MAX_ALTERNATIVES + 1),
            selected = evaluation,
            confidence = evaluation.confidence,
            origin = ImportChoiceOrigin.USER,
            skipped = false
        )
    }

    fun skip(entry: ImportEntry): ImportEntry = entry.copy(skipped = true)

    fun acceptSuggestion(entry: ImportEntry): ImportEntry =
        if (entry.selected == null || entry.skipped) entry else entry.copy(origin = ImportChoiceOrigin.USER)

    fun restoreAutomatic(entry: ImportEntry): ImportEntry {
        val automatic = entry.alternatives.firstOrNull { it.candidate.id == entry.automaticId }
        return entry.copy(
            selected = automatic,
            confidence = if (automatic == null) MatchConfidence.UNRESOLVED else entry.automaticConfidence,
            origin = ImportChoiceOrigin.AUTOMATIC,
            skipped = false
        )
    }

    fun acceptAllSuggestions(entries: List<ImportEntry>): List<ImportEntry> =
        entries.map { if (it.status == ImportEntryStatus.REVIEW) acceptSuggestion(it) else it }

    fun matches(entry: ImportEntry, filter: ImportReviewFilter): Boolean = when (filter) {
        ImportReviewFilter.ALL -> true
        ImportReviewFilter.MATCHED -> entry.status == ImportEntryStatus.MATCHED
        ImportReviewFilter.REVIEW -> entry.status == ImportEntryStatus.REVIEW
        ImportReviewFilter.MISSING -> entry.status == ImportEntryStatus.MISSING || entry.status == ImportEntryStatus.SKIPPED
        ImportReviewFilter.DUPLICATES -> ImportFlag.DUPLICATE_SOURCE in entry.flags || ImportFlag.COLLISION in entry.flags
    }

    fun counts(entries: List<ImportEntry>): ImportReviewCounts {
        var matched = 0
        var review = 0
        var missing = 0
        var skipped = 0
        var pending = 0
        var duplicates = 0
        var manual = 0
        var local = 0
        var online = 0
        entries.forEach { entry ->
            when (entry.status) {
                ImportEntryStatus.MATCHED -> {
                    matched++
                    if (entry.selected?.candidate?.origin == CandidateOrigin.LOCAL) local++ else online++
                }
                ImportEntryStatus.REVIEW -> review++
                ImportEntryStatus.MISSING -> missing++
                ImportEntryStatus.SKIPPED -> skipped++
                ImportEntryStatus.PENDING -> pending++
            }
            if (ImportFlag.DUPLICATE_SOURCE in entry.flags) duplicates++
            if (entry.origin == ImportChoiceOrigin.USER && !entry.skipped && entry.selected?.candidate?.id != entry.automaticId) manual++
        }
        val ready = commitSelection(entries).size
        return ImportReviewCounts(
            total = entries.size,
            matched = matched,
            review = review,
            missing = missing,
            skipped = skipped,
            pending = pending,
            duplicates = duplicates,
            manual = manual,
            local = local,
            online = online,
            ready = ready,
            mergedRepeats = 0
        )
    }

    fun commitSelection(entries: List<ImportEntry>): List<MatchCandidate> = entries
        .asSequence()
        .filter { it.status == ImportEntryStatus.MATCHED }
        .mapNotNull { it.selected?.candidate }
        .toList()

    private fun mergeAlternatives(current: List<MatchEvaluation>, fresh: List<MatchEvaluation>): List<MatchEvaluation> =
        (current + fresh).distinctBy { it.candidate.id }.take(PlaylistMatchEngine.MAX_ALTERNATIVES + 1)
}

data class PlaylistHealResult(
    val entries: List<ImportEntry>,
    val reSearchPositions: Set<Int>,
    val healedPositions: Set<Int>
)

object PlaylistImportHealer {
    private const val SUSPICIOUS_DELTA_MS = 10_000L
    private val recomputedFlags = setOf(ImportFlag.DUPLICATE_SOURCE, ImportFlag.COLLISION, ImportFlag.SUSPICIOUS_DURATION, ImportFlag.UNAVAILABLE)

    fun heal(input: List<ImportEntry>): PlaylistHealResult {
        val entries = input.map { it.copy(flags = it.flags - recomputedFlags, duplicateGroup = -1) }.toMutableList()
        val keys = entries.map { PlaylistMatchEngine.sourceKey(it.identity) }

        keys.withIndex().groupBy({ it.value }, { it.index }).values.filter { it.size > 1 }.forEach { group ->
            val groupId = group.first()
            group.forEach { index ->
                entries[index] = entries[index].copy(flags = entries[index].flags + ImportFlag.DUPLICATE_SOURCE, duplicateGroup = groupId)
            }
        }

        val healed = HashSet<Int>()
        val taken = entries.mapNotNullTo(HashSet()) { entry -> entry.selected?.candidate?.id?.takeUnless { entry.skipped } }
        entries.indices
            .filter { entries[it].selected != null && !entries[it].skipped }
            .groupBy { entries[it].selected!!.candidate.id }
            .values
            .filter { group -> group.map { keys[it] }.distinct().size > 1 }
            .forEach { group ->
                val byKey = group.groupBy { keys[it] }
                val keeperKey = byKey.maxByOrNull { (_, indexes) ->
                    indexes.maxOf { index ->
                        val entry = entries[index]
                        if (entry.origin == ImportChoiceOrigin.USER) Int.MAX_VALUE else entry.selected!!.score
                    }
                }!!.key
                byKey.filterKeys { it != keeperKey }.values.flatten().forEach { index ->
                    val entry = entries[index]
                    if (entry.origin == ImportChoiceOrigin.USER) {
                        entries[index] = entry.copy(flags = entry.flags + ImportFlag.COLLISION)
                        return@forEach
                    }
                    val replacement = entry.alternatives.firstOrNull { alternative ->
                        alternative.candidate.id !in taken && alternative.confidence.autoAccepted
                    }
                    entries[index] = if (replacement != null) {
                        taken += replacement.candidate.id
                        healed += index
                        entry.copy(
                            selected = replacement,
                            confidence = replacement.confidence,
                            automaticId = replacement.candidate.id,
                            automaticConfidence = replacement.confidence,
                            flags = entry.flags + ImportFlag.HEALED
                        )
                    } else {
                        entry.copy(flags = entry.flags + ImportFlag.COLLISION)
                    }
                }
            }

        entries.indices.forEach { index ->
            val entry = entries[index]
            val selected = entry.selected ?: return@forEach
            var flags = entry.flags
            if (!selected.candidate.available) flags = flags + ImportFlag.UNAVAILABLE
            val drift = selected.reasons.firstOrNull {
                it.signal == MatchSignal.DURATION_DRIFT || it.signal == MatchSignal.DURATION_FAR
            }
            if (entry.origin == ImportChoiceOrigin.AUTOMATIC && drift != null && abs(drift.deltaMs) > SUSPICIOUS_DELTA_MS) {
                flags = flags + ImportFlag.SUSPICIOUS_DURATION
            }
            if (flags != entry.flags) entries[index] = entry.copy(flags = flags)
        }

        val reSearch = entries.indices.filterTo(HashSet()) { index ->
            val entry = entries[index]
            entry.resolved && !entry.skipped && entry.selected == null && entry.origin == ImportChoiceOrigin.AUTOMATIC &&
                entry.alternatives.none { it.confidence != MatchConfidence.UNRESOLVED }
        }
        return PlaylistHealResult(entries, reSearch, healed)
    }
}
