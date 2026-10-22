package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.hct.Hct

internal const val YouTubeMusicPaletteId = "levyra_youtube_music_style"
val LevyraIsYouTubeMusic: Boolean get() = LevyraActivePalette.id == YouTubeMusicPaletteId

internal object YouTubeMusicDesign {
    val background = Color(0xFF000000)
    val surface = Color(0xFF212121)
    val surfaceRaised = Color(0xFF282828)
    val surfaceHigh = Color(0xFF2E2E2E)
    val navBar = Color(0xFF000000)
    val miniPlayer = Color(0xFF212121)
    val divider = Color(0xFF3A3A3A)
    val text = Color(0xFFFFFFFE)
    val textSecondary = Color(0xFFAAAAAA)
    val textTranslucent = Color(0xAAFFFFFF)
    val textTertiary = Color(0xFF717171)
    val chipFill = Color(0x1AFFFFFF)
    val buttonFill = Color(0x1AFFFFFF)
    val outline = Color(0x33FFFFFF)
    val placeholder = Color(0xFF3A3A3A)
    val red = Color(0xFFFF0033)
    val premiumRed = Color(0xFFCC0000)
    val trendUp = Color(0xFF1ED760)
    val trendDown = Color(0xFFFF4E45)
    val seekTrack = Color(0x4DFFFFFF)

    val gutter = 16.dp
    val itemGap = 16.dp
    val sectionGap = 24.dp
    val chipHeight = 32.dp
    val chipGap = 8.dp
    val chipCorner = 8.dp
    val headerTitleSize = 24.sp
    val listRow = 72.dp
    val listArtwork = 56.dp
    val compactRow = 64.dp
    val compactArtwork = 48.dp
    val artworkCorner = 2.dp
    val cardArtwork = 165.dp
    val cardCorner = 4.dp
    val miniHeight = 68.dp
    val miniThumb = 48.dp
    val miniIcon = 32.dp
    val navHeight = 56.dp
    val navIcon = 24.dp
    val topBarHeight = 64.dp
    val avatar = 32.dp
    val tileHeight = 116.dp
    val tileCorner = 16.dp
    val collectionArtwork = 220.dp
    val collectionPlay = 64.dp
    val collectionAction = 48.dp
    val playerInset = 32.dp
    val playerArtworkCorner = 8.dp
    val playerPlay = 72.dp
    val playerSkip = 40.dp
    val playerSide = 24.dp
    val playerPillHeight = 36.dp
    val segmentWidth = 144.dp
    val segmentHeight = 32.dp

    val fadeInMillis = 200
    val fadeOutMillis = 100
    val decelerateQuad = androidx.compose.animation.core.Easing { fraction -> 1f - (1f - fraction) * (1f - fraction) }
    val accelerateQuad = androidx.compose.animation.core.Easing { fraction -> fraction * fraction }
    val discoveryCorner = 16.dp

    val thumbShape: Shape = RoundedCornerShape(artworkCorner)
    val cardShape: Shape = RoundedCornerShape(cardCorner)
    val chipShape: Shape = RoundedCornerShape(chipCorner)
    val tileShape: Shape = RoundedCornerShape(tileCorner)
    val pillShape: Shape = CircleShape

    fun quickPickColumnWidth(viewportWidth: Dp): Dp = (viewportWidth - gutter - 32.dp).coerceIn(280.dp, 420.dp)

    fun playerField(seed: Color): Color = toned(seed, 40.0, 20.0)

    fun playerFieldDeep(seed: Color): Color = toned(seed, 30.0, 8.0)

    fun sheetField(seed: Color): Color = toned(seed, 40.0, 26.0)

    fun activeRow(seed: Color): Color = toned(seed, 48.0, 30.0)

    private fun toned(seed: Color, maxChroma: Double, tone: Double): Color {
        val source = Hct.fromInt(seed.toArgb())
        return Color(Hct.from(source.hue, source.chroma.coerceAtMost(maxChroma), tone).toInt())
    }
}

