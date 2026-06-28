package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LevyraBlack = Color(0xFF050A1F)
val LevyraInk = Color(0xFF081229)
val LevyraPanel = Color(0xFF0E1A38)
val LevyraPanelSoft = Color(0xFF16254B)
val LevyraCyan = Color(0xFF00E5FF)
val LevyraBlue = Color(0xFF0055FF)
val LevyraViolet = Color(0xFF9D00FF)
val LevyraPink = Color(0xFFFF007A)
val LevyraOrange = Color(0xFFFF3D00)
val LevyraText = Color(0xFFF8FAFC)
val LevyraMuted = Color(0xFF8892B0)

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
