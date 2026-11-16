package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.vaultSyncFileCopy
import java.io.FileNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSyncFileTest {

    @Test
    fun writesOnlyWhenLinkedEnabledAndPermitted() {
        assertTrue(syncFileShouldWrite(linked = true, writesEnabled = true, hasPermission = true))
        assertFalse(syncFileShouldWrite(linked = false, writesEnabled = true, hasPermission = true))
        assertFalse(syncFileShouldWrite(linked = true, writesEnabled = false, hasPermission = true))
        assertFalse(syncFileShouldWrite(linked = true, writesEnabled = true, hasPermission = false))
    }

    @Test
    fun truncatingModeIsTriedFirst() {
        val modes = mutableListOf<String>()
        val opened = openTruncatingOrOverwriting { mode -> modes += mode; mode }
        assertEquals("wt", opened)
        assertEquals(listOf("wt"), modes)
    }

    @Test
    fun unsupportedTruncateFallsBackToTruncatingReadWrite() {
        listOf(FileNotFoundException("wt"), IllegalArgumentException("wt"), UnsupportedOperationException("wt")).forEach { refusal ->
            val modes = mutableListOf<String>()
            val opened = openTruncatingOrOverwriting { mode ->
                modes += mode
                if (mode == "wt") throw refusal
                mode
            }
            assertEquals("rwt", opened)
            assertEquals(listOf("wt", "rwt"), modes)
        }
    }

    @Test(expected = SecurityException::class)
    fun lostPermissionIsNotRetried() {
        openTruncatingOrOverwriting<String> { mode ->
            if (mode == "rwt") throw AssertionError("retried")
            throw SecurityException("gone")
        }
    }

    @Test
    fun nullStreamIsReturnedAsIs() {
        assertNull(openTruncatingOrOverwriting<String> { null })
    }

    @Test
    fun everyLanguageResolvesSyncFileCopy() {
        LevyraStrings.all().forEach { strings ->
            val copy = strings.vaultSyncFileCopy()
            listOf(copy.title, copy.steps, copy.create, copy.useExisting, copy.accessLost, copy.backupSavedNotSynced).forEach { text ->
                assertFalse(strings.code, text.isBlank())
            }
            assertTrue(strings.code, copy.updated("12:00").contains("12:00"))
            assertFalse(strings.code, copy.updated("12:00").contains("{time}"))
        }
        assertTrue(LevyraStrings.forCode("en").vaultSyncFileCopy().steps.contains("Google Drive"))
    }
}
