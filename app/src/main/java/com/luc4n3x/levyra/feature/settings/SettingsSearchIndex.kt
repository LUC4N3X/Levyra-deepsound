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
    val description: String,
    val categoryId: String,
    val categoryLabel: String,
    val score: Int
)

class SettingsSearchIndex(entries: List<SettingsSearchEntry>, locale: Locale) {
    private val indexedEntries = entries.mapIndexed { index, entry ->
        IndexedEntry(
            entry = entry,
            order = index,
            title = normalize(entry.title, locale),
            description = normalize(entry.description, locale),
            keywords = normalize(entry.keywords, locale),
            category = normalize(entry.categoryLabel, locale)
        )
    }
    private val locale = locale

    fun search(query: String): List<SettingsSearchResult> {
        val normalizedQuery = normalize(query, locale)
        if (normalizedQuery.isBlank()) return emptyList()
        val terms = normalizedQuery.split(' ').filter(String::isNotBlank)
        return indexedEntries.asSequence()
            .mapNotNull { indexed -> indexed.score(normalizedQuery, terms)?.let { score -> indexed to score } }
            .sortedWith(compareByDescending<Pair<IndexedEntry, Int>> { it.second }.thenBy { it.first.order })
            .map { (indexed, score) ->
                SettingsSearchResult(
                    title = indexed.entry.title,
                    description = indexed.entry.description,
                    categoryId = indexed.entry.categoryId,
                    categoryLabel = indexed.entry.categoryLabel,
                    score = score
                )
            }
            .take(MAX_RESULTS)
            .toList()
    }

    private data class IndexedEntry(
        val entry: SettingsSearchEntry,
        val order: Int,
        val title: String,
        val description: String,
        val keywords: String,
        val category: String
    ) {
        private val titleTokens = title.split(' ').filter(String::isNotBlank)
        private val keywordTokens = keywords.split(' ').filter(String::isNotBlank)
        private val descriptionTokens = description.split(' ').filter(String::isNotBlank)
        private val categoryTokens = category.split(' ').filter(String::isNotBlank)
        private val allTokens = titleTokens + keywordTokens + descriptionTokens + categoryTokens

        fun score(query: String, terms: List<String>): Int? {
            if (title == query) return SCORE_EXACT_TITLE
            val termScores = terms.map { term -> scoreTerm(term) ?: return null }
            val phraseBonus = when {
                title.startsWith(query) -> SCORE_TITLE_PREFIX
                title.contains(query) -> SCORE_TITLE_PHRASE
                keywords.contains(query) -> SCORE_KEYWORD_PHRASE
                description.contains(query) -> SCORE_DESCRIPTION_PHRASE
                category.contains(query) -> SCORE_CATEGORY_PHRASE
                else -> 0
            }
            return phraseBonus + termScores.sum()
        }

        private fun scoreTerm(term: String): Int? = when {
            titleTokens.any { it == term } -> SCORE_TITLE_TOKEN
            titleTokens.any { it.startsWith(term) } -> SCORE_TITLE_TOKEN_PREFIX
            keywordTokens.any { it == term } -> SCORE_KEYWORD_TOKEN
            keywordTokens.any { it.startsWith(term) } -> SCORE_KEYWORD_PREFIX
            descriptionTokens.any { it == term } -> SCORE_DESCRIPTION_TOKEN
            descriptionTokens.any { it.startsWith(term) } -> SCORE_DESCRIPTION_PREFIX
            categoryTokens.any { it == term } -> SCORE_CATEGORY_TOKEN
            categoryTokens.any { it.startsWith(term) } -> SCORE_CATEGORY_PREFIX
            term.length >= MIN_FUZZY_LENGTH && allTokens.any { token -> isReasonableFuzzyMatch(term, token) } -> SCORE_FUZZY
            else -> null
        }
    }

    companion object {
        private fun normalize(value: String, locale: Locale): String {
            val decomposed = Normalizer.normalize(value.lowercase(locale), Normalizer.Form.NFD)
            return buildString(decomposed.length) {
                var previousWasSpace = true
                decomposed.forEach { character ->
                    if (Character.getType(character) != Character.NON_SPACING_MARK.toInt()) {
                        val type = Character.getType(character)
                        if (character.isLetterOrDigit() ||
                            type == Character.COMBINING_SPACING_MARK.toInt() ||
                            type == Character.ENCLOSING_MARK.toInt()
                        ) {
                            append(character)
                            previousWasSpace = false
                        } else if (!previousWasSpace) {
                            append(' ')
                            previousWasSpace = true
                        }
                    }
                }
            }.trim()
        }

        private fun isReasonableFuzzyMatch(query: String, candidate: String): Boolean {
            if (candidate.length < MIN_FUZZY_LENGTH || kotlin.math.abs(query.length - candidate.length) > 2) return false
            if (isAdjacentTransposition(query, candidate)) return true
            val maximum = if (query.length >= 8) 2 else 1
            return editDistanceAtMost(query, candidate, maximum)
        }

        private fun isAdjacentTransposition(left: String, right: String): Boolean {
            if (left.length != right.length) return false
            val differences = left.indices.filter { left[it] != right[it] }
            return differences.size == 2 &&
                differences[1] == differences[0] + 1 &&
                left[differences[0]] == right[differences[1]] &&
                left[differences[1]] == right[differences[0]]
        }

        private fun editDistanceAtMost(left: String, right: String, maximum: Int): Boolean {
            if (left == right) return true
            if (kotlin.math.abs(left.length - right.length) > maximum) return false
            var previous = IntArray(right.length + 1) { it }
            var current = IntArray(right.length + 1)
            for (leftIndex in left.indices) {
                current[0] = leftIndex + 1
                var rowMinimum = current[0]
                for (rightIndex in right.indices) {
                    val substitution = previous[rightIndex] + if (left[leftIndex] == right[rightIndex]) 0 else 1
                    current[rightIndex + 1] = minOf(
                        current[rightIndex] + 1,
                        previous[rightIndex + 1] + 1,
                        substitution
                    )
                    rowMinimum = minOf(rowMinimum, current[rightIndex + 1])
                }
                if (rowMinimum > maximum) return false
                val swap = previous
                previous = current
                current = swap
            }
            return previous[right.length] <= maximum
        }

        private const val MAX_RESULTS = 24
        private const val MIN_FUZZY_LENGTH = 4
        private const val SCORE_EXACT_TITLE = 100_000
        private const val SCORE_TITLE_PREFIX = 20_000
        private const val SCORE_TITLE_PHRASE = 15_000
        private const val SCORE_KEYWORD_PHRASE = 10_000
        private const val SCORE_DESCRIPTION_PHRASE = 6_000
        private const val SCORE_CATEGORY_PHRASE = 4_000
        private const val SCORE_TITLE_TOKEN = 3_000
        private const val SCORE_TITLE_TOKEN_PREFIX = 2_500
        private const val SCORE_KEYWORD_TOKEN = 2_000
        private const val SCORE_KEYWORD_PREFIX = 1_700
        private const val SCORE_DESCRIPTION_TOKEN = 1_200
        private const val SCORE_DESCRIPTION_PREFIX = 1_000
        private const val SCORE_CATEGORY_TOKEN = 800
        private const val SCORE_CATEGORY_PREFIX = 650
        private const val SCORE_FUZZY = 250
    }
}
