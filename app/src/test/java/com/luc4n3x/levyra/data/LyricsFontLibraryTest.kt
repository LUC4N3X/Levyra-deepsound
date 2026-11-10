package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LevyraLyricsStudioSettings
import com.luc4n3x.levyra.domain.LyricsFontFormat
import com.luc4n3x.levyra.domain.LyricsFontTestFonts
import com.luc4n3x.levyra.domain.LyricsStylePreset
import com.luc4n3x.levyra.domain.LyricsTextSize
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LyricsFontLibraryTest {
    @get:Rule val folder = TemporaryFolder()

    private val directory: File get() = File(folder.root, "lyrics_fonts")
    private var renderable = true

    private fun library() = LyricsFontLibrary(directory, { _, _ -> renderable }, clock = { 1_731_260_000_000L })

    private fun source(bytes: ByteArray): () -> InputStream = { ByteArrayInputStream(bytes) }

    private fun expectError(expected: LyricsFontError, block: suspend () -> Unit) = runBlocking {
        try {
            block()
            fail("Expected $expected")
        } catch (error: LyricsFontException) {
            assertEquals(expected, error.error)
        }
    }

    private fun fontFiles() = directory.listFiles().orEmpty().filter { it.extension == "ttf" || it.extension == "otf" }

    @Test
    fun importedFontPersistsAcrossInstances() = runBlocking {
        val bytes = LyricsFontTestFonts.trueType("Noto Sans TC", 100..900)
        val imported = library().import(source(bytes), "fallback")
        assertEquals("Noto Sans TC", imported.name)
        assertEquals(100..900, imported.variableWeight)
        assertEquals(listOf(imported), library().load())
        val (entry, file) = library().fontFile(imported.id)!!
        assertEquals(imported, entry)
        assertTrue(file.readBytes().contentEquals(bytes))
    }

    @Test
    fun fallbackNameIsUsedWhenFontHasNoFamilyName() = runBlocking {
        val entry = library().import(source(LyricsFontTestFonts.openType(family = "")), "My Lyrics Font")
        assertEquals("My Lyrics Font", entry.name)
        assertEquals(LyricsFontFormat.OPEN_TYPE, entry.format)
    }

    @Test
    fun invalidFilesAreRejectedWithoutLeavingFiles() {
        val library = library()
        expectError(LyricsFontError.INVALID) { library.import(source("<html>".toByteArray()), "page") }
        expectError(LyricsFontError.INVALID) { library.import(source(ByteArray(0)), "empty") }
        expectError(LyricsFontError.UNREADABLE) { library.import({ null }, "missing") }
        renderable = false
        expectError(LyricsFontError.INVALID) { library.import(source(LyricsFontTestFonts.trueType()), "broken") }
        assertTrue(runBlocking { library.load() }.isEmpty())
        assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(".import-") })
        assertTrue(fontFiles().isEmpty())
    }

    @Test
    fun oversizedFontIsRejected() {
        val library = library()
        val huge = LyricsFontTestFonts.trueType() + ByteArray(LyricsFontLibrary.MAX_FONT_BYTES.toInt())
        expectError(LyricsFontError.TOO_LARGE) { library.import(source(huge), "huge") }
        assertTrue(fontFiles().isEmpty())
    }

    @Test
    fun libraryCountLimitIsEnforced() {
        val library = library()
        runBlocking { repeat(LyricsFontLibrary.MAX_FONTS) { library.import(source(LyricsFontTestFonts.trueType("F$it")), "f") } }
        expectError(LyricsFontError.LIBRARY_FULL) { library.import(source(LyricsFontTestFonts.trueType()), "extra") }
    }

    @Test
    fun renameReplaceAndDeleteUpdateIndexAndFiles() = runBlocking {
        val library = library()
        val entry = library.import(source(LyricsFontTestFonts.trueType("Old")), "old")
        assertEquals("Karaoke Bold", library.rename(entry.id, "  Karaoke   Bold ")!!.name)
        assertNull(library.rename(entry.id, "   "))
        val replaced = library.replace(entry.id, source(LyricsFontTestFonts.openType("New")))
        assertEquals(entry.id, replaced.id)
        assertEquals("Karaoke Bold", replaced.name)
        assertEquals(LyricsFontFormat.OPEN_TYPE, replaced.format)
        assertNotEquals(entry.fileName, replaced.fileName)
        assertEquals(listOf(replaced.fileName), fontFiles().map { it.name })
        assertEquals(listOf(replaced), library().load())
        assertTrue(library.delete(entry.id))
        assertFalse(library.delete(entry.id))
        assertTrue(fontFiles().isEmpty())
        assertTrue(library().load().isEmpty())
    }

    @Test
    fun failedReplaceKeepsOriginalFont() = runBlocking {
        val library = library()
        val entry = library.import(source(LyricsFontTestFonts.trueType("Keep")), "keep")
        try {
            library.replace(entry.id, source("junk".toByteArray()))
            fail()
        } catch (expected: LyricsFontException) {
            assertEquals(LyricsFontError.INVALID, expected.error)
        }
        assertEquals(listOf(entry), library().load())
        assertTrue(File(directory, entry.fileName).isFile)
    }

    @Test
    fun corruptIndexOrMissingFileFallsBackSafely() = runBlocking {
        val entry = library().import(source(LyricsFontTestFonts.trueType()), "x")
        File(directory, entry.fileName).delete()
        assertTrue(library().load().isEmpty())
        assertNull(library().fontFile(entry.id))
        File(directory, "index.json").writeText("{broken")
        assertTrue(library().load().isEmpty())
    }

    @Test
    fun backupRoundTripRestoresFontsAndReplacesLibrary() = runBlocking {
        val kept = library().import(source(LyricsFontTestFonts.trueType("Backup Sans", 200..800)), "b")
        val items = library().backupItems()
        assertEquals(1, items.size)
        val json = JSONArray().apply { items.forEach { put(lyricsFontEntryToJson(it.entry)) } }
        val archive = items.associate { lyricsFontBackupEntry(it.entry.id, it.entry.format) to it.bytes }
        assertTrue(archive.keys.all(::lyricsFontBackupEntryAllowed))
        val parsed = parseLyricsFontBackup(JSONArray(json.toString()), archive)

        val device = library()
        assertTrue(device.delete(kept.id))
        val stale = device.import(source(LyricsFontTestFonts.trueType("Stale")), "s")
        device.commitRestore(device.stageRestore(parsed))
        val restored = library().load()
        assertEquals(listOf(kept.id, stale.id), restored.map { it.id })
        assertEquals("Backup Sans", restored.first().name)
        assertEquals(200..800, restored.first().variableWeight)
        assertTrue(library().fontFile(stale.id) != null)
        assertEquals(2, fontFiles().size)
    }

    @Test
    fun discardedRestoreLeavesLibraryUntouched() = runBlocking {
        val current = library().import(source(LyricsFontTestFonts.trueType("Current")), "c")
        val library = library()
        val staged = library.stageRestore(emptyList())
        library.discardRestore(staged)
        assertEquals(listOf(current), library().load())
        assertTrue(directory.listFiles().orEmpty().none { it.isDirectory })
    }

    @Test
    fun restoreDropsInvalidBackupFonts() = runBlocking {
        val good = library().import(source(LyricsFontTestFonts.trueType("Good")), "g")
        val items = library().backupItems()
        val forged = LyricsFontBackupItem(good.copy(id = "11111111-2222-3333-4444-555555555555"), "junk".toByteArray())
        val traversal = LyricsFontBackupItem(good.copy(id = "../../evil"), items.single().bytes)
        val library = library()
        library.commitRestore(library.stageRestore(items + forged + traversal))
        assertEquals(listOf(good.id), library().load().map { it.id })
        assertFalse(lyricsFontBackupEntryAllowed("fonts/lyrics/../../evil.ttf"))
        assertFalse(lyricsFontBackupEntryAllowed("fonts/lyrics/${good.id}.woff"))
    }

    @Test
    fun customFontSelectionSurvivesEncodingAndPresets() {
        val settings = LevyraLyricsStudioSettings(customFontId = "abc", textSize = LyricsTextSize.HUGE)
        val decoded = LevyraLyricsStudioSettings.decode(settings.encode())
        assertEquals("abc", decoded.customFontId)
        assertEquals(LyricsTextSize.HUGE, decoded.textSize)
        assertEquals("abc", decoded.applyPreset(LyricsStylePreset.CINEMATIC).customFontId)
        assertEquals("", LevyraLyricsStudioSettings.decode("{\"fontPreset\":\"Inter\"}").customFontId)
    }
}
