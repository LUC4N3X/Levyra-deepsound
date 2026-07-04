package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LevyraBlack = Color(0xFF03020A)
val LevyraInk = Color(0xFF090814)
val LevyraPanel = Color(0xFF12101D)
val LevyraPanelSoft = Color(0xFF1A1728)
val LevyraCyan = Color(0xFF67E8FF)
val LevyraBlue = Color(0xFF5C8CFF)
val LevyraViolet = Color(0xFFA78BFA)
val LevyraPink = Color(0xFFFF5C9A)
val LevyraOrange = Color(0xFFFFA24A)
val LevyraText = Color(0xFFF8F7FF)
val LevyraMuted = Color(0xFFB9B2CC)

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
    outline = Color(0x3300E5FF)
)

@Composable
fun LevyraTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = LevyraScheme,
        content = content
    )
}
