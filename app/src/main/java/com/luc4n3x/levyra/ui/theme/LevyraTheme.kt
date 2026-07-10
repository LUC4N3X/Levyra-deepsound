package com.luc4n3x.levyra.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

data class LevyraPalette(
    val id: String,
    val label: String,
    val emoji: String,
    val isLight: Boolean,
    val followsCover: Boolean,
    val followsMood: Boolean,
    val black: Color,
    val ink: Color,
    val panel: Color,
    val panelSoft: Color,
    val cyan: Color,
    val blue: Color,
    val violet: Color,
    val pink: Color,
    val orange: Color,
    val text: Color,
    val muted: Color,
    val outline: Color
)

object LevyraThemes {
    const val COSMIC = "cosmic"
    const val AMOLED = "amoled"
    const val NEON_CYAN = "neon_cyan"
    const val PURPLE_GLASS = "purple_glass"
    const val MINIMAL_WHITE = "minimal_white"
    const val COVER_FLOW = "cover_flow"
    const val MOOD_FLOW = "mood_flow"

    val cosmic = LevyraPalette(
        id = COSMIC,
        label = "Linear Glass",
        emoji = "✨",
        isLight = false,
        followsCover = false,
        followsMood = false,
        black = Color(0xFF030303),
        ink = Color(0xFF0A0A0A),
        panel = Color(0xFF121212),
        panelSoft = Color(0xFF1A1A1A),
        cyan = Color(0xFF5E6AD2),
        blue = Color(0xFF26B5CE),
        violet = Color(0xFF8A63D2),
        pink = Color(0xFFE24A8D),
        orange = Color(0xFFF58E3E),
        text = Color(0xFFEDEDED),
        muted = Color(0xFF8A8A93),
        outline = Color(0x33FFFFFF)
    )

    val amoled = cosmic.copy(
        id = AMOLED,
        label = "Black AMOLED",
        emoji = "🖤",
        black = Color(0xFF000000),
        ink = Color(0xFF000000),
        panel = Color(0xFF0A0A0C),
        panelSoft = Color(0xFF121216),
        muted = Color(0xFFAFA9C2),
        outline = Color(0x2467E8FF)
    )

    val neonCyan = cosmic.copy(
        id = NEON_CYAN,
        label = "Neon Cyan",
        emoji = "⚡",
        black = Color(0xFF01070C),
        ink = Color(0xFF041017),
        panel = Color(0xFF071A24),
        panelSoft = Color(0xFF0B2431),
        cyan = Color(0xFF00F0FF),
        blue = Color(0xFF00A8FF),
        violet = Color(0xFF38D6F5),
        pink = Color(0xFF35FFC3),
        orange = Color(0xFFB4FF39),
        muted = Color(0xFF9CC4D4),
        outline = Color(0x4D00F0FF)
    )

    val purpleGlass = cosmic.copy(
        id = PURPLE_GLASS,
        label = "Purple Glass",
        emoji = "🔮",
        black = Color(0xFF0B0417),
        ink = Color(0xFF150A26),
        panel = Color(0xFF1E1133),
        panelSoft = Color(0xFF291845),
        cyan = Color(0xFFC084FC),
        blue = Color(0xFF8B5CF6),
        violet = Color(0xFFD8B4FE),
        pink = Color(0xFFF472B6),
        orange = Color(0xFFFBBF24),
        muted = Color(0xFFC4B4E0),
        outline = Color(0x40C084FC)
    )

    val minimalWhite = cosmic.copy(
        id = MINIMAL_WHITE,
        label = "Minimal White",
        emoji = "🤍",
        isLight = true,
        black = Color(0xFFFFFFFF),
        ink = Color(0xFFF8FAFF),
        panel = Color(0xFFEFF3FA),
        panelSoft = Color(0xFFE4EAF5),
        cyan = Color(0xFF0084A8),
        blue = Color(0xFF2E5BFF),
        violet = Color(0xFF6D3FE0),
        pink = Color(0xFFD91A6D),
        orange = Color(0xFFC66A00),
        text = Color(0xFF11131F),
        muted = Color(0xFF5F667C),
        outline = Color(0x330084A8)
    )

