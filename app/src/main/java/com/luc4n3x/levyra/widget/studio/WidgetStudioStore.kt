package com.luc4n3x.levyra.widget.studio

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.luc4n3x.levyra.widget.LevyraWidgetBridge
import com.luc4n3x.levyra.widget.LevyraWidgetCenter
import com.luc4n3x.levyra.widget.WidgetTrackSnapshot

object WidgetStudioStore {
    @Volatile
    private var sessionLive = false

    @Volatile
    private var livePositionMs = 0L

    @Volatile
    private var liveSnapshotMs = 0L

    private const val CONFIG_PREFS = "levyra_widget_studio"
    private const val STATE_PREFS = "levyra_widget_studio_state"

    private const val KEY_STYLE = "style"
    private const val KEY_PALETTE = "palette"
    private const val KEY_CUSTOM_COLOR = "custom_color"
    private const val KEY_ARTWORK = "artwork"
    private const val KEY_CONTROLS = "controls"
    private const val KEY_CONFIGURED = "configured"
    private const val KEY_PROGRESS_REFRESH = "progress_refresh"

    private const val KEY_SESSION_PLAYING = "session_playing"
    private const val KEY_SESSION_TITLE = "session_title"
    private const val KEY_SESSION_ARTIST = "session_artist"
    private const val KEY_SESSION_ALBUM = "session_album"
    private const val KEY_SESSION_ARTWORK = "session_artwork"
    private const val KEY_FAVORITE = "favorite"
    private const val KEY_SHUFFLE = "shuffle"
    private const val KEY_REPEAT = "repeat"
    private const val KEY_POSITION = "position"
    private const val KEY_DURATION = "duration"

