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
    onPrimary = Color(0xFF001A20),
    primaryContainer = Color(0xFF12343E),
    onPrimaryContainer = Color(0xFFD4F8FF),
    secondary = LevyraViolet,
    onSecondary = Color(0xFF1B103B),
    secondaryContainer = Color(0xFF30275A),
    onSecondaryContainer = Color(0xFFECE6FF),
    tertiary = Color(0xFF5EE0B6),
    onTertiary = Color(0xFF00271D),
    background = Color(0xFF060708),
    onBackground = Color(0xFFF5F7FA),
    surface = Color(0xFF0A0C0F),
    onSurface = Color(0xFFF5F7FA),
    surfaceVariant = Color(0xFF171B22),
    onSurfaceVariant = Color(0xFFAAB2BE),
    surfaceContainerLowest = Color(0xFF060708),
    surfaceContainerLow = Color(0xFF0A0C0F),
    surfaceContainer = Color(0xFF101318),
    surfaceContainerHigh = Color(0xFF171B22),
    surfaceContainerHighest = Color(0xFF202631),
    outline = Color(0xFF35404F),
    outlineVariant = Color(0xFF252C36),
    error = Color(0xFFFF7585),
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
    displaySmall = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.7).sp,
        lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.45).sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontSize = 21.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.25).sp,
        lineHeight = 27.sp
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.15).sp,
        lineHeight = 25.sp
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
        letterSpacing = 0.05.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.15.sp
    ),
    labelSmall = TextStyle(
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.3.sp
    )
)

private val LevyraShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(17.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

object LevyraBrand {
    val cyan: Color get() = LevyraCyan
    val violet: Color get() = LevyraViolet
    val deepBlue: Color get() = LevyraDeepBlue
}

object LevyraMotion {
    const val HOVER_ALPHA = 0.78f
    const val SELECTED_ALPHA = 0.18f
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
