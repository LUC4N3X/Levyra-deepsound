package com.luc4n3x.levyra.desktop.core.account

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.DriverManager
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BrowserCookieImportTest {
    private val home: Path = Files.createTempDirectory("levyra-home")
    private val now = System.currentTimeMillis() / 1_000

    @After
    fun tearDown() {
        Files.walk(home).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
    }

    private fun linux() = BrowserCookieImport(home, { null }, windows = false, keyringPassword = { null })

    private fun firefoxProfile(rows: List<List<Any>>, containers: Boolean = true) {
        val root = Files.createDirectories(home.resolve(".mozilla/firefox"))
        Files.writeString(
            root.resolve("profiles.ini"),
            "[General]\nStartWithLastProfile=1\n\n[Profile0]\nName=default-release\nIsRelative=1\nPath=abc.default-release\n"
        )
        val directory = Files.createDirectories(root.resolve("abc.default-release"))
        DriverManager.getConnection("jdbc:sqlite:${directory.resolve("cookies.sqlite")}").use { connection ->
            val extra = if (containers) ", originAttributes TEXT" else ""
            connection.createStatement().use { it.execute("CREATE TABLE moz_cookies (host TEXT, name TEXT, value TEXT, path TEXT, expiry INTEGER$extra)") }
            rows.forEach { row ->
                val values = if (containers) row else row.take(5)
                val marks = values.joinToString(", ") { "?" }
                connection.prepareStatement("INSERT INTO moz_cookies VALUES ($marks)").use { statement ->
                    values.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
                    statement.executeUpdate()
                }
            }
        }
    }

    @Test
    fun firefoxSignInBecomesACookieHeader() = runBlocking {
        firefoxProfile(listOf(
            listOf(".youtube.com", "SAPISID", "sap", "/", now + 3_600, ""),
            listOf(".youtube.com", "SID", "sid", "/", (now + 3_600) * 1_000, ""),
            listOf(".youtube.com", "OLD", "gone", "/", now - 10, ""),
            listOf(".youtube.com", "SID", "container", "/", now + 3_600, "^userContextId=2"),
            listOf("www.youtube.com", "PREF", "www-only", "/", now + 3_600, ""),
            listOf(".google.com", "NID", "google", "/", now + 3_600, "")
        ))
        val import = linux()
        val profile = import.profiles().single()
        assertEquals(BrowserKind.Firefox, profile.kind)
        assertEquals("default-release", profile.name)
        assertEquals("SAPISID=sap; SID=sid", import.cookieHeader(profile))
    }

    @Test
    fun signedOutFirefoxIsReported() = runBlocking {
        firefoxProfile(listOf(listOf(".youtube.com", "PREF", "f6=1", "/", now + 3_600, "")), containers = false)
        val import = linux()
        try {
            import.cookieHeader(import.profiles().single())
            fail("signed-out profile imported")
        } catch (error: BrowserImportException) {
            assertEquals(BrowserImportFailure.NotSignedIn, error.failure)
        }
    }

    @Test
    fun profilesIniKeepsRelativeAndAbsolutePaths() {
        val parsed = parseProfilesIni(
            listOf("[Install4F96]", "Default=x", "[Profile1]", "Name=Work", "IsRelative=0", "Path=/data/work", "[Profile0]", "Path=abc.default")
        )
        assertEquals(listOf(Triple("Work", false, "/data/work"), Triple("abc.default", true, "abc.default")), parsed)
    }

    @Test
    fun chromiumValuesDecryptForEveryKeyVersion() {
        assertEquals("plain", decryptChromium(chromiumEncrypt("v10", "peanuts", "plain".toByteArray()), ".youtube.com", 23) { null })
        val hashed = MessageDigest.getInstance("SHA-256").digest(".youtube.com".toByteArray()) + "hashed".toByteArray()
        assertEquals("hashed", decryptChromium(chromiumEncrypt("v10", "peanuts", hashed), ".youtube.com", 24) { null })
        assertEquals("keyring", decryptChromium(chromiumEncrypt("v11", "secret", "keyring".toByteArray()), ".youtube.com", 23) { "secret".toByteArray() })
        try {
            decryptChromium(chromiumEncrypt("v11", "secret", "keyring".toByteArray()), ".youtube.com", 23) { null }
            fail("v11 decrypted without its key")
        } catch (error: BrowserImportException) {
            assertEquals(BrowserImportFailure.KeyUnavailable, error.failure)
        }
    }

    @Test
    fun chromiumOnWindowsIsListedAsProtected() = runBlocking {
        val network = Files.createDirectories(home.resolve("AppData/Local/Google/Chrome/User Data/Default/Network"))
        Files.write(network.resolve("Cookies"), byteArrayOf(0))
        val import = BrowserCookieImport(home, { null }, windows = true, keyringPassword = { null })
        val chrome = import.profiles().single()
        assertEquals(BrowserKind.Chrome, chrome.kind)
        assertEquals(BrowserProfileStatus.ProtectedOnWindows, chrome.status)
    }

    @Test
    fun cookieHeaderPrefersTheMusicHostAndDropsUnsafeValues() {
        val header = youtubeCookieHeader(listOf(
            BrowserCookie(".youtube.com", "PREF", "global", "/", 0),
            BrowserCookie("music.youtube.com", "PREF", "music", "/", 0),
            BrowserCookie(".youtube.com", "BAD", "a;b", "/", 0),
            BrowserCookie(".youtube.com", "DEEP", "x", "/watch", 0)
        ), now)
        assertEquals("PREF=music", header)
        assertEquals(0L, chromiumExpiry(0))
        assertEquals(1_700_000_000L, chromiumExpiry((1_700_000_000L + 11_644_473_600L) * 1_000_000L))
    }

    private fun chromiumEncrypt(prefix: String, password: String, plain: ByteArray): ByteArray {
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            .generateSecret(PBEKeySpec(password.toCharArray(), "saltysalt".toByteArray(), 1, 128)).encoded
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(ByteArray(16) { ' '.code.toByte() }))
        return prefix.toByteArray() + cipher.doFinal(plain)
    }
}
