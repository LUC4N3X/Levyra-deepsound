package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.jamQrKeys
import com.luc4n3x.levyra.ui.i18n.jamQrLocalizationEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JamQrLocalizationTest {
    @Test
    fun everyLanguageResolvesQrActionsWithAnIntentionalEnglishFallback() {
        LevyraStrings.all().forEach { strings ->
            assertTrue(strings.entryKeys.containsAll(jamQrKeys))
            jamQrKeys.forEach { assertTrue(strings.value(it).isNotBlank()) }
        }
        assertEquals(jamQrLocalizationEntries("en"), jamQrLocalizationEntries("unknown"))
    }
}
