package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LevyraBlack = Color(0xFF030407)
val LevyraInk = Color(0xFF08090E)
val LevyraPanel = Color(0xFF0E101A)
val LevyraPanelSoft = Color(0xFF151827)
val LevyraCyan = Color(0xFF00F5FF)
val LevyraBlue = Color(0xFF0066FF)
val LevyraViolet = Color(0xFF7C3AED)
val LevyraPink = Color(0xFFEC4899)
val LevyraOrange = Color(0xFFF97316)
val LevyraText = Color(0xFFF8FAFC)
val LevyraMuted = Color(0xFF94A3B8)

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
