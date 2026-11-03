package com.luc4n3x.levyra.viewmodel

import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.domain.ArtistExclusions
import com.luc4n3x.levyra.domain.MoodCurve
import com.luc4n3x.levyra.domain.MoodCurvePreset
import com.luc4n3x.levyra.domain.MoodEvidence
import com.luc4n3x.levyra.domain.MoodJourneyCandidate
import com.luc4n3x.levyra.domain.MoodJourneyEngine
import com.luc4n3x.levyra.domain.MoodJourneyRequest
import com.luc4n3x.levyra.domain.MoodJourneyResult
import com.luc4n3x.levyra.domain.MoodPoint
import com.luc4n3x.levyra.domain.MoodStyle
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.moodjourney.MoodEditorialEvidence
import com.luc4n3x.levyra.feature.moodjourney.MoodStyleEvidence
import com.luc4n3x.levyra.feature.moodjourney.MoodStyleShelf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

data class MoodJourneyLibraryTrack(val track: Track, val affinity: Float)

interface MoodJourneyGateway {
    suspend fun libraryTracks(): List<MoodJourneyLibraryTrack>
    suspend fun editorialEvidence(): MoodEditorialEvidence
    suspend fun styleShelf(style: MoodStyle): MoodStyleShelf
    suspend fun exclusions(): ArtistExclusions
    suspend fun saveAsPlaylist(name: String, tracks: List<Track>): String
    fun onSaved(playlistId: String)
}

enum class MoodJourneyStage { Loading, Generating, Ready, Empty, Error }

@Immutable
data class MoodJourneySession(
    val generation: Long,
    val curve: MoodCurve,
    val trackCount: Int,
    val stage: MoodJourneyStage = MoodJourneyStage.Loading,
    val result: MoodJourneyResult? = null,
    val pinnedKeys: List<String> = emptyList(),
    val excludedKeys: Set<String> = emptySet(),
    val editorialPlaylists: Int = 0,
    val poolSize: Int = 0,
    val saving: Boolean = false,
    val savedPlaylistId: String? = null,
    val saveFailed: Boolean = false,
    val seed: Long = 0L,
    val styles: Set<MoodStyle> = emptySet(),
    val stylesLoading: Boolean = false,
    val stylePlaylists: Int = 0
)

