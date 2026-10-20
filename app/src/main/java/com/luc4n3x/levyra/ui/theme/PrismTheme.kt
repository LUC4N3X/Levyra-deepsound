package com.luc4n3x.levyra.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.domain.*
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.preferredPlayerArtworkUrl
import com.materialkolor.hct.Hct

internal const val PrismPaletteId = "levyra_prism"
val LevyraIsPrism: Boolean get() = LevyraActivePalette.id == PrismPaletteId
internal val LevyraSkinManagesLayout: Boolean get() = LevyraIsNocturne || LevyraIsPrism
internal val LevyraSkinLabel: String get() = LevyraActivePalette.label
internal var activePrismSettings by mutableStateOf(PrismSettings())
    private set

internal object PrismDesign {
    val artworkCorner = 18.dp
    val surfaceCorner = 24.dp
    val controlCorner = 18.dp
    val gutter = 20.dp
    val hairline = 1.dp
}

internal val PrismShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

internal val PrismTypography = levyraTypographyFor(LevyraFontPreset.Outfit).let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(letterSpacing = (-1.1).sp),
        headlineMedium = base.headlineMedium.copy(letterSpacing = (-0.7).sp),
        titleLarge = base.titleLarge.copy(letterSpacing = (-0.4).sp),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.8.sp)
    )
}

internal fun prismPalette(
    settings: PrismSettings = PrismSettings(),
    systemDark: Boolean = true,
    artworkStart: Int? = null,
    artworkEnd: Int? = null
): LevyraPalette {
    val light = settings.appearance == PrismAppearance.Light ||
        settings.appearance == PrismAppearance.Automatic && !systemDark
    val seeds = when (settings.colors) {
        PrismColors.Adaptive -> (artworkStart ?: 0xFFA18AFF.toInt()) to (artworkEnd ?: 0xFF65DFF4.toInt())
        PrismColors.Ultraviolet -> 0xFFA18AFF.toInt() to 0xFFEC70C9.toInt()
        PrismColors.Glacier -> 0xFF638FFF.toInt() to 0xFF65DFF4.toInt()
    }
    fun tone(seed: Int, tone: Double, chroma: Double): Color {
        val source = Hct.fromInt(seed)
        return Color(Hct.from(source.hue, source.chroma.coerceIn(16.0, chroma), tone).toInt())
    }
    val accent = tone(seeds.first, if (light) 40.0 else 78.0, 48.0)
    val companion = tone(seeds.second, if (light) 40.0 else 80.0, 40.0)
    return LevyraThemes.cosmic.copy(
        id = PrismPaletteId, label = "Levyra Prism", emoji = "◈", isLight = light,
        followsCover = false, followsMood = false,
        black = if (light) Color(0xFFF5F5FA) else Color(0xFF080A12),
        ink = if (light) Color.White else Color(0xFF101321),
        panel = if (light) Color.White else Color(0xFF171B2B),
        panelSoft = if (light) Color(0xFFE9EBF5) else Color(0xFF22283C),
        cyan = accent, blue = tone(seeds.first, if (light) 48.0 else 68.0, 48.0),
        violet = companion, pink = tone(seeds.second, if (light) 45.0 else 72.0, 44.0), orange = companion,
        text = if (light) Color(0xFF171827) else Color(0xFFF8F9FF),
        muted = if (light) Color(0xFF62697D) else Color(0xFFADB5CC),
        outline = if (light) Color(0xFFCDD1E2) else Color(0xFF363E56)
    )
}

@Composable
internal fun PrismThemeEffect(settings: LevyraInterfaceSettings, track: Track?, animationsEnabled: Boolean) {
    val prism = settings.prism
    val systemDark = isSystemInDarkTheme()
    val key = remember(track?.id, track?.thumbnailUrl, track?.largeThumbnailUrl) {
        track?.let { ArtworkPaletteCache.key(it.id, it.thumbnailUrl, it.largeThumbnailUrl) }.orEmpty()
    }
    val fallback = remember { ArtworkPalette(0xFFA18AFF.toInt(), 0xFF65DFF4.toInt()) }
    val artwork by rememberArtworkPalette(
        paletteKey = if (prism.colors == PrismColors.Adaptive) key else "",
        artworkUrl = track?.let(::preferredPlayerArtworkUrl).orEmpty(),
        fallback = fallback,
        retainPreviousUntilLoaded = true
    )
    val target = remember(prism, systemDark, artwork) { prismPalette(prism, systemDark, artwork.start, artwork.end) }
    val motion = animationsEnabled && LocalLevyraVisualCapabilities.current.decorativeMotion
    val start by animateColorAsState(target.cyan, if (motion) tween(450) else snap(), label = "prism-primary")
    val end by animateColorAsState(target.violet, if (motion) tween(450) else snap(), label = "prism-companion")
    val animatedPalette = target.copy(cyan = start, violet = end, blue = start, pink = end)
    SideEffect {
        activePrismSettings = prism
        LevyraThemeController.applyPrism(animatedPalette)
    }
}

internal fun prismMiniSettings(saved: LevyraMiniPlayerStudioSettings): LevyraMiniPlayerStudioSettings {
    val tuning = LevyraMiniPlayerStudioSettings.spec(MiniPlayerStyle.Floating).defaults.copy(
        cornerDp = 24, heightDp = 68, artworkDp = 48, shadow = 0, glow = 0,
        artworkShape = MiniArtworkShape.Rounded, progress = MiniProgressStyle.Line,
        buttons = MiniButtonStyle.Outlined, colorMode = MiniColorMode.Theme,
        background = MiniBackground.Solid, motion = MiniMotion.Normal,
        controls = saved.activeTuning().controls, showClose = saved.activeTuning().showClose
    )
    return saved.copy(style = MiniPlayerStyle.Floating, tunings = saved.tunings + (MiniPlayerStyle.Floating to tuning))
}

internal fun prismNavSettings(saved: LevyraNavigationStudioSettings): LevyraNavigationStudioSettings {
    val tuning = saved.activeTuning().copy(
        cornerDp = 28, heightDp = 68, indicator = NavIndicator.Pill,
        background = NavBackground.Solid, colorSource = NavColorSource.Theme,
        surfaceTone = NavSurfaceTone.Auto, motion = NavMotion.Minimal,
        opacity = 94, inactiveAlpha = 100, labels = NavLabelMode.Always
    )
    return saved.copy(style = NavStyle.FloatingDock, tunings = saved.tunings + (NavStyle.FloatingDock to tuning))
}

internal fun skinNavigationSettings(saved: LevyraNavigationStudioSettings): LevyraNavigationStudioSettings = when {
    LevyraIsNocturne -> nocturneNavSettings(saved)
    LevyraIsPrism -> prismNavSettings(saved)
    else -> saved
}
