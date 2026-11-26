package com.luc4n3x.levyra.feature.radio

import java.text.CollationKey
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

internal enum class RadioFavoriteSort(val id: String) {
    Newest("newest"),
    Oldest("oldest"),
    NameAscending("name_asc"),
    NameDescending("name_desc"),
    Country("country"),
    Genre("genre");

    companion object {
        val Default = Newest

        fun fromId(id: String?): RadioFavoriteSort = entries.firstOrNull { it.id == id } ?: Default
    }
}

internal data class RadioFavoriteFacts(
    val name: String,
    val countryCode: String,
    val country: String,
    val tags: List<String>,
    val addedAt: Long
)

internal data class RadioFavoriteGroup<T>(val key: String, val title: String?, val items: List<T>)

internal class RadioFavoriteGroupLabels(
    val countryName: (String) -> String,
    val genreName: (RadioCategory) -> String,
    val unknownCountry: String,
    val otherGenre: String
)

internal fun <T> radioFavoriteGroups(
    items: List<T>,
    sort: RadioFavoriteSort,
    locale: Locale,
    labels: RadioFavoriteGroupLabels,
    facts: (T) -> RadioFavoriteFacts
): List<RadioFavoriteGroup<T>> {
    if (items.isEmpty()) return emptyList()
    val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
    val entries = items.mapIndexed { index, item ->
        val itemFacts = facts(item)
        RankedFavorite(item, index, itemFacts, collator.getCollationKey(itemFacts.name.trim()))
    }
    val byName = compareBy<RankedFavorite<T>> { it.nameKey }.thenBy { it.index }
    val newest = compareByDescending<RankedFavorite<T>> { it.facts.addedAt }.thenBy { it.index }
    return when (sort) {
        RadioFavoriteSort.Newest -> listOf(ungrouped(sort, entries.sortedWith(newest)))
        RadioFavoriteSort.Oldest -> listOf(ungrouped(sort, entries.sortedWith(newest.reversed())))
        RadioFavoriteSort.NameAscending -> listOf(ungrouped(sort, entries.sortedWith(byName)))
        RadioFavoriteSort.NameDescending -> listOf(ungrouped(sort, entries.sortedWith(byName.reversed())))
        RadioFavoriteSort.Country -> grouped(sort, entries, collator, byName, labels.unknownCountry) { radioCountryTitle(it, labels) }
        RadioFavoriteSort.Genre -> grouped(sort, entries, collator, byName, labels.otherGenre) { favorite ->
            primaryRadioGenre(favorite.tags)?.let(labels.genreName)
        }
    }
}

internal fun primaryRadioGenre(tags: List<String>): RadioCategory? {
    for (tag in tags) {
        val words = " ${normalizeRadioGenreTag(tag)} "
        var match: RadioCategory? = null
        var matchIndex = Int.MAX_VALUE
        for ((category, aliases) in radioGenreAliases) {
            for (alias in aliases) {
                val index = words.indexOf(alias)
                if (index in 0 until matchIndex) {
                    match = category
                    matchIndex = index
                }
            }
        }
        if (match != null) return match
    }
    return null
}

private class RankedFavorite<T>(val item: T, val index: Int, val facts: RadioFavoriteFacts, val nameKey: CollationKey)

private fun <T> ungrouped(sort: RadioFavoriteSort, entries: List<RankedFavorite<T>>) =
    RadioFavoriteGroup(sort.id, null, entries.map { it.item })

private fun <T> grouped(
    sort: RadioFavoriteSort,
    entries: List<RankedFavorite<T>>,
    collator: Collator,
    byName: Comparator<RankedFavorite<T>>,
    fallbackTitle: String,
    title: (RadioFavoriteFacts) -> String?
): List<RadioFavoriteGroup<T>> = entries.groupBy { title(it.facts) }
    .map { (groupTitle, members) -> Triple(groupTitle, groupTitle?.let(collator::getCollationKey), members) }
    .sortedWith(compareBy<Triple<String?, CollationKey?, List<RankedFavorite<T>>>> { it.first == null }.thenBy { it.second })
    .map { (groupTitle, _, members) ->
        RadioFavoriteGroup(
            key = "${sort.id}:${groupTitle ?: ""}",
            title = groupTitle ?: fallbackTitle,
            items = members.sortedWith(byName).map { it.item }
        )
    }

private fun radioCountryTitle(facts: RadioFavoriteFacts, labels: RadioFavoriteGroupLabels): String? {
    val code = facts.countryCode.trim().uppercase(Locale.ROOT)
    val localized = code.takeIf { it.length == 2 }?.let(labels.countryName)?.trim().orEmpty()
    return localized.ifBlank { facts.country.trim() }.takeIf(String::isNotBlank)
}

private val radioGenreAliases: List<Pair<RadioCategory, List<String>>> = listOf(
    RadioCategory.Pop to listOf("pop"),
    RadioCategory.Rock to listOf("rock"),
    RadioCategory.HipHop to listOf("hip hop", "hiphop", "rap"),
    RadioCategory.Electronic to listOf("electronic", "electronica", "techno", "house", "trance", "edm"),
    RadioCategory.Dance to listOf("dance"),
    RadioCategory.Chill to listOf("chillout", "chill", "lounge", "ambient"),
    RadioCategory.Jazz to listOf("jazz"),
    RadioCategory.Classical to listOf("classical"),
    RadioCategory.News to listOf("news"),
    RadioCategory.Talk to listOf("talk"),
    RadioCategory.Sport to listOf("sports", "sport")
).map { (category, aliases) -> category to aliases.map { " $it " } }

private val radioGenreSeparatorPattern = Regex("[^\\p{L}\\p{N}]+")
private val radioGenreMarkPattern = Regex("\\p{Mn}+")

private fun normalizeRadioGenreTag(value: String): String =
    Normalizer.normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(radioGenreMarkPattern, "")
        .replace(radioGenreSeparatorPattern, " ")
        .trim()
