package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.*
import org.junit.Test

class RewindHealthLocalizationTest {
    @Test fun everySupportedLanguageHasEveryTextAndMatchingCoveragePlaceholder() {
        assertEquals(LevyraLanguageCatalog.languages.map { it.code }.toSet(), musicExperienceCopies.keys)
        musicExperienceCopies.forEach { (code, copy) ->
            assertEquals(code, MusicExperienceText.entries.toSet(), copy.keys)
            assertTrue(code, copy.values.all { it.isNotBlank() })
            assertEquals(code, 1, copy.getValue(MusicExperienceText.Coverage).windowed(2).count { it == "%s" })
            assertEquals(code, copy.getValue(MusicExperienceText.Chapters), LevyraStrings.forCode(code).musicExperience(MusicExperienceText.Chapters))
        }
    }
}
