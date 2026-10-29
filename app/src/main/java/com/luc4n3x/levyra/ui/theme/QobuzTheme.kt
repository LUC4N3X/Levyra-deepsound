package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val QobuzPaletteId = "levyra_qobuz_style"
val LevyraIsQobuz: Boolean get() = LevyraActivePalette.id == QobuzPaletteId

internal object QobuzDesign {
    private val light: Boolean get() = LevyraActivePalette.id != QobuzPaletteId || LevyraActivePalette.isLight
    private fun tone(day: Long, night: Long): Color = Color(if (light) day else night)

    val surfacePrimary: Color get() = tone(0xFFFFFFFF, 0xFF121212)
    val surfaceSecondary: Color get() = tone(0xFFF3F3F3, 0xFF242424)
    val surfaceTertiary: Color get() = tone(0xFFEDEDED, 0xFF323232)
    val surfaceQuaternary: Color get() = tone(0xFFD7D7D7, 0xFF424242)
    val surfaceInvert: Color get() = tone(0xFF000000, 0xFFFFFFFF)
    val surfaceAccent: Color get() = tone(0xFFFFF0D8, 0xFF2A2215)
    val textPrimary: Color get() = tone(0xFF242424, 0xFFFFFFFF)
    val textSecondary: Color get() = tone(0xFF767575, 0xFFBDBDBD)
    val textTertiary: Color get() = tone(0xFF6F6F6F, 0xFFBDBDBD)
    val textAccent: Color get() = tone(0xFF9E6301, 0xFFFFCB74)
    val textDisabled: Color get() = tone(0xFFBDBDBD, 0xFF424242)
    val textInvert: Color get() = tone(0xFFFFFFFF, 0xFF242424)
    val iconPrimary: Color get() = textPrimary
    val iconSecondary: Color get() = textSecondary
    val separator: Color get() = tone(0xFFEDEDED, 0xFF242424)
    val separatorStrong: Color get() = tone(0xFFD7D7D7, 0xFF424242)
    val border: Color get() = tone(0xFFEDEDED, 0xFF323232)
    val placeholder: Color get() = tone(0xFFD7D7D7, 0xFF323232)
    val placeholderIcon: Color get() = tone(0xFF9E9E9E, 0xFF6F6F6F)
    val miniPlayer: Color get() = tone(0xFFEDEDED, 0xFF242424)
    val navBar: Color get() = tone(0xF0FFFFFF, 0xF0121212)
    val buttonPrimary: Color get() = tone(0xFF000000, 0xFFFFFFFF)
    val buttonPrimaryContent: Color get() = tone(0xFFFFFFFF, 0xFF242424)
    val buttonSecondary: Color get() = surfaceSecondary
    val buttonSecondaryContent: Color get() = textPrimary
    val award: Color = Color(0xFF7B6641)
    val error: Color get() = tone(0xFFD1404D, 0xFFCD5D59)
    val overlayCardCover: Color = Color(0xB2000000)
    val playerContent: Color = Color.White
    val playerContentSecondary: Color = Color(0xB2FFFFFF)
    val playerContentTertiary: Color = Color(0x80FFFFFF)
    val playerFill: Color = Color(0x1AFFFFFF)
    val playerFillStrong: Color = Color(0x33FFFFFF)
    val hiResGold: Color = Color(0xFFFFCB74)
    val aotwBadge: Color = Color(0xFFE2541B)

    val gutter: Dp = 16.dp
    val contentPadding: Dp = 8.dp
    val sectionGap: Dp = 40.dp
    val headerToContent: Dp = 12.dp
    val itemGap: Dp = 16.dp
    val gridCard: Dp = 156.dp
    val wideCard: Dp = 266.dp
    val extraWideCard: Dp = 294.dp
    val tagCardWidth: Dp = 171.dp
    val tagCardHeight: Dp = 88.dp
    val cardCorner: Dp = 6.dp
    val tagCorner: Dp = 8.dp
    val welcomeCorner: Dp = 12.dp
    val welcomeArtwork: Dp = 135.dp
    val welcomeAvatar: Dp = 40.dp
    val chipHeight: Dp = 40.dp
    val chipGap: Dp = 6.dp
    val chipHorizontal: Dp = 18.dp
    val seeAllHeight: Dp = 30.dp
    val rankNumberWidth: Dp = 48.dp
    val rankRow: Dp = 64.dp
    val rankArtwork: Dp = 56.dp
    val listRow: Dp = 72.dp
    val listArtwork: Dp = 56.dp
    val artworkCorner: Dp = 4.dp
    val miniHeight: Dp = 64.dp
    val miniArtwork: Dp = 48.dp
    val miniCorner: Dp = 10.dp
    val miniMargin: Dp = 6.dp
    val miniProgress: Dp = 2.dp
    val navHeight: Dp = 64.dp
    val navIcon: Dp = 28.dp
    val topBarHeight: Dp = 64.dp
    val artistCircle: Dp = 170.dp
    val playerArtworkCorner: Dp = 8.dp
    val playerPill: Dp = 40.dp
    val playerSeek: Dp = 4.dp
    val playerPlay: Dp = 64.dp
    val playerSkip: Dp = 40.dp

    val chipShape = RoundedCornerShape(50)
    val cardShape = RoundedCornerShape(cardCorner)
    val artworkShape = RoundedCornerShape(artworkCorner)
    val buttonShape = RoundedCornerShape(6.dp)
}

