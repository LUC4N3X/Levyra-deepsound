package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.SmartMusicProfile
import com.luc4n3x.levyra.domain.SmartMusicTasteSeed
import com.luc4n3x.levyra.domain.Track
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.Locale

class LevyraSmartMusicProfileStore(context: Context) {
    private val lock = Any()
    private val file = File(context.applicationContext.filesDir, "levyra_smart_music_profile.json")

    fun load(): SmartMusicProfile = synchronized(lock) {
        readProfile()
    }

    fun recordPlayback(track: Track): SmartMusicProfile = mutate { profile ->
        profile.plays += 1
        profile.touchTrack(track, 4)
    }

    fun recordCompletion(track: Track): SmartMusicProfile = mutate { profile ->
        profile.completedPlays += 1
        profile.touchTrack(track, 12)
    }

    fun recordFavorite(track: Track, added: Boolean): SmartMusicProfile = mutate { profile ->
        if (added) {
            profile.favoriteSignals += 1
            profile.touchTrack(track, 18)
        } else {
            profile.touchTrack(track, -6)
        }
    }

    fun recordDownload(track: Track): SmartMusicProfile = mutate { profile ->
        profile.downloadSignals += 1
        profile.touchTrack(track, 22)
    }

    fun recordAlbumOpen(album: AlbumHit): SmartMusicProfile = mutate { profile ->
        profile.albumOpenSignals += 1
        profile.touchAlbum(album.title, album.artist, 10)
    }

    private fun mutate(block: (MutableProfile) -> Unit): SmartMusicProfile = synchronized(lock) {
        val profile = readMutableProfile()
        block(profile)
        profile.lastUpdated = System.currentTimeMillis()
        val stable = profile.toStable()
        writeStable(stable)
        stable
    }

    private fun MutableProfile.touchTrack(track: Track, weight: Int) {
        val artist = track.artist.cleanSeed()
        val album = track.album.cleanSeed()
        if (artist.isNotBlank()) add(artists, artist, weight)
        if (album.isNotBlank() && artist.isNotBlank() && !album.equals(track.title.cleanSeed(), ignoreCase = true)) add(albums, "$album|$artist", weight)
        track.moodTags.take(5).forEach { tag ->
            val clean = tag.cleanSeed()
            if (clean.isNotBlank()) add(moods, clean, (weight / 2).coerceAtLeast(1))
        }
    }

    private fun MutableProfile.touchAlbum(title: String, artist: String, weight: Int) {
        val cleanTitle = title.cleanSeed()
        val cleanArtist = artist.cleanSeed()
        if (cleanArtist.isNotBlank()) add(artists, cleanArtist, weight)
        if (cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) add(albums, "$cleanTitle|$cleanArtist", weight)
    }

    private fun add(target: MutableMap<String, Int>, key: String, delta: Int) {
        val current = target[key] ?: 0
        val next = (current + delta).coerceAtLeast(0)
        if (next == 0) target.remove(key) else target[key] = next.coerceAtMost(9999)
    }

    private fun readProfile(): SmartMusicProfile = readMutableProfile().toStable()

    private fun readMutableProfile(): MutableProfile {
        if (!file.isFile) return MutableProfile()
        return runCatching {
            val root = JSONObject(file.readText())
            MutableProfile(
                plays = root.optInt("plays", 0),
                completedPlays = root.optInt("completedPlays", 0),
                favoriteSignals = root.optInt("favoriteSignals", 0),
                downloadSignals = root.optInt("downloadSignals", 0),
                albumOpenSignals = root.optInt("albumOpenSignals", 0),
                artists = root.optJSONObject("artists").toScoreMap(),
                albums = root.optJSONObject("albums").toScoreMap(),
                moods = root.optJSONObject("moods").toScoreMap(),
                lastUpdated = root.optLong("lastUpdated", 0L)
            )
        }.onFailure { Timber.w(it, "Smart music profile restore failed") }.getOrDefault(MutableProfile())
    }

