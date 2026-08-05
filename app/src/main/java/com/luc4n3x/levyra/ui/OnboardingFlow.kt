package com.luc4n3x.levyra.ui

internal enum class OnboardingStep {
    Intro,
    Language,
    Profile,
    Taste;

    fun next(): OnboardingStep = when (this) {
        Intro -> Language
        Language -> Profile
        Profile -> Taste
        Taste -> Taste
    }

    fun previous(): OnboardingStep = when (this) {
        Intro -> Intro
        Language -> Intro
        Profile -> Language
        Taste -> Profile
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