private val QobuzShapeScale = Shapes(
    extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

internal val QobuzShapes: Shapes get() = QobuzShapeScale

private fun TextStyle.qobuz(size: TextUnit, weight: FontWeight, lineHeight: TextUnit, tracking: TextUnit = 0.sp): TextStyle =
    copy(fontSize = size, fontWeight = weight, lineHeight = lineHeight, letterSpacing = tracking)

internal val QobuzTypography: Typography = typographyForFamily(QobuzFont).let { base ->
    base.copy(
        displayLarge = base.displayLarge.qobuz(56.sp, FontWeight.Bold, 70.sp),
        displayMedium = base.displayMedium.qobuz(48.sp, FontWeight.Bold, 62.sp),
        displaySmall = base.displaySmall.qobuz(32.sp, FontWeight.Medium, 42.sp),
        headlineLarge = base.headlineLarge.qobuz(32.sp, FontWeight.Medium, 42.sp),
        headlineMedium = base.headlineMedium.qobuz(28.sp, FontWeight.Bold, 32.sp),
        headlineSmall = base.headlineSmall.qobuz(22.sp, FontWeight.Bold, 28.sp),
        titleLarge = base.titleLarge.qobuz(20.sp, FontWeight.SemiBold, 26.sp),
        titleMedium = base.titleMedium.qobuz(18.sp, FontWeight.SemiBold, 24.sp),
        titleSmall = base.titleSmall.qobuz(16.sp, FontWeight.SemiBold, 22.sp),
        bodyLarge = base.bodyLarge.qobuz(16.sp, FontWeight.Normal, 24.sp),
        bodyMedium = base.bodyMedium.qobuz(14.sp, FontWeight.Normal, 22.sp),
        bodySmall = base.bodySmall.qobuz(12.sp, FontWeight.Normal, 16.sp),
        labelLarge = base.labelLarge.qobuz(14.sp, FontWeight.SemiBold, 20.sp),
        labelMedium = base.labelMedium.qobuz(12.sp, FontWeight.Medium, 16.sp, 0.48.sp),
        labelSmall = base.labelSmall.qobuz(10.sp, FontWeight.Bold, 14.sp, 0.4.sp)
    )
}

internal fun qobuzPalette(systemDark: Boolean = false): LevyraPalette {
    val light = !systemDark
    fun tone(day: Long, night: Long) = Color(if (light) day else night)
    val accent = tone(0xFF9E6301, 0xFFFFCB74)
    return LevyraThemes.cosmic.copy(
        id = QobuzPaletteId, label = "Qobuz Style", emoji = "♫", isLight = light,
        followsCover = false, followsMood = false,
        black = tone(0xFFFFFFFF, 0xFF121212),
        ink = tone(0xFFFFFFFF, 0xFF181818),
        panel = tone(0xFFF3F3F3, 0xFF242424),
        panelSoft = tone(0xFFEDEDED, 0xFF323232),
        cyan = accent,
        blue = accent,
        violet = accent,
        pink = accent,
        orange = accent,
        text = tone(0xFF242424, 0xFFFFFFFF),
        muted = tone(0xFF767575, 0xFFBDBDBD),
        outline = tone(0xFFD7D7D7, 0xFF424242)
    )
}

internal fun qobuzColorScheme(palette: LevyraPalette): ColorScheme {
    val accent = palette.cyan
    val label = palette.text
    val secondary = palette.muted
    val grouped = palette.panel
    val raised = palette.panelSoft
    return if (palette.isLight) lightColorScheme(
        primary = Color(0xFF000000), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFF0D8), onPrimaryContainer = Color(0xFF2A2215),
        secondary = secondary, onSecondary = Color.White,
        secondaryContainer = grouped, onSecondaryContainer = label,
        tertiary = accent, onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFF0D8), onTertiaryContainer = accent,
        background = palette.black, onBackground = label,
        surface = palette.black, onSurface = label,
        surfaceVariant = grouped, onSurfaceVariant = secondary,
        surfaceTint = Color.Transparent,
        inverseSurface = Color(0xFF242424), inverseOnSurface = Color.White, inversePrimary = Color(0xFFFFCB74),
        error = Color(0xFFD1404D), onError = Color.White,
        outline = palette.outline, outlineVariant = Color(0xFFEDEDED),
        surfaceDim = grouped, surfaceBright = palette.black,
        surfaceContainerLowest = palette.black, surfaceContainerLow = Color(0xFFF9F9F9),
        surfaceContainer = grouped, surfaceContainerHigh = raised,
        surfaceContainerHighest = Color(0xFFD7D7D7)
    ) else darkColorScheme(
        primary = Color.White, onPrimary = Color(0xFF242424),
        primaryContainer = Color(0xFF2A2215), onPrimaryContainer = Color(0xFFFFCB74),
        secondary = secondary, onSecondary = Color.Black,
        secondaryContainer = grouped, onSecondaryContainer = label,
        tertiary = accent, onTertiary = Color(0xFF2A2215),
        tertiaryContainer = Color(0xFF2A2215), onTertiaryContainer = accent,
        background = palette.black, onBackground = label,
        surface = palette.black, onSurface = label,
        surfaceVariant = grouped, onSurfaceVariant = secondary,
        surfaceTint = Color.Transparent,
        inverseSurface = Color(0xFFF3F3F3), inverseOnSurface = Color(0xFF242424), inversePrimary = Color(0xFF9E6301),
        error = Color(0xFFCD5D59), onError = Color.Black,
        outline = palette.outline, outlineVariant = Color(0xFF242424),
        surfaceDim = palette.black, surfaceBright = raised,
        surfaceContainerLowest = palette.black, surfaceContainerLow = Color(0xFF181818),
        surfaceContainer = grouped, surfaceContainerHigh = raised,
        surfaceContainerHighest = Color(0xFF424242)
    )
}

@Composable
internal fun QobuzThemeEffect() {
    val systemDark = isSystemInDarkTheme()
    val palette = remember(systemDark) { qobuzPalette(systemDark) }
    SideEffect { LevyraThemeController.applyPrism(palette) }
}
