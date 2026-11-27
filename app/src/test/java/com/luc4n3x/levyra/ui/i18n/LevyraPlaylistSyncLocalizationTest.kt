package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncErrorKind
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncSummary
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraPlaylistSyncLocalizationTest {

    @Test
    fun supportedLanguagesProvideCompleteAndDistinctSyncCopy() {
        val en = playlistSyncCopy("en")
        val it = playlistSyncCopy("it")
        val ru = playlistSyncCopy("ru")

        listOf(en, it, ru).forEach { copy ->
            assertTrue(copy.action.isNotBlank())
            assertTrue(copy.syncWithLevyra.isNotBlank())
            assertTrue(copy.syncNow.isNotBlank())
            assertTrue(copy.syncing.isNotBlank())
            assertTrue(copy.syncedBadge.isNotBlank())
            assertTrue(copy.sheetTitle.isNotBlank())
            assertTrue(copy.subtitle(5, 3).isNotBlank())
            assertTrue(copy.emptySubtitle.isNotBlank())
            assertTrue(copy.twoWayNote.isNotBlank())
            assertTrue(copy.signedOutTitle.isNotBlank())
            assertTrue(copy.signedOutMessage.isNotBlank())
            assertTrue(copy.signInButton.isNotBlank())
            assertTrue(copy.accountConnected("user@example.com").isNotBlank())
            assertTrue(copy.createPrivateTitle.isNotBlank())
            assertTrue(copy.createPrivateSubtitle.isNotBlank())
            assertTrue(copy.existingSection.isNotBlank())
            assertTrue(copy.loadingRemote.isNotBlank())
            assertTrue(copy.emptyRemote.isNotBlank())
            assertTrue(copy.retry.isNotBlank())
            assertTrue(copy.autoSyncTitle.isNotBlank())
            assertTrue(copy.autoSyncSubtitle.isNotBlank())
            assertTrue(copy.linkedHeader.isNotBlank())
            assertTrue(copy.unmatchedNote(2).isNotBlank())
            assertTrue(copy.openRemote.isNotBlank())
            assertTrue(copy.unlinkButton.isNotBlank())
            assertTrue(copy.unlinkNote.isNotBlank())
            assertTrue(copy.errorTitle.isNotBlank())
            assertTrue(copy.lastSyncedLabel(0L).isNotBlank())
            assertTrue(copy.lastSyncedLabel(1_700_000_000_000L).isNotBlank())
            assertTrue(copy.summaryText(PlaylistSyncSummary()).isNotBlank())
            assertTrue(
                copy.summaryText(
                    PlaylistSyncSummary(
                        addedLocally = 1,
                        removedLocally = 2,
                        addedRemotely = 3,
                        removedRemotely = 1,
                        renamedLocally = true
                    )
                ).isNotBlank()
            )
            PlaylistSyncErrorKind.entries.forEach { kind ->
                assertTrue(copy.errorMessage(kind).isNotBlank())
            }
        }

        assertNotEquals(en.sheetTitle, it.sheetTitle)
        assertNotEquals(en.sheetTitle, ru.sheetTitle)
        assertNotEquals(en.syncWithLevyra, it.syncWithLevyra)
        assertNotEquals(en.syncWithLevyra, ru.syncWithLevyra)
    }

    @Test
    fun regionTagsNormalizeToBaseLanguageCopy() {
        assertTrue(playlistSyncCopy("it-IT").syncNow == playlistSyncCopy("it").syncNow)
        assertTrue(playlistSyncCopy("ru-RU").syncNow == playlistSyncCopy("ru").syncNow)
        assertTrue(playlistSyncCopy("en-US").syncNow == playlistSyncCopy("en").syncNow)
    }
}
