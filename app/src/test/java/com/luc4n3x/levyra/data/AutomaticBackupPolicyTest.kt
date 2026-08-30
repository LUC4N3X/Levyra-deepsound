package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticBackupPolicyTest {
    @Test
    fun retentionKeepsNewestMatchingArchivesOnly() {
        val result = automaticBackupFilesToDelete(
            listOf(
                "levyra-auto-backup-100.levyra",
                "notes.txt",
                "levyra-auto-backup-300.levyra",
                "levyra-auto-backup-200.zip",
                "other-archive-500.levyra"
            ),
            retentionCount = 2
        )

        assertEquals(listOf("levyra-auto-backup-100.levyra"), result)
    }

    @Test
    fun retentionAcceptsThreeFiveTen() {
        val files = (1L..12L).map { "levyra-auto-backup-${1700000000000L + it}.levyra" }
        assertEquals(3, files.size - automaticBackupFilesToDelete(files, 3).size)
        assertEquals(5, files.size - automaticBackupFilesToDelete(files, 5).size)
        assertEquals(10, files.size - automaticBackupFilesToDelete(files, 10).size)
    }

    @Test
    fun automaticBackupNameMatchesLevyraAndLegacyZip() {
        assertTrue(isAutomaticBackupName("levyra-auto-backup-1700000000000.levyra"))
        assertTrue(isAutomaticBackupName("levyra-auto-backup-1700000000000.zip"))
        assertFalse(isAutomaticBackupName("levyra-auto-backup-1700000000000.txt"))
        assertFalse(isAutomaticBackupName("Levyra_2026-08-30_1255.levyra"))
        assertFalse(isAutomaticBackupName("../levyra-auto-backup-1.levyra"))
    }

    @Test
    fun vaultEntriesRejectTraversalAndUnknownNames() {
        assertTrue(vaultEntryAllowed("manifest.json"))
        assertTrue(vaultEntryAllowed("data/settings.json"))
        assertTrue(vaultEntryAllowed("data/favorites.json"))
        assertTrue(vaultEntryAllowed("data/followed_artists.json"))
        assertTrue(vaultEntryAllowed("data/playlists.json"))
        assertTrue(vaultEntryAllowed("data/history.json"))
        assertTrue(vaultEntryAllowed("data/queue.json"))
        assertTrue(vaultEntryAllowed("payload.json"))
        assertFalse(vaultEntryAllowed("../manifest.json"))
        assertFalse(vaultEntryAllowed("/etc/passwd"))
        assertFalse(vaultEntryAllowed("data/../../secret.json"))
        assertFalse(vaultEntryAllowed("..\\manifest.json"))
        assertFalse(vaultEntryAllowed("data/evil.json"))
        assertFalse(vaultEntryAllowed(""))
    }

    @Test
    fun vaultFileNameUsesLevyraPrefixAndExtension() {
        val name = levyraVaultFileName(0L)
        assertTrue(name.startsWith("Levyra_"))
        assertTrue(name.endsWith(".levyra"))
        assertFalse(name.contains(" "))
    }
}
