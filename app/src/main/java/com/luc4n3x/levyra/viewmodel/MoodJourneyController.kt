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
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.moodjourney.MoodEditorialEvidence
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    val seed: Long = 0L
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
    private var pool: List<MoodJourneyCandidate> = emptyList()
    private var exclusions: ArtistExclusions = ArtistExclusions.Empty

    fun open() {
        generation += 1
        cancelJobs()
        val sessionGeneration = generation
        val saved = store.load()
        _session.value = MoodJourneySession(
            generation = sessionGeneration,
            curve = saved?.first ?: MoodCurve.preset(MoodCurvePreset.Sunrise),
            trackCount = saved?.second ?: MoodJourneyRequest.DEFAULT_TRACKS
        )
        loadJob = scope.launch {
            val library = try {
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
            pool = withContext(computeDispatcher) { candidatesFrom(library, MoodEditorialEvidence.Empty) }
            mutate(sessionGeneration) { it.copy(poolSize = pool.size) }
            if (pool.isNotEmpty()) rebuild(debounceMs = 0L)
            val evidence = try {
                gateway.editorialEvidence()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Mood Journey editorial evidence unavailable")
                MoodEditorialEvidence.Empty
            }
            pool = withContext(computeDispatcher) { candidatesFrom(library, evidence) }
            mutate(sessionGeneration) { it.copy(editorialPlaylists = evidence.playlistsRead, poolSize = pool.size) }
            if (pool.isEmpty()) {
                mutate(sessionGeneration) { it.copy(stage = MoodJourneyStage.Empty) }
            } else {
                rebuild(debounceMs = 0L)
            }
        }
    }

    fun close() {
        generation += 1
        cancelJobs()
        pool = emptyList()
        _session.value = null
    }

    fun movePoint(index: Int, point: MoodPoint) {
        val current = _session.value ?: return
        mutate(current.generation) { it.copy(curve = it.curve.withPoint(index, point)) }
        rebuild(debounceMs = LIVE_DEBOUNCE_MS)
    }

    fun commitCurve() {
        val current = _session.value ?: return
        store.save(current.curve, current.trackCount)
        rebuild(debounceMs = 0L)
    }

    fun applyPreset(preset: MoodCurvePreset) {
        val current = _session.value ?: return
        val curve = MoodCurve.preset(preset, current.curve.points.size)
        mutate(current.generation) { it.copy(curve = curve) }
        store.save(curve, current.trackCount)
        rebuild(debounceMs = 0L)
    }

    fun setTrackCount(count: Int) {
        val current = _session.value ?: return
        val bounded = count.coerceIn(MoodJourneyRequest.MIN_TRACKS, MoodJourneyRequest.MAX_TRACKS)
        if (bounded == current.trackCount) return
        mutate(current.generation) { it.copy(trackCount = bounded) }
        store.save(current.curve, bounded)
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
                seed = snapshot.seed
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

    private fun candidatesFrom(library: List<MoodJourneyLibraryTrack>, evidence: MoodEditorialEvidence): List<MoodJourneyCandidate> {
        val candidates = ArrayList<MoodJourneyCandidate>(library.size + evidence.tracks.size)
        library.forEach { entry ->
            candidates += MoodJourneyCandidate(entry.track, MoodEvidence.estimate(entry.track, evidence.labelsFor(entry.track)), entry.affinity)
        }
        evidence.tracks.forEach { track ->
            candidates += MoodJourneyCandidate(track, MoodEvidence.estimate(track, evidence.labelsFor(track)), 0f)
        }
        return candidates
    }

    private fun cancelJobs() {
        loadJob?.cancel()
        buildJob?.cancel()
        saveJob?.cancel()
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

class MoodJourneyCurveStore(private val preferences: SharedPreferences) {
    fun load(): Pair<MoodCurve, Int>? = MoodJourneyCurveCodec.decode(preferences.getString(KEY_CURVE, null))

    fun save(curve: MoodCurve, trackCount: Int) {
        preferences.edit().putString(KEY_CURVE, MoodJourneyCurveCodec.encode(curve, trackCount)).apply()
    }

    private companion object {
        const val KEY_CURVE = "last_curve"
    }
}

internal object MoodJourneyCurveCodec {
    fun encode(curve: MoodCurve, trackCount: Int): String = JSONObject()
        .put("count", trackCount)
        .put("points", JSONArray().apply {
            curve.normalized.forEach { put(JSONArray().put(it.energy.toDouble()).put(it.valence.toDouble())) }
        })
        .toString()

    fun decode(raw: String?): Pair<MoodCurve, Int>? {
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
        return curve to count
    }
}
