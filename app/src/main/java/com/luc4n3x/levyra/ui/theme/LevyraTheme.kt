package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LevyraBlack = Color(0xFF050505) // Deeper, neutral black
val LevyraInk = Color(0xFF0A0A0A) // Slightly elevated neutral black
val LevyraPanel = Color(0xFF141414) // Soft panel color
val LevyraPanelSoft = Color(0xFF1C1C1E) // Premium soft gray for panels (Apple/Linear style)
val LevyraCyan = Color(0xFF4AC2F5) // Delicate light blue/cyan
val LevyraBlue = Color(0xFF3B82F6) // Calmer blue
val LevyraViolet = Color(0xFF9370DB) // Softer violet (Medium Purple)
val LevyraPink = Color(0xFFFF7EB3) // Delicate pastel pink
val LevyraOrange = Color(0xFFFF9F43) // Soft orange
val LevyraText = Color(0xFFFAFAFA) // Very soft off-white
val LevyraMuted = Color(0xFFA1A1AA) // Zinc-400 for muted text

private val LevyraScheme: ColorScheme = darkColorScheme(
    primary = LevyraCyan,
    onPrimary = LevyraBlack,
    secondary = LevyraViolet,
    onSecondary = LevyraText,
    tertiary = LevyraPink,
    background = LevyraBlack,
    onBackground = LevyraText,
    surface = LevyraInk,
    onSurface = LevyraText,
    surfaceVariant = LevyraPanel,
    onSurfaceVariant = LevyraMuted,
    outline = Color(0x3348E9FF)
)

@Composable
fun LevyraTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = LevyraScheme,
        content = content
    )
}