    val coverFlow = cosmic.copy(
        id = COVER_FLOW,
        label = "Auto Cover",
        emoji = "💿",
        followsCover = true
    )

    val moodFlow = cosmic.copy(
        id = MOOD_FLOW,
        label = "Mood",
        emoji = "🌗",
        followsMood = true
    )

    val presets: List<LevyraPalette> = listOf(cosmic, amoled, neonCyan, purpleGlass, minimalWhite, coverFlow, moodFlow)

    fun byId(id: String): LevyraPalette = presets.firstOrNull { it.id == id } ?: cosmic

    fun normalize(id: String): String = byId(id).id
}

private val activePaletteState = mutableStateOf(LevyraThemes.cosmic)

object LevyraThemeController {
    fun apply(
        presetId: String,
        coverAccentStart: Int? = null,
        coverAccentEnd: Int? = null,
        moodAccentStart: Int? = null,
        moodAccentEnd: Int? = null
    ) {
        val base = LevyraThemes.byId(presetId)
        val palette = when {
            base.followsCover && coverAccentStart != null && coverAccentEnd != null ->
                tinted(base, Color(coverAccentStart), Color(coverAccentEnd))
            base.followsMood && moodAccentStart != null && moodAccentEnd != null ->
                tinted(base, Color(moodAccentStart), Color(moodAccentEnd))
            else -> base
        }
        if (activePaletteState.value != palette) {
            activePaletteState.value = palette
        }
    }

    private fun tinted(base: LevyraPalette, start: Color, end: Color): LevyraPalette = base.copy(
        cyan = brighten(start),
        blue = start,
        violet = brighten(end),
        pink = end,
        outline = start.copy(alpha = 0.28f)
    )

    private fun brighten(color: Color): Color = Color(
        red = color.red + (1f - color.red) * 0.35f,
        green = color.green + (1f - color.green) * 0.35f,
        blue = color.blue + (1f - color.blue) * 0.35f,
        alpha = 1f
    )
}

val LevyraActivePalette: LevyraPalette get() = activePaletteState.value
val LevyraBlack: Color get() = activePaletteState.value.black
val LevyraInk: Color get() = activePaletteState.value.ink
val LevyraPanel: Color get() = activePaletteState.value.panel
val LevyraPanelSoft: Color get() = activePaletteState.value.panelSoft
val LevyraCyan: Color get() = activePaletteState.value.cyan
val LevyraBlue: Color get() = activePaletteState.value.blue
val LevyraViolet: Color get() = activePaletteState.value.violet
val LevyraPink: Color get() = activePaletteState.value.pink
val LevyraOrange: Color get() = activePaletteState.value.orange
val LevyraText: Color get() = activePaletteState.value.text
val LevyraMuted: Color get() = activePaletteState.value.muted
val LevyraGlass: Color get() = if (activePaletteState.value.isLight) Color(0x14101322) else Color(0x0FFFFFFF)
val LevyraGlassBorder: Color get() = if (activePaletteState.value.isLight) Color(0x26101322) else Color(0x1AFFFFFF)
val LevyraOnAccent: Color get() = if (activePaletteState.value.isLight) Color(0xFFF8F7FF) else activePaletteState.value.black

private fun schemeFor(palette: LevyraPalette): ColorScheme {
    return if (palette.isLight) {
        lightColorScheme(
            primary = palette.cyan,
            onPrimary = Color.White,
            secondary = palette.violet,
            onSecondary = Color.White,
            tertiary = palette.pink,
            background = palette.black,
            onBackground = palette.text,
            surface = palette.ink,
            onSurface = palette.text,
            surfaceVariant = palette.panel,
            onSurfaceVariant = palette.muted,
            outline = palette.outline
        )
    } else {
        darkColorScheme(
            primary = palette.cyan,
            onPrimary = palette.black,
            secondary = palette.violet,
            onSecondary = palette.text,
            tertiary = palette.pink,
            background = palette.black,
            onBackground = palette.text,
            surface = palette.ink,
            onSurface = palette.text,
            surfaceVariant = palette.panel,
            onSurfaceVariant = palette.muted,
            outline = palette.outline
        )
    }
}

@Composable
fun LevyraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = schemeFor(activePaletteState.value),
        typography = LevyraTypography,
        content = content
    )
}
