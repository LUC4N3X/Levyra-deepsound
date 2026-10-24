package com.luc4n3x.levyra.domain

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.json.JSONObject

enum class LyricsStylePreset {
    LEVYRA_ORIGINAL,
    EXPRESSIVE_KARAOKE,
    MINIMAL,
    FOCUS,
    CINEMATIC;

    companion object {
        fun from(value: String?): LyricsStylePreset =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: LEVYRA_ORIGINAL
    }
}

enum class LyricsBackgroundMode {
    ORIGINAL,
    SOLID,
    AMOLED,
    GRADIENT,
    BLURRED_ARTWORK,
    CINEMATIC;

    companion object {
        fun from(value: String?): LyricsBackgroundMode =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: ORIGINAL
    }
}

enum class LyricsAlignment(val textAlign: TextAlign) {
    START(TextAlign.Start),
    CENTER(TextAlign.Center),
    END(TextAlign.End);

    companion object {
        fun from(value: String?): LyricsAlignment =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: START
    }
}

enum class LyricsTextSize(val scale: Float) {
    COMPACT(0.88f),
    STANDARD(1.0f),
    LARGE(1.16f),
    EXTRA_LARGE(1.32f);

    companion object {
        fun from(value: String?): LyricsTextSize =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: STANDARD
    }
}

enum class LyricsLineSpacing(val spacingDp: Dp) {
    COMPACT(2.dp),
    STANDARD(6.dp),
    RELAXED(14.dp),
    SPACIOUS(22.dp);

    companion object {
        fun from(value: String?): LyricsLineSpacing =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: STANDARD
    }
}

enum class LyricsFontWeightPreset(val weight: FontWeight) {
    MEDIUM(FontWeight.Medium),
    SEMI_BOLD(FontWeight.SemiBold),
    BOLD(FontWeight.Bold),
    EXTRA_BOLD(FontWeight.ExtraBold),
    BLACK(FontWeight.Black);

    companion object {
        fun from(value: String?): LyricsFontWeightPreset =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: BOLD
    }
}

enum class LyricsActiveColorMode {
    WHITE,
    AUTOMATIC,
    CUSTOM;

    companion object {
        fun from(value: String?): LyricsActiveColorMode =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: WHITE
    }
}

enum class LyricsGlowEffect {
    OFF,
    SUBTLE,
    VIBRANT;

    companion object {
        fun from(value: String?): LyricsGlowEffect =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: OFF
    }
}

enum class LyricsViewerStyle {
    SKIN_DEFAULT,
    APPLE_MUSIC;

    companion object {
        fun from(value: String?): LyricsViewerStyle =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: SKIN_DEFAULT
    }
}

