package com.luc4n3x.levyra.domain

import org.json.JSONObject

enum class MiniPlayerStyle(val storageId: String) {
    Original("original"),
    SuperExpressive("super_expressive"),
    Floating("floating"),
    LiquidGlass("liquid_glass"),
    UltraCompact("ultra_compact"),
    ArtworkFocus("artwork_focus"),
    YtMusic("yt_music"),
    Connected("connected");

    companion object {
        fun from(value: String?): MiniPlayerStyle = entries.firstOrNull { it.storageId == value } ?: Original
    }
}

enum class MiniArtworkShape(val storageId: String) {
    Cookie("cookie"),
    Circle("circle"),
    Rounded("rounded"),
    Clover("clover"),
    Square("square");

    companion object {
        fun from(value: String?): MiniArtworkShape? = entries.firstOrNull { it.storageId == value }
    }
}

enum class MiniProgressStyle(val storageId: String) {
    Ring("ring"),
    Line("line"),
    Wavy("wavy"),
    Fill("fill"),
    Hidden("hidden");

    companion object {
        fun from(value: String?): MiniProgressStyle? = entries.firstOrNull { it.storageId == value }
    }
}

enum class MiniButtonStyle(val storageId: String) {
    Filled("filled"),
    Tonal("tonal"),
    Outlined("outlined"),
    Plain("plain");

    companion object {
        fun from(value: String?): MiniButtonStyle? = entries.firstOrNull { it.storageId == value }
    }
}

enum class MiniControlSet(val storageId: String) {
    Essential("essential"),
    Standard("standard"),
    Full("full");

    companion object {
        fun from(value: String?): MiniControlSet? = entries.firstOrNull { it.storageId == value }
    }
}

enum class MiniColorMode(val storageId: String) {
    Artwork("artwork"),
    Theme("theme"),
    Custom("custom");

    companion object {
        fun from(value: String?): MiniColorMode? = entries.firstOrNull { it.storageId == value }
    }
}

enum class MiniTypography(val storageId: String) {
    Standard("standard"),
    Bold("bold"),
    Editorial("editorial");

    companion object {
        fun from(value: String?): MiniTypography? = entries.firstOrNull { it.storageId == value }
    }
}

enum class MiniMotion(val storageId: String) {
    Reduced("reduced"),
    Normal("normal"),
    Expressive("expressive");

    companion object {
        fun from(value: String?): MiniMotion? = entries.firstOrNull { it.storageId == value }
    }
}

enum class MiniBackground(val storageId: String) {
    Gradient("gradient"),
    Solid("solid"),
    Artwork("artwork");

    companion object {
        fun from(value: String?): MiniBackground? = entries.firstOrNull { it.storageId == value }
    }
}

enum class MiniTuningOption {
    Height,
    Corner,
    Inset,
    ArtworkShape,
    ArtworkSize,
    Shadow,
    Glow,
    Opacity,
    Progress,
    Buttons,
    ControlScale,
    Controls,
    ShowClose,
    Color,
    Motion,
    Typography,
    Background
}

data class MiniPlayerSpec(
    val options: Set<MiniTuningOption>,
    val height: IntRange,
    val corner: IntRange,
    val inset: IntRange,
    val artwork: IntRange,
    val opacity: IntRange,
    val progressStyles: List<MiniProgressStyle>,
    val buttonStyles: List<MiniButtonStyle>,
    val backgrounds: List<MiniBackground>,
    val defaults: MiniPlayerTuning
)

