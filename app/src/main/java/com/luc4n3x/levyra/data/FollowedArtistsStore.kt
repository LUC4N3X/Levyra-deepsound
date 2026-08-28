package com.luc4n3x.levyra.data

import android.content.Context
import androidx.room.withTransaction
import com.luc4n3x.levyra.data.local.FollowedArtistEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.FollowedArtist
import org.json.JSONArray
import timber.log.Timber

class FollowedArtistsStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val database = LevyraDatabase.get(appContext)
    private val dao = database.followedArtistsDao()

    suspend fun load(): List<FollowedArtist> {
        migrateLegacyRowsIfNeeded()
        return dao.all().map { it.toDomain() }
    }

    private suspend fun migrateLegacyRowsIfNeeded() {
        if (dao.all().isNotEmpty()) return
        val raw = prefs.getString(KEY_ARTISTS, null).orEmpty()
        if (raw.isBlank()) return
        val legacy = try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let { json ->
                    val name = json.optString("name").trim()
                    val browseId = json.optString("browseId").trim()
                    if (name.isBlank()) return@let null
                    FollowedArtist(
                        browseId = browseId,
                        name = name,
                        thumbnailUrl = json.optString("thumbnailUrl"),
                        followedAt = json.optLong("followedAt", 0L)
                    )
                }
            }
        } catch (error: Exception) {
            Timber.w(error, "Followed artists migration failed; preserving legacy data")
            return
        }
        val migrated = runCatching {
            database.withTransaction {
                if (legacy.isNotEmpty()) dao.upsertAll(legacy.map(FollowedArtist::toEntity))
            }
        }.onFailure { Timber.w(it, "Followed artists Room migration failed; preserving legacy data") }
        if (migrated.isFailure) return
        if (!prefs.edit().remove(KEY_ARTISTS).commit()) {
            Timber.w("Followed artists legacy cleanup was not persisted")
        }
    }

    suspend fun save(artists: List<FollowedArtist>) {
        val rows = artists.map(FollowedArtist::toEntity)
        database.withTransaction {
            dao.clear()
            dao.upsertAll(rows)
        }
    }

    suspend fun saveDurable(artists: List<FollowedArtist>) = save(artists)

    fun hasReleaseBaseline(artistKey: String): Boolean = prefs.contains(KEY_KNOWN_PREFIX + artistKey)

    fun knownReleases(artistKey: String): Set<String> =
        prefs.getStringSet(KEY_KNOWN_PREFIX + artistKey, emptySet()).orEmpty()

    fun saveKnownReleases(artistKey: String, keys: Set<String>) {
        prefs.edit().putStringSet(KEY_KNOWN_PREFIX + artistKey, keys.take(200).toSet()).apply()
    }

    fun clearKnownReleases(artistKey: String) {
        prefs.edit().remove(KEY_KNOWN_PREFIX + artistKey).apply()
    }

    fun radarOffset(): Int = prefs.getInt(KEY_RADAR_OFFSET, 0).coerceAtLeast(0)

    fun saveRadarOffset(offset: Int) {
        prefs.edit().putInt(KEY_RADAR_OFFSET, offset.coerceAtLeast(0)).apply()
    }

    private companion object {
        const val PREFS_NAME = "levyra_followed_artists"
        const val KEY_ARTISTS = "artists"
        const val KEY_KNOWN_PREFIX = "known_releases_"
        const val KEY_RADAR_OFFSET = "radar_offset"
    }
}

private fun FollowedArtist.toEntity() = FollowedArtistEntity(
    artistKey = browseId.ifBlank { "legacy:${name.trim().lowercase()}" },
    browseId = browseId,
    name = name,
    thumbnailUrl = thumbnailUrl,
    followedAt = followedAt
)

private fun FollowedArtistEntity.toDomain() = FollowedArtist(
    browseId = browseId,
    name = name,
    thumbnailUrl = thumbnailUrl,
    followedAt = followedAt
)
