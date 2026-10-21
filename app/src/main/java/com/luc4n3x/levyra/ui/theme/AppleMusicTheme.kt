package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.hct.Hct

internal const val AppleMusicPaletteId = "levyra_apple_music_style"
val LevyraIsAppleMusic: Boolean get() = LevyraActivePalette.id == AppleMusicPaletteId

internal object AppleMusicDesign {
    private val light: Boolean get() = LevyraActivePalette.isLight

    val accent: Color get() = if (light) Color(0xFFFA233B) else Color(0xFFFC3C44)
    val background: Color get() = if (light) Color.White else Color.Black
    val groupedBackground: Color get() = if (light) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)
    val raised: Color get() = if (light) Color(0xFFF2F2F7) else Color(0xFF2C2C2E)
    val label: Color get() = if (light) Color.Black else Color.White
    val secondaryLabel: Color get() = if (light) Color(0x993C3C43) else Color(0x99EBEBF5)
    val tertiaryLabel: Color get() = if (light) Color(0x4D3C3C43) else Color(0x4DEBEBF5)
    val separator: Color get() = if (light) Color(0x4A3C3C43) else Color(0xFF38383A)
    val opaqueSeparator: Color get() = if (light) Color(0xFFC6C6C8) else Color(0xFF38383A)
    val fill: Color get() = if (light) Color(0x1F767680) else Color(0x3D767680)
    val secondaryFill: Color get() = if (light) Color(0x29787880) else Color(0x52787880)
    val chrome: Color get() = if (light) Color(0xF2F5F5F7) else Color(0xF2262628)
    val chromeStroke: Color get() = if (light) Color(0x1A000000) else Color(0x33FFFFFF)
    val artworkStroke: Color get() = if (light) Color(0x1A000000) else Color(0x1AFFFFFF)
    val placeholder: Color get() = if (light) Color(0xFFE5E5EA) else Color(0xFF2C2C2E)

    val gutter = 20.dp
    val itemGap = 12.dp
    val sectionGap = 28.dp
    val headerHeight = 44.dp
    val heroAspect = 1.5f
    val heroPeek = 36.dp
    val songRow = 56.dp
    val songArtwork = 48.dp
    val listRow = 48.dp
    val navHeight = 62.dp
    val navIcon = 30.dp
    val miniHeight = 48.dp
    val categoryTileAspect = 1.777f
    val pillHeight = 44.dp
    val collectionArtworkInset = 72.dp
    val artistHeroAspect = 1.562f
    val playerArtworkInset = 24.dp
    val playerMetadataInset = 32.dp
    val playerSeekbarInset = 32.dp
    val playerTransportGap = 40.dp
    val playerSkipIcon = 40.dp
    val playerPlayIcon = 38.dp
    val playerControlPadding = 11.dp
    val playerActionIcon = 28.dp
    val playerActionPadding = 16.dp
    val playerActionBottom = 14.dp

    val cornerS = 5.dp
    val cornerM = 7.dp
    val cornerL = 10.dp
    val cornerXl = 14.dp

    val artworkShape: Shape = RoundedCornerShape(cornerL)
    val thumbShape: Shape = RoundedCornerShape(cornerS)
    val pillShape: Shape = CircleShape

    fun cardWidth(viewportWidth: Dp): Dp = ((viewportWidth - gutter - itemGap * 2 - heroPeek) / 2).coerceIn(140.dp, 220.dp)

    fun heroWidth(viewportWidth: Dp): Dp = (viewportWidth - gutter - itemGap - heroPeek).coerceIn(280.dp, 520.dp)

    fun songColumnWidth(viewportWidth: Dp): Dp = (viewportWidth - gutter - itemGap - heroPeek).coerceIn(280.dp, 420.dp)

    fun playerField(seed: Color): Color = toned(seed, 36.0, 34.0)

    fun playerFieldDeep(seed: Color): Color = toned(seed, 30.0, 18.0)

    private fun toned(seed: Color, maxChroma: Double, tone: Double): Color {
        val source = Hct.fromInt(seed.toArgb())
        return Color(Hct.from(source.hue, source.chroma.coerceAtMost(maxChroma), tone).toInt())
    }
}

internal object AppleMusicVibrant {
    val primary = Color.White.copy(alpha = 0.94f)
    val secondary = Color.White.copy(alpha = 0.45f)
    val tertiary = Color.White.copy(alpha = 0.18f)
    val quaternary = Color.White.copy(alpha = 0.08f)
}

