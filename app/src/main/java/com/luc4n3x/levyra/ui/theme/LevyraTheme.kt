package com.luc4n3x.levyra.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.luc4n3x.levyra.domain.LevyraCustomTheme
import com.luc4n3x.levyra.domain.LevyraFontPreset
import com.luc4n3x.levyra.domain.LevyraSkin

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
    const val LEVYRA_AURA = "levyra_aura"
    const val APPLE_MUSIC = LEVYRA_AURA
    const val COVER_FLOW = "cover_flow"
    const val MOOD_FLOW = "mood_flow"
    const val FROST = "frost"
    const val OCEAN = "ocean"
    const val EMBER = "ember"
    const val MIDNIGHT = "midnight"
    const val AURORA = "aurora"
    const val PURE_MATERIAL = "pure_material"
    const val VELVET = "velvet"
    const val EDITORIAL = "editorial"

    private const val LEGACY_APPLE_MUSIC = "apple_music"

    val cosmic = LevyraPalette(
        id = COSMIC,
        label = "Linear Glass",
        emoji = "✨",
        isLight = false,
        followsCover = false,
        followsMood = false,
        black = Color(0xFF030303),
        ink = Color(0xFF0A0A0A),
        panel = Color(0xFF1D1D1F),
        panelSoft = Color(0xFF2D2D2F),
        cyan = Color(0xFF2997FF),
        blue = Color(0xFF60A5FA),
        violet = Color(0xFF818CF8),
        pink = Color(0xFFEC4899),
        orange = Color(0xFFF58E3E),
        text = Color(0xFFF5F5F7),
        muted = Color(0xFF86868B),
        outline = Color(0x33FFFFFF)
    )

    val levyraAura = cosmic.copy(
        id = LEVYRA_AURA,
        label = "Levyra Aura",
        emoji = "✦",
        black = Color(0xFF121212),
        ink = Color(0xFF181818),
        panel = Color(0xFF242424),
        panelSoft = Color(0xFF2E2E2E),
        cyan = Color(0xFF0A84FF),
        blue = Color(0xFF0066FF),
        violet = Color(0xFF5E5CE6),
        pink = Color(0xFF0A84FF),
        orange = Color(0xFF30D158),
        text = Color(0xFFF5F5F7),
        muted = Color(0xFF8E8E93),
        outline = Color(0x330A84FF)
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

    val frost = cosmic.copy(
        id = FROST,
        label = "Frost",
        emoji = "❄",
        isLight = true,
        black = Color(0xFFF7F9FC),
        ink = Color(0xFFEFF3F9),
        panel = Color(0xFFE4EBF4),
        panelSoft = Color(0xFFD7E0EC),
        cyan = Color(0xFF00708F),
        blue = Color(0xFF2F6BD8),
        violet = Color(0xFF5A63C8),
        pink = Color(0xFF9C4BB0),
        orange = Color(0xFFB2640C),
        text = Color(0xFF10151D),
        muted = Color(0xFF55606F),
        outline = Color(0x3300708F)
    )

    val ocean = cosmic.copy(
        id = OCEAN,
        label = "Ocean",
        emoji = "🌊",
        black = Color(0xFF03090F),
        ink = Color(0xFF061420),
        panel = Color(0xFF0A1F30),
        panelSoft = Color(0xFF102B41),
        cyan = Color(0xFF3FC4D8),
        blue = Color(0xFF2E86C8),
        violet = Color(0xFF4FA8E8),
        pink = Color(0xFF63D6C0),
        orange = Color(0xFF8FD98A),
        text = Color(0xFFEAF4F8),
        muted = Color(0xFF8FA9B8),
        outline = Color(0x333FC4D8)
    )

    val ember = cosmic.copy(
        id = EMBER,
        label = "Ember",
        emoji = "🔥",
        black = Color(0xFF0C0605),
        ink = Color(0xFF150B08),
        panel = Color(0xFF22120D),
        panelSoft = Color(0xFF2E1912),
        cyan = Color(0xFFFF8A4C),
        blue = Color(0xFFE2633A),
        violet = Color(0xFFFFA96B),
        pink = Color(0xFFD9485F),
        orange = Color(0xFFFFC46B),
        text = Color(0xFFF7EBE5),
        muted = Color(0xFFB2938A),
        outline = Color(0x33FF8A4C)
    )

    val midnight = cosmic.copy(
        id = MIDNIGHT,
        label = "Midnight",
        emoji = "\uD83C\uDF19",
        black = Color(0xFF05070F),
        ink = Color(0xFF0A0E1A),
        panel = Color(0xFF131A2B),
        panelSoft = Color(0xFF1C2438),
        cyan = Color(0xFF8AB4FF),
        blue = Color(0xFF4C7DFF),
        violet = Color(0xFFA5B8FF),
        pink = Color(0xFF7F9CF5),
        orange = Color(0xFFFFC978),
        text = Color(0xFFE8ECF6),
        muted = Color(0xFF8A93AB),
        outline = Color(0x338AB4FF)
    )

    val aurora = cosmic.copy(
        id = AURORA,
        label = "Aurora",
        emoji = "\uD83C\uDF0C",
        black = Color(0xFF040B0A),
        ink = Color(0xFF081412),
        panel = Color(0xFF10211E),
        panelSoft = Color(0xFF172C28),
        cyan = Color(0xFF5EE6B5),
        blue = Color(0xFF2BC59A),
        violet = Color(0xFFB59CFF),
        pink = Color(0xFFE38BD8),
        orange = Color(0xFFF6D776),
        text = Color(0xFFE9F6F1),
        muted = Color(0xFF8FB0A6),
        outline = Color(0x335EE6B5)
    )

    val pureMaterial = cosmic.copy(
        id = PURE_MATERIAL,
        label = "Pure Material",
        emoji = "\u25D0",
        black = Color(0xFF141218),
        ink = Color(0xFF1D1B20),
        panel = Color(0xFF211F26),
        panelSoft = Color(0xFF2B2930),
        cyan = Color(0xFFD0BCFF),
        blue = Color(0xFF6750A4),
        violet = Color(0xFFCCC2DC),
        pink = Color(0xFFEFB8C8),
        orange = Color(0xFFF2B8B5),
        text = Color(0xFFE6E0E9),
        muted = Color(0xFFCAC4D0),
        outline = Color(0x33938F99)
    )

    val velvet = cosmic.copy(
        id = VELVET,
        label = "Velvet",
        emoji = "\uD83C\uDF77",
        black = Color(0xFF0E0508),
        ink = Color(0xFF180A0F),
        panel = Color(0xFF26111A),
        panelSoft = Color(0xFF331822),
        cyan = Color(0xFFF28AA8),
        blue = Color(0xFFC2416B),
        violet = Color(0xFFE0A8C0),
        pink = Color(0xFFFF7E9D),
        orange = Color(0xFFE8B86B),
        text = Color(0xFFF6E9EE),
        muted = Color(0xFFB48E9C),
        outline = Color(0x33F28AA8)
    )

    val editorial = cosmic.copy(
        id = EDITORIAL,
        label = "Editorial",
        emoji = "\uD83D\uDCF0",
        isLight = true,
        black = Color(0xFFF6F2EA),
        ink = Color(0xFFEFE9DE),
        panel = Color(0xFFE6DED0),
        panelSoft = Color(0xFFDCD2C1),
        cyan = Color(0xFFA63C26),
        blue = Color(0xFF8C3420),
        violet = Color(0xFF3E5C76),
        pink = Color(0xFFB4442C),
        orange = Color(0xFF8F6417),
        text = Color(0xFF1B1813),
        muted = Color(0xFF6B6357),
        outline = Color(0x331B1813)
    )

    val presets: List<LevyraPalette> = listOf(
        levyraAura,
        coverFlow,
        amoled,
        frost,
        ocean,
        ember,
        cosmic,
        neonCyan,
        purpleGlass,
        minimalWhite,
        moodFlow,
        midnight,
        aurora,
        pureMaterial,
        velvet,
        editorial
    )

    @Volatile
    private var customThemes: List<LevyraCustomTheme> = emptyList()

    @Volatile
    private var customPalettes: Map<String, LevyraPalette> = emptyMap()

    val defaultPresetId: String get() = normalize("")

    fun setCustomThemes(themes: List<LevyraCustomTheme>) {
        customThemes = themes
        customPalettes = themes.associate { it.id to it.toPalette() }
    }

    fun customTheme(id: String): LevyraCustomTheme? = customThemes.firstOrNull { it.id == id }

    fun byId(id: String): LevyraPalette = when {
        id == LEVYRA_AURA || id == LEGACY_APPLE_MUSIC -> levyraAura
        LevyraCustomTheme.isCustomId(id) -> customPalettes[id] ?: levyraAura
        else -> presets.firstOrNull { it.id == id } ?: levyraAura
    }

    fun normalize(id: String): String = if (LevyraCustomTheme.isCustomId(id)) id else byId(id).id
}

