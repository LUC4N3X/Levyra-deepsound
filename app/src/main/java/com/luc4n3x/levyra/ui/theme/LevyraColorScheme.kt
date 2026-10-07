package com.luc4n3x.levyra.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
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

internal fun levyraColorScheme(palette: LevyraPalette): ColorScheme {
    val light = palette.isLight
    val primary = levyraTonalRole(palette.cyan, light, AccentContainerChroma)
    val secondary = levyraTonalRole(palette.violet, light, NeutralContainerChroma)
    val tertiary = levyraTonalRole(palette.pink, light, AccentContainerChroma)
    val inversePrimary = levyraInverseAccent(palette.cyan, light)
    val containerHighest = lerp(palette.panelSoft, palette.text, 0.06f)
    val outlineVariant = lerp(palette.panelSoft, palette.muted, 0.35f)
    return if (light) {
        lightColorScheme(
            primary = palette.cyan,
            onPrimary = Color.White,
            primaryContainer = primary.container,
            onPrimaryContainer = primary.onContainer,
            inversePrimary = inversePrimary,
            secondary = palette.violet,
            onSecondary = Color.White,
            secondaryContainer = secondary.container,
            onSecondaryContainer = secondary.onContainer,
            tertiary = palette.pink,
            onTertiary = Color.White,
            tertiaryContainer = tertiary.container,
            onTertiaryContainer = tertiary.onContainer,
            background = palette.black,
            onBackground = palette.text,
            surface = palette.ink,
            onSurface = palette.text,
            surfaceVariant = palette.panel,
            onSurfaceVariant = palette.muted,
            surfaceTint = palette.cyan,
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
            primary = palette.cyan,
            onPrimary = palette.black,
            primaryContainer = primary.container,
            onPrimaryContainer = primary.onContainer,
            inversePrimary = inversePrimary,
            secondary = palette.violet,
            onSecondary = palette.text,
            secondaryContainer = secondary.container,
            onSecondaryContainer = secondary.onContainer,
            tertiary = palette.pink,
            onTertiary = palette.black,
            tertiaryContainer = tertiary.container,
            onTertiaryContainer = tertiary.onContainer,
            background = palette.black,
            onBackground = palette.text,
            surface = palette.ink,
            onSurface = palette.text,
            surfaceVariant = palette.panel,
            onSurfaceVariant = palette.muted,
            surfaceTint = palette.cyan,
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