data class MiniPlayerTuning(
    val heightDp: Int,
    val cornerDp: Int,
    val insetDp: Int,
    val artworkShape: MiniArtworkShape,
    val artworkDp: Int,
    val shadow: Int,
    val glow: Int,
    val opacity: Int,
    val progress: MiniProgressStyle,
    val buttons: MiniButtonStyle,
    val controlScale: Int,
    val controls: MiniControlSet,
    val showClose: Boolean,
    val colorMode: MiniColorMode,
    val customColor: Int,
    val motion: MiniMotion,
    val typography: MiniTypography,
    val background: MiniBackground
) {
    fun normalized(style: MiniPlayerStyle): MiniPlayerTuning {
        val spec = LevyraMiniPlayerStudioSettings.spec(style)
        val height = heightDp.coerceIn(spec.height)
        return copy(
            heightDp = height,
            cornerDp = cornerDp.coerceIn(spec.corner).coerceAtMost(height / 2),
            insetDp = insetDp.coerceIn(spec.inset),
            artworkDp = artworkDp.coerceIn(spec.artwork).coerceAtMost(height - MIN_ARTWORK_MARGIN),
            shadow = shadow.coerceIn(0, 100),
            glow = glow.coerceIn(0, 100),
            opacity = opacity.coerceIn(spec.opacity),
            progress = progress.takeIf { it in spec.progressStyles } ?: spec.defaults.progress,
            buttons = buttons.takeIf { it in spec.buttonStyles } ?: spec.defaults.buttons,
            controlScale = controlScale.coerceIn(CONTROL_SCALE_RANGE),
            customColor = customColor or OPAQUE,
            background = background.takeIf { it in spec.backgrounds } ?: spec.defaults.background
        )
    }

    internal fun toJson(): JSONObject = JSONObject().apply {
        put("height", heightDp)
        put("corner", cornerDp)
        put("inset", insetDp)
        put("artworkShape", artworkShape.storageId)
        put("artwork", artworkDp)
        put("shadow", shadow)
        put("glow", glow)
        put("opacity", opacity)
        put("progress", progress.storageId)
        put("buttons", buttons.storageId)
        put("controlScale", controlScale)
        put("controls", controls.storageId)
        put("showClose", showClose)
        put("colorMode", colorMode.storageId)
        put("customColor", customColor)
        put("motion", motion.storageId)
        put("typography", typography.storageId)
        put("background", background.storageId)
    }

    companion object {
        val CONTROL_SCALE_RANGE = 85..125
        private const val MIN_ARTWORK_MARGIN = 12
        private const val OPAQUE = 0xFF000000.toInt()

        internal fun fromJson(json: JSONObject, style: MiniPlayerStyle): MiniPlayerTuning {
            val defaults = LevyraMiniPlayerStudioSettings.spec(style).defaults
            return MiniPlayerTuning(
                heightDp = json.optInt("height", defaults.heightDp),
                cornerDp = json.optInt("corner", defaults.cornerDp),
                insetDp = json.optInt("inset", defaults.insetDp),
                artworkShape = MiniArtworkShape.from(json.optString("artworkShape")) ?: defaults.artworkShape,
                artworkDp = json.optInt("artwork", defaults.artworkDp),
                shadow = json.optInt("shadow", defaults.shadow),
                glow = json.optInt("glow", defaults.glow),
                opacity = json.optInt("opacity", defaults.opacity),
                progress = MiniProgressStyle.from(json.optString("progress")) ?: defaults.progress,
                buttons = MiniButtonStyle.from(json.optString("buttons")) ?: defaults.buttons,
                controlScale = json.optInt("controlScale", defaults.controlScale),
                controls = MiniControlSet.from(json.optString("controls")) ?: defaults.controls,
                showClose = json.optBoolean("showClose", defaults.showClose),
                colorMode = MiniColorMode.from(json.optString("colorMode")) ?: defaults.colorMode,
                customColor = json.optInt("customColor", defaults.customColor),
                motion = MiniMotion.from(json.optString("motion")) ?: defaults.motion,
                typography = MiniTypography.from(json.optString("typography")) ?: defaults.typography,
                background = MiniBackground.from(json.optString("background")) ?: defaults.background
            ).normalized(style)
        }
    }
}

