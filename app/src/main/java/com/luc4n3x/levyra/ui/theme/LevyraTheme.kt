package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LevyraBlack = Color(0xFF000000) // True OLED Black
val LevyraInk = Color(0xFF0A0A0A) // Slightly elevated neutral black
val LevyraPanel = Color(0xFF121214) // Premium soft gray for panels (Vercel/Linear style)
val LevyraPanelSoft = Color(0xFF18181B) // Slightly lighter premium gray (Zinc 900)
val LevyraCyan = Color(0xFF00E5FF) // Vibrant neon cyan
val LevyraBlue = Color(0xFF2563EB) // Stronger brand blue
val LevyraViolet = Color(0xFF9D4EDD) // Deep vibrant violet
val LevyraPink = Color(0xFFFF4D85) // Sharp neon pink
val LevyraOrange = Color(0xFFFF7A00) // Punchy neon orange
val LevyraText = Color(0xFFF4F4F5) // Zinc 50 for pristine off-white
val LevyraMuted = Color(0xFFA1A1AA) // Zinc 400 for muted text

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
