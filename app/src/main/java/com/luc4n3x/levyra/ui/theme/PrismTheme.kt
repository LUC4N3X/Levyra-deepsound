package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.domain.*
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.preferredPlayerArtworkUrl
import com.materialkolor.hct.Hct

internal const val PrismPaletteId = "levyra_prism"
internal const val TorquePaletteId = "levyra_torque"
val LevyraIsTorque: Boolean get() = LevyraActivePalette.id == TorquePaletteId
val LevyraIsPrism: Boolean get() = LevyraActivePalette.id == PrismPaletteId || LevyraIsTorque || LevyraIsIvory || LevyraIsSpotify || LevyraIsAppleMusic || LevyraIsYouTubeMusic || LevyraIsQobuz
internal val LevyraSkinManagesLayout: Boolean get() = LevyraIsNocturne || LevyraIsPrism
internal val LevyraIsDefaultSkin: Boolean get() = !LevyraSkinManagesLayout
internal val LevyraSkinLabel: String get() = LevyraActivePalette.label
internal var activePrismSettings by mutableStateOf(PrismSettings())
    private set
internal var activeTorqueSettings by mutableStateOf(TorqueSettings())
    private set

internal object PrismDesign {
    val artworkCorner: Dp get() = if (LevyraIsTorque) 0.dp else if (LevyraIsQobuz) QobuzDesign.playerArtworkCorner else if (LevyraIsYouTubeMusic) YouTubeMusicDesign.playerArtworkCorner else if (LevyraIsAppleMusic) AppleMusicDesign.cornerL else if (LevyraIsSpotify) SpotifyDesign.cornerSmall else if (LevyraIsIvory) IvoryDesign.coverCorner else 18.dp
    val tileCorner: Dp get() = if (LevyraIsTorque) 2.dp else if (LevyraIsQobuz) QobuzDesign.cardCorner else if (LevyraIsYouTubeMusic) YouTubeMusicDesign.cardCorner else if (LevyraIsAppleMusic) AppleMusicDesign.cornerL else if (LevyraIsSpotify) SpotifyDesign.cornerSmall else if (LevyraIsIvory) IvoryDesign.tileCorner else 12.dp
    val surfaceCorner: Dp get() = if (LevyraIsTorque) 2.dp else if (LevyraIsQobuz) QobuzDesign.welcomeCorner else if (LevyraIsYouTubeMusic) YouTubeMusicDesign.tileCorner else if (LevyraIsAppleMusic) AppleMusicDesign.cornerXl else if (LevyraIsSpotify) SpotifyDesign.cornerLarge else if (LevyraIsIvory) IvoryDesign.surfaceCorner else 20.dp
    val controlCorner: Dp get() = if (LevyraIsTorque) 2.dp else if (LevyraIsQobuz) QobuzDesign.cardCorner else if (LevyraIsYouTubeMusic) YouTubeMusicDesign.chipCorner else if (LevyraIsAppleMusic) AppleMusicDesign.cornerL else if (LevyraIsSpotify) SpotifyDesign.corner else if (LevyraIsIvory) IvoryDesign.controlCorner else 14.dp
    val gutter = 24.dp
    val rowHeight = 64.dp
    val iconTile = 44.dp
    val hairline = 1.dp
    val seekbarHeight: Dp get() = if (LevyraIsTorque) 3.dp else if (LevyraIsIvory || LevyraIsSpotify || LevyraIsAppleMusic) 4.dp else if (LevyraIsYouTubeMusic) 2.dp else if (LevyraIsQobuz) QobuzDesign.playerSeek else 6.dp

    fun cut(corner: Dp): Shape =
        if (LevyraIsTorque) CutCornerShape(bottomEnd = 12.dp)
        else if (LevyraIsIvory || LevyraIsSpotify || LevyraIsAppleMusic || LevyraIsYouTubeMusic || LevyraIsQobuz) RoundedCornerShape(corner)
        else RoundedCornerShape(topStart = corner, topEnd = corner, bottomEnd = corner / 3, bottomStart = corner)
}

internal object PrismAccent {
    private val nightStart = Color(0xFFF03A62)
    private val nightEnd = Color(0xFFD13FC0)
    private val lightStart = Color(0xFFC41E4C)
    private val lightEnd = Color(0xFF9F2594)

