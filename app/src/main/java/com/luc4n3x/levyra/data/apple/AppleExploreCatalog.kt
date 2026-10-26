package com.luc4n3x.levyra.data.apple

import java.text.Normalizer
import java.util.Locale

internal object AppleExploreCatalog {
    private val marks = Regex("\\p{M}+")
    private val separators = Regex("[^\\p{L}\\p{N}]+")
    private val aliases = mapOf(
        "commute" to "commuting", "energize" to "motivation", "workout" to "fitness",
        "relax" to "chill", "classica" to "classical", "musica classica" to "classical",
        "musica latina" to "latin", "latin music" to "latin", "latino" to "latin", "hip hop rap" to "hip hop",
        "r b soul" to "r b", "r b e soul" to "r b", "indie alternative" to "alternative", "dance electronic" to "dance",
        "musica di natale" to "natale", "christmas music" to "christmas"
    )
    private val curatorsByCategoryParams = mapOf(
        "ggMPOg1uX044Z2o5WERLckpU" to "1558257238",
        "ggMPOg1uX2w1aW1CRDFTSUNo" to "1558257035",
        "ggMPOg1uX3NISTh4UmtWcFgz" to "1558256865",
        "ggMPOg1uX2lRZUZiMnNrQnJW" to "1558257146",
        "ggMPOg1uX0NvNGNhWThMYWRh" to "1558257095",
        "ggMPOg1uX1JOQWZFeDByc2Jm" to "1558256251",
        "ggMPOg1uX280STVTNVQwRGZ0" to "976439540",
        "ggMPOg1uX1JCQnB2QXVYVEIz" to "1558257331",
        "ggMPOg1uX1MxaFQ3Z0JMZkN4" to "1558257257",
        "ggMPOg1uXzIxYkNac21YZ2Z0" to "1558256909",
        "ggMPOg1uXzZQbDB5eThLRTQ3" to "1558256919",
        "ggMPOg1uX3NmUVV4Vzl3WGQ0" to "1558257191",
        "ggMPOg1uX3VOQWxsblVZTFNE" to "982302682",
        "ggMPOg1uX0UzWGxlRE5jMDVk" to "988656348",
        "ggMPOg1uX2NXUkgxdW0zUHJp" to "976439528",
        "ggMPOg1uX0JrcUJGUHhxaTFV" to "982307152",
        "ggMPOg1uX3E1VnpSRHhLVElG" to "1482068485",
        "ggMPOg1uX3lPcDFRaE9wM1BS" to "976439542",
        "ggMPOg1uXzAwSjVITDBZckJR" to "987714244",
        "ggMPOg1uX21NWWpBbU01SDgy" to "976439541",
        "ggMPOg1uX1RXcFlyZEpRb1d3" to "976439534",
        "ggMPOg1uX1N4VmduTmdUR3dm" to "976439532",
        "ggMPOg1uX29wWTRjMHV1dWN5" to "1531542847",
        "ggMPOg1uX2hXVUQwc0JTNXlE" to "988658199",
        "ggMPOg1uXzdlSXhKZ0hMV1Z4" to "976439543",
        "ggMPOg1uX1BmNzc2V2p0YXJ5" to "976439548",
        "ggMPOg1uX1JUc2lFcDFuUUth" to "976439552",
        "ggMPOg1uXzJKTm5jUEZ5Uzlu" to "976439554",
        "ggMPOg1uX2tWZXBsRm05cHNR" to "976439586",
        "ggMPOg1uXzMyY3J2SGM0bVh5" to "1555173397",
        "ggMPOg1uX1NPTld3SDN3WGs4" to "976439535",
        "ggMPOg1uXzBTRFBmQ3N4b0R6" to "979231701",
        "ggMPOg1uX2E4aVJuU05GOVQ0" to "976439539",
        "ggMPOg1uX0JrbjBDOFFPSzJW" to "988658197",
        "ggMPOg1uX2JxQ2hxc2J5UFhR" to "976439551"
    )

    fun curatorId(categoryParams: String): String? = curatorsByCategoryParams[categoryParams.trim()]

    fun findCurator(title: String, items: List<AppleCatalogItem>): AppleCatalogItem? {
        val key = identity(title)
        if (key.isBlank()) return null
        return items.firstOrNull { item -> isCategory(item) && identity(item.title) == key }
    }

    fun isCategory(item: AppleCatalogItem): Boolean =
        item.id.isNotBlank() && item.id.all(Char::isDigit) && item.url.contains("/curator/")

    fun identity(title: String): String {
        val normalized = separators.replace(
            marks.replace(Normalizer.normalize(title.lowercase(Locale.ROOT), Normalizer.Form.NFD), ""), " "
        ).trim().removePrefix("apple music ")
        return aliases[normalized] ?: normalized
    }
}
