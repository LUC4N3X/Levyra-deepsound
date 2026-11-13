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

    private val curatorsByZone = mapOf(
        "rap-drill" to listOf("989061185", "976439539", "989061415", "989061413", "989061403", "982348875"),
        "elettronica" to listOf("989076708", "976439536", "989074778", "989075753", "976439535"),
        "pop-global" to listOf("988588080", "976439548", "988589172"),
        "rnb-soul" to listOf("988583890", "976439551", "976439585"),
        "rock-alt" to listOf("988581516", "976439554", "988556214", "976439526", "988574300", "988574297", "976439531"),
        "latino" to listOf("1531542847", "976439553", "988953085", "976439549"),
        "lofi-chill" to listOf("1558256251"),
        "anime-jpop" to listOf("987714244", "982302294"),
        "afrobeats" to listOf("1747003654", "1765343179"),
        "chart-kpop" to listOf("988658197"),
        "chart-metal" to listOf("988658201", "976439543"),
        "chart-indie" to listOf("976439541"),
        "chart-jazz" to listOf("976439542")
    )

    fun curatorId(categoryParams: String): String? = curatorsByCategoryParams[categoryParams.trim()]

    fun zoneCuratorIds(zoneId: String): List<String> = curatorsByZone[zoneId.trim()].orEmpty()

    fun genreShelves(page: AppleCatalogPage?): List<AppleCatalogShelf> = page?.shelves.orEmpty().asSequence()
        .filter { shelf -> shelf.kind in discoveryKinds && (shelf.kind == AppleCatalogShelfKind.Hero || shelf.title.isNotBlank()) }
        .filterNot { shelf -> isAppleBranded(shelf.title) }
        .mapNotNull { shelf ->
            val items = shelf.items.filter { item -> item.kind != AppleCatalogItemKind.Other && item.title.isNotBlank() }
                .take(MAX_SHELF_ITEMS)
                .map(::unbranded)
            if (items.isEmpty()) null else shelf.copy(items = items)
        }
        .distinctBy { shelf -> "${shelf.kind}:${shelf.title}:${shelf.items.first().id}" }
        .take(MAX_SHELVES)
        .toList()

    fun unbranded(item: AppleCatalogItem): AppleCatalogItem = item.copy(
        subtitle = item.subtitle.takeUnless(::isAppleBranded).orEmpty(),
        artworkTemplate = plainArtwork(item.artworkTemplate),
        coverArtworkTemplate = plainArtwork(item.coverArtworkTemplate)
    )

    fun plainArtwork(template: String): String = compositedArtwork.replace(template, "{w}x{h}cc.{f}")

    private fun isAppleBranded(text: String): Boolean = text.contains("apple music", ignoreCase = true)

    private val compositedArtwork = Regex("\\{w\\}x\\{h\\}SC\\.[A-Za-z0-9]+\\.\\{f\\}")

    private val discoveryKinds = setOf(
        AppleCatalogShelfKind.Hero, AppleCatalogShelfKind.Songs, AppleCatalogShelfKind.Grid, AppleCatalogShelfKind.Videos
    )
    private const val MAX_SHELF_ITEMS = 20
    private const val MAX_SHELVES = 24

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