data class LevyraMiniPlayerStudioSettings(
    val style: MiniPlayerStyle = MiniPlayerStyle.Original,
    val tunings: Map<MiniPlayerStyle, MiniPlayerTuning> = emptyMap()
) {
    fun tuningFor(target: MiniPlayerStyle): MiniPlayerTuning =
        tunings[target]?.normalized(target) ?: spec(target).defaults

    fun activeTuning(): MiniPlayerTuning = tuningFor(style)

    fun withStyle(target: MiniPlayerStyle): LevyraMiniPlayerStudioSettings = copy(style = target)

    fun updateTuning(
        target: MiniPlayerStyle,
        transform: (MiniPlayerTuning) -> MiniPlayerTuning
    ): LevyraMiniPlayerStudioSettings {
        if (target == MiniPlayerStyle.Original) return this
        val next = transform(tuningFor(target)).normalized(target)
        return copy(tunings = if (next == spec(target).defaults) tunings - target else tunings + (target to next))
    }

    fun resetStyle(target: MiniPlayerStyle): LevyraMiniPlayerStudioSettings = copy(tunings = tunings - target)

    fun normalized(): LevyraMiniPlayerStudioSettings = copy(
        tunings = tunings
            .filterKeys { it != MiniPlayerStyle.Original }
            .mapValues { (target, tuning) -> tuning.normalized(target) }
            .filter { (target, tuning) -> tuning != spec(target).defaults }
    )

    fun encode(): String = JSONObject().apply {
        put("version", STORAGE_VERSION)
        put("style", style.storageId)
        put("tunings", JSONObject().apply {
            normalized().tunings.forEach { (target, tuning) -> put(target.storageId, tuning.toJson()) }
        })
    }.toString()

    companion object {
        const val STORAGE_VERSION = 1

        private val baseTuning = MiniPlayerTuning(
            heightDp = 64,
            cornerDp = 32,
            insetDp = 12,
            artworkShape = MiniArtworkShape.Cookie,
            artworkDp = 46,
            shadow = 40,
            glow = 40,
            opacity = 60,
            progress = MiniProgressStyle.Ring,
            buttons = MiniButtonStyle.Filled,
            controlScale = 100,
            controls = MiniControlSet.Standard,
            showClose = true,
            colorMode = MiniColorMode.Artwork,
            customColor = 0xFF7C8CFF.toInt(),
            motion = MiniMotion.Normal,
            typography = MiniTypography.Standard,
            background = MiniBackground.Gradient
        )

        private val originalSpec = MiniPlayerSpec(
            options = emptySet(),
            height = 64..64,
            corner = 32..32,
            inset = 12..12,
            artwork = 46..46,
            opacity = 60..60,
            progressStyles = listOf(MiniProgressStyle.Ring),
            buttonStyles = listOf(MiniButtonStyle.Filled),
            backgrounds = listOf(MiniBackground.Gradient),
            defaults = baseTuning
        )

        private val superExpressiveSpec = MiniPlayerSpec(
            options = setOf(
                MiniTuningOption.Height,
                MiniTuningOption.ArtworkShape,
                MiniTuningOption.ArtworkSize,
                MiniTuningOption.Glow,
                MiniTuningOption.Progress,
                MiniTuningOption.Buttons,
                MiniTuningOption.ControlScale,
                MiniTuningOption.Controls,
                MiniTuningOption.ShowClose,
                MiniTuningOption.Color,
                MiniTuningOption.Motion,
                MiniTuningOption.Typography,
                MiniTuningOption.Background
            ),
            height = 60..80,
            corner = 24..40,
            inset = 8..8,
            artwork = 40..60,
            opacity = 100..100,
            progressStyles = listOf(MiniProgressStyle.Wavy, MiniProgressStyle.Ring, MiniProgressStyle.Line),
            buttonStyles = listOf(MiniButtonStyle.Filled, MiniButtonStyle.Tonal),
            backgrounds = listOf(MiniBackground.Gradient, MiniBackground.Solid),
            defaults = baseTuning.copy(
                heightDp = 72,
                cornerDp = 36,
                insetDp = 8,
                artworkShape = MiniArtworkShape.Clover,
                artworkDp = 52,
                glow = 55,
                opacity = 100,
                progress = MiniProgressStyle.Wavy,
                motion = MiniMotion.Expressive,
                typography = MiniTypography.Bold
            )
        )

        private val floatingSpec = MiniPlayerSpec(
            options = setOf(
                MiniTuningOption.Height,
                MiniTuningOption.Corner,
                MiniTuningOption.Inset,
                MiniTuningOption.ArtworkShape,
                MiniTuningOption.ArtworkSize,
                MiniTuningOption.Shadow,
                MiniTuningOption.Progress,
                MiniTuningOption.Buttons,
                MiniTuningOption.ControlScale,
                MiniTuningOption.Controls,
                MiniTuningOption.ShowClose,
                MiniTuningOption.Color,
                MiniTuningOption.Motion,
                MiniTuningOption.Typography
            ),
            height = 56..76,
            corner = 16..38,
            inset = 8..32,
            artwork = 36..56,
            opacity = 100..100,
            progressStyles = listOf(MiniProgressStyle.Line, MiniProgressStyle.Ring, MiniProgressStyle.Hidden),
            buttonStyles = listOf(MiniButtonStyle.Tonal, MiniButtonStyle.Filled, MiniButtonStyle.Outlined, MiniButtonStyle.Plain),
            backgrounds = listOf(MiniBackground.Solid),
            defaults = baseTuning.copy(
                heightDp = 64,
                cornerDp = 32,
                insetDp = 20,
                artworkShape = MiniArtworkShape.Circle,
                artworkDp = 44,
                shadow = 55,
                glow = 0,
                opacity = 100,
                progress = MiniProgressStyle.Line,
                buttons = MiniButtonStyle.Tonal,
                background = MiniBackground.Solid
            )
        )

        private val liquidGlassSpec = MiniPlayerSpec(
            options = setOf(
                MiniTuningOption.Height,
                MiniTuningOption.Corner,
                MiniTuningOption.Inset,
                MiniTuningOption.ArtworkShape,
                MiniTuningOption.ArtworkSize,
                MiniTuningOption.Glow,
                MiniTuningOption.Opacity,
                MiniTuningOption.Progress,
                MiniTuningOption.Buttons,
                MiniTuningOption.Controls,
                MiniTuningOption.ShowClose,
                MiniTuningOption.Color,
                MiniTuningOption.Motion
            ),
            height = 56..76,
            corner = 16..38,
            inset = 0..28,
            artwork = 36..56,
            opacity = 30..85,
            progressStyles = listOf(MiniProgressStyle.Line, MiniProgressStyle.Ring, MiniProgressStyle.Hidden),
            buttonStyles = listOf(MiniButtonStyle.Tonal, MiniButtonStyle.Outlined, MiniButtonStyle.Plain),
            backgrounds = listOf(MiniBackground.Solid),
            defaults = baseTuning.copy(
                heightDp = 64,
                cornerDp = 28,
                insetDp = 12,
                artworkShape = MiniArtworkShape.Rounded,
                artworkDp = 44,
                glow = 30,
                opacity = 55,
                progress = MiniProgressStyle.Line,
                buttons = MiniButtonStyle.Tonal,
                background = MiniBackground.Solid
            )
        )

        private val ultraCompactSpec = MiniPlayerSpec(
            options = setOf(
                MiniTuningOption.Height,
                MiniTuningOption.Corner,
                MiniTuningOption.ArtworkShape,
                MiniTuningOption.ArtworkSize,
                MiniTuningOption.Progress,
                MiniTuningOption.Controls,
                MiniTuningOption.ShowClose,
                MiniTuningOption.Color,
                MiniTuningOption.Typography
            ),
            height = 48..58,
            corner = 8..29,
            inset = 12..12,
            artwork = 28..40,
            opacity = 100..100,
            progressStyles = listOf(MiniProgressStyle.Line, MiniProgressStyle.Fill, MiniProgressStyle.Hidden),
            buttonStyles = listOf(MiniButtonStyle.Plain),
            backgrounds = listOf(MiniBackground.Solid),
            defaults = baseTuning.copy(
                heightDp = 52,
                cornerDp = 16,
                insetDp = 12,
                artworkShape = MiniArtworkShape.Rounded,
                artworkDp = 34,
                shadow = 20,
                glow = 0,
                opacity = 100,
                progress = MiniProgressStyle.Line,
                buttons = MiniButtonStyle.Plain,
                controls = MiniControlSet.Essential,
                showClose = false,
                motion = MiniMotion.Reduced,
                background = MiniBackground.Solid
            )
        )

        private val artworkFocusSpec = MiniPlayerSpec(
            options = setOf(
                MiniTuningOption.Height,
                MiniTuningOption.Corner,
                MiniTuningOption.ArtworkSize,
                MiniTuningOption.Shadow,
                MiniTuningOption.Progress,
                MiniTuningOption.Buttons,
                MiniTuningOption.ControlScale,
                MiniTuningOption.Controls,
                MiniTuningOption.ShowClose,
                MiniTuningOption.Color,
                MiniTuningOption.Motion,
                MiniTuningOption.Typography,
                MiniTuningOption.Background
            ),
            height = 68..88,
            corner = 12..32,
            inset = 12..12,
            artwork = 52..76,
            opacity = 100..100,
            progressStyles = listOf(MiniProgressStyle.Fill, MiniProgressStyle.Line, MiniProgressStyle.Wavy, MiniProgressStyle.Hidden),
            buttonStyles = listOf(MiniButtonStyle.Filled, MiniButtonStyle.Tonal, MiniButtonStyle.Plain),
            backgrounds = listOf(MiniBackground.Artwork, MiniBackground.Gradient),
            defaults = baseTuning.copy(
                heightDp = 76,
                cornerDp = 24,
                insetDp = 12,
                artworkShape = MiniArtworkShape.Square,
                artworkDp = 64,
                shadow = 45,
                glow = 0,
                opacity = 100,
                progress = MiniProgressStyle.Fill,
                typography = MiniTypography.Editorial,
                background = MiniBackground.Artwork
            )
        )

        private val ytMusicSpec = MiniPlayerSpec(
            options = setOf(
                MiniTuningOption.Height,
                MiniTuningOption.ArtworkSize,
                MiniTuningOption.Progress,
                MiniTuningOption.ControlScale,
                MiniTuningOption.Controls,
                MiniTuningOption.ShowClose,
                MiniTuningOption.Typography
            ),
            height = 56..72,
            corner = 0..0,
            inset = 0..0,
            artwork = 40..56,
            opacity = 100..100,
            progressStyles = listOf(MiniProgressStyle.Line, MiniProgressStyle.Hidden),
            buttonStyles = listOf(MiniButtonStyle.Plain),
            backgrounds = listOf(MiniBackground.Solid),
            defaults = baseTuning.copy(
                heightDp = 64,
                cornerDp = 0,
                insetDp = 0,
                artworkShape = MiniArtworkShape.Square,
                artworkDp = 48,
                shadow = 0,
                glow = 0,
                opacity = 100,
                progress = MiniProgressStyle.Line,
                buttons = MiniButtonStyle.Plain,
                controlScale = 115,
                controls = MiniControlSet.Full,
                showClose = false,
                colorMode = MiniColorMode.Theme,
                background = MiniBackground.Solid
            )
        )

        private val connectedSpec = MiniPlayerSpec(
            options = setOf(
                MiniTuningOption.Height,
                MiniTuningOption.ArtworkShape,
                MiniTuningOption.ArtworkSize,
                MiniTuningOption.Progress,
                MiniTuningOption.Buttons,
                MiniTuningOption.ControlScale,
                MiniTuningOption.Controls,
                MiniTuningOption.ShowClose,
                MiniTuningOption.Color,
                MiniTuningOption.Motion,
                MiniTuningOption.Typography
            ),
            height = 56..72,
            corner = 28..28,
            inset = 16..16,
            artwork = 36..56,
            opacity = 100..100,
            progressStyles = listOf(MiniProgressStyle.Line, MiniProgressStyle.Ring, MiniProgressStyle.Hidden),
            buttonStyles = listOf(MiniButtonStyle.Tonal, MiniButtonStyle.Filled, MiniButtonStyle.Plain),
            backgrounds = listOf(MiniBackground.Solid),
            defaults = baseTuning.copy(
                heightDp = 64,
                cornerDp = 28,
                insetDp = 16,
                artworkShape = MiniArtworkShape.Rounded,
                artworkDp = 44,
                shadow = 0,
                glow = 0,
                opacity = 100,
                progress = MiniProgressStyle.Line,
                buttons = MiniButtonStyle.Tonal,
                background = MiniBackground.Solid
            )
        )

        fun spec(style: MiniPlayerStyle): MiniPlayerSpec = when (style) {
            MiniPlayerStyle.Original -> originalSpec
            MiniPlayerStyle.SuperExpressive -> superExpressiveSpec
            MiniPlayerStyle.Floating -> floatingSpec
            MiniPlayerStyle.LiquidGlass -> liquidGlassSpec
            MiniPlayerStyle.UltraCompact -> ultraCompactSpec
            MiniPlayerStyle.ArtworkFocus -> artworkFocusSpec
            MiniPlayerStyle.YtMusic -> ytMusicSpec
            MiniPlayerStyle.Connected -> connectedSpec
        }

        fun decode(raw: String?): LevyraMiniPlayerStudioSettings {
            if (raw.isNullOrBlank()) return LevyraMiniPlayerStudioSettings()
            return runCatching {
                val json = JSONObject(raw)
                val tuningsJson = json.optJSONObject("tunings")
                val tunings = buildMap {
                    tuningsJson?.keys()?.forEach { key ->
                        val target = MiniPlayerStyle.entries.firstOrNull { it.storageId == key } ?: return@forEach
                        val tuning = tuningsJson.optJSONObject(key) ?: return@forEach
                        put(target, MiniPlayerTuning.fromJson(tuning, target))
                    }
                }
                LevyraMiniPlayerStudioSettings(
                    style = MiniPlayerStyle.from(json.optString("style")),
                    tunings = tunings
                ).normalized()
            }.getOrDefault(LevyraMiniPlayerStudioSettings())
        }
    }
}
