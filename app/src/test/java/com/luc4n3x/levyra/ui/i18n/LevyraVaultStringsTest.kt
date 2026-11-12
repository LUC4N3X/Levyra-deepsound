package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraVaultStringsTest {
    @Test
    fun everySupportedLanguageHasItsOwnVaultCopy() {
        val supportedCodes = LevyraStrings.all().map { it.code }.toSet()
        assertEquals(supportedCodes, vaultLocalizationCodes())
        val english = vaultLocalizationEntries("en")
        supportedCodes.forEach { code ->
            val entries = vaultLocalizationEntries(code)
            assertEquals("Incomplete bundle for $code", vaultKeys, entries.keys)
            assertTrue(entries.values.all { it.isNotBlank() })
            assertTrue("Missing count placeholder for $code", entries.getValue("vaultBackupCopies").contains("%d"))
            if (code != "en") {
                assertNotEquals("English fallback for $code", english["vaultSubtitle"], entries["vaultSubtitle"])
                assertNotEquals("English fallback for $code", english["vaultBackupNowSubtitle"], entries["vaultBackupNowSubtitle"])
            }
        }
    }

    @Test
    fun stringsAreExposedThroughLevyraStrings() {
        val russian = LevyraStrings.forCode("ru")
        assertEquals("Защищает плейлисты, избранное, историю и настройки", russian.vaultSubtitle)
        assertEquals("Копий: 5", russian.backupRetentionLabel(5))
        assertEquals("Каждый месяц", russian.backupFrequencyLabel("Monthly"))
        assertEquals("Backup ora", LevyraStrings.forCode("it").backupNow)
        assertEquals("3 copies", LevyraStrings.forCode("en").backupRetentionLabel(3))
    }
}
