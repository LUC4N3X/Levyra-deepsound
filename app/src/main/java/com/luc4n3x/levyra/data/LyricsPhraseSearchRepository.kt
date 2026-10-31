package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricsPhraseMatcher
import com.luc4n3x.levyra.domain.LyricsPhraseVersionKind
import com.luc4n3x.levyra.domain.Track
import java.util.Collections
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

enum class LyricsPhraseConfidence { EXACT, HIGH, POSSIBLE, UNVERIFIED }

data class LyricsPhraseHit(
    val track: Track,
    val confidence: LyricsPhraseConfidence,
    val score: Float,
    val snippet: String,
    val version: LyricsPhraseVersionKind
)

sealed interface LyricsPhraseOutcome {
    data class Results(val hits: List<LyricsPhraseHit>, val verificationAvailable: Boolean) : LyricsPhraseOutcome
    data object NoMatch : LyricsPhraseOutcome
    data object ProviderUnavailable : LyricsPhraseOutcome
}

internal object LyricsPhraseRanking {
    private const val EXACT_SCORE = 0.93f
    private const val HIGH_SCORE = 0.78f
    private const val MAX_SNIPPET_CHARS = 110
    private const val MIN_OVERLAP_TOKEN_CHARS = 4
    private const val MIN_FRAGMENT_SOURCE_TOKENS = 4

    fun verify(candidate: Track, rank: Int, phrase: String, lyricLines: List<String>?): LyricsPhraseHit? {
        val version = LyricsPhraseMatcher.versionKind(candidate.title, candidate.artist)
        if (lyricLines.isNullOrEmpty()) {
            return LyricsPhraseHit(candidate, LyricsPhraseConfidence.UNVERIFIED, unverifiedScore(rank), "", version)
        }
        val match = LyricsPhraseMatcher.match(phrase, lyricLines)
        if (match == null || match.score < LyricsPhraseMatcher.VERIFIED_SCORE) return null
        val confidence = when {
            match.score >= EXACT_SCORE -> LyricsPhraseConfidence.EXACT
            match.score >= HIGH_SCORE -> LyricsPhraseConfidence.HIGH
            else -> LyricsPhraseConfidence.POSSIBLE
        }
        val snippet = lyricLines.getOrNull(match.lineIndex).orEmpty().trim().take(MAX_SNIPPET_CHARS)
        return LyricsPhraseHit(candidate, confidence, match.score, snippet, version)
    }

    fun isVerified(hit: LyricsPhraseHit): Boolean = hit.confidence != LyricsPhraseConfidence.UNVERIFIED

    fun titleOverlaps(track: Track, phrase: String): Boolean {
        val queryTokens = LyricsPhraseMatcher.tokens(LyricsPhraseMatcher.normalize(phrase))
            .filter { it.length >= MIN_OVERLAP_TOKEN_CHARS || it.first().code > 0x2E7F }
            .toSet()
        val titleTokens = LyricsPhraseMatcher.tokens(LyricsPhraseMatcher.normalize(track.title))
        return titleTokens.any { it in queryTokens }
    }

    fun fragments(phrase: String): List<String> {
        val tokens = LyricsPhraseMatcher.tokens(LyricsPhraseMatcher.normalize(phrase))
        if (tokens.size < MIN_FRAGMENT_SOURCE_TOKENS) return emptyList()
        val half = tokens.size / 2
        return listOf(tokens.take(half + 1).joinToString(" "), tokens.takeLast(half + 1).joinToString(" "))
    }

    fun order(hits: List<LyricsPhraseHit>): List<LyricsPhraseHit> {
        val ranked = hits.sortedWith(
            compareBy<LyricsPhraseHit> { it.confidence.ordinal }
                .thenByDescending { it.score }
                .thenBy { it.version.ordinal }
        )
        val seen = HashSet<String>(ranked.size)
        val anyVerified = ranked.any { it.confidence != LyricsPhraseConfidence.UNVERIFIED }
        return ranked
            .filter { !anyVerified || it.confidence != LyricsPhraseConfidence.UNVERIFIED }
            .filter { hit -> seen.add(LyricsPhraseMatcher.dedupeKey(hit.track.title, hit.track.artist)) }
    }

    private fun unverifiedScore(rank: Int): Float = (0.5f - rank * 0.02f).coerceAtLeast(0.1f)
}

