package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.IvoryAppearance
import com.luc4n3x.levyra.domain.IvorySettings
import com.luc4n3x.levyra.domain.LevyraFontPreset
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings

internal const val IvoryPaletteId = "levyra_ivory"
val LevyraIsIvory: Boolean get() = LevyraActivePalette.id == IvoryPaletteId
internal var activeIvorySettings by mutableStateOf(IvorySettings())
    private set

internal object IvoryDesign {
    val ink: Color get() = LevyraActivePalette.blue
    val onInk: Color get() = LevyraActivePalette.black
    val rose: Color get() = LevyraActivePalette.pink
    val well: Color get() = LevyraActivePalette.panel
    val coverShape: Shape = RoundedCornerShape(percent = 24)
    val coverCorner = 30.dp
    val tileCorner = 16.dp
    val surfaceCorner = 24.dp
    val controlCorner = 16.dp
    val tabHeight = 46.dp
}

internal fun ivoryEdgeTabShape(start: Boolean, flareFraction: Float = 0.2f): Shape = GenericShape { size, direction ->
    val f = size.height * flareFraction
    val w = size.width
    val h = size.height
    val fromLeft = start == (direction == LayoutDirection.Ltr)
    fun x(value: Float) = if (fromLeft) value else w - value
    val r = ((h - f * 2) / 2f).coerceAtLeast(0f)
    moveTo(x(0f), 0f)
    quadraticTo(x(0f), f, x(f), f)
    lineTo(x(w - r), f)
    quadraticTo(x(w), f, x(w), f + r)
    quadraticTo(x(w), h - f, x(w - r), h - f)
    lineTo(x(f), h - f)
    quadraticTo(x(0f), h - f, x(0f), h)
    close()
}

internal val IvoryNotchShape: Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0f, h)
    cubicTo(w * 0.16f, h, w * 0.18f, 0f, w * 0.34f, 0f)
    lineTo(w * 0.66f, 0f)
    cubicTo(w * 0.82f, 0f, w * 0.84f, h, w, h)
    close()
}

internal val IvoryCornerShape: Shape = RoundedCornerShape(topStartPercent = 100)

internal const val IvoryPocketInset = 0.24f
internal const val IvoryPocketRadius = 0.28f

internal val IvoryPocketShape: Shape = GenericShape { size, direction ->
    val h = size.height
    val corner = h * 0.21f
    val inset = h * IvoryPocketInset
    val cx = if (direction == LayoutDirection.Ltr) size.width - inset else inset
    val body = androidx.compose.ui.graphics.Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, h, corner, corner))
    }
    val pocket = androidx.compose.ui.graphics.Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset(cx, inset), h * IvoryPocketRadius))
    }
    op(body, pocket, androidx.compose.ui.graphics.PathOperation.Difference)
}

private val IvoryShapeScale = Shapes(
    extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

internal val IvoryShapes: Shapes get() = IvoryShapeScale

internal val IvoryTypography = levyraTypographyFor(LevyraFontPreset.Manrope).let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Light, letterSpacing = (-0.6).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.4).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.2).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Medium),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        bodyMedium = base.bodyMedium.copy(letterSpacing = 0.3.sp),
        bodySmall = base.bodySmall.copy(letterSpacing = 0.3.sp),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
    )
}

internal fun ivoryIsLight(settings: IvorySettings, systemDark: Boolean): Boolean =
    settings.appearance == IvoryAppearance.Light || settings.appearance == IvoryAppearance.Automatic && !systemDark

internal fun ivoryPalette(settings: IvorySettings = IvorySettings(), systemDark: Boolean = false): LevyraPalette {
    val light = ivoryIsLight(settings, systemDark)
    fun tone(day: Long, night: Long) = Color(if (light) day else night)
    return LevyraThemes.cosmic.copy(
        id = IvoryPaletteId, label = "Ivory", emoji = "◖", isLight = light,
        followsCover = false, followsMood = false,
        black = tone(0xFFEDEFF4, 0xFF130E18),
        ink = tone(0xFFF8F9FB, 0xFF1A1420),
        panel = tone(0xFFE2E5ED, 0xFF211A28),
        panelSoft = tone(0xFFD7DBE5, 0xFF2B2333),
        cyan = tone(0xFF1B1022, 0xFFEEEBF4),
        blue = tone(0xFF1B1022, 0xFFEEEBF4),
        violet = tone(0xFF3E3049, 0xFFC8C0D3),
        pink = tone(0xFFE5245C, 0xFFFF5287),
        orange = tone(0xFFE5245C, 0xFFFF5287),
        text = tone(0xFF1B1022, 0xFFF2F0F6),
        muted = tone(0xFF67616F, 0xFFA69FB0),
        outline = tone(0xFFD2D6DF, 0xFF352D3D)
    )
}

@Composable
internal fun IvoryThemeEffect(settings: LevyraInterfaceSettings) {
    val ivory = settings.ivory
    val systemDark = isSystemInDarkTheme()
    val palette = remember(ivory, systemDark) { ivoryPalette(ivory, systemDark) }
    SideEffect {
        activeIvorySettings = ivory
        LevyraThemeController.applyPrism(palette)
    }
}
