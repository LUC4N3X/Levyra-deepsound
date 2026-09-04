package com.luc4n3x.levyra.ui.ambient

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.luc4n3x.levyra.domain.LevyraAmbientSettings
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel

@Composable
internal fun LevyraAmbientOverlay(
    state: LevyraUiState,
    viewModel: LevyraViewModel,
    modifier: Modifier = Modifier
) {
    val ambientState = remember(
        state.currentTrack,
        state.isPlaying,
        state.positionMs,
        state.durationMs,
        state.activeLyric,
        state.motionArtwork,
        state.animationsEnabled,
        state.motionArtworkEnabled,
        state.interfaceSettings.canvasQuality,
        state.ambientSettings
    ) {
        state.toAmbientUiState()
    }

    BackHandler(enabled = true) { viewModel.closeAmbient() }
    AmbientWindowEffect(state.ambientSettings)

    AmbientScreen(
        state = ambientState,
        onTogglePlay = viewModel::togglePlay,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onExit = viewModel::closeAmbient,
        modifier = modifier
    )
}

internal fun LevyraUiState.toAmbientUiState(): AmbientUiState {
    val track = currentTrack
    return AmbientUiState(
        hasTrack = track != null,
        title = track?.title.orEmpty(),
        artist = track?.artist.orEmpty(),
        artworkUrl = track?.largeThumbnailUrl?.ifBlank { track.thumbnailUrl }.orEmpty(),
        isPlaying = isPlaying,
        positionMs = positionMs,
        durationMs = durationMs,
        lyricLine = activeLyric?.text.orEmpty(),
        motionArtwork = motionArtwork,
        animationsEnabled = animationsEnabled && motionArtworkEnabled,
        canvasQuality = interfaceSettings.canvasQuality,
        settings = ambientSettings
    )
}

@Composable
private fun AmbientWindowEffect(settings: LevyraAmbientSettings) {
    val view = LocalView.current
    val brightness = settings.normalized().brightness
    DisposableEffect(view, brightness) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            val previousBrightness = window.attributes.screenBrightness
            window.applyScreenBrightness(brightness)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                window.applyScreenBrightness(previousBrightness)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

private fun Window.applyScreenBrightness(value: Float) {
    attributes = attributes.apply { screenBrightness = value }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
