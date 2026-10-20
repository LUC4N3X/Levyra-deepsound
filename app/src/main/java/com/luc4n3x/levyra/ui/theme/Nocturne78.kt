package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraFontPreset
import com.luc4n3x.levyra.domain.*

internal val Nocturne78Palette = LevyraThemes.cosmic.copy(
    id = "nocturne_78",
    label = "Nocturne 78",
    emoji = "◉",
    black = Color(0xFF08090B),
    ink = Color.Black,
    panel = Color(0xFF111316),
    panelSoft = Color(0xFF1B1D20),
    cyan = Color(0xFFD49A46),
    blue = Color(0xFFD49A46),
    violet = Color(0xFFE7B96D),
    pink = Color(0xFFD49A46),
    orange = Color(0xFFE7B96D),
    text = Color(0xFFF2EEE7),
    muted = Color(0xFFABA59B),
    outline = Color(0xFF393632)
)

val LevyraIsNocturne: Boolean get() = LevyraActivePalette.id == Nocturne78Palette.id

internal val Nocturne78Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp)
)

internal val Nocturne78Typography = levyraTypographyFor(LevyraFontPreset.System).let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
        labelSmall = base.labelSmall.copy(letterSpacing = 1.2.sp)
    )
}

internal object Nocturne78Design {
    val artworkCorner = 6.dp
    val cardCorner = 10.dp
    val panelCorner = 14.dp
    val controlCorner = 8.dp
    val variant = Color(0xFF252629)
}

internal fun nocturneMiniSettings(saved: LevyraMiniPlayerStudioSettings): LevyraMiniPlayerStudioSettings {
    val tuning = LevyraMiniPlayerStudioSettings.spec(MiniPlayerStyle.Floating).defaults.copy(
        cornerDp = 10, heightDp = 64, artworkDp = 44, shadow = 0, glow = 0,
        artworkShape = MiniArtworkShape.Square, progress = MiniProgressStyle.Line,
        buttons = MiniButtonStyle.Outlined, colorMode = MiniColorMode.Theme,
        background = MiniBackground.Solid, motion = MiniMotion.Normal,
        controls = saved.activeTuning().controls, showClose = saved.activeTuning().showClose
    )
    return saved.copy(style = MiniPlayerStyle.Floating, tunings = saved.tunings + (MiniPlayerStyle.Floating to tuning))
}

internal fun nocturneNavSettings(saved: LevyraNavigationStudioSettings): LevyraNavigationStudioSettings {
    val tuning = saved.activeTuning().copy(
        cornerDp = 0, heightDp = 64, indicator = NavIndicator.None,
        background = NavBackground.Solid, colorSource = NavColorSource.Theme,
        surfaceTone = NavSurfaceTone.Auto, motion = NavMotion.Minimal,
        opacity = 100, inactiveAlpha = 100, labels = NavLabelMode.Always
    )
    return saved.copy(style = NavStyle.Minimal, tunings = saved.tunings + (NavStyle.Minimal to tuning))
}
