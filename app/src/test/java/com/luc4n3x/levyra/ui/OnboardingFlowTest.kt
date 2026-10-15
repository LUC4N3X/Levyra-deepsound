package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraOnboardingAppearanceStrings
import com.luc4n3x.levyra.ui.i18n.LevyraOnboardingYoutubeMusicStrings
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class OnboardingFlowTest {

    @Test
    fun `flow advances through intro language appearance profile taste and youtube music`() {
        assertEquals(OnboardingStep.Language, OnboardingStep.Intro.next())
        assertEquals(OnboardingStep.Appearance, OnboardingStep.Language.next())
        assertEquals(OnboardingStep.Profile, OnboardingStep.Appearance.next())
        assertEquals(OnboardingStep.Taste, OnboardingStep.Profile.next())
        assertEquals(OnboardingStep.YoutubeMusic, OnboardingStep.Taste.next())
        assertEquals(OnboardingStep.YoutubeMusic, OnboardingStep.YoutubeMusic.next())
    }

    @Test
    fun `flow returns to the previous completed step`() {
        assertEquals(OnboardingStep.Intro, OnboardingStep.Intro.previous())
        assertEquals(OnboardingStep.Intro, OnboardingStep.Language.previous())
        assertEquals(OnboardingStep.Language, OnboardingStep.Appearance.previous())
        assertEquals(OnboardingStep.Appearance, OnboardingStep.Profile.previous())
        assertEquals(OnboardingStep.Profile, OnboardingStep.Taste.previous())
        assertEquals(OnboardingStep.Taste, OnboardingStep.YoutubeMusic.previous())
    }

    @Test
    fun `the intro stage owns the whole screen and stays out of the progress dots`() {
        assertFalse(onboardingShowsChrome(OnboardingStep.Intro))
        assertTrue(onboardingShowsChrome(OnboardingStep.Language))
        assertTrue(onboardingShowsChrome(OnboardingStep.Appearance))
        assertTrue(onboardingShowsChrome(OnboardingStep.Profile))
        assertTrue(onboardingShowsChrome(OnboardingStep.Taste))
        assertTrue(onboardingShowsChrome(OnboardingStep.YoutubeMusic))
        assertEquals(
            listOf(OnboardingStep.Language, OnboardingStep.Appearance, OnboardingStep.Profile, OnboardingStep.Taste, OnboardingStep.YoutubeMusic),
            onboardingProgressSteps()
        )
        assertEquals(0, onboardingProgressIndex(OnboardingStep.Intro))
        assertEquals(0, onboardingProgressIndex(OnboardingStep.Language))
        assertEquals(3, onboardingProgressIndex(OnboardingStep.Taste))
        assertEquals(4, onboardingProgressIndex(OnboardingStep.YoutubeMusic))
    }

    @Test
    fun `taste step requires at least three selections for primary action`() {
        assertTrue(onboardingPrimaryEnabled(OnboardingStep.Intro, selectedTasteCount = 0))
        assertTrue(onboardingPrimaryEnabled(OnboardingStep.Language, selectedTasteCount = 0))
        assertTrue(onboardingPrimaryEnabled(OnboardingStep.Profile, selectedTasteCount = 0))
        assertFalse(onboardingPrimaryEnabled(OnboardingStep.Taste, selectedTasteCount = 0))
        assertFalse(onboardingPrimaryEnabled(OnboardingStep.Taste, selectedTasteCount = 2))
        assertTrue(onboardingPrimaryEnabled(OnboardingStep.Taste, selectedTasteCount = 3))
        assertTrue(onboardingPrimaryEnabled(OnboardingStep.YoutubeMusic, selectedTasteCount = 0))
    }

    @Test
    fun `taste labels follow the selected onboarding language`() {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/ui/OnboardingSetup.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/ui/OnboardingSetup.kt")
        ).firstOrNull(Files::exists) ?: error("OnboardingSetup.kt not found")
        val content = Files.readString(source)

        assertTrue(content.contains("val tastes = remember(languageCode)"))
        assertTrue(content.contains("moodEngine.tastesForLanguage(languageCode)"))
        assertFalse(content.contains("OnboardingOverlay(tastes = state.tastes"))
    }

    @Test
    fun `profile name is preserved exactly as entered`() {
        listOf("e.e. cummings", " lowercase", "iPhone DJ", "Élodie", "").forEach { input ->
            assertEquals(input, preserveProfileNameInput(input))
        }
    }

    @Test
    fun `youtube music setup copy is translated for every supported language`() {
        val english = LevyraOnboardingYoutubeMusicStrings.forCode("en")
        LevyraLanguageCatalog.languages.map { it.code }.filter { it != "en" }.forEach { code ->
            val copy = LevyraOnboardingYoutubeMusicStrings.forCode(code)
            val fields = listOf(copy.title, copy.body, copy.optionalBody, copy.secureBody, copy.skip, copy.connectedBody, copy.signInError)
            assertTrue(code, fields.all(String::isNotBlank))
            assertTrue(code, copy.body != english.body && copy.skip != english.skip && copy.secureBody != english.secureBody)
        }
    }

    @Test
    fun `appearance setup copy is translated for every supported language`() {
        val english = LevyraOnboardingAppearanceStrings.forCode("en")
        LevyraLanguageCatalog.languages.map { it.code }.filter { it != "en" }.forEach { code ->
            val copy = LevyraOnboardingAppearanceStrings.forCode(code)
            val fields = listOf(copy.title, copy.body, copy.dark, copy.darkBody, copy.light, copy.lightBody, copy.footnote)
            assertTrue(code, fields.all(String::isNotBlank))
            assertTrue(code, copy.title != english.title && copy.lightBody != english.lightBody)
        }
    }
}
