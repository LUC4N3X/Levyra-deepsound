package com.luc4n3x.levyra.feature.settings

import java.text.Normalizer
import java.util.Locale

data class SettingsSearchEntry(
    val title: String,
    val description: String,
    val keywords: String,
    val categoryId: String,
    val categoryLabel: String
)

data class SettingsSearchResult(
    val title: String,
    val categoryId: String,
    val categoryLabel: String
)

class SettingsSearchIndex(entries: List<SettingsSearchEntry>, locale: Locale) {
    private val indexedEntries = entries.map { entry ->
        IndexedEntry(
            entry = entry,
            searchableText = normalize(
                listOf(entry.title, entry.description, entry.keywords, entry.categoryLabel)
                    .joinToString(" "),
                locale
            )
        )
    }
    private val locale = locale

    fun search(query: String): List<SettingsSearchResult> {
        val normalizedQuery = normalize(query, locale)
        if (normalizedQuery.isBlank()) return emptyList()
        val terms = normalizedQuery.split(' ').filter(String::isNotBlank)
        return indexedEntries.asSequence()
            .filter { indexed -> terms.all(indexed.searchableText::contains) }
            .map { indexed ->
                SettingsSearchResult(
                    title = indexed.entry.title,
                    categoryId = indexed.entry.categoryId,
                    categoryLabel = indexed.entry.categoryLabel
                )
            }
            .toList()
    }

    private data class IndexedEntry(
        val entry: SettingsSearchEntry,
        val searchableText: String
    )

    companion object {
        private fun normalize(value: String, locale: Locale): String {
            val decomposed = Normalizer.normalize(value.lowercase(locale), Normalizer.Form.NFD)
            return buildString(decomposed.length) {
                decomposed.forEach { character ->
                    if (Character.getType(character) != Character.NON_SPACING_MARK.toInt()) {
                        append(character)
                    }
                }
            }.trim()
        }
    }
}
