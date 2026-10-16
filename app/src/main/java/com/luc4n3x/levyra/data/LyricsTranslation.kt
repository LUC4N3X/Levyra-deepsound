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
    data class Success(val texts: List<String?>) : LyricsBatchTranslation
    data object SameLanguage : LyricsBatchTranslation
    data class Unavailable(val state: LyricsTranslationState) : LyricsBatchTranslation
}

internal interface LyricsTranslationBackend {
    val version: String

    suspend fun translate(
        texts: List<String>,
        targetLanguageTag: String
    ): LyricsBatchTranslation
}

internal data class LyricsTranslationOutcome(
    val lines: List<LyricLine>,
    val state: LyricsTranslationState
)

internal class LyricsTranslationCoordinator(
    private val backend: LyricsTranslationBackend?,
    private val timeoutMs: Long = TRANSLATION_TIMEOUT_MS
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
        if (eligibleIndices.isEmpty()) {
            return LyricsTranslationOutcome(lines, LyricsTranslationState.SAME_LANGUAGE)
        }
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
        val sources = eligibleIndices.map { index -> lines[index].text.trim() }
        val result = try {
            withTimeoutOrNull(timeoutMs) {
                translateCompletely(translator, sources, target)
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
                val translations = eligibleIndices.zip(result.texts)
                    .mapNotNull { (index, text) -> text?.let { index to it } }
                    .toMap()
                synchronized(cache) { cache[cacheKey] = translations }
                LyricsTranslationOutcome(applyTranslations(lines, translations), LyricsTranslationState.ON_DEVICE)
            }
        }
    }

    private suspend fun translateCompletely(
        translator: LyricsTranslationBackend,
        sources: List<String>,
        target: String
    ): LyricsBatchTranslation {
        val first = translator.translate(sources, target)
        if (first !is LyricsBatchTranslation.Success) return first
        if (first.texts.size != sources.size) return LyricsBatchTranslation.Unavailable(LyricsTranslationState.FAILED)
        val merged = first.texts.map(::normalizeTranslatedLine).toMutableList()
        val missing = merged.indices.filter { merged[it] == null }
        if (missing.isNotEmpty()) {
            val retry = translator.translate(missing.map(sources::get), target)
            if (retry is LyricsBatchTranslation.Success && retry.texts.size == missing.size) {
                missing.zip(retry.texts).forEach { (position, text) ->
                    merged[position] = normalizeTranslatedLine(text)
                }
            }
        }
        val stillMissing = merged.count { it == null }
        if (stillMissing > merged.size / MAX_UNTRANSLATED_LINE_RATIO) {
            return LyricsBatchTranslation.Unavailable(LyricsTranslationState.FAILED)
        }
        return LyricsBatchTranslation.Success(merged)
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

    companion object {
        private const val CACHE_SIZE = 24
        private const val TRANSLATION_TIMEOUT_MS = 120_000L
        private const val MAX_UNTRANSLATED_LINE_RATIO = 10
        private val WHITESPACE = Regex("\\s+")

        private fun normalizeTranslatedLine(text: String?): String? =
            text?.replace(WHITESPACE, " ")?.trim()?.takeIf(String::isNotEmpty)
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
