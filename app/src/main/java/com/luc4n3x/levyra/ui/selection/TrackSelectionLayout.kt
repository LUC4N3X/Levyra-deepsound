package com.luc4n3x.levyra.ui.selection

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val LocalTrackSelectionChromeHeight = staticCompositionLocalOf<State<Int>?> { null }

internal fun selectionBottomObstruction(chrome: Int, navigation: Int, keyboard: Int): Int =
    maxOf(chrome, navigation, keyboard)

@Composable
internal fun trackSelectionBottomInset(aboveRootDock: Boolean): Dp {
    val density = LocalDensity.current
    val chrome = if (aboveRootDock) LocalTrackSelectionChromeHeight.current?.value ?: 0 else 0
    val obstruction = selectionBottomObstruction(
        chrome = chrome,
        navigation = WindowInsets.navigationBars.getBottom(density),
        keyboard = WindowInsets.ime.getBottom(density)
    )
    return with(density) { obstruction.toDp() }
}

@Composable
internal fun TrackSelectionState.listBottomPadding(base: Dp): Dp =
    if (isActive && dockHeightPx > 0) with(LocalDensity.current) { dockHeightPx.toDp() } + SelectionDockGap else base

internal val SelectionDockGap = 12.dp