data class LevyraLyricsStudioSettings(
    val stylePreset: LyricsStylePreset = LyricsStylePreset.LEVYRA_ORIGINAL,
    val fontPreset: LevyraFontPreset = LevyraFontPreset.Outfit,
    val textSize: LyricsTextSize = LyricsTextSize.STANDARD,
    val fontWeight: LyricsFontWeightPreset = LyricsFontWeightPreset.BOLD,
    val alignment: LyricsAlignment = LyricsAlignment.START,
    val lineSpacing: LyricsLineSpacing = LyricsLineSpacing.STANDARD,
    val inactiveOpacity: Float = 0.52f,
    val inactiveScale: Float = 1.0f,
    val activeColorMode: LyricsActiveColorMode = LyricsActiveColorMode.WHITE,
    val customAccentColor: Int = 0xFF64D2FF.toInt(),
    val glowEffect: LyricsGlowEffect = LyricsGlowEffect.OFF,
    val backgroundMode: LyricsBackgroundMode = LyricsBackgroundMode.ORIGINAL,
    val progressiveKaraokeEnabled: Boolean = true,
    val blurEnabled: Boolean = true,
    val viewerStyle: LyricsViewerStyle = LyricsViewerStyle.SKIN_DEFAULT,
    val autoScrollEnabled: Boolean = true,
    val showOriginalLyrics: Boolean = true,
    val romanizationEnabled: Boolean = true,
    val appleBlurEnabled: Boolean = false
) {
    fun normalized(): LevyraLyricsStudioSettings = copy(
        inactiveOpacity = inactiveOpacity.coerceIn(0.15f, 0.90f),
        inactiveScale = inactiveScale.coerceIn(0.70f, 1.0f)
    )

    fun usesAppleMusicLyrics(appleMusicSkin: Boolean): Boolean = when (viewerStyle) {
        LyricsViewerStyle.APPLE_MUSIC -> true
        LyricsViewerStyle.SKIN_DEFAULT -> appleMusicSkin
    }

    fun applyPreset(preset: LyricsStylePreset): LevyraLyricsStudioSettings = presetBase(preset).copy(
        viewerStyle = viewerStyle,
        autoScrollEnabled = autoScrollEnabled,
        showOriginalLyrics = showOriginalLyrics,
        romanizationEnabled = romanizationEnabled,
        appleBlurEnabled = appleBlurEnabled
    )

    private fun presetBase(preset: LyricsStylePreset): LevyraLyricsStudioSettings = when (preset) {
        LyricsStylePreset.LEVYRA_ORIGINAL -> LevyraLyricsStudioSettings(
            stylePreset = LyricsStylePreset.LEVYRA_ORIGINAL,
            fontPreset = LevyraFontPreset.Outfit,
            textSize = LyricsTextSize.STANDARD,
            fontWeight = LyricsFontWeightPreset.BOLD,
            alignment = LyricsAlignment.START,
            lineSpacing = LyricsLineSpacing.STANDARD,
            inactiveOpacity = 0.52f,
            inactiveScale = 1.0f,
            activeColorMode = LyricsActiveColorMode.WHITE,
            customAccentColor = customAccentColor,
            glowEffect = LyricsGlowEffect.OFF,
            backgroundMode = LyricsBackgroundMode.ORIGINAL,
            progressiveKaraokeEnabled = true,
            blurEnabled = true
        )
        LyricsStylePreset.EXPRESSIVE_KARAOKE -> LevyraLyricsStudioSettings(
            stylePreset = LyricsStylePreset.EXPRESSIVE_KARAOKE,
            fontPreset = LevyraFontPreset.Outfit,
            textSize = LyricsTextSize.LARGE,
            fontWeight = LyricsFontWeightPreset.EXTRA_BOLD,
            alignment = LyricsAlignment.START,
            lineSpacing = LyricsLineSpacing.RELAXED,
            inactiveOpacity = 0.42f,
            inactiveScale = 0.88f,
            activeColorMode = LyricsActiveColorMode.AUTOMATIC,
            customAccentColor = customAccentColor,
            glowEffect = LyricsGlowEffect.SUBTLE,
            backgroundMode = LyricsBackgroundMode.GRADIENT,
            progressiveKaraokeEnabled = true,
            blurEnabled = true
        )
        LyricsStylePreset.MINIMAL -> LevyraLyricsStudioSettings(
            stylePreset = LyricsStylePreset.MINIMAL,
            fontPreset = LevyraFontPreset.Inter,
            textSize = LyricsTextSize.STANDARD,
            fontWeight = LyricsFontWeightPreset.SEMI_BOLD,
            alignment = LyricsAlignment.CENTER,
            lineSpacing = LyricsLineSpacing.COMPACT,
            inactiveOpacity = 0.32f,
            inactiveScale = 0.94f,
            activeColorMode = LyricsActiveColorMode.WHITE,
            customAccentColor = customAccentColor,
            glowEffect = LyricsGlowEffect.OFF,
            backgroundMode = LyricsBackgroundMode.AMOLED,
            progressiveKaraokeEnabled = false,
            blurEnabled = false
        )
        LyricsStylePreset.FOCUS -> LevyraLyricsStudioSettings(
            stylePreset = LyricsStylePreset.FOCUS,
            fontPreset = LevyraFontPreset.Manrope,
            textSize = LyricsTextSize.LARGE,
            fontWeight = LyricsFontWeightPreset.BLACK,
            alignment = LyricsAlignment.START,
            lineSpacing = LyricsLineSpacing.RELAXED,
            inactiveOpacity = 0.22f,
            inactiveScale = 0.82f,
            activeColorMode = LyricsActiveColorMode.WHITE,
            customAccentColor = customAccentColor,
            glowEffect = LyricsGlowEffect.SUBTLE,
            backgroundMode = LyricsBackgroundMode.SOLID,
            progressiveKaraokeEnabled = true,
            blurEnabled = true
        )
        LyricsStylePreset.CINEMATIC -> LevyraLyricsStudioSettings(
            stylePreset = LyricsStylePreset.CINEMATIC,
            fontPreset = LevyraFontPreset.Poppins,
            textSize = LyricsTextSize.LARGE,
            fontWeight = LyricsFontWeightPreset.BOLD,
            alignment = LyricsAlignment.START,
            lineSpacing = LyricsLineSpacing.RELAXED,
            inactiveOpacity = 0.44f,
            inactiveScale = 0.90f,
            activeColorMode = LyricsActiveColorMode.AUTOMATIC,
            customAccentColor = customAccentColor,
            glowEffect = LyricsGlowEffect.VIBRANT,
            backgroundMode = LyricsBackgroundMode.CINEMATIC,
            progressiveKaraokeEnabled = true,
            blurEnabled = true
        )
    }

    fun encode(): String = JSONObject().apply {
        put("stylePreset", stylePreset.name)
        put("fontPreset", fontPreset.name)
        put("textSize", textSize.name)
        put("fontWeight", fontWeight.name)
        put("alignment", alignment.name)
        put("lineSpacing", lineSpacing.name)
        put("inactiveOpacity", inactiveOpacity.toDouble())
        put("inactiveScale", inactiveScale.toDouble())
        put("activeColorMode", activeColorMode.name)
        put("customAccentColor", customAccentColor)
        put("glowEffect", glowEffect.name)
        put("backgroundMode", backgroundMode.name)
        put("progressiveKaraokeEnabled", progressiveKaraokeEnabled)
        put("blurEnabled", blurEnabled)
        put("viewerStyle", viewerStyle.name)
        put("autoScrollEnabled", autoScrollEnabled)
        put("showOriginalLyrics", showOriginalLyrics)
        put("romanizationEnabled", romanizationEnabled)
        put("appleBlurEnabled", appleBlurEnabled)
    }.toString()

    companion object {
        fun decode(raw: String?): LevyraLyricsStudioSettings {
            if (raw.isNullOrBlank()) return LevyraLyricsStudioSettings()
            return runCatching {
                val json = JSONObject(raw)
                LevyraLyricsStudioSettings(
                    stylePreset = LyricsStylePreset.from(json.optString("stylePreset")),
                    fontPreset = LevyraFontPreset.from(json.optString("fontPreset")),
                    textSize = LyricsTextSize.from(json.optString("textSize")),
                    fontWeight = LyricsFontWeightPreset.from(json.optString("fontWeight")),
                    alignment = LyricsAlignment.from(json.optString("alignment")),
                    lineSpacing = LyricsLineSpacing.from(json.optString("lineSpacing")),
                    inactiveOpacity = json.optDouble("inactiveOpacity", 0.52).toFloat(),
                    inactiveScale = json.optDouble("inactiveScale", 1.0).toFloat(),
                    activeColorMode = LyricsActiveColorMode.from(json.optString("activeColorMode")),
                    customAccentColor = json.optInt("customAccentColor", 0xFF64D2FF.toInt()),
                    glowEffect = LyricsGlowEffect.from(json.optString("glowEffect")),
                    backgroundMode = LyricsBackgroundMode.from(json.optString("backgroundMode")),
                    progressiveKaraokeEnabled = json.optBoolean("progressiveKaraokeEnabled", true),
                    blurEnabled = json.optBoolean("blurEnabled", true),
                    viewerStyle = LyricsViewerStyle.from(json.optString("viewerStyle")),
                    autoScrollEnabled = json.optBoolean("autoScrollEnabled", true),
                    showOriginalLyrics = json.optBoolean("showOriginalLyrics", true),
                    romanizationEnabled = json.optBoolean("romanizationEnabled", true),
                    appleBlurEnabled = json.optBoolean("appleBlurEnabled", false)
                ).normalized()
            }.getOrDefault(LevyraLyricsStudioSettings())
        }
    }
}

