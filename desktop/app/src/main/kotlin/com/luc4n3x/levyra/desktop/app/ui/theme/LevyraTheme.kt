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

private val LevyraCyan = Color(0xFF4DE6FF)
private val LevyraViolet = Color(0xFFA58BFF)
private val LevyraDeepBlue = Color(0xFF0A1830)

private val DarkColors = darkColorScheme(
    primary = LevyraCyan,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF12343E),
    onPrimaryContainer = Color(0xFFD4F8FF),
    secondary = LevyraViolet,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF30275A),
    onSecondaryContainer = Color(0xFFECE6FF),
    tertiary = Color(0xFF5EE0B6),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0x0AFFFFFF), // White 4%
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0x12FFFFFF), // White 7%
    onSurfaceVariant = Color(0x99FFFFFF), // White 60%
    surfaceContainerLowest = Color(0x00FFFFFF), // Transparent
    surfaceContainerLow = Color(0x08FFFFFF), // White 3%
    surfaceContainer = Color(0x0FFFFFFF), // White 6%
    surfaceContainerHigh = Color(0x17FFFFFF), // White 9%
    surfaceContainerHighest = Color(0x1EFFFFFF), // White 12%
    outline = Color(0x26FFFFFF), // White 15%
    outlineVariant = Color(0x0DFFFFFF), // White 5%
    error = Color(0xFFFF453A),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF4A1620),
    onErrorContainer = Color(0xFFFFD9DE)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B7D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC0F3FF),
    onPrimaryContainer = Color(0xFF001F27),
    secondary = Color(0xFF5C55B5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E0FF),
    onSecondaryContainer = Color(0xFF18143B),
    tertiary = Color(0xFF007A5D),
    onTertiary = Color.White,
    background = Color(0xFFF3F5F8),
    onBackground = Color(0xFF11151C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF11151C),
    surfaceVariant = Color(0xFFE8ECF2),
    onSurfaceVariant = Color(0xFF4A5360),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF0F3F7),
    surfaceContainerHigh = Color(0xFFE7EBF1),
    surfaceContainerHighest = Color(0xFFDDE3EA),
    outline = Color(0xFFB8C2CE),
    outlineVariant = Color(0xFFD2D9E2),
    error = Color(0xFFBA1A2B),
    onError = Color.White
)

private val LevyraTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.2).sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.1).sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.3.sp
    )
)

private val LevyraShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object LevyraBrand {
    val cyan: Color get() = LevyraCyan
    val violet: Color get() = LevyraViolet
    val deepBlue: Color get() = LevyraDeepBlue
}

object LevyraMotion {
    const val HOVER_ALPHA = 0.08f
    const val SELECTED_ALPHA = 0.12f
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
