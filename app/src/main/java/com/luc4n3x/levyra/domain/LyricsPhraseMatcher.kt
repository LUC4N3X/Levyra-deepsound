package com.luc4n3x.levyra.domain

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

enum class LyricsPhraseVersionKind { ORIGINAL, REMIX, COVER, LIVE, KARAOKE }

data class LyricsPhraseMatch(
    val score: Float,
    val lineIndex: Int,
    val matchedWords: Int,
    val totalWords: Int
)

object LyricsPhraseMatcher {
    const val MIN_QUERY_LETTERS = 6
    const val VERIFIED_SCORE = 0.62f
    private const val MAX_LINE_WINDOW = 4
    private const val MAX_QUERY_TOKENS = 40
    private const val MAX_TOKEN_LENGTH_FOR_EDIT = 32

    private val combiningMarks = Regex("""\p{Mn}+""")
    private val nonWord = Regex("""[^\p{L}\p{M}\p{N}\s]+""")
    private val whitespace = Regex("""\s+""")
    private val apostrophes = Regex("""['’ʼ`´]""")
    private val englishContractions = listOf(
        Regex("""(?i)\b(can)['’]t\b""") to "$1 not",
        Regex("""(?i)\bwon['’]t\b""") to "will not",
        Regex("""(?i)n['’]t\b""") to " not",
        Regex("""(?i)['’]ve\b""") to " have",
        Regex("""(?i)['’]re\b""") to " are",
        Regex("""(?i)['’]ll\b""") to " will",
        Regex("""(?i)\bi['’]m\b""") to "i am"
    )
    private val remixTitle =Regex("""\b(remix|rmx|bootleg|mashup|edit)\b""", RegexOption.IGNORE_CASE)
    private val coverTitle = Regex("""\b(cover|tribute|version by)\b""", RegexOption.IGNORE_CASE)
    private val liveTitle = Regex("""\b(live|unplugged|acoustic session)\b""", RegexOption.IGNORE_CASE)
    private val karaokeTitle = Regex("""\b(karaoke|instrumental|backing track)\b""", RegexOption.IGNORE_CASE)

    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFKD)
        val noMarks = if (requiresMarks(decomposed)) decomposed else combiningMarks.replace(decomposed, "")
        val noApostrophes = apostrophes.replace(expandContractions(noMarks), "")
        return whitespace.replace(nonWord.replace(noApostrophes, " "), " ").trim().lowercase()
    }

    private fun expandContractions(text: String): String =
        englishContractions.fold(text) { current, (pattern, replacement) -> pattern.replace(current, replacement) }

    fun tokens(normalized: String): List<String> {
        if (normalized.isEmpty()) return emptyList()
        val tokens = ArrayList<String>()
        for (word in normalized.split(' ')) {
            if (word.isEmpty()) continue
            if (word.any(::isUnspacedScript)) {
                val run = StringBuilder()
                for (char in word) {
                    if (isUnspacedScript(char)) {
                        if (run.isNotEmpty()) {
                            tokens += run.toString()
                            run.clear()
                        }
                        tokens += char.toString()
                    } else {
                        run.append(char)
                    }
                }
                if (run.isNotEmpty()) tokens += run.toString()
            } else {
                tokens += word
            }
        }
        return tokens
    }

    fun isSearchable(query: String): Boolean {
        val normalized = normalize(query)
        val letters = normalized.count { !it.isWhitespace() }
        val hasUnspaced = normalized.any(::isUnspacedScript)
        return if (hasUnspaced) letters >= 3 else letters >= MIN_QUERY_LETTERS && tokens(normalized).size >= 2
    }

    fun match(query: String, lines: List<String>): LyricsPhraseMatch? {
        val queryTokens = tokens(normalize(query)).take(MAX_QUERY_TOKENS)
        if (queryTokens.isEmpty() || lines.isEmpty()) return null
        val normalizedLines = lines.map { tokens(normalize(it)) }
        var best: LyricsPhraseMatch? = null
        for (start in normalizedLines.indices) {
            val window = ArrayList<String>(queryTokens.size + 8)
            for (span in 0 until MAX_LINE_WINDOW) {
                val line = normalizedLines.getOrNull(start + span) ?: break
                window += line
                if (window.isEmpty()) continue
                val candidate = alignScore(queryTokens, window)
                val score = candidate.first
                if (best == null || score > best.score) {
                    best = LyricsPhraseMatch(score, start, candidate.second, queryTokens.size)
                }
                if (window.size >= queryTokens.size * 2) break
            }
            if (best != null && best.score >= 0.999f) break
        }
        return best
    }

    fun versionKind(title: String, artist: String = ""): LyricsPhraseVersionKind {
        val text = "$title $artist"
        return when {
            karaokeTitle.containsMatchIn(text) -> LyricsPhraseVersionKind.KARAOKE
            coverTitle.containsMatchIn(text) -> LyricsPhraseVersionKind.COVER
            remixTitle.containsMatchIn(title) -> LyricsPhraseVersionKind.REMIX
            liveTitle.containsMatchIn(title) -> LyricsPhraseVersionKind.LIVE
            else -> LyricsPhraseVersionKind.ORIGINAL
        }
    }

    fun dedupeKey(title: String, artist: String): String {
        val cleanTitle = normalize(title.substringBefore('(').substringBefore('['))
        val cleanArtist = normalize(artist.split(',', '&').first())
        return "$cleanTitle|$cleanArtist"
    }

    private fun alignScore(query: List<String>, text: List<String>): Pair<Float, Int> {
        var previous = FloatArray(text.size + 1)
        var current = FloatArray(text.size + 1)
        var bestWeight = 0f
        for (queryToken in query) {
            for (index in text.indices) {
                val similarity = tokenSimilarity(queryToken, text[index])
                val diagonal = previous[index] + similarity
                current[index + 1] = max(diagonal, max(previous[index + 1], current[index]))
            }
            val swap = previous
            previous = current
            current = swap
            current[0] = 0f
        }
        bestWeight = previous[text.size]
        val matched = bestWeight.toInt()
        val coverage = bestWeight / query.size
        val density = bestWeight / max(query.size, min(text.size, query.size * 2)).toFloat()
        return (coverage * 0.8f + density * 0.2f) to matched
    }

    private fun tokenSimilarity(left: String, right: String): Float {
        if (left == right) return 1f
        val shortest = min(left.length, right.length)
        val longest = max(left.length, right.length)
        if (shortest < 4 || longest > MAX_TOKEN_LENGTH_FOR_EDIT) return 0f
        if (longest - shortest > 2) return 0f
        val distance = editDistance(left, right)
        val allowed = if (shortest >= 7) 2 else 1
        return if (distance <= allowed) 1f - distance.toFloat() / longest else 0f
    }

    private fun editDistance(left: String, right: String): Int {
        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)
        for (i in left.indices) {
            current[0] = i + 1
            for (j in right.indices) {
                val substitution = previous[j] + if (left[i] == right[j]) 0 else 1
                current[j + 1] = min(substitution, min(previous[j + 1] + 1, current[j] + 1))
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[right.length]
    }

    private fun requiresMarks(text: String): Boolean = text.any { char ->
        when (Character.UnicodeBlock.of(char)) {
            Character.UnicodeBlock.DEVANAGARI,
            Character.UnicodeBlock.BENGALI,
            Character.UnicodeBlock.THAI,
            Character.UnicodeBlock.HANGUL_JAMO -> true
            else -> false
        }
    }

    private fun isUnspacedScript(char: Char): Boolean = when (Character.UnicodeBlock.of(char)) {
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.THAI -> true
        else -> false
    }
}
