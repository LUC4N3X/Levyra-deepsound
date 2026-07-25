package com.luc4n3x.levyra.desktop.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.desktop.core.model.ThemeMode

private val LevyraCyan = Color(0xFF20E7FF)
private val LevyraViolet = Color(0xFF8E57FF)
private val LevyraDeepBlue = Color(0xFF091A3D)

private val DarkColors = darkColorScheme(
    primary = LevyraCyan,
    onPrimary = Color(0xFF00202B),
    primaryContainer = Color(0xFF00384A),
    onPrimaryContainer = Color(0xFFB4F1FF),
    secondary = LevyraViolet,
    onSecondary = Color(0xFF1B0A3B),
    secondaryContainer = Color(0xFF33207A),
    onSecondaryContainer = Color(0xFFE2D6FF),
    tertiary = Color(0xFF35FFC3),
    onTertiary = Color(0xFF00382A),
    background = Color(0xFF05070D),
    onBackground = Color(0xFFEDF2F8),
    surface = Color(0xFF080B14),
    onSurface = Color(0xFFEDF2F8),
    surfaceVariant = Color(0xFF141A28),
    onSurfaceVariant = Color(0xFFA9B4C6),
    surfaceContainer = Color(0xFF0D1220),
    surfaceContainerHigh = Color(0xFF141A28),
    surfaceContainerHighest = Color(0xFF1A2233),
    outline = Color(0xFF2C3648),
    outlineVariant = Color(0xFF1E2635),
    error = Color(0xFFFF6B7A),
    onError = Color(0xFF3B000A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0075A6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F1FF),
    onPrimaryContainer = Color(0xFF001F2B),
    secondary = Color(0xFF5B34C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6DBFF),
    onSecondaryContainer = Color(0xFF1B0A3B),
    tertiary = Color(0xFF00875F),
    onTertiary = Color.White,
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF10141C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10141C),
    surfaceVariant = Color(0xFFE9EEF6),
    onSurfaceVariant = Color(0xFF4A5468),
    surfaceContainer = Color(0xFFF1F4FA),
    surfaceContainerHigh = Color(0xFFE9EEF6),
    surfaceContainerHighest = Color(0xFFE1E8F2),
    outline = Color(0xFFC3CCDA),
    outlineVariant = Color(0xFFD9E1EC),
    error = Color(0xFFBA1A2B),
    onError = Color.White
)

private val LevyraTypography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

object LevyraBrand {
    val cyan: Color get() = LevyraCyan
    val violet: Color get() = LevyraViolet
    val deepBlue: Color get() = LevyraDeepBlue
}

@Composable
fun LevyraTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = LevyraTypography,
        content = content
    )
}
