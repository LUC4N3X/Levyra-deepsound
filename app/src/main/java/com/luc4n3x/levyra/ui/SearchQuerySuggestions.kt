package com.luc4n3x.levyra.ui

import java.util.Locale

internal data class SearchSuggestionSegment(val text: String, val novel: Boolean)

internal fun refineSearchQuerySuggestions(query: String, suggestions: List<String>): List<String> {
    val queryKey = searchSuggestionKey(query)
    val seen = HashSet<String>()
    return suggestions.mapNotNull { raw ->
        val suggestion = raw.trim().replace(WHITESPACE, " ")
        val key = searchSuggestionKey(suggestion)
        suggestion.takeIf { key.isNotEmpty() && key != queryKey && seen.add(key) }
    }
}

internal fun searchSuggestionSegments(query: String, suggestion: String): List<SearchSuggestionSegment> {
    val queryTokens = query.split(WHITESPACE).map(::searchSuggestionToken).filter { it.isNotEmpty() }.toSet()
    if (queryTokens.isEmpty()) return listOf(SearchSuggestionSegment(suggestion, novel = true))
    val segments = ArrayList<SearchSuggestionSegment>()
    TOKEN.findAll(suggestion).forEach { match ->
        val word = match.value
        val normalized = searchSuggestionToken(word)
        val novel = normalized.isNotEmpty() && normalized !in queryTokens
        val last = segments.lastOrNull()
        if (last != null && (last.novel == novel || word.isBlank())) {
            segments[segments.lastIndex] = last.copy(text = last.text + word)
        } else {
            segments += SearchSuggestionSegment(word, novel)
        }
    }
    return segments
}

private fun searchSuggestionToken(word: String): String =
    word.lowercase(Locale.ROOT).trim { !it.isLetterOrDigit() }

private fun searchSuggestionKey(value: String): String =
    value.trim().lowercase(Locale.ROOT).replace(WHITESPACE, " ")

private val WHITESPACE = Regex("\\s+")
private val TOKEN = Regex("\\s+|\\S+")
