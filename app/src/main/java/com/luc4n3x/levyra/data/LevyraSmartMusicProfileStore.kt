package com.luc4n3x.levyra.data

import android.content.Context
import android.util.AtomicFile
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.SmartMusicProfile
import com.luc4n3x.levyra.domain.Track
import java.io.File
import java.io.FileNotFoundException
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class LevyraSmartMusicProfileStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val atomicFile = AtomicFile(file)
    private val cacheKey = file.absolutePath
    private val engine = LevyraResonanceEngine()

    fun load(): SmartMusicProfile = synchronized(FILE_LOCK) {
        engine.toSmartProfile(cachedStateLocked())
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

    internal suspend fun rankRadioCandidates(
        candidates: List<Track>,
        currentQueue: List<Track>,
        currentTrack: Track?,
        limit: Int = LEVYRA_MAX_RANK_LIMIT
    ): List<Track> = withContext(Dispatchers.IO) {
        val state = synchronized(FILE_LOCK) {
            cachedStateLocked().deepCopy()
        }
        engine.rankRadioCandidates(
            state = state,
            candidates = candidates,
            currentQueue = currentQueue,
            currentTrack = currentTrack,
            limit = limit
        )
    }

    internal suspend fun flush() = withContext(Dispatchers.IO) {
        val pending = synchronized(FILE_LOCK) {
            PERSIST_JOBS.remove(cacheKey)?.cancel()
            val cache = STATE_CACHES[cacheKey] ?: return@withContext
            val state = cache.state ?: return@withContext
            cache.generation to state.deepCopy()
        }
        val success = synchronized(FILE_LOCK) {
            writeState(pending.second)
        }
        if (success) {
            synchronized(FILE_LOCK) {
                STATE_CACHES[cacheKey]?.let { cache ->
                    cache.persistedGeneration = maxOf(cache.persistedGeneration, pending.first)
                }
            }
        }
    }

    private fun mutate(block: (LevyraResonanceState) -> Unit): SmartMusicProfile {
        val pending = synchronized(FILE_LOCK) {
            val cache = cacheEntryLocked()
            val state = cachedStateLocked()
            block(state)
            if (state.lastUpdated <= 0L) state.lastUpdated = System.currentTimeMillis()
            compact(state)
            cache.generation += 1L
            PendingPersistence(
                generation = cache.generation,
                state = state.deepCopy(),
                profile = engine.toSmartProfile(state)
            )
        }
        schedulePersist(pending.generation)
        return pending.profile
    }

    private fun cachedStateLocked(): LevyraResonanceState {
        val cache = cacheEntryLocked()
        cache.state?.let { return it }
        return readState().also { cache.state = it }
    }

    private fun cacheEntryLocked(): CachedProfile =
        STATE_CACHES.getOrPut(cacheKey) { CachedProfile() }

    private fun schedulePersist(generation: Long) {
        synchronized(FILE_LOCK) {
            PERSIST_JOBS.remove(cacheKey)?.cancel()
            PERSIST_JOBS[cacheKey] = PERSIST_SCOPE.launch {
                delay(PERSIST_DEBOUNCE_MS)
                persistLatest(generation)
            }
        }
    }

    private fun persistLatest(requestedGeneration: Long) {
        val pending = synchronized(FILE_LOCK) {
            val cache = STATE_CACHES[cacheKey] ?: return
            if (cache.generation < requestedGeneration || cache.persistedGeneration >= cache.generation) return
            cache.generation to (cache.state ?: return).deepCopy()
        }
        val success = synchronized(FILE_LOCK) {
            val cache = STATE_CACHES[cacheKey] ?: return
            if (cache.generation != pending.first) return
            writeState(pending.second)
        }
        synchronized(FILE_LOCK) {
            if (success) {
                STATE_CACHES[cacheKey]?.let { cache ->
                    cache.persistedGeneration = maxOf(cache.persistedGeneration, pending.first)
                }
            }
            PERSIST_JOBS.remove(cacheKey)
        }
    }

    private fun readState(): LevyraResonanceState {
        return try {
            val text = atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
            decodeLevyraResonanceState(text)
        } catch (_: FileNotFoundException) {
            LevyraResonanceState()
        } catch (error: Exception) {
            Timber.w(error, "Smart music resonance restore failed")
            LevyraResonanceState()
        }
    }

    private fun writeState(state: LevyraResonanceState): Boolean {
        val bytes = encodeLevyraResonanceState(state).toByteArray(Charsets.UTF_8)
        var stream: java.io.FileOutputStream? = null
        return runCatching {
            val activeStream = atomicFile.startWrite()
            stream = activeStream
            activeStream.write(bytes)
            activeStream.flush()
            atomicFile.finishWrite(activeStream)
            stream = null
            true
        }.onFailure { error ->
            stream?.let(atomicFile::failWrite)
            Timber.w(error, "Smart music resonance save failed")
        }.getOrDefault(false)
    }

    private fun compact(state: LevyraResonanceState) {
        val now = System.currentTimeMillis()
        state.artists.retainTop(MAX_ARTIST_NODES, now)
        state.albums.retainTop(MAX_ALBUM_NODES, now)
        state.moods.retainTop(MAX_MOOD_NODES, now)
        state.resolverArms.retainTop(MAX_RESOLVER_ARMS) { arm ->
            val observations = (arm.successes + arm.failures).toDouble()
            observations + recencyFreshness(arm.updatedAt, now) * 0.75
        }
        while (state.recentTrackIds.size > LEVYRA_RECENT_TRACK_LIMIT) state.recentTrackIds.removeAt(0)
        while (state.recentArtists.size > LEVYRA_RECENT_ARTIST_LIMIT) state.recentArtists.removeAt(0)
    }

    private fun MutableMap<String, ResonanceNode>.retainTop(limit: Int, now: Long) {
        retainTop(limit) { node ->
            node.weight +
                (node.positiveSignals - node.negativeSignals).toDouble() * 0.05 +
                recencyFreshness(node.updatedAt, now) * 0.5
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

    private fun recencyFreshness(updatedAt: Long, now: Long): Double {
        if (updatedAt <= 0L || now <= updatedAt) return 1.0
        val ageDays = (now - updatedAt).toDouble() / STORE_DAY_MS.toDouble()
        return 1.0 / (1.0 + ageDays / 30.0)
    }

    private fun LevyraResonanceState.deepCopy(): LevyraResonanceState = LevyraResonanceState(
        plays = plays,
        completedPlays = completedPlays,
        favoriteSignals = favoriteSignals,
        downloadSignals = downloadSignals,
        albumOpenSignals = albumOpenSignals,
        skipSignals = skipSignals,
        artists = artists.mapValuesTo(LinkedHashMap()) { (_, node) -> node.copy() },
        albums = albums.mapValuesTo(LinkedHashMap()) { (_, node) -> node.copy() },
        moods = moods.mapValuesTo(LinkedHashMap()) { (_, node) -> node.copy() },
        resolverArms = resolverArms.mapValuesTo(LinkedHashMap()) { (_, arm) -> arm.copy() },
        recentTrackIds = ArrayList(recentTrackIds),
        recentArtists = ArrayList(recentArtists),
        session = session.copy(),
        lastUpdated = lastUpdated
    )

    private data class PendingPersistence(
        val generation: Long,
        val state: LevyraResonanceState,
        val profile: SmartMusicProfile
    )

    private data class CachedProfile(
        var state: LevyraResonanceState? = null,
        var generation: Long = 0L,
        var persistedGeneration: Long = 0L
    )

    companion object {
        private const val FILE_NAME = "levyra_smart_music_profile.json"
        private const val PERSIST_DEBOUNCE_MS = 650L
        private const val MAX_ARTIST_NODES = 256
        private const val MAX_ALBUM_NODES = 256
        private const val MAX_MOOD_NODES = 128
        private const val MAX_RESOLVER_ARMS = 24
        private const val STORE_DAY_MS = 86_400_000L
        private val FILE_LOCK = Any()
        private val STATE_CACHES = HashMap<String, CachedProfile>()
        private val PERSIST_JOBS = HashMap<String, Job>()
        private val PERSIST_SCOPE = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Volatile
        private var instance: LevyraSmartMusicProfileStore? = null

        fun get(context: Context): LevyraSmartMusicProfileStore = instance ?: synchronized(this) {
            instance ?: LevyraSmartMusicProfileStore(context.applicationContext).also { instance = it }
        }
    }
}

internal fun encodeLevyraResonanceState(state: LevyraResonanceState): String {
    return JSONObject()
        .put("version", RESONANCE_FORMAT_VERSION)
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
        .toString()
}

internal fun decodeLevyraResonanceState(
    text: String,
    now: Long = System.currentTimeMillis()
): LevyraResonanceState {
    if (text.isBlank()) return LevyraResonanceState()
    val root = JSONObject(text)
    val version = root.optInt("version", LEGACY_RESONANCE_FORMAT_VERSION)
    return when (version) {
        LEGACY_RESONANCE_FORMAT_VERSION -> decodeResonanceRoot(root, now, legacy = true)
        RESONANCE_FORMAT_VERSION -> decodeResonanceRoot(root, now, legacy = false)
        else -> decodeResonanceRoot(root, now, legacy = false)
    }
}

internal fun normalizeLevyraStoredKey(value: String): String = value
    .trim()
    .replace(" • ", "|")
    .replace(STORED_WHITESPACE_REGEX, " ")
    .trim(' ', '.', '-', '_', '•')
    .take(180)
    .lowercase(Locale.ROOT)

private fun decodeResonanceRoot(
    root: JSONObject,
    now: Long,
    legacy: Boolean
): LevyraResonanceState {
    val fallbackTimestamp = root.optLong("lastUpdated", now).coerceAtLeast(0L)
    return LevyraResonanceState(
        plays = root.optInt("plays", 0).coerceAtLeast(0),
        completedPlays = root.optInt("completedPlays", 0).coerceAtLeast(0),
        favoriteSignals = root.optInt("favoriteSignals", 0).coerceAtLeast(0),
        downloadSignals = root.optInt("downloadSignals", 0).coerceAtLeast(0),
        albumOpenSignals = root.optInt("albumOpenSignals", 0).coerceAtLeast(0),
        skipSignals = root.optInt("skipSignals", 0).coerceAtLeast(0),
        artists = root.optJSONObject("artists").toNodeMap(fallbackTimestamp, legacy),
        albums = root.optJSONObject("albums").toNodeMap(fallbackTimestamp, legacy),
        moods = root.optJSONObject("moods").toNodeMap(fallbackTimestamp, legacy),
        resolverArms = root.optJSONObject("resolverArms").toResolverMap(fallbackTimestamp),
        recentTrackIds = root.optJSONArray("recentTrackIds").toStringList(LEVYRA_RECENT_TRACK_LIMIT),
        recentArtists = root.optJSONArray("recentArtists").toStringList(LEVYRA_RECENT_ARTIST_LIMIT),
        session = root.optJSONObject("session").toSessionDna(),
        lastUpdated = fallbackTimestamp
    )
}

private fun JSONObject?.toNodeMap(
    fallbackTimestamp: Long,
    legacy: Boolean
): MutableMap<String, ResonanceNode> {
    if (this == null) return LinkedHashMap()
    val out = LinkedHashMap<String, ResonanceNode>()
    val iterator = keys()
    while (iterator.hasNext()) {
        val rawKey = iterator.next()
        val key = normalizeLevyraStoredKey(rawKey)
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
                halfLifeDays = if (legacy) 60.0 else 30.0,
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
        val key = normalizeLevyraStoredKey(rawKey).take(48)
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

private const val LEGACY_RESONANCE_FORMAT_VERSION = 1
private const val RESONANCE_FORMAT_VERSION = 2
private val STORED_WHITESPACE_REGEX = Regex("\\s+")