    private fun configPrefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)

    private fun statePrefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)

    private fun key(widgetId: Int, name: String) = "w${widgetId}_$name"

    fun config(context: Context, widgetId: Int, family: StudioWidgetFamily): StudioWidgetConfig {
        val prefs = configPrefs(context)
        val defaults = family.defaultConfig
        val controls = prefs.getString(key(widgetId, KEY_CONTROLS), null)
            ?.split(',')
            ?.mapNotNullTo(linkedSetOf()) { StudioControl.fromKey(it) }
            ?: defaults.controls
        val stored = StudioWidgetConfig(
            style = StudioWidgetStyle.fromKey(prefs.getString(key(widgetId, KEY_STYLE), null)) ?: defaults.style,
            palette = StudioPalette.fromKey(prefs.getString(key(widgetId, KEY_PALETTE), null)) ?: defaults.palette,
            customColor = prefs.getInt(key(widgetId, KEY_CUSTOM_COLOR), defaults.customColor),
            artwork = StudioArtworkMode.fromKey(prefs.getString(key(widgetId, KEY_ARTWORK), null)) ?: defaults.artwork,
            controls = controls
        )
        return family.normalize(stored)
    }

    fun isConfigured(context: Context, widgetId: Int): Boolean =
        configPrefs(context).getBoolean(key(widgetId, KEY_CONFIGURED), false)

    fun saveConfig(context: Context, widgetId: Int, family: StudioWidgetFamily, config: StudioWidgetConfig) {
        val normalized = family.normalize(config)
        configPrefs(context).edit()
            .putString(key(widgetId, KEY_STYLE), normalized.style.key)
            .putString(key(widgetId, KEY_PALETTE), normalized.palette.key)
            .putInt(key(widgetId, KEY_CUSTOM_COLOR), normalized.customColor)
            .putString(key(widgetId, KEY_ARTWORK), normalized.artwork.key)
            .putString(key(widgetId, KEY_CONTROLS), normalized.controls.joinToString(",") { it.key })
            .putBoolean(key(widgetId, KEY_CONFIGURED), true)
            .apply()
    }

    fun progressRefresh(context: Context): StudioProgressRefresh =
        StudioProgressRefresh.fromKey(configPrefs(context).getString(KEY_PROGRESS_REFRESH, null))

    fun setProgressRefresh(context: Context, refresh: StudioProgressRefresh) {
        configPrefs(context).edit().putString(KEY_PROGRESS_REFRESH, refresh.key).apply()
    }

    fun deleteConfigs(context: Context, widgetIds: IntArray) {
        val prefs = configPrefs(context)
        val prefixes = widgetIds.map { "w${it}_" }
        val editor = prefs.edit()
        prefs.all.keys.filter { stored -> prefixes.any(stored::startsWith) }.forEach(editor::remove)
        editor.apply()
    }

    fun playbackState(context: Context): StudioPlaybackState {
        val legacy = LevyraWidgetCenter.trackSnapshot(context)
        val state = statePrefs(context)
        val sessionActive = sessionLive
        val sessionTitle = if (sessionActive) state.getString(KEY_SESSION_TITLE, "").orEmpty() else ""
        val useSession = sessionTitle.isNotBlank()
        val title = if (useSession) sessionTitle else legacy.title
        val artist = if (useSession) state.getString(KEY_SESSION_ARTIST, "").orEmpty() else legacy.artist
        val sessionArtwork = if (useSession) state.getString(KEY_SESSION_ARTWORK, "").orEmpty() else ""
        val artworkUrl = resolveArtwork(useSession, sessionTitle, sessionArtwork, legacy)
        val repeat = sessionRepeat(state, sessionActive)
        return StudioPlaybackState(
            title = title,
            artist = artist,
            album = if (useSession) state.getString(KEY_SESSION_ALBUM, "").orEmpty() else "",
            artworkUrl = artworkUrl,
            playing = resolvePlaying(state, sessionActive, legacy),
            hasTrack = title.isNotBlank(),
            accentSeed = if (legacy.title == title) legacy.accent else 0,
            favorite = sessionFavorite(state, sessionActive),
            shuffle = sessionShuffle(state, repeat),
            repeat = repeat,
            positionMs = if (sessionActive) livePositionMs else state.getLong(KEY_POSITION, 0L),
            durationMs = state.getLong(KEY_DURATION, 0L),
            snapshotRealtimeMs = if (sessionActive) liveSnapshotMs else SystemClock.elapsedRealtime()
        )
    }

    private fun resolveArtwork(
        useSession: Boolean,
        sessionTitle: String,
        sessionArtwork: String,
        legacy: WidgetTrackSnapshot
    ): String = when {
        !useSession -> legacy.artworkUrl
        legacy.title == sessionTitle && legacy.artworkUrl.isNotBlank() -> legacy.artworkUrl
        else -> sessionArtwork.ifBlank { legacy.artworkUrl }
    }

    private fun resolvePlaying(state: SharedPreferences, sessionActive: Boolean, legacy: WidgetTrackSnapshot): Boolean = when {
        sessionActive -> state.getBoolean(KEY_SESSION_PLAYING, false)
        LevyraWidgetBridge.onToggle != null -> legacy.playing
        else -> false
    }

    private fun sessionRepeat(state: SharedPreferences, sessionActive: Boolean): StudioRepeatState =
        if (sessionActive) {
            StudioRepeatState.entries.getOrElse(state.getInt(KEY_REPEAT, 0)) { StudioRepeatState.Unavailable }
        } else {
            StudioRepeatState.Unavailable
        }

    private fun sessionFavorite(state: SharedPreferences, sessionActive: Boolean): StudioFavoriteState =
        if (sessionActive) {
            StudioFavoriteState.entries.getOrElse(state.getInt(KEY_FAVORITE, 0)) { StudioFavoriteState.Unavailable }
        } else {
            StudioFavoriteState.Unavailable
        }

    private fun sessionShuffle(state: SharedPreferences, repeat: StudioRepeatState): Boolean? =
        if (state.contains(KEY_SHUFFLE) && repeat != StudioRepeatState.Unavailable) {
            state.getBoolean(KEY_SHUFFLE, false)
        } else {
            null
        }

    fun writeSession(
        context: Context,
        active: Boolean,
        playing: Boolean,
        title: String,
        artist: String,
        album: String,
        artworkUrl: String,
        positionMs: Long,
        durationMs: Long
    ): Boolean {
        val prefs = statePrefs(context)
        val changed = sessionLive != active ||
            prefs.getBoolean(KEY_SESSION_PLAYING, false) != playing ||
            prefs.getString(KEY_SESSION_TITLE, "") != title ||
            prefs.getString(KEY_SESSION_ARTIST, "") != artist ||
            prefs.getString(KEY_SESSION_ALBUM, "") != album ||
            prefs.getString(KEY_SESSION_ARTWORK, "") != artworkUrl ||
            prefs.getLong(KEY_DURATION, 0L) != durationMs
        sessionLive = active
        livePositionMs = positionMs
        liveSnapshotMs = SystemClock.elapsedRealtime()
        if (!changed) return false
        prefs.edit()
            .putBoolean(KEY_SESSION_PLAYING, playing)
            .putString(KEY_SESSION_TITLE, title)
            .putString(KEY_SESSION_ARTIST, artist)
            .putString(KEY_SESSION_ALBUM, album)
            .putString(KEY_SESSION_ARTWORK, artworkUrl)
            .putLong(KEY_POSITION, positionMs)
            .putLong(KEY_DURATION, durationMs)
            .apply()
        return true
    }

    fun writeActions(
        context: Context,
        favorite: StudioFavoriteState,
        shuffle: Boolean?,
        repeat: StudioRepeatState
    ) {
        statePrefs(context).edit()
            .putInt(KEY_FAVORITE, favorite.ordinal)
            .apply {
                if (shuffle == null) remove(KEY_SHUFFLE) else putBoolean(KEY_SHUFFLE, shuffle)
            }
            .putInt(KEY_REPEAT, repeat.ordinal)
            .apply()
    }
}
