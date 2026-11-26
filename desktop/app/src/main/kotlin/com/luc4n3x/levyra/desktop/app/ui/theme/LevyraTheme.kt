package com.luc4n3x.levyra.desktop.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.desktop.core.model.ThemeMode

private val LevyraCyan = Color(0xFF2997FF)
private val LevyraViolet = Color(0xFF818CF8)
private val LevyraPink = Color(0xFFEC4899)
private val LevyraDeepBlue = Color(0xFF030303)

private val DarkColors = darkColorScheme(
    primary = LevyraCyan,
    onPrimary = Color(0xFF030303),
    primaryContainer = Color(0xFF0B3A66),
    onPrimaryContainer = Color(0xFFD3E6FF),
    secondary = Color(0xFF98CFEA),
    onSecondary = Color(0xFF00344A),
    secondaryContainer = Color(0xFF1B2733),
    onSecondaryContainer = Color(0xFFD7ECF8),
    tertiary = LevyraViolet,
    onTertiary = Color(0xFF030303),
    tertiaryContainer = Color(0xFF2A2D52),
    onTertiaryContainer = Color(0xFFE0E2FF),
    background = Color(0xFF0B0B0C),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF0B0B0C),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF1D1D1F),
    onSurfaceVariant = Color(0xFF98989F),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFF030303),
    surfaceBright = Color(0xFF2D2D2F),
    surfaceContainerLowest = Color(0xFF030303),
    surfaceContainerLow = Color(0xFF141416),
    surfaceContainer = Color(0xFF1A1A1C),
    surfaceContainerHigh = Color(0xFF242426),
    surfaceContainerHighest = Color(0xFF2E2E31),
    inverseSurface = Color(0xFFF5F5F7),
    inverseOnSurface = Color(0xFF0A0A0A),
    inversePrimary = Color(0xFF0066CC),
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1AFFFFFF),
    error = Color(0xFFFF6B7A),
    onError = Color(0xFF030303),
    errorContainer = Color(0xFF4A1620),
    onErrorContainer = Color(0xFFFFD9DE)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0066CC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8FF),
    onPrimaryContainer = Color(0xFF00305F),
    secondary = Color(0xFF00708F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5ECF7),
    onSecondaryContainer = Color(0xFF00283A),
    tertiary = Color(0xFF5A63C8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE2E4FF),
    onTertiaryContainer = Color(0xFF151A5C),
    background = Color(0xFFF3F3F5),
    onBackground = Color(0xFF111114),
    surface = Color(0xFFF3F3F5),
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFE6E6EA),
    onSurfaceVariant = Color(0xFF5E5E66),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFFE2E2E6),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAFB),
    surfaceContainer = Color(0xFFEFEFF2),
    surfaceContainerHigh = Color(0xFFE7E7EB),
    surfaceContainerHighest = Color(0xFFDDDDE2),
    inverseSurface = Color(0xFF1D1D1F),
    inverseOnSurface = Color(0xFFF5F5F7),
    inversePrimary = LevyraCyan,
    outline = Color(0x29000000),
    outlineVariant = Color(0x14000000),
    error = Color(0xFFBA1A2B),
    onError = Color.White
)

private val LevyraTypography = Typography(
    displayLarge = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.SemiBold, lineHeight = 52.sp, letterSpacing = (-0.4).sp),
    displayMedium = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.SemiBold, lineHeight = 42.sp, letterSpacing = (-0.3).sp),
    displaySmall = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp, letterSpacing = (-0.2).sp),
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp, letterSpacing = (-0.2).sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp, letterSpacing = (-0.15).sp),
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp)
)

private val LevyraShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

object LevyraBrand {
    val cyan: Color get() = LevyraCyan
    val violet: Color get() = LevyraViolet
    val pink: Color get() = LevyraPink
    val deepBlue: Color get() = LevyraDeepBlue
    val soft: Color = Color(0xFF98CFEA)
    val onSoft: Color = Color(0xFF00344A)
}

object LevyraChrome {
    fun caption(dark: Boolean): Int = if (dark) 0x0B0B0C else 0xF3F3F5
    fun captionText(dark: Boolean): Int = if (dark) 0xD8D8DC else 0x1D1D1F
    fun border(dark: Boolean): Int = if (dark) 0x1F1F22 else 0xD8D8DE
}

object LevyraMotion {
    const val HOVER_ALPHA = 0.06f
    const val SELECTED_ALPHA = 0.10f
}

val LocalAccentColor = compositionLocalOf { LevyraCyan }

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
        shapes = LevyraShapes,
        content = content
    )
}