internal class LyricsPhraseSearchRepository(
    private val searchCandidates: suspend (String, Int) -> List<Track>,
    private val fetchLyricLines: suspend (Track) -> List<String>?,
    private val clock: () -> Long = System::currentTimeMillis,
    private val isOnline: () -> Boolean = { true }
) {
    private class CachedOutcome(val outcome: LyricsPhraseOutcome, val storedAtMs: Long)

    private val cache: MutableMap<String, CachedOutcome> = Collections.synchronizedMap(
        object : LinkedHashMap<String, CachedOutcome>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedOutcome>?): Boolean =
                size > MAX_CACHE_ENTRIES
        }
    )
    private val verificationPermits = Semaphore(MAX_PARALLEL_VERIFICATIONS)

    @Volatile private var consecutiveVerificationFailures = 0

    @Volatile private var verificationOpenUntilMs = 0L

    suspend fun search(phrase: String): LyricsPhraseOutcome = withContext(Dispatchers.IO) {
        val normalized = LyricsPhraseMatcher.normalize(phrase)
        cache[normalized]?.let { cached ->
            if (clock() - cached.storedAtMs < CACHE_TTL_MS) return@withContext cached.outcome
        }
        val candidates = candidatesWithRetry(phrase) ?: return@withContext LyricsPhraseOutcome.ProviderUnavailable
        if (candidates.isEmpty() && !isOnline()) return@withContext LyricsPhraseOutcome.ProviderUnavailable
        val seen = HashSet<String>()
        var pass = verifyPass(candidates, phrase, seen)
        var hits = pass.hits
        if (hits.none(LyricsPhraseRanking::isVerified)) {
            for (fragment in LyricsPhraseRanking.fragments(phrase)) {
                val more = candidatesWithRetry(fragment)
                val fragmentPass = verifyPass(more.orEmpty(), phrase, seen)
                pass = VerificationPass(fragmentPass.hits, pass.completed && fragmentPass.completed && more != null)
                hits = hits + pass.hits
                if (pass.hits.any(LyricsPhraseRanking::isVerified)) break
            }
        }
        val plausible = hits.filter { LyricsPhraseRanking.isVerified(it) || LyricsPhraseRanking.titleOverlaps(it.track, phrase) }
        val ordered = LyricsPhraseRanking.order(plausible).take(MAX_RESULTS)
        val outcome = if (ordered.isEmpty()) {
            LyricsPhraseOutcome.NoMatch
        } else {
            LyricsPhraseOutcome.Results(ordered, ordered.any(LyricsPhraseRanking::isVerified))
        }
        if (pass.completed && clock() >= verificationOpenUntilMs) {
            cache[normalized] = CachedOutcome(outcome, clock())
        }
        outcome
    }

    private class VerificationPass(val hits: List<LyricsPhraseHit>, val completed: Boolean)

    private suspend fun verifyPass(candidates: List<Track>, phrase: String, seen: MutableSet<String>): VerificationPass {
        val distinct = candidates
            .filter { seen.add(LyricsPhraseMatcher.dedupeKey(it.title, it.artist)) }
            .take(MAX_VERIFIED_CANDIDATES)
        if (distinct.isEmpty()) return VerificationPass(emptyList(), true)
        val verified = withTimeoutOrNull(TOTAL_VERIFICATION_TIMEOUT_MS) {
            coroutineScope {
                distinct.mapIndexed { rank, candidate ->
                    async {
                        val lines = verificationPermits.withPermit { linesFor(candidate) }
                        LyricsPhraseRanking.verify(candidate, rank, phrase, lines)
                    }
                }.awaitAll().filterNotNull()
            }
        }
        if (verified != null) return VerificationPass(verified, true)
        val unchecked = distinct.take(MAX_UNVERIFIED_FALLBACK).mapIndexedNotNull { rank, candidate ->
            LyricsPhraseRanking.verify(candidate, rank, phrase, null)
        }
        return VerificationPass(unchecked, false)
    }

    private suspend fun candidatesWithRetry(phrase: String): List<Track>? {
        repeat(CANDIDATE_ATTEMPTS) { attempt ->
            val result = try {
                withTimeoutOrNull(CANDIDATE_TIMEOUT_MS) { searchCandidates(phrase, CANDIDATE_LIMIT) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.d(error, "lyrics phrase candidate search failed, attempt %d", attempt)
                null
            }
            if (result != null) return result
        }
        return null
    }

    private suspend fun linesFor(candidate: Track): List<String>? {
        if (clock() < verificationOpenUntilMs) return null
        val lines = try {
            withTimeoutOrNull(VERIFICATION_TIMEOUT_MS) { fetchLyricLines(candidate) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.d(error, "lyrics phrase verification failed for %s", candidate.id)
            null
        }
        if (lines.isNullOrEmpty()) {
            consecutiveVerificationFailures += 1
            if (consecutiveVerificationFailures >= CIRCUIT_FAILURE_THRESHOLD) {
                verificationOpenUntilMs = clock() + CIRCUIT_OPEN_MS
                consecutiveVerificationFailures = 0
            }
        } else {
            consecutiveVerificationFailures = 0
        }
        return lines
    }

    private companion object {
        const val CANDIDATE_LIMIT = 16
        const val CANDIDATE_ATTEMPTS = 2
        const val CANDIDATE_TIMEOUT_MS = 8_000L
        const val MAX_VERIFIED_CANDIDATES = 10
        const val MAX_UNVERIFIED_FALLBACK = 6
        const val MAX_RESULTS = 12
        const val MAX_PARALLEL_VERIFICATIONS = 3
        const val VERIFICATION_TIMEOUT_MS = 7_000L
        const val TOTAL_VERIFICATION_TIMEOUT_MS = 18_000L
        const val CIRCUIT_FAILURE_THRESHOLD = 6
        const val CIRCUIT_OPEN_MS = 60_000L
        const val MAX_CACHE_ENTRIES = 32
        const val CACHE_TTL_MS = 20L * 60L * 1000L
    }
}
