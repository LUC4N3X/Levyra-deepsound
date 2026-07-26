package com.luc4n3x.levyra.desktop.core.storage

import com.luc4n3x.levyra.desktop.core.model.DesktopSettings
import com.luc4n3x.levyra.desktop.core.model.ThemeMode
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JsonFileStoreTest {

    private lateinit var directory: Path

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("levyra-store")
    }

    @After
    fun tearDown() {
        directory.toFile().deleteRecursively()
    }

    private fun newStore(name: String = "settings.json") = JsonFileStore(
        file = directory.resolve(name),
        serializer = DesktopSettings.serializer(),
        defaultValue = { DesktopSettings() }
    )

    @Test
    fun `missing file falls back to the default value`() {
        assertEquals(DesktopSettings(), newStore().read())
    }

    @Test
    fun `written values survive a reload`() {
        val store = newStore()
        store.write(DesktopSettings(themeMode = ThemeMode.LIGHT, volume = 42))

        val reloaded = newStore().read()
        assertEquals(ThemeMode.LIGHT, reloaded.themeMode)
        assertEquals(42, reloaded.volume)
    }

    @Test
    fun `corrupted files are quarantined and replaced by defaults`() {
        val file = directory.resolve("settings.json")
        Files.writeString(file, "{ not json", StandardCharsets.UTF_8)

        val store = newStore()
        assertEquals(DesktopSettings(), store.read())
        assertTrue(Files.isRegularFile(directory.resolve("settings.json.invalid")))
    }

    @Test
    fun `unknown fields are ignored`() {
        val file = directory.resolve("settings.json")
        Files.writeString(file, "{\"volume\":11,\"legacyField\":true}", StandardCharsets.UTF_8)

        assertEquals(11, newStore().read().volume)
    }

    @Test
    fun `settings are sanitised`() {
        val sanitized = DesktopSettings(contentCountry = " ita ", volume = 320, vlcDirectory = "  C:/VLC  ").sanitized()
        assertEquals("IT", sanitized.contentCountry)
        assertEquals(100, sanitized.volume)
        assertEquals("C:/VLC", sanitized.vlcDirectory)
    }
}
