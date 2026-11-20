package com.luc4n3x.levyra.ui.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LevyraStudioTheme(content: @Composable () -> Unit) {
    if (!LevyraUtilityStyleActive) {
        content()
        return
    }
    val tone = levyraUtilityColors(LevyraUtilitySeed)
    val base = MaterialTheme.colorScheme
    val scheme = remember(base, tone) {
        base.copy(
            primary = tone.accent,
            onPrimary = tone.onAccent,
            primaryContainer = tone.container,
            onPrimaryContainer = tone.onContainer,
            secondaryContainer = base.surfaceContainerHighest,
            onSecondaryContainer = base.onSurface,
            surfaceTint = tone.accent
        )
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MaterialTheme.motionScheme,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        content = content
    )
}
