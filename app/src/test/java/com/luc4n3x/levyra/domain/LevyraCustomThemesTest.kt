package com.luc4n3x.levyra.domain

import com.luc4n3x.levyra.ui.i18n.themeLibraryCompleteLocalizationCodes
import com.luc4n3x.levyra.ui.i18n.themeLibraryLocalizationCodes
import com.luc4n3x.levyra.ui.theme.LevyraThemes
import com.luc4n3x.levyra.ui.theme.parseThemeHex
import com.luc4n3x.levyra.ui.theme.toPalette
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraCustomThemesTest {

    private val sample = LevyraCustomTheme(
        id = "custom:sample",
        name = "  Night drive  ",
        isLight = false,
        background = 0x05070F,
        surface = 0xFF131A2B.toInt(),
        primary = 0xFF4C7DFF.toInt(),
        secondary = 0xFFE38BD8.toInt(),
        accent = 0xFFFFC978.toInt(),
        cornerScale = 500
    )

    @After
    fun clearRegistry() {
        LevyraThemes.setCustomThemes(emptyList())
    }

    @Test
    fun defaultPresetStaysLevyraAura() {
        assertEquals(LevyraThemes.LEVYRA_AURA, LevyraThemes.defaultPresetId)
        assertEquals(LevyraThemes.levyraAura, LevyraThemes.byId(""))
        assertEquals(LevyraThemes.LEVYRA_AURA, LevyraThemes.normalize("unknown"))
    }

    @Test
    fun existingPresetsKeepTheirOrderAndNewOnesAreAppended() {
        val ids = LevyraThemes.presets.map { it.id }
        assertEquals(
            listOf("levyra_aura", "cover_flow", "amoled", "frost", "ocean", "ember", "cosmic", "neon_cyan", "purple_glass", "minimal_white", "mood_flow"),
            ids.take(11)
        )
        assertEquals(listOf("midnight", "aurora", "pure_material", "velvet", "editorial"), ids.drop(11))
    }

    @Test
    fun normalizationCleansValues() {
        val normalized = sample.normalized()
        assertEquals("Night drive", normalized.name)
        assertEquals(0xFF05070F.toInt(), normalized.background)
        assertEquals(LevyraCustomTheme.CORNER_SCALE_RANGE.last, normalized.cornerScale)
    }

    @Test
    fun storageRoundTripsAndRejectsGarbage() {
        val themes = listOf(sample.normalized())
        assertEquals(themes, LevyraCustomThemes.decode(LevyraCustomThemes.encode(themes)))
        assertTrue(LevyraCustomThemes.decode("{broken").isEmpty())
        assertTrue(LevyraCustomThemes.decode("""{"themes":[{"id":"preset","background":1,"surface":1,"primary":1,"secondary":1,"accent":1}]}""").isEmpty())
        assertTrue(LevyraCustomThemes.decode("""{"themes":[{"id":"custom:x","name":"no colors"}]}""").isEmpty())
    }

    @Test
    fun libraryOperationsKeepIdsUnique() {
        val saved = LevyraCustomThemes.upsert(emptyList(), sample)
        val duplicated = LevyraCustomThemes.duplicate(saved, sample.id, "copy")
        assertEquals(2, duplicated.size)
        assertEquals("Night drive copy", duplicated[1].name)
        assertTrue(duplicated[0].id != duplicated[1].id)
        val renamed = LevyraCustomThemes.upsert(duplicated, duplicated[0].copy(name = "Renamed"))
        assertEquals("Renamed", renamed[0].name)
        assertEquals(1, LevyraCustomThemes.remove(renamed, sample.id).size)
    }

    @Test
    fun duplicateKeepsSuffixForLongNames() {
        val long = sample.copy(name = "A".repeat(LevyraCustomTheme.MAX_NAME_LENGTH)).normalized()
        val duplicated = LevyraCustomThemes.duplicate(listOf(long), long.id, "copy")
        assertTrue(duplicated[1].name.endsWith(" copy"))
        assertTrue(duplicated[1].name.length <= LevyraCustomTheme.MAX_NAME_LENGTH)
    }

    @Test
    fun customIdsSurviveNormalizationAndResolveThroughRegistry() {
        assertEquals(sample.id, LevyraThemes.normalize(sample.id))
        assertEquals(LevyraThemes.levyraAura, LevyraThemes.byId(sample.id))
        LevyraThemes.setCustomThemes(listOf(sample.normalized()))
        val palette = LevyraThemes.byId(sample.id)
        assertEquals(sample.id, palette.id)
        assertEquals("Night drive", palette.label)
    }

    @Test
    fun customPaletteKeepsAccentsReadable() {
        val dim = sample.copy(primary = 0xFF0A0A20.toInt()).normalized().toPalette()
        val lum = { c: androidx.compose.ui.graphics.Color -> 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue }
        assertTrue(lum(dim.cyan) > lum(dim.black))
    }

    @Test
    fun hexParsingIsStrict() {
        assertEquals(0xFF4C7DFF.toInt(), parseThemeHex("#4c7dff"))
        assertNull(parseThemeHex("4C7DF"))
    }

    @Test
    fun themeLibraryCopyCoversEveryLanguage() {
        val expected = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(expected, themeLibraryLocalizationCodes())
        assertEquals(expected, themeLibraryCompleteLocalizationCodes())
    }
}