class MoodJourneyController(
    private val scope: CoroutineScope,
    private val gateway: MoodJourneyGateway,
    private val store: MoodJourneyCurveStore,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val _session = MutableStateFlow<MoodJourneySession?>(null)
    val session: StateFlow<MoodJourneySession?> = _session.asStateFlow()

    private var generation = 0L
    private var loadJob: Job? = null
    private var buildJob: Job? = null
    private var saveJob: Job? = null
    private var styleJob: Job? = null
    private var pool: List<MoodJourneyCandidate> = emptyList()
    private var exclusions: ArtistExclusions = ArtistExclusions.Empty
    private var library: List<MoodJourneyLibraryTrack> = emptyList()
    private var editorial: MoodEditorialEvidence = MoodEditorialEvidence.Empty
    private val shelves = HashMap<MoodStyle, MoodStyleShelf>()

    fun open() {
        generation += 1
        cancelJobs()
        val sessionGeneration = generation
        val saved = store.load()
        _session.value = MoodJourneySession(
            generation = sessionGeneration,
            curve = saved?.curve ?: MoodCurve.preset(MoodCurvePreset.Sunrise),
            trackCount = saved?.trackCount ?: MoodJourneyRequest.DEFAULT_TRACKS,
            styles = saved?.styles.orEmpty()
        )
        loadJob = scope.launch {
            library = try {
                withContext(computeDispatcher) {
                    exclusions = gateway.exclusions()
                    gateway.libraryTracks()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Mood Journey library failed")
                emptyList()
            }
            refreshPool(sessionGeneration)
            if (pool.isNotEmpty()) rebuild(debounceMs = 0L)
            coroutineScope {
                val evidence = async {
                    try {
                        gateway.editorialEvidence()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Timber.w(error, "Mood Journey editorial evidence unavailable")
                        MoodEditorialEvidence.Empty
                    }
                }
                val styled = launch {
                    if (loadStyles(sessionGeneration)) {
                        refreshPool(sessionGeneration)
                        if (pool.isNotEmpty()) rebuild(debounceMs = 0L)
                    }
                }
                editorial = evidence.await()
                styled.join()
            }
            mutate(sessionGeneration) { it.copy(editorialPlaylists = editorial.playlistsRead) }
            refreshPool(sessionGeneration)
            settle(sessionGeneration)
        }
    }

    fun selectStyle(style: MoodStyle?) {
        val current = _session.value ?: return
        val styles = when {
            style == null -> emptySet()
            style in current.styles -> current.styles - style
            else -> current.styles + style
        }
        if (styles == current.styles) return
        mutate(current.generation) { it.copy(styles = styles) }
        store.save(current.curve, current.trackCount, styles)
        val sessionGeneration = current.generation
        styleJob?.cancel()
        styleJob = scope.launch {
            loadStyles(sessionGeneration)
            refreshPool(sessionGeneration)
            if (loadJob?.isActive != true) settle(sessionGeneration)
        }
    }

    private suspend fun loadStyles(sessionGeneration: Long): Boolean {
        val missing = _session.value?.takeIf { it.generation == sessionGeneration }?.styles.orEmpty().filterNot { it in shelves }
        if (missing.isEmpty()) return false
        mutate(sessionGeneration) { it.copy(stylesLoading = true) }
        try {
            val loaded = coroutineScope {
                missing.map { style ->
                    async {
                        try {
                            gateway.styleShelf(style)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            Timber.w(error, "Mood Journey style %s unavailable", style.storageId)
                            MoodStyleShelf.empty(style)
                        }
                    }
                }.awaitAll()
            }
            loaded.filter { it.playlistsRead > 0 }.forEach { shelves[it.style] = it }
        } finally {
            mutate(sessionGeneration) { it.copy(stylesLoading = false) }
        }
        return true
    }

    private suspend fun refreshPool(sessionGeneration: Long) {
        val styles = _session.value?.takeIf { it.generation == sessionGeneration }?.styles ?: return
        val styleEvidence = MoodStyleEvidence(styles.mapNotNull { shelves[it] })
        val currentLibrary = library
        val currentEditorial = editorial
        val candidates = withContext(computeDispatcher) { candidatesFrom(currentLibrary, currentEditorial, styleEvidence, styles) }
        if (_session.value?.generation != sessionGeneration) return
        pool = candidates
        mutate(sessionGeneration) { it.copy(poolSize = candidates.size, stylePlaylists = styleEvidence.playlistsRead) }
    }

    private fun settle(sessionGeneration: Long) {
        val session = _session.value ?: return
        if (session.generation != sessionGeneration) return
        if (pool.isEmpty()) {
            buildJob?.cancel()
            mutate(sessionGeneration) { it.copy(stage = MoodJourneyStage.Empty, result = null) }
        } else {
            rebuild(debounceMs = 0L)
        }
    }

    fun close() {
        generation += 1
        cancelJobs()
        pool = emptyList()
        library = emptyList()
        editorial = MoodEditorialEvidence.Empty
        _session.value = null
    }

    fun movePoint(index: Int, point: MoodPoint) {
        val current = _session.value ?: return
        mutate(current.generation) { it.copy(curve = it.curve.withPoint(index, point)) }
        rebuild(debounceMs = LIVE_DEBOUNCE_MS)
    }

    fun commitCurve() {
        val current = _session.value ?: return
        store.save(current.curve, current.trackCount, current.styles)
        rebuild(debounceMs = 0L)
    }

    fun applyPreset(preset: MoodCurvePreset) {
        val current = _session.value ?: return
        val curve = MoodCurve.preset(preset, current.curve.points.size)
        mutate(current.generation) { it.copy(curve = curve) }
        store.save(curve, current.trackCount, current.styles)
        rebuild(debounceMs = 0L)
    }

    fun setTrackCount(count: Int) {
        val current = _session.value ?: return
        val bounded = count.coerceIn(MoodJourneyRequest.MIN_TRACKS, MoodJourneyRequest.MAX_TRACKS)
        if (bounded == current.trackCount) return
        mutate(current.generation) { it.copy(trackCount = bounded) }
        store.save(current.curve, bounded, current.styles)
        rebuild(debounceMs = LIVE_DEBOUNCE_MS)
    }

    fun togglePin(key: String) {
        val current = _session.value ?: return
        val pinned = if (key in current.pinnedKeys) current.pinnedKeys - key else (current.pinnedKeys + key).takeLast(MAX_PINNED)
        mutate(current.generation) { it.copy(pinnedKeys = pinned) }
        rebuild(debounceMs = 0L)
    }

    fun exclude(key: String) {
        val current = _session.value ?: return
        mutate(current.generation) { it.copy(excludedKeys = it.excludedKeys + key, pinnedKeys = it.pinnedKeys - key) }
        rebuild(debounceMs = 0L)
    }

    fun regenerate() {
        val current = _session.value ?: return
        mutate(current.generation) { it.copy(seed = it.seed + 1) }
        rebuild(debounceMs = 0L, avoidCurrent = true)
    }

    fun saveAsPlaylist(name: String) {
        val current = _session.value ?: return
        val tracks = current.result?.tracks.orEmpty()
        if (tracks.isEmpty() || name.isBlank() || current.saving) return
        val sessionGeneration = current.generation
        mutate(sessionGeneration) { it.copy(saving = true, saveFailed = false) }
        saveJob = scope.launch {
            val playlistId = try {
                gateway.saveAsPlaylist(name.trim(), tracks)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Mood Journey save failed")
                null
            }
            mutate(sessionGeneration) {
                if (playlistId == null) it.copy(saving = false, saveFailed = true)
                else it.copy(saving = false, saveFailed = false, savedPlaylistId = playlistId)
            }
            if (playlistId != null) gateway.onSaved(playlistId)
        }
    }

    private fun rebuild(debounceMs: Long, avoidCurrent: Boolean = false) {
        val current = _session.value ?: return
        if (pool.isEmpty()) return
        val candidates = pool
        val sessionGeneration = current.generation
        buildJob?.cancel()
        buildJob = scope.launch {
            if (debounceMs > 0L) delay(debounceMs)
            val snapshot = _session.value ?: return@launch
            if (snapshot.generation != sessionGeneration) return@launch
            mutate(sessionGeneration) { it.copy(stage = MoodJourneyStage.Generating, savedPlaylistId = null, saveFailed = false) }
            val avoid = if (avoidCurrent) snapshot.result?.steps?.filterNot { it.pinned }?.map { it.key }?.toSet().orEmpty() else emptySet()
            val request = MoodJourneyRequest(
                curve = snapshot.curve,
                trackCount = snapshot.trackCount,
                pinnedKeys = snapshot.pinnedKeys,
                excludedKeys = snapshot.excludedKeys,
                exclusions = exclusions,
                avoidKeys = avoid,
                seed = snapshot.seed,
                styles = snapshot.styles
            )
            val result = try {
                withContext(computeDispatcher) { MoodJourneyEngine.build(candidates, request) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Mood Journey build failed")
                null
            }
            mutate(sessionGeneration) { session ->
                when {
                    result == null -> session.copy(stage = MoodJourneyStage.Error)
                    result.steps.isEmpty() -> session.copy(stage = MoodJourneyStage.Empty, result = result)
                    else -> session.copy(stage = MoodJourneyStage.Ready, result = result)
                }
            }
        }
    }

    private fun candidatesFrom(
        library: List<MoodJourneyLibraryTrack>,
        evidence: MoodEditorialEvidence,
        styleEvidence: MoodStyleEvidence,
        styles: Set<MoodStyle>
    ): List<MoodJourneyCandidate> {
        val candidates = ArrayList<MoodJourneyCandidate>(library.size + evidence.tracks.size + styleEvidence.tracks.size)
        fun candidate(track: Track, affinity: Float): MoodJourneyCandidate {
            val trackStyles = if (styles.isEmpty()) emptySet() else styleEvidence.stylesFor(track).filterTo(HashSet()) { it in styles }
            val labels = evidence.labelsFor(track) + styleEvidence.labelsFor(track)
            return MoodJourneyCandidate(track, MoodEvidence.estimate(track, labels, trackStyles), affinity, trackStyles)
        }
        library.forEach { entry -> candidates += candidate(entry.track, entry.affinity) }
        styleEvidence.tracks.forEach { track -> candidates += candidate(track, 0f) }
        evidence.tracks.forEach { track -> candidates += candidate(track, 0f) }
        return candidates
    }

    private fun cancelJobs() {
        loadJob?.cancel()
        buildJob?.cancel()
        saveJob?.cancel()
        styleJob?.cancel()
        styleJob = null
        loadJob = null
        buildJob = null
        saveJob = null
    }

    private inline fun mutate(sessionGeneration: Long, transform: (MoodJourneySession) -> MoodJourneySession) {
        _session.update { session -> if (session == null || session.generation != sessionGeneration) session else transform(session) }
    }

    private companion object {
        const val LIVE_DEBOUNCE_MS = 280L
        const val MAX_PINNED = 12
    }
}

data class MoodJourneySaved(val curve: MoodCurve, val trackCount: Int, val styles: Set<MoodStyle>)

class MoodJourneyCurveStore(private val preferences: SharedPreferences) {
    fun load(): MoodJourneySaved? = MoodJourneyCurveCodec.decode(preferences.getString(KEY_CURVE, null))

    fun save(curve: MoodCurve, trackCount: Int, styles: Set<MoodStyle>) {
        preferences.edit().putString(KEY_CURVE, MoodJourneyCurveCodec.encode(curve, trackCount, styles)).apply()
    }

    private companion object {
        const val KEY_CURVE = "last_curve"
    }
}

internal object MoodJourneyCurveCodec {
    fun encode(curve: MoodCurve, trackCount: Int, styles: Set<MoodStyle> = emptySet()): String = JSONObject()
        .put("count", trackCount)
        .put("styles", JSONArray().apply { MoodStyle.entries.filter { it in styles }.forEach { put(it.storageId) } })
        .put("points", JSONArray().apply {
            curve.normalized.forEach { put(JSONArray().put(it.energy.toDouble()).put(it.valence.toDouble())) }
        })
        .toString()

    fun decode(raw: String?): MoodJourneySaved? {
        if (raw.isNullOrBlank()) return null
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val array = root.optJSONArray("points") ?: return null
        val points = (0 until array.length()).mapNotNull { index ->
            val pair = array.optJSONArray(index) ?: return@mapNotNull null
            MoodPoint(pair.optDouble(0, 0.5).toFloat(), pair.optDouble(1, 0.5).toFloat()).clamped()
        }
        if (points.size < MoodCurve.MIN_POINTS) return null
        val curve = MoodCurve(points.take(MoodCurve.MAX_POINTS))
        val count = root.optInt("count", MoodJourneyRequest.DEFAULT_TRACKS).coerceIn(MoodJourneyRequest.MIN_TRACKS, MoodJourneyRequest.MAX_TRACKS)
        val stored = root.optJSONArray("styles")
        val styles = if (stored == null) emptySet() else (0 until stored.length()).mapNotNullTo(LinkedHashSet()) { MoodStyle.from(stored.optString(it)) }
        return MoodJourneySaved(curve, count, styles)
    }
}
