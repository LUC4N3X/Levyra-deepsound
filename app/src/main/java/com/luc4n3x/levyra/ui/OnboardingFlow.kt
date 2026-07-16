package com.luc4n3x.levyra.ui

internal enum class OnboardingStep {
    Language,
    Profile,
    Taste;

    fun next(): OnboardingStep = when (this) {
        Language -> Profile
        Profile -> Taste
        Taste -> Taste
    }

    fun previous(): OnboardingStep = when (this) {
        Language -> Language
        Profile -> Language
        Taste -> Profile
    }
}

internal fun onboardingPrimaryEnabled(
    step: OnboardingStep,
    selectedTasteCount: Int
): Boolean = step != OnboardingStep.Taste || selectedTasteCount >= 3
