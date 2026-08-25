package com.luc4n3x.levyra.data

import org.json.JSONArray
import org.json.JSONObject

internal fun parseYoutubeMusicSearchSuggestions(root: JSONObject?): List<String> {
    root ?: return emptyList()
    val contents = root.optJSONArray("contents") ?: return emptyList()
    val suggestions = LinkedHashSet<String>()

    for (sectionIndex in 0 until contents.length()) {
        val section = contents.optJSONObject(sectionIndex)
            ?.optJSONObject("searchSuggestionsSectionRenderer")
            ?: continue
        val items = section.optJSONArray("contents") ?: continue
        for (itemIndex in 0 until items.length()) {
            val item = items.optJSONObject(itemIndex) ?: continue
            val renderer = item.optJSONObject("historySuggestionRenderer")
                ?: item.optJSONObject("searchSuggestionRenderer")
                ?: continue
            val query = renderer.optJSONObject("navigationEndpoint")
                ?.optJSONObject("searchEndpoint")
                ?.optString("query")
                .orEmpty()
                .trim()
                .ifBlank { suggestionText(renderer.optJSONObject("suggestion")?.optJSONArray("runs")) }
            if (query.isNotBlank()) suggestions += query
            if (suggestions.size >= MAX_SEARCH_SUGGESTIONS) return suggestions.toList()
        }
    }

    return suggestions.toList()
}

private fun suggestionText(runs: JSONArray?): String {
    runs ?: return ""
    val text = StringBuilder()
    for (index in 0 until runs.length()) {
        val part = runs.optJSONObject(index)?.optString("text").orEmpty()
        if (part.isNotEmpty()) text.append(part)
    }
    return text.toString().trim()
}

private const val MAX_SEARCH_SUGGESTIONS = 12