private val YouTubeMusicShapeScale = Shapes(
    extraSmall = RoundedCornerShape(2.dp), small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

internal val YouTubeMusicShapes: Shapes get() = YouTubeMusicShapeScale

private fun TextStyle.ytm(size: TextUnit, weight: FontWeight, lineHeight: TextUnit, tracking: TextUnit = 0.sp): TextStyle =
    copy(fontSize = size, fontWeight = weight, lineHeight = lineHeight, letterSpacing = tracking)

internal val YouTubeMusicTypography: Typography = typographyForFamily(FontFamily.Default).let { base ->
    base.copy(
        displayLarge = base.displayLarge.ytm(40.sp, FontWeight.Bold, 48.sp, (-0.5).sp),
        displayMedium = base.displayMedium.ytm(34.sp, FontWeight.Bold, 40.sp, (-0.4).sp),
        displaySmall = base.displaySmall.ytm(28.sp, FontWeight.Bold, 34.sp, (-0.3).sp),
        headlineLarge = base.headlineLarge.ytm(28.sp, FontWeight.Bold, 34.sp, (-0.3).sp),
        headlineMedium = base.headlineMedium.ytm(24.sp, FontWeight.Bold, 30.sp, (-0.2).sp),
        headlineSmall = base.headlineSmall.ytm(20.sp, FontWeight.Bold, 26.sp),
        titleLarge = base.titleLarge.ytm(20.sp, FontWeight.Medium, 26.sp),
        titleMedium = base.titleMedium.ytm(16.sp, FontWeight.Medium, 22.sp),
        titleSmall = base.titleSmall.ytm(14.sp, FontWeight.Medium, 20.sp),
        bodyLarge = base.bodyLarge.ytm(16.sp, FontWeight.Normal, 22.sp),
        bodyMedium = base.bodyMedium.ytm(14.sp, FontWeight.Normal, 20.sp),
        bodySmall = base.bodySmall.ytm(12.sp, FontWeight.Normal, 16.sp),
        labelLarge = base.labelLarge.ytm(16.sp, FontWeight.Medium, 20.sp),
        labelMedium = base.labelMedium.ytm(14.sp, FontWeight.Medium, 18.sp),
        labelSmall = base.labelSmall.ytm(12.sp, FontWeight.Medium, 16.sp)
    )
}

internal fun youTubeMusicPalette(): LevyraPalette = LevyraThemes.cosmic.copy(
    id = YouTubeMusicPaletteId, label = "YouTube Music Style", emoji = "▶", isLight = false,
    followsCover = false, followsMood = false,
    black = YouTubeMusicDesign.background,
    ink = YouTubeMusicDesign.surface,
    panel = YouTubeMusicDesign.surface,
    panelSoft = YouTubeMusicDesign.surfaceRaised,
    cyan = YouTubeMusicDesign.text,
    blue = YouTubeMusicDesign.text,
    violet = YouTubeMusicDesign.text,
    pink = YouTubeMusicDesign.red,
    orange = YouTubeMusicDesign.red,
    text = YouTubeMusicDesign.text,
    muted = YouTubeMusicDesign.textSecondary,
    outline = YouTubeMusicDesign.divider
)

internal fun youTubeMusicColorScheme(palette: LevyraPalette): ColorScheme = darkColorScheme(
    primary = Color.White, onPrimary = Color.Black,
    primaryContainer = YouTubeMusicDesign.surfaceRaised, onPrimaryContainer = Color.White,
    secondary = YouTubeMusicDesign.textSecondary, onSecondary = Color.Black,
    secondaryContainer = YouTubeMusicDesign.surface, onSecondaryContainer = Color.White,
    tertiary = YouTubeMusicDesign.red, onTertiary = Color.White,
    tertiaryContainer = YouTubeMusicDesign.surface, onTertiaryContainer = Color.White,
    background = palette.black, onBackground = palette.text,
    surface = palette.black, onSurface = palette.text,
    surfaceVariant = YouTubeMusicDesign.surface, onSurfaceVariant = YouTubeMusicDesign.textSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = Color.White, inverseOnSurface = Color.Black, inversePrimary = Color.Black,
    error = YouTubeMusicDesign.trendDown, onError = Color.Black,
    outline = palette.outline, outlineVariant = YouTubeMusicDesign.surfaceRaised,
    surfaceDim = palette.black, surfaceBright = YouTubeMusicDesign.surfaceHigh,
    surfaceContainerLowest = palette.black, surfaceContainerLow = Color(0xFF0F0F0F),
    surfaceContainer = YouTubeMusicDesign.surface, surfaceContainerHigh = YouTubeMusicDesign.surfaceRaised,
    surfaceContainerHighest = YouTubeMusicDesign.surfaceHigh
)

@Composable
internal fun YouTubeMusicThemeEffect() {
    val palette = remember { youTubeMusicPalette() }
    SideEffect { LevyraThemeController.applyPrism(palette) }
}
