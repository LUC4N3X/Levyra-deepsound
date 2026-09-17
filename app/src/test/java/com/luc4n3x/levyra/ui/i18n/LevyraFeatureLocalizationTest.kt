package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraFeatureLocalizationTest {

    @Test
    fun newBundlesCoverTheSameLanguagesAsTheJamBundle() {
        val reference = jamModerationLocalizationCodes()

        assertTrue(reference.size >= 26)
        assertEquals(reference, ambientModeLocalizationCodes())
        assertEquals(reference, themeStudioLocalizationCodes())
        assertEquals(reference, playerDeckLocalizationCodes())
        assertEquals(reference, playlistStudioLocalizationCodes())
        assertTrue("en" in reference)
        assertTrue("it" in reference)
    }

    @Test
    fun everyLanguageResolvesTheNewStandbyCopy() {
        LevyraStrings.all().forEach { strings ->
            listOf(
                strings.ambientLayout,
                strings.ambientLayoutSubtitle,
                strings.ambientShowClockSubtitle,
                strings.ambientShowTitleSubtitle,
                strings.ambientShowProgressSubtitle,
                strings.ambientAmoledBlackSubtitle,
                strings.ambientModeMinimal,
                strings.ambientModeArtwork,
                strings.ambientModeSpotlight,
                strings.ambientModeLyrics,
                strings.ambientShowClock,
                strings.ambientShowTitle,
                strings.ambientShowProgress,
                strings.ambientAmoledBlack
            ).forEach { value ->
                assertTrue("Blank standby copy for ${strings.code}", value.isNotBlank())
            }
        }
    }

    @Test
    fun everyLanguageResolvesTheJamModerationCopy() {
        LevyraStrings.all().forEach { strings ->
            listOf(
                strings.jamHostControls,
                strings.jamApprovalRequired,
                strings.jamApprovalRequiredSubtitle,
                strings.jamPendingRequests,
                strings.jamApprove,
                strings.jamReject,
                strings.jamKick,
                strings.jamBan,
                strings.jamBannedGuests,
                strings.jamClearBans,
                strings.jamLockSession,
                strings.jamLockSessionSubtitle,
                strings.jamLocked,
                strings.jamAwaitingApproval,
                strings.jamRejected,
                strings.jamBannedMessage,
                strings.jamSessionLockedMessage,
                strings.jamSessionFull,
                strings.jamRemovedMessage,
                strings.jamNoParticipants,
                strings.jamYou,
                strings.jamShareInvite
            ).forEach { value ->
                assertTrue("Blank Jam moderation copy for ${strings.code}", value.isNotBlank())
            }
        }
    }

    @Test
    fun everyLanguageResolvesTheThemeStudioCopy() {
        LevyraStrings.all().forEach { strings ->
            listOf(
                strings.themeStudio,
                strings.themeStudioSubtitle,
                strings.themeStudioPreview,
                strings.themeAccent,
                strings.themeAccentFromPreset,
                strings.themeAccentBlue,
                strings.themeAccentGreen,
                strings.themeAccentIndigo,
                strings.themeAccentOrange,
                strings.themeAccentPink,
                strings.themeAccentCyan,
                strings.themeAccentPurple,
                strings.themeAccentYellow
            ).forEach { value ->
                assertTrue("Blank Theme Studio copy for ${strings.code}", value.isNotBlank())
            }
        }
    }

    @Test
    fun coreLanguagesDoNotFallBackToEnglishForTheNewCopy() {
        val italian = LevyraStrings.forCode("it")
        val german = LevyraStrings.forCode("de")
        val english = LevyraStrings.forCode("en")

        assertEquals("Minimale", italian.ambientModeMinimal)
        assertEquals("Approva", italian.jamApprove)
        assertEquals("Anteprima", italian.themeStudioPreview)
        assertFalse(german.jamLockSession == english.jamLockSession)
        assertFalse(italian.ambientAmoledBlack == english.ambientAmoledBlack)
    }

    @Test
    fun everyLanguageResolvesThePlaylistStudioCopy() {
        LevyraStrings.all().forEach { strings ->
            listOf(
                strings.playlistStudio,
                strings.playlistStudioNew,
                strings.playlistStudioEdit,
                strings.playlistStudioOpen,
                strings.playlistStudioNameHint,
                strings.playlistStudioNameRequired,
                strings.playlistStudioCover,
                strings.playlistStudioCoverCurrent,
                strings.playlistStudioCoverAutomatic,
                strings.playlistStudioCoverArtwork,
                strings.playlistStudioCoverMosaic,
                strings.playlistStudioCoverSpotlight,
                strings.playlistStudioCoverSignal,
                strings.playlistStudioCoverPhoto,
                strings.playlistStudioChooseArtwork,
                strings.playlistStudioAddSongs,
                strings.playlistStudioSearchLibrary,
                strings.playlistStudioInPlaylist,
                strings.playlistStudioStateSaved,
                strings.playlistStudioStateUnsaved,
                strings.playlistStudioStateSaving,
                strings.playlistStudioStateFailed,
                strings.playlistStudioRetry,
                strings.playlistStudioUndo,
                strings.playlistStudioEmptyTitle,
                strings.playlistStudioEmptyBody,
                strings.playlistStudioLibraryEmpty,
                strings.playlistStudioLoading,
                strings.playlistStudioDiscardTitle,
                strings.playlistStudioDiscardBody,
                strings.playlistStudioDiscard,
                strings.playlistStudioKeepEditing
            ).forEach { value ->
                assertTrue("Blank Playlist Studio copy for ${strings.code}", value.isNotBlank())
            }
            val removed = strings.playlistStudioRemoved("Halo")
            val moved = strings.playlistStudioMoved("Halo")
            assertTrue("Removed copy for ${strings.code}", removed.contains("Halo") && !removed.contains("{title}"))
            assertTrue("Moved copy for ${strings.code}", moved.contains("Halo") && !moved.contains("{title}"))
        }
        assertEquals("Rimosso Halo", LevyraStrings.forCode("it").playlistStudioRemoved("Halo"))
    }
}
