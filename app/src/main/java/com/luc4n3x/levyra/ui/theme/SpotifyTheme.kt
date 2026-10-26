package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.hct.Hct

internal const val SpotifyPaletteId = "levyra_spotify_style"
val LevyraIsSpotify: Boolean get() = LevyraActivePalette.id == SpotifyPaletteId

internal object SpotifyDesign {
    val base = Color(0xFF121212)
    val elevated = Color(0xFF1F1F1F)
    val highlight = Color(0xFF2A2A2A)
    val card = Color(0xFF181818)
    val tinted = Color(0x1AFFFFFF)
    val tintedHighlight = Color(0x24FFFFFF)
    val decorative = Color(0xFF292929)
    val text = Color(0xFFFFFFFF)
    val subdued = Color(0xFFB3B3B3)
    val faint = Color(0xFF7C7C7C)
    val green = Color(0xFF1ED760)
    val greenPress = Color(0xFF1ABC54)
    val greenText = Color(0xFF1ED760)
    val negative = Color(0xFFED2C3F)
    val negativeStrong = Color(0xFFD71E33)
    val sheet = Color(0xFF282828)
    val placeholder = Color(0xFF282828)
    val searchField = Color(0xFFFFFFFF)
    val searchFieldText = Color(0xFF121212)
    val chip = Color(0xFF333333)
    val navScrim = Color(0xD9000000)

    val gutter = 16.dp
    val cornerSmall = 4.dp
    val corner = 6.dp
    val cornerLarge = 8.dp
    val cardSize = 176.dp
    val compactCard = 112.dp
    val radioCard = 176.dp
    val shortcutHeight = 56.dp
    val chipHeight = 32.dp
    val filterRowHeight = 48.dp
    val rowCover = 48.dp
    val libraryCover = 64.dp
    val playButton = 56.dp
    val heroArtwork = 248.dp
    val miniHeight = 56.dp
    val navHeight = 64.dp
    val dockGap = 12.dp

    val coverShape: Shape = RoundedCornerShape(cornerSmall)
    val tileShape: Shape = RoundedCornerShape(corner)
    val chipShape: Shape = CircleShape

    fun wash(seed: Color, amount: Float = 0.42f): Color = lerp(base, seed.copy(alpha = 1f), amount.coerceIn(0f, 1f))

    fun miniSurface(seed: Color): Color = toned(seed, 40.0, 24.0)

    fun heroSurface(seed: Color): Color = toned(seed, 48.0, 36.0)

    fun lyricsSurface(seed: Color): Color = toned(seed, 64.0, 48.0)

    private fun toned(seed: Color, maxChroma: Double, tone: Double): Color {
        val source = Hct.fromInt(seed.toArgb())
        return Color(Hct.from(source.hue, source.chroma.coerceAtMost(maxChroma), tone).toInt())
    }
}

private val SpotifyShapeScale = Shapes(
    extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

internal val SpotifyShapes: Shapes get() = SpotifyShapeScale

private fun TextStyle.encore(size: TextUnit, weight: FontWeight, lineHeight: TextUnit, tracking: TextUnit = 0.sp): TextStyle =
    copy(fontSize = size, fontWeight = weight, lineHeight = lineHeight, letterSpacing = tracking)

internal val SpotifyTypography: Typography = typographyForFamily(SpotifyFont).let { base ->
    base.copy(
        displayLarge = base.displayLarge.encore(48.sp, FontWeight.Black, 52.sp, (-1.2).sp),
        displayMedium = base.displayMedium.encore(40.sp, FontWeight.Black, 44.sp, (-1.0).sp),
        displaySmall = base.displaySmall.encore(32.sp, FontWeight.Bold, 36.sp, (-0.8).sp),
        headlineLarge = base.headlineLarge.encore(28.sp, FontWeight.Bold, 32.sp, (-0.6).sp),
        headlineMedium = base.headlineMedium.encore(24.sp, FontWeight.Bold, 28.sp, (-0.4).sp),
        headlineSmall = base.headlineSmall.encore(20.sp, FontWeight.Bold, 24.sp, (-0.3).sp),
        titleLarge = base.titleLarge.encore(20.sp, FontWeight.Bold, 24.sp, (-0.2).sp),
        titleMedium = base.titleMedium.encore(16.sp, FontWeight.Bold, 20.sp),
        titleSmall = base.titleSmall.encore(14.sp, FontWeight.Bold, 18.sp),
        bodyLarge = base.bodyLarge.encore(16.sp, FontWeight.Normal, 20.sp),
        bodyMedium = base.bodyMedium.encore(14.sp, FontWeight.Normal, 18.sp),
        bodySmall = base.bodySmall.encore(13.sp, FontWeight.Normal, 16.sp),
        labelLarge = base.labelLarge.encore(14.sp, FontWeight.Bold, 18.sp),
        labelMedium = base.labelMedium.encore(13.sp, FontWeight.Medium, 16.sp),
        labelSmall = base.labelSmall.encore(11.sp, FontWeight.Medium, 14.sp, 0.1.sp)
    )
}

internal fun spotifyPalette(): LevyraPalette = LevyraThemes.cosmic.copy(
    id = SpotifyPaletteId, label = "Spotify Style", emoji = "◉", isLight = false,
    followsCover = false, followsMood = false,
    black = SpotifyDesign.base,
    ink = SpotifyDesign.card,
    panel = SpotifyDesign.elevated,
    panelSoft = SpotifyDesign.highlight,
    cyan = SpotifyDesign.green,
    blue = SpotifyDesign.green,
    violet = SpotifyDesign.greenPress,
    pink = SpotifyDesign.green,
    orange = SpotifyDesign.green,
    text = SpotifyDesign.text,
    muted = SpotifyDesign.subdued,
    outline = SpotifyDesign.decorative
)
