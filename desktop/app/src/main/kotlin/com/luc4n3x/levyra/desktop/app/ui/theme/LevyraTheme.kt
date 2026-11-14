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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.desktop.core.model.ThemeMode

private val LevyraCyan = Color(0xFF2997FF) // Apple Sky Link Blue for dark, Action Blue for light
private val LevyraViolet = Color(0xFFA58BFF)
private val LevyraDeepBlue = Color(0xFF000000)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2997FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF004499),
    onPrimaryContainer = Color(0xFFD4F8FF),
    secondary = Color(0xFF7A7A7A), // Muted text/icons
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFF5EE0B6),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF101418),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF171C21),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF252D34),
    onSurfaceVariant = Color(0xFFCCCCCC),
    surfaceContainerLowest = Color(0xFF0B0F13),
    surfaceContainerLow = Color(0xFF171C21),
    surfaceContainer = Color(0xFF1D242B),
    surfaceContainerHigh = Color(0xFF273039),
    surfaceContainerHighest = Color(0xFF323C46),
    outline = Color(0xFF404040),
    outlineVariant = Color(0xFF333333),
    error = Color(0xFFFF453A),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF4A1620),
    onErrorContainer = Color(0xFFFFD9DE)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0066CC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5F0FF),
    onPrimaryContainer = Color(0xFF004499),
    secondary = Color(0xFF7A7A7A), // Muted text/icons
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF1D1D1F),
    tertiary = Color(0xFF007A5D),
    onTertiary = Color.White,
    background = Color(0xFFF7FAFE),
    onBackground = Color(0xFF1D1D1F),
    surface = Color(0xFFF5F5F7), // Apple Parchment
    onSurface = Color(0xFF1D1D1F),
    surfaceVariant = Color(0xFFE8ECF2),
    onSurfaceVariant = Color(0xFF333333),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFC),
    surfaceContainer = Color(0xFFF5F5F7),
    surfaceContainerHigh = Color(0xFFE0E0E0),
    surfaceContainerHighest = Color(0xFFCCCCCC),
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFF0F0F0),
    error = Color(0xFFBA1A2B),
    onError = Color.White
)

private val LevyraTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 40.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.28).sp,
        lineHeight = 44.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.37).sp,
        lineHeight = 38.sp
    ),
    headlineSmall = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontSize = 21.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.23.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.37).sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.2).sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.37).sp,
        lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.24).sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.08).sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.22).sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.12).sp
    ),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.08).sp
    )
)

private val LevyraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
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
fun LevyraTheme(themeMode: ThemeMode, accent: Color = LevyraCyan, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = remember(dark, accent) {
        val base = if (dark) DarkColors else LightColors
        val primary = lerp(accent, if (dark) Color.White else Color.Black, if (dark) 0.38f else 0.3f)
        val onPrimary = if (primary.luminance() > 0.179f) Color.Black else Color.White
        base.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = lerp(base.surface, primary, if (dark) 0.23f else 0.12f),
            onPrimaryContainer = base.onSurface,
            surfaceTint = primary,
            secondaryContainer = lerp(base.surface, primary, 0.09f),
            onSecondaryContainer = base.onSurface
        )
    }
    MaterialTheme(
        colorScheme = colors,
        typography = LevyraTypography,
        shapes = LevyraShapes,
        content = content
    )
}
