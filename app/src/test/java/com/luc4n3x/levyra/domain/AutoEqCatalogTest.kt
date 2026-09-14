package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoEqCatalogTest {
    private val index = """
        # Index
        This is a list of all equalization profiles.

        - [Sennheiser HD 600](./crinacle/GRAS%2043AG-7%20over-ear/Sennheiser%20HD%20600) by crinacle on GRAS 43AG-7
        - [Sennheiser HD 600](./oratory1990/over-ear/Sennheiser%20HD%20600) by oratory1990
        - [Sennheiser HD 650](./oratory1990/over-ear/Sennheiser%20HD%20650) by oratory1990
        - [Sony WH-1000XM4](./Rtings/HMS%20II.3%20over-ear/Sony%20WH-1000XM4) by Rtings on HMS II.3
        - [Sony WF-1000XM4](./crinacle/711%20in-ear/Sony%20WF-1000XM4) by crinacle on 711
        - [1MORE Aero (ANC Off)](./HypetheSonics/GRAS%20RA0045%20in-ear/1MORE%20Aero%20(ANC%20Off)) by HypetheSonics on GRAS RA0045
        - [Sony WF-1000XM4](./crinacle/711%20in-ear/Sony%20WF-1000XM4) by crinacle on 711
        - [Escape](./oratory1990/../Escape) by oratory1990
        - [Traversal](./oratory1990/over-ear/..) by oratory1990
        - [Renamed](./oratory1990/over-ear/Other) by oratory1990
        - [Spoofed](./crinacle/over-ear/Spoofed) by oratory1990
        - [Query](./oratory1990/over-ear/Query?x=1) by oratory1990
        - [Nested](./oratory1990/over-ear/a%2Fb) by oratory1990
        - [Control](./oratory1990/over-ear/Control%0A) by oratory1990
        - Plain bullet without link
    """.trimIndent()

    private val catalog = AutoEqCatalog.parseIndex(index)

    @Test
    fun keepsOnlyWellFormedUniqueEntries() {
        assertEquals(6, catalog.size)
    }

    @Test
    fun matchesBrandAndModelIgnoringPunctuationAndOrder() {
        val names = catalog.search("xm4 sony").map { it.name }

        assertEquals(setOf("Sony WH-1000XM4", "Sony WF-1000XM4"), names.toSet())
        assertEquals("Sony WH-1000XM4", catalog.search("wh1000xm4").single().name)
    }

    @Test
    fun preferredMeasurementSourceWinsForSameModel() {
        val results = catalog.search("hd 600")

        assertEquals(listOf("oratory1990", "crinacle"), results.map { it.source })
        assertEquals(listOf("over-ear", "GRAS 43AG-7 over-ear"), results.map { it.variant })
    }

    @Test
    fun exactAndPrefixMatchesRankAheadOfSubstrings() {
        val results = AutoEqCatalog.parseIndex(
            """
            - [Audio Technica Pro](./crinacle/over-ear/Audio%20Technica%20Pro) by crinacle
            - [Pro](./crinacle/over-ear/Pro) by crinacle
            - [Pro X](./crinacle/over-ear/Pro%20X) by crinacle
            """.trimIndent()
        ).search("pro")

        assertEquals(listOf("Pro", "Pro X", "Audio Technica Pro"), results.map { it.name })
    }

    @Test
    fun graphicEqPathIsPercentEncodedUnderResults() {
        val hd600 = catalog.search("hd600").first()
        val aero = catalog.search("aero").single()

        assertEquals(
            "results/oratory1990/over-ear/Sennheiser%20HD%20600/Sennheiser%20HD%20600%20GraphicEQ.txt",
            hd600.graphicEqPath
        )
        assertEquals(
            "results/HypetheSonics/GRAS%20RA0045%20in-ear/1MORE%20Aero%20%28ANC%20Off%29/" +
                "1MORE%20Aero%20%28ANC%20Off%29%20GraphicEQ.txt",
            aero.graphicEqPath
        )
    }

    @Test
    fun blankOrSymbolOnlyQueriesReturnNothing() {
        assertTrue(catalog.search("").isEmpty())
        assertTrue(catalog.search("  - / ").isEmpty())
        assertTrue(catalog.search("sony", limit = 0).isEmpty())
    }

    @Test
    fun resultLimitIsRespected() {
        assertEquals(1, catalog.search("sony", limit = 1).size)
    }

    @Test
    fun foldingRemovesAccentsCaseAndSeparators() {
        assertEquals("beyerdynamicdt770proedition", AutoEqCatalog.foldSearchText("Beyerdynamic DT-770 Pro Édition"))
    }

    @Test
    fun encodingKeepsUnreservedCharactersOnly() {
        assertEquals("K%C3%B8ss%20Porta%20Pro%20%28v2%29", AutoEqCatalog.encodePathSegment("Køss Porta Pro (v2)"))
    }

    @Test
    fun largeCatalogStaysSearchable() {
        val large = buildString {
            repeat(12_000) { index ->
                append("- [Brand Model $index](./crinacle/711%20in-ear/Brand%20Model%20$index) by crinacle on 711\n")
            }
        }

        val parsed = AutoEqCatalog.parseIndex(large)

        assertEquals(12_000, parsed.size)
        assertEquals("Brand Model 11999", parsed.search("model 11999").first().name)
        assertEquals(AutoEqCatalog.DEFAULT_RESULT_LIMIT, parsed.search("brand").size)
    }
}
