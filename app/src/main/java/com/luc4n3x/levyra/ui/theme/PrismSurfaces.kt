package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.luc4n3x.levyra.domain.PrismTranslucency
import com.luc4n3x.levyra.ui.player.PlayerSurfaceTokens

@Composable
internal fun prismGlassColor(): Color {
    val capabilities = LocalLevyraVisualCapabilities.current
    val opaque = activePrismSettings.translucency == PrismTranslucency.Reduced || !capabilities.heavyBlur
    if (LevyraIsIvory) return LevyraActivePalette.ink
    if (LevyraIsSpotify) return SpotifyDesign.elevated
    if (LevyraIsAppleMusic) return AppleMusicDesign.chrome
    if (LevyraIsYouTubeMusic) return YouTubeMusicDesign.surface
    if (LevyraIsQobuz) return QobuzDesign.miniPlayer
    return MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (opaque) 1f else 0.96f)
}

@Composable
internal fun PrismAtmosphere(modifier: Modifier = Modifier) {
    val palette = LevyraActivePalette
    val wash = PrismAccent.wash(palette.isLight)
    Box(modifier.fillMaxSize().background(palette.black).drawWithCache {
        val top = Brush.verticalGradient(listOf(wash.copy(alpha = wash.alpha * 0.6f), Color.Transparent), endY = size.height * 0.32f)
        onDrawBehind { drawRect(top) }
    })
}

@Composable
internal fun PrismPlayerAtmosphere(modifier: Modifier = Modifier) {
    val palette = LevyraActivePalette
    if (LevyraIsTorque || LevyraIsIvory || LevyraIsSpotify || LevyraIsAppleMusic || LevyraIsYouTubeMusic || LevyraIsQobuz) {
        Box(modifier.fillMaxSize().background(if (LevyraIsAppleMusic || LevyraIsYouTubeMusic) Color.Black else palette.black))
        return
    }
    val wash = PrismAccent.wash(palette.isLight)
    Box(modifier.fillMaxSize().background(palette.black).drawWithCache {
        val fall = Brush.verticalGradient(listOf(wash, wash.copy(alpha = wash.alpha * 0.35f), Color.Transparent), endY = size.height * 0.72f)
        val halo = Brush.radialGradient(
            listOf(palette.violet.copy(alpha = if (palette.isLight) 0.05f else 0.10f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.3f), radius = size.width * 0.8f
        )
        onDrawBehind { drawRect(fall); drawRect(halo) }
    })
}

internal fun prismPlayerSurfaces(palette: LevyraPalette): PlayerSurfaceTokens = if (palette.id == AppleMusicPaletteId) PlayerSurfaceTokens(
    amoled = false, control = AppleMusicVibrant.quaternary, controlQuiet = Color.Transparent, outline = Color.Transparent,
    active = AppleMusicVibrant.tertiary, activeContent = AppleMusicVibrant.primary, tonal = AppleMusicVibrant.quaternary,
    hero = AppleMusicVibrant.primary, heroContent = Color.Black,
    content = AppleMusicVibrant.primary, contentMuted = AppleMusicVibrant.secondary, contentFaint = AppleMusicVibrant.tertiary, glow = Color.Transparent
) else if (palette.id == SpotifyPaletteId) PlayerSurfaceTokens(
    amoled = false, control = SpotifyDesign.tinted, controlQuiet = Color.Transparent, outline = Color.Transparent,
    active = SpotifyDesign.tintedHighlight, activeContent = SpotifyDesign.green, tonal = SpotifyDesign.tinted,
    hero = Color.White, heroContent = Color.Black,
    content = SpotifyDesign.text, contentMuted = SpotifyDesign.subdued, contentFaint = SpotifyDesign.faint, glow = Color.Transparent
) else PlayerSurfaceTokens(
    amoled = false, control = palette.panelSoft, controlQuiet = palette.panel, outline = palette.outline,
    active = palette.panelSoft, activeContent = palette.cyan, tonal = palette.panel,
    hero = palette.cyan, heroContent = prismHeroContent(palette),
    content = palette.text, contentMuted = palette.muted, contentFaint = palette.muted, glow = Color.Transparent
)

internal fun prismHeroContent(palette: LevyraPalette): Color = when {
    palette.id == IvoryPaletteId -> palette.black
    palette.cyan.luminance() > PrismHeroLightAccentLuminance -> Color.Black
    else -> Color.White
}

private const val PrismHeroLightAccentLuminance = 0.5f
