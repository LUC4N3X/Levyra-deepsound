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
            "Animazioni",
            index.search("animazioni").single().title
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

    @Test
    fun `turkish dotted capital stays reachable from a plain ascii query`() {
        val turkish = SettingsSearchIndex(
            entries = listOf(
                SettingsSearchEntry(
                    title = "İstanbul",
                    description = "Bölge",
                    keywords = "region",
                    categoryId = "system",
                    categoryLabel = "Tercihler"
                )
            ),
            locale = Locale.forLanguageTag("tr")
        )

        assertEquals("system", turkish.search("istanbul").single().categoryId)
    }

    @Test
    fun `exact title outranks keyword and description matches`() {
        val ranked = SettingsSearchIndex(
            entries = listOf(
                SettingsSearchEntry("Audio", "Crossfade controls", "sound", "audio", "Audio"),
                SettingsSearchEntry("Crossfade", "Blend adjacent songs", "transition", "audio", "Audio"),
                SettingsSearchEntry("Playback", "Player options", "crossfade", "player", "Player")
            ),
            locale = Locale.ENGLISH
        ).search("crossfade")

        assertEquals(listOf("Crossfade", "Playback", "Audio"), ranked.map(SettingsSearchResult::title))
        assertTrue(ranked[0].score > ranked[1].score)
        assertTrue(ranked[1].score > ranked[2].score)
    }

    @Test
    fun `diacritics punctuation and a conservative typo are normalized`() {
        val localized = SettingsSearchIndex(
            entries = listOf(
                SettingsSearchEntry("Qualità audio", "Audio ad alta fedeltà", "hi-fi", "audio", "Riproduzione")
            ),
            locale = Locale.ITALIAN
        )

        assertEquals("Qualità audio", localized.search("qualita audio").single().title)
        assertEquals("Qualità audio", localized.search("qualita audoi").single().title)
        assertTrue(localized.search("qua").isNotEmpty())
        assertTrue(localized.search("completamente diverso").isEmpty())
    }
}