    fun gradient(light: Boolean = LevyraActivePalette.isLight): Brush = when {
        LevyraIsTorque || LevyraIsSpotify || LevyraIsAppleMusic || LevyraIsYouTubeMusic || LevyraIsQobuz -> SolidColor(LevyraActivePalette.blue)
        LevyraIsIvory -> SolidColor(IvoryDesign.ink)
        light -> Brush.horizontalGradient(listOf(lightStart, lightEnd))
        else -> Brush.horizontalGradient(listOf(nightStart, nightEnd))
    }

    fun wash(light: Boolean): Color = when {
        LevyraIsTorque || LevyraIsIvory || LevyraIsSpotify || LevyraIsAppleMusic || LevyraIsYouTubeMusic || LevyraIsQobuz -> Color.Transparent
        light -> lightStart.copy(alpha = 0.07f)
        else -> nightStart.copy(alpha = 0.16f)
    }

    val Tint: Color get() = LevyraCyan.copy(alpha = if (LevyraActivePalette.isLight) 0.10f else 0.16f)
    val Hairline: Color get() = LevyraText.copy(alpha = if (LevyraActivePalette.isLight) 0.10f else 0.08f)
}

private val PrismRoundShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val TorqueShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp), small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp), large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp)
)

internal val PrismShapes: Shapes get() = if (LevyraIsTorque) TorqueShapes else if (LevyraIsSpotify) SpotifyShapes else if (LevyraIsAppleMusic) AppleMusicShapes else if (LevyraIsYouTubeMusic) YouTubeMusicShapes else if (LevyraIsQobuz) QobuzShapes else if (LevyraIsIvory) IvoryShapes else PrismRoundShapes

internal val PrismTypography = levyraTypographyFor(LevyraFontPreset.Outfit).let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Normal, letterSpacing = (-1.2).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Normal, letterSpacing = (-1.0).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.6).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.4).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = (-0.3).sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.6.sp)
    )
}

internal fun prismPalette(settings: PrismSettings = PrismSettings(), systemDark: Boolean = true): LevyraPalette {
    val light = settings.appearance == PrismAppearance.Light ||
        settings.appearance == PrismAppearance.Automatic && !systemDark
    return LevyraThemes.cosmic.copy(
        id = PrismPaletteId, label = "Prism", emoji = "◈", isLight = light,
        followsCover = false, followsMood = false,
        black = prismColor(light, 0xFFF7F6F8, 0xFF000000),
        ink = prismColor(light, 0xFFFFFFFF, 0xFF0B0B0D),
        panel = prismColor(light, 0xFFFFFFFF, 0xFF131316),
        panelSoft = prismColor(light, 0xFFEEECF0, 0xFF1C1C20),
        cyan = prismColor(light, 0xFFC2185E, 0xFFF2367F),
        blue = prismColor(light, 0xFFC41E4C, 0xFFF03A62),
        violet = prismColor(light, 0xFF9F2594, 0xFFD13FC0),
        pink = prismColor(light, 0xFFB0246E, 0xFFF76AA6),
        orange = prismColor(light, 0xFF9F2594, 0xFFD13FC0),
        text = prismColor(light, 0xFF151418, 0xFFF6F5F7),
        muted = prismColor(light, 0xFF5D5963, 0xFFA7A4AC),
        outline = prismColor(light, 0xFFDAD7DE, 0xFF2B2B31)
    )
}

private fun prismColor(light: Boolean, day: Long, night: Long): Color = Color(if (light) day else night)

internal const val TorqueRed = 0xFFE8322D.toInt()

internal fun torquePalette(settings: TorqueSettings = TorqueSettings(), coverSeed: Int? = null): LevyraPalette {
    val seed = if (settings.accent == TorqueAccent.Cover) coverSeed ?: TorqueRed else TorqueRed
    val source = Hct.fromInt(seed)
    val chroma = if (seed == TorqueRed) source.chroma else source.chroma.coerceAtLeast(36.0)
    fun tone(value: Double, maxChroma: Double) = Color(Hct.from(source.hue, chroma.coerceAtMost(maxChroma), value).toInt())
    val band = if (seed == TorqueRed) Color(TorqueRed) else tone(46.0, 64.0)
    return LevyraThemes.cosmic.copy(
        id = TorquePaletteId, label = "Torque", emoji = "◢", isLight = false,
        followsCover = false, followsMood = false,
        black = Color(0xFF111111), ink = Color(0xFF161616),
        panel = Color(0xFF1D1D1D), panelSoft = Color(0xFF272727),
        cyan = tone(66.0, 72.0), blue = band, violet = tone(34.0, 56.0),
        pink = tone(74.0, 60.0), orange = band,
        text = Color(0xFFF4F4F4), muted = Color(0xFFA9A9A9), outline = Color(0xFF333333)
    )
}

