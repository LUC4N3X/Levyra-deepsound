package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class OnboardingFlowTest {

    @Test
    fun `flow advances through intro language profile and taste`() {
        assertEquals(OnboardingStep.Language, OnboardingStep.Intro.next())
        assertEquals(OnboardingStep.Profile, OnboardingStep.Language.next())
        assertEquals(OnboardingStep.Taste, OnboardingStep.Profile.next())
        assertEquals(OnboardingStep.Taste, OnboardingStep.Taste.next())
    }

    @Test
    fun `flow returns to the previous completed step`() {
        assertEquals(OnboardingStep.Intro, OnboardingStep.Intro.previous())
        assertEquals(OnboardingStep.Intro, OnboardingStep.Language.previous())
        assertEquals(OnboardingStep.Language, OnboardingStep.Profile.previous())
        assertEquals(OnboardingStep.Profile, OnboardingStep.Taste.previous())
    }

    @Test
    fun `the intro stage owns the whole screen and stays out of the progress dots`() {
        assertFalse(onboardingShowsChrome(OnboardingStep.Intro))
        assertTrue(onboardingShowsChrome(OnboardingStep.Language))
        assertTrue(onboardingShowsChrome(OnboardingStep.Profile))
        assertTrue(onboardingShowsChrome(OnboardingStep.Taste))
        assertEquals(
            listOf(OnboardingStep.Language, OnboardingStep.Profile, OnboardingStep.Taste),
            onboardingProgressSteps()
        )
        assertEquals(0, onboardingProgressIndex(OnboardingStep.Intro))
        assertEquals(0, onboardingProgressIndex(OnboardingStep.Language))
        assertEquals(2, onboardingProgressIndex(OnboardingStep.Taste))
    }

    @Test
    fun `taste step requires at least three selections for primary action`() {
        assertTrue(onboardingPrimaryEnabled(OnboardingStep.Intro, selectedTasteCount = 0))
        assertTrue(onboardingPrimaryEnabled(OnboardingStep.Language, selectedTasteCount = 0))
        assertTrue(onboardingPrimaryEnabled(OnboardingStep.Profile, selectedTasteCount = 0))
        assertFalse(onboardingPrimaryEnabled(OnboardingStep.Taste, selectedTasteCount = 0))
        assertFalse(onboardingPrimaryEnabled(OnboardingStep.Taste, selectedTasteCount = 2))
        assertTrue(onboardingPrimaryEnabled(OnboardingStep.Taste, selectedTasteCount = 3))
    }

    @Test
    fun `taste labels follow the selected onboarding language`() {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
        ).firstOrNull(Files::exists) ?: error("LevyraApp.kt not found")
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
}
