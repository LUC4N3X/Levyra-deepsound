package com.luc4n3x.levyra.desktop.core.localmusic

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMediaLocationTest {

    @Test
    fun resolvesRegularFileFromDirectPath() {
        val temp = Files.createTempFile("test_normal", ".mp3")
        try {
            val resolved = resolveLocalFile(temp.toString())
            assertNotNull(resolved)
            assertEquals(temp.toRealPath(), resolved!!.toRealPath())
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    @Test
    fun resolvesFileWithSpacesAndSpecialCharacters() {
        val temp = Files.createTempFile("test audio with spaces and (brackets)", ".wav")
        try {
            val resolvedDirect = resolveLocalFile(temp.toString())
            assertNotNull(resolvedDirect)
            assertEquals(temp.toRealPath(), resolvedDirect!!.toRealPath())

            val uriString = temp.toUri().toASCIIString()
            val resolvedUri = resolveLocalFile(uriString)
            assertNotNull(resolvedUri)
            assertEquals(temp.toRealPath(), resolvedUri!!.toRealPath())
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    @Test
    fun resolvesFileFromUnencodedFileUri() {
        val temp = Files.createTempFile("test unencoded space", ".mp3")
        try {
            val rawUri = "file:///" + temp.toString().replace('\\', '/')
            val resolved = resolveLocalFile(rawUri)
            assertNotNull(resolved)
            assertEquals(temp.toRealPath(), resolved!!.toRealPath())
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    @Test
    fun returnsNullForBlankOrNonExistentFile() {
        assertNull(resolveLocalFile(""))
        assertNull(resolveLocalFile("   "))
        assertNull(resolveLocalFile("C:\\non_existent_folder_12345\\non_existent_file.mp3"))
    }

    @Test
    fun matchesMediaUrlHandlesFormatDifferences() {
        val mrl = "file:///C:/Music/Artist%20-%20Song%20(2026).mp3"
        val path = "C:\\Music\\Artist - Song (2026).mp3"
        val lowerMrl = "file:///c:/music/artist%20-%20song%20(2026).mp3"

        assertTrue(matchesMediaUrl(mrl, path))
        assertTrue(matchesMediaUrl(path, mrl))
        assertTrue(matchesMediaUrl(mrl, lowerMrl))
        assertTrue(matchesMediaUrl(mrl, mrl))
    }
}
