package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.toFavoriteTrackEntity
import com.luc4n3x.levyra.data.local.toTrack
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import timber.log.Timber

class FavoritesStore(context: Context) {
    private val appContext = context.applicationContext
    private val dao = LevyraDatabase.get(appContext).favoriteTracksDao()
    private val legacyPrefs = appContext.getSharedPreferences("levyra_favorites", Context.MODE_PRIVATE)

    fun load(): List<Track> = runBlocking(Dispatchers.IO) {
        val stored = runCatching { dao.all().map { it.toTrack() } }
            .onFailure { Timber.w(it, "Favorite tracks load failed") }
            .getOrDefault(emptyList())
        if (stored.isNotEmpty()) return@runBlocking stored
        val legacy = loadLegacy()
        if (legacy.isNotEmpty()) saveInternal(legacy)
        legacy
    }

    fun save(tracks: List<Track>) {
        runBlocking(Dispatchers.IO) { saveInternal(tracks) }
    }

    private suspend fun saveInternal(tracks: List<Track>) {
        runCatching {
            val now = System.currentTimeMillis()
            dao.replaceAll(tracks.mapIndexed { index, track -> track.toFavoriteTrackEntity(now - index) })
        }.onFailure { Timber.w(it, "Favorite tracks save failed") }
    }

    private fun loadLegacy(): List<Track> {
        val raw = legacyPrefs.getString(KEY, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let(TrackJson::fromJson) }
        }.onFailure { Timber.w(it, "Legacy favorites migration failed") }.getOrDefault(emptyList())
    }

    private companion object {
        const val KEY = "liked_tracks"
    }
}
