package com.luc4n3x.levyra.domain

import java.text.BreakIterator
import java.text.Normalizer
import java.util.Locale

internal data class ArtistBiographyEditorial(
    val summary: String,
    val paragraphs: List<String>,
    val hasMore: Boolean
) {
    val fullText: String
        get() = paragraphs.joinToString("\n\n")
}

internal fun artistBiographyEditorial(
    text: String,
    description: String = "",
    languageCode: String = ""
): ArtistBiographyEditorial {
    val sourceText = text.trim().ifBlank { description.trim() }
    val paragraphs = biographyParagraphs(sourceText, languageCode)
    val fullText = paragraphs.joinToString("\n\n")
    if (fullText.isBlank()) {
        return ArtistBiographyEditorial(
            summary = "",
            paragraphs = emptyList(),
            hasMore = false
        )
    }

    val sentences = paragraphs.flatMap { paragraph ->
        biographySentences(paragraph, languageCode)
    }.distinctBy { normalizeBiographyKey(it) }

    val selected = LinkedHashSet<Int>()
    if (sentences.isNotEmpty()) selected += 0
    if (sentences.size > 1 && sentences[0].length + sentences[1].length <= 470) selected += 1

    val ranked = sentences.indices
        .filterNot(selected::contains)
        .map { index -> index to biographySentenceScore(sentences[index], index) }
        .sortedWith(compareByDescending<Pair<Int, Int>> { it.second }.thenBy { it.first })

    var summaryLength = selected.sumOf { sentences[it].length + 1 }
    for ((index, score) in ranked) {
        if (score <= 0 || selected.size >= MAX_SUMMARY_SENTENCES) continue
        val candidateLength = sentences[index].length + if (summaryLength > 0) 1 else 0
        if (summaryLength + candidateLength > MAX_SUMMARY_LENGTH && summaryLength >= MIN_SUMMARY_LENGTH) continue
        selected += index
        summaryLength += candidateLength
        if (summaryLength >= TARGET_SUMMARY_LENGTH && selected.size >= MIN_SUMMARY_SENTENCES) break
    }

    if (summaryLength < MIN_SUMMARY_LENGTH) {
        for (index in sentences.indices) {
            if (index in selected) continue
            val candidateLength = sentences[index].length + if (summaryLength > 0) 1 else 0
            if (summaryLength + candidateLength > MAX_SUMMARY_LENGTH && summaryLength > 0) break
            selected += index
            summaryLength += candidateLength
            if (summaryLength >= MIN_SUMMARY_LENGTH) break
        }
    }

    val summary = selected
        .sorted()
        .joinToString(" ") { sentences[it] }
        .trim()
        .ifBlank { paragraphs.first() }

    return ArtistBiographyEditorial(
        summary = summary,
        paragraphs = paragraphs,
        hasMore = fullText.length > summary.length + MORE_TEXT_THRESHOLD
    )
}

private fun biographyParagraphs(value: String, languageCode: String): List<String> {
    val normalized = value
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace(Regex("[ \t]+"), " ")
        .replace(Regex("\n[ \t]+"), "\n")
        .trim()
    if (normalized.isBlank()) return emptyList()

    val explicit = normalized
        .split(Regex("""\n\s*\n+"""))
        .map(::completeBiographyText)
        .filter { it.isNotBlank() }
    val sources = if (explicit.size >= 2) explicit else listOf(completeBiographyText(normalized))
    return sources
        .flatMap { source -> boundedBiographyParagraphs(source, languageCode) }
        .filter { it.length >= MIN_PARAGRAPH_LENGTH }
        .ifEmpty {
            boundedBiographyParagraphs(completeBiographyText(normalized), languageCode)
                .filter { it.isNotBlank() }
        }
}

