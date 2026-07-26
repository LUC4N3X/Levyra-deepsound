package com.luc4n3x.levyra.desktop.app.state

import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfflinePartialFileMigrationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun legacyPartialIsMovedToHashedNameWithoutLosingProgress() {
        val downloads = temporaryFolder.newFolder("offline").toPath()
        val legacy = downloads.resolve("Artist - Track [legacy].m4a.part")
        val target = downloads.resolve("Artist - Track [0123456789abcdef].m4a.part")
        val content = ByteArray(16_384) { index -> (index % 251).toByte() }
        Files.write(legacy, content)

        val resumedBytes = OfflinePartialFileMigration.prepare(
            downloadsDirectory = downloads,
            recordedPath = legacy.toString(),
            targetPath = target
        )

        assertEquals(content.size.toLong(), resumedBytes)
        assertFalse(Files.exists(legacy))
        assertTrue(Files.isRegularFile(target))
        assertArrayEquals(content, Files.readAllBytes(target))
    }

    @Test
    fun largerLegacyPartialReplacesSmallerTarget() {
        val downloads = temporaryFolder.newFolder("offline-conflict").toPath()
        val legacy = downloads.resolve("old-name.webm.part")
        val target = downloads.resolve("new-name.webm.part")
        val legacyContent = ByteArray(8_192) { 7 }
        Files.write(legacy, legacyContent)
        Files.write(target, ByteArray(512) { 3 })

        val resumedBytes = OfflinePartialFileMigration.prepare(
            downloadsDirectory = downloads,
            recordedPath = legacy.toString(),
            targetPath = target
        )

        assertEquals(legacyContent.size.toLong(), resumedBytes)
        assertFalse(Files.exists(legacy))
        assertArrayEquals(legacyContent, Files.readAllBytes(target))
    }

    @Test
    fun recordedPathOutsideDownloadDirectoryIsIgnored() {
        val downloads = temporaryFolder.newFolder("offline-safe").toPath()
        val outside = temporaryFolder.newFile("outside.m4a.part").toPath()
        val target = downloads.resolve("safe-name.m4a.part")
        Files.writeString(outside, "do not touch")

        val resumedBytes = OfflinePartialFileMigration.prepare(
            downloadsDirectory = downloads,
            recordedPath = outside.toString(),
            targetPath = target
        )

        assertEquals(0L, resumedBytes)
        assertTrue(Files.isRegularFile(outside))
        assertFalse(Files.exists(target))
    }
}
