package com.luc4n3x.levyra.desktop.core.account

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SecretBoxTest {
    @Test
    fun keyedBoxRoundTripsAndRejectsTampering() {
        val key = ByteArray(32) { it.toByte() }
        val box = KeyedSecretBox { key.copyOf() }
        val sealed = box.seal("session".toByteArray())
        assertArrayEquals("session".toByteArray(), box.open(sealed))
        sealed[sealed.lastIndex] = (sealed.last().toInt() xor 1).toByte()
        try {
            box.open(sealed)
            fail("tampered data opened")
        } catch (_: SecretBoxUnavailable) {
        }
    }

    @Test
    fun windowsBoxUsesTheUserProfile() {
        if (!System.getProperty("os.name").orEmpty().startsWith("Windows")) return
        val box = WindowsSecretBox()
        val sealed = box.seal("session".toByteArray())
        assertTrue(!String(sealed, Charsets.ISO_8859_1).contains("session"))
        assertArrayEquals("session".toByteArray(), box.open(sealed))
    }

    @Test
    fun fileKeyIsStableAndPrivate() {
        val root = Files.createTempDirectory("levyra-key")
        val file = root.resolve("youtube-music.key")
        val first = SecretKeys.fileKey(file)
        assertEquals(32, first.size)
        assertArrayEquals(first, SecretKeys.fileKey(file))
        runCatching { Files.getPosixFilePermissions(file) }.getOrNull()?.let { permissions ->
            assertEquals(setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), permissions)
        }
    }

    @Test
    fun unreadableSessionFileMeansSignedOut() = runBlocking {
        val root = Files.createTempDirectory("levyra-accounts")
        val file = root.resolve("youtube-music-session.bin")
        Files.write(file, byteArrayOf(1, 2, 3))
        val accounts = DesktopYoutubeMusicAccounts(file, KeyedSecretBox { ByteArray(32) })
        accounts.load()
        assertTrue(accounts.accounts.value.isEmpty())
    }
}
