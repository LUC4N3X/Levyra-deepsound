package com.luc4n3x.levyra.ui

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun LevyraUiState.shouldKeepScreenOn(): Boolean =
    showAmbient || (interfaceSettings.keepScreenOnWhilePlaying && isPlaying)

@Composable
internal fun LevyraKeepScreenOnEffect(uiState: StateFlow<LevyraUiState>) {
    val keepScreenOnFlow = remember(uiState) {
        uiState.map(LevyraUiState::shouldKeepScreenOn).distinctUntilChanged()
    }
    val keepScreenOn by keepScreenOnFlow.collectAsStateWithLifecycle(
        initialValue = uiState.value.shouldKeepScreenOn()
    )
    val window = LocalActivity.current?.window
    DisposableEffect(window, keepScreenOn) {
        if (window == null || !keepScreenOn) {
            onDispose { }
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}
