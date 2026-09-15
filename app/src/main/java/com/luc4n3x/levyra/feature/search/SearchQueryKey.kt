package com.luc4n3x.levyra.feature.search

import java.text.Normalizer

internal const val MIN_SEARCH_QUERY_CHARS = 2

private val FOLDED_MARK_SCRIPTS = setOf(
    Character.UnicodeScript.LATIN,
    Character.UnicodeScript.GREEK,
    Character.UnicodeScript.CYRILLIC,
    Character.UnicodeScript.COMMON,
    Character.UnicodeScript.INHERITED
)

internal fun searchQueryKey(raw: String): String {
    if (raw.isBlank()) return ""
    val decomposed = Normalizer.normalize(raw, Normalizer.Form.NFKD)
    val out = StringBuilder(decomposed.length)
    var pendingSeparator = false
    var baseScript: Character.UnicodeScript? = null
    var index = 0
    while (index < decomposed.length) {
        val codePoint = decomposed.codePointAt(index)
        index += Character.charCount(codePoint)
        when {
            isCombiningMark(codePoint) -> {
                val script = baseScript
                if (script != null && script !in FOLDED_MARK_SCRIPTS) out.appendCodePoint(codePoint)
            }
            Character.isLetterOrDigit(codePoint) -> {
                if (pendingSeparator && out.isNotEmpty()) out.append(' ')
                pendingSeparator = false
                baseScript = Character.UnicodeScript.of(codePoint)
                out.appendCodePoint(Character.toLowerCase(codePoint))
            }
            else -> {
                pendingSeparator = true
                baseScript = null
            }
        }
    }
    return out.toString()
}

internal fun searchQueryTokens(raw: String): List<String> =
    searchQueryKey(raw).split(' ').filter(String::isNotEmpty)

internal fun isSearchableQuery(raw: String): Boolean =
    raw.trim().length >= MIN_SEARCH_QUERY_CHARS && searchQueryKey(raw).isNotEmpty()

internal fun areRelatedSearchKeys(first: String, second: String): Boolean =
    first.isNotEmpty() && second.isNotEmpty() && (first.startsWith(second) || second.startsWith(first))

internal fun mergeSearchQuerySuggestions(
    currentQuery: String,
    remote: List<String>,
    shownEntityKeys: Set<String> = emptySet(),
    limit: Int = SEARCH_QUERY_SUGGESTION_LIMIT
): List<String> {
    val currentKey = searchQueryKey(currentQuery)
    val seen = HashSet<String>()
    return remote.asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .filter { suggestion ->
            val key = searchQueryKey(suggestion)
            key.isNotEmpty() && key != currentKey && key !in shownEntityKeys && seen.add(key)
        }
        .take(limit.coerceAtLeast(0))
        .toList()
}

private fun isCombiningMark(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
    Character.NON_SPACING_MARK.toInt(),
    Character.COMBINING_SPACING_MARK.toInt(),
    Character.ENCLOSING_MARK.toInt() -> true
    else -> false
}

internal const val SEARCH_QUERY_SUGGESTION_LIMIT = 6
