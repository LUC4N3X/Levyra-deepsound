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
import com.luc4n3x.levyra.domain.PrismTranslucency
import com.luc4n3x.levyra.ui.player.PlayerSurfaceTokens

@Composable
internal fun prismGlassColor(): Color {
    val capabilities = LocalLevyraVisualCapabilities.current
    val opaque = activePrismSettings.translucency == PrismTranslucency.Reduced || !capabilities.heavyBlur
    if (LevyraIsIvory) return LevyraActivePalette.ink
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
    if (LevyraIsTorque || LevyraIsIvory) {
        Box(modifier.fillMaxSize().background(palette.black))
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

internal fun prismPlayerSurfaces(palette: LevyraPalette): PlayerSurfaceTokens = PlayerSurfaceTokens(
    amoled = false, control = palette.panelSoft, controlQuiet = palette.panel, outline = palette.outline,
    active = palette.panelSoft, activeContent = palette.cyan, tonal = palette.panel,
    hero = palette.cyan, heroContent = if (palette.id == IvoryPaletteId) palette.black else Color.White,
    content = palette.text, contentMuted = palette.muted, contentFaint = palette.muted, glow = Color.Transparent
)
