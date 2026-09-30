package com.luc4n3x.levyra.domain

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

data class ChartMarket(
    val region: ChartRegion,
    val displayName: String,
    internal val searchTerms: List<String>
)

object ChartMarketDirectory {
    private val combiningMarks = Regex("\\p{Mn}+")
    private val wordSeparators = Regex("[\\s\\-]+")

    fun displayName(region: ChartRegion, languageCode: String): String =
        localizedCountryName(region, Locale.forLanguageTag(languageCode))

    fun markets(regions: List<ChartRegion>, languageCode: String): List<ChartMarket> {
        val locale = Locale.forLanguageTag(languageCode)
        val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
        return regions
            .map { region ->
                val displayName = localizedCountryName(region, locale)
                val englishName = localizedCountryName(region, Locale.ENGLISH)
                ChartMarket(
                    region = region,
                    displayName = displayName,
                    searchTerms = listOf(displayName, region.label, englishName).map(::searchKey).distinct()
                )
            }
            .sortedWith(compareBy(collator) { it.displayName })
    }

    fun filter(markets: List<ChartMarket>, query: String): List<ChartMarket> {
        val normalizedQuery = searchKey(query)
        if (normalizedQuery.isEmpty()) return markets
        return markets
            .filter { market ->
                market.region.id == normalizedQuery || market.searchTerms.any { it.contains(normalizedQuery) }
            }
            .sortedBy { market -> if (market.matchesWordStart(normalizedQuery)) 0 else 1 }
    }

    fun suggested(markets: List<ChartMarket>, selectedId: String, deviceRegionId: String?): List<ChartMarket> =
        listOfNotNull(
            markets.firstOrNull { it.region.id == selectedId },
            markets.firstOrNull { it.region.id == deviceRegionId && it.region.id != selectedId }
        )

    internal fun searchKey(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)

    private fun ChartMarket.matchesWordStart(query: String): Boolean =
        region.id == query || searchTerms.any { term -> term.split(wordSeparators).any { it.startsWith(query) } }

    private fun localizedCountryName(region: ChartRegion, locale: Locale): String {
        val countryCode = region.country.uppercase(Locale.ROOT)
        val localized = runCatching {
            Locale.Builder().setRegion(countryCode).build().getDisplayCountry(locale)
        }.getOrDefault("")
        return localized.takeIf { it.isNotBlank() && it != countryCode } ?: region.label
    }
}
