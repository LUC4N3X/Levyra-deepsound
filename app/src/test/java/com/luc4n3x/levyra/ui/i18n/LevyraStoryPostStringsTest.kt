package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.data.storypost.StoryPostImportError
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraStoryPostStringsTest {

    private fun assertComplete(label: String, values: List<String>) {
        assertEquals(label, LevyraStoryPostStrings.KEY_COUNT, values.size)
        values.forEachIndexed { index, value -> assertTrue("$label[$index] blank", value.isNotBlank()) }
        assertTrue("$label[5] {n}", values[5].contains("{n}"))
        assertTrue("$label[14] {n}", values[14].contains("{n}"))
        assertTrue("$label[14] {total}", values[14].contains("{total}"))
    }

    @Test
    fun everyLanguageHasEveryStringWithPlaceholders() {
        LevyraStoryPostCatalog.codes().forEach { code -> assertComplete(code, LevyraStoryPostCatalog.rawValues(code)) }
        assertComplete(LevyraLanguageCatalog.BRAZILIAN_PORTUGUESE, LevyraStoryPostCatalog.brazilianValues())
    }

    @Test
    fun errorSlotsMatchImportErrors() {
        assertEquals(StoryPostImportError.entries.size, LevyraStoryPostStrings.ERROR_COUNT)
        assertEquals(LevyraStoryPostStrings.KEY_COUNT, LevyraStoryPostStrings.ERROR_OFFSET + LevyraStoryPostStrings.ERROR_COUNT)
        val english = LevyraStoryPostCatalog.forCode("en")
        StoryPostImportError.entries.forEach { error -> assertTrue(english.error(error.ordinal).isNotBlank()) }
    }

    @Test
    fun brazilianPortugueseUsesItsOwnText() {
        val brazil = LevyraStoryPostCatalog.forCode(LevyraLanguageCatalog.BRAZILIAN_PORTUGUESE)
        val portugal = LevyraStoryPostCatalog.forCode("pt")
        assertEquals(LevyraStoryPostCatalog.brazilianValues()[10], brazil.reading)
        assertNotEquals(portugal.reading, brazil.reading)
    }

    @Test
    fun formattedStringsFillPlaceholders() {
        val english = LevyraStoryPostCatalog.forCode("en")
        assertTrue(english.photos(3).contains("3"))
        assertEquals("3 photos", english.photos(3))
        assertEquals("Photo 2 of 5", english.photoPosition(2, 5))
    }
}
