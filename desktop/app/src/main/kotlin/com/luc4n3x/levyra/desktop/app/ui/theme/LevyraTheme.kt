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

private val LevyraCyan = Color(0xFF27D9F5)
private val LevyraViolet = Color(0xFF8B7CFF)
private val LevyraDeepBlue = Color(0xFF0B1B35)

private val DarkColors = darkColorScheme(
    primary = LevyraCyan,
    onPrimary = Color(0xFF001B22),
    primaryContainer = Color(0xFF103744),
    onPrimaryContainer = Color(0xFFC7F6FF),
    secondary = LevyraViolet,
    onSecondary = Color(0xFF171033),
    secondaryContainer = Color(0xFF2B2857),
    onSecondaryContainer = Color(0xFFE8E4FF),
    tertiary = Color(0xFF50E3B3),
    onTertiary = Color(0xFF00271D),
    background = Color(0xFF07090D),
    onBackground = Color(0xFFF4F7FB),
    surface = Color(0xFF0B0E14),
    onSurface = Color(0xFFF4F7FB),
    surfaceVariant = Color(0xFF151A23),
    onSurfaceVariant = Color(0xFFA7B0BE),
    surfaceContainerLowest = Color(0xFF07090D),
    surfaceContainerLow = Color(0xFF0B0E14),
    surfaceContainer = Color(0xFF0F131B),
    surfaceContainerHigh = Color(0xFF151A23),
    surfaceContainerHighest = Color(0xFF1B2230),
    outline = Color(0xFF303949),
    outlineVariant = Color(0xFF202734),
    error = Color(0xFFFF7485),
    onError = Color(0xFF3B000B),
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
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF11151C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF11151C),
    surfaceVariant = Color(0xFFE9EDF3),
    onSurfaceVariant = Color(0xFF4A5360),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF0F3F7),
    surfaceContainerHigh = Color(0xFFE8ECF2),
    surfaceContainerHighest = Color(0xFFE0E5EC),
    outline = Color(0xFFBBC4CF),
    outlineVariant = Color(0xFFD4DAE2),
    error = Color(0xFFBA1A2B),
    onError = Color.White
)

private val LevyraTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.6).sp,
        lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontSize = 21.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        lineHeight = 27.sp
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.35.sp
    )
)

private val LevyraShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object LevyraBrand {
    val cyan: Color get() = LevyraCyan
    val violet: Color get() = LevyraViolet
    val deepBlue: Color get() = LevyraDeepBlue
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
