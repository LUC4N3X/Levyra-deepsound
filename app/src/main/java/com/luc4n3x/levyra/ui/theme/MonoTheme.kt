package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val MonoPaletteId = "levyra_mono"
val LevyraIsMono: Boolean get() = LevyraActivePalette.id == MonoPaletteId

internal object MonoDesign {
    val void = Color(0xFF080808)
    val base = Color(0xFF0E0E10)
    val surface = Color(0xFF17171A)
    val raised = Color(0xFF202024)
    val raisedHigh = Color(0xFF2A2A2F)
    val hairline = Color(0x14FFFFFF)
    val hairlineStrong = Color(0x29FFFFFF)
    val text = Color(0xFFF1F4F6)
    val textSecondary = Color(0xFFA3A3AB)
    val textFaint = Color(0xFF66666E)
    val accent = Color(0xFF8ED6F4)
    val accentDim = Color(0xFF4F8EA8)
    val accentGlow = Color(0x338ED6F4)
    val onAccent = Color(0xFF052231)
    val error = Color(0xFFF08A7E)

    val moodFocus = Color(0xFF3E6A86)
    val moodEnergy = Color(0xFF9A6650)
    val moodNight = Color(0xFF5A557E)
    val moodWellness = Color(0xFF5E7D58)
    val moodLove = Color(0xFF9A5A64)
    val moodTravel = Color(0xFF4C6F94)
    val moodTints = listOf(moodFocus, moodEnergy, moodNight, moodWellness, moodLove, moodTravel)
    val genreBars = listOf(Color(0xFF8ED6F4), Color(0xFFE8A0A8), Color(0xFFA8C8A0), Color(0xFFB8A6E8), Color(0xFFE8C48E), Color(0xFF8EE8D0))

    val gutter: Dp = 20.dp
    val sectionGap: Dp = 32.dp
    val headerToContent: Dp = 12.dp
    val itemGap: Dp = 12.dp
    val rowHeight: Dp = 60.dp
    val rowArtwork: Dp = 48.dp
    val card: Dp = 112.dp
    val cardWide: Dp = 156.dp
    val artistCircle: Dp = 104.dp
    val artCorner: Dp = 6.dp
    val cardCorner: Dp = 14.dp
    val panelCorner: Dp = 18.dp
    val pillCorner: Dp = 50.dp
    val miniHeight: Dp = 64.dp
    val miniArtwork: Dp = 44.dp
    val miniMargin: Dp = 12.dp
    val navHeight: Dp = 64.dp
    val playerSeek: Dp = 3.dp
    val playerPlay: Dp = 76.dp

    val artShape = RoundedCornerShape(artCorner)
    val cardShape = RoundedCornerShape(cardCorner)
    val panelShape = RoundedCornerShape(panelCorner)
    val pillShape = RoundedCornerShape(pillCorner)
}

private val MonoShapeScale = Shapes(
    extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

internal val MonoShapes: Shapes get() = MonoShapeScale

private fun TextStyle.mono(size: TextUnit, weight: FontWeight, lineHeight: TextUnit, tracking: TextUnit = 0.sp, family: FontFamily? = null): TextStyle =
    copy(fontSize = size, fontWeight = weight, lineHeight = lineHeight, letterSpacing = tracking, fontFamily = family ?: fontFamily)

internal val MonoTypography: Typography = typographyForFamily(MonoFont).let { base ->
    base.copy(
        displayLarge = base.displayLarge.mono(56.sp, FontWeight.Bold, 60.sp, (-2.4).sp),
        displayMedium = base.displayMedium.mono(44.sp, FontWeight.Bold, 48.sp, (-1.8).sp),
        displaySmall = base.displaySmall.mono(34.sp, FontWeight.Bold, 40.sp, (-1.2).sp),
        headlineLarge = base.headlineLarge.mono(30.sp, FontWeight.Bold, 36.sp, (-0.9).sp),
        headlineMedium = base.headlineMedium.mono(26.sp, FontWeight.Bold, 32.sp, (-0.6).sp),
        headlineSmall = base.headlineSmall.mono(22.sp, FontWeight.SemiBold, 28.sp, (-0.4).sp),
        titleLarge = base.titleLarge.mono(19.sp, FontWeight.SemiBold, 24.sp, (-0.2).sp),
        titleMedium = base.titleMedium.mono(15.sp, FontWeight.SemiBold, 20.sp),
        titleSmall = base.titleSmall.mono(14.sp, FontWeight.Medium, 19.sp),
        bodyLarge = base.bodyLarge.mono(15.sp, FontWeight.Normal, 22.sp),
        bodyMedium = base.bodyMedium.mono(13.sp, FontWeight.Normal, 18.sp),
        bodySmall = base.bodySmall.mono(12.sp, FontWeight.Normal, 16.sp),
        labelLarge = base.labelLarge.mono(13.sp, FontWeight.Medium, 18.sp, 0.1.sp),
        labelMedium = base.labelMedium.mono(11.sp, FontWeight.Medium, 14.sp, 2.4.sp, MonoCodeFont),
        labelSmall = base.labelSmall.mono(10.sp, FontWeight.Medium, 13.sp, 1.6.sp, MonoCodeFont)
    )
}

internal fun monoPalette(): LevyraPalette = LevyraThemes.cosmic.copy(
    id = MonoPaletteId, label = "Glacier", emoji = "●", isLight = false,
    followsCover = false, followsMood = false,
    black = MonoDesign.base,
    ink = MonoDesign.void,
    panel = MonoDesign.surface,
    panelSoft = MonoDesign.raised,
    cyan = MonoDesign.accent,
    blue = MonoDesign.accent,
    violet = MonoDesign.accent,
    pink = MonoDesign.accent,
    orange = MonoDesign.accent,
    text = MonoDesign.text,
    muted = MonoDesign.textSecondary,
    outline = MonoDesign.hairlineStrong
)

internal fun monoColorScheme(palette: LevyraPalette): ColorScheme = darkColorScheme(
    primary = MonoDesign.accent, onPrimary = MonoDesign.onAccent,
    primaryContainer = MonoDesign.raised, onPrimaryContainer = MonoDesign.accent,
    secondary = MonoDesign.textSecondary, onSecondary = MonoDesign.base,
    secondaryContainer = MonoDesign.raised, onSecondaryContainer = MonoDesign.text,
    tertiary = MonoDesign.accent, onTertiary = MonoDesign.onAccent,
    tertiaryContainer = MonoDesign.raised, onTertiaryContainer = MonoDesign.accent,
    background = palette.black, onBackground = palette.text,
    surface = palette.black, onSurface = palette.text,
    surfaceVariant = MonoDesign.surface, onSurfaceVariant = palette.muted,
    surfaceTint = Color.Transparent,
    inverseSurface = MonoDesign.text, inverseOnSurface = MonoDesign.base, inversePrimary = MonoDesign.onAccent,
    error = MonoDesign.error, onError = MonoDesign.base,
    outline = palette.outline, outlineVariant = MonoDesign.hairline,
    surfaceDim = MonoDesign.void, surfaceBright = MonoDesign.raisedHigh,
    surfaceContainerLowest = MonoDesign.void, surfaceContainerLow = MonoDesign.base,
    surfaceContainer = MonoDesign.surface, surfaceContainerHigh = MonoDesign.raised,
    surfaceContainerHighest = MonoDesign.raisedHigh
)

@Composable
internal fun MonoThemeEffect() {
    val palette = remember { monoPalette() }
    SideEffect { LevyraThemeController.applyPrism(palette) }
}
