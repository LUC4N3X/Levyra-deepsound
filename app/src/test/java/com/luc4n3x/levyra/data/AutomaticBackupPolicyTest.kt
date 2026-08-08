package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AutomaticBackupPolicyTest {
    @Test
    fun retentionKeepsNewestMatchingArchivesOnly() {
        val result = automaticBackupFilesToDelete(
            listOf(
                "levyra-auto-backup-100.zip",
                "notes.txt",
                "levyra-auto-backup-300.zip",
                "levyra-auto-backup-200.zip"
            ),
            retentionCount = 2
        )

        assertEquals(listOf("levyra-auto-backup-100.zip"), result)
    }
}
