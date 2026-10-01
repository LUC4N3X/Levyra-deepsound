package com.luc4n3x.levyra.ui.settings

import com.luc4n3x.levyra.feature.settings.SettingsSearchIndex
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchCatalogTest {
    private val english = LevyraStrings.forCode("en")
    private val index = SettingsSearchIndex(entries(english), Locale.ENGLISH)

    @Test
    fun `every language builds unique ids pointing at real settings categories`() {
        LevyraStrings.all().forEach { strings ->
            val entries = entries(strings)
            assertEquals(strings.code, entries.size, entries.map { it.id }.toSet().size)
            entries.forEach { entry ->
                assertTrue("${strings.code}:${entry.id}", entry.categoryId in CATEGORY_IDS)
                assertTrue("${strings.code}:${entry.id}", entry.id.startsWith("${entry.categoryId}."))
                assertTrue("${strings.code}:${entry.id}", entry.title.isNotBlank())
                assertTrue("${strings.code}:${entry.id}", entry.categoryLabel.isNotBlank())
            }
        }
    }

    @Test
    fun `technical aliases reach the setting that actually owns them`() {
        assertEquals("audio.alternative_hq", topId("320"))
        assertEquals("audio.alternative_hq", topId("JioSaavn"))
        assertEquals("network.proxy", topId("proxy"))
        assertEquals("network.dns", topId("DNS"))
        assertEquals("audio.autoeq", topId("autoeq"))
        assertEquals("audio.equalizer", topId("equalizer"))
        assertEquals("design.liquid_glass", topId("glass"))
        assertEquals("audio.preload", topId("cache"))
    }

    @Test
    fun `eq and canvas queries stay inside their real sections`() {
        assertTrue(ids("eq").take(3).contains("audio.equalizer"))
        val canvas = index.search("canvas")
        assertTrue(canvas.isNotEmpty())
        assertTrue(canvas.take(4).all { it.categoryId == "design" || it.categoryId == "player" })
        assertTrue(canvas.any { it.id == "design.canvas_quality" })
    }

    @Test
    fun `broad queries return their own section`() {
        assertEquals("lyrics.provider_priority", topId("lyrics"))
        assertTrue(index.search("download").take(3).count { it.categoryId == "downloads" } >= 2)
        assertEquals(SETTINGS_CATEGORY_AUDIO, index.search("audio").first().categoryId)
    }

    @Test
    fun `settings that do not exist in this screen are not invented`() {
        assertTrue(index.search("radio").isEmpty())
    }

    @Test
    fun `optional rows follow build and device availability`() {
        val without = settingsSearchEntries(english, Locale.ENGLISH, updatesAvailable = false, aaudioOutputAvailable = false).map { it.id }
        val with = settingsSearchEntries(english, Locale.ENGLISH, updatesAvailable = true, aaudioOutputAvailable = true).map { it.id }

        assertFalse("app.updates" in without)
        assertFalse("audio.aaudio" in without)
        assertTrue("app.updates" in with)
        assertTrue("audio.aaudio" in with)
    }

    @Test
    fun `localized catalog keeps english technical aliases`() {
        val italian = SettingsSearchIndex(entries(LevyraStrings.forCode("it")), Locale.ITALIAN)

        assertEquals("audio.alternative_hq", italian.search("320").first().id)
        assertEquals("network.proxy", italian.search("proxy").first().id)
    }

    private fun entries(strings: LevyraStrings) =
        settingsSearchEntries(strings, Locale.forLanguageTag(strings.code), updatesAvailable = true, aaudioOutputAvailable = true)

    private fun ids(query: String) = index.search(query).map { it.id }

    private fun topId(query: String) = ids(query).first()

    private companion object {
        val CATEGORY_IDS = setOf(
            "design", "home", "player", SETTINGS_CATEGORY_AUDIO, "downloads", "lyrics",
            "backup", "system", "app", "integrations", "network", "jam"
        )
    }
}
