package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferLocalAudioCopyTest {

    @Test
    fun copyCoversEveryCatalogLanguage() {
        val catalog = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalog, preferLocalAudioLocalizationCodes())
        catalog.forEach { code ->
            val copy = preferLocalAudioCopy(code)
            listOf(copy.title, copy.subtitle, copy.permissionNeeded).forEach {
                assertTrue("$code has blank copy", it.isNotBlank())
            }
        }
    }
}