fun LevyraCustomTheme.toPalette(): LevyraPalette {
    val background = Color(background)
    val surface = Color(surface)
    val text = if (isLight) Color(0xFF15161C) else Color(0xFFF4F4F7)
    val primary = Color(primary)
    val secondary = Color(secondary)
    val readablePrimary = readableOn(primary, background, isLight)
    val readableSecondary = readableOn(secondary, background, isLight)
    return LevyraPalette(
        id = id,
        label = name,
        emoji = "\u25C6",
        isLight = isLight,
        followsCover = false,
        followsMood = false,
        black = background,
        ink = blend(background, surface, 0.35f),
        panel = surface,
        panelSoft = blend(surface, text, 0.08f),
        cyan = readablePrimary,
        blue = primary,
        violet = readableSecondary,
        pink = secondary,
        orange = Color(accent),
        text = text,
        muted = blend(text, background, 0.45f),
        outline = readablePrimary.copy(alpha = 0.28f)
    )
}

private fun blend(from: Color, to: Color, fraction: Float): Color = Color(
    red = from.red + (to.red - from.red) * fraction,
    green = from.green + (to.green - from.green) * fraction,
    blue = from.blue + (to.blue - from.blue) * fraction,
    alpha = 1f
)

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

private fun readableOn(color: Color, background: Color, light: Boolean): Color {
    val target = if (light) Color.Black else Color.White
    var candidate = color
    var step = 0
    while (contrast(candidate, background) < MIN_ACCENT_CONTRAST && step < 10) {
        candidate = blend(candidate, target, 0.15f)
        step += 1
    }
    return candidate
}

