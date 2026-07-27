package com.luc4n3x.levyra.data

import android.content.Context
import android.util.AtomicFile
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.SmartMusicProfile
import com.luc4n3x.levyra.domain.Track
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.Locale

class LevyraSmartMusicProfileStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, "levyra_smart_music_profile.json")
    private val atomicFile = AtomicFile(file)
    private val engine = LevyraResonanceEngine()

    fun load(): SmartMusicProfile = synchronized(FILE_LOCK) {
        engine.toSmartProfile(readState())
    }

    fun recordPlayback(track: Track): SmartMusicProfile = mutate { state ->
        engine.recordPlayback(state, track)
    }

    fun recordCompletion(track: Track): SmartMusicProfile = mutate { state ->
        engine.recordCompletion(state, track)
    }

    fun recordFavorite(track: Track, added: Boolean): SmartMusicProfile = mutate { state ->
        engine.recordFavorite(state, track, added)
    }

    fun recordDownload(track: Track): SmartMusicProfile = mutate { state ->
        engine.recordDownload(state, track)
    }

    fun recordAlbumOpen(album: AlbumHit): SmartMusicProfile = mutate { state ->
        engine.recordAlbumOpen(state, album)
    }

    internal fun recordTransition(track: Track, positionMs: Long, durationMs: Long = track.durationMs) {
        mutate { state ->
            engine.recordTransition(state, track, positionMs, durationMs)
        }
    }

    internal fun recordReplay(track: Track) {
        mutate { state ->
            engine.recordReplay(state, track)
        }
    }

    internal fun recordResolverResult(source: String, success: Boolean, latencyMs: Long) {
        mutate { state ->
            engine.recordResolverResult(state, source, success, latencyMs)
            state.lastUpdated = System.currentTimeMillis()
        }
    }

    internal fun rankRadioCandidates(
        candidates: List<Track>,
        currentQueue: List<Track>,
        currentTrack: Track?,
        limit: Int = 20
    ): List<Track> = synchronized(FILE_LOCK) {
        engine.rankRadioCandidates(
            state = readState(),
            candidates = candidates,
            currentQueue = currentQueue,
            currentTrack = currentTrack,
            limit = limit
        )
    }

    private fun mutate(block: (LevyraResonanceState) -> Unit): SmartMusicProfile = synchronized(FILE_LOCK) {
        val state = readState()
        block(state)
        if (state.lastUpdated <= 0L) state.lastUpdated = System.currentTimeMillis()
        compact(state)
        writeState(state)
        engine.toSmartProfile(state)
    }

    private fun readState(): LevyraResonanceState {
        if (!file.isFile) return LevyraResonanceState()
        return runCatching {
            val text = atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (text.isBlank()) return@runCatching LevyraResonanceState()
            val root = JSONObject(text)
            val fallbackTimestamp = root.optLong("lastUpdated", System.currentTimeMillis())
            LevyraResonanceState(
                plays = root.optInt("plays", 0).coerceAtLeast(0),
                completedPlays = root.optInt("completedPlays", 0).coerceAtLeast(0),
                favoriteSignals = root.optInt("favoriteSignals", 0).coerceAtLeast(0),
                downloadSignals = root.optInt("downloadSignals", 0).coerceAtLeast(0),
                albumOpenSignals = root.optInt("albumOpenSignals", 0).coerceAtLeast(0),
                skipSignals = root.optInt("skipSignals", 0).coerceAtLeast(0),
                artists = root.optJSONObject("artists").toNodeMap(fallbackTimestamp),
                albums = root.optJSONObject("albums").toNodeMap(fallbackTimestamp),
                moods = root.optJSONObject("moods").toNodeMap(fallbackTimestamp),
                resolverArms = root.optJSONObject("resolverArms").toResolverMap(fallbackTimestamp),
                recentTrackIds = root.optJSONArray("recentTrackIds").toStringList(24),
                recentArtists = root.optJSONArray("recentArtists").toStringList(18),
                session = root.optJSONObject("session").toSessionDna(),
                lastUpdated = fallbackTimestamp.coerceAtLeast(0L)
            )
        }.onFailure { Timber.w(it, "Smart music resonance restore failed") }
            .getOrDefault(LevyraResonanceState())
    }

    private fun writeState(state: LevyraResonanceState) {
        val root = JSONObject()
            .put("version", FORMAT_VERSION)
            .put("plays", state.plays)
            .put("completedPlays", state.completedPlays)
            .put("favoriteSignals", state.favoriteSignals)
            .put("downloadSignals", state.downloadSignals)
            .put("albumOpenSignals", state.albumOpenSignals)
            .put("skipSignals", state.skipSignals)
            .put("lastUpdated", state.lastUpdated)
            .put("artists", state.artists.toNodeObject())
            .put("albums", state.albums.toNodeObject())
            .put("moods", state.moods.toNodeObject())
            .put("resolverArms", state.resolverArms.toResolverObject())
            .put("recentTrackIds", state.recentTrackIds.toJsonArray())
            .put("recentArtists", state.recentArtists.toJsonArray())
            .put("session", state.session.toJsonObject())
        val bytes = root.toString().toByteArray(Charsets.UTF_8)
        var stream: java.io.FileOutputStream? = null
        runCatching {
            val activeStream = atomicFile.startWrite()
            stream = activeStream
            activeStream.write(bytes)
            activeStream.flush()
            atomicFile.finishWrite(activeStream)
            stream = null
        }.onFailure { error ->
            stream?.let(atomicFile::failWrite)
            Timber.w(error, "Smart music resonance save failed")
        }
    }

    private fun JSONObject?.toNodeMap(fallbackTimestamp: Long): MutableMap<String, ResonanceNode> {
        if (this == null) return LinkedHashMap()
        val out = LinkedHashMap<String, ResonanceNode>()
        val iterator = keys()
        while (iterator.hasNext()) {
            val rawKey = iterator.next()
            val key = normalizeStoredKey(rawKey)
            if (key.isBlank()) continue
            val value = opt(rawKey)
            val node = when (value) {
                is JSONObject -> ResonanceNode(
                    weight = value.optDouble("weight", value.optDouble("w", 0.0)).coerceIn(0.0, 9999.0),
                    halfLifeDays = value.optDouble("halfLifeDays", value.optDouble("h", 30.0)).coerceIn(1.0, 365.0),
                    positiveSignals = value.optInt("positiveSignals", value.optInt("p", 1)).coerceIn(0, 100_000),
                    negativeSignals = value.optInt("negativeSignals", value.optInt("n", 0)).coerceIn(0, 100_000),
                    updatedAt = value.optLong("updatedAt", value.optLong("t", fallbackTimestamp)).coerceAtLeast(0L)
                )
                is Number -> ResonanceNode(
                    weight = value.toDouble().coerceIn(0.0, 9999.0),
                    halfLifeDays = 60.0,
                    positiveSignals = 1,
                    updatedAt = fallbackTimestamp
                )
                else -> null
            }
            if (node != null && node.weight > 0.0) out[key] = node
        }
        return out
    }

    private fun JSONObject?.toResolverMap(fallbackTimestamp: Long): MutableMap<String, ResolverArmState> {
        if (this == null) return LinkedHashMap()
        val out = LinkedHashMap<String, ResolverArmState>()
        val iterator = keys()
        while (iterator.hasNext()) {
            val rawKey = iterator.next()
            val key = normalizeStoredKey(rawKey).take(48)
            val value = optJSONObject(rawKey) ?: continue
            if (key.isBlank()) continue
            out[key] = ResolverArmState(
                successes = value.optInt("successes", value.optInt("s", 0)).coerceIn(0, 100_000),
                failures = value.optInt("failures", value.optInt("f", 0)).coerceIn(0, 100_000),
                averageLatencyMs = value.optDouble("averageLatencyMs", value.optDouble("l", 0.0)).coerceIn(0.0, 120_000.0),
                updatedAt = value.optLong("updatedAt", value.optLong("t", fallbackTimestamp)).coerceAtLeast(0L)
            )
        }
        return out
    }

    private fun JSONObject?.toSessionDna(): SessionDna {
        if (this == null) return SessionDna()
        return SessionDna(
            explorationPressure = optDouble("explorationPressure", optDouble("e", 0.5)).coerceIn(0.05, 0.95),
            energyDirection = optDouble("energyDirection", optDouble("d", 0.0)).coerceIn(-1.0, 1.0),
            familiarityNeed = optDouble("familiarityNeed", optDouble("f", 0.35)).coerceIn(0.05, 0.95),
            repetitionTolerance = optDouble("repetitionTolerance", optDouble("r", 0.55)).coerceIn(0.05, 0.95),
            averageCompletion = optDouble("averageCompletion", optDouble("c", 0.5)).coerceIn(0.0, 1.0),
            skipMomentum = optDouble("skipMomentum", optDouble("m", 0.0)).coerceIn(0.0, 1.0),
            consecutiveSkips = optInt("consecutiveSkips", optInt("k", 0)).coerceIn(0, 12),
            lastEnergy = optInt("lastEnergy", optInt("g", -1)).coerceIn(-1, 100),
            startedAt = optLong("startedAt", optLong("a", 0L)).coerceAtLeast(0L),
            lastEventAt = optLong("lastEventAt", optLong("u", 0L)).coerceAtLeast(0L)
        )
    }

    private fun Map<String, ResonanceNode>.toNodeObject(): JSONObject {
        val out = JSONObject()
        forEach { (key, node) ->
            out.put(
                key,
                JSONObject()
                    .put("w", node.weight)
                    .put("h", node.halfLifeDays)
                    .put("p", node.positiveSignals)
                    .put("n", node.negativeSignals)
                    .put("t", node.updatedAt)
            )
        }
        return out
    }

    private fun Map<String, ResolverArmState>.toResolverObject(): JSONObject {
        val out = JSONObject()
        forEach { (key, arm) ->
            out.put(
                key,
                JSONObject()
                    .put("s", arm.successes)
                    .put("f", arm.failures)
                    .put("l", arm.averageLatencyMs)
                    .put("t", arm.updatedAt)
            )
        }
        return out
    }

    private fun SessionDna.toJsonObject(): JSONObject = JSONObject()
        .put("e", explorationPressure)
        .put("d", energyDirection)
        .put("f", familiarityNeed)
        .put("r", repetitionTolerance)
        .put("c", averageCompletion)
        .put("m", skipMomentum)
        .put("k", consecutiveSkips)
        .put("g", lastEnergy)
        .put("a", startedAt)
        .put("u", lastEventAt)

    private fun List<String>.toJsonArray(): JSONArray = JSONArray().also { array ->
        forEach(array::put)
    }

    private fun JSONArray?.toStringList(limit: Int): MutableList<String> {
        if (this == null) return ArrayList()
        val out = ArrayList<String>(minOf(length(), limit))
        val start = (length() - limit).coerceAtLeast(0)
        for (index in start until length()) {
            val value = optString(index).trim()
            if (value.isNotBlank()) out += value.take(180)
        }
        return out
    }

    private fun compact(state: LevyraResonanceState) {
        state.artists.retainTop(MAX_ARTIST_NODES)
        state.albums.retainTop(MAX_ALBUM_NODES)
        state.moods.retainTop(MAX_MOOD_NODES)
        state.resolverArms.retainTop(MAX_RESOLVER_ARMS) { arm ->
            (arm.successes + arm.failures).toDouble() + arm.updatedAt.toDouble() / 1_000_000_000_000.0
        }
        while (state.recentTrackIds.size > 24) state.recentTrackIds.removeAt(0)
        while (state.recentArtists.size > 18) state.recentArtists.removeAt(0)
    }

    private fun MutableMap<String, ResonanceNode>.retainTop(limit: Int) {
        retainTop(limit) { node ->
            node.weight + (node.positiveSignals - node.negativeSignals).toDouble() * 0.05 + node.updatedAt.toDouble() / 1_000_000_000_000.0
        }
    }

    private fun <T> MutableMap<String, T>.retainTop(limit: Int, score: (T) -> Double) {
        if (size <= limit) return
        val keep = entries
            .sortedWith(compareByDescending<Map.Entry<String, T>> { score(it.value) }.thenBy { it.key })
            .take(limit)
            .mapTo(HashSet(), Map.Entry<String, T>::key)
        keys.retainAll(keep)
    }

    private fun normalizeStoredKey(value: String): String = value
        .trim()
        .replace(" • ", "|")
        .replace(Regex("\\s+"), " ")
        .trim(' ', '.', '-', '_', '•')
        .take(180)
        .lowercase(Locale.ROOT)

    private companion object {
        const val FORMAT_VERSION = 2
        const val MAX_ARTIST_NODES = 256
        const val MAX_ALBUM_NODES = 256
        const val MAX_MOOD_NODES = 128
        const val MAX_RESOLVER_ARMS = 24
        val FILE_LOCK = Any()
    }
}
