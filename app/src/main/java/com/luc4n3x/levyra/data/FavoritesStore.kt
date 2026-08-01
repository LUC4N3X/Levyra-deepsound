package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.toFavoriteTrackEntity
import com.luc4n3x.levyra.data.local.toTrack
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import timber.log.Timber

class FavoritesStore(context: Context) {
    private val appContext = context.applicationContext
    private val dao = LevyraDatabase.get(appContext).favoriteTracksDao()
    private val legacyPrefs = appContext.getSharedPreferences("levyra_favorites", Context.MODE_PRIVATE)

    fun load(): List<Track> = runBlocking(Dispatchers.IO) {
        mutationMutex.withLock { loadInternal() }
    }

    fun save(tracks: List<Track>) {
        runBlocking(Dispatchers.IO) {
            mutationMutex.withLock { saveAndCompleteMigration(tracks) }
        }
    }

    suspend fun saveSuspending(tracks: List<Track>) = withContext(Dispatchers.IO) {
        mutationMutex.withLock { saveAndCompleteMigration(tracks) }
    }

    suspend fun toggleFavorite(track: Track): List<Track> = withContext(Dispatchers.IO) {
        mutationMutex.withLock {
            val current = loadInternal()
            val targetKey = favoriteKey(track)
            val existingIndex = current.indexOfFirst { favorite ->
                favoriteKey(favorite).equals(targetKey, ignoreCase = true)
            }
            val updated = if (existingIndex >= 0) {
                current.filterIndexed { index, _ -> index != existingIndex }
            } else {
                listOf(track) + current
            }
            saveAndCompleteMigration(updated)
            updated
        }
    }

    private suspend fun loadInternal(): List<Track> {
        val stored = runCatching { dao.all().map { it.toTrack() } }
            .onFailure { Timber.w(it, "Favorite tracks load failed") }
            .getOrNull()
            ?: return emptyList()
        if (stored.isNotEmpty()) {
            completeLegacyMigration()
            return stored
        }
        if (legacyPrefs.getBoolean(MIGRATION_COMPLETE_KEY, false)) return emptyList()

        val legacy = loadLegacyForMigration() ?: return emptyList()
        replaceAll(legacy)
        completeLegacyMigration()
        return legacy
    }

    private suspend fun saveAndCompleteMigration(tracks: List<Track>) {
        try {
            replaceAll(tracks)
            completeLegacyMigration()
        } catch (error: Throwable) {
            Timber.w(error, "Favorite tracks save failed")
            throw error
        }
    }

    private suspend fun replaceAll(tracks: List<Track>) {
        val now = System.currentTimeMillis()
        dao.replaceAll(tracks.mapIndexed { index, track ->
            track.toFavoriteTrackEntity(now - index)
        })
    }

    private fun loadLegacyForMigration(): List<Track>? {
        val raw = legacyPrefs.getString(KEY, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let(TrackJson::fromJson)
            }
        }.onFailure { Timber.w(it, "Legacy favorites migration failed") }
            .getOrNull()
    }

    private fun completeLegacyMigration() {
        if (legacyPrefs.getBoolean(MIGRATION_COMPLETE_KEY, false) && !legacyPrefs.contains(KEY)) return
        val committed = legacyPrefs.edit()
            .remove(KEY)
            .putBoolean(MIGRATION_COMPLETE_KEY, true)
            .commit()
        check(committed) { "Unable to complete legacy favorites migration" }
    }

    private fun favoriteKey(track: Track): String {
        return track.id.ifBlank { "${track.artist.trim()}|${track.title.trim()}" }
    }

    private companion object {
        const val KEY = "liked_tracks"
        const val MIGRATION_COMPLETE_KEY = "liked_tracks_migrated_to_room"

        // Shared by every store instance so UI and service mutations cannot interleave.
        val mutationMutex = Mutex()
    }
}
