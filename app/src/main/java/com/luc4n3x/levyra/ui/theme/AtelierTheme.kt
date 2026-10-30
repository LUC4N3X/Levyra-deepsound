package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import com.luc4n3x.levyra.ui.preferredPlayerArtworkUrl
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.asImageBitmap

internal const val AtelierPaletteId = "levyra_atelier"
val LevyraIsAtelier: Boolean get() = LevyraActivePalette.id == AtelierPaletteId

internal object AtelierDesign {
    val room = Color(0xFF070708)
    val cabinet = Color(0xFF0E0E10)
    val panel = Color(0xFF151518)
    val panelRaised = Color(0xFF1F1F23)
    val hairline = Color(0x1AFFFFFF)
    val hairlineStrong = Color(0x33FFFFFF)
    val text = Color(0xFFF2F2F5)
    val textSecondary = Color(0xFFA3A2AD)
    val textFaint = Color(0xFF6D6C78)
    val accent: Color get() = atelierAccent.value
    val accentBright: Color get() = atelierAccentBright.value
    val onAccent: Color get() = atelierOnAccent.value
    val error = Color(0xFFE08A6E)

    val gutter: Dp = 20.dp
    val sectionGap: Dp = 36.dp
    val headerToContent: Dp = 14.dp
    val itemGap: Dp = 14.dp
    val rowHeight: Dp = 64.dp
    val rowArtwork: Dp = 48.dp
    val albumCard: Dp = 148.dp
    val artistCircle: Dp = 112.dp
    val sleeveCorner: Dp = 3.dp
    val cardCorner: Dp = 4.dp
    val panelCorner: Dp = 16.dp
    val capsuleCorner: Dp = 28.dp
    val miniHeight: Dp = 60.dp
    val miniArtwork: Dp = 44.dp
    val miniMargin: Dp = 10.dp
    val navHeight: Dp = 64.dp
    val playerSeek: Dp = 2.dp
    val playerPlay: Dp = 68.dp

    val sleeveShape = RoundedCornerShape(sleeveCorner)
    val cardShape = RoundedCornerShape(cardCorner)
    val panelShape = RoundedCornerShape(panelCorner)
    val capsuleShape = RoundedCornerShape(capsuleCorner)
}

