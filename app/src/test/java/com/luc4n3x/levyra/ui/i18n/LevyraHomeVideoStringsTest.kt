package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraHomeVideoStringsTest {

    @Test
    fun `every supported language has its own video shelf title`() {
        val english = LevyraStrings.forCode("en").homeVideoShelfTitle
        LevyraLanguageCatalog.languages.forEach { language ->
            val title = LevyraStrings.forCode(language.code).homeVideoShelfTitle
            assertTrue(language.code, title.isNotBlank())
            if (language.code != "en") assertTrue(language.code, title != english || language.code in sharedWithEnglish)
        }
    }

    @Test
    fun `english and italian describe the shelf plainly`() {
        assertEquals("Music videos", LevyraStrings.forCode("en").homeVideoShelfTitle)
        assertEquals("Videoclip musicali", LevyraStrings.forCode("it").homeVideoShelfTitle)
    }

    private val sharedWithEnglish = emptySet<String>()
}
