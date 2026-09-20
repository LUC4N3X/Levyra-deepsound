package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricsTranslationState
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.LinkedHashMap
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

internal sealed interface LyricsBatchTranslation {
    data class Success(val batches: List<String>) : LyricsBatchTranslation
    data object SameLanguage : LyricsBatchTranslation
    data class Unavailable(val state: LyricsTranslationState) : LyricsBatchTranslation
}

internal interface LyricsTranslationBackend {
    val version: String

    suspend fun translate(
        batches: List<String>,
        targetLanguageTag: String
    ): LyricsBatchTranslation
}

internal data class LyricsTranslationOutcome(
    val lines: List<LyricLine>,
    val state: LyricsTranslationState
)

internal class LyricsTranslationCoordinator(
    private val backend: LyricsTranslationBackend?
) {
    private val cache = object : LinkedHashMap<String, Map<Int, String>>(CACHE_SIZE + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Map<Int, String>>?): Boolean =
            size > CACHE_SIZE
    }

    suspend fun translate(
        lines: List<LyricLine>,
        targetLanguageTag: String
    ): LyricsTranslationOutcome {
        val eligibleIndices = lines.indices.filter { index -> lines[index].isTranslationEligible() }
        if (eligibleIndices.isEmpty()) return LyricsTranslationOutcome(lines, LyricsTranslationState.UNAVAILABLE)
        if (eligibleIndices.all { lines[it].translated.isNotBlank() }) {
            return LyricsTranslationOutcome(lines, LyricsTranslationState.PROVIDER)
        }
        val translator = backend ?: return providerOr(lines, LyricsTranslationState.UNAVAILABLE)
        val target = targetLanguageTag.trim()
        if (target.isBlank()) return providerOr(lines, LyricsTranslationState.UNAVAILABLE)
        val cacheKey = cacheKey(lines, target, translator.version)
        synchronized(cache) { cache[cacheKey] }?.let { cached ->
            return LyricsTranslationOutcome(applyTranslations(lines, cached), LyricsTranslationState.ON_DEVICE)
        }
        val requests = eligibleIndices.chunked(LINES_PER_BATCH).map { indices ->
            TranslationBatch(
                indices = indices,
                payload = indices.joinToString("\n") { index ->
                    "${marker(index)} ${lines[index].text.trim()}"
                }
            )
        }
        val result = try {
            withTimeoutOrNull(TRANSLATION_TIMEOUT_MS) {
                translator.translate(requests.map(TranslationBatch::payload), target)
            } ?: LyricsBatchTranslation.Unavailable(LyricsTranslationState.FAILED)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            LyricsBatchTranslation.Unavailable(LyricsTranslationState.FAILED)
        }
        return when (result) {
            LyricsBatchTranslation.SameLanguage -> LyricsTranslationOutcome(lines, LyricsTranslationState.SAME_LANGUAGE)
            is LyricsBatchTranslation.Unavailable -> providerOr(lines, result.state)
            is LyricsBatchTranslation.Success -> {
                val translations = parseTranslations(requests, result.batches)
                    ?: return providerOr(lines, LyricsTranslationState.FAILED)
                synchronized(cache) { cache[cacheKey] = translations }
                LyricsTranslationOutcome(applyTranslations(lines, translations), LyricsTranslationState.ON_DEVICE)
            }
        }
    }

    private fun parseTranslations(
        requests: List<TranslationBatch>,
        translatedBatches: List<String>
    ): Map<Int, String>? {
        if (translatedBatches.size != requests.size) return null
        val translations = HashMap<Int, String>()
        requests.zip(translatedBatches).forEach { (request, translated) ->
            val translatedLines = translated.lineSequence().filter(String::isNotBlank).toList()
            if (translatedLines.size != request.indices.size) return null
            request.indices.zip(translatedLines).forEach { (expectedIndex, translatedLine) ->
                val match = LINE_MARKER.matchEntire(translatedLine.trim()) ?: return null
                val actualIndex = match.groupValues[1].toIntOrNull() ?: return null
                val text = match.groupValues[2].trim()
                if (actualIndex != expectedIndex || text.isBlank()) return null
                translations[expectedIndex] = text
            }
        }
        if (translations.size != requests.sumOf { it.indices.size }) return null
        return translations
    }

    private fun applyTranslations(
        original: List<LyricLine>,
        translations: Map<Int, String>
    ): List<LyricLine> {
        return original.mapIndexed { index, line ->
            val translated = translations[index]
            if (translated == null || line.translated.isNotBlank()) line else line.copy(translated = translated)
        }
    }

    private fun providerOr(lines: List<LyricLine>, fallback: LyricsTranslationState): LyricsTranslationOutcome {
        val eligible = lines.filter(LyricLine::isTranslationEligible)
        val providerTranslationComplete = eligible.isNotEmpty() && eligible.all { it.translated.isNotBlank() }
        return LyricsTranslationOutcome(
            lines = lines,
            state = if (providerTranslationComplete) LyricsTranslationState.PROVIDER else fallback
        )
    }

    private fun cacheKey(lines: List<LyricLine>, targetLanguageTag: String, version: String): String {
        val content = lines.joinToString("\n") { line ->
            "${line.role}:${line.isInstrumental}:${line.isMetadata}:${line.text.trim()}"
        }
        val seed = "$version|${targetLanguageTag.lowercase(Locale.ROOT)}|$content"
        return MessageDigest.getInstance("SHA-256")
            .digest(seed.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private data class TranslationBatch(
        val indices: List<Int>,
        val payload: String
    )

    companion object {
        private const val CACHE_SIZE = 24
        private const val LINES_PER_BATCH = 6
        private const val TRANSLATION_TIMEOUT_MS = 120_000L
        private val LINE_MARKER = Regex("^__LEVYRA_LINE_(\\d+)__\\s+(.+)$")

        private fun marker(index: Int): String = "__LEVYRA_LINE_${index}__"
    }
}

private fun LyricLine.isTranslationEligible(): Boolean =
    !isMetadata && !isInstrumental && text.isNotBlank()

internal fun needsLyricsTranslationRetry(
    state: LyricsTranslationState,
    translationEnabled: Boolean
): Boolean {
    if (!translationEnabled) return false
    return when (state) {
        LyricsTranslationState.DISABLED,
        LyricsTranslationState.PENDING,
        LyricsTranslationState.MODEL_DOWNLOAD_REQUIRED,
        LyricsTranslationState.MODEL_DOWNLOADING,
        LyricsTranslationState.UNAVAILABLE,
        LyricsTranslationState.FAILED -> true
        LyricsTranslationState.PROVIDER,
        LyricsTranslationState.ON_DEVICE,
        LyricsTranslationState.SAME_LANGUAGE -> false
    }
}