val DemoLyrics: List<LyricLine> = listOf(
    LyricLine(
        text = "Under the glowing city lights",
        startMs = 0L,
        endMs = 3200L,
        words = listOf(
            LyricWord(0L, 700L, "Under"),
            LyricWord(700L, 1100L, "the"),
            LyricWord(1100L, 1900L, "glowing"),
            LyricWord(1900L, 2400L, "city"),
            LyricWord(2400L, 3200L, "lights")
        )
    ),
    LyricLine(
        text = "Feel the rhythm carry us tonight",
        startMs = 3200L,
        endMs = 6800L,
        words = listOf(
            LyricWord(3200L, 3800L, "Feel"),
            LyricWord(3800L, 4200L, "the"),
            LyricWord(4200L, 4900L, "rhythm"),
            LyricWord(4900L, 5500L, "carry"),
            LyricWord(5500L, 6000L, "us"),
            LyricWord(6000L, 6800L, "tonight")
        )
    ),
    LyricLine(
        text = "Every heartbeat in perfect harmony",
        startMs = 6800L,
        endMs = 10400L,
        words = listOf(
            LyricWord(6800L, 7500L, "Every"),
            LyricWord(7500L, 8400L, "heartbeat"),
            LyricWord(8400L, 8700L, "in"),
            LyricWord(8700L, 9500L, "perfect"),
            LyricWord(9500L, 10400L, "harmony")
        )
    ),
    LyricLine(
        text = "Levyra sings your melody",
        startMs = 10400L,
        endMs = 14000L,
        words = listOf(
            LyricWord(10400L, 11200L, "Levyra"),
            LyricWord(11200L, 11900L, "sings"),
            LyricWord(11900L, 12600L, "your"),
            LyricWord(12600L, 14000L, "melody")
        )
    )
)
