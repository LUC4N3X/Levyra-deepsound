package com.luc4n3x.levyra.widget.studio

enum class StudioWidgetFamily(
    val key: String,
    val styles: List<StudioWidgetStyle>,
    val artworkModes: List<StudioArtworkMode>,
    val controls: List<StudioControl>,
    val defaultControls: Set<StudioControl>
) {
    Mini(
        key = "mini",
        styles = listOf(StudioWidgetStyle.Expressive, StudioWidgetStyle.Minimal),
        artworkModes = listOf(StudioArtworkMode.FullBleed, StudioArtworkMode.Circular),
        controls = emptyList(),
        defaultControls = emptySet()
    ),
    Slim(
        key = "slim",
        styles = listOf(StudioWidgetStyle.Expressive, StudioWidgetStyle.Minimal),
        artworkModes = listOf(StudioArtworkMode.Normal, StudioArtworkMode.Circular),
        controls = listOf(StudioControl.Like, StudioControl.Previous),
        defaultControls = setOf(StudioControl.Like)
    ),
    Expressive(
        key = "expressive",
        styles = listOf(StudioWidgetStyle.Expressive, StudioWidgetStyle.Minimal),
        artworkModes = listOf(StudioArtworkMode.Normal, StudioArtworkMode.Circular),
        controls = listOf(
            StudioControl.Like, StudioControl.Progress, StudioControl.Shuffle, StudioControl.Repeat,
            StudioControl.Lyrics, StudioControl.Queue
        ),
        defaultControls = setOf(StudioControl.Like, StudioControl.Progress, StudioControl.Lyrics)
    ),
    Hero(
        key = "hero",
        styles = listOf(StudioWidgetStyle.ArtworkFocus, StudioWidgetStyle.ArtworkControls),
        artworkModes = emptyList(),
        controls = listOf(
            StudioControl.Like, StudioControl.Progress, StudioControl.Shuffle, StudioControl.Repeat,
            StudioControl.Lyrics, StudioControl.Queue
        ),
        defaultControls = setOf(StudioControl.Like, StudioControl.Progress, StudioControl.Lyrics)
    ),
    Immersive(
        key = "immersive",
        styles = listOf(StudioWidgetStyle.Immersive),
        artworkModes = listOf(StudioArtworkMode.Large, StudioArtworkMode.FullBleed),
        controls = listOf(
            StudioControl.Like, StudioControl.Progress, StudioControl.Shuffle, StudioControl.Repeat,
            StudioControl.Queue, StudioControl.Lyrics, StudioControl.Flow, StudioControl.Downloads
        ),
        defaultControls = setOf(
            StudioControl.Like, StudioControl.Progress, StudioControl.Shuffle, StudioControl.Repeat,
            StudioControl.Queue, StudioControl.Lyrics
        )
    ),
    Vinyl(
        key = "vinyl",
        styles = listOf(StudioWidgetStyle.Vinyl),
        artworkModes = emptyList(),
        controls = listOf(StudioControl.Like),
        defaultControls = setOf(StudioControl.Like)
    ),
    QuickActions(
        key = "quick",
        styles = listOf(StudioWidgetStyle.Expressive, StudioWidgetStyle.Minimal),
        artworkModes = emptyList(),
        controls = emptyList(),
        defaultControls = emptySet()
    );

    val defaultConfig: StudioWidgetConfig
        get() = StudioWidgetConfig(
            style = styles.first(),
            palette = if (this == QuickActions) StudioPalette.MaterialYou else StudioPalette.Artwork,
            customColor = StudioWidgetConfig.DEFAULT_CUSTOM_COLOR,
            artwork = artworkModes.firstOrNull() ?: StudioArtworkMode.Normal,
            controls = defaultControls
        )

    fun normalize(config: StudioWidgetConfig): StudioWidgetConfig = config.copy(
        style = config.style.takeIf { it in styles } ?: styles.first(),
        artwork = config.artwork.takeIf { it in artworkModes } ?: artworkModes.firstOrNull() ?: StudioArtworkMode.Normal,
        controls = config.controls.filterTo(linkedSetOf()) { it in controls }
    )

    companion object {
        fun fromKey(key: String?): StudioWidgetFamily? = entries.firstOrNull { it.key == key }
    }
}

enum class StudioWidgetStyle(val key: String) {
    Expressive("expressive"),
    Minimal("minimal"),
    ArtworkFocus("artwork_focus"),
    ArtworkControls("artwork_controls"),
    Vinyl("vinyl"),
    Immersive("immersive");

    companion object {
        fun fromKey(key: String?): StudioWidgetStyle? = entries.firstOrNull { it.key == key }
    }
}

enum class StudioPalette(val key: String) {
    Artwork("artwork"),
    MaterialYou("material_you"),
    Levyra("levyra"),
    Light("light"),
    Dark("dark"),
    Custom("custom");

    companion object {
        fun fromKey(key: String?): StudioPalette? = entries.firstOrNull { it.key == key }
    }
}

enum class StudioArtworkMode(val key: String) {
    Normal("normal"),
    Large("large"),
    FullBleed("full_bleed"),
    Circular("circular");

    companion object {
        fun fromKey(key: String?): StudioArtworkMode? = entries.firstOrNull { it.key == key }
    }
}

enum class StudioControl(val key: String) {
    Like("like"),
    Previous("previous"),
    Shuffle("shuffle"),
    Repeat("repeat"),
    Progress("progress"),
    Lyrics("lyrics"),
    Queue("queue"),
    Flow("flow"),
    Downloads("downloads");

    companion object {
        fun fromKey(key: String?): StudioControl? = entries.firstOrNull { it.key == key }
    }
}

data class StudioWidgetConfig(
    val style: StudioWidgetStyle,
    val palette: StudioPalette,
    val customColor: Int,
    val artwork: StudioArtworkMode,
    val controls: Set<StudioControl>
) {
    companion object {
        const val DEFAULT_CUSTOM_COLOR = 0xFF7C5CFF.toInt()
        val customSwatches = listOf(
            0xFF7C5CFF.toInt(),
            0xFF3D7BFF.toInt(),
            0xFF00A389.toInt(),
            0xFF6DAA2C.toInt(),
            0xFFE0A100.toInt(),
            0xFFFF7A45.toInt(),
            0xFFE5486B.toInt(),
            0xFFB04CC8.toInt()
        )
    }
}

enum class StudioProgressRefresh(val key: String, val intervalMs: Long) {
    Saver("saver", 30_000L),
    Balanced("balanced", 10_000L),
    Smooth("smooth", 1_000L);

    companion object {
        fun fromKey(key: String?): StudioProgressRefresh = entries.firstOrNull { it.key == key } ?: Saver
    }
}

enum class StudioFavoriteState { Unavailable, NotFavorite, Favorite }

enum class StudioRepeatState { Unavailable, Off, All, One }

data class StudioPlaybackState(
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val playing: Boolean,
    val hasTrack: Boolean,
    val accentSeed: Int,
    val favorite: StudioFavoriteState,
    val shuffle: Boolean?,
    val repeat: StudioRepeatState,
    val positionMs: Long,
    val durationMs: Long,
    val snapshotRealtimeMs: Long
) {
    fun positionAt(nowRealtimeMs: Long): Long {
        if (durationMs <= 0L) return 0L
        val elapsed = if (playing) (nowRealtimeMs - snapshotRealtimeMs).coerceAtLeast(0L) else 0L
        return (positionMs + elapsed).coerceIn(0L, durationMs)
    }
}
