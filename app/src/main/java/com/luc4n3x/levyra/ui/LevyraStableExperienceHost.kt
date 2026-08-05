package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.viewmodel.LevyraViewModel

/**
 * Stable runtime host used while the redesigned motion player remains under
 * device-level validation. The production-proven LevyraApp owns player,
 * mini-player, navigation, overlays and input routing, so no duplicate player
 * surfaces or experimental full-screen graphics layers can be composed.
 *
 * The new introduction remains enabled and hands off to the existing onboarding
 * flow without changing persisted preferences.
 */
@Composable
fun LevyraStableExperienceHost(
    viewModel: LevyraViewModel,
    isInPictureInPicture: Boolean = false
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var introVisible by rememberSaveable(state.showOnboarding) {
        mutableStateOf(state.showOnboarding)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LevyraApp(
            viewModel = viewModel,
            isInPictureInPicture = isInPictureInPicture
        )

        if (!isInPictureInPicture && state.showOnboarding && introVisible) {
            LevyraIntroExperience(
                languageCode = state.languageCode,
                onContinue = { introVisible = false }
            )
        }
    }
}
