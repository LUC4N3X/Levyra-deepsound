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
                strings.themeAccentFromPreset
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
}
