package com.luc4n3x.levyra.feature.settings

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchIndexTest {
    private val index = SettingsSearchIndex(
        entries = listOf(
            SettingsSearchEntry(
                id = "design.animations",
                title = "Animazioni",
                description = "Movimento dell'interfaccia",
                keywords = "motion",
                categoryId = "design",
                categoryLabel = "Design"
            ),
            SettingsSearchEntry(
                id = "player.sponsorblock",
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
                    id = "system.region",
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
                SettingsSearchEntry("audio.engine", "Audio", "Crossfade controls", "sound", "audio", "Audio"),
                SettingsSearchEntry("audio.crossfade", "Crossfade", "Blend adjacent songs", "transition", "audio", "Audio"),
                SettingsSearchEntry("player.playback", "Playback", "Player options", "crossfade", "player", "Player")
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
                SettingsSearchEntry("audio.quality", "Qualità audio", "Audio ad alta fedeltà", "hi-fi", "audio", "Riproduzione")
            ),
            locale = Locale.ITALIAN
        )

        assertEquals("Qualità audio", localized.search("qualita audio").single().title)
        assertEquals("Qualità audio", localized.search("qualita audoi").single().title)
        assertTrue(localized.search("qua").isNotEmpty())
        assertTrue(localized.search("completamente diverso").isEmpty())
    }

    @Test
    fun `ranking follows exact title prefix contains keyword then subtitle`() {
        val ranked = SettingsSearchIndex(
            entries = listOf(
                SettingsSearchEntry("subtitle", "Other", "Mentions lyrics in passing", "", "a", "A"),
                SettingsSearchEntry("keyword", "Display", "", "lyrics", "a", "A"),
                SettingsSearchEntry("contains", "Synced lyrics", "", "", "a", "A"),
                SettingsSearchEntry("prefix", "Lyrics provider", "", "", "a", "A"),
                SettingsSearchEntry("exact", "Lyrics", "", "", "a", "A")
            ),
            locale = Locale.ENGLISH
        ).search("lyrics")

        assertEquals(listOf("exact", "prefix", "contains", "keyword", "subtitle"), ranked.map(SettingsSearchResult::id))
    }

    @Test
    fun `query is case insensitive and whitespace tolerant`() {
        val index = SettingsSearchIndex(
            entries = listOf(SettingsSearchEntry("network.proxy", "Proxy", "Route traffic", "http socks", "network", "Network")),
            locale = Locale.ENGLISH
        )

        assertEquals("network.proxy", index.search("  PROXY ").single().id)
        assertEquals("network.proxy", index.search("http   SOCKS").single().id)
    }

    @Test
    fun `technical alias finds a setting whose title does not contain it`() {
        val index = SettingsSearchIndex(
            entries = listOf(
                SettingsSearchEntry("audio.alternative", "High-quality alternative audio", "External stream", "320 kbps jiosaavn", "audio", "Audio"),
                SettingsSearchEntry("audio.crossfade", "Crossfade", "Blend songs", "transition", "audio", "Audio")
            ),
            locale = Locale.ENGLISH
        )

        assertEquals("audio.alternative", index.search("320").single().id)
        assertEquals("audio.alternative", index.search("JioSaavn").single().id)
    }

    @Test
    fun `equal scores keep catalog order and duplicate ids collapse`() {
        val results = SettingsSearchIndex(
            entries = listOf(
                SettingsSearchEntry("b", "Wifi only", "", "", "downloads", "Downloads"),
                SettingsSearchEntry("a", "Wifi only", "", "", "design", "Design"),
                SettingsSearchEntry("b", "Wifi only", "", "", "downloads", "Downloads")
            ),
            locale = Locale.ENGLISH
        ).search("wifi")

        assertEquals(listOf("b", "a"), results.map(SettingsSearchResult::id))
    }

    @Test
    fun `unrelated query returns no results`() {
        assertTrue(index.search("zzzz qqqq").isEmpty())
    }
}