@Composable
internal fun PrismThemeEffect(settings: LevyraInterfaceSettings) {
    val prism = settings.prism
    val systemDark = isSystemInDarkTheme()
    val palette = remember(prism, systemDark) { prismPalette(prism, systemDark) }
    SideEffect {
        activePrismSettings = prism
        LevyraThemeController.applyPrism(palette)
    }
}

@Composable
internal fun TorqueThemeEffect(settings: LevyraInterfaceSettings, track: Track?) {
    val torque = settings.torque
    val key = remember(track?.id, track?.thumbnailUrl, track?.largeThumbnailUrl) {
        track?.let { ArtworkPaletteCache.key(it.id, it.thumbnailUrl, it.largeThumbnailUrl) }.orEmpty()
    }
    val fallback = remember { ArtworkPalette(TorqueRed, TorqueRed) }
    val cover by rememberArtworkPalette(
        paletteKey = if (torque.accent == TorqueAccent.Cover) key else "",
        artworkUrl = track?.let(::preferredPlayerArtworkUrl).orEmpty(),
        fallback = fallback,
        retainPreviousUntilLoaded = true
    )
    val palette = remember(torque, cover) { torquePalette(torque, cover.start.takeIf { it != TorqueRed }) }
    SideEffect {
        activeTorqueSettings = torque
        LevyraThemeController.applyPrism(palette)
    }
}

internal fun prismMiniSettings(saved: LevyraMiniPlayerStudioSettings): LevyraMiniPlayerStudioSettings {
    if (LevyraIsSpotify) return spotifyMiniSettings(saved)
    if (LevyraIsAppleMusic) return appleMusicMiniSettings(saved)
    if (LevyraIsYouTubeMusic) return youTubeMusicMiniSettings(saved)
    if (LevyraIsQobuz) return qobuzMiniSettings(saved)
    val torque = LevyraIsTorque
    val tuning = LevyraMiniPlayerStudioSettings.spec(MiniPlayerStyle.Floating).defaults.copy(
        cornerDp = if (torque) 2 else if (LevyraIsIvory) 26 else 20, heightDp = 64, artworkDp = 46, shadow = 0, glow = 0,
        artworkShape = if (torque) MiniArtworkShape.Square else MiniArtworkShape.Rounded, progress = MiniProgressStyle.Line,
        buttons = if (torque) MiniButtonStyle.Outlined else MiniButtonStyle.Filled, colorMode = MiniColorMode.Theme,
        background = MiniBackground.Solid, motion = MiniMotion.Normal,
        controls = saved.activeTuning().controls, showClose = saved.activeTuning().showClose
    )
    return saved.copy(style = MiniPlayerStyle.Floating, tunings = saved.tunings + (MiniPlayerStyle.Floating to tuning))
}

private fun spotifyMiniSettings(saved: LevyraMiniPlayerStudioSettings): LevyraMiniPlayerStudioSettings {
    val tuning = LevyraMiniPlayerStudioSettings.spec(MiniPlayerStyle.Floating).defaults.copy(
        cornerDp = 8, heightDp = 56, insetDp = 8, artworkDp = 40, shadow = 0, glow = 0,
        artworkShape = MiniArtworkShape.Rounded, progress = MiniProgressStyle.Line,
        buttons = MiniButtonStyle.Plain, colorMode = MiniColorMode.Artwork,
        background = MiniBackground.Solid, motion = MiniMotion.Normal,
        controls = saved.activeTuning().controls, showClose = saved.activeTuning().showClose
    )
    return saved.copy(style = MiniPlayerStyle.Floating, tunings = saved.tunings + (MiniPlayerStyle.Floating to tuning))
}

