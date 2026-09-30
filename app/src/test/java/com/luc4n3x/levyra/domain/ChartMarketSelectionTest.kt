package com.luc4n3x.levyra.domain

import java.text.Collator
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMarketSelectionTest {

    @Test
    fun supportedDeviceCountryBecomesTheDefaultMarket() {
        assertEquals("ru", ChartsCatalog.startupRegion(storedRegionId = "", deviceCountry = "RU", languageCode = "it").id)
        assertEquals("us", ChartsCatalog.startupRegion(storedRegionId = "", deviceCountry = "us", languageCode = "it").id)
        assertEquals("gb", ChartsCatalog.startupRegion(storedRegionId = "", deviceCountry = "GB", languageCode = "de").id)
    }

    @Test
    fun unsupportedOrMissingDeviceCountryFallsBackToTheLanguageMarket() {
        listOf("", "XK", "419", "ZZ", "UK").forEach { deviceCountry ->
            assertEquals(
                "device=\"$deviceCountry\"",
                ChartsCatalog.defaultRegionForLanguage("de").id,
                ChartsCatalog.startupRegion(storedRegionId = "", deviceCountry = deviceCountry, languageCode = "de").id
            )
        }
        assertEquals("it", ChartsCatalog.startupRegion(storedRegionId = "", deviceCountry = "", languageCode = "it").id)
    }

    @Test
    fun persistedMarketWinsOverDeviceRegion() {
        assertEquals("ru", ChartsCatalog.startupRegion(storedRegionId = "ru", deviceCountry = "IT", languageCode = "it").id)
        assertEquals("jp", ChartsCatalog.startupRegion(storedRegionId = " JP ", deviceCountry = "US", languageCode = "en").id)
    }

    @Test
    fun unsupportedPersistedMarketIsIgnored() {
        assertEquals("de", ChartsCatalog.startupRegion(storedRegionId = "global", deviceCountry = "DE", languageCode = "it").id)
        assertNull(ChartsCatalog.supportedRegion("global"))
        assertNull(ChartsCatalog.supportedRegion(""))
    }

    @Test
    fun selectingTheVisibleMarketDoesNotReloadIt() {
        assertFalse(ChartsCatalog.requiresReload("it", "it", hasCharts = true, isLoading = false))
        assertFalse(ChartsCatalog.requiresReload("it", "it", hasCharts = false, isLoading = true))
        assertTrue(ChartsCatalog.requiresReload("it", "it", hasCharts = false, isLoading = false))
        assertTrue(ChartsCatalog.requiresReload("ru", "it", hasCharts = true, isLoading = false))
        assertTrue(ChartsCatalog.requiresReload("ru", "it", hasCharts = false, isLoading = true))
    }

    @Test
    fun marketsAreLocalizedAndSortedAlphabetically() {
        val markets = ChartMarketDirectory.markets(ChartsCatalog.regions, "it")
        val collator = Collator.getInstance(Locale.ITALIAN).apply { strength = Collator.PRIMARY }

        assertEquals(ChartsCatalog.regions.size, markets.size)
        assertEquals(ChartsCatalog.regions.map { it.id }.toSet(), markets.map { it.region.id }.toSet())
        assertEquals(markets.map { it.displayName }.sortedWith(collator), markets.map { it.displayName })
        assertEquals("Germania", markets.first { it.region.id == "de" }.displayName)
        assertEquals("Germany", ChartMarketDirectory.displayName(ChartsCatalog.region("de"), "en"))
        assertTrue(markets.all { it.displayName.isNotBlank() })
    }

    @Test
    fun searchIsCaseAndAccentInsensitiveAcrossLocalizedNativeAndEnglishNames() {
        val markets = ChartMarketDirectory.markets(ChartsCatalog.regions, "it")

        assertEquals(listOf("de"), ChartMarketDirectory.filter(markets, "GERMANIA").map { it.region.id })
        assertEquals(listOf("de"), ChartMarketDirectory.filter(markets, "germany").map { it.region.id })
        assertEquals(listOf("de"), ChartMarketDirectory.filter(markets, "Deutschland").map { it.region.id })
        assertTrue("es" in ChartMarketDirectory.filter(markets, "espana").map { it.region.id })
        assertTrue("tr" in ChartMarketDirectory.filter(markets, "turkiye").map { it.region.id })
        assertTrue("ru" in ChartMarketDirectory.filter(markets, "россия").map { it.region.id })
        assertTrue("us" in ChartMarketDirectory.filter(markets, "  stati ").map { it.region.id })
    }

    @Test
    fun emptySearchReturnsEveryMarketAndUnknownSearchReturnsNothing() {
        val markets = ChartMarketDirectory.markets(ChartsCatalog.regions, "en")

        assertEquals(markets, ChartMarketDirectory.filter(markets, ""))
        assertEquals(markets, ChartMarketDirectory.filter(markets, "   "))
        assertTrue(ChartMarketDirectory.filter(markets, "qqqqq").isEmpty())
    }

    @Test
    fun wordStartMatchesAreRankedBeforeInnerMatches() {
        val markets = ChartMarketDirectory.markets(ChartsCatalog.regions, "en")
        val results = ChartMarketDirectory.filter(markets, "sa").map { it.region.id }

        assertEquals("sa", results.first())
        assertTrue("us" in results)
        assertEquals("gb", ChartMarketDirectory.filter(markets, "king").first().region.id)
    }

    @Test
    fun suggestedMarketsListSelectionThenDistinctDeviceRegion() {
        val markets = ChartMarketDirectory.markets(ChartsCatalog.regions, "en")

        assertEquals(listOf("ru", "it"), ChartMarketDirectory.suggested(markets, "ru", "it").map { it.region.id })
        assertEquals(listOf("it"), ChartMarketDirectory.suggested(markets, "it", "it").map { it.region.id })
        assertEquals(listOf("it"), ChartMarketDirectory.suggested(markets, "it", null).map { it.region.id })
    }
}
