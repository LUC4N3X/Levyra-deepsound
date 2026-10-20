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
    return MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (opaque) 1f else 0.94f)
}

@Composable
internal fun PrismAtmosphere(modifier: Modifier = Modifier) {
    val palette = LevyraActivePalette
    Box(modifier.fillMaxSize().background(palette.black).drawWithCache {
        val top = Brush.radialGradient(
            listOf(palette.cyan.copy(alpha = if (palette.isLight) 0.10f else 0.18f), Color.Transparent),
            center = Offset(size.width * 0.12f, size.height * 0.12f), radius = size.width * 0.95f
        )
        val side = Brush.radialGradient(
            listOf(palette.violet.copy(alpha = if (palette.isLight) 0.08f else 0.13f), Color.Transparent),
            center = Offset(size.width, size.height * 0.36f), radius = size.width * 0.84f
        )
        onDrawBehind { drawRect(top); drawRect(side) }
    })
}

internal fun prismPlayerSurfaces(palette: LevyraPalette): PlayerSurfaceTokens = PlayerSurfaceTokens(
    amoled = false, control = palette.panelSoft, controlQuiet = palette.panel, outline = palette.outline,
    active = palette.panelSoft, activeContent = palette.cyan, tonal = palette.panel,
    hero = palette.cyan, heroContent = if (palette.isLight) Color.White else palette.black,
    content = palette.text, contentMuted = palette.muted, contentFaint = palette.muted, glow = Color.Transparent
)
