package com.luc4n3x.levyra.feature.settings

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchIndexTest {
    private val index = SettingsSearchIndex(
        entries = listOf(
            SettingsSearchEntry(
                title = "Animazioni",
                description = "Movimento dell'interfaccia",
                keywords = "motion",
                categoryId = "design",
                categoryLabel = "Design"
            ),
            SettingsSearchEntry(
                title = "SponsorBlock",
                description = "Salta segmenti",
                keywords = "skip sponsor",
                categoryId = "player",
                categoryLabel = "Player"
            )
        ),
        locale = Locale.ITALIAN
    )

    @Test
    fun `matches normalized localized text and keeps the real category route`() {
        assertEquals(
            listOf(SettingsSearchResult("Animazioni", "design", "Design")),
            index.search("animazioni")
        )
    }

    @Test
    fun `matches keywords without recompiling patterns`() {
        assertEquals("player", index.search("sponsor skip").single().categoryId)
    }

    @Test
    fun `blank query has no synthetic category results`() {
        assertTrue(index.search("   ").isEmpty())
    }
}
