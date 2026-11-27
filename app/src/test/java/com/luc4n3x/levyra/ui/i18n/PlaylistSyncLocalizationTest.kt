package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSyncLocalizationTest {
    private val catalog = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet()

    @Test
    fun everyCatalogLanguageHasCompleteSyncCopy() {
        assertEquals(emptySet<String>(), catalog - playlistSyncCompleteLocalizationCodes())
        assertEquals(playlistSyncLocalizationCodes(), playlistSyncCompleteLocalizationCodes())
    }

    @Test
    fun placeholdersSurviveEveryTranslation() {
        catalog.forEach { code ->
            val copy = LevyraStrings.forCode(code).playlistSyncCopy
            listOf(PlaylistSyncText.Ready, PlaylistSyncText.Unmatched, PlaylistSyncText.KeptAlbums, PlaylistSyncText.Rejected).forEach { key ->
                assertTrue("$code $key", copy.format(key, count = 7319).contains("7319"))
            }
            assertTrue("$code CreateNewBody", copy.format(PlaylistSyncText.CreateNewBody, name = "Mira").contains("Mira"))
            assertTrue("$code LastSynced", copy.format(PlaylistSyncText.LastSynced, time = "09:41").contains("09:41"))
            PlaylistSyncText.entries.forEach { key -> assertTrue("$code $key", copy[key].isNotBlank()) }
        }
    }

    @Test
    fun localizedCopyIsUsedInsteadOfEnglish() {
        val english = LevyraStrings.forCode("en").playlistSyncCopy
        assertEquals("Sync with YouTube Music", english[PlaylistSyncText.MenuAction])
        catalog.filterNot { it == "en" }.forEach { code ->
            assertNotEquals(code, english[PlaylistSyncText.Intro], LevyraStrings.forCode(code).playlistSyncCopy[PlaylistSyncText.Intro])
        }
        assertNotEquals(
            LevyraStrings.forCode("pt").playlistSyncCopy[PlaylistSyncText.SignIn],
            LevyraStrings.forCode(LevyraLanguageCatalog.BRAZILIAN_PORTUGUESE).playlistSyncCopy[PlaylistSyncText.SignIn]
        )
    }
}