    private fun writeStable(profile: SmartMusicProfile) {
        val root = JSONObject()
            .put("plays", profile.plays)
            .put("completedPlays", profile.completedPlays)
            .put("favoriteSignals", profile.favoriteSignals)
            .put("downloadSignals", profile.downloadSignals)
            .put("albumOpenSignals", profile.albumOpenSignals)
            .put("lastUpdated", profile.lastUpdated)
            .put("artists", profile.topArtists.toScoreObject())
            .put("albums", profile.topAlbums.toAlbumScoreObject())
            .put("moods", profile.topMoods.toScoreObject())
        runCatching { file.writeText(root.toString()) }
            .onFailure { Timber.w(it, "Smart music profile save failed") }
    }

    private fun JSONObject?.toScoreMap(): MutableMap<String, Int> {
        if (this == null) return LinkedHashMap()
        val out = LinkedHashMap<String, Int>()
        val iterator = keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val clean = key.cleanSeed()
            val value = optInt(key, 0)
            if (clean.isNotBlank() && value > 0) out[clean] = value
        }
        return out
    }

    private fun List<SmartMusicTasteSeed>.toScoreObject(): JSONObject {
        val out = JSONObject()
        forEach { seed -> out.put(seed.label, seed.weight) }
        return out
    }

    private fun List<SmartMusicTasteSeed>.toAlbumScoreObject(): JSONObject {
        val out = JSONObject()
        forEach { seed ->
            val label = seed.label
            val key = if (label.contains(" • ")) label.replace(" • ", "|") else label
            out.put(key, seed.weight)
        }
        return out
    }

    private data class MutableProfile(
        var plays: Int = 0,
        var completedPlays: Int = 0,
        var favoriteSignals: Int = 0,
        var downloadSignals: Int = 0,
        var albumOpenSignals: Int = 0,
        val artists: MutableMap<String, Int> = LinkedHashMap(),
        val albums: MutableMap<String, Int> = LinkedHashMap(),
        val moods: MutableMap<String, Int> = LinkedHashMap(),
        var lastUpdated: Long = 0L
    ) {
        fun toStable(): SmartMusicProfile {
            val artistSeeds = artists.toSeeds { label -> SmartMusicTasteSeed(label.toTitleSeed(), "${label.toTitleSeed()} album", artists[label] ?: 0) }
            val albumSeeds = albums.toSeeds { key ->
                val parts = key.split("|", limit = 2)
                val title = parts.getOrNull(0).orEmpty().toTitleSeed()
                val artist = parts.getOrNull(1).orEmpty().toTitleSeed()
                SmartMusicTasteSeed(listOf(title, artist).filter { it.isNotBlank() }.joinToString(" • "), listOf(title, artist, "album").filter { it.isNotBlank() }.joinToString(" "), albums[key] ?: 0)
            }
            val moodSeeds = moods.toSeeds { label -> SmartMusicTasteSeed(label.toTitleSeed(), "${label.toTitleSeed()} music", moods[label] ?: 0) }
            return SmartMusicProfile(
                plays = plays,
                completedPlays = completedPlays,
                favoriteSignals = favoriteSignals,
                downloadSignals = downloadSignals,
                albumOpenSignals = albumOpenSignals,
                topArtists = artistSeeds,
                topAlbums = albumSeeds,
                topMoods = moodSeeds,
                lastUpdated = lastUpdated
            )
        }

        private fun Map<String, Int>.toSeeds(factory: (String) -> SmartMusicTasteSeed): List<SmartMusicTasteSeed> {
            return entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(16)
                .map { factory(it.key) }
        }

        private fun String.toTitleSeed(): String {
            return split(" ").filter { it.isNotBlank() }.joinToString(" ") { token ->
                token.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString() }
            }
        }
    }

    private fun String.cleanSeed(): String {
        return trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("(?i)\\s*[(\\[].*?(official|video|lyrics|audio|prod\\.|feat\\.).*?[)\\]]"), "")
            .trim(' ', '.', '-', '_', '•')
            .take(80)
            .lowercase(Locale.ROOT)
    }

}