private const val MIN_ACCENT_CONTRAST = 3f

private val activePaletteState = mutableStateOf(LevyraThemes.cosmic)
private val activeFontPresetState = mutableStateOf(LevyraFontPreset.Outfit)
private val activePureBlackState = mutableStateOf(false)
private val activeCornerScaleState = mutableStateOf(1f)

object LevyraTypographyController {
    fun apply(preset: LevyraFontPreset) {
        if (activeFontPresetState.value != preset) {
            activeFontPresetState.value = preset
        }
    }
}

object LevyraThemeController {
    fun apply(
        presetId: String,
        coverAccentStart: Int? = null,
        coverAccentEnd: Int? = null,
        moodAccentStart: Int? = null,
        moodAccentEnd: Int? = null,
        pureBlack: Boolean = false,
        accentOverride: Int? = null,
        skin: LevyraSkin = LevyraSkin.Default
    ) {
        val base = if (skin == LevyraSkin.Nocturne78) Nocturne78Palette else LevyraThemes.byId(presetId)
        val tinted = when {
            skin == LevyraSkin.Nocturne78 -> base
            accentOverride != null -> withAccent(base, Color(accentOverride))
            base.followsCover && coverAccentStart != null && coverAccentEnd != null ->
                tinted(base, Color(coverAccentStart), Color(coverAccentEnd))
            base.followsMood && moodAccentStart != null && moodAccentEnd != null ->
                tinted(base, Color(moodAccentStart), Color(moodAccentEnd))
            else -> base
        }
        val pureBlackActive = pureBlack && !tinted.isLight && skin == LevyraSkin.Default
        val palette = if (pureBlackActive) asPureBlack(tinted) else tinted

        if (activePureBlackState.value != pureBlackActive) {
            activePureBlackState.value = pureBlackActive
        }
        applyCornerScale(if (skin == LevyraSkin.Nocturne78) "" else presetId)
        if (activePaletteState.value != palette) {
            activePaletteState.value = palette
        }
    }

    private fun applyCornerScale(presetId: String) {
        val cornerScale = LevyraThemes.customTheme(presetId)?.let { it.cornerScale / 100f } ?: 1f
        if (activeCornerScaleState.value != cornerScale) {
            activeCornerScaleState.value = cornerScale
        }
    }

