package com.luc4n3x.levyra.data.apple

import java.text.Normalizer
import java.util.Locale

internal object AppleExploreCatalog {
    private val marks = Regex("\\p{M}+")
    private val separators = Regex("[^\\p{L}\\p{N}]+")
    private val aliases = mapOf(
        "commute" to "commuting", "energize" to "motivation", "workout" to "fitness",
        "relax" to "chill", "classica" to "classical", "musica classica" to "classical",
        "musica latina" to "latin", "latin music" to "latin", "hip hop rap" to "hip hop",
        "r b soul" to "r b", "indie alternative" to "alternative", "dance electronic" to "dance"
    )

    fun findCurator(title: String, items: List<AppleCatalogItem>): AppleCatalogItem? {
        val key = identity(title)
        if (key.isBlank()) return null
        return items.firstOrNull { item ->
            item.id.isNotBlank() && item.id.all(Char::isDigit) &&
                item.url.contains("/curator/") && identity(item.title) == key
        }
    }

    private fun identity(title: String): String {
        val normalized = separators.replace(
            marks.replace(Normalizer.normalize(title.lowercase(Locale.ROOT), Normalizer.Form.NFD), ""), " "
        ).trim().removePrefix("apple music ")
        return aliases[normalized] ?: normalized
    }
}