private val AtelierShapeScale = Shapes(
    extraSmall = RoundedCornerShape(3.dp), small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(10.dp), large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

internal val AtelierShapes: Shapes get() = AtelierShapeScale

private fun TextStyle.atelier(size: TextUnit, weight: FontWeight, lineHeight: TextUnit, tracking: TextUnit = 0.sp): TextStyle =
    copy(fontSize = size, fontWeight = weight, lineHeight = lineHeight, letterSpacing = tracking)

internal val AtelierTypography: Typography = typographyForFamily(AtelierFont).let { base ->
    base.copy(
        displayLarge = base.displayLarge.atelier(52.sp, FontWeight.Light, 60.sp, (-1).sp),
        displayMedium = base.displayMedium.atelier(42.sp, FontWeight.Light, 50.sp, (-0.8).sp),
        displaySmall = base.displaySmall.atelier(34.sp, FontWeight.Normal, 42.sp, (-0.5).sp),
        headlineLarge = base.headlineLarge.atelier(30.sp, FontWeight.Normal, 38.sp, (-0.4).sp),
        headlineMedium = base.headlineMedium.atelier(26.sp, FontWeight.Medium, 32.sp, (-0.3).sp),
        headlineSmall = base.headlineSmall.atelier(22.sp, FontWeight.Medium, 28.sp, (-0.2).sp),
        titleLarge = base.titleLarge.atelier(19.sp, FontWeight.Medium, 25.sp),
        titleMedium = base.titleMedium.atelier(16.sp, FontWeight.Medium, 22.sp),
        titleSmall = base.titleSmall.atelier(14.sp, FontWeight.Medium, 20.sp),
        bodyLarge = base.bodyLarge.atelier(15.sp, FontWeight.Normal, 22.sp),
        bodyMedium = base.bodyMedium.atelier(13.sp, FontWeight.Normal, 19.sp),
        bodySmall = base.bodySmall.atelier(12.sp, FontWeight.Normal, 16.sp),
        labelLarge = base.labelLarge.atelier(13.sp, FontWeight.Medium, 18.sp, 0.2.sp),
        labelMedium = base.labelMedium.atelier(11.sp, FontWeight.Medium, 15.sp, 1.6.sp),
        labelSmall = base.labelSmall.atelier(10.sp, FontWeight.Medium, 14.sp, 1.8.sp)
    )
}

internal val AtelierDefaultAccent = Color(0xFF9C94F0)
private val atelierAccent = androidx.compose.runtime.mutableStateOf(AtelierDefaultAccent)
private val atelierAccentBright = androidx.compose.runtime.mutableStateOf(Color(0xFFCBC5FF))
private val atelierOnAccent = androidx.compose.runtime.mutableStateOf(Color(0xFF0E0B24))

internal fun atelierVividAccent(seed: Color): Color {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(seed.toArgb(), hsl)
    if (hsl[1] < 0.18f) return AtelierDefaultAccent
    hsl[1] = hsl[1].coerceIn(0.55f, 0.82f)
    hsl[2] = hsl[2].coerceIn(0.58f, 0.66f)
    return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
}

private fun atelierBright(accent: Color): Color {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(accent.toArgb(), hsl)
    hsl[2] = (hsl[2] + 0.16f).coerceAtMost(0.86f)
    return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
}

internal fun atelierPalette(): LevyraPalette = LevyraThemes.cosmic.copy(
    id = AtelierPaletteId, label = "Atelier", emoji = "◉", isLight = false,
    followsCover = false, followsMood = false,
    black = AtelierDesign.room,
    ink = AtelierDesign.cabinet,
    panel = AtelierDesign.panel,
    panelSoft = AtelierDesign.panelRaised,
    cyan = AtelierDesign.accent,
    blue = AtelierDesign.accent,
    violet = AtelierDesign.accent,
    pink = AtelierDesign.accent,
    orange = AtelierDesign.accent,
    text = AtelierDesign.text,
    muted = AtelierDesign.textSecondary,
    outline = AtelierDesign.hairlineStrong
)

internal fun atelierColorScheme(palette: LevyraPalette): ColorScheme = darkColorScheme(
    primary = AtelierDesign.accent, onPrimary = AtelierDesign.onAccent,
    primaryContainer = AtelierDesign.panelRaised, onPrimaryContainer = AtelierDesign.accentBright,
    secondary = AtelierDesign.textSecondary, onSecondary = AtelierDesign.room,
    secondaryContainer = AtelierDesign.panel, onSecondaryContainer = AtelierDesign.text,
    tertiary = AtelierDesign.accentBright, onTertiary = AtelierDesign.onAccent,
    tertiaryContainer = AtelierDesign.panelRaised, onTertiaryContainer = AtelierDesign.accentBright,
    background = palette.black, onBackground = palette.text,
    surface = palette.black, onSurface = palette.text,
    surfaceVariant = AtelierDesign.panel, onSurfaceVariant = palette.muted,
    surfaceTint = Color.Transparent,
    inverseSurface = AtelierDesign.text, inverseOnSurface = AtelierDesign.room, inversePrimary = AtelierDesign.onAccent,
    error = AtelierDesign.error, onError = AtelierDesign.room,
    outline = palette.outline, outlineVariant = AtelierDesign.hairline,
    surfaceDim = palette.black, surfaceBright = AtelierDesign.panelRaised,
    surfaceContainerLowest = palette.black, surfaceContainerLow = AtelierDesign.cabinet,
    surfaceContainer = AtelierDesign.panel, surfaceContainerHigh = AtelierDesign.panelRaised,
    surfaceContainerHighest = Color(0xFF29292E)
)

@Composable
internal fun AtelierThemeEffect(track: com.luc4n3x.levyra.domain.Track?) {
    val palette = remember { atelierPalette() }
    SideEffect { LevyraThemeController.applyPrism(palette) }
    val key = remember(track?.id, track?.thumbnailUrl, track?.largeThumbnailUrl) {
        track?.let { com.luc4n3x.levyra.data.ArtworkPaletteCache.key(it.id, it.thumbnailUrl, it.largeThumbnailUrl) }.orEmpty()
    }
    val fallback = remember { com.luc4n3x.levyra.data.ArtworkPalette(AtelierDefaultAccent.toArgb(), AtelierDefaultAccent.toArgb()) }
    val cover by com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette(
        paletteKey = key,
        artworkUrl = track?.let { preferredPlayerArtworkUrl(it) }.orEmpty(),
        fallback = fallback,
        retainPreviousUntilLoaded = true
    )
    val target = remember(cover) { atelierVividAccent(Color(cover.start)) }
    val animated = com.luc4n3x.levyra.ui.LocalAnimationsEnabled.current
    val accent by androidx.compose.animation.animateColorAsState(target, androidx.compose.animation.core.tween(if (animated) 600 else 0), label = "atelier-accent")
    SideEffect {
        atelierAccent.value = accent
        atelierAccentBright.value = atelierBright(accent)
        atelierOnAccent.value = if (androidx.core.graphics.ColorUtils.calculateLuminance(accent.toArgb()) > 0.45) Color(0xFF0B0B0D) else Color.White
    }
}

internal fun playerStageSpec(current: Float, opening: Boolean): androidx.compose.animation.core.AnimationSpec<Float> {
    if (!LevyraIsAtelier) return if (opening) LevyraPlayerDesign.expandSpring() else LevyraPlayerDesign.collapseSpring()
    val remaining = if (opening) 1f - current else current
    val duration = (if (opening) AtelierOpenMillis else AtelierCloseMillis) * remaining.coerceIn(0f, 1f)
    return androidx.compose.animation.core.tween(duration.toInt().coerceAtLeast(90), easing = androidx.compose.animation.core.LinearEasing)
}

private const val AtelierOpenMillis = 1250f
private const val AtelierCloseMillis = 1050f

private val AtelierCarbonWeave: androidx.compose.ui.graphics.ImageBitmap by lazy {
    val cell = 6
    val bitmap = android.graphics.Bitmap.createBitmap(cell * 2, cell * 2, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    canvas.drawColor(0xFF09090B.toInt())
    for (row in 0 until 2) for (col in 0 until 2) {
        val horizontal = (row + col) % 2 == 0
        val left = (col * cell).toFloat()
        val top = (row * cell).toFloat()
        val shade = android.graphics.LinearGradient(
            left, top, if (horizontal) left else left + cell, if (horizontal) top + cell else top,
            intArrayOf(0xFF17171B.toInt(), 0xFF0C0C0E.toInt(), 0xFF141417.toInt()), null, android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = shade
        canvas.drawRect(left + 0.5f, top + 0.5f, left + cell - 0.5f, top + cell - 0.5f, paint)
    }
    bitmap.asImageBitmap()
}

internal fun androidx.compose.ui.Modifier.atelierCarbon(): androidx.compose.ui.Modifier = drawWithCache {
    val brush = androidx.compose.ui.graphics.ShaderBrush(
        androidx.compose.ui.graphics.ImageShader(AtelierCarbonWeave, androidx.compose.ui.graphics.TileMode.Repeated, androidx.compose.ui.graphics.TileMode.Repeated)
    )
    val sheen = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent, Color.Black.copy(alpha = 0.25f)))
    onDrawBehind {
        drawRect(brush)
        drawRect(sheen)
    }
}