private fun appleMusicMiniSettings(saved: LevyraMiniPlayerStudioSettings): LevyraMiniPlayerStudioSettings {
    val tuning = LevyraMiniPlayerStudioSettings.spec(MiniPlayerStyle.Floating).defaults.copy(
        cornerDp = 24, heightDp = 48, insetDp = 20, artworkDp = 30, shadow = 0, glow = 0,
        artworkShape = MiniArtworkShape.Rounded, progress = MiniProgressStyle.Hidden,
        buttons = MiniButtonStyle.Plain, colorMode = MiniColorMode.Theme,
        background = MiniBackground.Solid, motion = MiniMotion.Normal,
        controls = saved.activeTuning().controls, showClose = saved.activeTuning().showClose
    )
    return saved.copy(style = MiniPlayerStyle.Floating, tunings = saved.tunings + (MiniPlayerStyle.Floating to tuning))
}

private fun qobuzMiniSettings(saved: LevyraMiniPlayerStudioSettings): LevyraMiniPlayerStudioSettings {
    val tuning = LevyraMiniPlayerStudioSettings.spec(MiniPlayerStyle.Floating).defaults.copy(
        cornerDp = QobuzDesign.miniCorner.value.toInt(), heightDp = QobuzDesign.miniHeight.value.toInt(),
        insetDp = QobuzDesign.miniMargin.value.toInt(), artworkDp = QobuzDesign.miniArtwork.value.toInt(), shadow = 0, glow = 0,
        artworkShape = MiniArtworkShape.Rounded, progress = MiniProgressStyle.Line,
        buttons = MiniButtonStyle.Plain, colorMode = MiniColorMode.Theme,
        background = MiniBackground.Solid, motion = MiniMotion.Normal,
        controls = saved.activeTuning().controls, showClose = false
    )
    return saved.copy(style = MiniPlayerStyle.Floating, tunings = saved.tunings + (MiniPlayerStyle.Floating to tuning))
}

private fun youTubeMusicMiniSettings(saved: LevyraMiniPlayerStudioSettings): LevyraMiniPlayerStudioSettings {
    val tuning = LevyraMiniPlayerStudioSettings.spec(MiniPlayerStyle.YtMusic).defaults.copy(
        cornerDp = 0, heightDp = YouTubeMusicDesign.miniHeight.value.toInt(), insetDp = 0,
        artworkDp = YouTubeMusicDesign.miniThumb.value.toInt(), shadow = 0, glow = 0,
        artworkShape = MiniArtworkShape.Square, progress = MiniProgressStyle.Line,
        buttons = MiniButtonStyle.Plain, colorMode = MiniColorMode.Theme,
        background = MiniBackground.Solid, motion = MiniMotion.Normal,
        controls = MiniControlSet.Essential, showClose = false
    )
    return saved.copy(style = MiniPlayerStyle.YtMusic, tunings = saved.tunings + (MiniPlayerStyle.YtMusic to tuning))
}

internal fun skinMiniPlayerCorner(tuning: MiniPlayerTuning): Dp =
    when {
        LevyraIsYouTubeMusic -> 0.dp
        LevyraIsQobuz -> QobuzDesign.miniCorner
        LevyraIsTorque -> 2.dp
        LevyraIsIvory -> 26.dp
        LevyraIsSpotify -> SpotifyDesign.cornerLarge
        LevyraIsAppleMusic -> AppleMusicDesign.miniHeight / 2
        LevyraIsNocturne -> 10.dp
        else -> tuning.cornerDp.dp
    }

internal fun prismNavSettings(saved: LevyraNavigationStudioSettings): LevyraNavigationStudioSettings {
    val tuning = saved.activeTuning().copy(
        cornerDp = if (LevyraIsTorque) 0 else 24, heightDp = 64, indicator = NavIndicator.Pill,
        background = NavBackground.Solid, colorSource = NavColorSource.Theme,
        surfaceTone = NavSurfaceTone.Auto, motion = NavMotion.Minimal,
        opacity = 96, inactiveAlpha = 100, labels = NavLabelMode.Always
    )
    return saved.copy(style = NavStyle.FloatingDock, tunings = saved.tunings + (NavStyle.FloatingDock to tuning))
}

internal fun skinNavigationSettings(saved: LevyraNavigationStudioSettings): LevyraNavigationStudioSettings = when {
    LevyraIsNocturne -> nocturneNavSettings(saved)
    LevyraIsPrism -> prismNavSettings(saved)
    else -> saved
}

internal fun skinMiniSettings(saved: LevyraMiniPlayerStudioSettings): LevyraMiniPlayerStudioSettings {
    return when {
        LevyraIsNocturne -> nocturneMiniSettings(saved)
        LevyraIsPrism -> prismMiniSettings(saved)
        else -> saved
    }
}