private fun boundedBiographyParagraphs(value: String, languageCode: String): List<String> {
    val clean = completeBiographyText(value)
    if (clean.isBlank()) return emptyList()
    val sentences = biographySentences(clean, languageCode)
        .flatMap { sentence -> splitOversizedBiographySentence(sentence) }
    if (sentences.isEmpty()) return splitOversizedBiographySentence(clean)

    return buildList {
        val current = StringBuilder()
        for (sentence in sentences) {
            if (current.isNotEmpty() && current.length + sentence.length + 1 > MAX_PARAGRAPH_LENGTH) {
                add(current.toString())
                current.clear()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
        }
        if (current.isNotEmpty()) add(current.toString())
    }
}

private fun splitOversizedBiographySentence(value: String): List<String> {
    var remaining = value.trim()
    if (remaining.isBlank()) return emptyList()
    val chunks = ArrayList<String>()
    while (remaining.length > MAX_PARAGRAPH_LENGTH) {
        val safeLimit = surrogateSafeIndex(remaining, MAX_PARAGRAPH_LENGTH)
        val minimumBoundary = (safeLimit * MIN_BOUNDARY_RATIO).toInt()
        var cut = -1
        for (index in safeLimit downTo minimumBoundary) {
            val previous = remaining[index - 1]
            if (Character.isWhitespace(previous) || previous in SOFT_BREAK_PUNCTUATION) {
                cut = index
                break
            }
        }
        if (cut <= 0) cut = safeLimit
        val chunk = remaining.substring(0, cut).trim()
        if (chunk.isNotBlank()) chunks += chunk
        remaining = remaining.substring(cut).trimStart()
    }
    if (remaining.isNotBlank()) chunks += remaining
    return chunks
}

private fun surrogateSafeIndex(value: String, requestedIndex: Int): Int {
    var index = requestedIndex.coerceIn(1, value.length)
    if (index < value.length && Character.isLowSurrogate(value[index]) && Character.isHighSurrogate(value[index - 1])) {
        index--
    }
    return index.coerceAtLeast(1)
}

private fun biographySentences(value: String, languageCode: String): List<String> {
    val clean = completeBiographyText(value)
    if (clean.isBlank()) return emptyList()
    val locale = languageCode
        .substringBefore('-')
        .takeIf { it.length in 2..3 }
        ?.let(Locale::forLanguageTag)
        ?: Locale.ROOT
    val iterator = BreakIterator.getSentenceInstance(locale)
    iterator.setText(clean)
    val result = ArrayList<String>()
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        val sentence = clean.substring(start, end)
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (sentence.length >= MIN_SENTENCE_LENGTH) result += sentence
        start = end
        end = iterator.next()
    }
    if (result.isEmpty() && clean.isNotBlank()) result += clean
    return result
}

private fun completeBiographyText(value: String): String {
    val clean = value
        .replace(Regex("""\s+"""), " ")
        .trim()
    if (clean.isBlank()) return ""
    if (clean.last() in TERMINAL_PUNCTUATION) return clean
    val boundary = clean.indices
        .filter { clean[it] in TERMINAL_PUNCTUATION }
        .lastOrNull { it >= (clean.length * MIN_COMPLETE_TAIL_RATIO).toInt() }
    return boundary?.let { clean.substring(0, it + 1).trim() } ?: clean
}

private fun biographySentenceScore(sentence: String, index: Int): Int {
    val normalized = normalizeBiographyKey(sentence)
    var score = 0
    if (YEAR_PATTERN.containsMatchIn(sentence)) score += 24
    score += CAREER_TERMS.count { normalized.contains(it) } * 24
    score += RELEASE_TERMS.count { normalized.contains(it) } * 20
    score += ACHIEVEMENT_TERMS.count { normalized.contains(it) } * 28
    if (sentence.length in 70..300) score += 18
    if (sentence.length > 430) score -= 20
    score -= index.coerceAtMost(20)
    return score
}

private fun normalizeBiographyKey(value: String): String {
    return Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("""\p{Mn}+"""), "")
        .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private const val MIN_SENTENCE_LENGTH = 28
private const val MIN_PARAGRAPH_LENGTH = 24
private const val MAX_PARAGRAPH_LENGTH = 520
private const val MIN_SUMMARY_SENTENCES = 3
private const val MAX_SUMMARY_SENTENCES = 6
private const val MIN_SUMMARY_LENGTH = 430
private const val TARGET_SUMMARY_LENGTH = 760
private const val MAX_SUMMARY_LENGTH = 980
private const val MORE_TEXT_THRESHOLD = 120
private const val MIN_COMPLETE_TAIL_RATIO = 0.55
private const val MIN_BOUNDARY_RATIO = 0.62
private val TERMINAL_PUNCTUATION = setOf('.', '!', '?', '…', '。', '！', '？', '؟', '۔')
private val SOFT_BREAK_PUNCTUATION = setOf(',', ';', ':', '،', '؛', '、', '，', '；', '：')
private val YEAR_PATTERN = Regex("""\b(?:18|19|20)\d{2}\b""")
private val CAREER_TERMS = setOf(
    "carriera", "debutto", "esordio", "notorieta", "successo", "collabor", "fondato", "formato", "career", "debut", "breakthrough", "success", "collabor", "founded", "formed"
)
private val RELEASE_TERMS = setOf(
    "album", "singolo", "brano", "disco", "ep", "pubblicato", "uscito", "classifica", "album", "single", "song", "record", "released", "chart"
)
private val ACHIEVEMENT_TERMS = setOf(
    "premio", "vinto", "festival", "sanremo", "certificato", "platino", "oro", "riconoscimento", "award", "won", "festival", "certified", "platinum", "gold", "nomination"
)
