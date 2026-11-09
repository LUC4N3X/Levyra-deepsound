package com.luc4n3x.levyra.diagnostics

import androidx.compose.ui.graphics.Color
import com.luc4n3x.levyra.ui.theme.LevyraThemeController
import com.luc4n3x.levyra.ui.theme.LevyraThemes

internal fun applyCrashPresentation(presentation: CrashPresentation): Boolean {
    val base = LevyraThemes.byId(LevyraThemes.APPLE_MUSIC)
    val colors = presentation.colors
    val palette = if (colors.size == 12) base.copy(
        isLight = presentation.light,
        black = Color(colors[0]), ink = Color(colors[1]), panel = Color(colors[2]), panelSoft = Color(colors[3]),
        cyan = Color(colors[4]), blue = Color(colors[5]), violet = Color(colors[6]), pink = Color(colors[7]),
        orange = Color(colors[8]), text = Color(colors[9]), muted = Color(colors[10]), outline = Color(colors[11])
    ) else base
    LevyraThemeController.applyPrism(palette)
    return palette.isLight
}