    fun asPureBlack(base: LevyraPalette): LevyraPalette {
        if (base.isLight) return base
        return base.copy(
            black = Color.Black,
            ink = Color.Black,
            panel = Color(0xFF0B0B0D),
            panelSoft = Color(0xFF15151A),
            outline = base.outline.copy(alpha = (base.outline.alpha + 0.12f).coerceAtMost(0.6f))
        )
    }

    fun withAccent(base: LevyraPalette, accent: Color): LevyraPalette {
        val readable = if (base.isLight) darken(accent) else brighten(accent)
        return base.copy(
            cyan = readable,
            blue = accent,
            violet = readable,
            pink = accent,
            outline = readable.copy(alpha = 0.28f)
        )
    }

    private fun darken(color: Color): Color = Color(
        red = color.red * 0.7f,
        green = color.green * 0.7f,
        blue = color.blue * 0.7f,
        alpha = 1f
    )

    private fun tinted(
        base: LevyraPalette,
        start: Color,
        end: Color
    ): LevyraPalette = base.copy(
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

val LevyraActivePalette: LevyraPalette
    get() = activePaletteState.value

val LevyraBlack: Color
    get() = activePaletteState.value.black

val LevyraInk: Color
    get() = activePaletteState.value.ink

val LevyraPanel: Color
    get() = activePaletteState.value.panel

val LevyraPanelSoft: Color
    get() = activePaletteState.value.panelSoft

val LevyraCyan: Color
    get() = activePaletteState.value.cyan

val LevyraBlue: Color
    get() = activePaletteState.value.blue

val LevyraViolet: Color
    get() = activePaletteState.value.violet

val LevyraPink: Color
    get() = activePaletteState.value.pink

val LevyraOrange: Color
    get() = activePaletteState.value.orange

val LevyraText: Color
    get() = activePaletteState.value.text

val LevyraMuted: Color
    get() = activePaletteState.value.muted

val LevyraIsPureBlack: Boolean
    get() = activePureBlackState.value

val LevyraGlass: Color
    get() = when {
        LevyraIsNocturne -> Nocturne78Palette.panel
        activePaletteState.value.isLight -> Color(0x14101322)
        LevyraIsPureBlack -> Color(0x0AFFFFFF)
        else -> Color(0x0FFFFFFF)
    }

val LevyraGlassBorder: Color
    get() = if (LevyraIsNocturne) {
        Nocturne78Palette.outline
    } else if (activePaletteState.value.isLight) {
        Color(0x26101322)
    } else {
        Color(0x1AFFFFFF)
    }

val LevyraOnAccent: Color
    get() = if (activePaletteState.value.isLight) {
        Color(0xFFF8F7FF)
    } else {
        activePaletteState.value.black
    }

private val LevyraExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
    largeIncreased = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(36.dp),
    extraExtraLarge = RoundedCornerShape(44.dp)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun scaledExpressiveShapes(scale: Float): Shapes = Shapes(
    extraSmall = RoundedCornerShape((8 * scale).dp),
    small = RoundedCornerShape((12 * scale).dp),
    medium = RoundedCornerShape((16 * scale).dp),
    large = RoundedCornerShape((24 * scale).dp),
    extraLarge = RoundedCornerShape((32 * scale).dp),
    largeIncreased = RoundedCornerShape((28 * scale).dp),
    extraLargeIncreased = RoundedCornerShape((36 * scale).dp),
    extraExtraLarge = RoundedCornerShape((44 * scale).dp)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object LevyraStaticMotionScheme : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = snap()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LevyraTheme(
    fontPreset: LevyraFontPreset? = null,
    animationsEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val palette = activePaletteState.value
    val colorScheme = remember(palette) { levyraColorScheme(palette) }
    val cornerScale = activeCornerScaleState.value
    val nocturne = LevyraIsNocturne
    val shapes = remember(cornerScale, nocturne) {
        if (nocturne) Nocturne78Shapes
        else if (cornerScale == 1f) LevyraExpressiveShapes else scaledExpressiveShapes(cornerScale)
    }
    val motionScheme = remember(animationsEnabled) {
        if (animationsEnabled) MotionScheme.expressive() else LevyraStaticMotionScheme
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = motionScheme,
        shapes = shapes,
        typography = if (nocturne) Nocturne78Typography else levyraTypographyFor(fontPreset ?: activeFontPresetState.value),
        content = content
    )
}
