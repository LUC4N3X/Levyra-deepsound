package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val OverlayScreenEndGap = 28.dp

@Composable
internal fun overlayScreenBottomPadding(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + OverlayScreenEndGap
