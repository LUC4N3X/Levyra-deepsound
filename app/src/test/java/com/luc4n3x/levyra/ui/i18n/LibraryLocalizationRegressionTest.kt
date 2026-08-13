package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class LibraryLocalizationRegressionTest {
    @Test
    fun libraryUiUsesSharedLocalizationInsteadOfItalianEnglishBranches() {
        val root = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/ui/library"),
            Path.of("src/main/java/com/luc4n3x/levyra/ui/library")
        ).firstOrNull(Files::exists) ?: error("Library UI sources not found")

        val files = listOf(
            "LevyraLibraryScreen.kt",
            "LibraryRows.kt",
            "LibraryOverviewComponents.kt",
            "LibraryActions.kt",
            "LibraryPlaylistImportUi.kt"
        ).map(root::resolve)

        files.forEach { file ->
            assertTrue("Missing Library source: $file", Files.exists(file))
        }

        val content = files.joinToString("\n") { Files.readString(it) }
        listOf(
            "val isItalian",
            "if (isItalian)",
            "\"Your library\"",
            "\"La tua libreria\"",
            "\"Search your music\"",
            "\"Cerca nella tua musica\"",
            "\"Quick access\"",
            "\"Accesso rapido\""
        ).forEach { leak ->
            assertFalse("Binary Italian/English Library localization leak: $leak", content.contains(leak))
        }

        assertTrue(content.contains("LocalLevyraStrings.current"))
        assertTrue(content.contains("formatTrackCount"))
    }
    @Test
    fun libraryValueFormattersFollowSelectedLocale() {
        val strings = LevyraStrings.forCode("it")
        assertEquals("1 h 30 min", strings.formatLibraryDuration(90L * 60_000L))
        assertEquals("1,5 GB", strings.formatLibraryBytes((1.5 * 1024 * 1024 * 1024).toLong()))
    }

}
