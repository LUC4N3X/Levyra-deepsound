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
    fun automaticBackupNameMatchesLevyraNormalizedAndLegacyZip() {
        assertTrue(isAutomaticBackupName("levyra-auto-backup-1700000000000.levyra"))
        assertTrue(isAutomaticBackupName("levyra-auto-backup-1700000000000.levyra.zip"))
        assertTrue(isAutomaticBackupName("levyra-auto-backup-1700000000000.zip"))
        assertFalse(isAutomaticBackupName("levyra-auto-backup-.levyra"))
        assertFalse(isAutomaticBackupName("levyra-auto-backup-abc.levyra"))
        assertFalse(isAutomaticBackupName("levyra-auto-backup-1700000000000.txt"))
        assertFalse(isAutomaticBackupName("Levyra_2026-08-30_1255.levyra"))
        assertFalse(isAutomaticBackupName("../levyra-auto-backup-1.levyra"))
    }

    @Test
    fun retentionCoversProviderNormalizedNames() {
        val files = listOf(
            "levyra-auto-backup-1700000000010.levyra.zip",
            "levyra-auto-backup-1700000000020.levyra",
            "levyra-auto-backup-1700000000030.zip",
            "notes.txt",
            "Levyra_2026-08-30_1255.levyra"
        )
        val deleted = automaticBackupFilesToDelete(files, retentionCount = 2)
        assertEquals(listOf("levyra-auto-backup-1700000000010.levyra.zip"), deleted)
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
        assertTrue(vaultEntryAllowed("data/library_organization.json"))
        assertTrue(vaultEntryAllowed("payload.json"))
        assertFalse(vaultEntryAllowed("../manifest.json"))
        assertFalse(vaultEntryAllowed("/etc/passwd"))
        assertFalse(vaultEntryAllowed("data/../../secret.json"))
        assertFalse(vaultEntryAllowed("..\\manifest.json"))
        assertFalse(vaultEntryAllowed("data/evil.json"))
        assertFalse(vaultEntryAllowed(""))
    }

    @Test
    fun everyWrittenVaultSectionIsAcceptedOnRead() {
        val written = setOf(
            LevyraBackupManager.MANIFEST_ENTRY,
            LevyraBackupManager.SETTINGS_ENTRY,
            LevyraBackupManager.FAVORITES_ENTRY,
            LevyraBackupManager.FOLLOWED_ARTISTS_ENTRY,
            LevyraBackupManager.PLAYLISTS_ENTRY,
            LevyraBackupManager.ORGANIZATION_ENTRY,
            LevyraBackupManager.HISTORY_ENTRY,
            LevyraBackupManager.QUEUE_ENTRY
        )
        written.forEach { entry ->
            assertTrue("$entry is written but rejected on read", vaultEntryAllowed(entry))
        }
        assertTrue(written.size <= LevyraBackupManager.MAX_ZIP_ENTRIES)
    }

    @Test
    fun optionalVaultSectionsAreNotRequiredForRestore() {
        val legacyEntries = REQUIRED_VAULT_ENTRIES + LevyraBackupManager.MANIFEST_ENTRY
        assertTrue(vaultStructureCompatible(legacyEntries))
        assertFalse(LevyraBackupManager.ORGANIZATION_ENTRY in REQUIRED_VAULT_ENTRIES)
    }

    @Test
    fun vaultFileNameUsesLevyraPrefixAndExtension() {
        val name = levyraVaultFileName(0L)
        assertTrue(name.startsWith("Levyra_"))
        assertTrue(name.endsWith(".levyra"))
        assertFalse(name.contains(" "))
    }

    @Test
    fun missingRequiredVaultSectionIsRejected() {
        val complete = setOf(
            "manifest.json",
            "data/settings.json",
            "data/favorites.json",
            "data/followed_artists.json",
            "data/playlists.json",
            "data/history.json",
            "data/queue.json"
        )
        assertEquals(emptySet<String>(), missingRequiredVaultSections(complete))
        assertEquals(setOf("data/playlists.json"), missingRequiredVaultSections(complete - "data/playlists.json"))
        assertEquals(
            setOf("data/favorites.json", "data/queue.json"),
            missingRequiredVaultSections(complete - "data/favorites.json" - "data/queue.json")
        )
    }

    @Test
    fun previewStructuralCompatibilityMatchesRequiredSections() {
        val complete = setOf(
            "data/settings.json",
            "data/favorites.json",
            "data/followed_artists.json",
            "data/playlists.json",
            "data/history.json",
            "data/queue.json"
        )
        assertTrue(vaultStructureCompatible(complete))
        assertFalse(vaultStructureCompatible(complete - "data/playlists.json"))
        assertFalse(vaultStructureCompatible(complete - "data/favorites.json" - "data/queue.json"))
    }
}
