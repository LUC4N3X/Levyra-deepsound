package com.luc4n3x.levyra.ui

internal enum class OnboardingStep {
    Intro,
    Language,
    Appearance,
    Profile,
    Taste,
    YoutubeMusic;

    fun next(): OnboardingStep = when (this) {
        Intro -> Language
        Language -> Appearance
        Appearance -> Profile
        Profile -> Taste
        Taste -> YoutubeMusic
        YoutubeMusic -> YoutubeMusic
    }

    fun previous(): OnboardingStep = when (this) {
        Intro -> Intro
        Language -> Intro
        Appearance -> Language
        Profile -> Appearance
        Taste -> Profile
        YoutubeMusic -> Taste
    }
}

internal fun onboardingPrimaryEnabled(
    step: OnboardingStep,
    selectedTasteCount: Int
): Boolean = step != OnboardingStep.Taste || selectedTasteCount >= 3

internal fun onboardingShowsChrome(step: OnboardingStep): Boolean = step != OnboardingStep.Intro

internal fun onboardingProgressSteps(): List<OnboardingStep> =
    OnboardingStep.entries.filter(::onboardingShowsChrome)

internal fun onboardingProgressIndex(step: OnboardingStep): Int =
    onboardingProgressSteps().indexOf(step).coerceAtLeast(0)

internal fun preserveProfileNameInput(input: String): String = input