private val AppleMusicShapeScale = Shapes(
    extraSmall = RoundedCornerShape(5.dp), small = RoundedCornerShape(7.dp),
    medium = RoundedCornerShape(10.dp), large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

internal val AppleMusicShapes: Shapes get() = AppleMusicShapeScale

private fun TextStyle.sf(size: TextUnit, weight: FontWeight, lineHeight: TextUnit, tracking: TextUnit = 0.sp): TextStyle =
    copy(fontSize = size, fontWeight = weight, lineHeight = lineHeight, letterSpacing = tracking)

internal val AppleMusicTypography: Typography = typographyForFamily(AppleMusicFont).let { base ->
    base.copy(
        displayLarge = base.displayLarge.sf(34.sp, FontWeight.Bold, 41.sp, (-0.6).sp),
        displayMedium = base.displayMedium.sf(34.sp, FontWeight.Bold, 41.sp, (-0.6).sp),
        displaySmall = base.displaySmall.sf(28.sp, FontWeight.Bold, 34.sp, (-0.5).sp),
        headlineLarge = base.headlineLarge.sf(34.sp, FontWeight.Bold, 41.sp, (-0.6).sp),
        headlineMedium = base.headlineMedium.sf(28.sp, FontWeight.Bold, 34.sp, (-0.5).sp),
        headlineSmall = base.headlineSmall.sf(22.sp, FontWeight.Bold, 28.sp, (-0.4).sp),
        titleLarge = base.titleLarge.sf(20.sp, FontWeight.SemiBold, 25.sp, (-0.3).sp),
        titleMedium = base.titleMedium.sf(17.sp, FontWeight.SemiBold, 22.sp, (-0.3).sp),
        titleSmall = base.titleSmall.sf(15.sp, FontWeight.SemiBold, 20.sp, (-0.2).sp),
        bodyLarge = base.bodyLarge.sf(17.sp, FontWeight.Normal, 22.sp, (-0.3).sp),
        bodyMedium = base.bodyMedium.sf(15.sp, FontWeight.Normal, 20.sp, (-0.2).sp),
        bodySmall = base.bodySmall.sf(13.sp, FontWeight.Normal, 18.sp, (-0.1).sp),
        labelLarge = base.labelLarge.sf(15.sp, FontWeight.Medium, 20.sp, (-0.2).sp),
        labelMedium = base.labelMedium.sf(12.sp, FontWeight.Medium, 16.sp),
        labelSmall = base.labelSmall.sf(11.sp, FontWeight.Medium, 14.sp, 0.1.sp)
    )
}

internal fun appleMusicPalette(systemDark: Boolean = false): LevyraPalette {
    val light = !systemDark
    fun tone(day: Long, night: Long) = Color(if (light) day else night)
    val accent = tone(0xFFFA233B, 0xFFFC3C44)
    return LevyraThemes.cosmic.copy(
        id = AppleMusicPaletteId, label = "Apple Music Style", emoji = "♪", isLight = light,
        followsCover = false, followsMood = false,
        black = tone(0xFFFFFFFF, 0xFF000000),
        ink = tone(0xFFFFFFFF, 0xFF1C1C1E),
        panel = tone(0xFFF2F2F7, 0xFF1C1C1E),
        panelSoft = tone(0xFFE5E5EA, 0xFF2C2C2E),
        cyan = accent,
        blue = accent,
        violet = accent,
        pink = accent,
        orange = accent,
        text = tone(0xFF000000, 0xFFFFFFFF),
        muted = tone(0xFF8A8A8E, 0xFF8D8D93),
        outline = tone(0xFFC6C6C8, 0xFF38383A)
    )
}

internal fun appleMusicColorScheme(palette: LevyraPalette): ColorScheme {
    val light = palette.isLight
    val accent = palette.cyan
    val label = palette.text
    val secondary = if (light) Color(0xFF8A8A8E) else Color(0xFF8D8D93)
    val grouped = palette.panel
    val raised = palette.panelSoft
    return if (light) lightColorScheme(
        primary = accent, onPrimary = Color.White,
        primaryContainer = Color(0xFFFFE3E6), onPrimaryContainer = Color(0xFF5C0010),
        secondary = secondary, onSecondary = Color.White,
        secondaryContainer = grouped, onSecondaryContainer = label,
        tertiary = accent, onTertiary = Color.White,
        tertiaryContainer = grouped, onTertiaryContainer = accent,
        background = palette.black, onBackground = label,
        surface = palette.black, onSurface = label,
        surfaceVariant = grouped, onSurfaceVariant = secondary,
        surfaceTint = Color.Transparent,
        inverseSurface = Color(0xFF1C1C1E), inverseOnSurface = Color.White, inversePrimary = Color(0xFFFF6B7A),
        error = Color(0xFFFF3B30), onError = Color.White,
        outline = palette.outline, outlineVariant = Color(0xFFE5E5EA),
        surfaceDim = grouped, surfaceBright = palette.black,
        surfaceContainerLowest = palette.black, surfaceContainerLow = Color(0xFFF9F9FB),
        surfaceContainer = grouped, surfaceContainerHigh = Color(0xFFEDEDF0),
        surfaceContainerHighest = raised
    ) else darkColorScheme(
        primary = accent, onPrimary = Color.White,
        primaryContainer = Color(0xFF4A0711), onPrimaryContainer = Color(0xFFFFD9DD),
        secondary = secondary, onSecondary = Color.Black,
        secondaryContainer = grouped, onSecondaryContainer = label,
        tertiary = accent, onTertiary = Color.White,
        tertiaryContainer = grouped, onTertiaryContainer = accent,
        background = palette.black, onBackground = label,
        surface = palette.black, onSurface = label,
        surfaceVariant = grouped, onSurfaceVariant = secondary,
        surfaceTint = Color.Transparent,
        inverseSurface = Color.White, inverseOnSurface = Color.Black, inversePrimary = Color(0xFFB0001A),
        error = Color(0xFFFF453A), onError = Color.Black,
        outline = palette.outline, outlineVariant = Color(0xFF2C2C2E),
        surfaceDim = palette.black, surfaceBright = raised,
        surfaceContainerLowest = palette.black, surfaceContainerLow = Color(0xFF111113),
        surfaceContainer = grouped, surfaceContainerHigh = raised,
        surfaceContainerHighest = Color(0xFF3A3A3C)
    )
}

@Composable
internal fun AppleMusicThemeEffect() {
    val systemDark = isSystemInDarkTheme()
    val palette = remember(systemDark) { appleMusicPalette(systemDark) }
    SideEffect { LevyraThemeController.applyPrism(palette) }
}
