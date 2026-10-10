package com.luc4n3x.levyra.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.luc4n3x.levyra.ui.PlayerMinimumContrast
import com.luc4n3x.levyra.ui.playerContrastRatio
import com.materialkolor.hct.Hct

internal data class LevyraTonalRole(
    val container: Color,
    val onContainer: Color
)

private const val DarkContainerTone = 30.0
private const val DarkOnContainerTone = 90.0
private const val LightContainerTone = 90.0
private const val LightOnContainerTone = 10.0
private const val AccentContainerChroma = 48.0
private const val NeutralContainerChroma = 32.0

internal fun levyraTonalRole(seed: Color, isLight: Boolean, maxChroma: Double): LevyraTonalRole {
    val source = Hct.fromInt(seed.toArgb())
    val chroma = source.chroma.coerceAtMost(maxChroma)
    val containerTone = if (isLight) LightContainerTone else DarkContainerTone
    val onContainerTone = if (isLight) LightOnContainerTone else DarkOnContainerTone
    return LevyraTonalRole(
        container = Color(Hct.from(source.hue, chroma, containerTone).toInt()),
        onContainer = Color(Hct.from(source.hue, chroma, onContainerTone).toInt())
    )
}

internal fun levyraInverseAccent(seed: Color, isLight: Boolean): Color {
    val source = Hct.fromInt(seed.toArgb())
    return Color(Hct.from(source.hue, source.chroma, if (isLight) 80.0 else 40.0).toInt())
}

internal fun levyraReadableAccent(accent: Color, content: Color): Color {
    if (playerContrastRatio(content, accent) >= PlayerMinimumContrast) return accent
    val source = Hct.fromInt(accent.toArgb())
    val step = if (content.luminance() > 0.5f) -1.0 else 1.0
    var tone = source.tone
    var candidate = accent
    while (tone in 0.0..100.0) {
        tone += step
        candidate = Color(Hct.from(source.hue, source.chroma, tone.coerceIn(0.0, 100.0)).toInt())
        if (playerContrastRatio(content, candidate) >= PlayerMinimumContrast) return candidate
    }
    return candidate
}

internal fun levyraColorScheme(palette: LevyraPalette): ColorScheme {
    val light = palette.isLight
    val primary = levyraTonalRole(palette.cyan, light, AccentContainerChroma)
    val secondary = levyraTonalRole(palette.violet, light, NeutralContainerChroma)
    val tertiary = levyraTonalRole(palette.pink, light, AccentContainerChroma)
    val inversePrimary = levyraInverseAccent(palette.cyan, light)
    val onPrimaryColor = if (light) Color.White else palette.black
    val onSecondaryColor = if (light) Color.White else palette.black
    val onTertiaryColor = if (light) Color.White else palette.black
    val primaryColor = levyraReadableAccent(palette.cyan, onPrimaryColor)
    val secondaryColor = levyraReadableAccent(palette.violet, onSecondaryColor)
    val tertiaryColor = levyraReadableAccent(palette.pink, onTertiaryColor)
    val containerHighest = lerp(palette.panelSoft, palette.text, 0.06f)
    val outlineVariant = lerp(palette.panelSoft, palette.muted, 0.35f)
    return if (light) {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            primaryContainer = primary.container,
            onPrimaryContainer = primary.onContainer,
            inversePrimary = inversePrimary,
            secondary = secondaryColor,
            onSecondary = onSecondaryColor,
            secondaryContainer = secondary.container,
            onSecondaryContainer = secondary.onContainer,
            tertiary = tertiaryColor,
            onTertiary = onTertiaryColor,
            tertiaryContainer = tertiary.container,
            onTertiaryContainer = tertiary.onContainer,
            background = palette.black,
            onBackground = palette.text,
            surface = palette.ink,
            onSurface = palette.text,
            surfaceVariant = palette.panel,
            onSurfaceVariant = palette.muted,
            surfaceTint = primaryColor,
            inverseSurface = palette.text,
            inverseOnSurface = palette.ink,
            outline = palette.outline,
            outlineVariant = outlineVariant,
            surfaceBright = palette.black,
            surfaceDim = lerp(palette.panelSoft, palette.text, 0.08f),
            surfaceContainerLowest = palette.black,
            surfaceContainerLow = palette.ink,
            surfaceContainer = palette.panel,
            surfaceContainerHigh = palette.panelSoft,
            surfaceContainerHighest = containerHighest
        )
    } else {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            primaryContainer = primary.container,
            onPrimaryContainer = primary.onContainer,
            inversePrimary = inversePrimary,
            secondary = secondaryColor,
            onSecondary = onSecondaryColor,
            secondaryContainer = secondary.container,
            onSecondaryContainer = secondary.onContainer,
            tertiary = tertiaryColor,
            onTertiary = onTertiaryColor,
            tertiaryContainer = tertiary.container,
            onTertiaryContainer = tertiary.onContainer,
            background = palette.black,
            onBackground = palette.text,
            surface = palette.ink,
            onSurface = palette.text,
            surfaceVariant = palette.panel,
            onSurfaceVariant = palette.muted,
            surfaceTint = primaryColor,
            inverseSurface = palette.text,
            inverseOnSurface = palette.ink,
            outline = palette.outline,
            outlineVariant = outlineVariant,
            surfaceBright = lerp(palette.panelSoft, palette.text, 0.10f),
            surfaceDim = palette.black,
            surfaceContainerLowest = palette.black,
            surfaceContainerLow = palette.ink,
            surfaceContainer = palette.panel,
            surfaceContainerHigh = palette.panelSoft,
            surfaceContainerHighest = containerHighest
        )
    }
}
